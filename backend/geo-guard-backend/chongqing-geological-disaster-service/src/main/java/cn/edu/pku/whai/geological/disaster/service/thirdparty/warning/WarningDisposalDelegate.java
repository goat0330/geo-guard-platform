package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.UserInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorPointMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.JcdWarningFetchResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorPointWarnings;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningPageResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorPointResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorWarningHandleDetailResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.WarningContext;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * 三方预警数据拆分处理服务（从 ThirdPartyWarningDataServiceImpl 提取为独立文件）。
 *
 * @author kongweiguang
 */
class WarningDisposalDelegate {
    private static final Duration DEFAULT_WARNING_LOOKBACK_DURATION = ThirdPartyWarningDataSplitSupport.DEFAULT_WARNING_LOOKBACK_DURATION;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = ThirdPartyWarningDataSplitSupport.DATE_TIME_FORMATTER;
    private static final String DEFAULT_REGION_CODE = ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE;
    private static final ZoneId BEIJING_ZONE_ID = ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID;

    private final ThirdPartyWarningDataSplitSupport support;
    private final MonitorPointMapper monitorPointMapper;
    private final IAdRegionService adRegionService;
    private final ThirdPartyWarningRemoteSupport remoteSupport;
    private final Map<String, String> adRegionNameCache;
    private final LocalBindingDelegate localBindingDelegate;
    private final ParallelFetchDelegate parallelFetchDelegate;

    WarningDisposalDelegate(ThirdPartyWarningDataSplitSupport support) {
        this.support = support;
        this.monitorPointMapper = support.monitorPointMapper;
        this.adRegionService = support.adRegionService;
        this.remoteSupport = support.remoteSupport;
        this.adRegionNameCache = support.adRegionNameCache;
        this.localBindingDelegate = support.localBindingDelegate;
        this.parallelFetchDelegate = support.parallelFetchDelegate;
    }

