/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.task;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorDeviceService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorWarningService;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.ThirdPartyCurveSyncService;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.ThirdPartyWarningRiskLinkageService;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.ThirdPartyWarningSyncContext;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 三方数据入库定时任务
 * <p>
 * 说明：
 * 1. 设备监测曲线、设备在线状态均采用“主任务 + 兜底任务”；
 * 2. 若主任务成功，则兜底任务跳过；
 * 3. 预警事件每小时执行一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdPartySyncScheduledTask {

    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private static final String CURVE_LOCK_PREFIX = "data_tp_curve_lock_test";
    private static final String CURVE_SUCCESS_PREFIX = "data_tp_curve_success_";
    private static final String STATUS_LOCK_PREFIX = "data_tp_status_lock_";
    private static final String STATUS_SUCCESS_PREFIX = "data_tp_status_success_";
    private static final String WARNING_LOCK_PREFIX = "data_tp_warning_lock_";
    private static final String DEFAULT_REGION_CODE = "422801";
    private static final int WARNING_PAGE_SIZE = 200;
    private static final int BATCH_SIZE = 500;
    private static final long HOURLY_TASK_LOCK_MINUTES = 10L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ThirdPartyCurveSyncService thirdPartyCurveSyncService;
    private final IThirdPartyMonitorDeviceService monitorDeviceService;
    private final IThirdPartyMonitorWarningService monitorWarningService;
    private final ThirdPartyWarningRiskLinkageService thirdPartyWarningRiskLinkageService;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * 设备监测曲线主任务（每小时整点）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncDeviceCurvePrimary() {
        runHourlyTask("设备监测曲线", CURVE_LOCK_PREFIX, CURVE_SUCCESS_PREFIX, false);
    }

    /**
     * 设备监测曲线兜底任务（每10分钟检查一次）
     */
    @Scheduled(cron = "0 10/20 * * * ?")
    public void syncDeviceCurveFallback() {
        runHourlyTask("设备监测曲线", CURVE_LOCK_PREFIX, CURVE_SUCCESS_PREFIX, true);
    }

    /**
     * 设备在线状态主任务（每小时整点）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncDeviceStatusPrimary() {
        runHourlyTask("设备在线状态", STATUS_LOCK_PREFIX, STATUS_SUCCESS_PREFIX, false);
    }

    /**
     * 设备在线状态兜底任务（每10分钟检查一次）
     */
    @Scheduled(cron = "0 10/20 * * * ?")
    public void syncDeviceStatusFallback() {
        runHourlyTask("设备在线状态", STATUS_LOCK_PREFIX, STATUS_SUCCESS_PREFIX, true);
    }

    /**
     * 预警事件同步（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncWarningEvent() {
        LocalDateTime now = LocalDateTime.now(BEIJING_ZONE_ID);
        String minuteKey = now.format(MINUTE_FMT);
        String lockKey = WARNING_LOCK_PREFIX + minuteKey;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 4, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("预警事件同步未获取到锁，跳过本次，lockKey={}", lockKey);
            return;
        }
        try {
            syncWarningEventData();
            log.info("预警事件同步任务执行完成，minuteKey={}", minuteKey);
        } catch (Exception e) {
            log.error("预警事件同步任务执行失败，minuteKey={}", minuteKey, e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private void runHourlyTask(String taskName, String lockPrefix, String successPrefix, boolean fallbackMode) {
        LocalDateTime hourTime = LocalDateTime.now(BEIJING_ZONE_ID).withMinute(0).withSecond(0).withNano(0);
        String hourKey = hourTime.format(HOUR_FMT);
        String successKey = successPrefix + hourKey;
        String lockKey = lockPrefix + hourKey;
        Set<String> curveMissingKeys = Collections.emptySet();

        if (fallbackMode) {
            if ("设备监测曲线".equals(taskName)) {
                curveMissingKeys = getMissingCurveKeysForFallback(hourTime, successKey);
                if (curveMissingKeys.isEmpty()) {
                    return;
                }
            } else if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(successKey))) {
                log.info("{}主任务已成功，兜底任务跳过，hourKey={}", taskName, hourKey);
                return;
            }
        }

        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, HOURLY_TASK_LOCK_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("{}未获取到锁，跳过本次，lockKey={}", taskName, lockKey);
            return;
        }

        try {
            if ("设备监测曲线".equals(taskName)) {
                syncDeviceCurveData(fallbackMode ? curveMissingKeys : null);
            } else if ("设备在线状态".equals(taskName)) {
                syncDeviceStatusData();
            }
            log.info("{}{}任务执行完成，hourKey={}", taskName, fallbackMode ? "兜底" : "主", hourKey);
            stringRedisTemplate.opsForValue().set(successKey, "SUCCESS", 2, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("{}{}任务执行失败，hourKey={}，error={}",
                taskName,
                fallbackMode ? "兜底" : "主",
                hourKey,
                e.getMessage());
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockValue)) {
            return;
        }
        String currentLockValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (StrUtil.equals(lockValue, currentLockValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void syncDeviceCurveData(Set<String> targetCurveKeys) {
        thirdPartyCurveSyncService.syncRecentDeviceCurveData(targetCurveKeys);
    }

    private Set<String> getMissingCurveKeysForFallback(LocalDateTime hourTime, String successKey) {
        boolean primarySucceeded = Boolean.TRUE.equals(stringRedisTemplate.hasKey(successKey));
        try {
            Set<String> expectedKeys = thirdPartyCurveSyncService.loadExpectedCurveKeys();
            if (expectedKeys.isEmpty()) {
                log.info("设备监测曲线兜底任务跳过，当前无可校验的监测类型，hourKey={}", hourTime.format(HOUR_FMT));
                return Collections.emptySet();
            }
            Set<String> existingKeys = thirdPartyCurveSyncService.loadExistingCurveKeys(hourTime);
            Set<String> missingKeys = new TreeSet<>(expectedKeys);
            missingKeys.removeAll(existingKeys);
            if (missingKeys.isEmpty()) {
                log.info("设备监测曲线当前整点数据完整，兜底任务跳过，hourKey={}, expectedCount={}",
                    hourTime.format(HOUR_FMT), expectedKeys.size());
                return Collections.emptySet();
            }
            log.info("设备监测曲线{}存在缺失，执行兜底，hourKey={}, missingCount={}",
                primarySucceeded ? "主任务后仍" : "", hourTime.format(HOUR_FMT), missingKeys.size());
            return missingKeys;
        } catch (Exception ex) {
            log.warn("设备监测曲线兜底校验失败，转为执行兜底，hourKey={}", hourTime.format(HOUR_FMT), ex);
            return Collections.emptySet();
        }
    }

    private void syncDeviceStatusData() {
        List<MonitorDeviceRuntimeVo> runtimeVos = monitorDeviceService.listAllMonitorDevicesRuntimeFromRemote(DEFAULT_REGION_CODE);
        final String statusUpsertSql = """
            INSERT INTO data_tp_device_status_current(
                device_id, client_id, monitor_point_id, region_code, status_code, status_name,
                last_online_time, source_update_time, create_date, update_date
            ) VALUES (?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON CONFLICT (device_id) DO UPDATE SET
                client_id = EXCLUDED.client_id,
                monitor_point_id = EXCLUDED.monitor_point_id,
                region_code = EXCLUDED.region_code,
                status_code = EXCLUDED.status_code,
                status_name = EXCLUDED.status_name,
                last_online_time = EXCLUDED.last_online_time,
                source_update_time = EXCLUDED.source_update_time,
                update_date = CURRENT_TIMESTAMP
            """;
        List<Object[]> batchArgs = new ArrayList<>();
        for (MonitorDeviceRuntimeVo vo : runtimeVos) {
            if (StrUtil.isBlank(vo.getId())) {
                continue;
            }
            int statusCode = vo.getOnlineStatus() == null ? 0 : vo.getOnlineStatus();
            String statusName = statusCode == 1 ? "在线" : "离线";
            batchArgs.add(new Object[]{
                vo.getId(),
                vo.getClientId(),
                vo.getMonitoringPointId(),
                DEFAULT_REGION_CODE,
                statusCode,
                statusName,
                vo.getDeviceLatestOnlineTime()
            });
        }
        int saved = executeBatch(statusUpsertSql, batchArgs);
        log.info("设备在线状态同步完成，落库设备数={}", saved);
    }

    private void syncWarningEventData() {
        ThirdPartyWarningDisposalBo bo = new ThirdPartyWarningDisposalBo();
        bo.setRegionCode(DEFAULT_REGION_CODE);
        LocalDateTime syncEndTime = LocalDateTime.now(BEIJING_ZONE_ID);
        LocalDateTime syncStartTime = syncEndTime.minusDays(7);
        int pageNum = 1;
        int saved = 0;
        Map<String, MonitorPointBinding> monitorPointByName = loadMonitorPointBindingsByName();
        Map<String, MonitorPointBinding> monitorPointById = buildMonitorPointBindingsById(monitorPointByName.values());
        Map<String, WarningDeviceBinding> deviceByPointAndName = loadWarningDeviceByPointAndName();
        Map<String, WarningDeviceBinding> deviceByName = loadWarningDeviceByName(deviceByPointAndName.values());
        Map<String, List<WarningDeviceBinding>> devicesByPointId = loadWarningDevicesByPointId(deviceByPointAndName.values());
        final String warningUpsertSql = """
            INSERT INTO data_tp_warning_event(
                warning_id, device_id, client_id, monitor_point_id, monitor_point_name, warning_device_name,
                warning_level, warning_time, disposal_status, disposal_type, disposal_time,
                disposal_person, valid_warning, source_update_time, create_date, update_date
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, ?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON CONFLICT (warning_id) DO UPDATE SET
                device_id = EXCLUDED.device_id,
                client_id = EXCLUDED.client_id,
                monitor_point_id = EXCLUDED.monitor_point_id,
                monitor_point_name = EXCLUDED.monitor_point_name,
                warning_device_name = EXCLUDED.warning_device_name,
                warning_level = EXCLUDED.warning_level,
                warning_time = EXCLUDED.warning_time,
                disposal_status = EXCLUDED.disposal_status,
                disposal_type = EXCLUDED.disposal_type,
                disposal_time = EXCLUDED.disposal_time,
                disposal_person = EXCLUDED.disposal_person,
                valid_warning = EXCLUDED.valid_warning,
                source_update_time = EXCLUDED.source_update_time,
                update_date = CURRENT_TIMESTAMP
            WHERE data_tp_warning_event.device_id IS DISTINCT FROM EXCLUDED.device_id
               OR data_tp_warning_event.client_id IS DISTINCT FROM EXCLUDED.client_id
               OR data_tp_warning_event.monitor_point_id IS DISTINCT FROM EXCLUDED.monitor_point_id
               OR data_tp_warning_event.monitor_point_name IS DISTINCT FROM EXCLUDED.monitor_point_name
               OR data_tp_warning_event.warning_device_name IS DISTINCT FROM EXCLUDED.warning_device_name
               OR data_tp_warning_event.warning_level IS DISTINCT FROM EXCLUDED.warning_level
               OR data_tp_warning_event.warning_time IS DISTINCT FROM EXCLUDED.warning_time
               OR data_tp_warning_event.disposal_status IS DISTINCT FROM EXCLUDED.disposal_status
               OR data_tp_warning_event.disposal_type IS DISTINCT FROM EXCLUDED.disposal_type
               OR data_tp_warning_event.disposal_time IS DISTINCT FROM EXCLUDED.disposal_time
               OR data_tp_warning_event.disposal_person IS DISTINCT FROM EXCLUDED.disposal_person
               OR data_tp_warning_event.valid_warning IS DISTINCT FROM EXCLUDED.valid_warning
               OR data_tp_warning_event.source_update_time IS DISTINCT FROM EXCLUDED.source_update_time
            """;
        while (true) {
            PageQuery pageQuery = new PageQuery(WARNING_PAGE_SIZE, pageNum);
            TableDataInfo<ThirdPartyWarningDisposalVo> pageData = monitorWarningService.getMonitorWarningDisposalFromRemote(
                bo,
                pageQuery,
                syncStartTime,
                syncEndTime
            );
            if (pageData == null || pageData.getData() == null || pageData.getData().isEmpty()) {
                break;
            }
            Set<String> existingWarningIds = loadExistingWarningIds(pageData.getData());
            List<Object[]> batchArgs = new ArrayList<>();
            List<ThirdPartyWarningSyncContext> syncContexts = new ArrayList<>();
            for (ThirdPartyWarningDisposalVo row : pageData.getData()) {
                if (StrUtil.isBlank(row.getWarningId())) {
                    continue;
                }
                Timestamp warningTime = toTimestamp(row.getWarningPublishTime());
                Timestamp disposalTime = toTimestamp(row.getWarningDisposalTime());
                int disposalStatus = disposalTime == null ? 0 : 1;
                String monitorPointId = StrUtil.blankToDefault(row.getMonitoringPointId(), null);
                MonitorPointBinding monitorPointBinding = StrUtil.isBlank(monitorPointId) ? null : monitorPointById.get(monitorPointId);
                if (StrUtil.isBlank(monitorPointId)) {
                    monitorPointBinding = monitorPointByName.get(normalizeKey(row.getMonitorPointName()));
                    monitorPointId = monitorPointBinding == null ? null : monitorPointBinding.pointId();
                }
                WarningDeviceBinding deviceBinding = null;
                if (StrUtil.isBlank(row.getDeviceId()) || StrUtil.isBlank(row.getClientId())) {
                    deviceBinding = resolveWarningDeviceBinding(
                        monitorPointId,
                        row.getWarningDeviceName(),
                        deviceByPointAndName,
                        deviceByName,
                        devicesByPointId
                    );
                }
                Timestamp sourceUpdateTime = disposalTime == null ? warningTime : disposalTime;
                String slopeUnitId = null;
                if (deviceBinding != null && StrUtil.isNotBlank(deviceBinding.slopeUnitId())) {
                    slopeUnitId = deviceBinding.slopeUnitId();
                } else if (monitorPointBinding != null && StrUtil.isNotBlank(monitorPointBinding.slopeUnitId())) {
                    slopeUnitId = monitorPointBinding.slopeUnitId();
                }
                Integer normalizedWarningLevel = row.getWarningLevel();
                batchArgs.add(new Object[]{
                    row.getWarningId(),
                    StrUtil.blankToDefault(row.getDeviceId(), deviceBinding == null ? null : deviceBinding.deviceId()),
                    StrUtil.blankToDefault(row.getClientId(), deviceBinding == null ? null : deviceBinding.clientId()),
                    monitorPointId,
                    row.getMonitorPointName(),
                    row.getWarningDeviceName(),
                    normalizedWarningLevel == null ? null : String.valueOf(normalizedWarningLevel),
                    warningTime,
                    disposalStatus,
                    row.getDisposalType(),
                    disposalTime,
                    row.getDisposalPerson(),
                    row.getValidWarning(),
                    sourceUpdateTime
                });
                syncContexts.add(new ThirdPartyWarningSyncContext(
                    row.getWarningId(),
                    StrUtil.blankToDefault(row.getDeviceId(), deviceBinding == null ? null : deviceBinding.deviceId()),
                    StrUtil.blankToDefault(row.getClientId(), deviceBinding == null ? null : deviceBinding.clientId()),
                    monitorPointId,
                    slopeUnitId,
                    deviceBinding == null ? null : deviceBinding.monitoringTypes(),
                    normalizedWarningLevel,
                    warningTime == null ? null : new Date(warningTime.getTime()),
                    !existingWarningIds.contains(row.getWarningId())
                ));
            }
            saved += executeBatch(warningUpsertSql, batchArgs);
            thirdPartyWarningRiskLinkageService.handleSyncedWarnings(syncContexts);
            if (pageData.getData().size() < WARNING_PAGE_SIZE) {
                break;
            }
            pageNum++;
        }
        log.info("预警事件同步完成，时间窗口=[{}, {}]，落库条数={}", syncStartTime, syncEndTime, saved);
    }

    private Set<String> loadExistingWarningIds(List<ThirdPartyWarningDisposalVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> warningIds = new LinkedHashSet<>();
        for (ThirdPartyWarningDisposalVo row : rows) {
            if (row == null || StrUtil.isBlank(row.getWarningId())) {
                continue;
            }
            warningIds.add(row.getWarningId());
        }
        if (warningIds.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = """
            SELECT warning_id
            FROM data_tp_warning_event
            WHERE warning_id IN (:warningIds)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("warningIds", warningIds);
        List<String> existingIds = namedParameterJdbcTemplate.query(sql, params,
            (rs, rowNum) -> rs.getString("warning_id"));
        return new HashSet<>(existingIds);
    }

    private int executeBatch(String sql, List<Object[]> args) {
        if (args == null || args.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < args.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, args.size());
            List<Object[]> slice = args.subList(i, end);
            int[] result = jdbcTemplate.batchUpdate(sql, slice);
            for (int updated : result) {
                if (updated > 0) {
                    total += updated;
                } else if (updated == Statement.SUCCESS_NO_INFO) {
                    total += 1;
                }
            }
        }
        return total;
    }

    private Map<String, MonitorPointBinding> loadMonitorPointBindingsByName() {
        String sql = """
            SELECT id, monitor_name, slope_unit_id
            FROM v_monitor_point
            WHERE administrative_region_code LIKE ?
              AND pilot_area_1 = 1
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, DEFAULT_REGION_CODE + "%");
        Map<String, MonitorPointBinding> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String pointId = asString(row.get("id"));
            String monitorName = normalizeKey(asString(row.get("monitor_name")));
            if (StrUtil.isNotBlank(pointId) && StrUtil.isNotBlank(monitorName)) {
                result.putIfAbsent(monitorName, new MonitorPointBinding(pointId, asString(row.get("slope_unit_id"))));
            }
        }
        return result;
    }

    private Map<String, MonitorPointBinding> buildMonitorPointBindingsById(Collection<MonitorPointBinding> bindings) {
        Map<String, MonitorPointBinding> result = new HashMap<>();
        if (bindings == null || bindings.isEmpty()) {
            return result;
        }
        for (MonitorPointBinding binding : bindings) {
            if (binding == null || StrUtil.isBlank(binding.pointId())) {
                continue;
            }
            result.putIfAbsent(binding.pointId(), binding);
        }
        return result;
    }

    private Map<String, WarningDeviceBinding> loadWarningDeviceByPointAndName() {
        String sql = """
            SELECT d.id, d.client_id, d.monitoring_point_id, d.device_name, d.monitoring_type, d.slope_unit_id
            FROM v_monitor_device d
            JOIN v_monitor_point p ON p.id = d.monitoring_point_id
            WHERE p.administrative_region_code LIKE ?
              AND p.pilot_area_1 = 1
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, DEFAULT_REGION_CODE + "%");
        Map<String, WarningDeviceBinding> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            WarningDeviceBinding binding = new WarningDeviceBinding(
                asString(row.get("id")),
                asString(row.get("client_id")),
                asString(row.get("monitoring_point_id")),
                asString(row.get("device_name")),
                asString(row.get("monitoring_type")),
                asString(row.get("slope_unit_id"))
            );
            if (StrUtil.isBlank(binding.monitorPointId()) || StrUtil.isBlank(binding.deviceName())) {
                continue;
            }
            result.putIfAbsent(binding.monitorPointId() + "|" + normalizeKey(binding.deviceName()), binding);
        }
        return result;
    }

    private Map<String, WarningDeviceBinding> loadWarningDeviceByName(Collection<WarningDeviceBinding> bindings) {
        Map<String, WarningDeviceBinding> result = new HashMap<>();
        for (WarningDeviceBinding binding : bindings) {
            if (binding == null || StrUtil.isBlank(binding.deviceName())) {
                continue;
            }
            result.putIfAbsent(normalizeKey(binding.deviceName()), binding);
        }
        return result;
    }

    private Map<String, List<WarningDeviceBinding>> loadWarningDevicesByPointId(Collection<WarningDeviceBinding> bindings) {
        Map<String, List<WarningDeviceBinding>> result = new HashMap<>();
        for (WarningDeviceBinding binding : bindings) {
            if (binding == null || StrUtil.isBlank(binding.monitorPointId())) {
                continue;
            }
            result.computeIfAbsent(binding.monitorPointId(), key -> new ArrayList<>()).add(binding);
        }
        return result;
    }

    private WarningDeviceBinding resolveWarningDeviceBinding(String monitorPointId, String warningDeviceName,
                                                             Map<String, WarningDeviceBinding> deviceByPointAndName,
                                                             Map<String, WarningDeviceBinding> deviceByName,
                                                             Map<String, List<WarningDeviceBinding>> devicesByPointId) {
        String normalizedDeviceName = normalizeKey(warningDeviceName);
        if (StrUtil.isNotBlank(monitorPointId) && StrUtil.isNotBlank(normalizedDeviceName)) {
            WarningDeviceBinding precise = deviceByPointAndName.get(monitorPointId + "|" + normalizedDeviceName);
            if (precise != null) {
                return precise;
            }
        }
        if (StrUtil.isNotBlank(normalizedDeviceName)) {
            WarningDeviceBinding byName = deviceByName.get(normalizedDeviceName);
            if (byName != null) {
                return byName;
            }
        }
        if (StrUtil.isNotBlank(monitorPointId)) {
            List<WarningDeviceBinding> bindings = devicesByPointId.get(monitorPointId);
            if (bindings != null && bindings.size() == 1) {
                return bindings.getFirst();
            }
        }
        return null;
    }

    private String normalizeKey(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toLowerCase(Locale.ROOT);
    }


    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Timestamp toTimestamp(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Timestamp.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private record WarningDeviceBinding(
        String deviceId,
        String clientId,
        String monitorPointId,
        String deviceName,
        String monitoringTypes,
        String slopeUnitId
    ) {
    }

    private record MonitorPointBinding(
        String pointId,
        String slopeUnitId
    ) {
    }

}

