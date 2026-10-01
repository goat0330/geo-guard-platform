/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DataAlarmBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataAlarmReportAnalysisReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingGroupResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingStatusBatchUpdateResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.WeatherAlarmStatsResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.event.DataAlarmCreatedEvent;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataAlarmMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 气象预警数据服务实现
 *
 * @author whai
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DataAlarmServiceImpl implements IDataAlarmService {

    public static final int SOURCE_TYPE_MANUAL = 1;
    public static final int SOURCE_TYPE_REPORT_ANALYSIS = 2;
    private static final String MANUAL_ALARM_CODE_PREFIX = "RG";
    private static final String MANUAL_ALARM_CODE_DATE_PATTERN = "yyyyMMdd";
    private static final int MANUAL_ALARM_CODE_RANDOM_LENGTH = 8;
    private static final int MANUAL_ALARM_CODE_GENERATE_MAX_RETRY = 10;
    private static final BigDecimal SQUARE_METERS_PER_SQUARE_KILOMETER = BigDecimal.valueOf(1_000_000L);
    private static final int AREA_SCALE = 2;
    private static final int ALARM_STATUS_UNTRIGGERED = 0;
    private static final int ALARM_STATUS_TRIGGERED = 1;
    private static final int ALARM_STATUS_EXPIRED = 2;
    private static final int ALARM_PENDING_STATUS_NONE = 0;
    private static final int ALARM_PENDING_STATUS_WAITING = 1;
    private static final int ALARM_PENDING_STATUS_TRIGGERED = 2;
    private static final String FILTER_REASON_ONLY_UNTRIGGERED_CAN_BE_MODIFIED = "只能修改未触发的预警数据";
    private static final String ENSHI_CITY = "恩施市";
    private static final String OTHER_STREET_NAME = "其他乡镇";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static final List<Integer> REPORT_ANALYSIS_LEVEL_PARSE_ORDER = List.of(4, 3, 2, 1);
    private static final String REPORT_ANALYSIS_SUFFIX = "请相关地方政府及有关部门按照《湖北省地质灾害防范规程》和预警响应工作方案做好地质灾害防范工作。"
        + "请以上地区自然资源部门及时逐级向上级自然资源部门反馈相关情况。"
        + "请蓝色预警区域的相关地区自然资源主管部门和技术支撑单位组织做好日常防范工作，密切关注气象预警信息，及时掌握地质灾害预警等级的动态变化。";

    private final DataAlarmMapper baseMapper;
    private final ISlopeUnitService slopeUnitService;
    private final IAdRegionService adRegionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DataAlarmVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<DataAlarmVo> queryPageList(DataAlarmBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DataAlarm> lqw = buildQueryWrapper(bo);
        Page<DataAlarmVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DataAlarmVo> queryList(DataAlarmBo bo) {
        LambdaQueryWrapper<DataAlarm> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<DataAlarmVo> listMatchByData(Date data) {
        if (data == null) {
            throw new ServiceException("data不能为空");
        }
        List<DataAlarm> triggeredAlarms = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                                        .eq(DataAlarm::getStatus, ALARM_STATUS_TRIGGERED));
        if (triggeredAlarms.size() > 1) {
            throw new ServiceException("status=1 的记录存在多条，无法确定起点记录");
        }

        List<DataAlarmVo> result = queryByValidEndDate(data);
        if (!triggeredAlarms.isEmpty()) {
            DataAlarm triggeredAlarm = triggeredAlarms.get(0);
            if (isTriggeredAlarmExpired(triggeredAlarm)) {
                if (triggeredAlarm.getValidStartDate() == null) {
                    throw new ServiceException("status=1 的起点记录 validStartDate 为空，无法查询后续记录");
                }
                result = baseMapper.selectVoList(Wrappers.<DataAlarm>lambdaQuery()
                                                         .ge(DataAlarm::getValidStartDate, triggeredAlarm.getValidStartDate())
                                                         .orderByAsc(DataAlarm::getValidStartDate)
                                                         .orderByAsc(DataAlarm::getValidEndDate)
                                                         .orderByAsc(DataAlarm::getCreateDate)
                );
            }
        }
        markLatestCreateDateFlag(result);
        return result;
    }

    private List<DataAlarmVo> queryByValidEndDate(Date data) {
        return baseMapper.selectVoList(Wrappers.<DataAlarm>lambdaQuery()
                                               .ge(DataAlarm::getValidEndDate, data)
                                               .orderByAsc(DataAlarm::getValidStartDate)
                                               .orderByAsc(DataAlarm::getValidEndDate)
                                               .orderByAsc(DataAlarm::getCreateDate));
    }

    private boolean isTriggeredAlarmExpired(DataAlarm triggeredAlarm) {
        return triggeredAlarm != null
            && triggeredAlarm.getValidEndDate() != null
            && new Date().after(triggeredAlarm.getValidEndDate());
    }

    private void markLatestCreateDateFlag(List<DataAlarmVo> alarms) {
        if (ObjUtil.isEmpty(alarms)) {
            return;
        }
        Date latestCreateDate = alarms.stream()
                                      .map(DataAlarmVo::getCreateDate)
                                      .filter(Objects::nonNull)
                                      .max(Date::compareTo)
                                      .orElse(null);
        alarms.forEach(alarm -> alarm.setLatestCreateDateFlag(
            latestCreateDate != null && Objects.equals(latestCreateDate, alarm.getCreateDate()) ? 1 : 0
        ));
    }

    @Override
    public List<DataAlarmPendingGroupResp> queryPendingGroupList() {
        List<DataAlarm> pendingAlarms = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                                      .eq(DataAlarm::getStatus, ALARM_STATUS_UNTRIGGERED)
                                                                      .eq(DataAlarm::getPendingStatus, 1)
                                                                      .orderByAsc(DataAlarm::getTime)
                                                                      .orderByAsc(DataAlarm::getSource)
                                                                      .orderByAsc(DataAlarm::getCreateDate));
        if (ObjUtil.isEmpty(pendingAlarms)) {
            return Collections.emptyList();
        }

        Map<Date, List<DataAlarm>> groupedMap = pendingAlarms.stream()
                                                             .collect(Collectors.groupingBy(DataAlarm::getTime, LinkedHashMap::new, Collectors.toList()));

        List<DataAlarmPendingGroupResp> result = new ArrayList<>(groupedMap.size());
        groupedMap.forEach((time, alarms) -> {
            DataAlarmPendingGroupResp resp = new DataAlarmPendingGroupResp();
            resp.setTime(time);
            List<DataAlarmPendingGroupResp.Item> items = alarms.stream().map(this::toPendingGroupItem).toList();
            resp.setSource(items);
            result.add(resp);
        });
        return result;
    }

    private LambdaQueryWrapper<DataAlarm> buildQueryWrapper(DataAlarmBo bo) {
        LambdaQueryWrapper<DataAlarm> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, DataAlarm::getId, bo.getId());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), DataAlarm::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), DataAlarm::getCity, bo.getCity());
        lqw.like(StringUtils.isNotBlank(bo.getLevelStreetsJson()), DataAlarm::getLevelStreetsJson, bo.getLevelStreetsJson());
        lqw.eq(bo.getTime() != null, DataAlarm::getTime, bo.getTime());
        lqw.eq(bo.getPublishDate() != null, DataAlarm::getPublishDate, bo.getPublishDate());
        lqw.eq(bo.getValidStartDate() != null, DataAlarm::getValidStartDate, bo.getValidStartDate());
        lqw.eq(bo.getValidEndDate() != null, DataAlarm::getValidEndDate, bo.getValidEndDate());
        if (Boolean.TRUE.equals(bo.getOnlyNotExpired())) {
            lqw.ge(DataAlarm::getValidEndDate, new Date());
        } else if (Boolean.FALSE.equals(bo.getOnlyNotExpired())) {
            lqw.lt(DataAlarm::getValidEndDate, new Date());
        }
        lqw.eq(bo.getStatus() != null, DataAlarm::getStatus, bo.getStatus());
        lqw.eq(bo.getPendingStatus() != null, DataAlarm::getPendingStatus, bo.getPendingStatus());
        lqw.eq(bo.getCreateDate() != null, DataAlarm::getCreateDate, bo.getCreateDate());
        lqw.eq(StringUtils.isNotBlank(bo.getSource()), DataAlarm::getSource, bo.getSource());
        lqw.eq(bo.getSourceType() != null, DataAlarm::getSourceType, bo.getSourceType());
        lqw.like(StringUtils.isNotBlank(bo.getMessage()), DataAlarm::getMessage, bo.getMessage());
        lqw.orderByDesc(DataAlarm::getCreateDate);
        return lqw;
    }

    @Override
    public Boolean insertByBo(DataAlarmBo bo) {
        DataAlarm add = MapstructUtils.convert(bo, DataAlarm.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            publishDataAlarmCreated(add.getId());
        }
        return flag;
    }

    @Override
    public Long insertManualByBo(DataAlarmBo bo) {
        if (bo == null) {
            throw new ServiceException("请求体不能为空");
        }
        validateManualRequiredFields(bo);
        if (bo.getId() == null) {
            bo.setId(IdUtil.getSnowflakeNextId());
        }
        bo.setCode(generateManualAlarmCode());
        bo.setTime(new Date(DateUtil.beginOfDay(bo.getPublishDate()).getTime()));
        if (bo.getCreateDate() == null) {
            bo.setCreateDate(new Date());
        }
        if (bo.getStatus() == null) {
            bo.setStatus(ALARM_STATUS_UNTRIGGERED);
        }
        if (bo.getPendingStatus() == null) {
            bo.setPendingStatus(Objects.equals(bo.getStatus(), ALARM_STATUS_TRIGGERED)
                ? ALARM_PENDING_STATUS_TRIGGERED
                : ALARM_PENDING_STATUS_WAITING);
        }
        if (Objects.equals(bo.getStatus(), ALARM_STATUS_TRIGGERED)) {
            bo.setPendingStatus(ALARM_PENDING_STATUS_TRIGGERED);
        } else if (Objects.equals(bo.getPendingStatus(), ALARM_PENDING_STATUS_TRIGGERED)) {
            bo.setStatus(ALARM_STATUS_TRIGGERED);
        }
        if (StringUtils.isBlank(bo.getSource())) {
            bo.setSource("人工补录");
        }
        if (bo.getSourceType() == null) {
            bo.setSourceType(SOURCE_TYPE_MANUAL);
        }
        normalizeManualLevelStreetsJson(bo);
        if (!insertByBo(bo)) {
            throw new ServiceException("人工补录警报失败");
        }
        return bo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateManualByBo(DataAlarmBo bo) {
        if (bo == null) {
            throw new ServiceException("请求体不能为空");
        }
        if (bo.getId() == null) {
            throw new ServiceException("id不能为空");
        }
        DataAlarm existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("预警数据不存在");
        }
        validateManualRequiredFields(bo);
        bo.setCode(existing.getCode());
        bo.setSessionId(existing.getSessionId());
        bo.setTime(new Date(DateUtil.beginOfDay(bo.getPublishDate()).getTime()));
        bo.setCreateDate(existing.getCreateDate());
        bo.setUpdateDate(new Date());
        if (bo.getStatus() == null) {
            bo.setStatus(ALARM_STATUS_UNTRIGGERED);
        }
        if (bo.getPendingStatus() == null) {
            bo.setPendingStatus(Objects.equals(bo.getStatus(), ALARM_STATUS_TRIGGERED)
                ? ALARM_PENDING_STATUS_TRIGGERED
                : ALARM_PENDING_STATUS_WAITING);
        }
        if (Objects.equals(bo.getStatus(), ALARM_STATUS_TRIGGERED)) {
            bo.setPendingStatus(ALARM_PENDING_STATUS_TRIGGERED);
        } else if (Objects.equals(bo.getPendingStatus(), ALARM_PENDING_STATUS_TRIGGERED)) {
            bo.setStatus(ALARM_STATUS_TRIGGERED);
        }
        if (StringUtils.isBlank(bo.getSource())) {
            bo.setSource(existing.getSource());
        }
        if (bo.getSourceType() == null) {
            bo.setSourceType(existing.getSourceType());
        }
        normalizeManualLevelStreetsJson(bo);
        expireEarlierAlarmsIfResetToUntriggeredNone(existing, bo);
        if (!updateByBo(bo)) {
            throw new ServiceException("修改人工补录警报失败");
        }
        return bo.getId();
    }

    private void expireEarlierAlarmsIfResetToUntriggeredNone(DataAlarm existing, DataAlarmBo bo) {
        if (existing == null || bo == null || bo.getValidStartDate() == null) {
            return;
        }
        boolean triggerStatusModified = !Objects.equals(existing.getStatus(), bo.getStatus())
            || !Objects.equals(existing.getPendingStatus(), bo.getPendingStatus());
        if (!triggerStatusModified
            || !Objects.equals(existing.getStatus(), ALARM_STATUS_UNTRIGGERED)
            || !Objects.equals(existing.getPendingStatus(), ALARM_PENDING_STATUS_NONE)) {
            return;
        }
        LambdaUpdateWrapper<DataAlarm> updateWrapper = Wrappers.<DataAlarm>lambdaUpdate()
                                                               .lt(DataAlarm::getValidStartDate, bo.getValidStartDate())
                                                               .ne(DataAlarm::getId, bo.getId())
                                                               .set(DataAlarm::getStatus, ALARM_STATUS_EXPIRED)
                                                               .set(DataAlarm::getPendingStatus, ALARM_PENDING_STATUS_NONE)
                                                               .set(DataAlarm::getUpdateDate, new Date());
        baseMapper.update(null, updateWrapper);
    }

    private void validateManualRequiredFields(DataAlarmBo bo) {
        if (bo.getPublishDate() == null) {
            throw new ServiceException("publishDate不能为空");
        }
        if (bo.getValidStartDate() == null) {
            throw new ServiceException("validStartDate不能为空");
        }
        if (bo.getValidEndDate() == null) {
            throw new ServiceException("validEndDate不能为空");
        }
        if (bo.getValidStartDate().after(bo.getValidEndDate())) {
            throw new ServiceException("validStartDate不能晚于validEndDate");
        }
        if (StringUtils.isBlank(bo.getCity())) {
            throw new ServiceException("city不能为空");
        }
        Map<Integer, LinkedHashSet<String>> levelStreetsMap = parseLevelStreetsJson(bo.getLevelStreetsJson());
        if (levelStreetsMap.isEmpty()) {
            throw new ServiceException("levelStreetsJson不能为空");
        }
        boolean hasInvalidLevel = levelStreetsMap.keySet().stream()
                                                 .anyMatch(level -> level == null || level < 1 || level > 4);
        if (hasInvalidLevel) {
            throw new ServiceException("levelStreetsJson中的level必须在1-4之间");
        }
    }

    private String generateManualAlarmCode() {
        String datePart = DateUtil.format(new Date(), MANUAL_ALARM_CODE_DATE_PATTERN);
        for (int i = 0; i < MANUAL_ALARM_CODE_GENERATE_MAX_RETRY; i++) {
            String code = MANUAL_ALARM_CODE_PREFIX + datePart + RandomUtil.randomNumbers(MANUAL_ALARM_CODE_RANDOM_LENGTH);
            Long exists = baseMapper.selectCount(Wrappers.<DataAlarm>lambdaQuery().eq(DataAlarm::getCode, code));
            if (exists == null || exists == 0L) {
                return code;
            }
        }
        throw new ServiceException("生成预警编号失败，请重试");
    }

    @Override
    public Boolean insertBatchByBo(List<DataAlarmBo> boList) {
        if (ObjUtil.isEmpty(boList)) {
            return false;
        }
        List<DataAlarm> entities = boList.stream()
                                         .map(bo -> MapstructUtils.convert(bo, DataAlarm.class))
                                         .peek(this::validEntityBeforeSave)
                                         .toList();
        boolean inserted = baseMapper.insertBatch(entities);
        if (inserted) {
            publishDataAlarmCreated(entities);
        }
        return inserted;
    }

    private Boolean insertReportAnalysisBatch(List<DataAlarmBo> boList) {
        if (ObjUtil.isEmpty(boList)) {
            return false;
        }
        List<DataAlarm> entities = boList.stream()
                                         .map(bo -> MapstructUtils.convert(bo, DataAlarm.class))
                                         .peek(this::validEntityBeforeSave)
                                         .toList();
        boolean inserted = baseMapper.insertBatch(entities);
        if (inserted) {
            publishDataAlarmCreated(entities);
        }
        return inserted;
    }

    private void publishDataAlarmCreated(Long alarmId) {
        if (alarmId != null) {
            eventPublisher.publishEvent(new DataAlarmCreatedEvent(List.of(alarmId)));
        }
    }

    private void publishDataAlarmCreated(List<DataAlarm> alarms) {
        if (ObjUtil.isEmpty(alarms)) {
            return;
        }
        List<Long> alarmIds = alarms.stream()
                                    .map(DataAlarm::getId)
                                    .filter(Objects::nonNull)
                                    .toList();
        if (!alarmIds.isEmpty()) {
            eventPublisher.publishEvent(new DataAlarmCreatedEvent(alarmIds));
        }
    }

    @Override
    public Boolean updateByBo(DataAlarmBo bo) {
        DataAlarm update = MapstructUtils.convert(bo, DataAlarm.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean updateTriggerStatusById(Long id, Integer status, Integer pendingStatus, Date updateDate) {
        if (id == null || status == null || pendingStatus == null) {
            return false;
        }
        LambdaUpdateWrapper<DataAlarm> updateWrapper = Wrappers.<DataAlarm>lambdaUpdate()
                                                               .eq(DataAlarm::getId, id)
                                                               .set(DataAlarm::getStatus, status)
                                                               .set(DataAlarm::getPendingStatus, pendingStatus)
                                                               .set(updateDate != null, DataAlarm::getUpdateDate, updateDate);
        return baseMapper.update(null, updateWrapper) > 0;
    }

    @Override
    public DataAlarmPendingStatusBatchUpdateResp batchUpdatePendingStatus(List<String> ids, Integer pendingStatus) {
        if (ObjUtil.isEmpty(ids)) {
            throw new ServiceException("ids不能为空");
        }
        if (pendingStatus == null) {
            throw new ServiceException("pending_status不能为空");
        }
        if (!isValidPendingStatus(pendingStatus)) {
            throw new ServiceException("pending_status只能为0/1/2");
        }
        List<Long> alarmIds = ids.stream()
                                 .filter(StringUtils::isNotBlank)
                                 .map(String::trim)
                                 .distinct()
                                 .map(id -> {
                                     try {
                                         return Long.parseLong(id);
                                     } catch (NumberFormatException e) {
                                         throw new ServiceException("id格式错误，必须为数字: " + id);
                                     }
                                 })
                                 .toList();
        if (alarmIds.isEmpty()) {
            throw new ServiceException("ids不能为空");
        }

        List<DataAlarm> existingAlarms = baseMapper.selectByIds(alarmIds);
        if (existingAlarms.size() != alarmIds.size()) {
            Set<Long> existingIdSet = existingAlarms.stream().map(DataAlarm::getId).collect(Collectors.toSet());
            List<String> missingIds = alarmIds.stream()
                                              .filter(id -> !existingIdSet.contains(id))
                                              .map(String::valueOf)
                                              .toList();
            throw new ServiceException("以下预警数据不存在: " + String.join(",", missingIds));
        }

        List<Long> updatableIds = existingAlarms.stream()
                                                .filter(alarm -> Objects.equals(alarm.getStatus(), ALARM_STATUS_UNTRIGGERED))
                                                .map(DataAlarm::getId)
                                                .toList();
        List<DataAlarmPendingStatusBatchUpdateResp.FilteredItem> filteredItems = existingAlarms.stream()
                                                                                               .filter(alarm -> !Objects.equals(alarm.getStatus(), ALARM_STATUS_UNTRIGGERED))
                                                                                               .map(alarm -> buildFilteredItem(alarm.getId(),
                                                                                                   FILTER_REASON_ONLY_UNTRIGGERED_CAN_BE_MODIFIED))
                                                                                               .toList();

        if (!updatableIds.isEmpty()) {
            LambdaUpdateWrapper<DataAlarm> updateWrapper = Wrappers.<DataAlarm>lambdaUpdate()
                                                                   .in(DataAlarm::getId, updatableIds)
                                                                   .set(DataAlarm::getPendingStatus, pendingStatus)
                                                                   .set(DataAlarm::getStatus, resolveStatusByPendingStatus(pendingStatus));
            baseMapper.update(null, updateWrapper);
        }

        DataAlarmPendingStatusBatchUpdateResp resp = new DataAlarmPendingStatusBatchUpdateResp();
        resp.setUpdatedIds(updatableIds);
        resp.setFilteredItems(filteredItems);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> importReportAnalysis(DataAlarmReportAnalysisReq req) {
        String sessionId = null;
        String requestCode = null;
        if (req == null) {
            throw new ServiceException("请求体不能为空");
        }
        if (StringUtils.isBlank(req.getId())) {
            throw new ServiceException("id不能为空");
        }
        if (StringUtils.isBlank(req.getMessageId())) {
            throw new ServiceException("message_id不能为空");
        }
        if (ObjUtil.isEmpty(req.getData())) {
            throw new ServiceException("data不能为空");
        }
        sessionId = req.getId().trim();
        requestCode = req.getMessageId().trim();

        List<String> allEnshiStreets = queryAllEnshiStreets();
        Date now = new Date();
        Map<String, Set<String>> usedStreetsByWindowAndLevel = new HashMap<>();
        Map<Long, ReportMessageContext> reportMessageContextMap = new LinkedHashMap<>();
        Map<Long, DailyReportAlarmDraft> dailyDraftMap = new LinkedHashMap<>();
        String source = "州气象局,州自然资源和城乡建设局,省地质局第二地质大队联合发布";

        for (DataAlarmReportAnalysisReq.AlarmItem item : req.getData()) {
            if (item == null || ObjUtil.isEmpty(item.getData())) {
                continue;
            }

            Date time = parseRequiredDate(item.getTime(), "time", DATE_PATTERN);
            Date publishDate = parseRequiredDate(item.getPublishDate(), "publish_date", DATE_TIME_PATTERN);
            Date validStartDate = parseRequiredDate(item.getValidStartDate(), "valid_start_date", DATE_TIME_PATTERN);
            Date validEndDate = parseRequiredDate(item.getValidEndDate(), "valid_end_date", DATE_TIME_PATTERN);
            if (validStartDate.after(validEndDate)) {
                throw new ServiceException("valid_start_date不能晚于valid_end_date");
            }
            ReportMessageContext reportMessageContext = reportMessageContextMap.computeIfAbsent(time.getTime(),
                key -> new ReportMessageContext(validStartDate, validEndDate));
            reportMessageContext.mergeWindow(validStartDate, validEndDate);
            DailyReportAlarmDraft dailyDraft = dailyDraftMap.computeIfAbsent(time.getTime(), key ->
                new DailyReportAlarmDraft(time, publishDate, validStartDate, validEndDate));
            dailyDraft.mergeWindow(publishDate, validStartDate, validEndDate);

            for (Integer level : REPORT_ANALYSIS_LEVEL_PARSE_ORDER) {
                DataAlarmReportAnalysisReq.AlarmLevelItem levelItem = item.getData().get(resolveLevelKey(level));
                if (levelItem == null) {
                    continue;
                }

                Set<String> usedStreetSet = getUsedStreetSet(validStartDate, validEndDate, level, usedStreetsByWindowAndLevel, dailyDraft);
                Set<String> resolvedStreets = resolveStreets(levelItem, allEnshiStreets, usedStreetSet);
                if (resolvedStreets.isEmpty()) {
                    continue;
                }
                reportMessageContext.addStreets(level, resolvedStreets);
                dailyDraft.addStreets(level, resolvedStreets);
            }
        }

        if (dailyDraftMap.isEmpty()) {
            return Collections.emptyList();
        }

        String finalRequestCode = requestCode;
        String finalSessionId = sessionId;
        Map<Long, String> messageByTime = reportMessageContextMap.entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                                     entry -> buildReportAnalysisMessage(entry.getValue()),
                                                                     (left, right) -> left,
                                                                     LinkedHashMap::new));
        List<DataAlarmBo> candidateAlarms = dailyDraftMap.values().stream()
                                                         .filter(draft -> !draft.levelStreets.isEmpty())
                                                         .map(draft -> buildReportAnalysisAlarmBo(draft, finalRequestCode, finalSessionId, source, now, resolveKeyTips(req)))
                                                         .toList();
        candidateAlarms.forEach(bo -> bo.setMessage(resolveMessageByTime(messageByTime, bo.getTime())));

        Set<String> existingKeys = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                                 .eq(DataAlarm::getCode, requestCode))
                                             .stream()
                                             .map(this::buildAlarmUniqueKey)
                                             .collect(Collectors.toSet());
        List<DataAlarmBo> toInsert = candidateAlarms.stream()
                                                    .filter(bo -> !existingKeys.contains(buildAlarmUniqueKey(bo)))
                                                    .toList();
        if (toInsert.isEmpty()) {
            return Collections.emptyList();
        }

        if (!insertReportAnalysisBatch(toInsert)) {
            throw new ServiceException("保存预警数据失败");
        }
        return toInsert.stream().map(DataAlarmBo::getId).toList();
    }

    private String resolveKeyTips(DataAlarmReportAnalysisReq req) {
        return req != null && StringUtils.isNotBlank(req.getKeyTips()) ? req.getKeyTips().trim() : null;
    }

    @Override
    public int closeExpiredUntriggeredAlarms(Date now) {
        if (now == null) {
            return 0;
        }
        LambdaUpdateWrapper<DataAlarm> updateWrapper = Wrappers.<DataAlarm>lambdaUpdate()
                                                               .eq(DataAlarm::getStatus, ALARM_STATUS_UNTRIGGERED)
                                                               .lt(DataAlarm::getValidEndDate, now)
                                                               .set(DataAlarm::getStatus, ALARM_STATUS_EXPIRED);
        return baseMapper.update(null, updateWrapper);
    }

    private Integer resolveStatusByPendingStatus(Integer pendingStatus) {
        if (Objects.equals(pendingStatus, ALARM_PENDING_STATUS_TRIGGERED)) {
            return ALARM_STATUS_TRIGGERED;
        }
        return ALARM_STATUS_UNTRIGGERED;
    }

    private boolean isValidPendingStatus(Integer pendingStatus) {
        return Objects.equals(pendingStatus, ALARM_PENDING_STATUS_NONE)
            || Objects.equals(pendingStatus, ALARM_PENDING_STATUS_WAITING)
            || Objects.equals(pendingStatus, ALARM_PENDING_STATUS_TRIGGERED);
    }

    private DataAlarmPendingStatusBatchUpdateResp.FilteredItem buildFilteredItem(Long id, String reason) {
        DataAlarmPendingStatusBatchUpdateResp.FilteredItem item = new DataAlarmPendingStatusBatchUpdateResp.FilteredItem();
        item.setId(id);
        item.setReason(reason);
        return item;
    }

    private String resolveMessageByTime(Map<Long, String> messageByTime, Date time) {
        if (time == null || messageByTime.isEmpty()) {
            return null;
        }
        return messageByTime.get(time.getTime());
    }

    private String resolveLevelKey(Integer level) {
        return switch (level) {
            case 4 -> "red_level";
            case 3 -> "orange_level";
            case 2 -> "yellow_level";
            case 1 -> "blue_level";
            default -> null;
        };
    }

    @Override
    public Set<String> getExistingCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<DataAlarm> lqw = Wrappers.lambdaQuery();
        lqw.in(DataAlarm::getCode, codes);
        lqw.select(DataAlarm::getCode);
        return baseMapper.selectList(lqw).stream()
                         .map(DataAlarm::getCode)
                         .filter(StringUtils::isNotBlank)
                         .collect(Collectors.toSet());
    }

    @Override
    public List<String> listCodesBySessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                             .eq(DataAlarm::getSessionId, sessionId.trim())
                                             .select(DataAlarm::getCode)
                                             .orderByDesc(DataAlarm::getCreateDate)
                                             .orderByDesc(DataAlarm::getId))
                         .stream()
                         .map(DataAlarm::getCode)
                         .filter(StringUtils::isNotBlank)
                         .distinct()
                         .toList();
    }

    @Override
    public WeatherAlarmStatsResp getWeatherAlarmStats(Long id) {
        WeatherAlarmStatsResp resp = buildEmptyWeatherAlarmStatsResp();

        DataAlarm dataAlarm = baseMapper.selectById(id);
        if (ObjUtil.isEmpty(dataAlarm)) {
            return resp;
        }
        List<String> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(dataAlarm.getMessage())) {
            messages.add(dataAlarm.getMessage());
        }
        List<String> streets = extractStreetList(dataAlarm.getLevelStreetsJson());
        resp.setCreateDate(dataAlarm.getCreateDate());
        resp.setStreetCount(streets.size());
        resp.setMessages(messages);
        resp.setAdRegionVoList(buildAdRegionVoListByStreetLevels(buildStreetHighestLevelMap(dataAlarm.getLevelStreetsJson())));
        if (streets.isEmpty()) {
            return resp;
        }

        List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(streets);
        if (ObjUtil.isEmpty(slopeUnits)) {
            return resp;
        }

        resp.setSlopeUnitCount(slopeUnits.size());
        BigDecimal totalArea = calculateAlarmAreaByAdRegion(streets);
        resp.setTotalArea(totalArea);
        return resp;
    }

    @Override
    public WeatherAlarmStatsResp latest() {
        WeatherAlarmStatsResp resp = buildEmptyWeatherAlarmStatsResp();

        DataAlarm latestAlarm = baseMapper.selectOne(Wrappers.<DataAlarm>lambdaQuery()
                                                             .orderByDesc(DataAlarm::getCreateDate)
                                                             .last("limit 1"));
        if (ObjUtil.isEmpty(latestAlarm) || latestAlarm.getCreateDate() == null) {
            return resp;
        }
        Date todayStart = DateUtil.beginOfDay(new Date());
        if (latestAlarm.getCreateDate().before(todayStart)) {
            return resp;
        }
        List<DataAlarm> sameTimeAlarms = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                                       .eq(DataAlarm::getCreateDate, latestAlarm.getCreateDate()));
        if (ObjUtil.isEmpty(sameTimeAlarms)) {
            return resp;
        }

        BigDecimal totalArea = BigDecimal.ZERO;
        int totalStreetCount = 0;
        int totalSlopeUnitCount = 0;
        List<String> messages = new ArrayList<>();
        Map<String, AdRegionVo> adRegionMap = new LinkedHashMap<>();
        for (DataAlarm alarm : sameTimeAlarms) {
            if (StringUtils.isNotBlank(alarm.getMessage())) {
                messages.add(alarm.getMessage());
            }

            Map<String, Integer> streetLevelMap = buildStreetHighestLevelMap(alarm.getLevelStreetsJson());
            if (streetLevelMap.isEmpty()) {
                continue;
            }
            for (AdRegionVo adRegionVo : buildAdRegionVoListByStreetLevels(streetLevelMap)) {
                String key = StringUtils.defaultString(adRegionVo.getPcode()) + "_" + StringUtils.defaultString(adRegionVo.getName())
                    + "_" + StringUtils.defaultString(adRegionVo.getStreet());
                AdRegionVo exist = adRegionMap.get(key);
                if (exist == null) {
                    adRegionMap.put(key, adRegionVo);
                } else {
                    exist.setEventLevel(mergeHigherRiskLevel(exist.getEventLevel(), adRegionVo.getEventLevel()));
                }
            }
            List<String> streets = new ArrayList<>(streetLevelMap.keySet());
            List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(streets);
            if (ObjUtil.isEmpty(slopeUnits)) {
                continue;
            }
            totalStreetCount += streets.size();
            totalSlopeUnitCount += slopeUnits.size();
            totalArea = totalArea.add(calculateAlarmAreaByAdRegion(streets));
        }
        resp.setCreateDate(latestAlarm.getCreateDate());
        resp.setMessages(messages);
        resp.setStreetCount(totalStreetCount);
        resp.setSlopeUnitCount(totalSlopeUnitCount);
        resp.setTotalArea(totalArea);
        resp.setAdRegionVoList(new ArrayList<>(adRegionMap.values()));
        return resp;
    }

    @Override
    public WeatherAlarmStatsResp todayStats() {
        WeatherAlarmStatsResp resp = buildEmptyWeatherAlarmStatsResp();
        Date todayStart = DateUtil.beginOfDay(new Date());
        Date tomorrowStart = DateUtil.offsetDay(todayStart, 1);

        List<DataAlarm> todayAlarms = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                                    .ge(DataAlarm::getCreateDate, todayStart)
                                                                    .lt(DataAlarm::getCreateDate, tomorrowStart)
                                                                    .orderByDesc(DataAlarm::getCreateDate));
        if (ObjUtil.isEmpty(todayAlarms)) {
            return resp;
        }
        Set<String> allStreetSet = new LinkedHashSet<>();
        Set<String> messageSet = new LinkedHashSet<>();
        Map<String, AdRegionVo> adRegionMap = new LinkedHashMap<>();
        Date latestCreateDate = null;
        for (DataAlarm alarm : todayAlarms) {
            if (alarm.getCreateDate() != null && (latestCreateDate == null || alarm.getCreateDate().after(latestCreateDate))) {
                latestCreateDate = alarm.getCreateDate();
            }
            if (StringUtils.isNotBlank(alarm.getMessage())) {
                messageSet.add(alarm.getMessage());
            }
            Map<String, Integer> streetLevelMap = buildStreetHighestLevelMap(alarm.getLevelStreetsJson());
            if (streetLevelMap.isEmpty()) {
                continue;
            }
            allStreetSet.addAll(streetLevelMap.keySet());
            for (AdRegionVo adRegionVo : buildAdRegionVoListByStreetLevels(streetLevelMap)) {
                String key = StringUtils.defaultString(adRegionVo.getPcode()) + "_" + StringUtils.defaultString(adRegionVo.getName())
                    + "_" + StringUtils.defaultString(adRegionVo.getStreet());
                AdRegionVo exist = adRegionMap.get(key);
                if (exist == null) {
                    adRegionMap.put(key, adRegionVo);
                } else {
                    exist.setEventLevel(mergeHigherRiskLevel(exist.getEventLevel(), adRegionVo.getEventLevel()));
                }
            }
        }
        List<String> allStreets = List.copyOf(allStreetSet);
        if (allStreets.isEmpty()) {
            resp.setCreateDate(latestCreateDate);
            resp.setMessages(new ArrayList<>(messageSet));
            return resp;
        }

        List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(allStreets);
        resp.setCreateDate(latestCreateDate);
        resp.setMessages(new ArrayList<>(messageSet));
        resp.setStreetCount(allStreets.size());
        resp.setSlopeUnitCount(ObjUtil.isEmpty(slopeUnits) ? 0 : slopeUnits.size());
        resp.setTotalArea(calculateAlarmAreaByAdRegion(allStreets));
        resp.setAdRegionVoList(new ArrayList<>(adRegionMap.values()));
        return resp;
    }

    private WeatherAlarmStatsResp buildEmptyWeatherAlarmStatsResp() {
        WeatherAlarmStatsResp resp = new WeatherAlarmStatsResp();
        resp.setTotalArea(BigDecimal.ZERO);
        resp.setStreetCount(0);
        resp.setSlopeUnitCount(0);
        resp.setMessages(new ArrayList<>());
        resp.setAdRegionVoList(new ArrayList<>());
        return resp;
    }

    private DataAlarmPendingGroupResp.Item toPendingGroupItem(DataAlarm alarm) {
        DataAlarmPendingGroupResp.Item item = new DataAlarmPendingGroupResp.Item();
        item.setId(alarm.getId());
        item.setCity(alarm.getCity());
        item.setLevelStreetsJson(alarm.getLevelStreetsJson());
        item.setCreateDate(alarm.getCreateDate());
        item.setPublishDate(alarm.getPublishDate());
        item.setValidStartDate(alarm.getValidStartDate());
        item.setValidEndDate(alarm.getValidEndDate());
        item.setKeyTips(alarm.getKeyTips());
        item.setPendingStatus(alarm.getPendingStatus());
        return item;
    }

    private List<AdRegionVo> buildAdRegionVoListByStreetLevels(Map<String, Integer> streetLevelMap) {
        if (streetLevelMap == null || streetLevelMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<AdRegionVo> adRegions = adRegionService.querySlopeUnitListByStreets(new ArrayList<>(streetLevelMap.keySet()));
        if (ObjUtil.isEmpty(adRegions)) {
            return Collections.emptyList();
        }
        adRegions.forEach(adRegionVo -> adRegionVo.setEventLevel(streetLevelMap.get(adRegionVo.getStreet())));
        return adRegions;
    }

    /**
     * 合并同一区域多条告警时的风险等级（数值越小风险越高）。
     */
    private Integer mergeHigherRiskLevel(Integer levelA, Integer levelB) {
        if (levelA == null) {
            return levelB;
        }
        if (levelB == null) {
            return levelA;
        }
        return Math.min(levelA, levelB);
    }

    /**
     * 按街道对应行政区划统计告警面积。
     *
     * @param streets 街道列表
     * @return 面积总和（平方千米）
     */
    private BigDecimal calculateAlarmAreaByAdRegion(List<String> streets) {
        List<AdRegionVo> adRegions = adRegionService.querySlopeUnitListByStreets(streets);
        if (ObjUtil.isEmpty(adRegions)) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalAreaInSquareMeter = adRegions.stream()
                                                     .map(AdRegionVo::getArea)
                                                     .filter(Objects::nonNull)
                                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAreaInSquareMeter.divide(SQUARE_METERS_PER_SQUARE_KILOMETER, AREA_SCALE, RoundingMode.HALF_UP);
    }

    private List<String> queryAllEnshiStreets() {
        AdRegionBo countyBo = new AdRegionBo();
        countyBo.setCounty(ENSHI_CITY);
        countyBo.setLevel(4);
        List<String> streets = extractDistinctStreets(adRegionService.queryList(countyBo));
        if (!streets.isEmpty()) {
            return streets;
        }

        AdRegionBo cityBo = new AdRegionBo();
        cityBo.setCity(ENSHI_CITY);
        cityBo.setLevel(4);
        return extractDistinctStreets(adRegionService.queryList(cityBo));
    }

    private List<String> extractDistinctStreets(List<AdRegionVo> adRegions) {
        if (ObjUtil.isEmpty(adRegions)) {
            return Collections.emptyList();
        }
        return adRegions.stream()
                        .map(AdRegionVo::getStreet)
                        .map(street -> street == null ? null : street.trim())
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .toList();
    }

    private Set<String> getUsedStreetSet(Date validStartDate,
                                         Date validEndDate,
                                         Integer currentLevel,
                                         Map<String, Set<String>> usedStreetsByWindowAndLevel,
                                         DailyReportAlarmDraft dailyDraft) {
        String windowLevelKey = buildWindowLevelKey(validStartDate, validEndDate, currentLevel);
        Set<String> usedStreetSet = new LinkedHashSet<>(usedStreetsByWindowAndLevel.computeIfAbsent(windowLevelKey,
            key -> loadUsedStreets(validStartDate, validEndDate, currentLevel)));
        if (dailyDraft != null) {
            dailyDraft.levelStreets.forEach((level, streets) -> {
                if (isHigherLevel(level, currentLevel) && ObjUtil.isNotEmpty(streets)) {
                    usedStreetSet.addAll(streets);
                }
            });
        }
        return usedStreetSet;
    }

    private Set<String> loadUsedStreets(Date validStartDate, Date validEndDate, Integer currentLevel) {
        List<DataAlarm> alarms = baseMapper.selectList(Wrappers.<DataAlarm>lambdaQuery()
                                                               .eq(DataAlarm::getCity, ENSHI_CITY)
                                                               .eq(DataAlarm::getValidStartDate, validStartDate)
                                                               .eq(DataAlarm::getValidEndDate, validEndDate));
        Set<String> usedStreets = alarms.stream()
                                        .flatMap(alarm -> parseLevelStreetsJson(alarm.getLevelStreetsJson()).entrySet().stream())
                                        .filter(entry -> isHigherLevel(entry.getKey(), currentLevel))
                                        .flatMap(entry -> entry.getValue().stream())
                                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return usedStreets;
    }

    private boolean isHigherLevel(Integer candidateLevel, Integer currentLevel) {
        return candidateLevel != null && currentLevel != null && candidateLevel > currentLevel;
    }

    private Date parseRequiredDate(String value, String fieldName, String pattern) {
        if (StringUtils.isBlank(value)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            return new Date(DateUtil.parse(value.trim(), pattern).getTime());
        } catch (Exception e) {
            throw new ServiceException(fieldName + "格式错误，应为" + pattern);
        }
    }

    private Set<String> resolveStreets(DataAlarmReportAnalysisReq.AlarmLevelItem levelItem,
                                       List<String> allEnshiStreets,
                                       Set<String> usedStreetSet) {
        if (levelItem == null || ObjUtil.isEmpty(levelItem.getStreets())) {
            return Collections.emptySet();
        }

        Set<String> resolved = new LinkedHashSet<>();
        boolean containsOtherStreet = false;
        for (String street : levelItem.getStreets()) {
            if (StringUtils.isBlank(street)) {
                continue;
            }
            String trimmed = street.trim();
            if (OTHER_STREET_NAME.equals(trimmed) || ENSHI_CITY.equals(trimmed)) {
                containsOtherStreet = true;
                continue;
            }
            resolved.add(trimmed);
        }

        if (containsOtherStreet) {
            if (allEnshiStreets.isEmpty()) {
                throw new ServiceException("未查询到恩施市乡镇配置，无法解析其他乡镇");
            }
            allEnshiStreets.stream()
                           .filter(StringUtils::isNotBlank)
                           .filter(street -> !usedStreetSet.contains(street))
                           .forEach(resolved::add);
        }
        return resolved;
    }

    private String buildReportAnalysisMessage(ReportMessageContext context) {
        if (context == null) {
            return null;
        }
        List<String> segments = new ArrayList<>(4);
        appendRiskSegment(segments, context.levelStreets.get(1), "较低", true);
        appendRiskSegment(segments, context.levelStreets.get(2), "中", false);
        appendRiskSegment(segments, context.levelStreets.get(3), "高", false);
        appendRiskSegment(segments, context.levelStreets.get(4), "极高", false);
        if (segments.isEmpty()) {
            return null;
        }
        return formatReportRange(context.validStartDate, context.validEndDate) + "，预计" + String.join("；", segments) + "。" + REPORT_ANALYSIS_SUFFIX;
    }

    private void appendRiskSegment(List<String> segments, Set<String> streets, String riskText, boolean appendHeavyRainArea) {
        if (ObjUtil.isEmpty(streets)) {
            return;
        }
        String streetText = String.join(",", streets);
        String prefix = ENSHI_CITY + "(" + streetText + ")";
        if (appendHeavyRainArea) {
            prefix += "以及其他强降水区域";
        }
        segments.add(prefix + "发生崩塌、滑坡、泥石流的风险" + riskText);
    }

    private String formatReportRange(Date validStartDate, Date validEndDate) {
        return formatChineseDateTime(validStartDate) + "-" + formatChineseDateTime(validEndDate);
    }

    private String formatChineseDateTime(Date date) {
        if (date == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (calendar.get(Calendar.MINUTE) == 0) {
            return DateUtil.format(date, "yyyy年MM月dd日HH时");
        }
        return DateUtil.format(date, "yyyy年MM月dd日HH时mm分");
    }

    private String buildWindowKey(Date validStartDate, Date validEndDate) {
        long validStartMillis = validStartDate == null ? 0L : validStartDate.getTime();
        long validEndMillis = validEndDate == null ? 0L : validEndDate.getTime();
        return validStartMillis + "_" + validEndMillis;
    }

    private String buildWindowLevelKey(Date validStartDate, Date validEndDate, Integer level) {
        return buildWindowKey(validStartDate, validEndDate) + "_" + level;
    }

    private String buildAlarmUniqueKey(DataAlarmBo bo) {
        return buildAlarmUniqueKey(bo.getCode(), bo.getCity(), bo.getLevelStreetsJson(), bo.getTime(),
            bo.getPublishDate(), bo.getValidStartDate(), bo.getValidEndDate());
    }

    private String buildAlarmUniqueKey(DataAlarm entity) {
        return buildAlarmUniqueKey(entity.getCode(), entity.getCity(), entity.getLevelStreetsJson(), entity.getTime(),
            entity.getPublishDate(), entity.getValidStartDate(), entity.getValidEndDate());
    }

    private String buildAlarmUniqueKey(String code, String city, String levelStreetsJson, Date time,
                                       Date publishDate, Date validStartDate, Date validEndDate) {
        return String.join("|",
            StringUtils.defaultString(code),
            StringUtils.defaultString(city),
            StringUtils.defaultString(normalizeLevelStreetsJson(levelStreetsJson)),
            String.valueOf(time == null ? 0L : time.getTime()),
            String.valueOf(publishDate == null ? 0L : publishDate.getTime()),
            String.valueOf(validStartDate == null ? 0L : validStartDate.getTime()),
            String.valueOf(validEndDate == null ? 0L : validEndDate.getTime()));
    }

    private void validEntityBeforeSave(DataAlarm entity) {
        Assert.notNull(entity.getId(), "ID不能为空");
        Assert.notNull(entity.getCode(), "编号不能为空");
        Assert.notNull(entity.getCity(), "城市不能为空");
        Assert.notNull(entity.getLevelStreetsJson(), "levelStreetsJson不能为空");
        Assert.notNull(entity.getStatus(), "状态不能为空");
        Assert.notNull(entity.getCreateDate(), "创建时间不能为空");
        Assert.notNull(entity.getSource(), "数据来源不能为空");
        LambdaQueryWrapper<DataAlarm> lqw = Wrappers.<DataAlarm>lambdaQuery()
                                                    .eq(DataAlarm::getCode, entity.getCode())
                                                    .eq(DataAlarm::getCity, entity.getCity())
                                                    .eq(DataAlarm::getLevelStreetsJson, normalizeLevelStreetsJson(entity.getLevelStreetsJson()))
                                                    .eq(entity.getTime() != null, DataAlarm::getTime, entity.getTime())
                                                    .eq(entity.getPublishDate() != null, DataAlarm::getPublishDate, entity.getPublishDate())
                                                    .eq(entity.getValidStartDate() != null, DataAlarm::getValidStartDate, entity.getValidStartDate())
                                                    .eq(entity.getValidEndDate() != null, DataAlarm::getValidEndDate, entity.getValidEndDate())
                                                    .ne(entity.getId() != null, DataAlarm::getId, entity.getId());
        if (baseMapper.selectCount(lqw) > 0) {
            throw new IllegalArgumentException("相同预警数据已存在");
        }
    }

    private void normalizeManualLevelStreetsJson(DataAlarmBo bo) {
        bo.setLevelStreetsJson(normalizeLevelStreetsJson(bo.getLevelStreetsJson()));
    }

    private Map<Integer, LinkedHashSet<String>> parseLevelStreetsJson(String json) {
        return DataAlarmLevelStreetsUtil.parse(json);
    }

    private String normalizeLevelStreetsJson(String json) {
        return DataAlarmLevelStreetsUtil.toJson(parseLevelStreetsJson(json));
    }

    private DataAlarmBo buildReportAnalysisAlarmBo(DailyReportAlarmDraft draft,
                                                   String requestCode,
                                                   String sessionId,
                                                   String source,
                                                   Date now,
                                                   String keyTips) {
        DataAlarmBo bo = new DataAlarmBo();
        bo.setId(IdUtil.getSnowflakeNextId());
        bo.setCode(requestCode);
        bo.setSessionId(sessionId);
        bo.setCity(ENSHI_CITY);
        bo.setLevelStreetsJson(DataAlarmLevelStreetsUtil.toJson(draft.levelStreets));
        bo.setTime(draft.time);
        bo.setPublishDate(draft.publishDate);
        bo.setValidStartDate(draft.validStartDate);
        bo.setValidEndDate(draft.validEndDate);
        bo.setStatus(ALARM_STATUS_UNTRIGGERED);
        bo.setPendingStatus(ALARM_PENDING_STATUS_NONE);
        bo.setCreateDate(now);
        bo.setSource(source);
        bo.setSourceType(SOURCE_TYPE_REPORT_ANALYSIS);
        bo.setKeyTips(keyTips);
        return bo;
    }

    private List<String> extractStreetList(String levelStreetsJson) {
        return DataAlarmLevelStreetsUtil.flattenStreets(levelStreetsJson);
    }

    private Map<String, Integer> buildStreetHighestLevelMap(String levelStreetsJson) {
        return DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(levelStreetsJson);
    }

    private static final class ReportMessageContext {

        private Date validStartDate;
        private Date validEndDate;
        private final Map<Integer, Set<String>> levelStreets = new LinkedHashMap<>();

        private ReportMessageContext(Date validStartDate, Date validEndDate) {
            this.validStartDate = validStartDate;
            this.validEndDate = validEndDate;
        }

        private void mergeWindow(Date validStartDate, Date validEndDate) {
            if (this.validStartDate == null || (validStartDate != null && validStartDate.before(this.validStartDate))) {
                this.validStartDate = validStartDate;
            }
            if (this.validEndDate == null || (validEndDate != null && validEndDate.after(this.validEndDate))) {
                this.validEndDate = validEndDate;
            }
        }

        private void addStreets(Integer level, Collection<String> streets) {
            if (level == null || ObjUtil.isEmpty(streets)) {
                return;
            }
            levelStreets.computeIfAbsent(level, key -> new LinkedHashSet<>()).addAll(streets);
        }
    }

    private static final class DailyReportAlarmDraft {

        private final Date time;
        private Date publishDate;
        private Date validStartDate;
        private Date validEndDate;
        private final Map<Integer, LinkedHashSet<String>> levelStreets = new LinkedHashMap<>();

        private DailyReportAlarmDraft(Date time, Date publishDate, Date validStartDate, Date validEndDate) {
            this.time = time;
            this.publishDate = publishDate;
            this.validStartDate = validStartDate;
            this.validEndDate = validEndDate;
        }

        private void mergeWindow(Date publishDate, Date validStartDate, Date validEndDate) {
            if (this.publishDate == null || (publishDate != null && publishDate.before(this.publishDate))) {
                this.publishDate = publishDate;
            }
            if (this.validStartDate == null || (validStartDate != null && validStartDate.before(this.validStartDate))) {
                this.validStartDate = validStartDate;
            }
            if (this.validEndDate == null || (validEndDate != null && validEndDate.after(this.validEndDate))) {
                this.validEndDate = validEndDate;
            }
        }

        private void addStreets(Integer level, Collection<String> streets) {
            if (level == null || ObjUtil.isEmpty(streets)) {
                return;
            }
            levelStreets.computeIfAbsent(level, key -> new LinkedHashSet<>()).addAll(streets);
        }
    }
}
