/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ThirdPartyWarningRiskIncreaseSimulateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ThirdPartyWarningRiskIncreaseSimulateVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ThirdPartyWarningRiskReplayVo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentWarningRelation;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentWarningRelationService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

/**
 * 第三方预警驱动动态风险联动服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyWarningRiskLinkageService {

    private static final int EXTREME_RISK_LEVEL = 4;
    private static final int TASK_TRIGGER_MIN_RISK_LEVEL = 3;
    private static final double YL_RAINFALL_COMPARE_OFFSET = 30D;
    private static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final List<String> RAINFALL_VALUE_KEYS = List.of(
        "rainfall",
        "rain",
        "yl",
        "value",
        "rainfallValue",
        "rainValue",
        "ylValue",
        "drp",
        "p",
        "pre"
    );

    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final IDzRiskAssessmentWarningRelationService relationService;
    private final IDzTaskDistListService dzTaskDistListService;
    private final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;
    private final IDzAutomationService dzAutomationService;
    private final ThirdPartyWarningRiskLinkageProps riskLinkageProps;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public ThirdPartyWarningRiskReplayVo replayTodayWarnings() {
        long beginTimeMs = System.currentTimeMillis();
        LocalDate today = LocalDate.now(BEIJING_ZONE_ID);
        LocalDateTime windowStart = today.atStartOfDay();
        LocalDateTime windowEnd = windowStart.plusDays(1);
        List<ThirdPartyWarningSyncContext> contexts = loadSyncedWarningContexts(windowStart, windowEnd);
        doHandleSyncedWarnings(contexts);
        long endTimeMs = System.currentTimeMillis();

        ThirdPartyWarningRiskReplayVo result = new ThirdPartyWarningRiskReplayVo();
        result.setReplayDate(today.toString());
        result.setStartTime(windowStart.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli());
        result.setEndTime(windowEnd.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli());
        result.setWarningCount(contexts.size());
        result.setTotalDurationMs(endTimeMs - beginTimeMs);
        log.info("按今日预警表数据重放风险联动完成，date={}, warningCount={}, durationMs={}",
            today, contexts.size(), result.getTotalDurationMs());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleSyncedWarnings(List<ThirdPartyWarningSyncContext> contexts) {
        doHandleSyncedWarnings(contexts);
    }

    @Transactional(rollbackFor = Exception.class)
    public ThirdPartyWarningRiskIncreaseSimulateVo simulateRiskIncrease(ThirdPartyWarningRiskIncreaseSimulateBo bo) {
        if (bo == null || StrUtil.isBlank(bo.getSlopeUnitId()) || bo.getIncreaseLevel() == null || bo.getIncreaseLevel() <= 0) {
            throw new ServiceException("模拟参数不完整");
        }
        DzRiskAssessment assessment = loadLatestRiskAssessmentForToday(bo.getSlopeUnitId());
        if (assessment == null || assessment.getId() == null) {
            throw new ServiceException("未找到该斜坡单元今日风险评估记录");
        }
        int currentLevel = assessment.getDynamicRiskLevel() == null ? 0 : assessment.getDynamicRiskLevel();
        int updatedLevel = currentLevel + bo.getIncreaseLevel();
        if (updatedLevel > EXTREME_RISK_LEVEL) {
            throw new ServiceException("当前风险等级" + currentLevel + "，修改后的等级为" + updatedLevel + "，超过红色等级，本次更新失败");
        }

        dzRiskAssessmentMapper.update(
            null,
            Wrappers.<DzRiskAssessment>lambdaUpdate()
                .eq(DzRiskAssessment::getId, assessment.getId())
                .set(DzRiskAssessment::getDynamicRiskLevel, updatedLevel)
                .set(DzRiskAssessment::getRiskLevel, updatedLevel)
        );
        taskProcessChainSummaryService.refreshDynamicRiskLevelByRiskAssessment(assessment.getId());
        boolean shouldTriggerTask = isDynamicRiskLevelIncreased(currentLevel, updatedLevel);
        if (shouldTriggerTask) {
            scheduleMonitorWarningTaskUpsert(assessment.getId());
        }

        ThirdPartyWarningRiskIncreaseSimulateVo result = new ThirdPartyWarningRiskIncreaseSimulateVo();
        result.setRiskAssessmentId(assessment.getId());
        result.setSlopeUnitId(assessment.getSlopeUnitId());
        result.setOriginalDynamicRiskLevel(currentLevel);
        result.setUpdatedDynamicRiskLevel(updatedLevel);
        result.setTriggeredMonitorWarningTask(shouldTriggerTask);
        return result;
    }

    private void doHandleSyncedWarnings(List<ThirdPartyWarningSyncContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return;
        }
        Map<String, ThirdPartyWarningSyncContext> contextByWarningId = new LinkedHashMap<>();
        for (ThirdPartyWarningSyncContext context : contexts) {
            if (context == null || StrUtil.isBlank(context.warningId())) {
                continue;
            }
            contextByWarningId.putIfAbsent(context.warningId(), context);
        }
        if (contextByWarningId.isEmpty()) {
            return;
        }

        Map<String, SyncedWarningRow> warningRowMap = loadSyncedWarningRows(contextByWarningId.keySet());
        Map<Long, DzRiskAssessment> assessmentMap = new HashMap<>();
        Map<Long, List<AssessmentSyncItem>> itemsByAssessmentId = new LinkedHashMap<>();

        for (ThirdPartyWarningSyncContext context : contextByWarningId.values()) {
            SyncedWarningRow warningRow = warningRowMap.get(context.warningId());
            if (warningRow == null) {
                continue;
            }
            if (StrUtil.isBlank(context.slopeUnitId())) {
                log.warn("第三方预警风险联动跳过，未找到斜坡单元，warningId={}, deviceId={}, monitorPointId={}",
                    context.warningId(), context.deviceId(), context.monitorPointId());
                continue;
            }
            DzRiskAssessment assessment = loadLatestRiskAssessment(context.slopeUnitId(), warningRow.warningTime(), context.warningTime());
            if (assessment == null || assessment.getId() == null) {
                log.warn("第三方预警风险联动跳过，未找到风险评估记录，warningId={}, slopeUnitId={}",
                    context.warningId(), context.slopeUnitId());
                continue;
            }
            DzRiskAssessmentWarningRelation relation = relationService.saveOrUpdateRelation(
                warningRow.eventId(),
                assessment.getId(),
                context.slopeUnitId(),
                warningRow.disposalType(),
                warningRow.disposalTime(),
                warningRow.validWarning()
            );
            assessmentMap.putIfAbsent(assessment.getId(), assessment);
            itemsByAssessmentId.computeIfAbsent(assessment.getId(), key -> new ArrayList<>())
                .add(new AssessmentSyncItem(relation, warningRow, context));
        }

        for (Map.Entry<Long, List<AssessmentSyncItem>> entry : itemsByAssessmentId.entrySet()) {
            DzRiskAssessment assessment = assessmentMap.get(entry.getKey());
            if (assessment == null) {
                continue;
            }
            syncSingleAssessment(assessment, entry.getValue());
        }
    }

    private List<ThirdPartyWarningSyncContext> loadSyncedWarningContexts(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT e.warning_id,
                   e.device_id,
                   e.client_id,
                   e.monitor_point_id,
                   COALESCE(d.slope_unit_id, p.slope_unit_id) AS slope_unit_id,
                   d.monitoring_type,
                   e.warning_level,
                   e.warning_time
            FROM data_tp_warning_event e
            LEFT JOIN v_monitor_device d ON d.id = e.device_id
            LEFT JOIN v_monitor_point p ON p.id = e.monitor_point_id
            WHERE e.warning_id IS NOT NULL
              AND e.warning_time IS NOT NULL
              AND e.warning_time >= :startTime
              AND e.warning_time < :endTime
            ORDER BY e.warning_time ASC, e.id ASC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startTime", java.sql.Timestamp.valueOf(startTime))
            .addValue("endTime", java.sql.Timestamp.valueOf(endTime));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new ThirdPartyWarningSyncContext(
            rs.getString("warning_id"),
            rs.getString("device_id"),
            rs.getString("client_id"),
            rs.getString("monitor_point_id"),
            rs.getString("slope_unit_id"),
            rs.getString("monitoring_type"),
            parseInteger(rs.getString("warning_level")),
            rs.getTimestamp("warning_time"),
            false
        ));
    }

    private void syncSingleAssessment(DzRiskAssessment assessment, List<AssessmentSyncItem> syncItems) {
        if (assessment == null || assessment.getId() == null) {
            return;
        }
        if (isExtremeDynamicRiskLevel(assessment.getDynamicRiskLevel())) {
            clearTempRiskLevelIfNeeded(assessment);
            log.info("设备预警风险联动跳过，斜坡单元动态风险等级已为最高级, riskAssessmentId={}, slopeUnitId={}, dynamicRiskLevel={}",
                assessment.getId(), assessment.getSlopeUnitId(), assessment.getDynamicRiskLevel());
            return;
        }
        Integer originalDynamicRiskLevel = assessment.getDynamicRiskLevel();
        Integer originalTempRiskLevel = assessment.getTemDynamicRiskLevel();
        boolean disposalChanged = false;
        boolean shouldPromoteDynamicRisk = false;
        List<LinkedWarningRow> justProcessedDisposedValidWarnings = new ArrayList<>();
        for (AssessmentSyncItem item : syncItems) {
            if (item == null || item.relation() == null || item.warningRow() == null) {
                continue;
            }
            if (item.context() != null && item.context().newWarning()) {
                if (item.warningRow().disposalTime() != null) {
                    disposalChanged = true;
                    if (Objects.equals(item.warningRow().validWarning(), 1)) {
                        shouldPromoteDynamicRisk = true;
                        justProcessedDisposedValidWarnings.add(toLinkedWarningRow(item));
                    }
                    relationService.updateProcessedState(
                        item.relation().getId(),
                        item.warningRow().disposalType(),
                        item.warningRow().disposalTime(),
                        item.warningRow().validWarning()
                    );
                }
                continue;
            }
            if (!hasDisposalChanged(item.relation(), item.warningRow())) {
                continue;
            }
            disposalChanged = true;
            if (Objects.equals(item.warningRow().validWarning(), 1)) {
                shouldPromoteDynamicRisk = true;
                if (item.warningRow().disposalTime() != null) {
                    justProcessedDisposedValidWarnings.add(toLinkedWarningRow(item));
                }
            }
            relationService.updateProcessedState(
                item.relation().getId(),
                item.warningRow().disposalType(),
                item.warningRow().disposalTime(),
                item.warningRow().validWarning()
            );
        }

        if (disposalChanged) {
            if (shouldPromoteDynamicRisk) {
                Integer promotedDynamicRiskLevel = assessment.getTemDynamicRiskLevel();
                if (!justProcessedDisposedValidWarnings.isEmpty()) {
                    promotedDynamicRiskLevel = calculateTempRiskLevel(assessment, justProcessedDisposedValidWarnings);
                }
                if (promotedDynamicRiskLevel != null) {
                    assessment.setDynamicRiskLevel(promotedDynamicRiskLevel);
                }
            }
            assessment.setTemDynamicRiskLevel(null);
        }

        Integer recalculatedTempRiskLevel = calculateTempRiskLevel(assessment);
        assessment.setTemDynamicRiskLevel(recalculatedTempRiskLevel);

        boolean dynamicRiskChanged = !Objects.equals(originalDynamicRiskLevel, assessment.getDynamicRiskLevel());
        boolean tempRiskChanged = !Objects.equals(originalTempRiskLevel, assessment.getTemDynamicRiskLevel());
        if (dynamicRiskChanged || tempRiskChanged) {
            if (dynamicRiskChanged) {
                assessment.setRiskLevel(assessment.getDynamicRiskLevel());
            }
            dzRiskAssessmentMapper.update(
                null,
                Wrappers.<DzRiskAssessment>lambdaUpdate()
                    .eq(DzRiskAssessment::getId, assessment.getId())
                    .set(dynamicRiskChanged, DzRiskAssessment::getDynamicRiskLevel, assessment.getDynamicRiskLevel())
                    .set(dynamicRiskChanged, DzRiskAssessment::getRiskLevel, assessment.getRiskLevel())
                    .set(tempRiskChanged, DzRiskAssessment::getTemDynamicRiskLevel, assessment.getTemDynamicRiskLevel())
            );
            if (dynamicRiskChanged) {
                taskProcessChainSummaryService.refreshDynamicRiskLevelByRiskAssessment(assessment.getId());
            }
        }
        if (isDynamicRiskLevelIncreased(originalDynamicRiskLevel, assessment.getDynamicRiskLevel())
            || shouldEnsureMonitorWarningTask(assessment, syncItems)) {
            scheduleMonitorWarningTaskUpsert(assessment.getId());
        }
    }

    private boolean isDynamicRiskLevelIncreased(Integer originalDynamicRiskLevel, Integer currentDynamicRiskLevel) {
        if (currentDynamicRiskLevel == null) {
            return false;
        }
        if (currentDynamicRiskLevel < TASK_TRIGGER_MIN_RISK_LEVEL || currentDynamicRiskLevel > EXTREME_RISK_LEVEL) {
            return false;
        }
        int original = originalDynamicRiskLevel == null ? 0 : originalDynamicRiskLevel;
        int threshold = Math.max(1, Objects.requireNonNullElse(riskLinkageProps.getDynamicRiskLevelIncreaseThreshold(), 1));
        return currentDynamicRiskLevel - original >= threshold;
    }

    private boolean shouldEnsureMonitorWarningTask(DzRiskAssessment assessment, List<AssessmentSyncItem> syncItems) {
        if (assessment == null || assessment.getId() == null || assessment.getDynamicRiskLevel() == null) {
            return false;
        }
        if (assessment.getDynamicRiskLevel() < TASK_TRIGGER_MIN_RISK_LEVEL
            || isExtremeDynamicRiskLevel(assessment.getDynamicRiskLevel())) {
            return false;
        }
        if (syncItems == null || syncItems.isEmpty()) {
            return false;
        }
        return syncItems.stream()
                        .filter(Objects::nonNull)
                        .map(AssessmentSyncItem::warningRow)
                        .filter(Objects::nonNull)
                        .anyMatch(row -> row.disposalTime() != null && Objects.equals(row.validWarning(), 1));
    }

    private boolean isExtremeDynamicRiskLevel(Integer dynamicRiskLevel) {
        return dynamicRiskLevel != null && dynamicRiskLevel >= EXTREME_RISK_LEVEL;
    }

    private void clearTempRiskLevelIfNeeded(DzRiskAssessment assessment) {
        if (assessment == null || assessment.getId() == null || assessment.getTemDynamicRiskLevel() == null) {
            return;
        }
        dzRiskAssessmentMapper.update(
            null,
            Wrappers.<DzRiskAssessment>lambdaUpdate()
                .eq(DzRiskAssessment::getId, assessment.getId())
                .set(DzRiskAssessment::getTemDynamicRiskLevel, null)
        );
        assessment.setTemDynamicRiskLevel(null);
    }

    private DzRiskAssessment loadLatestRiskAssessmentForToday(String slopeUnitId) {
        if (StrUtil.isBlank(slopeUnitId)) {
            return null;
        }
        String trimmedSlopeUnitId = slopeUnitId.trim();
        String normalizedSlopeUnitId = normalizeSlopeUnitId(trimmedSlopeUnitId);
        List<String> slopeUnitIds = Objects.equals(trimmedSlopeUnitId, normalizedSlopeUnitId)
            ? List.of(trimmedSlopeUnitId)
            : List.of(trimmedSlopeUnitId, normalizedSlopeUnitId);
        Date now = new Date();
        return dzRiskAssessmentMapper.selectOne(Wrappers.<DzRiskAssessment>lambdaQuery()
            .in(DzRiskAssessment::getSlopeUnitId, slopeUnitIds)
            .ge(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(now))
            .le(DzRiskAssessment::getCreateDate, DateUtil.endOfDay(now))
            .orderByDesc(DzRiskAssessment::getCreateDate)
            .orderByDesc(DzRiskAssessment::getId)
            .last("limit 1"));
    }

    private String normalizeSlopeUnitId(String slopeUnitId) {
        if (StrUtil.isBlank(slopeUnitId)) {
            return slopeUnitId;
        }
        String trimmed = slopeUnitId.trim();
        if (!trimmed.matches("\\d+")) {
            return trimmed;
        }
        return String.format("%4s", trimmed).replace(' ', '0');
    }

    private void scheduleMonitorWarningTaskUpsert(Long riskAssessmentId) {
        if (riskAssessmentId == null) {
            return;
        }
        Runnable action = () -> {
            try {
                boolean autoPushTask = resolveAutoPushMonitorWarningTaskEnabled();
                boolean sendTaskSms = resolveTaskSmsEnabledForAutomation();
                Integer count = dzTaskDistListService.upsertMonitorWarningTaskByRiskId(riskAssessmentId, autoPushTask, sendTaskSms);
                log.info("预警联动监测预警任务处理完成, riskAssessmentId={}, autoPushTask={}, sendTaskSms={}, count={}",
                    riskAssessmentId, autoPushTask, sendTaskSms, count);
            } catch (Exception e) {
                log.error("预警联动监测预警任务处理失败, riskAssessmentId={}", riskAssessmentId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private boolean resolveAutoPushMonitorWarningTaskEnabled() {
        boolean fallback = Boolean.TRUE.equals(riskLinkageProps.getAutoPushTask());
        try {
            return dzAutomationService.isWarningAutoPushMonitorTaskEnabled(fallback);
        } catch (Exception e) {
            log.warn("读取预警联动监测预警任务自动推送配置失败，使用配置默认值继续生成任务, fallback={}", fallback, e);
            return fallback;
        }
    }

    private boolean resolveTaskSmsEnabledForAutomation() {
        try {
            return dzAutomationService.isTaskSmsEnabledForAutomation();
        } catch (Exception e) {
            log.warn("读取普通任务短信自动发送配置失败，监测预警即时自动推送改为不发送短信", e);
            return false;
        }
    }

    private Integer calculateTempRiskLevel(DzRiskAssessment assessment) {
        return calculateTempRiskLevel(assessment, Collections.emptyList());
    }

    private Integer calculateTempRiskLevel(DzRiskAssessment assessment, List<LinkedWarningRow> extraWarnings) {
        if (assessment == null || assessment.getId() == null || StrUtil.isBlank(assessment.getSlopeUnitId())) {
            return null;
        }
        List<LinkedWarningRow> linkedWarnings = new ArrayList<>(loadLinkedWarningRows(assessment.getId()));
        if (extraWarnings != null && !extraWarnings.isEmpty()) {
            linkedWarnings.addAll(extraWarnings);
        }
        if (linkedWarnings.isEmpty()) {
            return null;
        }

        LocalDate assessmentDate = toLocalDate(assessment.getCreateDate());
        DirectExtremeRiskSummary directExtremeRiskSummary = resolveDirectExtremeRiskForGpJs(
            assessmentDate,
            assessment.getSlopeUnitId(),
            linkedWarnings
        );
        boolean gpStepUp = false;
        boolean jsStepUp = false;
        boolean ylRainfallValidationPassed = false;

        for (LinkedWarningRow warning : linkedWarnings) {
            if (!isEligibleForSameDaySameSlopeUnitRiskCalculation(assessmentDate, assessment.getSlopeUnitId(), warning)) {
                continue;
            }
            if (warning.containsType("L1_GP")) {
                gpStepUp |= warning.isBlueOrYellow();
            }
            if (warning.containsType("L1_JS")) {
                jsStepUp |= warning.isBlueOrYellow();
            }
            if (warning.containsYlType()) {
                ylRainfallValidationPassed |= passYlRainfallValidation(assessment, warning);
            }
        }

        Integer tempRiskLevel = null;
        if (directExtremeRiskSummary.shouldPromoteToExtreme()) {
            tempRiskLevel = EXTREME_RISK_LEVEL;
        }
        tempRiskLevel = applyStepRule(tempRiskLevel, assessment.getDynamicRiskLevel(), gpStepUp);
        tempRiskLevel = applyStepRule(tempRiskLevel, assessment.getDynamicRiskLevel(), jsStepUp);
        if (ylRainfallValidationPassed) {
            tempRiskLevel = increaseRiskLevel(tempRiskLevel, assessment.getDynamicRiskLevel());
        }
        return tempRiskLevel;
    }

    static DirectExtremeRiskSummary resolveDirectExtremeRiskForGpJs(LocalDate assessmentDate,
                                                                    String assessmentSlopeUnitId,
                                                                    Collection<LinkedWarningRow> linkedWarnings) {
        if (assessmentDate == null || StrUtil.isBlank(assessmentSlopeUnitId) || linkedWarnings == null || linkedWarnings.isEmpty()) {
            return DirectExtremeRiskSummary.empty();
        }
        Set<String> gpDeviceKeys = new HashSet<>();
        Set<String> jsDeviceKeys = new HashSet<>();
        boolean gpHighLevel = false;
        boolean jsHighLevel = false;
        for (LinkedWarningRow warning : linkedWarnings) {
            if (!isEligibleForSameDaySameSlopeUnitRiskCalculation(assessmentDate, assessmentSlopeUnitId, warning)) {
                continue;
            }
            if (warning.containsType("L1_GP")) {
                if (StrUtil.isNotBlank(warning.deviceKey())) {
                    gpDeviceKeys.add(warning.deviceKey());
                }
                gpHighLevel |= warning.isOrangeOrAbove();
            }
            if (warning.containsType("L1_JS")) {
                if (StrUtil.isNotBlank(warning.deviceKey())) {
                    jsDeviceKeys.add(warning.deviceKey());
                }
                jsHighLevel |= warning.isOrangeOrAbove();
            }
        }
        return new DirectExtremeRiskSummary(gpHighLevel, jsHighLevel, gpDeviceKeys.size(), jsDeviceKeys.size());
    }

    static boolean isEligibleForSameDaySameSlopeUnitRiskCalculation(LocalDate assessmentDate,
                                                                    String assessmentSlopeUnitId,
                                                                    LinkedWarningRow warning) {
        if (assessmentDate == null || StrUtil.isBlank(assessmentSlopeUnitId) || warning == null || !warning.isActive()) {
            return false;
        }
        if (!StrUtil.equals(StrUtil.trim(assessmentSlopeUnitId), StrUtil.trim(warning.slopeUnitId()))) {
            return false;
        }
        LocalDate warningDate = toLocalDate(warning.warningTime());
        return assessmentDate.equals(warningDate);
    }

    private LinkedWarningRow toLinkedWarningRow(AssessmentSyncItem item) {
        String monitoringTypes = item.context() == null ? null : item.context().monitoringTypes();
        return new LinkedWarningRow(
            item.warningRow().warningLevel(),
            null,
            item.warningRow().validWarning(),
            monitoringTypes,
            item.warningRow().warningTime(),
            item.context() == null ? null : item.context().clientId(),
            item.context() == null ? null : item.context().deviceId(),
            item.context() == null ? null : item.context().slopeUnitId()
        );
    }

    private boolean passYlRainfallValidation(DzRiskAssessment assessment, LinkedWarningRow warning) {
        if (assessment == null || warning == null) {
            return false;
        }
        LocalDateTime cutoffTime = toLocalDateTime(warning.warningTime());
        if (cutoffTime == null) {
            cutoffTime = LocalDateTime.now(BEIJING_ZONE_ID);
            log.info("YL预警雨量校验使用当前时间作为统计截止时间，riskAssessmentId={}, slopeUnitId={}",
                assessment.getId(), assessment.getSlopeUnitId());
        }
        LocalDateTime dayStart = cutoffTime.toLocalDate().atStartOfDay();
        LocalDateTime cutoffHour = truncateToHour(cutoffTime);
        String slopeUnitId = StrUtil.blankToDefault(warning.slopeUnitId(), assessment.getSlopeUnitId());
        ForecastRainfallSummary forecastSummary = queryForecastRainfallElapsedToday(slopeUnitId, dayStart, cutoffTime);
        DeviceRainfallSummary deviceSummary = queryDeviceRainfallElapsedToday(warning.clientId(), warning.deviceId(), dayStart, cutoffTime);
        boolean passed = deviceSummary.totalRainfall() > forecastSummary.totalRainfall() + YL_RAINFALL_COMPARE_OFFSET;
        log.info("YL预警雨量校验完成（当天已过去时间+预报完整时段直算/当前时段按小时折算），riskAssessmentId={}, slopeUnitId={}, clientId={}, deviceId={}, dayStart={}, cutoffTime={}, cutoffHour={}, forecastRecordCount={}, forecastSplitHourCount={}, deviceRecordCount={}, deviceRainfall={}, forecastRainfall={}, offset={}, passed={}",
            assessment.getId(), slopeUnitId, warning.clientId(), warning.deviceId(), dayStart, cutoffTime, cutoffHour,
            forecastSummary.rawRecordCount(), forecastSummary.flattenedHourCount(), deviceSummary.recordCount(),
            deviceSummary.totalRainfall(), forecastSummary.totalRainfall(), YL_RAINFALL_COMPARE_OFFSET, passed);
        return passed;
    }

    private ForecastRainfallSummary queryForecastRainfallElapsedToday(String slopeUnitId, LocalDateTime dayStart, LocalDateTime cutoffTime) {
        if (StrUtil.isBlank(slopeUnitId) || dayStart == null || cutoffTime == null) {
            return ForecastRainfallSummary.empty();
        }
        LocalDateTime cutoffHour = truncateToHour(cutoffTime);
        if (cutoffHour.isBefore(dayStart)) {
            return ForecastRainfallSummary.empty();
        }
        String trimmedSlopeUnitId = slopeUnitId.trim();
        String normalizedSlopeUnitId = normalizeSlopeUnitId(trimmedSlopeUnitId);
        List<String> slopeUnitIds = Objects.equals(trimmedSlopeUnitId, normalizedSlopeUnitId)
            ? List.of(trimmedSlopeUnitId)
            : List.of(trimmedSlopeUnitId, normalizedSlopeUnitId);
        String sql = """
            SELECT f.forecast_time,
                   f.rainfall
            FROM data_rainfall_entity_grid_mapping m
            JOIN data_rainfall_forecast_raster_unit f
              ON f.lon = m.forecast_grid_lon
             AND f.lat = m.forecast_grid_lat
            WHERE m.entity_type = 'SLOPE_UNIT'
              AND m.slope_unit_id IN (:slopeUnitIds)
              AND m.forecast_grid_lon IS NOT NULL
              AND m.forecast_grid_lat IS NOT NULL
              AND f.forecast_time >= :startTime
              AND f.forecast_time <= :endTime
            ORDER BY f.forecast_time ASC
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("slopeUnitIds", slopeUnitIds)
            .addValue("startTime", Timestamp.valueOf(dayStart.minusHours(2)))
            .addValue("endTime", Timestamp.valueOf(cutoffHour));
        Map<LocalDateTime, Double> rainfallByForecastTime = new LinkedHashMap<>();
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            Timestamp forecastTime = rs.getTimestamp("forecast_time");
            if (forecastTime == null) {
                return;
            }
            rainfallByForecastTime.merge(
                forecastTime.toLocalDateTime(),
                defaultRainfall(rs.getObject("rainfall") instanceof Number number ? number.doubleValue() : null),
                Double::sum
            );
        });
        if (rainfallByForecastTime.isEmpty()) {
            log.debug("YL预警雨量校验未查询到斜坡单元当天预报雨量，slopeUnitId={}, dayStart={}, cutoffTime={}, cutoffHour={}",
                slopeUnitId, dayStart, cutoffTime, cutoffHour);
            return ForecastRainfallSummary.empty();
        }
        FlattenedForecastRainfallSummary flattenedSummary =
            flattenForecastRainfallByHour(rainfallByForecastTime, dayStart, cutoffHour);
        return new ForecastRainfallSummary(flattenedSummary.totalRainfall(), rainfallByForecastTime.size(), flattenedSummary.flattenedHourCount());
    }

    static FlattenedForecastRainfallSummary flattenForecastRainfallByHour(Map<LocalDateTime, Double> rainfallByForecastTime,
                                                                          LocalDateTime dayStart,
                                                                          LocalDateTime cutoffHour) {
        if (rainfallByForecastTime == null || rainfallByForecastTime.isEmpty() || dayStart == null || cutoffHour == null
            || cutoffHour.isBefore(dayStart)) {
            return FlattenedForecastRainfallSummary.empty();
        }
        double totalRainfall = 0D;
        int flattenedHourCount = 0;
        LocalDateTime cutoffExclusive = cutoffHour.plusHours(1);
        for (Map.Entry<LocalDateTime, Double> entry : rainfallByForecastTime.entrySet()) {
            LocalDateTime forecastTime = entry.getKey();
            if (forecastTime == null) {
                continue;
            }
            LocalDateTime periodStart = forecastTime;
            LocalDateTime periodEnd = forecastTime.plusHours(3);
            LocalDateTime overlapStart = periodStart.isBefore(dayStart) ? dayStart : periodStart;
            LocalDateTime overlapEnd = periodEnd.isAfter(cutoffExclusive) ? cutoffExclusive : periodEnd;
            long overlapHours = java.time.Duration.between(overlapStart, overlapEnd).toHours();
            if (overlapHours <= 0) {
                continue;
            }
            double rainfall = defaultRainfall(entry.getValue());
            if (overlapHours >= 3) {
                totalRainfall += rainfall;
                continue;
            }
            totalRainfall += rainfall / 3D * overlapHours;
            flattenedHourCount += (int) overlapHours;
        }
        return new FlattenedForecastRainfallSummary(totalRainfall, flattenedHourCount);
    }

    private DeviceRainfallSummary queryDeviceRainfallElapsedToday(String clientId, String deviceId, LocalDateTime dayStart, LocalDateTime cutoffTime) {
        if (dayStart == null || cutoffTime == null) {
            return DeviceRainfallSummary.empty();
        }
        List<String> clientIds = Stream.of(clientId, deviceId)
            .filter(StrUtil::isNotBlank)
            .map(StrUtil::trim)
            .distinct()
            .toList();
        if (clientIds.isEmpty()) {
            log.debug("YL预警雨量校验缺少设备标识，dayStart={}, cutoffTime={}", dayStart, cutoffTime);
            return DeviceRainfallSummary.empty();
        }
        String sql = """
            SELECT point_values_json
            FROM data_tp_device_curve_hourly
            WHERE client_id IN (:clientIds)
              AND UPPER(monitor_type) = 'YL'
              AND source_update_time >= :startTime
              AND source_update_time <= :endTime
            ORDER BY source_update_time ASC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("clientIds", clientIds)
            .addValue("startTime", Timestamp.valueOf(dayStart))
            .addValue("endTime", Timestamp.valueOf(cutoffTime));
        List<String> pointValuesJsonList = namedParameterJdbcTemplate.queryForList(sql, params, String.class);
        if (pointValuesJsonList == null || pointValuesJsonList.isEmpty()) {
            log.debug("YL预警雨量校验未查询到设备当天雨量曲线，clientIds={}, dayStart={}, cutoffTime={}",
                clientIds, dayStart, cutoffTime);
            return DeviceRainfallSummary.empty();
        }
        double totalRainfall = 0D;
        for (String pointValuesJson : pointValuesJsonList) {
            totalRainfall += parseRainfallValue(pointValuesJson);
        }
        return new DeviceRainfallSummary(totalRainfall, pointValuesJsonList.size());
    }

    private Double parseRainfallValue(String pointValuesJson) {
        if (StrUtil.isBlank(pointValuesJson)) {
            return 0D;
        }
        try {
            JSONObject jsonObject = JSONUtil.parseObj(pointValuesJson);
            for (String key : RAINFALL_VALUE_KEYS) {
                Double value = parseDoubleValue(getIgnoreCase(jsonObject, key));
                if (value != null) {
                    return value;
                }
            }
            for (String key : jsonObject.keySet()) {
                Double value = parseDoubleValue(jsonObject.get(key));
                if (value != null) {
                    return value;
                }
            }
        } catch (Exception ex) {
            log.warn("解析YL设备雨量曲线点值失败，err={}", ex.getMessage());
        }
        return 0D;
    }

    private Object getIgnoreCase(JSONObject values, String key) {
        if (values == null || values.isEmpty() || StrUtil.isBlank(key)) {
            return null;
        }
        if (values.containsKey(key)) {
            return values.get(key);
        }
        for (String actualKey : values.keySet()) {
            if (StrUtil.equalsIgnoreCase(actualKey, key)) {
                return values.get(actualKey);
            }
        }
        return null;
    }

    private Double parseDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String text = StrUtil.trim(String.valueOf(value));
            return StrUtil.isBlank(text) ? null : Double.parseDouble(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer applyStepRule(Integer currentTempRiskLevel, Integer dynamicRiskLevel, boolean hasStepUpWarning) {
        if (hasStepUpWarning) {
            return increaseRiskLevel(currentTempRiskLevel, dynamicRiskLevel);
        }
        return currentTempRiskLevel;
    }

    private Integer increaseRiskLevel(Integer currentTempRiskLevel, Integer dynamicRiskLevel) {
        int baseRiskLevel = currentTempRiskLevel != null
            ? currentTempRiskLevel
            : (dynamicRiskLevel == null ? 0 : dynamicRiskLevel);
        return Math.min(baseRiskLevel + 1, EXTREME_RISK_LEVEL);
    }

    private boolean hasDisposalChanged(DzRiskAssessmentWarningRelation relation, SyncedWarningRow warningRow) {
        if (relation == null || warningRow == null) {
            return false;
        }
        return !Objects.equals(relation.getLastDisposalType(), warningRow.disposalType())
            || !Objects.equals(relation.getLastDisposalTime(), warningRow.disposalTime())
            || !Objects.equals(relation.getLastValidWarning(), warningRow.validWarning());
    }

    private DzRiskAssessment loadLatestRiskAssessment(String slopeUnitId, Date warningTime, Date fallbackTime) {
        Date baseTime = warningTime != null ? warningTime : fallbackTime;
        if (baseTime == null) {
            baseTime = new Date();
        }
        return dzRiskAssessmentMapper.selectOne(Wrappers.<DzRiskAssessment>lambdaQuery()
            .eq(DzRiskAssessment::getSlopeUnitId, slopeUnitId)
            .ge(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(baseTime))
            .le(DzRiskAssessment::getCreateDate, DateUtil.endOfDay(baseTime))
            .orderByDesc(DzRiskAssessment::getCreateDate)
            .orderByDesc(DzRiskAssessment::getId)
            .last("limit 1"));
    }

    private Map<String, SyncedWarningRow> loadSyncedWarningRows(Collection<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = """
            SELECT id, warning_id, device_id, client_id, warning_level, warning_time, disposal_type, disposal_time, valid_warning
            FROM data_tp_warning_event
            WHERE warning_id IN (:warningIds)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("warningIds", warningIds);
        List<SyncedWarningRow> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new SyncedWarningRow(
            rs.getLong("id"),
            rs.getString("warning_id"),
            rs.getString("device_id"),
            rs.getString("client_id"),
            parseInteger(rs.getString("warning_level")),
            rs.getTimestamp("warning_time"),
            rs.getString("disposal_type"),
            rs.getTimestamp("disposal_time"),
            getNullableInt(rs.getObject("valid_warning"))
        ));
        Map<String, SyncedWarningRow> result = new HashMap<>();
        for (SyncedWarningRow row : rows) {
            if (row == null || StrUtil.isBlank(row.warningId())) {
                continue;
            }
            result.put(row.warningId(), row);
        }
        return result;
    }

    private List<LinkedWarningRow> loadLinkedWarningRows(Long riskAssessmentId) {
        String sql = """
            SELECT e.warning_level,
                   e.warning_time,
                   e.client_id,
                   e.device_id,
                   e.disposal_time,
                   e.valid_warning,
                   d.monitoring_type,
                   COALESCE(d.slope_unit_id, p.slope_unit_id) AS slope_unit_id
            FROM dz_risk_assessment_warning_relation r
            JOIN data_tp_warning_event e ON e.id = r.warning_event_id
            LEFT JOIN v_monitor_device d ON d.id = e.device_id
            LEFT JOIN v_monitor_point p ON p.id = e.monitor_point_id
            WHERE r.risk_assessment_id = :riskAssessmentId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("riskAssessmentId", riskAssessmentId);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new LinkedWarningRow(
            parseInteger(rs.getString("warning_level")),
            rs.getTimestamp("disposal_time"),
            getNullableInt(rs.getObject("valid_warning")),
            rs.getString("monitoring_type"),
            rs.getTimestamp("warning_time"),
            rs.getString("client_id"),
            rs.getString("device_id"),
            rs.getString("slope_unit_id")
        ));
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer getNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return parseInteger(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return date.toInstant().atZone(BEIJING_ZONE_ID).toLocalDateTime();
    }

    private static LocalDateTime truncateToHour(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withMinute(0).withSecond(0).withNano(0);
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return date.toInstant().atZone(BEIJING_ZONE_ID).toLocalDate();
    }

    private static double defaultRainfall(Double rainfall) {
        return rainfall == null ? 0D : rainfall;
    }

    private record AssessmentSyncItem(
        DzRiskAssessmentWarningRelation relation,
        SyncedWarningRow warningRow,
        ThirdPartyWarningSyncContext context
    ) {
    }

    private record SyncedWarningRow(
        Long eventId,
        String warningId,
        String deviceId,
        String clientId,
        Integer warningLevel,
        Date warningTime,
        String disposalType,
        Date disposalTime,
        Integer validWarning
    ) {
    }

    record LinkedWarningRow(
        Integer warningLevel,
        Date disposalTime,
        Integer validWarning,
        String monitoringTypes,
        Date warningTime,
        String clientId,
        String deviceId,
        String slopeUnitId
    ) {
        private boolean isActive() {
            return disposalTime == null && !Objects.equals(validWarning, 0);
        }

        private boolean containsType(String targetType) {
            if (StrUtil.isBlank(monitoringTypes) || StrUtil.isBlank(targetType)) {
                return false;
            }
            return Arrays.stream(monitoringTypes.split(","))
                .map(String::trim)
                .anyMatch(targetType::equalsIgnoreCase);
        }

        private boolean containsYlType() {
            if (StrUtil.isBlank(monitoringTypes)) {
                return false;
            }
            return Arrays.stream(monitoringTypes.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(type -> type.toUpperCase(Locale.ROOT))
                .anyMatch(type -> "YL".equals(type) || type.endsWith("_YL"));
        }

        private String deviceKey() {
            if (StrUtil.isNotBlank(clientId)) {
                return "client:" + StrUtil.trim(clientId);
            }
            if (StrUtil.isNotBlank(deviceId)) {
                return "device:" + StrUtil.trim(deviceId);
            }
            return null;
        }

        private boolean isBlueOrYellow() {
            return warningLevel != null && warningLevel >= 1 && warningLevel <= 2;
        }

        private boolean isOrangeOrAbove() {
            return warningLevel != null && warningLevel >= 3;
        }
    }

    record DirectExtremeRiskSummary(
        boolean gpHighLevel,
        boolean jsHighLevel,
        int gpDeviceCount,
        int jsDeviceCount
    ) {
        boolean shouldPromoteToExtreme() {
            return gpHighLevel || jsHighLevel || gpDeviceCount >= 2 || jsDeviceCount >= 2;
        }

        static DirectExtremeRiskSummary empty() {
            return new DirectExtremeRiskSummary(false, false, 0, 0);
        }
    }

    record ForecastRainfallSummary(
        double totalRainfall,
        int rawRecordCount,
        int flattenedHourCount
    ) {
        private static ForecastRainfallSummary empty() {
            return new ForecastRainfallSummary(0D, 0, 0);
        }
    }

    record DeviceRainfallSummary(
        double totalRainfall,
        int recordCount
    ) {
        private static DeviceRainfallSummary empty() {
            return new DeviceRainfallSummary(0D, 0);
        }
    }

    record FlattenedForecastRainfallSummary(
        double totalRainfall,
        int flattenedHourCount
    ) {
        private static FlattenedForecastRainfallSummary empty() {
            return new FlattenedForecastRainfallSummary(0D, 0);
        }
    }
}
