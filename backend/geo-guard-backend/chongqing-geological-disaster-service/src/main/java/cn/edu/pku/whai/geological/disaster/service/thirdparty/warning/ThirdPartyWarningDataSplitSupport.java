package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.StatThirdWarningBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceTreeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DeviceRainfallDisplacementVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LiveRainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceTreeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorDeviceMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorPointMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallCellMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallEntityGridMappingMapper;
import cn.edu.pku.whai.geological.disaster.data.props.DisasterPreventionPlatformProps;
import cn.edu.pku.whai.geological.disaster.data.props.WarningDataProps;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DeviceLatestWarningInfo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusSnapshot;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventPageResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventRow;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorWarningDataResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 三方预警数据拆分处理服务（从 ThirdPartyWarningDataServiceImpl 提取为独立文件）。
 *
 * @author kongweiguang
 */
@Service
@SuppressWarnings({"unused", "SameParameterValue"})
public class ThirdPartyWarningDataSplitSupport {

    public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThirdPartyWarningDataSplitSupport.class);
    public static final int THIRD_PARTY_PAGE_SIZE = 200;
    public static final int WARNING_DETAIL_PARALLELISM = 10;
    public static final int WARNING_LIST_PARALLELISM = 8;
    public static final Duration WARNING_LIST_REQ_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration WARNING_DETAIL_REQ_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration DEFAULT_WARNING_LOOKBACK_DURATION = Duration.ofDays(7);
    static final long RUNTIME_STAGE_TIMEOUT_SECONDS = 90;
    static final int RUNTIME_WARNING_MAX_IN_FLIGHT = 20;
    public static final int WARNING_PAGE_MAX_RETRY = 2;
    public static final AtomicInteger WARNING_DETAIL_THREAD_SEQ = new AtomicInteger(1);
    public static final AtomicInteger WARNING_LIST_THREAD_SEQ = new AtomicInteger(1);
    public static final ExecutorService WARNING_DETAIL_EXECUTOR = new ThreadPoolExecutor(
        WARNING_DETAIL_PARALLELISM, WARNING_DETAIL_PARALLELISM,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("warning-detail-fetch-" + WARNING_DETAIL_THREAD_SEQ.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    );
    static final ExecutorService WARNING_LIST_EXECUTOR = new ThreadPoolExecutor(
        WARNING_LIST_PARALLELISM, WARNING_LIST_PARALLELISM,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("warning-list-fetch-" + WARNING_LIST_THREAD_SEQ.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    );
    final WarningDataProps warningDataProps;
    final DisasterPreventionPlatformProps disasterPreventionPlatformProps;
    final MonitorPointMapper monitorPointMapper;
    final MonitorDeviceMapper monitorDeviceMapper;
    final RainfallEntityGridMappingMapper rainfallEntityGridMappingMapper;
    final RainfallCellMapper rainfallCellMapper;
    final IAdRegionService adRegionService;
    final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    final ThirdPartyWarningRemoteSupport remoteSupport;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DEVICE_CURE_DIRECT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter DEVICE_CURE_DIRECT_UTC_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
    public static final double DISPLACEMENT_UNAVAILABLE = -1D;
    public static final String DEFAULT_REGION_CODE = "422801";
    public static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");
    final Map<String, String> adRegionNameCache = new ConcurrentHashMap<>();
    final MonitorWarningDelegate monitorWarningDelegate;
    final DeviceDataDelegate deviceDataDelegate;
    final LocalBindingDelegate localBindingDelegate;
    final ParallelFetchDelegate parallelFetchDelegate;
    final StatsMiscDelegate statsMiscDelegate;
    final WarningQueryGuardDelegate warningQueryGuardDelegate;
    final WarningDisposalDelegate warningDisposalDelegate;
    String token;

    public ThirdPartyWarningDataSplitSupport(
        WarningDataProps warningDataProps,
        DisasterPreventionPlatformProps disasterPreventionPlatformProps,
        MonitorPointMapper monitorPointMapper,
        MonitorDeviceMapper monitorDeviceMapper,
        RainfallEntityGridMappingMapper rainfallEntityGridMappingMapper,
        RainfallCellMapper rainfallCellMapper,
        IAdRegionService adRegionService,
        NamedParameterJdbcTemplate namedParameterJdbcTemplate,
        ThirdPartyWarningRemoteSupport remoteSupport
    ) {
        this.warningDataProps = warningDataProps;
        this.disasterPreventionPlatformProps = disasterPreventionPlatformProps;
        this.monitorPointMapper = monitorPointMapper;
        this.monitorDeviceMapper = monitorDeviceMapper;
        this.rainfallEntityGridMappingMapper = rainfallEntityGridMappingMapper;
        this.rainfallCellMapper = rainfallCellMapper;
        this.adRegionService = adRegionService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.remoteSupport = remoteSupport;

        // Delegates depend on Spring-injected collaborators, so initialize them only after constructor injection completes.
        this.monitorWarningDelegate = new MonitorWarningDelegate(this);
        this.deviceDataDelegate = new DeviceDataDelegate(this);
        this.localBindingDelegate = new LocalBindingDelegate(this);
        this.parallelFetchDelegate = new ParallelFetchDelegate(this);
        this.statsMiscDelegate = new StatsMiscDelegate(this);
        this.warningQueryGuardDelegate = new WarningQueryGuardDelegate(this);
        this.warningDisposalDelegate = new WarningDisposalDelegate(this);
    }

    NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() { return namedParameterJdbcTemplate; }
    MonitorPointMapper getMonitorPointMapper() { return monitorPointMapper; }
    MonitorDeviceMapper getMonitorDeviceMapper() { return monitorDeviceMapper; }
    RainfallEntityGridMappingMapper getRainfallEntityGridMappingMapper() { return rainfallEntityGridMappingMapper; }
    RainfallCellMapper getRainfallCellMapper() { return rainfallCellMapper; }
    WarningDataProps getWarningDataProps() { return warningDataProps; }
    DisasterPreventionPlatformProps getDisasterPreventionPlatformProps() { return disasterPreventionPlatformProps; }
    DeviceDataDelegate getDeviceDataDelegate() { return deviceDataDelegate; }
    LocalBindingDelegate getLocalBindingDelegate() { return localBindingDelegate; }
    ParallelFetchDelegate getParallelFetchDelegate() { return parallelFetchDelegate; }
    WarningQueryGuardDelegate getWarningQueryGuardDelegate() { return warningQueryGuardDelegate; }
    WarningDisposalDelegate getWarningDisposalDelegate() { return warningDisposalDelegate; }
    String getToken() { return token; }
    public org.slf4j.Logger getLog() { return log; }

    /**
     * 获取监测预警数据（先获取全部数据，然后在内存中分页）
     *
     * @param bo        查询条件，包含开始时间和结束时间
     * @param pageQuery 分页查询参数
     * @return 分页的监测预警数据列表
     */
    public TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return monitorWarningDelegate.getMonitorWarning(bo, pageQuery);
    }

    /**
     * 获取三方预警处置分页列表
     */
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return monitorWarningDelegate.getMonitorWarningDisposal(bo, pageQuery);
    }

    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return monitorWarningDelegate.getMonitorWarningDisposalFromRemote(bo, pageQuery);
    }

    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(
        ThirdPartyWarningDisposalBo bo, PageQuery pageQuery,
        LocalDateTime warningStartTime, LocalDateTime warningEndTime) {
        return monitorWarningDelegate.getMonitorWarningDisposalFromRemote(bo, pageQuery, warningStartTime, warningEndTime);
    }

    /**
     * 获取三方设备监测曲线
     */
    public ThirdPartyDeviceCureVo queryDeviceCure(ThirdPartyDeviceCureBo bo) {
        // noinspection SqlResolve

        return deviceDataDelegate.queryDeviceCure(bo);
    }

    /**
     * 获取三方监测设备分页
     */
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {

        return deviceDataDelegate.getMonitorDevicePage(bo, pageQuery);
    }

    /**
     * 从三方接口拉取监测设备分页，供同步任务使用。
     */
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePageFromRemote(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {

        return deviceDataDelegate.getMonitorDevicePageFromRemote(bo, pageQuery);
    }

    /**
     * 根据监测点名称构建设备-监测器树结构
     */
    public List<ThirdPartyMonitorDeviceTreeVo> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo) {

        return deviceDataDelegate.getMonitorDeviceTree(bo);
    }

    /**
     * 全量：本地 v_monitor_device、按监测点区划拉 jcsbxx、按监测点拉 jcdsbyj，合并最近预警等级与设备状态
     */
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId) {

        return deviceDataDelegate.listAllMonitorDevicesRuntime(regionCode, slopeUnitId);
    }

    /**
     * 从三方接口拉取设备运行时状态，供同步任务使用。
     */
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntimeFromRemote(String regionCode) {
        return deviceDataDelegate.listAllMonitorDevicesRuntimeFromRemote(regionCode);
    }

    List<MonitorPoint> queryMonitorPoints(String regionCode, String monitorPointName) {
        return localBindingDelegate.queryMonitorPoints(regionCode, monitorPointName);
    }

    List<MonitorDevice> queryMonitorDevices(Set<String> pointIds, String deviceCode, String deviceName, String manufacturer) {
        return localBindingDelegate.queryMonitorDevices(pointIds, deviceCode, deviceName, manufacturer);
    }

    List<MonitorDevice> filterBySlopeUnitId(List<MonitorDevice> devices, String slopeUnitId) {
        return localBindingDelegate.filterBySlopeUnitId(devices, slopeUnitId);
    }

    Map<String, MonitorPoint> buildMonitorPointById(List<MonitorPoint> points) {
        return localBindingDelegate.buildMonitorPointById(points);
    }

    Map<String, MonitorPoint> buildMonitorPointByName(List<MonitorPoint> points) {
        return localBindingDelegate.buildMonitorPointByName(points);
    }

    MonitorPoint resolveLocalMonitorPoint(Map<String, MonitorPoint> pointById, Map<String, MonitorPoint> pointByName,
                                                  String monitorPointId, String monitorPointName) {
        return localBindingDelegate.resolveLocalMonitorPoint(pointById, pointByName, monitorPointId, monitorPointName);
    }

    MonitorPoint resolveRemoteLocalMonitorPoint(
        ThirdMonitorDeviceWarningResp warningResp,
        Map<String, MonitorPoint> localMonitorPointMap,
        Map<String, MonitorPoint> pointById,
        Map<String, MonitorPoint> pointByName
    ) {
        return localBindingDelegate.resolveRemoteLocalMonitorPoint(warningResp, localMonitorPointMap, pointById, pointByName);
    }

    Map<String, MonitorDevice> loadMonitorDeviceById(Set<String> pointIds) {
        return localBindingDelegate.loadMonitorDeviceById(pointIds);
    }

    Map<String, MonitorDevice> buildMonitorDeviceByClientId(Collection<MonitorDevice> devices) {
        return localBindingDelegate.buildMonitorDeviceByClientId(devices);
    }

    LocalStatusLookup loadLocalStatusLookup(List<MonitorDevice> devices) {

        return localBindingDelegate.loadLocalStatusLookup(devices);
    }

    LocalWarningLookup loadLocalWarningLookup(List<MonitorDevice> devices) {

        return localBindingDelegate.loadLocalWarningLookup(devices);
    }

    LocalWarningEventPageResult queryLocalWarningEventPage(ThirdPartyWarningDisposalBo bo, Set<String> pointIds, Set<String> pointNames,
                                                                   int pageNum, int pageSize) {

        // noinspection SqlResolve
        return localBindingDelegate.queryLocalWarningEventPage(bo, pointIds, pointNames, pageNum, pageSize);
    }

    LocalStatusSnapshot resolveLocalStatus(MonitorDevice device, LocalStatusLookup lookup) {
        return localBindingDelegate.resolveLocalStatus(device, lookup);
    }

    DeviceLatestWarningInfo resolveLatestWarningInfo(MonitorDevice device, LocalWarningLookup lookup) {
        return localBindingDelegate.resolveLatestWarningInfo(device, lookup);
    }

    void enrichMonitorWarningDisposalRuntimeFields(
        ThirdPartyWarningDisposalVo vo,
        MonitorDevice device,
        LocalStatusSnapshot status,
        DeviceLatestWarningInfo latestWarningInfo,
        String fallbackWarningTime
    ) {
        localBindingDelegate.enrichMonitorWarningDisposalRuntimeFields(vo, device, status, latestWarningInfo, fallbackWarningTime);
    }

    ThirdPartyMonitorDeviceVo buildLocalMonitorDeviceVo(MonitorDevice device, MonitorPoint point, LocalStatusSnapshot status, int rowId) {
        ThirdPartyMonitorDeviceVo vo = new ThirdPartyMonitorDeviceVo();
        vo.setId(device.getId());
        vo.setClientId(device.getClientId());
        vo.setCreateTime(formatDate(device.getCreatedTime()));
        vo.setLocationDesc(point == null ? device.getDeviceInstallAddress() : firstNonBlank(point.getLocationDesc(), device.getDeviceInstallAddress()));
        vo.setDeviceModel(device.getDeviceModel());
        vo.setLon(toDouble(device.getLongitude()));
        vo.setManufacturer(device.getDeviceParameters());
        vo.setManufacturerId(null);
        Date lastOnlineTime = status != null && status.lastOnlineTime() != null
            ? new Date(status.lastOnlineTime().getTime())
            : firstNonNullDate(device.getDeviceLastOnlineTime(), device.getDeviceLatestOnlineTime());
        vo.setLastOnlineTime(formatDate(lastOnlineTime));
        vo.setEnabledFlag(device.getIsEnabled());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setDeviceEnableTime(formatDate(device.getDeviceEnableTime()));
        vo.setSn(device.getDeviceSerialNumber());
        vo.setDeviceStatusName(status == null ? null : firstNonBlank(status.statusName(), buildStatusName(status.statusCode())));
        vo.setAddress(device.getDeviceInstallAddress());
        vo.setLat(toDouble(device.getLatitude()));
        vo.setDeviceCode(device.getDeviceBusinessCode());
        vo.setMonitorPointId(device.getMonitoringPointId());
        vo.setPlatformDeviceCode(device.getBatchSyncDeviceCode());
        vo.setCommunicationType(device.getCommunicationMethod());
        vo.setMonitorPointName(point == null ? null : point.getMonitorName());
        vo.setSyncToBigScreen(device.getIsSyncToScreen() == null ? null : device.getIsSyncToScreen().intValue());
        vo.setRowId(rowId);
        vo.setDeviceKey(device.getDeviceSecret());
        vo.setMonitorType(device.getMonitoringType());
        vo.setDeviceCategory(device.getDeviceCategory() == null ? null : device.getDeviceCategory().intValue());
        vo.setAccessProtocol(device.getAccessProtocol());
        vo.setDeviceStatus(status == null ? (device.getDeviceStatus() == null ? null : device.getDeviceStatus().intValue()) : status.statusCode());
        vo.setOnlineStatusTime(formatTimestamp(status == null ? toTimestamp(device.getDeviceLatestOnlineTime()) : status.sourceUpdateTime()));
        return vo;
    }

    String resolveWarningDeviceName(LocalWarningEventRow row, Map<String, MonitorDevice> deviceById, Map<String, MonitorDevice> deviceByClientId) {
        if (StrUtil.isNotBlank(row.warningDeviceName())) {
            return row.warningDeviceName();
        }
        if (StrUtil.isNotBlank(row.deviceId())) {
            MonitorDevice byId = deviceById.get(row.deviceId());
            if (byId != null) {
                return byId.getDeviceName();
            }
        }
        if (StrUtil.isNotBlank(row.clientId())) {
            MonitorDevice byClientId = deviceByClientId.get(row.clientId());
            if (byClientId != null) {
                return byClientId.getDeviceName();
            }
        }
        return null;
    }

    MonitorDevice resolveWarningDevice(LocalWarningEventRow row, Map<String, MonitorDevice> deviceById, Map<String, MonitorDevice> deviceByClientId) {
        if (row == null) {
            return null;
        }
        if (StrUtil.isNotBlank(row.deviceId())) {
            MonitorDevice byId = deviceById.get(row.deviceId());
            if (byId != null) {
                return byId;
            }
        }
        if (StrUtil.isBlank(row.clientId())) {
            return null;
        }
        return deviceByClientId.get(row.clientId());
    }

    String resolveWarningDeviceId(LocalWarningEventRow row, Map<String, MonitorDevice> deviceByClientId) {
        if (row == null) {
            return null;
        }
        if (StrUtil.isNotBlank(row.deviceId())) {
            return row.deviceId();
        }
        if (StrUtil.isBlank(row.clientId())) {
            return null;
        }
        MonitorDevice device = deviceByClientId.get(row.clientId());
        return device == null ? null : device.getId();
    }

    Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    Integer getNullableInt(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    Integer parseStoredWarningLevel(String warningLevel) {
        if (StrUtil.isBlank(warningLevel)) {
            return null;
        }
        try {
            return Integer.parseInt(warningLevel.trim());
        } catch (Exception ignore) {
            return convertFromThirdPartyWarningLevel(warningLevel.trim());
        }
    }

    String buildStatusName(Integer statusCode) {
        if (statusCode == null) {
            return null;
        }
        return statusCode == 1 ? "在线" : "离线";
    }

    String formatDate(Date value) {
        if (value == null) {
            return null;
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    String formatTimestamp(Timestamp value) {
        if (value == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(value.toLocalDateTime());
    }

    Timestamp toTimestamp(Date value) {
        return value == null ? null : new Timestamp(value.getTime());
    }

    Date firstNonNullDate(Date... values) {
        for (Date value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    boolean matchesRegionCode(String sourceRegionCode, String queryRegionCode) {
        if (StrUtil.isBlank(sourceRegionCode) || StrUtil.isBlank(queryRegionCode)) {
            return false;
        }
        String source = sourceRegionCode.trim();
        String query = queryRegionCode.trim();
        if (query.length() >= 12) {
            return source.equals(query);
        }
        return source.startsWith(query);
    }

    /**
     * 按设备 sbid 取 yjsj 最新一条预警的 yjdj，并转为 1-4
     */
    Map<String, DeviceLatestWarningInfo> mergeLatestWarningBySbid(List<ThirdMonitorDeviceWarningResp> allWarnings) {
        return parallelFetchDelegate.mergeLatestWarningBySbid(allWarnings);
    }

    /**
     * 组装脱敏后的运行时 VO
     */
    MonitorDeviceRuntimeVo toMonitorDeviceRuntimeVo(
        MonitorDevice d,
        DeviceLatestWarningInfo latestWarningInfo,
        Map<String, ThirdPartyMonitorDeviceVo> byId,
        Map<String, ThirdPartyMonitorDeviceVo> byClientId) {
        return parallelFetchDelegate.toMonitorDeviceRuntimeVo(d, latestWarningInfo, byId, byClientId);
    }

    /**
     * 根据设备状态名称解析在线状态，0-离线，1-在线
     */
    Integer resolveOnlineStatus(String statusName) {
        if (StrUtil.isBlank(statusName)) {
            return null;
        }
        if (statusName.contains("在线")) {
            return 1;
        }
        if (statusName.contains("离线")) {
            return 0;
        }
        return null;
    }

    /**
     * 将行政区划编码补齐到12位
     */
    String normalizeRegionCode(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            return regionCode;
        }
        if (regionCode.length() >= 12) {
            return regionCode;
        }
        StringBuilder builder = new StringBuilder(regionCode);
        while (builder.length() < 12) {
            builder.append('0');
        }
        return builder.toString();
    }

    /**
     * 统一处理监测设备查询参数
     */
    ThirdPartyMonitorDeviceBo prepareMonitorDeviceQuery(ThirdPartyMonitorDeviceBo bo, boolean fillDefaultRegionCode) {
        ThirdPartyMonitorDeviceBo queryBo = bo == null ? new ThirdPartyMonitorDeviceBo() : bo;
        if (fillDefaultRegionCode && StrUtil.isBlank(queryBo.getRegionCode())) {
            queryBo.setRegionCode(DEFAULT_REGION_CODE);
        }
        queryBo.setRegionCode(normalizeRegionCode(queryBo.getRegionCode()));
        if (StrUtil.isBlank(queryBo.getRegionCode())) {
            throw new ServiceException("行政区划不能为空");
        }
        return queryBo;
    }

    /**
     * 按监测点和设备名称稳定排序
     */
    void sortMonitorDevicesByPointAndName(List<MonitorDevice> devices, Map<String, MonitorPoint> pointById) {
        devices.sort(Comparator
            .comparing((MonitorDevice item) -> {
                MonitorPoint point = pointById.get(item.getMonitoringPointId());
                return point == null ? null : point.getMonitorName();
            }, Comparator.nullsLast(String::compareTo))
            .thenComparing(MonitorDevice::getDeviceName, Comparator.nullsLast(String::compareTo))
            .thenComparing(MonitorDevice::getId, Comparator.nullsLast(String::compareTo)));
    }

    /**
     * 分页拉取指定监测点下的全部设备
     */
    List<ThirdPartyMonitorDeviceVo> fetchAllMonitorDevices(ThirdPartyMonitorDeviceBo bo, boolean remote) {
        return parallelFetchDelegate.fetchAllMonitorDevices(bo, remote);
    }

    List<ThirdPartyMonitorDeviceVo> fetchAllMonitorDevicesForRegionSafe(String regionCode) {
        return parallelFetchDelegate.fetchAllMonitorDevicesForRegionSafe(regionCode);
    }

    /**
     * 拆分设备监测类型字符串
     */
    List<String> splitMonitorTypes(String monitorType) {
        if (StrUtil.isBlank(monitorType)) {
            return List.of();
        }
        return Arrays.stream(monitorType.split(","))
                     .map(String::trim)
                     .filter(StrUtil::isNotBlank)
                     .distinct()
                     .toList();
    }

    /**
     * 从监测器名称中提取监测器类型
     */
    String resolveMonitorTypeCode(String monitorName) {
        if (StrUtil.isBlank(monitorName) || !monitorName.contains("_")) {
            return monitorName;
        }
        return StrUtil.subAfter(monitorName, "_", true);
    }

    /**
     * 将 yyyyMMddHHmmss 按北京时间解析为毫秒时间戳。
     */
    Long parseDirectDeviceCureTime(String value, String fieldName) {
        String time = StrUtil.trim(value);
        try {
            LocalDateTime dateTime = LocalDateTime.parse(time, DEVICE_CURE_DIRECT_TIME_FORMATTER);
            return dateTime.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new ServiceException(fieldName + "格式错误，应为yyyyMMddHHmmss");
        }
    }

    /**
     * 直连三方曲线接口时，将返回 values 首列毫秒时间转换为 UTC 时间字符串。
     */
    void normalizeDirectDeviceCureResp(ThirdPartyWarningDataResp resp) {
        if (resp == null || resp.getData() == null) {
            return;
        }
        JSONObject data = JSONUtil.parseObj(resp.getData());
        JSONArray valuesArray = parseFlexibleJsonArray(data.get("values"), "values");
        if (valuesArray == null) {
            resp.setData(data);
            return;
        }
        for (int i = 0; i < valuesArray.size(); i++) {
            JSONArray rowArray = parseFlexibleJsonArray(valuesArray.get(i), "values[" + i + "]");
            if (rowArray == null || rowArray.isEmpty()) {
                continue;
            }
            Long rawEpochMillis = parseLongValue(rowArray.getFirst());
            if (rawEpochMillis == null) {
                continue;
            }
            rowArray.set(0, formatDirectDeviceCureTime(rawEpochMillis));
            valuesArray.set(i, rowArray);
        }
        data.set("values", valuesArray);
        resp.setData(data);
    }

    /**
     * 将毫秒时间戳转为可直接展示的 UTC 时间字符串。
     */
    String formatDirectDeviceCureTime(Long rawEpochMillis) {
        if (rawEpochMillis == null) {
            return null;
        }
        return DEVICE_CURE_DIRECT_UTC_DISPLAY_FORMATTER.format(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(rawEpochMillis), ZoneOffset.UTC)
        );
    }

    /**
     * 构建监测设备分页查询参数
     */
    Map<String, Object> buildMonitorDevicePageParams(ThirdPartyMonitorDeviceBo bo, int pageNum, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("sbbh", StrUtil.blankToDefault(bo.getDeviceCode(), ""));
        params.put("sbname", StrUtil.blankToDefault(bo.getDeviceName(), ""));
        params.put("jcdname", StrUtil.blankToDefault(bo.getMonitorPointName(), ""));
        params.put("regionCode", StrUtil.blankToDefault(bo.getRegionCode(), ""));
        params.put("sbcs", StrUtil.blankToDefault(bo.getManufacturer(), ""));
        params.put("current", pageNum);
        params.put("size", pageSize);
        return params;
    }

    /**
     * 构建设备监测曲线查询参数
     */
    Map<String, Object> buildDeviceCureParams(ThirdPartyDeviceCureBo bo) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", bo.getType());
        params.put("clientID", bo.getClientId());
        params.put("startTime", bo.getStartTime());
        params.put("endTime", bo.getEndTime());
        return params;
    }

    /**
     * 解析三方监测设备分页记录
     */
    List<ThirdPartyMonitorDeviceVo> parseMonitorDeviceRecords(JSONArray recordsArray) {
        List<ThirdPartyMonitorDeviceVo> records = new ArrayList<>();
        if (recordsArray == null) {
            return records;
        }
        for (int i = 0; i < recordsArray.size(); i++) {
            JSONObject obj = recordsArray.getJSONObject(i);
            ThirdPartyMonitorDeviceVo vo = new ThirdPartyMonitorDeviceVo();
            vo.setId(obj.getStr("id"));
            vo.setClientId(obj.getStr("clientid"));
            vo.setCreateTime(obj.getStr("crearteTime"));
            vo.setLocationDesc(obj.getStr("dlwz"));
            vo.setDeviceModel(obj.getStr("sbxh"));
            vo.setLon(obj.getDouble("lon"));
            vo.setManufacturer(obj.getStr("sbcs"));
            vo.setManufacturerId(obj.getStr("sbcsid"));
            vo.setLastOnlineTime(obj.getStr("sczxsj"));
            vo.setEnabledFlag(obj.getStr("sfqy"));
            vo.setDeviceName(obj.getStr("sbname"));
            vo.setDeviceType(obj.getStr("sbtype"));
            vo.setDeviceEnableTime(obj.getStr("sbqysj"));
            vo.setSn(obj.getStr("sn"));
            vo.setDeviceStatusName(obj.getStr("sbztname"));
            vo.setAddress(obj.getStr("addr"));
            vo.setLat(obj.getDouble("lat"));
            vo.setDeviceCode(obj.getStr("sbbh"));
            vo.setMonitorPointId(obj.getStr("jcdid"));
            vo.setPlatformDeviceCode(obj.getStr("bptsbbh"));
            vo.setCommunicationType(obj.getStr("txfs"));
            vo.setMonitorPointName(obj.getStr("jcdname"));
            vo.setSyncToBigScreen(obj.getInt("sftbdb"));
            vo.setRowId(obj.getInt("rowId"));
            vo.setDeviceKey(obj.getStr("sbkey"));
            vo.setMonitorType(obj.getStr("jclx"));
            vo.setDeviceCategory(obj.getInt("sbzl"));
            vo.setAccessProtocol(obj.getStr("jrxy"));
            vo.setDeviceStatus(obj.getInt("sbzt"));
            vo.setOnlineStatusTime(obj.getStr("zxztsj"));
            records.add(vo);
        }
        return records;
    }

    /**
     * 兼容第三方返回中数组字段在不同接口版本下的多种结构：
     * 1) 标准JSON数组
     * 2) JSON字符串数组
     * 3) 单个JSON对象（自动封装为单元素数组）
     */
    JSONArray parseFlexibleJsonArray(Object rawValue, String fieldName) {
        switch (rawValue) {
            case null -> {
                return null;
            }
            case JSONArray jsonArray -> {
                return jsonArray;
            }
            case Collection<?> collection -> {
                return JSONUtil.parseArray(collection);
            }
            case CharSequence sequence -> {
                String text = StrUtil.trim(sequence.toString());
                if (StrUtil.isBlank(text)) {
                    return null;
                }
                try {
                    if (StrUtil.startWith(text, "[")) {
                        return JSONUtil.parseArray(text);
                    }
                    if (StrUtil.startWith(text, "{")) {
                        JSONArray wrapper = new JSONArray();
                        wrapper.add(JSONUtil.parseObj(text));
                        return wrapper;
                    }
                } catch (Exception e) {
                    log.warn("解析数组字段失败, fieldName={}, value={}", fieldName, text, e);
                    return null;
                }
                log.warn("数组字段格式不支持, fieldName={}, value={}", fieldName, text);
                return null;
            }
            case JSONObject jsonObject -> {
                JSONArray wrapper = new JSONArray();
                wrapper.add(jsonObject);
                return wrapper;
            }
            default -> {
            }
        }
        log.warn("数组字段类型不支持, fieldName={}, valueType={}", fieldName, rawValue.getClass().getName());
        return null;
    }

    Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 参数校验
     *
     * @param bo 查询条件
     */
    MonitorWarningDataResult fetchMonitorWarningData(RiskWarningRecordBo bo) {
        // 构建请求参数（只传开始时间和结束时间）

        return parallelFetchDelegate.fetchMonitorWarningData(bo);
    }

    /**
     * 从本地库查询监测预警记录（用于统计概览），时间条件与落库表 {@code data_tp_warning_event} 一致，
     * 区域条件与三方接口返回后的「地址含恩施」过滤对齐。
     */
    MonitorWarningDataResult fetchMonitorWarningDataFromLocal(RiskWarningRecordBo bo) {

        // noinspection SqlResolve

        return parallelFetchDelegate.fetchMonitorWarningDataFromLocal(bo);
    }

    String resolveMonitorStatWarningLevel(String storedWarningLevel) {
        if (StrUtil.isBlank(storedWarningLevel)) {
            return "未知";
        }
        Integer parsed = parseStoredWarningLevel(storedWarningLevel);
        if (parsed == null) {
            return storedWarningLevel;
        }
        String thirdPartyCode = convertToThirdPartyWarningLevel(parsed);
        return StrUtil.blankToDefault(thirdPartyCode, storedWarningLevel);
    }

    /**
     * 与三方监测预警统计口径一致：A=未处置，B=处置中，C=已处置。
     * 已处置：有处置时间或 disposal_status=1；处置中：disposal_status=2，或已有处置类型/处置人但尚无处置时间。
     */
    String mapMonitorWarningProcessStatusLabel(
        Integer disposalStatus,
        Timestamp disposalTime,
        String disposalType,
        String disposalPerson
    ) {
        if (disposalStatus != null && disposalStatus == 2) {
            return "B";
        }
        if (disposalTime != null || (disposalStatus != null && disposalStatus == 1)) {
            return "C";
        }
        if (StrUtil.isNotBlank(disposalType) || StrUtil.isNotBlank(disposalPerson)) {
            return "B";
        }
        return "A";
    }

    static int compareStatMonitorWarningLevelKey(String a, String b) {
        int ra = statMonitorWarningLevelRank(a);
        int rb = statMonitorWarningLevelRank(b);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return Comparator.nullsLast(String::compareTo).compare(a, b);
    }

    /**
     * 统计预警等级的排序，用于排序。(C1是蓝色预警,C4是红色预警)
     */
    static int statMonitorWarningLevelRank(String level) {
        if (level == null) {
            return 99;
        }
        return switch (level) {
            case "C4" -> 4;
            case "C3" -> 3;
            case "C2" -> 2;
            case "C1" -> 1;
            case "未知" -> 8;
            default -> 50;
        };
    }

    static int compareStatMonitorProcessStatusKey(String a, String b) {
        int ra = statMonitorProcessStatusRank(a);
        int rb = statMonitorProcessStatusRank(b);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return Comparator.nullsLast(String::compareTo).compare(a, b);
    }

    static int statMonitorProcessStatusRank(String type) {
        if (type == null) {
            return 99;
        }
        return switch (type) {
            case "A" -> 0;
            case "B" -> 1;
            case "C" -> 2;
            default -> 50;
        };
    }

    /**
     * 拉取并组装预警处置列表数据
     *
     * @return 预警处置列表
     */
    String firstNonBlank(String... values) { return warningDisposalDelegate.firstNonBlank(values); }
    String convertToThirdPartyWarningLevel(Integer warningLevel) { return warningDisposalDelegate.convertToThirdPartyWarningLevel(warningLevel); }
    Integer convertFromThirdPartyWarningLevel(String warningLevel) { return warningDisposalDelegate.convertFromThirdPartyWarningLevel(warningLevel); }

    public DeviceRainfallDisplacementVo getDeviceRainfallAndDisplacement(String deviceId) {

        return statsMiscDelegate.getDeviceRainfallAndDisplacement(deviceId);
    }

    /**
     * 检查三方接口连通性
     */
    public Map<String, Object> checkThirdPartyConnectivity() {

        return statsMiscDelegate.checkThirdPartyConnectivity();
    }

    /**
     * 判断是否为最后一页
     */
    boolean isLastPage(Integer current, Integer pages, int currentPageSize) {
        if (pages != null && current != null) {
            return current >= pages;
        }
        return currentPageSize < THIRD_PARTY_PAGE_SIZE;
    }

    //监测预警数据结果内部类
    /**
     * 在内存中进行分页
     *
     * @param pageQuery 分页查询参数
     * @return 分页后的数据列表
     */
    int resolvePageNum(PageQuery pageQuery) {
        return pageQuery == null || pageQuery.getPageNum() == null || pageQuery.getPageNum() < 1 ? 1 : pageQuery.getPageNum();
    }

    /**
     * 解析分页大小
     */
    int resolvePageSize(PageQuery pageQuery) {
        return pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1 ? 10 : pageQuery.getPageSize();
    }

    /**
     * 对结果列表进行内存分页
     */
    <T> List<T> paginateInMemory(List<T> allRecords, int pageNum, int pageSize) {
        int total = allRecords.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        if (start >= total) {
            return new ArrayList<>();
        }

        return allRecords.subList(start, end);
    }

    /**
     * 统计监测预警概览数据
     */
    public StatThirdWarningVo statMonitorWarning(StatThirdWarningBo bo) {


        return statsMiscDelegate.statMonitorWarning(bo);
    }

    /**
     * 获取实时雨情数据
     */
    public List<LiveRainVo> getLiveRain(String date) {

        return statsMiscDelegate.getLiveRain(date);
    }

}
