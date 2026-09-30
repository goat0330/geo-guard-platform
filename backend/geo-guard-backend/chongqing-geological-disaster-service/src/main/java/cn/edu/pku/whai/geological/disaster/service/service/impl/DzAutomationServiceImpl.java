/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutomationConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AutoModeTraceContext;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPrediction;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationRunResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationStatusVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskPredictionMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskPredictionPushMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSysConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeExecutionTraceService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzReportDisasterService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskPredictionPushService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.ISmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.utils.AiVisionStatusUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 自动化运营 Service。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzAutomationServiceImpl implements IDzAutomationService {

    private static final String PREFIX = "dizai.automation.";
    private static final String KEY_DAILY_PATROL_GENERATE = PREFIX + "task.daily_patrol_generate_enabled";
    private static final String KEY_DAILY_PATROL_GENERATE_TIME = PREFIX + "task.daily_patrol_generate_time";
    private static final String KEY_DAILY_PATROL_PUSH = PREFIX + "task.daily_patrol_auto_push_enabled";
    private static final String KEY_UNPUSHED_PUSH = PREFIX + "task.unpushed_auto_push_enabled";
    private static final String KEY_UNPUSHED_SOURCE_TYPES = PREFIX + "task.unpushed_source_types";
    private static final String KEY_REMINDER_ENABLED = PREFIX + "task.reminder_enabled";
    private static final String KEY_REMINDER_TIME = PREFIX + "task.reminder_time";
    private static final String KEY_REMINDER_SOURCE_TYPES = PREFIX + "task.reminder_source_types";
    private static final String KEY_WARNING_PUSH_MONITOR = PREFIX + "warning.auto_push_monitor_task_enabled";
    private static final String KEY_REPORT_HANDLE = PREFIX + "report.auto_handle_enabled";
    private static final String KEY_REPORT_PUSH = PREFIX + "report.auto_push_task_enabled";
    private static final String KEY_PREDICTION_CREATE_PUSH = PREFIX + "prediction.auto_create_push_enabled";
    private static final String KEY_PREDICTION_PUSH = PREFIX + "prediction.auto_push_enabled";
    private static final String KEY_TASK_SMS = PREFIX + "notice.task_sms_enabled";
    private static final String KEY_SMS_TASK_PUSH = "dizai.sms.scene.task_push.enabled";
    private static final String KEY_MSG_NOTICE = PREFIX + "notice.msg_notice_enabled";
    private static final String KEY_SYNC_RETRY = PREFIX + "sync.retry_enabled";
    private static final String KEY_SYNC_ALERT = PREFIX + "sync.alert_enabled";
    private static final String KEY_UPDATED_BY = PREFIX + "updated_by";
    private static final String KEY_UPDATED_AT = PREFIX + "updated_at";

    private static final String CACHE_KEY = "dizai:automation:config";
    private static final String RUN_LOCK_KEY = "dizai:automation:runner";
    private static final String DAILY_PATROL_GENERATE_LOCK_KEY = "dizai:automation:daily-patrol-generate:lock";
    private static final String DAILY_PATROL_GENERATE_DAY_KEY_PREFIX = "dizai:automation:daily-patrol-generate:";
    private static final String REMINDER_DAY_KEY_PREFIX = "dizai:automation:reminder:";
    private static final String REPORT_FAILED_KEY_PREFIX = "dizai:automation:report:failed:";
    private static final String NOTICE_ONCE_KEY_PREFIX = "dizai:automation:notice:";
    private static final int MAX_TASK_PER_RUN = 50;
    private static final int MAX_REPORT_PER_RUN = 20;
    private static final int MAX_RISK_PER_RUN = 100;
    private static final int MAX_PREDICTION_PER_RUN = 10;
    private static final int AI_RISK_HIGH = 3;
    private static final int AI_RISK_EXTREME = 4;

    private final DzSysConfigMapper sysConfigMapper;
    private final IDzAutoModeService dzAutoModeService;
    private final IDzTaskDistListService dzTaskDistListService;
    private final IDzReportDisasterService dzReportDisasterService;
    private final IDzRiskPredictionPushService dzRiskPredictionPushService;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final DzRiskPredictionMapper dzRiskPredictionMapper;
    private final DzRiskPredictionPushMapper dzRiskPredictionPushMapper;
    private final DzMsgNoticeMapper dzMsgNoticeMapper;
    private final IDzAutoModeExecutionTraceService executionTraceService;
    private final ISmsConfigService smsConfigService;

    @Override
    public AutomationStatusVo getStatus() {
        try {
            AutomationStatusVo cached = RedisUtils.getCacheObject(CACHE_KEY);
            if (cached != null) {
                return attachAutoModeStatus(cached);
            }
        } catch (Exception e) {
            log.warn("自动化运营配置缓存反序列化失败，删除旧缓存并回源重建, key={}", CACHE_KEY, e);
            RedisUtils.deleteObject(CACHE_KEY);
        }
        AutomationStatusVo status = loadStatusFromDb();
        cacheStatus(status);
        return attachAutoModeStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutomationStatusVo updateConfig(AutomationConfigBo bo) {
        if (bo == null) {
            throw new ServiceException("自动化运营配置不能为空");
        }
        applyConfig(bo);
        saveUpdateMeta();
        AutomationStatusVo status = refreshCache();
        refreshSmsConfigAfterCommit();
        return status;
    }

    private void refreshSmsConfigAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            smsConfigService.refreshCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                smsConfigService.refreshCache();
            }
        });
    }

    @Override
    public AutomationRunResultVo runOnce() {
        RLock lock = RedisUtils.getClient().getLock(RUN_LOCK_KEY);
        boolean locked = false;
        AutomationRunResultVo result = new AutomationRunResultVo();
        try {
            locked = lock.tryLock(0, 120, TimeUnit.SECONDS);
            if (!locked) {
                return result;
            }
            AutomationStatusVo status = getStatus();
            runDailyPatrolGenerationIfDue(status, result);
            runPredictionCreateAutomation(status);
            runDailyPatrolAutoPush(status, result);
            if (Boolean.TRUE.equals(status.getAutoModeEnabled())) {
                runTaskAutomation(status, result);
                runReportAutomation(status, result);
                runReportPushAutomation(status, result);
                runPredictionPushAutomation(status, result);
            }
            runSyncHealthAutomation(status, result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("自动化运营获取全局锁被中断", e);
        } catch (Exception e) {
            log.error("自动化运营执行失败", e);
            createNoticeIfEnabled(getStatus(), "自动化运营执行失败", e.getMessage(), "automation");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return result;
    }

    @Override
    public boolean isWarningAutoPushMonitorTaskEnabled(boolean fallback) {
        String configured = getConfigValue(KEY_WARNING_PUSH_MONITOR);
        if (StringUtils.isBlank(configured)) {
            return fallback;
        }
        return Boolean.parseBoolean(configured);
    }

    @Override
    public boolean isTaskSmsEnabledForAutomation() {
        AutomationStatusVo status = getStatus();
        return Boolean.TRUE.equals(status.getAutoModeEnabled()) && Boolean.TRUE.equals(status.getTaskSmsEnabled());
    }

    @Override
    public boolean isDailyPatrolGenerateEnabled() {
        return Boolean.TRUE.equals(getStatus().getDailyPatrolGenerateEnabled());
    }

    @Override
    public boolean runDailyPatrolGenerationIfDue() {
        AutomationRunResultVo result = new AutomationRunResultVo();
        return runDailyPatrolGenerationIfDue(getStatus(), result);
    }

    private void runTaskAutomation(AutomationStatusVo status, AutomationRunResultVo result) {
        boolean sendSms = isTaskSmsEnabled(status);
        if (Boolean.TRUE.equals(status.getUnpushedAutoPushEnabled())) {
            int pushed = runCountAction(actionContext(
                    AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_CODE,
                    AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_NAME,
                    null,
                    null,
                    null,
                    true,
                    detailOf("sourceTypes", status.getUnpushedSourceTypes(), "limit", MAX_TASK_PER_RUN, "sendTaskSms", sendSms)
                ),
                () -> dzTaskDistListService.autoPushUnpushedTasks(status.getUnpushedSourceTypes(), MAX_TASK_PER_RUN, sendSms));
            result.setPushedTaskCount(result.getPushedTaskCount() + pushed);
        }
        if (Boolean.TRUE.equals(status.getReminderEnabled()) && shouldRunReminder(status)) {
            int reminded = runCountAction(actionContext(
                    AutoModeExecutionActions.TASK_AUTO_REMIND_CODE,
                    AutoModeExecutionActions.TASK_AUTO_REMIND_NAME,
                    null,
                    null,
                    null,
                    true,
                    detailOf("sourceTypes", status.getReminderSourceTypes(), "limit", MAX_TASK_PER_RUN)
                ),
                () -> dzTaskDistListService.autoRemindOpenTasks(status.getReminderSourceTypes(), MAX_TASK_PER_RUN));
            result.setRemindedTaskCount(reminded);
        }
    }

    private void runDailyPatrolAutoPush(AutomationStatusVo status, AutomationRunResultVo result) {
        if (status == null || !Boolean.TRUE.equals(status.getDailyPatrolAutoPushEnabled())) {
            return;
        }
        boolean sendSms = isTaskSmsEnabled(status);
        int pushed = runCountAction(actionContext(
                AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_CODE,
                AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_NAME,
                null,
                null,
                null,
                true,
                detailOf("limit", MAX_TASK_PER_RUN, "sendTaskSms", sendSms, "autoModeEnabled", status.getAutoModeEnabled())
            ),
            () -> dzTaskDistListService.autoPushDailyPatrolTasks(MAX_TASK_PER_RUN, sendSms));
        result.setPushedTaskCount(result.getPushedTaskCount() + pushed);
    }

    private boolean isTaskSmsEnabled(AutomationStatusVo status) {
        return status != null
            && Boolean.TRUE.equals(status.getAutoModeEnabled())
            && Boolean.TRUE.equals(status.getTaskSmsEnabled());
    }

    private void runReportAutomation(AutomationStatusVo status, AutomationRunResultVo result) {
        if (!Boolean.TRUE.equals(status.getReportAutoHandleEnabled())) {
            return;
        }
        AutoModeTraceContext context = actionContext(
            AutoModeExecutionActions.REPORT_AUTO_HANDLE_CODE,
            AutoModeExecutionActions.REPORT_AUTO_HANDLE_NAME,
            null,
            null,
            null,
            true,
            detailOf("limit", MAX_REPORT_PER_RUN)
        );
        Long traceId = executionTraceService.start(context);
        int handled = 0;
        int candidateCount = 0;
        int failedCount = 0;
        List<Long> failedReportIds = new ArrayList<>();
        Exception firstFailure = null;
        try {
            List<DzReportDisaster> reports = dzReportDisasterMapper.selectList(
                Wrappers.<DzReportDisaster>lambdaQuery()
                        .in(DzReportDisaster::getStatus, 0, 1)
                        .in(DzReportDisaster::getAiRiskLevel, AI_RISK_HIGH, AI_RISK_EXTREME)
                        .orderByAsc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
                        .last("limit " + MAX_REPORT_PER_RUN)
            );
            candidateCount = reports.size();
            for (DzReportDisaster report : reports) {
                if (report == null || report.getId() == null) {
                    continue;
                }
                if (!isHighOrExtremeAiRisk(report)) {
                    continue;
                }
                try {
                    dzReportDisasterService.handle(report.getId());
                    handled++;
                } catch (Exception e) {
                    log.warn("自动化运营处理报灾失败, reportId={}", report.getId(), e);
                    RedisUtils.setCacheObject(REPORT_FAILED_KEY_PREFIX + report.getId(), e.getMessage(), Duration.ofDays(7));
                    failedCount++;
                    failedReportIds.add(report.getId());
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                    createNoticeIfEnabled(status, "报灾自动处理失败", "报灾ID " + report.getId() + ": " + e.getMessage(), "report:" + report.getId());
                }
            }
            if (firstFailure == null) {
                executionTraceService.success(traceId, context, handled, detailOf("candidateCount", candidateCount));
            } else {
                executionTraceService.fail(traceId, context, firstFailure,
                    detailOf("candidateCount", candidateCount, "handledCount", handled,
                        "failedCount", failedCount, "failedReportIds", failedReportIds));
            }
        } catch (Exception e) {
            executionTraceService.fail(traceId, context, e, detailOf("candidateCount", candidateCount, "handledCount", handled));
            throw e;
        }
        result.setHandledReportCount(handled);
    }

    static boolean isHighOrExtremeAiRisk(DzReportDisaster report) {
        return report != null
            && AiVisionStatusUtils.isCompleted(report)
            && (Objects.equals(report.getAiRiskLevel(), AI_RISK_HIGH)
            || Objects.equals(report.getAiRiskLevel(), AI_RISK_EXTREME));
    }

    private void runReportPushAutomation(AutomationStatusVo status, AutomationRunResultVo result) {
        if (!Boolean.TRUE.equals(status.getReportAutoPushTaskEnabled())) {
            return;
        }
        AutoModeTraceContext context = actionContext(
            AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_CODE,
            AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_NAME,
            null,
            null,
            null,
            true,
            detailOf("limit", MAX_REPORT_PER_RUN, "sendTaskSms", Boolean.TRUE.equals(status.getTaskSmsEnabled()))
        );
        Long traceId = executionTraceService.start(context);
        int totalPushed = 0;
        int candidateCount = 0;
        try {
            List<DzReportDisaster> reports = dzReportDisasterMapper.selectList(
                Wrappers.<DzReportDisaster>lambdaQuery()
                        .in(DzReportDisaster::getStatus, 2, 3)
                        .orderByAsc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
                        .last("limit " + MAX_REPORT_PER_RUN)
            );
            candidateCount = reports.size();
            for (DzReportDisaster report : reports) {
                if (report == null || report.getId() == null) {
                    continue;
                }
                int pushed = dzTaskDistListService.autoPushTasksByReportId(report.getId(), Boolean.TRUE.equals(status.getTaskSmsEnabled()));
                totalPushed += pushed;
                result.setPushedTaskCount(result.getPushedTaskCount() + pushed);
            }
            executionTraceService.success(traceId, context, totalPushed, detailOf("candidateCount", candidateCount));
        } catch (Exception e) {
            executionTraceService.fail(traceId, context, e, detailOf("candidateCount", candidateCount, "pushedCount", totalPushed));
            throw e;
        }
    }

    private void runPredictionCreateAutomation(AutomationStatusVo status) {
        if (Boolean.TRUE.equals(status.getPredictionAutoCreatePushEnabled())) {
            createPredictionPushRecords();
        }
    }

    private void runPredictionPushAutomation(AutomationStatusVo status, AutomationRunResultVo result) {
        if (!Boolean.TRUE.equals(status.getPredictionAutoPushEnabled())) {
            return;
        }
        AutoModeTraceContext context = actionContext(
            AutoModeExecutionActions.RISK_PREDICTION_AUTO_PUSH_CODE,
            AutoModeExecutionActions.RISK_PREDICTION_AUTO_PUSH_NAME,
            null,
            null,
            null,
            true,
            detailOf("limit", MAX_PREDICTION_PER_RUN)
        );
        Long traceId = executionTraceService.start(context);
        int pushed = 0;
        int candidateCount = 0;
        int failedCount = 0;
        List<Long> failedPushIds = new ArrayList<>();
        Exception firstFailure = null;
        try {
            List<DzRiskPredictionPush> pushes = dzRiskPredictionPushMapper.selectList(
                Wrappers.<DzRiskPredictionPush>lambdaQuery()
                        .eq(DzRiskPredictionPush::getStatus, 0)
                        .orderByAsc(DzRiskPredictionPush::getCreateDate, DzRiskPredictionPush::getId)
                        .last("limit " + MAX_PREDICTION_PER_RUN)
            );
            candidateCount = pushes.size();
            for (DzRiskPredictionPush push : pushes) {
                try {
                    dzRiskPredictionPushService.pushReport(push.getId());
                    pushed++;
                } catch (Exception e) {
                    Long pushId = push == null ? null : push.getId();
                    log.warn("自动化运营风险预测推送失败, pushId={}", pushId, e);
                    failedCount++;
                    failedPushIds.add(pushId);
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                    createNoticeIfEnabled(status, "风险预测自动推送失败",
                        "推送ID " + pushId + ": " + e.getMessage(), "prediction:" + pushId);
                }
            }
            if (firstFailure == null) {
                executionTraceService.success(traceId, context, pushed, detailOf("candidateCount", candidateCount));
            } else {
                executionTraceService.fail(traceId, context, firstFailure,
                    detailOf("candidateCount", candidateCount, "pushedCount", pushed,
                        "failedCount", failedCount, "failedPushIds", failedPushIds));
            }
        } catch (Exception e) {
            executionTraceService.fail(traceId, context, e, detailOf("candidateCount", candidateCount, "pushedCount", pushed));
            throw e;
        }
        result.setPushedPredictionCount(pushed);
    }

    private boolean runDailyPatrolGenerationIfDue(AutomationStatusVo status, AutomationRunResultVo result) {
        if (status == null || !Boolean.TRUE.equals(status.getDailyPatrolGenerateEnabled())) {
            return false;
        }
        LocalTime generateTime = parseTime(status.getDailyPatrolGenerateTime(), LocalTime.of(8, 30));
        if (LocalTime.now().isBefore(generateTime)) {
            return false;
        }
        String key = DAILY_PATROL_GENERATE_DAY_KEY_PREFIX + DateUtil.today();
        if (RedisUtils.hasKey(key)) {
            return false;
        }
        RLock lock = RedisUtils.getClient().getLock(DAILY_PATROL_GENERATE_LOCK_KEY);
        boolean locked = false;
        AutoModeTraceContext context = null;
        Long traceId = null;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.MINUTES);
            if (!locked || RedisUtils.hasKey(key)) {
                return false;
            }
            context = actionContext(
                AutoModeExecutionActions.DAILY_PATROL_GENERATE_CODE,
                AutoModeExecutionActions.DAILY_PATROL_GENERATE_NAME,
                null,
                null,
                null,
                true,
                detailOf("generateDate", DateUtil.today(), "generateTime", status.getDailyPatrolGenerateTime())
            );
            traceId = executionTraceService.start(context);
            dzTaskDistListService.scheduleCreatePatrolQuotaTasks();
            RedisUtils.setCacheObject(key, "1", Duration.ofHours(30));
            result.setCreatedPatrolTaskCount(result.getCreatedPatrolTaskCount() + 1);
            executionTraceService.success(traceId, context, 1, detailOf("dayKey", key));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executionTraceService.fail(traceId, context, e, detailOf("dayKey", key));
            log.warn("日常巡逻任务生成获取锁被中断", e);
            return false;
        } catch (Exception e) {
            executionTraceService.fail(traceId, context, e, detailOf("dayKey", key));
            throw e;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void runSyncHealthAutomation(AutomationStatusVo status, AutomationRunResultVo result) {
        if (Boolean.TRUE.equals(status.getSyncAlertEnabled())) {
            if (createNoticeOnceIfEnabled(status, "同步监控已启用", "同步失败重试和异常告警开关已开启，具体同步任务失败会写入业务日志。", "sync:health", Duration.ofDays(1))) {
                result.setNoticeCount(result.getNoticeCount() + 1);
            }
        }
    }

    private void createPredictionPushRecords() {
        List<DzRiskPrediction> predictions = dzRiskPredictionMapper.selectList(
            Wrappers.<DzRiskPrediction>lambdaQuery()
                    .in(DzRiskPrediction::getRiskLevel, 4, 5)
                    .isNotNull(DzRiskPrediction::getBatchId)
                    .orderByDesc(DzRiskPrediction::getPredictionDate, DzRiskPrediction::getId)
                    .last("limit " + MAX_RISK_PER_RUN)
        );
        Set<Long> batchIds = predictions.stream()
                                        .map(DzRiskPrediction::getBatchId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long batchId : batchIds) {
            Long existing = dzRiskPredictionPushMapper.selectCount(
                Wrappers.<DzRiskPredictionPush>lambdaQuery().eq(DzRiskPredictionPush::getPredictionBatchId, batchId)
            );
            if (existing != null && existing > 0) {
                continue;
            }
            DzRiskPrediction sample = predictions.stream()
                                                .filter(item -> Objects.equals(item.getBatchId(), batchId))
                                                .findFirst()
                                                .orElse(null);
            DzRiskPredictionPush push = new DzRiskPredictionPush();
            push.setId(IdUtil.getSnowflakeNextId());
            push.setPredictionBatchId(batchId);
            push.setType(sample == null ? 1 : sample.getType());
            push.setTitle("地灾风险预测预警");
            push.setContent("系统检测到风险预测批次 " + batchId + " 存在高风险或极高风险斜坡单元，请及时关注。");
            push.setStatus(0);
            push.setCreateDate(new Date());
            dzRiskPredictionPushMapper.insert(push);
        }
    }

    private boolean shouldRunReminder(AutomationStatusVo status) {
        LocalTime reminderTime = parseTime(status.getReminderTime(), LocalTime.of(16, 30));
        if (LocalTime.now().isBefore(reminderTime)) {
            return false;
        }
        String key = REMINDER_DAY_KEY_PREFIX + DateUtil.today();
        if (RedisUtils.hasKey(key)) {
            return false;
        }
        RedisUtils.setCacheObject(key, "1", Duration.ofHours(30));
        return true;
    }

    private void createNoticeIfEnabled(AutomationStatusVo status, String title, String content, String bizData) {
        if (status == null || !Boolean.TRUE.equals(status.getMsgNoticeEnabled())) {
            return;
        }
        DzMsgNotice notice = new DzMsgNotice();
        notice.setTitle(title);
        notice.setContent(StringUtils.blankToDefault(content, title));
        notice.setType("automation");
        notice.setStatus(0);
        notice.setBizData(bizData);
        notice.setCreateDate(new Date());
        dzMsgNoticeMapper.insert(notice);
    }

    private boolean createNoticeOnceIfEnabled(AutomationStatusVo status, String title, String content, String bizData, Duration ttl) {
        if (status == null || !Boolean.TRUE.equals(status.getMsgNoticeEnabled())) {
            return false;
        }
        String key = NOTICE_ONCE_KEY_PREFIX + bizData;
        if (RedisUtils.hasKey(key)) {
            return false;
        }
        createNoticeIfEnabled(status, title, content, bizData);
        RedisUtils.setCacheObject(key, "1", ttl);
        return true;
    }

    private int runCountAction(AutoModeTraceContext context, CountAction action) {
        Long traceId = executionTraceService.start(context);
        try {
            int count = Math.max(0, action.run());
            executionTraceService.success(traceId, context, count, detailOf("executeCount", count));
            return count;
        } catch (Exception e) {
            executionTraceService.fail(traceId, context, e, null);
            throw e;
        }
    }

    private AutoModeTraceContext actionContext(Integer actionType, String actionName, Integer bizType, Long bizId,
                                               Long taskId, boolean batchFlag, Map<String, Object> detailJson) {
        return AutoModeTraceContext.builder()
            .actionType(actionType)
            .actionName(actionName)
            .bizType(bizType)
            .bizId(bizId)
            .taskId(taskId)
            .batchFlag(batchFlag)
            .executeCount(0)
            .detailJson(detailJson)
            .build();
    }

    private Map<String, Object> detailOf(Object... keyValues) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (keyValues == null) {
            return detail;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            detail.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return detail;
    }

    @FunctionalInterface
    private interface CountAction {
        int run();
    }

    private AutomationStatusVo loadStatusFromDb() {
        AutomationStatusVo status = new AutomationStatusVo();
        status.setDailyPatrolGenerateEnabled(parseBoolean(KEY_DAILY_PATROL_GENERATE, true));
        status.setDailyPatrolGenerateTime(StringUtils.blankToDefault(getConfigValue(KEY_DAILY_PATROL_GENERATE_TIME), "08:30"));
        status.setDailyPatrolAutoPushEnabled(parseBoolean(KEY_DAILY_PATROL_PUSH, false));
        status.setUnpushedAutoPushEnabled(parseBoolean(KEY_UNPUSHED_PUSH, false));
        status.setUnpushedSourceTypes(parseIntList(getConfigValue(KEY_UNPUSHED_SOURCE_TYPES), List.of(1, 2, 3, 5, 6)));
        status.setReminderEnabled(parseBoolean(KEY_REMINDER_ENABLED, false));
        status.setReminderTime(StringUtils.blankToDefault(getConfigValue(KEY_REMINDER_TIME), "16:30"));
        status.setReminderSourceTypes(parseIntList(getConfigValue(KEY_REMINDER_SOURCE_TYPES), List.of(1, 3)));
        status.setWarningAutoPushMonitorTaskEnabled(parseBoolean(KEY_WARNING_PUSH_MONITOR, false));
        status.setReportAutoHandleEnabled(parseBoolean(KEY_REPORT_HANDLE, false));
        status.setReportAutoPushTaskEnabled(parseBoolean(KEY_REPORT_PUSH, false));
        status.setPredictionAutoCreatePushEnabled(parseBoolean(KEY_PREDICTION_CREATE_PUSH, false));
        status.setPredictionAutoPushEnabled(parseBoolean(KEY_PREDICTION_PUSH, false));
        status.setTaskSmsEnabled(parseBooleanWithFallback(KEY_SMS_TASK_PUSH, KEY_TASK_SMS, false));
        status.setMsgNoticeEnabled(parseBoolean(KEY_MSG_NOTICE, false));
        status.setSyncRetryEnabled(parseBoolean(KEY_SYNC_RETRY, false));
        status.setSyncAlertEnabled(parseBoolean(KEY_SYNC_ALERT, false));
        status.setUpdatedBy(parseLong(getConfigValue(KEY_UPDATED_BY), null));
        String updatedAt = getConfigValue(KEY_UPDATED_AT);
        status.setUpdatedAt(StringUtils.isBlank(updatedAt) ? null : new Date(DateUtil.parse(updatedAt).getTime()));
        return status;
    }

    private AutomationStatusVo attachAutoModeStatus(AutomationStatusVo status) {
        if (status == null) {
            return null;
        }
        boolean autoModeEnabled = Boolean.TRUE.equals(dzAutoModeService.getStatus().getEnabled());
        status.setAutoModeEnabled(autoModeEnabled);
        return status;
    }

    private void applyConfig(AutomationConfigBo bo) {
        saveBoolean(KEY_DAILY_PATROL_GENERATE, bo.getDailyPatrolGenerateEnabled(), "日常巡逻任务定时生成");
        if (StringUtils.isNotBlank(bo.getDailyPatrolGenerateTime())) {
            parseTime(bo.getDailyPatrolGenerateTime(), null);
            saveConfig(KEY_DAILY_PATROL_GENERATE_TIME, bo.getDailyPatrolGenerateTime().trim(), "日常巡逻任务生成时间");
        }
        saveBoolean(KEY_DAILY_PATROL_PUSH, bo.getDailyPatrolAutoPushEnabled(), "日常巡逻任务生成后自动推送");
        saveBoolean(KEY_UNPUSHED_PUSH, bo.getUnpushedAutoPushEnabled(), "未推送任务自动推送");
        saveList(KEY_UNPUSHED_SOURCE_TYPES, bo.getUnpushedSourceTypes(), "未推送任务自动推送来源");
        saveBoolean(KEY_REMINDER_ENABLED, bo.getReminderEnabled(), "自动催办开关");
        if (StringUtils.isNotBlank(bo.getReminderTime())) {
            parseTime(bo.getReminderTime(), null);
            saveConfig(KEY_REMINDER_TIME, bo.getReminderTime().trim(), "自动催办时间");
        }
        saveList(KEY_REMINDER_SOURCE_TYPES, bo.getReminderSourceTypes(), "自动催办来源");
        saveBoolean(KEY_WARNING_PUSH_MONITOR, bo.getWarningAutoPushMonitorTaskEnabled(), "预警监测任务自动推送");
        saveBoolean(KEY_REPORT_HANDLE, bo.getReportAutoHandleEnabled(), "报灾自动处理");
        saveBoolean(KEY_REPORT_PUSH, bo.getReportAutoPushTaskEnabled(), "报灾任务自动推送");
        saveBoolean(KEY_PREDICTION_CREATE_PUSH, bo.getPredictionAutoCreatePushEnabled(), "风险预测自动创建推送记录");
        saveBoolean(KEY_PREDICTION_PUSH, bo.getPredictionAutoPushEnabled(), "风险预测自动推送");
        saveBoolean(KEY_TASK_SMS, bo.getTaskSmsEnabled(), "普通任务短信自动发送");
        saveBoolean(KEY_SMS_TASK_PUSH, bo.getTaskSmsEnabled(), "任务推送短信开关");
        saveBoolean(KEY_MSG_NOTICE, bo.getMsgNoticeEnabled(), "消息中心通知自动生成");
        saveBoolean(KEY_SYNC_RETRY, bo.getSyncRetryEnabled(), "同步失败自动重试");
        saveBoolean(KEY_SYNC_ALERT, bo.getSyncAlertEnabled(), "同步异常自动告警");
    }

    private void saveBoolean(String key, Boolean value, String name) {
        if (value != null) {
            saveConfig(key, String.valueOf(value), name);
        }
    }

    private void saveList(String key, List<Integer> values, String name) {
        if (values == null) {
            return;
        }
        List<Integer> normalized = values.stream().filter(Objects::nonNull).distinct().toList();
        saveConfig(key, normalized.stream().map(String::valueOf).collect(Collectors.joining(",")), name);
    }

    private AutomationStatusVo refreshCache() {
        RedisUtils.deleteObject(CACHE_KEY);
        AutomationStatusVo status = loadStatusFromDb();
        cacheStatus(status);
        return attachAutoModeStatus(status);
    }

    private void cacheStatus(AutomationStatusVo status) {
        RedisUtils.setCacheObject(CACHE_KEY, status, Duration.ofSeconds(30));
    }

    private boolean parseBoolean(String key, boolean defaultValue) {
        String value = getConfigValue(key);
        return StringUtils.isBlank(value) ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private boolean parseBooleanWithFallback(String key, String fallbackKey, boolean defaultValue) {
        String value = getConfigValue(key);
        if (StringUtils.isBlank(value)) {
            value = getConfigValue(fallbackKey);
        }
        return StringUtils.isBlank(value) ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private List<Integer> parseIntList(String value, List<Integer> defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        List<Integer> result = new ArrayList<>();
        for (String item : value.split(",")) {
            try {
                result.add(Integer.parseInt(item.trim()));
            } catch (Exception ignored) {
            }
        }
        return result.isEmpty() ? defaultValue : result.stream().distinct().toList();
    }

    private LocalTime parseTime(String value, LocalTime defaultValue) {
        if (StringUtils.isBlank(value)) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new ServiceException("自动催办时间不能为空");
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ServiceException("自动催办时间格式必须为 HH:mm");
        }
    }

    private Long parseLong(String value, Long defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getConfigValue(String key) {
        DzSysConfig config = sysConfigMapper.selectOne(
            Wrappers.<DzSysConfig>lambdaQuery().eq(DzSysConfig::getConfigKey, key).last("limit 1")
        );
        return config == null ? null : config.getConfigValue();
    }

    private void saveUpdateMeta() {
        Long userId = resolveCurrentUserId();
        saveConfig(KEY_UPDATED_BY, userId == null ? "" : String.valueOf(userId), "自动化运营最后修改人");
        saveConfig(KEY_UPDATED_AT, DateUtil.formatDateTime(new Date()), "自动化运营最后修改时间");
    }

    private void saveConfig(String key, String value, String name) {
        Date now = new Date();
        Long userId = resolveCurrentUserId();
        DzSysConfig current = sysConfigMapper.selectOne(
            Wrappers.<DzSysConfig>lambdaQuery().eq(DzSysConfig::getConfigKey, key).last("limit 1")
        );
        if (current == null) {
            DzSysConfig insert = new DzSysConfig();
            insert.setConfigId(IdUtil.getSnowflakeNextId());
            insert.setTenantId("000000");
            insert.setConfigName(name);
            insert.setConfigKey(key);
            insert.setConfigValue(value);
            insert.setConfigType("N");
            insert.setCreateBy(userId);
            insert.setCreateTime(now);
            insert.setUpdateBy(userId);
            insert.setUpdateTime(now);
            insert.setRemark("地灾自动化运营配置");
            sysConfigMapper.insert(insert);
            return;
        }
        DzSysConfig update = new DzSysConfig();
        update.setConfigId(current.getConfigId());
        update.setConfigName(name);
        update.setConfigValue(value);
        update.setUpdateBy(userId);
        update.setUpdateTime(now);
        sysConfigMapper.updateById(update);
    }

    private Long resolveCurrentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
