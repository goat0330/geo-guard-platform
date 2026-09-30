/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyCurveRefreshBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyCurveRefreshVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyDeviceCurveQueryService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.ThirdPartyCurveTypeEnum;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 三方设备监测曲线同步服务
 *
 * @author 12064
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyCurveSyncService {

    private static final String DEFAULT_REGION_CODE = "422801";
    private static final int BATCH_SIZE = 500;
    private static final Path ERROR_LOG_PATH = Path.of("error.log");
    private static final long MAX_HOURLY_POINT_DIFF_MILLIS = 10 * 60 * 1000L;
    private static final Pattern CURVE_MONITOR_TYPE_PATTERN = Pattern.compile("(?i)[A-Z0-9_]+");
    private static final Pattern HTTP_5XX_PATTERN = Pattern.compile("(^|\\D)(5\\d\\d)(\\D|$)");
    private static final String DEVICE_CURVE_HOURLY_TABLE = "data_tp_device_curve_hourly";
    private static final int CURVE_SYNC_PARALLELISM = 20;
    private static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final AtomicInteger CURVE_SYNC_THREAD_SEQ = new AtomicInteger(1);
    private static final ExecutorService CURVE_SYNC_EXECUTOR = Executors.newFixedThreadPool(
        CURVE_SYNC_PARALLELISM,
        runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("curve-sync-fetch-" + CURVE_SYNC_THREAD_SEQ.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    );

    private final JdbcTemplate jdbcTemplate;
    private final IThirdPartyDeviceCurveQueryService deviceCurveQueryService;

    public void syncRecentDeviceCurveData(Set<String> targetCurveKeys) {
        LocalDateTime now = LocalDateTime.now(BEIJING_ZONE_ID);
        LocalDateTime start = now.minusHours(24);
        syncDeviceCurveData(toEpochMillis(start), toEpochMillis(now), targetCurveKeys);
    }

    public ThirdPartyCurveRefreshVo refreshDeviceCurveData(ThirdPartyCurveRefreshBo bo) {
        long refreshStart = System.currentTimeMillis();
        if (bo == null || bo.getStartTime() == null || bo.getEndTime() == null) {
            throw new ServiceException("开始时间和结束时间不能为空");
        }
        if (bo.getStartTime() > bo.getEndTime()) {
            throw new ServiceException("开始时间不能大于结束时间");
        }
        CurveSyncSummary summary = syncDeviceCurveData(bo.getStartTime(), bo.getEndTime(), null);
        ThirdPartyCurveRefreshVo vo = new ThirdPartyCurveRefreshVo();
        vo.setStartTime(bo.getStartTime());
        vo.setEndTime(bo.getEndTime());
        vo.setDeviceCount(summary.deviceCount());
        vo.setTargetCurveTypeCount(summary.targetCurveTypeCount());
        vo.setSavedPointCount(summary.savedPointCount());
        vo.setRequestCount(summary.requestCount());
        vo.setSuccessCount(summary.successCount());
        vo.setFailedCount(summary.failedCount());
        vo.setTimeoutCount(summary.timeoutCount());
        vo.setTimeoutRate(summary.timeoutRate());
        vo.setHttp429Count(summary.http429Count());
        vo.setHttp429Rate(summary.http429Rate());
        vo.setHttp5xxCount(summary.http5xxCount());
        vo.setHttp5xxRate(summary.http5xxRate());
        vo.setAvgRequestDurationMs(summary.avgRequestDurationMs());
        vo.setTotalDurationMs(System.currentTimeMillis() - refreshStart);
        return vo;
    }

    public Set<String> loadExpectedCurveKeys() {
        Set<String> expectedKeys = new HashSet<>();
        for (Map<String, Object> row : loadCurveSyncDevices()) {
            String clientId = asString(row.get("client_id"));
            String monitoringType = asString(row.get("monitoring_type"));
            if (StrUtil.isBlank(clientId) || StrUtil.isBlank(monitoringType)) {
                continue;
            }
            for (String curveType : resolveCurveTypes(monitoringType)) {
                expectedKeys.add(buildCurveKey(clientId, curveType));
            }
        }
        return expectedKeys;
    }

    public Set<String> loadExistingCurveKeys(LocalDateTime hourTime) {
        // 定时任务传入的是北京时间整点，这里需要先换算为库中 UTC 语义的整点再做兜底校验。
        LocalDateTime utcHourTime = toUtcLocalDateTime(hourTime);
        LocalDateTime windowStart = utcHourTime.minusMinutes(MAX_HOURLY_POINT_DIFF_MILLIS / 60000L);
        LocalDateTime windowEnd = utcHourTime.plusMinutes(MAX_HOURLY_POINT_DIFF_MILLIS / 60000L);
        String sql = """
            SELECT client_id, monitor_type
            FROM %s
            WHERE source_update_time >= ?
              AND source_update_time <= ?
            GROUP BY client_id, monitor_type
            """.formatted(DEVICE_CURVE_HOURLY_TABLE);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            sql,
            Timestamp.valueOf(windowStart),
            Timestamp.valueOf(windowEnd)
        );
        Set<String> existingKeys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String clientId = asString(row.get("client_id"));
            String monitorType = asString(row.get("monitor_type"));
            if (StrUtil.isBlank(clientId) || StrUtil.isBlank(monitorType)) {
                continue;
            }
            existingKeys.add(buildCurveKey(clientId, monitorType));
        }
        return existingKeys;
    }

    private LocalDateTime toUtcLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(BEIJING_ZONE_ID).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private CurveSyncSummary syncDeviceCurveData(long startTime, long endTime, Set<String> targetCurveKeys) {
        List<Map<String, Object>> devices = loadCurveSyncDevices();
        CurveSyncMetrics metrics = new CurveSyncMetrics();
        final String curveUpsertSql = """
            INSERT INTO %s(
                client_id, monitor_point_id, monitor_type,
                curve_columns_json, point_values_json,
                source_update_time, create_date
            ) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)
            ON CONFLICT (client_id, monitor_type, source_update_time) DO UPDATE SET
                monitor_point_id = EXCLUDED.monitor_point_id,
                curve_columns_json = EXCLUDED.curve_columns_json,
                point_values_json = EXCLUDED.point_values_json
            """.formatted(DEVICE_CURVE_HOURLY_TABLE);
        List<Future<List<Object[]>>> futures = new ArrayList<>();
        int targetCurveTypeCount = 0;
        for (Map<String, Object> row : devices) {
            String monitorId = asString(row.get("monitor_id"));
            String deviceId = asString(row.get("device_id"));
            String clientId = asString(row.get("client_id"));
            String monitoringType = asString(row.get("monitoring_type"));
            if (StrUtil.isBlank(clientId) || StrUtil.isBlank(monitoringType)) {
                continue;
            }
            List<String> monitorTypes = resolveCurveTypes(monitoringType);
            if (monitorTypes.isEmpty()) {
                continue;
            }
            for (String monitorType : monitorTypes) {
                String curveKey = buildCurveKey(clientId, monitorType);
                if (targetCurveKeys != null && !targetCurveKeys.contains(curveKey)) {
                    continue;
                }
                targetCurveTypeCount++;
                futures.add(CURVE_SYNC_EXECUTOR.submit(() ->
                    buildCurveBatchArgs(row, monitorId, deviceId, clientId, monitorType, startTime, endTime, metrics)
                ));
            }
        }
        List<Object[]> batchArgs = new ArrayList<>();
        for (Future<List<Object[]>> future : futures) {
            try {
                batchArgs.addAll(future.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("设备监测曲线同步线程被中断", ex);
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                log.error("设备监测曲线并行任务执行失败: {}", cause.getMessage(), cause);
            }
        }
        int saved = executeBatch(curveUpsertSql, batchArgs);
        CurveSyncSummary summary = metrics.toSummary(devices.size(), targetCurveTypeCount, saved);
        log.info("设备监测曲线同步完成，设备数={}, 目标类型数={}, 并发上限={}, 时间范围=[{}, {}], 落库点数={}, 请求数={}, 成功数={}, 失败数={}, 超时数={}, 429数={}, 5xx数={}, 平均耗时={}ms, 总请求耗时={}ms",
            summary.deviceCount(), summary.targetCurveTypeCount(), CURVE_SYNC_PARALLELISM, startTime, endTime,
            summary.savedPointCount(), summary.requestCount(), summary.successCount(), summary.failedCount(),
            summary.timeoutCount(), summary.http429Count(), summary.http5xxCount(), summary.avgRequestDurationMs(),
            summary.totalRequestDurationMs());
        return summary;
    }

    private List<Object[]> buildCurveBatchArgs(Map<String, Object> row,
                                               String monitorId,
                                               String deviceId,
                                               String clientId,
                                               String monitorType,
                                               long startTime,
                                               long endTime,
                                               CurveSyncMetrics metrics) {
        ThirdPartyDeviceCureBo bo = new ThirdPartyDeviceCureBo();
        bo.setType(resolveCurveRequestType(monitorType));
        bo.setClientId(clientId);
        bo.setStartTime(startTime);
        bo.setEndTime(endTime);
        ThirdPartyDeviceCureVo cureVo;
        long requestStart = System.currentTimeMillis();
        metrics.requestCount.increment();
        try {
            cureVo = deviceCurveQueryService.queryDeviceCureFromRemote(bo);
            metrics.successCount.increment();
        } catch (Exception ex) {
            metrics.failedCount.increment();
            classifyException(ex, metrics);
            log.warn("设备曲线同步失败，clientId={}, monitorType={}, err={}", clientId, monitorType, ex.getMessage());
            writeFetchErrorLog(monitorId, deviceId, monitorType, ex.getMessage());
            return Collections.emptyList();
        } finally {
            metrics.totalDurationMillis.add(System.currentTimeMillis() - requestStart);
        }
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = normalizeCurveColumns(cureVo.getColumns());
        if (columns.isEmpty() || cureVo.getValues() == null) {
            writeFetchErrorLog(monitorId, deviceId, monitorType, "响应缺少列定义或values为空");
            return Collections.emptyList();
        }
        if (cureVo.getValues().isEmpty()) {
            writeFetchErrorLog(monitorId, deviceId, monitorType, "指定时间段无曲线数据");
            return Collections.emptyList();
        }
        String columnsJson = JSONUtil.toJsonStr(columns);
        Map<Long, CurveCandidate> candidateByHour = selectHourlyCandidates(cureVo.getValues(), columns);
        if (candidateByHour.isEmpty()) {
            writeFetchErrorLog(monitorId, deviceId, monitorType, "指定时间段数据与整点偏差均超过10分钟");
            return Collections.emptyList();
        }
        List<Object[]> result = new ArrayList<>();
        for (CurveCandidate candidate : candidateByHour.values()) {
            result.add(new Object[]{
                clientId,
                asString(row.get("monitor_id")),
                monitorType,
                columnsJson,
                candidate.pointValuesJson(),
                toRawLocalTimestamp(candidate.rawTs())
            });
        }
        return result;
    }

    private void classifyException(Exception ex, CurveSyncMetrics metrics) {
        String message = ex == null ? "" : StrUtil.blankToDefault(ex.getMessage(), "");
        String exceptionName = ex == null ? "" : ex.getClass().getName();
        String combined = (exceptionName + " " + message).toLowerCase(Locale.ROOT);
        if (combined.contains("timeout") || combined.contains("timed out") || combined.contains("超时")) {
            metrics.timeoutCount.increment();
        }
        if (combined.contains("429") || combined.contains("too many requests")) {
            metrics.http429Count.increment();
        }
        if (HTTP_5XX_PATTERN.matcher(combined).find()) {
            metrics.http5xxCount.increment();
        }
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

    private void writeFetchErrorLog(String monitorId, String deviceId, String monitoringType, String reason) {
        String line = String.format(
            "%s monitorId=%s, deviceId=%s, monitoringType=%s, reason=%s%n",
            LocalDateTime.now(BEIJING_ZONE_ID),
            StrUtil.blankToDefault(monitorId, "-"),
            StrUtil.blankToDefault(deviceId, "-"),
            StrUtil.blankToDefault(monitoringType, "-"),
            StrUtil.blankToDefault(reason, "-")
        );
        try {
            Files.writeString(
                ERROR_LOG_PATH,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            log.error("写入error.log失败，monitorId={}, deviceId={}, monitoringType={}", monitorId, deviceId, monitoringType, e);
        }
    }

    private List<Map<String, Object>> loadCurveSyncDevices() {
        String deviceSql = """
            WITH local_points AS (
                SELECT id, monitor_code, monitor_name, administrative_region_code
                FROM v_monitor_point
                WHERE administrative_region_code LIKE ?
                  AND pilot_area_1 = 1
            )
            SELECT p.id AS monitor_id, p.monitor_code, p.monitor_name, p.administrative_region_code,
                   sb.id AS device_id, sb.clientid AS client_id, sb.jclx AS monitoring_type
            FROM local_points p
            LEFT JOIN dzzh.jc_ba10_sb sb ON sb.jcdid::text = p.id::text
            WHERE sb.id IS NOT NULL
            ORDER BY p.id, sb.id
            """;
        return jdbcTemplate.queryForList(deviceSql, DEFAULT_REGION_CODE + "%");
    }

    private String buildCurveKey(String clientId, String curveType) {
        return clientId + "|" + curveType;
    }

    private List<String> resolveCurveTypes(String monitoringType) {
        if (StrUtil.isBlank(monitoringType)) {
            return List.of();
        }
        Set<String> types = new LinkedHashSet<>();
        Matcher matcher = CURVE_MONITOR_TYPE_PATTERN.matcher(monitoringType.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String resolvedType = resolveCurveRequestType(matcher.group());
            if (ThirdPartyCurveTypeEnum.containsCode(resolvedType)) {
                types.add(resolvedType);
            }
        }
        return new ArrayList<>(types);
    }

    private String resolveCurveRequestType(String monitorType) {
        if (StrUtil.isBlank(monitorType)) {
            return monitorType;
        }
        String normalized = StrUtil.trim(monitorType).toUpperCase(Locale.ROOT);
        if (!normalized.contains("_")) {
            return normalized;
        }
        String[] segments = normalized.split("_");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (ThirdPartyCurveTypeEnum.containsCode(segments[i])) {
                return segments[i];
            }
        }
        return segments[segments.length - 1];
    }

    private List<ThirdPartyDeviceCureVo.ColumnInfo> normalizeCurveColumns(List<ThirdPartyDeviceCureVo.ColumnInfo> columns) {
        if (columns == null || columns.isEmpty()) {
            return List.of();
        }
        List<ThirdPartyDeviceCureVo.ColumnInfo> normalized = new ArrayList<>();
        Set<String> usedCodes = new HashSet<>();
        int unnamedIndex = 1;
        for (ThirdPartyDeviceCureVo.ColumnInfo column : columns) {
            if (column == null) {
                continue;
            }
            String code = StrUtil.blankToDefault(StrUtil.trim(column.getCode()), "value" + unnamedIndex++);
            String uniqueCode = code;
            int duplicateIndex = 2;
            while (!usedCodes.add(uniqueCode.toLowerCase(Locale.ROOT))) {
                uniqueCode = code + "_" + duplicateIndex++;
            }
            ThirdPartyDeviceCureVo.ColumnInfo normalizedColumn = new ThirdPartyDeviceCureVo.ColumnInfo();
            normalizedColumn.setCode(uniqueCode);
            normalizedColumn.setName(StrUtil.blankToDefault(StrUtil.trim(column.getName()), uniqueCode));
            normalizedColumn.setUnit(StrUtil.trim(column.getUnit()));
            normalized.add(normalizedColumn);
        }
        return normalized;
    }

    private Map<Long, CurveCandidate> selectHourlyCandidates(List<List<Object>> values,
                                                             List<ThirdPartyDeviceCureVo.ColumnInfo> columns) {
        Map<Long, CurveCandidate> candidateByHour = new HashMap<>();
        if (values == null || values.isEmpty()) {
            return candidateByHour;
        }
        for (List<Object> row : values) {
            if (row == null || row.size() <= 1) {
                continue;
            }
            Long ts = toLong(row.getFirst());
            if (ts == null) {
                continue;
            }
            String pointValuesJson = buildPointValuesJson(row, columns);
            if (pointValuesJson == null) {
                continue;
            }
            long nearestHourTs = nearestHourTs(ts);
            long diff = Math.abs(ts - nearestHourTs);
            if (diff > MAX_HOURLY_POINT_DIFF_MILLIS) {
                continue;
            }
            CurveCandidate candidate = new CurveCandidate(ts, pointValuesJson, diff);
            CurveCandidate existing = candidateByHour.get(nearestHourTs);
            if (existing == null
                || candidate.diffMillis() < existing.diffMillis()
                || (candidate.diffMillis() == existing.diffMillis() && candidate.rawTs() > existing.rawTs())) {
                candidateByHour.put(nearestHourTs, candidate);
            }
        }
        return candidateByHour;
    }

    private String buildPointValuesJson(List<Object> row, List<ThirdPartyDeviceCureVo.ColumnInfo> columns) {
        if (row == null || columns == null || columns.isEmpty()) {
            return null;
        }
        Map<String, Object> pointValues = new LinkedHashMap<>();
        boolean hasData = false;
        for (int i = 0; i < columns.size(); i++) {
            Object value = row.size() > i + 1 ? row.get(i + 1) : null;
            pointValues.put(columns.get(i).getCode(), value);
            if (!isEmptyCurveValue(value)) {
                hasData = true;
            }
        }
        return hasData ? JSONUtil.toJsonStr(pointValues) : null;
    }

    private boolean isEmptyCurveValue(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof CharSequence sequence && StrUtil.isBlank(sequence.toString());
    }

    private long nearestHourTs(long ts) {
        long floor = ts - (ts % 3600000L);
        long ceil = floor + 3600000L;
        long floorDiff = Math.abs(ts - floor);
        long ceilDiff = Math.abs(ceil - ts);
        return floorDiff <= ceilDiff ? floor : ceil;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Timestamp toRawLocalTimestamp(Long tsMillis) {
        if (tsMillis == null) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMillis), ZoneOffset.UTC));
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
    }

    private record CurveCandidate(
        long rawTs,
        String pointValuesJson,
        long diffMillis
    ) {
    }

    private record CurveSyncSummary(
        int deviceCount,
        int targetCurveTypeCount,
        int savedPointCount,
        int requestCount,
        int successCount,
        int failedCount,
        int timeoutCount,
        double timeoutRate,
        int http429Count,
        double http429Rate,
        int http5xxCount,
        double http5xxRate,
        double avgRequestDurationMs,
        long totalRequestDurationMs
    ) {
    }

    private static class CurveSyncMetrics {
        private final LongAdder requestCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failedCount = new LongAdder();
        private final LongAdder timeoutCount = new LongAdder();
        private final LongAdder http429Count = new LongAdder();
        private final LongAdder http5xxCount = new LongAdder();
        private final LongAdder totalDurationMillis = new LongAdder();

        private CurveSyncSummary toSummary(int deviceCount, int targetCurveTypeCount, int savedPointCount) {
            int request = requestCount.intValue();
            int success = successCount.intValue();
            int failed = failedCount.intValue();
            int timeout = timeoutCount.intValue();
            int http429 = http429Count.intValue();
            int http5xx = http5xxCount.intValue();
            double avgDuration = request == 0 ? 0D : totalDurationMillis.doubleValue() / request;
            return new CurveSyncSummary(
                deviceCount,
                targetCurveTypeCount,
                savedPointCount,
                request,
                success,
                failed,
                timeout,
                calcRate(timeout, request),
                http429,
                calcRate(http429, request),
                http5xx,
                calcRate(http5xx, request),
                avgDuration,
                totalDurationMillis.longValue()
            );
        }

        private double calcRate(int numerator, int denominator) {
            if (denominator <= 0) {
                return 0D;
            }
            return (double) numerator / denominator;
        }
    }
}