    List<ThirdPartyWarningDisposalVo> fetchAllMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, int targetRecordCount) {

        return parallelFetchDelegate.fetchAllMonitorWarningDisposal(bo, targetRecordCount);
    }

    /**
     * 根据入参查询本地监测点
     */
    List<MonitorPoint> queryLocalMonitorPoints(ThirdPartyWarningDisposalBo bo) {
        LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getMonitorPointName()), MonitorPoint::getMonitorName, bo.getMonitorPointName());
        if (StrUtil.isNotBlank(bo.getRegionCode())) {
            if (bo.getRegionCode().length() < 12) {
                lqw.likeRight(MonitorPoint::getAdministrativeRegionCode, bo.getRegionCode());
            } else {
                lqw.eq(MonitorPoint::getAdministrativeRegionCode, bo.getRegionCode());
            }
        }
        return monitorPointMapper.selectList(lqw);
    }

    /**
     * 将本地监测点转换为三方监测点对象
     */
    List<ThirdMonitorPointResp> convertToThirdMonitorPoints(List<MonitorPoint> localMonitorPoints) {
        List<ThirdMonitorPointResp> result = new ArrayList<>();
        for (MonitorPoint localMonitorPoint : localMonitorPoints) {
            ThirdMonitorPointResp resp = new ThirdMonitorPointResp();
            resp.setId(localMonitorPoint.getId());
            resp.setJcdbh(localMonitorPoint.getMonitorCode());
            resp.setJcdname(localMonitorPoint.getMonitorName());
            result.add(resp);
        }
        return result;
    }

    /**
     * 构建本地监测点映射
     */
    Map<String, MonitorPoint> buildLocalMonitorPointMap(List<MonitorPoint> localMonitorPoints) {
        return localBindingDelegate.buildLocalMonitorPointMap(localMonitorPoints);
    }

    /**
     * 判断查询是否依赖处置详情字段
     */
    boolean needDetailFilter(ThirdPartyWarningDisposalBo bo) {
        if (bo == null) {
            return false;
        }
        return bo.getValidWarning() != null
            || bo.getDisposalStatus() != null
            || StrUtil.isNotBlank(bo.getWarningDisposalTime());
    }

    /**
     * 为当前页记录补充处置详情字段
     */
    void enrichPagedWarningDetail(List<ThirdPartyWarningDisposalVo> pagedRecords) {
        if (pagedRecords == null || pagedRecords.isEmpty()) {
            return;
        }
        List<String> warningIds = pagedRecords.stream()
                                              .map(ThirdPartyWarningDisposalVo::getWarningId)
                                              .filter(StrUtil::isNotBlank)
                                              .toList();
        Map<String, ThirdMonitorWarningHandleDetailResp> detailRespMap = parallelFetchDelegate.fetchMonitorWarningHandleDetailByIdsParallel(warningIds);
        for (ThirdPartyWarningDisposalVo record : pagedRecords) {
            if (StrUtil.isBlank(record.getWarningId())) {
                continue;
            }
            ThirdMonitorWarningHandleDetailResp detailResp = detailRespMap.get(record.getWarningId());
            if (detailResp == null) {
                continue;
            }
            record.setDisposalPerson(detailResp.getClr());
            record.setWarningDisposalTime(detailResp.getClsj());
            record.setValidWarning(resolveValidWarningFlag(detailResp));
        }
    }

    /**
     * 并发按预警id查询处置详情
     */
    Map<String, ThirdMonitorWarningHandleDetailResp> fetchMonitorWarningHandleDetailByIdsParallel(List<String> warningIds) {
        return parallelFetchDelegate.fetchMonitorWarningHandleDetailByIdsParallel(warningIds);
    }

    /**
     * 并发查询预警上下文对应的处置详情
     */
    Map<String, ThirdMonitorWarningHandleDetailResp> fetchMonitorWarningHandleDetailParallel(List<WarningContext> warningContexts) {
        return parallelFetchDelegate.fetchMonitorWarningHandleDetailParallel(warningContexts);
    }

    /**
     * 并发查询监测点下的预警记录
     */
    List<MonitorPointWarnings> fetchMonitorPointWarningsParallel(List<ThirdMonitorPointResp> monitorPoints, ThirdPartyWarningDisposalBo bo) {
        return parallelFetchDelegate.fetchMonitorPointWarningsParallel(monitorPoints, bo);
    }

    /**
     * 拉取单个监测点预警并记录耗时，失败降级为空列表
     */
    JcdWarningFetchResult fetchWarningsForJcdId(String jcdId) {
        return parallelFetchDelegate.fetchWarningsForJcdId(jcdId);
    }

    JcdWarningFetchResult fetchWarningsForJcdId(String jcdId, BooleanSupplier stopRequested) {
        return parallelFetchDelegate.fetchWarningsForJcdId(jcdId, stopRequested);
    }

    /**
     * 拉取监测点下全部预警记录
     */
    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(String monitorPointId, Integer warningLevel, String disposalType) {
        return parallelFetchDelegate.fetchAllMonitorDeviceWarnings(monitorPointId, warningLevel, disposalType);
    }

    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(
        String monitorPointId,
        Integer warningLevel,
        String disposalType,
        LocalDateTime warningStartTime,
        LocalDateTime warningEndTime
    ) {

        return parallelFetchDelegate.fetchAllMonitorDeviceWarnings(monitorPointId, warningLevel, disposalType, warningStartTime, warningEndTime);
    }

    /**
     * 获取三方预警监测点详情
     */
    ThirdMonitorDeviceWarningPageResp fetchMonitorDeviceWarningPage(
        String monitorPointId, Integer warningLevel, String disposalType, int current) {
        return parallelFetchDelegate.fetchMonitorDeviceWarningPage(monitorPointId, warningLevel, disposalType, current);
    }

    /**
     * 获取三方监测预警分页数据
     */
    ThirdMonitorDeviceWarningPageResp fetchMonitorDeviceWarningPage(
        String monitorPointId,
        String monitorPointName,
        String regionCode,
        Integer warningLevel,
        Integer validWarning,
        String disposalType,
        int current,
        int size,
        LocalDateTime warningStartTime,
        LocalDateTime warningEndTime
    ) {

        return parallelFetchDelegate.fetchMonitorDeviceWarningPage(monitorPointId, monitorPointName, regionCode, warningLevel, validWarning, disposalType, current, size, warningStartTime, warningEndTime);
    }

    ThirdMonitorDeviceWarningPageResp parseMonitorDeviceWarningPageResp(Object data) {
        JSONObject dataObj = JSONUtil.parseObj(data);
        ThirdMonitorDeviceWarningPageResp resp = new ThirdMonitorDeviceWarningPageResp();
        resp.setRecords(parseMonitorDeviceWarningRecords(support.parseFlexibleJsonArray(dataObj.get("records"), "records")));
        resp.setTotal(dataObj.getInt("total"));
        resp.setSize(dataObj.getInt("size"));
        resp.setCurrent(dataObj.getInt("current"));
        resp.setPages(dataObj.getInt("pages"));
        return resp;
    }

    List<ThirdMonitorDeviceWarningResp> parseMonitorDeviceWarningRecords(JSONArray recordsArray) {
        List<ThirdMonitorDeviceWarningResp> records = new ArrayList<>();
        if (recordsArray == null) {
            return records;
        }
        for (int i = 0; i < recordsArray.size(); i++) {
            JSONObject obj = recordsArray.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            ThirdMonitorDeviceWarningResp record = new ThirdMonitorDeviceWarningResp();
            record.setAddvcd(obj.getStr("addvcd"));
            record.setYjsj(obj.getStr("yjsj"));
            record.setJcdid(obj.getStr("jcdid"));
            record.setJcdbh(obj.getStr("jcdbh"));
            record.setJcdname(obj.getStr("jcdname"));
            record.setJcdyjid(obj.getStr("jcdyjid"));
            record.setType(obj.getStr("type"));
            record.setSbid(obj.getStr("sbid"));
            record.setSbname(StrUtil.blankToDefault(obj.getStr("sbname"), obj.getStr("yjsbmc")));
            record.setYjdj(StrUtil.blankToDefault(obj.getStr("yjdj"), obj.getStr("yjlevel")));
            record.setClr(obj.getStr("clr"));
            record.setClsj(obj.getStr("clsj"));
            record.setSfwb(obj.getInt("sfwb"));
            record.setCzzt(obj.getStr("czzt"));
            record.setLon(obj.getDouble("lon"));
            record.setLat(obj.getDouble("lat"));
            records.add(record);
        }
        return records;
    }

    /**
     * 判断是否为三方返回非JSON导致的解析异常（常见为HTML错误页）
     */
    boolean isNonJsonResponseException(Exception e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (StrUtil.isNotBlank(message)
                && (message.contains("JsonParseException")
                || message.contains("Unexpected character ('<'"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 判断是否为网络连接重置异常
     */
    boolean isConnectionResetException(Exception e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (StrUtil.isNotBlank(message) && message.toLowerCase().contains("connection reset")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 判断是否为三方监测预警 SSO 凭证失效。该类错误继续请求只会批量失败，需要终止当前批次后续调用。
     */
    boolean isWarningSsoAuthFailure(ThirdPartyWarningDataResp resp) {
        if (resp == null) {
            return false;
        }
        return isWarningSsoAuthFailureText(resp.getStatus(), resp.getMessage());
    }

    boolean isWarningSsoAuthFailure(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ThirdPartyWarningAuthException) {
                return true;
            }
            if (isWarningSsoAuthFailureText(null, current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    ThirdPartyWarningAuthException toWarningSsoAuthException(Throwable e) {
        if (e instanceof ThirdPartyWarningAuthException authException) {
            return authException;
        }
        String message = e == null ? "三方监测预警SSO凭证失效" : e.getMessage();
        return new ThirdPartyWarningAuthException(StrUtil.blankToDefault(message, "三方监测预警SSO凭证失效"), e);
    }

    private boolean isWarningSsoAuthFailureText(String status, String message) {
        String statusText = StrUtil.trimToEmpty(status);
        if ("412".equals(statusText)) {
            return true;
        }
        String text = StrUtil.trimToEmpty(message);
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return text.contains("[status=412]")
            || lowerText.contains("status=412")
            || text.contains("用户凭证不存在")
            || text.contains("单点登陆失败")
            || text.contains("SSO凭证")
            || lowerText.contains("sso")
            || lowerText.contains("credential");
    }

    /**
     * 获取单条预警处置详情
     */
    ThirdMonitorWarningHandleDetailResp fetchMonitorWarningHandleDetail(String warningId) {

        return parallelFetchDelegate.fetchMonitorWarningHandleDetail(warningId);
    }

    /**
     * 确保当前已获取sso凭证
     */
    UserInfoResp loginAuth() {
        UserInfoResp result = remoteSupport.loginAuth();
        support.token = remoteSupport.getRawToken();
        return result;
    }

    void ensureWarningSsoToken() {
        support.token = remoteSupport.getToken();
    }

    /**
     * 获取监测相关三方接口地址
     */
    String getMonitorWarningUrl() {
        return remoteSupport.getMonitorWarningUrl();
    }

    /**
     * 构造三方接口异常信息
     */
    ServiceException buildThirdPartyException(String prefix, ThirdPartyWarningDataResp resp) {
        return remoteSupport.buildThirdPartyException(prefix, resp);
    }

    /**
     * 根据三方监测点信息匹配本地监测点
     */
    MonitorPoint resolveLocalMonitorPoint(Map<String, MonitorPoint> localMonitorPointMap, ThirdMonitorPointResp monitorPointResp) {
        if (localMonitorPointMap == null || localMonitorPointMap.isEmpty() || monitorPointResp == null) {
            return null;
        }
        MonitorPoint localMonitorPoint = localMonitorPointMap.get("ID:" + monitorPointResp.getId());
        if (localMonitorPoint != null) {
            return localMonitorPoint;
        }
        return localMonitorPointMap.get("CODE:" + monitorPointResp.getJcdbh());
    }

    /**
     * 组装预警处置展示对象
     */
    ThirdPartyWarningDisposalVo buildMonitorWarningDisposalVo(
        ThirdMonitorPointResp monitorPointResp,
        ThirdMonitorDeviceWarningResp warningResp,
        ThirdMonitorWarningHandleDetailResp detailResp,
        MonitorPoint localMonitorPoint,
        String fallbackRegionName) {

        ThirdPartyWarningDisposalVo vo = new ThirdPartyWarningDisposalVo();
        vo.setWarningId(warningResp.getJcdyjid());
        vo.setMonitorPointName(StrUtil.blankToDefault(
            monitorPointResp == null ? null : monitorPointResp.getJcdname(),
            warningResp.getJcdname()
        ));
        vo.setMonitoringPointId(localMonitorPoint == null ? null : localMonitorPoint.getId());
        vo.setAdministrativeRegion(resolveAdministrativeRegion(localMonitorPoint, fallbackRegionName));
        vo.setLatitude(localMonitorPoint == null ? warningResp.getLat() : localMonitorPoint.getLatitude());
        vo.setLongitude(localMonitorPoint == null ? warningResp.getLon() : localMonitorPoint.getLongitude());
        vo.setWarningDeviceName(warningResp.getSbname());
        vo.setWarningLevel(convertFromThirdPartyWarningLevel(StrUtil.blankToDefault(warningResp.getYjdj(), detailResp == null ? null : detailResp.getYjdj())));
        vo.setWarningPublishTime(warningResp.getYjsj());
        vo.setDisposalType(warningResp.getType());
        vo.setDisposalPerson(firstNonBlank(detailResp == null ? null : detailResp.getClr(), warningResp.getClr()));
        vo.setWarningDisposalTime(firstNonBlank(detailResp == null ? null : detailResp.getClsj(), warningResp.getClsj()));
        vo.setValidWarning(detailResp == null ? resolveValidWarningFlag(warningResp) : resolveValidWarningFlag(detailResp));
        return vo;
    }

    ThirdPartyWarningDisposalVo buildMonitorWarningDisposalVo(
        ThirdMonitorDeviceWarningResp warningResp,
        MonitorPoint localMonitorPoint,
        String fallbackRegionName
    ) {
        if (warningResp == null) {
            return null;
        }
        ThirdPartyWarningDisposalVo vo = new ThirdPartyWarningDisposalVo();
        vo.setWarningId(warningResp.getJcdyjid());
        vo.setMonitorPointName(StrUtil.blankToDefault(
            warningResp.getJcdname(),
            localMonitorPoint == null ? null : localMonitorPoint.getMonitorName()
        ));
        vo.setMonitoringPointId(localMonitorPoint == null ? null : localMonitorPoint.getId());
        vo.setAdministrativeRegion(resolveAdministrativeRegion(localMonitorPoint, fallbackRegionName));
        vo.setLatitude(localMonitorPoint == null ? warningResp.getLat() : localMonitorPoint.getLatitude());
        vo.setLongitude(localMonitorPoint == null ? warningResp.getLon() : localMonitorPoint.getLongitude());
        vo.setWarningDeviceName(warningResp.getSbname());
        vo.setWarningLevel(convertFromThirdPartyWarningLevel(warningResp.getYjdj()));
        vo.setWarningPublishTime(warningResp.getYjsj());
        vo.setDisposalType(warningResp.getType());
        vo.setDisposalPerson(warningResp.getClr());
        vo.setWarningDisposalTime(warningResp.getClsj());
        vo.setValidWarning(resolveValidWarningFlag(warningResp));
        return vo;
    }

    /**
     * 将内部预警等级转换为三方等级编码
     */
    String convertToThirdPartyWarningLevel(Integer warningLevel) {
        return LevelCodeUtil.convertNormalizedWarningLevelToThirdPartyCode(warningLevel);
    }

    /**
     * 将三方预警等级转换为内部等级
     */
    Integer convertFromThirdPartyWarningLevel(String warningLevel) {
        return LevelCodeUtil.convertThirdPartyCodeToNormalizedWarningLevel(warningLevel);
    }

    /**
     * 判断预警记录是否满足处置筛选条件
     */
    boolean matchMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, ThirdMonitorWarningHandleDetailResp detailResp, ThirdPartyWarningDisposalVo vo) {
        if (bo.getValidWarning() != null) {
            Integer validWarningFlag = detailResp == null ? vo.getValidWarning() : resolveValidWarningFlag(detailResp);
            if (!bo.getValidWarning().equals(validWarningFlag)) {
                return false;
            }
        }

        if (bo.getDisposalStatus() != null) {
            Integer disposalStatus = detailResp == null ? resolveDisposalStatus(vo.getWarningDisposalTime()) : resolveDisposalStatus(detailResp);
            if (!bo.getDisposalStatus().equals(disposalStatus)) {
                return false;
            }
        }

        return matchDateRange(vo.getWarningDisposalTime(), bo.getWarningDisposalTime());
    }

    /**
     * 解析是否有效预警标识
     */
    Integer resolveValidWarningFlag(ThirdMonitorWarningHandleDetailResp detailResp) {
        if (detailResp == null || detailResp.getSfwb() == null) {
            return null;
        }
        return detailResp.getSfwb() == 1 ? 0 : 1;
    }

    Integer resolveValidWarningFlag(ThirdMonitorDeviceWarningResp warningResp) {
        if (warningResp == null || warningResp.getSfwb() == null) {
            return null;
        }
        return warningResp.getSfwb() == 1 ? 0 : 1;
    }

    /**
     * 解析处置状态
     */
    Integer resolveDisposalStatus(ThirdMonitorWarningHandleDetailResp detailResp) {
        if (detailResp == null) {
            return 0;
        }
        return StrUtil.isNotBlank(detailResp.getClsj()) ? 1 : 0;
    }

    Integer resolveDisposalStatus(String disposalTime) {
        return StrUtil.isNotBlank(disposalTime) ? 1 : 0;
    }

    boolean matchRemoteMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, ThirdPartyWarningDisposalVo vo) {
        if (vo == null) {
            return false;
        }
        if (bo.getValidWarning() != null && !bo.getValidWarning().equals(vo.getValidWarning())) {
            return false;
        }
        if (bo.getDisposalStatus() != null && !bo.getDisposalStatus().equals(resolveDisposalStatus(vo.getWarningDisposalTime()))) {
            return false;
        }
        return matchDateRange(vo.getWarningDisposalTime(), bo.getWarningDisposalTime());
    }

    /**
     * 解析行政区划名称
     */
    String resolveAdministrativeRegion(MonitorPoint localMonitorPoint, String fallbackRegionName) {
        if (localMonitorPoint == null) {
            return fallbackRegionName;
        }
        String regionName = resolveRegionNameByCode(localMonitorPoint.getAdministrativeRegionCode());
        if (StrUtil.isNotBlank(regionName)) {
            return regionName;
        }
        return firstNonBlank(
            localMonitorPoint.getVillage(),
            localMonitorPoint.getStreet(),
            localMonitorPoint.getCounty(),
            localMonitorPoint.getCity(),
            localMonitorPoint.getProvince(),
            fallbackRegionName
        );
    }

    /**
     * 根据行政区划编码查询名称
     */
    String resolveRegionNameByCode(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            return null;
        }
        String cached = adRegionNameCache.computeIfAbsent(regionCode, code -> {
            AdRegionVo adRegionVo = adRegionService.getInfo(code);
            return adRegionVo == null ? "" : StrUtil.nullToDefault(adRegionVo.getName(), "");
        });
        return StrUtil.isBlank(cached) ? null : cached;
    }

    /**
     * 获取第一个非空字符串
     */
    String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 校验单个日期参数格式
     */
    void validateDateParam(String value, String fieldName) {
        if (StrUtil.isNotBlank(value) && parseQueryDate(value) == null) {
            throw new ServiceException(fieldName + "格式错误，需为yyyy-MM-dd");
        }
    }

    /**
     * 判断时间是否命中给定日期
     */
    boolean matchDateRange(String time, String queryDate) {
        if (StrUtil.isBlank(queryDate)) {
            return true;
        }
        LocalDateTime target = parseDateTime(time);
        if (target == null) {
            return false;
        }
        LocalDateTime start = parseStartDateTime(queryDate);
        LocalDateTime end = parseEndDateTime(queryDate);
        if (start != null && target.isBefore(start)) {
            return false;
        }
        return end == null || !target.isAfter(end);
    }

    LocalDateTime resolveRemoteWarningStartTime(ThirdPartyWarningDisposalBo bo, LocalDateTime warningEndTime) {
        LocalDateTime startTime = parseStartDateTime(bo == null ? null : bo.getWarningPublishTime());
        return startTime == null ? resolveDefaultWarningStartTime(warningEndTime) : startTime;
    }

    LocalDateTime resolveRemoteWarningEndTime(ThirdPartyWarningDisposalBo bo) {
        LocalDateTime endTime = parseEndDateTime(bo == null ? null : bo.getWarningPublishTime());
        return endTime == null ? LocalDateTime.now(BEIJING_ZONE_ID) : endTime;
    }

    LocalDateTime resolveDefaultWarningStartTime(LocalDateTime endTime) {
        LocalDateTime baseEndTime = endTime == null ? LocalDateTime.now(BEIJING_ZONE_ID) : endTime;
        return baseEndTime.minus(DEFAULT_WARNING_LOOKBACK_DURATION);
    }

    String formatWarningQueryTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    String resolveMonitorWarningRegionCode(String regionCode, MonitorPoint localMonitorPoint) {
        String rawRegionCode = firstNonBlank(
            regionCode,
            localMonitorPoint == null ? null : localMonitorPoint.getAdministrativeRegionCode(),
            DEFAULT_REGION_CODE
        );
        if (StrUtil.isBlank(rawRegionCode)) {
            return DEFAULT_REGION_CODE;
        }
        return rawRegionCode.length() > 6 ? rawRegionCode.substring(0, 6) : rawRegionCode;
    }

    Map<String, String> loadMonitorDeviceIdByPointAndName(List<ThirdMonitorDeviceWarningResp> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return Map.of();
        }
        ThirdPartyWarningDisposalBo localMappingBo = new ThirdPartyWarningDisposalBo();
        localMappingBo.setRegionCode(DEFAULT_REGION_CODE);
        List<MonitorPoint> localMonitorPoints = queryLocalMonitorPoints(localMappingBo);
        if (localMonitorPoints.isEmpty()) {
            return Map.of();
        }
        Map<String, MonitorPoint> pointById = localBindingDelegate.buildMonitorPointById(localMonitorPoints);
        Map<String, MonitorPoint> pointByName = localBindingDelegate.buildMonitorPointByName(localMonitorPoints);
        Map<String, MonitorPoint> localMonitorPointMap = localBindingDelegate.buildLocalMonitorPointMap(localMonitorPoints);
        Set<String> localPointIds = warnings.stream()
                                            .map(warning -> localBindingDelegate.resolveRemoteLocalMonitorPoint(warning, localMonitorPointMap, pointById, pointByName))
                                            .filter(Objects::nonNull)
                                            .map(MonitorPoint::getId)
                                            .filter(StrUtil::isNotBlank)
                                            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (localPointIds.isEmpty()) {
            return Map.of();
        }
        List<MonitorDevice> devices = localBindingDelegate.queryMonitorDevices(localPointIds, null, null, null);
        Map<String, String> result = buildMonitorDeviceIdByPointAndName(devices);
        Map<String, List<MonitorDevice>> devicesByPointId = buildMonitorDevicesByPointId(devices);
        for (ThirdMonitorDeviceWarningResp warning : warnings) {
            MonitorPoint localMonitorPoint = localBindingDelegate.resolveRemoteLocalMonitorPoint(warning, localMonitorPointMap, pointById, pointByName);
            if (localMonitorPoint == null) {
                continue;
            }
            List<MonitorDevice> pointDevices = devicesByPointId.get(localMonitorPoint.getId());
            if (pointDevices == null || pointDevices.isEmpty()) {
                continue;
            }
            for (MonitorDevice device : pointDevices) {
                if (device == null || StrUtil.isBlank(device.getId()) || StrUtil.isBlank(device.getDeviceName())) {
                    continue;
                }
                if (StrUtil.isNotBlank(warning.getJcdid())) {
                    result.putIfAbsent(buildWarningDeviceLookupKey(warning.getJcdid(), device.getDeviceName()), device.getId());
                }
                if (StrUtil.isNotBlank(warning.getJcdbh())) {
                    result.putIfAbsent(buildWarningDeviceLookupKey(warning.getJcdbh(), device.getDeviceName()), device.getId());
                }
            }
        }
        return result;
    }

    Map<String, String> buildMonitorDeviceIdByPointAndName(Collection<MonitorDevice> devices) {
        Map<String, String> result = new HashMap<>();
        for (MonitorDevice device : devices) {
            if (device == null || StrUtil.isBlank(device.getMonitoringPointId()) || StrUtil.isBlank(device.getDeviceName()) || StrUtil.isBlank(device.getId())) {
                continue;
            }
            result.putIfAbsent(buildWarningDeviceLookupKey(device.getMonitoringPointId(), device.getDeviceName()), device.getId());
        }
        return result;
    }

    Map<String, List<MonitorDevice>> buildMonitorDevicesByPointId(Collection<MonitorDevice> devices) {
        Map<String, List<MonitorDevice>> result = new HashMap<>();
        if (devices == null || devices.isEmpty()) {
            return result;
        }
        for (MonitorDevice device : devices) {
            if (device == null || StrUtil.isBlank(device.getMonitoringPointId()) || StrUtil.isBlank(device.getId())) {
                continue;
            }
            result.computeIfAbsent(device.getMonitoringPointId(), key -> new ArrayList<>()).add(device);
        }
        return result;
    }

    MonitorDevice resolveWarningDevice(
        ThirdMonitorDeviceWarningResp warningResp,
        MonitorPoint localMonitorPoint,
        Map<String, MonitorDevice> deviceById,
        Map<String, String> localDeviceIdByPointAndName,
        Map<String, List<MonitorDevice>> devicesByPointId
    ) {
        String deviceId = resolveWarningDeviceId(warningResp, localMonitorPoint, localDeviceIdByPointAndName, devicesByPointId);
        return StrUtil.isBlank(deviceId) ? null : deviceById.get(deviceId);
    }

    String resolveWarningDeviceId(
        ThirdMonitorDeviceWarningResp warningResp,
        MonitorPoint localMonitorPoint,
        Map<String, String> localDeviceIdByPointAndName,
        Map<String, List<MonitorDevice>> devicesByPointId
    ) {
        if (warningResp == null) {
            return null;
        }
        if (StrUtil.isNotBlank(warningResp.getSbid())) {
            return warningResp.getSbid();
        }
        if (localMonitorPoint != null && localDeviceIdByPointAndName != null && !localDeviceIdByPointAndName.isEmpty()) {
            String localDeviceId = localDeviceIdByPointAndName.get(buildWarningDeviceLookupKey(localMonitorPoint.getId(), warningResp.getSbname()));
            if (StrUtil.isNotBlank(localDeviceId)) {
                return localDeviceId;
            }
        }
        if (localDeviceIdByPointAndName != null && !localDeviceIdByPointAndName.isEmpty()) {
            String remotePointDeviceId = localDeviceIdByPointAndName.get(buildWarningDeviceLookupKey(warningResp.getJcdid(), warningResp.getSbname()));
            if (StrUtil.isNotBlank(remotePointDeviceId)) {
                return remotePointDeviceId;
            }
        }
        if (localMonitorPoint != null && devicesByPointId != null) {
            List<MonitorDevice> pointDevices = devicesByPointId.get(localMonitorPoint.getId());
            if (pointDevices != null && pointDevices.size() == 1) {
                return pointDevices.getFirst().getId();
            }
        }
        return null;
    }

    String resolveWarningDeviceId(ThirdMonitorDeviceWarningResp warningResp, Map<String, String> localDeviceIdByPointAndName) {
        return resolveWarningDeviceId(warningResp, null, localDeviceIdByPointAndName, null);
    }

    String buildWarningDeviceLookupKey(String monitorPointId, String deviceName) {
        return StrUtil.blankToDefault(monitorPointId, "") + "#" + StrUtil.blankToDefault(StrUtil.trim(deviceName), "");
    }

    /**
     * 解析通用时间字符串
     */
    LocalDateTime parseDateTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignore) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 解析开始时间
     */
    LocalDateTime parseStartDateTime(String value) {
        LocalDate date = parseQueryDate(value);
        return date == null ? null : date.atStartOfDay();
    }

    /**
     * 解析结束时间
     */
    LocalDateTime parseEndDateTime(String value) {
        LocalDate date = parseQueryDate(value);
        return date == null ? null : date.atTime(23, 59, 59);
    }

    /**
     * 解析查询日期参数
     */
    LocalDate parseQueryDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }


}
