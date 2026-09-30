/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.task;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DataAlarmBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskWarningRecord;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.RiskWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskWarningRecordVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskWarningRecordService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyWeatherWarningService;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanGenerateByPendingAlarmBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AiDataAlarmAnswerDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.resp.DefRespPlanGenerateByPendingAlarmResp;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespAlarmHistoryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefRespScheduledTask {
    private static final int WEATHER_SYNC_PAGE_SIZE = 100;
    private static final int ALARM_PENDING_STATUS_NONE = 0;
    private static final int ALARM_PENDING_STATUS_WAITING = 1;
    private static final int ALARM_PENDING_STATUS_TRIGGERED = 2;
    private static final int ALARM_SOURCE_TYPE_MANUAL = 1;
    private static final int ALARM_SOURCE_TYPE_REPORT_ANALYSIS = 2;
    private static final int ALARM_STATUS_UNTRIGGERED = 0;
    private static final int ALARM_STATUS_TRIGGERED = 1;
    private static final String FILTER_REASON_OUT_OF_WINDOW = "当前时间不在预警记录有效时间内";
    private static final String DEFAULT_ALARM_SOURCE = "第三方预警AI解析";
    private static final DateTimeFormatter HOUR_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String WEATHER_SYNC_LOCK_PREFIX = "def_resp_weather_sync_lock_";
    private static final String WEATHER_SYNC_SUCCESS_PREFIX = "def_resp_weather_sync_success_";
    private static final String AI_PARSE_LOCK_PREFIX = "def_resp_ai_parse_lock_";
    private static final String AI_PARSE_SUCCESS_PREFIX = "def_resp_ai_parse_success_";
    private static final String GENERATE_DEF_RESP_LOCK_PREFIX = "def_resp_generate_plan_lock_";
    private static final String GENERATE_DEF_RESP_SUCCESS_PREFIX = "def_resp_generate_plan_success_";
    private static final String CLOSE_EXPIRED_ALARM_LOCK_PREFIX = "def_resp_close_expired_alarm_lock_";
    private static final String CLOSE_EXPIRED_ALARM_SUCCESS_PREFIX = "def_resp_close_expired_alarm_success_";
    private static final String CREATE_PATROL_QUOTA_LOCK_PREFIX = "def_resp_create_patrol_quota_lock_";
    private static final String CREATE_PATROL_QUOTA_SUCCESS_PREFIX = "def_resp_create_patrol_quota_success_";
    private static final long TASK_SUCCESS_TTL_HOURS = 4L;
    private static final long DAILY_TASK_SUCCESS_TTL_HOURS = 48L;
    private static final long WEATHER_SYNC_LOCK_MINUTES = 30L;
    private static final long AI_PARSE_LOCK_MINUTES = 90L;
    private static final long GENERATE_DEF_RESP_LOCK_MINUTES = 90L;
    private static final long CLOSE_EXPIRED_ALARM_LOCK_MINUTES = 30L;
    private static final long CREATE_PATROL_QUOTA_LOCK_HOURS = 12L;
    private static final List<String> ENSHI_STREETS = List.of(
        "白果乡", "盛家坝镇", "芭蕉侗族乡", "太阳河乡", "白杨坪镇", "崔家坝镇", "板桥镇", "新塘乡", "七里坪街道", "屯堡乡", "六角亭街道", "小渡船街道", "红土乡", "龙凤镇", "三岔镇", "金子坝街道", "沐抚办事处", "舞阳坝街道", "沙地乡"
    );

    public static boolean isEnshiStreet(String street) {
        return street != null && ENSHI_STREETS.contains(street.trim());
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final IThirdPartyWeatherWarningService weatherWarningService;
    private final IRiskWarningRecordService riskWarningRecordService;
    private final IDzDefRespPlanService defRespPlanService;
    private final IDataAlarmService dataAlarmService;
    private final DifyAgentClient difyAgentClient;
    private final IDzAutomationService dzAutomationService;
    private final IDzDefRespAlarmHistoryService dzDefRespAlarmHistoryService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final AtomicBoolean generatingDefRespPlan = new AtomicBoolean(false);

    /**
     * 任务1：同步第三方气象预警数据到本地库
     * 每小时整点执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncWeatherWarning() {
        executeSyncWeatherWarning();
    }

    /**
     * 手动触发：同步第三方气象预警数据到本地库。
     */
    public TaskTriggerResult triggerSyncWeatherWarning() {
        return executeSyncWeatherWarningDirect(null, null);
    }

    /**
     * 手动触发：同步指定日期范围内的第三方气象预警数据到本地库。
     */
    public TaskTriggerResult triggerSyncWeatherWarning(String startTime, String endTime) {
        return executeSyncWeatherWarningDirect(startTime, endTime);
    }

    private TaskTriggerResult executeSyncWeatherWarning() {
        return executeSyncWeatherWarning(null, null);
    }

    private TaskTriggerResult executeSyncWeatherWarning(String startTime, String endTime) {
        RiskWarningRecordBo queryBo = buildWeatherWarningQueryBo(startTime, endTime);
        return runHourlyDistributedTask(
            "同步气象预警",
            WEATHER_SYNC_LOCK_PREFIX,
            WEATHER_SYNC_SUCCESS_PREFIX,
            WEATHER_SYNC_LOCK_MINUTES,
            buildWeatherSyncTaskScope(queryBo),
            () -> syncWeatherWarningData(queryBo)
        );
    }

    private TaskTriggerResult executeSyncWeatherWarningDirect(String startTime, String endTime) {
        RiskWarningRecordBo queryBo = buildWeatherWarningQueryBo(startTime, endTime);
        return syncWeatherWarningData(queryBo) ? TaskTriggerResult.SUCCESS : TaskTriggerResult.FAILED;
    }

    private List<RiskWarningRecordVo> fetchAllWeatherWarnings(RiskWarningRecordBo bo) {
        int pageNum = 1;
        long total = Long.MAX_VALUE;
        List<RiskWarningRecordVo> allRecords = new java.util.ArrayList<>();
        while (allRecords.size() < total) {
            PageQuery pageQuery = new PageQuery(WEATHER_SYNC_PAGE_SIZE, pageNum);
            TableDataInfo<RiskWarningRecordVo> table = weatherWarningService.getWeatherWarning(bo, pageQuery);
            if (table == null || table.getData() == null || table.getData().isEmpty()) {
                break;
            }
            allRecords.addAll(table.getData());
            total = table.getTotal();
            if (table.getData().size() < WEATHER_SYNC_PAGE_SIZE) {
                break;
            }
            pageNum++;
        }
        return allRecords;
    }

    /**
     * 任务2：对未解析的预警记录调用大模型解析并写入 data_alarm
     * 每小时第5分钟执行（在同步任务之后）
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void parseAlarmWithAi() {
        executeParseAlarmWithAi();
    }

    /**
     * 手动触发：对今日未解析预警记录执行 AI 解析并写入 data_alarm。
     */
    public TaskTriggerResult triggerParseAlarmWithAi() {
        return executeParseAlarmWithAiDirect();
    }

    private TaskTriggerResult executeParseAlarmWithAi() {
        return runHourlyDistributedTask(
            "AI解析预警",
            AI_PARSE_LOCK_PREFIX,
            AI_PARSE_SUCCESS_PREFIX,
            AI_PARSE_LOCK_MINUTES,
            "default",
            this::parseAlarmWithAiData
        );
    }

    private TaskTriggerResult executeParseAlarmWithAiDirect() {
        return parseAlarmWithAiData() ? TaskTriggerResult.SUCCESS : TaskTriggerResult.FAILED;
    }

    private boolean syncWeatherWarningData(RiskWarningRecordBo bo) {
        try {
            List<RiskWarningRecordVo> data;
            try {
                data = fetchAllWeatherWarnings(bo);
            } catch (Exception e) {
                log.error("获取预警数据失败", e);
                return false;
            }

            if (data.isEmpty()) {
                log.info("没有新的预警数据, startTime={}, endTime={}", bo.getStartTime(), bo.getEndTime());
                return true;
            }

            List<String> incomingIds = data.stream()
                                           .map(RiskWarningRecordVo::getId)
                                           .filter(id -> id != null && !id.isBlank())
                                           .collect(Collectors.toList());
            Set<String> existingIds = riskWarningRecordService.getExistingIds(incomingIds);
            List<RiskWarningRecordVo> toInsert = data.stream()
                                                     .filter(vo -> vo.getId() != null && !vo.getId().isBlank() && !existingIds.contains(vo.getId()))
                                                     .toList();

            if (toInsert.isEmpty()) {
                log.info("无新增预警数据需同步, startTime={}, endTime={}", bo.getStartTime(), bo.getEndTime());
                return true;
            }
            List<RiskWarningRecord> riskWarningRecords = toInsert.stream()
                                                                 .map(this::buildRiskWarningRecord)
                                                                 .toList();
            if (!riskWarningRecordService.insertBatch(riskWarningRecords)) {
                log.warn("插入第三方气象预警数据记录失败");
                return false;
            } else {
                log.info("同步气象预警记录成功, 数量: {}, startTime={}, endTime={}", toInsert.size(), bo.getStartTime(), bo.getEndTime());
            }
            return true;
        } catch (Exception e) {
            log.error("同步气象预警任务执行失败", e);
            return false;
        }
    }

    private RiskWarningRecord buildRiskWarningRecord(RiskWarningRecordVo vo) {
        RiskWarningRecord record = new RiskWarningRecord();
        org.springframework.beans.BeanUtils.copyProperties(vo, record);
        return record;
    }

    private boolean parseAlarmWithAiData() {
        try {
            LocalDate today = LocalDate.now();
            ZoneId zone = ZoneId.systemDefault();
            Date start = Date.from(today.atStartOfDay(zone).toInstant());
            Date end = Date.from(today.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant());

            List<RiskWarningRecord> records = riskWarningRecordService.listByWarningDateBetween(start, end);
            if (records == null || records.isEmpty()) {
                log.info("今日无待解析的预警记录");
                return true;
            }
            Set<String> existingCodes = dataAlarmService.getExistingCodes(
                records.stream()
                       .map(RiskWarningRecord::getResultTableName)
                       .filter(Objects::nonNull)
                       .filter(code -> !code.isBlank())
                       .collect(Collectors.toSet())
            );
            List<RiskWarningRecord> pendingRecords = records.stream()
                                                            .filter(Objects::nonNull)
                                                            .filter(record -> record.getId() != null)
                                                            .filter(record -> {
                                                                String code = record.getResultTableName();
                                                                return code == null || code.isBlank() || !existingCodes.contains(code);
                                                            })
                                                            .toList();
            Map<String, RiskWarningRecord> uniqueByCode = new LinkedHashMap<>();
            List<RiskWarningRecord> dedupedRecords = pendingRecords.stream()
                                                                   .filter(record -> {
                                                                       String code = record.getResultTableName();
                                                                       if (code == null || code.isBlank()) {
                                                                           return true;
                                                                       }
                                                                       return uniqueByCode.putIfAbsent(code, record) == null;
                                                                   })
                                                                   .toList();
            if (dedupedRecords.isEmpty()) {
                log.info("今日预警记录已全部解析");
                return true;
            }
            List<RiskWarningRecord> supportedRecords = dedupedRecords.stream()
                                                                     .filter(record -> mapRiskWarningRecordLevel(record.getMaxWarningLevel()) != null)
                                                                     .toList();
            if (supportedRecords.isEmpty()) {
                log.info("今日预警记录中无满足 max_warning_level 映射规则的数据，跳过AI解析");
                return true;
            }
            if (supportedRecords.size() < dedupedRecords.size()) {
                log.info("预警记录按 max_warning_level 过滤后进入AI解析: total={}, supported={}, skipped={}",
                    dedupedRecords.size(), supportedRecords.size(), dedupedRecords.size() - supportedRecords.size());
            }
            Semaphore semaphore = new Semaphore(Math.min(8, supportedRecords.size()));
            List<CompletableFuture<DataAlarmBo>> futures = supportedRecords.stream()
                                                                           .map(record -> CompletableFuture.supplyAsync(() -> parseRecordToAlarm(record, semaphore), Run.executor))
                                                                           .toList();
            List<DataAlarmBo> dataAlarmBos = futures.stream()
                                                    .map(CompletableFuture::join)
                                                    .filter(Objects::nonNull)
                                                    .toList();
            if (!dataAlarmBos.isEmpty() && !dataAlarmService.insertBatchByBo(dataAlarmBos)) {
                log.warn("批量插入警报数据失败, size={}", dataAlarmBos.size());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("AI解析预警任务执行失败", e);
            return false;
        }
    }

    private TaskTriggerResult runHourlyDistributedTask(String taskName,
                                                       String lockPrefix,
                                                       String successPrefix,
                                                       long lockMinutes,
                                                       String taskScope,
                                                       BooleanSupplier task) {
        String hourKey = LocalDateTime.now().format(HOUR_KEY_FORMATTER);
        return runDistributedTask(
            taskName,
            hourKey,
            lockPrefix,
            successPrefix,
            lockMinutes,
            TimeUnit.MINUTES,
            TASK_SUCCESS_TTL_HOURS,
            TimeUnit.HOURS,
            taskScope,
            task
        );
    }

    private TaskTriggerResult runDailyDistributedTask(String taskName,
                                                      String lockPrefix,
                                                      String successPrefix,
                                                      long lockHours,
                                                      String taskScope,
                                                      BooleanSupplier task) {
        String dayKey = LocalDate.now().format(DAY_KEY_FORMATTER);
        return runDistributedTask(
            taskName,
            dayKey,
            lockPrefix,
            successPrefix,
            lockHours,
            TimeUnit.HOURS,
            DAILY_TASK_SUCCESS_TTL_HOURS,
            TimeUnit.HOURS,
            taskScope,
            task
        );
    }

    private TaskTriggerResult runMinuteDistributedTask(String taskName,
                                                       String lockPrefix,
                                                       String successPrefix,
                                                       long lockMinutes,
                                                       String taskScope,
                                                       BooleanSupplier task) {
        String minuteKey = LocalDateTime.now().withSecond(0).withNano(0).format(MINUTE_KEY_FORMATTER);
        return runDistributedTask(
            taskName,
            minuteKey,
            lockPrefix,
            successPrefix,
            lockMinutes,
            TimeUnit.MINUTES,
            TASK_SUCCESS_TTL_HOURS,
            TimeUnit.HOURS,
            taskScope,
            task
        );
    }

    private TaskTriggerResult runDistributedTask(String taskName,
                                                 String timeKey,
                                                 String lockPrefix,
                                                 String successPrefix,
                                                 long lockTtl,
                                                 TimeUnit lockTimeUnit,
                                                 long successTtl,
                                                 TimeUnit successTimeUnit,
                                                 String taskScope,
                                                 BooleanSupplier task) {
        String keySuffix = buildTaskKeySuffix(taskScope);
        String successKey = successPrefix + timeKey + keySuffix;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(successKey))) {
            log.info("{}任务在当前时间窗已成功执行，跳过重复触发，timeKey={}", taskName, timeKey);
            return TaskTriggerResult.ALREADY_SUCCESS;
        }
        String lockKey = lockPrefix + timeKey + keySuffix;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockTtl, lockTimeUnit);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("{}任务未获取到分布式锁，跳过本次执行，lockKey={}", taskName, lockKey);
            return TaskTriggerResult.RUNNING;
        }
        try {
            if (!task.getAsBoolean()) {
                log.warn("{}任务执行未成功，不写入成功标记，timeKey={}", taskName, timeKey);
                return TaskTriggerResult.FAILED;
            }
            stringRedisTemplate.opsForValue().set(successKey, "SUCCESS", successTtl, successTimeUnit);
            log.info("{}任务执行完成，timeKey={}", taskName, timeKey);
            return TaskTriggerResult.SUCCESS;
        } catch (Exception e) {
            log.error("{}任务执行失败，timeKey={}", taskName, timeKey, e);
            return TaskTriggerResult.FAILED;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private RiskWarningRecordBo buildWeatherWarningQueryBo(String startTime, String endTime) {
        RiskWarningRecordBo bo = new RiskWarningRecordBo();
        if (isBlank(startTime) && isBlank(endTime)) {
            bo.setStartTime(DATE_TIME_FORMATTER.format(LocalDate.now()));
            bo.setEndTime(DATE_TIME_FORMATTER.format(LocalDate.now().plusDays(1)));
            return bo;
        }
        if (isBlank(startTime) || isBlank(endTime)) {
            throw new ServiceException("开始时间和结束时间需同时传入，格式为 yyyy-MM-dd");
        }
        LocalDate startDate = parseDate(startTime, "开始时间格式错误，应为 yyyy-MM-dd");
        LocalDate endDate = parseDate(endTime, "结束时间格式错误，应为 yyyy-MM-dd");
        if (endDate.isBefore(startDate)) {
            throw new ServiceException("结束时间不能早于开始时间");
        }
        bo.setStartTime(DATE_TIME_FORMATTER.format(startDate));
        bo.setEndTime(DATE_TIME_FORMATTER.format(endDate));
        return bo;
    }

    private LocalDate parseDate(String dateText, String errorMessage) {
        try {
            return LocalDate.parse(dateText, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new ServiceException(errorMessage);
        }
    }

    private String buildWeatherSyncTaskScope(RiskWarningRecordBo bo) {
        if (bo == null || (isBlank(bo.getStartTime()) && isBlank(bo.getEndTime()))) {
            return "default";
        }
        return bo.getStartTime() + "_" + bo.getEndTime();
    }

    private String buildTaskKeySuffix(String taskScope) {
        return isBlank(taskScope) ? "" : "_" + taskScope;
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private void releaseLock(String lockKey, String lockValue) {
        if (lockKey == null || lockKey.isBlank() || lockValue == null || lockValue.isBlank()) {
            return;
        }
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (Objects.equals(lockValue, currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private DataAlarmBo parseRecordToAlarm(RiskWarningRecord record, Semaphore semaphore) {
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
            log.info("开始处理预警数据：{}", record.getId());
            RiskWarningRecordResp riskWarningRecordResp = weatherWarningService.weatherWarningDetail(record.getId());
            if (riskWarningRecordResp == null) {
                log.warn("预警详情为空: recordId={}", record.getId());
                return null;
            }
            String forecastWords = riskWarningRecordResp.getForecastWords();
            if (forecastWords == null || forecastWords.isBlank()) {
                log.warn("预警详情缺少预报词，跳过AI解析: recordId={}", record.getId());
                return null;
            }
            DifyChatflowClient agent = difyAgentClient.getAiAnalyzeAlarmAgent();
            ChatMessage message = ChatMessage.builder()
                                             .query(forecastWords)
                                             .responseMode(ResponseMode.BLOCKING)
                                             .user("admin")
                                             .conversationId("")
                                             .build();
            ChatMessageResponse chatResp = agent.sendChatMessage(message);
            if (chatResp == null || chatResp.getAnswer() == null || chatResp.getAnswer().isBlank()) {
                log.warn("大模型返回为空: recordId={}", record.getId());
                return null;
            }
            String answer = chatResp.getAnswer();
            AiDataAlarmAnswerDto answerDto = normalizeAiAlarmAnswer(answer, record.getId());
            if (answerDto == null) {
                log.warn("大模型解析预警结果为空: recordId={}", record.getId());
                return null;
            }
            return buildDataAlarmBo(answerDto, riskWarningRecordResp, record);
        } catch (Exception e) {
            log.error("处理预警数据失败: {}", record.getId(), e);
            return null;
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    private AiDataAlarmAnswerDto normalizeAiAlarmAnswer(String answer, String recordId) {
        JsonNode answerNode = JacksonUtil.toJsonNode(answer);
        if (answerNode == null || !answerNode.isObject()) {
            log.warn("大模型返回结果不是有效JSON对象，跳过入库: recordId={}", recordId);
            return null;
        }
        AiDataAlarmAnswerDto answerDto = JsonUtils.parseObject(answer, AiDataAlarmAnswerDto.class);
        if (answerDto == null) {
            return null;
        }
        String city = extractJsonText(answerNode.get("city"));
        if (city == null || !city.contains("恩施")) {
            log.info("AI解析结果非恩施区域，跳过入库: recordId={}, city={}", recordId, city);
            return null;
        }
        List<String> matchedStreets = matchEnshiStreets(answerNode.get("streets"));
        if (matchedStreets.isEmpty()) {
            matchedStreets = ENSHI_STREETS;
            log.info("AI解析结果未命中恩施乡镇，按全量乡镇兜底: recordId={}", recordId);
        }
        answerDto.setCity(city.trim());
        Integer level = answerDto.getLevel();
        if (level != null) {
            answerDto.setLevelStreetsJson(DataAlarmLevelStreetsUtil.toJson(Map.of(level, matchedStreets)));
        }
        return answerDto;
    }

    private List<String> matchEnshiStreets(JsonNode streetsNode) {
        List<String> parsedStreets = extractStreetCandidates(streetsNode);
        if (parsedStreets.isEmpty()) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        for (String candidate : parsedStreets) {
            for (String street : ENSHI_STREETS) {
                if (!matched.contains(street) && isSameStreet(candidate, street)) {
                    matched.add(street);
                }
            }
        }
        return matched;
    }

    private List<String> extractStreetCandidates(JsonNode streetsNode) {
        if (streetsNode == null || streetsNode.isNull()) {
            return List.of();
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        collectStreetCandidates(streetsNode, candidates);
        return new ArrayList<>(candidates);
    }

    private void collectStreetCandidates(JsonNode streetsNode, Set<String> candidates) {
        if (streetsNode == null || streetsNode.isNull()) {
            return;
        }
        if (streetsNode.isArray()) {
            for (JsonNode node : streetsNode) {
                collectStreetCandidates(node, candidates);
            }
            return;
        }
        String text = extractJsonText(streetsNode);
        if (text == null || text.isBlank()) {
            return;
        }
        for (String item : text.split("[,，、；;\\s]+")) {
            String street = item.trim();
            if (!street.isEmpty()) {
                candidates.add(street);
            }
        }
    }

    private String extractJsonText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString().replace("\"", "").trim();
    }

    private boolean isSameStreet(String candidate, String standardStreet) {
        String normalizedCandidate = normalizeStreetName(candidate);
        String normalizedStandardStreet = normalizeStreetName(standardStreet);
        if (normalizedCandidate.isEmpty() || normalizedStandardStreet.isEmpty()) {
            return false;
        }
        return normalizedCandidate.equals(normalizedStandardStreet)
            || normalizedCandidate.contains(normalizedStandardStreet)
            || normalizedStandardStreet.contains(normalizedCandidate);
    }

    private String normalizeStreetName(String street) {
        if (street == null) {
            return "";
        }
        String normalized = street.replace("恩施市", "")
                                  .replace("恩施", "")
                                  .replaceAll("[,，、；;\\s]", "")
                                  .trim();
        for (String suffix : List.of("街道", "办事处", "镇", "乡")) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }
        return normalized;
    }

    private DataAlarmBo buildDataAlarmBo(AiDataAlarmAnswerDto answerDto, RiskWarningRecordResp detail, RiskWarningRecord record) {
        DataAlarmBo dataAlarmBo = BeanUtil.toBean(answerDto, DataAlarmBo.class);
        String code = detail.getResultTableName();
        if (code == null || code.isBlank()) {
            code = record.getResultTableName();
        }
        if (code == null || code.isBlank()) {
            log.warn("AI解析结果缺少编码，跳过入库: recordId={}", record.getId());
            return null;
        }
        Integer level = DataAlarmLevelStreetsUtil.resolveHighestLevel(dataAlarmBo.getLevelStreetsJson());
        if (level == null) {
            level = mapRiskWarningRecordLevel(record == null ? null : record.getMaxWarningLevel());
        }
        if (level == null) {
            if (record != null) {
                log.warn("AI解析结果缺少有效级别，跳过入库: recordId={}", record.getId());
            }
            return null;
        }
        if (dataAlarmBo.getCity() == null || dataAlarmBo.getCity().isBlank()) {
            dataAlarmBo.setCity(extractCity(detail.getAdministrativeDivision()));
        }
        if (dataAlarmBo.getCity() == null || dataAlarmBo.getCity().isBlank()) {
            dataAlarmBo.setCity(extractCity(detail.getAdministrativeDivision()));
        }
        if (StringUtils.isBlank(dataAlarmBo.getLevelStreetsJson())) {
            List<String> matchedStreets = matchEnshiStreets(JacksonUtil.toJsonNode(detail.getAdministrativeDivision()));
            if (matchedStreets.isEmpty()) {
                matchedStreets = ENSHI_STREETS;
            }
            dataAlarmBo.setLevelStreetsJson(DataAlarmLevelStreetsUtil.toJson(Map.of(level, matchedStreets)));
        }
        if (dataAlarmBo.getCity() == null || dataAlarmBo.getCity().isBlank()
            || StringUtils.isBlank(dataAlarmBo.getLevelStreetsJson())) {
            if (record != null) {
                log.warn("AI解析结果缺少区域信息，跳过入库: recordId={}", record.getId());
            }
            return null;
        }
        dataAlarmBo.setCode(code);
        dataAlarmBo.setCreateDate(dataAlarmBo.getCreateDate() != null ? dataAlarmBo.getCreateDate() : new Date());
        dataAlarmBo.setStatus(dataAlarmBo.getStatus() != null ? dataAlarmBo.getStatus() : ALARM_STATUS_UNTRIGGERED);
        dataAlarmBo.setPendingStatus(dataAlarmBo.getPendingStatus() != null ? dataAlarmBo.getPendingStatus() : ALARM_PENDING_STATUS_WAITING);
        dataAlarmBo.setSource(dataAlarmBo.getSource() != null && !dataAlarmBo.getSource().isBlank()
            ? dataAlarmBo.getSource()
            : DEFAULT_ALARM_SOURCE);
        if (dataAlarmBo.getMessage() == null || dataAlarmBo.getMessage().isBlank()) {
            dataAlarmBo.setMessage(detail.getForecastWords());
        }
        dataAlarmBo.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
        return dataAlarmBo;
    }

    private Integer mapRiskWarningRecordLevel(Integer maxWarningLevel) {
        if (maxWarningLevel == null) {
            return null;
        }
        return switch (maxWarningLevel) {
            case 3 -> 2;
            case 4 -> 4;
            default -> null;
        };
    }

    private String extractCity(String administrativeDivision) {
        if (administrativeDivision == null || administrativeDivision.isBlank()) {
            return null;
        }
        String[] parts = administrativeDivision.split("[,，]");
        return parts.length == 0 ? administrativeDivision : parts[0].trim();
    }

//    /**
//     * 任务3：根据当前处于有效时间内、status=0 且 pending_status=1 的警报生成或更新区域防御响应方案
//     * 每5分钟执行一次。
//     * 区域模型：优先县级 + 乡镇子行；匹配与更新逻辑见 {@link #handleAlarmToDefRespPlan(DataAlarmVo)}。
//     */
//    @Scheduled(cron = "0 */5 * * * ?")
//    public void generateDefRespPlan() {
//        runMinuteDistributedTask(
//            "生成防御响应方案",
//            GENERATE_DEF_RESP_LOCK_PREFIX,
//            GENERATE_DEF_RESP_SUCCESS_PREFIX,
//            GENERATE_DEF_RESP_LOCK_MINUTES,
//            "default",
//            this::generateDefRespPlanData
//        );
//    }

    /**
     * 手动触发：根据未触发的警报生成或更新区域防御响应方案。
     *
     * @return true 表示已成功进入执行流程；false 表示当前已有同任务在执行
     */
    public DefRespPlanGenerateByPendingAlarmResp triggerGenerateDefRespPlan(DefRespPlanGenerateByPendingAlarmBo bo) {
        return executeGenerateDefRespPlan("manual", bo);
    }

    private DefRespPlanGenerateByPendingAlarmResp executeGenerateDefRespPlan(String triggerSource,
                                                                             DefRespPlanGenerateByPendingAlarmBo req) {
        if (!generatingDefRespPlan.compareAndSet(false, true)) {
            log.warn("生成防御响应方案任务正在执行中，忽略本次触发: source={}", triggerSource);
            return null;
        }
        try {
            List<DataAlarmVo> candidateAlarms = queryCandidateAlarms(req);
            if (candidateAlarms == null || candidateAlarms.isEmpty()) {
                log.info("无待生成防御响应方案的警报数据");
                return emptyGenerateResp();
            }
            Date now = new Date();
            DefRespPlanGenerateByPendingAlarmResp resp = new DefRespPlanGenerateByPendingAlarmResp();
            List<Long> triggeredIds = new ArrayList<>();
            List<DefRespPlanGenerateByPendingAlarmResp.FilteredItem> filteredItems = new ArrayList<>();
            for (DataAlarmVo dataAlarmVo : candidateAlarms) {
                if (!isAlarmInPendingTriggerWindow(dataAlarmVo, now)) {
                    filteredItems.add(buildFilteredItem(dataAlarmVo == null ? null : dataAlarmVo.getId(), FILTER_REASON_OUT_OF_WINDOW));
                    continue;
                }
                try {
                    DefRespPlan matchedPlan = handleAlarmToDefRespPlan(dataAlarmVo);
                    if (matchedPlan != null) {
                        dzDefRespAlarmHistoryService.saveHistory(dataAlarmVo, matchedPlan);
                        markAlarmTriggered(dataAlarmVo);
                        triggeredIds.add(dataAlarmVo.getId());
                    }
                } catch (Exception e) {
                    log.error("生成防御响应方案异常, 警报id: {}", dataAlarmVo.getId(), e);
                }
            }
            resp.setTriggeredIds(triggeredIds);
            resp.setFilteredItems(filteredItems);
            if (triggeredIds.isEmpty()) {
                log.info("当前时间窗内无待生成防御响应方案的警报数据");
            }
            return resp;
        } catch (Exception e) {
            log.error("生成防御响应方案任务执行失败", e);
            return null;
        } finally {
            generatingDefRespPlan.set(false);
        }
    }

    private List<DataAlarmVo> queryCandidateAlarms(DefRespPlanGenerateByPendingAlarmBo req) {
        DataAlarmBo alarmBo = new DataAlarmBo();
        alarmBo.setStatus(ALARM_STATUS_UNTRIGGERED);
        alarmBo.setPendingStatus(ALARM_PENDING_STATUS_WAITING);
        if (req != null && req.getIds() != null && !req.getIds().isEmpty()) {
            List<Long> ids = req.getIds().stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(id -> !id.isEmpty())
                                .map(id -> {
                                    try {
                                        return Long.parseLong(id);
                                    } catch (NumberFormatException e) {
                                        throw new ServiceException("id格式错误，必须为数字: " + id);
                                    }
                                })
                                .distinct()
                                .toList();
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            return dataAlarmService.queryList(alarmBo)
                                   .stream()
                                   .filter(alarm -> ids.contains(alarm.getId()))
                                   .filter(alarm -> req.getSource() == null || req.getSource().isBlank()
                                       || Objects.equals(req.getSource().trim(), alarm.getSource()))
                                   .toList();
        }
        if (req != null && req.getSource() != null && !req.getSource().isBlank()) {
            alarmBo.setSource(req.getSource().trim());
        }
        return dataAlarmService.queryList(alarmBo);
    }

    private DefRespPlanGenerateByPendingAlarmResp emptyGenerateResp() {
        DefRespPlanGenerateByPendingAlarmResp resp = new DefRespPlanGenerateByPendingAlarmResp();
        resp.setTriggeredIds(Collections.emptyList());
        resp.setFilteredItems(Collections.emptyList());
        return resp;
    }

    private DefRespPlanGenerateByPendingAlarmResp.FilteredItem buildFilteredItem(Long id, String reason) {
        DefRespPlanGenerateByPendingAlarmResp.FilteredItem item = new DefRespPlanGenerateByPendingAlarmResp.FilteredItem();
        item.setId(id);
        item.setReason(reason);
        return item;
    }

    private boolean isAlarmInPendingTriggerWindow(DataAlarmVo dataAlarmVo, Date now) {
        if (dataAlarmVo == null || now == null) {
            return false;
        }
        if (!Objects.equals(dataAlarmVo.getStatus(), ALARM_STATUS_UNTRIGGERED)) {
            return false;
        }
        if (!Objects.equals(dataAlarmVo.getPendingStatus(), ALARM_PENDING_STATUS_WAITING)) {
            return false;
        }
        Date validStartDate = dataAlarmVo.getValidStartDate();
        Date validEndDate = dataAlarmVo.getValidEndDate();
        if (validStartDate == null || validEndDate == null) {
            return false;
        }
        return !now.before(validStartDate) && !now.after(validEndDate);
    }

    /**
     * 将一条待触发预警与区域防御方案对齐：按街道查库后只认县级行，忽略乡镇子行，避免误匹配。
     *
     * @return true 表示已创建或已更新方案，外层可将预警标为已触发
     */
    private DefRespPlan handleAlarmToDefRespPlan(DataAlarmVo dataAlarmVo) {
        Integer sourceType = dataAlarmVo == null ? null : dataAlarmVo.getSourceType();
        if (Objects.equals(sourceType, ALARM_SOURCE_TYPE_REPORT_ANALYSIS)
            || Objects.equals(sourceType, ALARM_SOURCE_TYPE_MANUAL)) {
            return handleAlarmFragmentsToDefRespPlan(dataAlarmVo);
        }
        if (dataAlarmVo != null) {
            log.info("当前警报来源类型暂未接入防御响应逻辑，跳过处理, alarmId={}, sourceType={}",
                dataAlarmVo.getId(), sourceType);
        }
        return null;
    }

    private DefRespPlan handleAlarmFragmentsToDefRespPlan(DataAlarmVo dataAlarmVo) {
        if (dataAlarmVo == null) {
            return null;
        }
        Map<Integer, LinkedHashSet<String>> levelStreetsMap = DataAlarmLevelStreetsUtil.parse(dataAlarmVo.getLevelStreetsJson());
        if (levelStreetsMap.isEmpty()) {
            return null;
        }
        DefRespPlan lastPlan = null;
        for (Map.Entry<Integer, LinkedHashSet<String>> entry : levelStreetsMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            DataAlarmVo fragment = BeanUtil.copyProperties(dataAlarmVo, DataAlarmVo.class);
            fragment.setLevel(entry.getKey());
            fragment.setStreets(String.join(",", entry.getValue()));
            lastPlan = defRespPlanService.generateOrUpdateRegionPlansFromAlarm(fragment);
        }
        return lastPlan;
    }

    private void markAlarmTriggered(DataAlarmVo alarmVo) {
        if (alarmVo == null || alarmVo.getId() == null) {
            return;
        }
        Date now = new Date();
        if (!Boolean.TRUE.equals(dataAlarmService.updateTriggerStatusById(
            alarmVo.getId(), ALARM_STATUS_TRIGGERED, ALARM_PENDING_STATUS_TRIGGERED, now))) {
            log.warn("标记预警已触发失败, alarmId={}", alarmVo.getId());
        }
    }

    /**
     * 任务4：关闭已过有效期且仍未触发的预警记录
     * 每小时第5分钟执行
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void closeExpiredPendingAlarms() {
        runHourlyDistributedTask(
            "关闭已过有效期的预警",
            CLOSE_EXPIRED_ALARM_LOCK_PREFIX,
            CLOSE_EXPIRED_ALARM_SUCCESS_PREFIX,
            CLOSE_EXPIRED_ALARM_LOCK_MINUTES,
            "default",
            this::closeExpiredPendingAlarmsData
        );
    }

//    /**
//     * 任务5：定时补齐防御响应监测员任务
//     * 每小时第10分钟执行，对status=5的防御响应方案按斜坡单元每日仅生成一次监测员任务
//     */
//    @Scheduled(cron = "0 10 * * * ?")
//    public void scheduleCreateMonitorTasks() {
//        runHourlyDistributedTask(
//            "定时生成监测员任务",
//            CREATE_MONITOR_TASK_LOCK_PREFIX,
//            CREATE_MONITOR_TASK_SUCCESS_PREFIX,
//            CREATE_MONITOR_TASK_LOCK_MINUTES,
//            "default",
//            this::scheduleCreateMonitorTasksData
//        );
//    }

    /**
     * 任务7：每日8点30分生成防御响应任务，每个斜坡单元按角色补齐监测员、巡查员各一条。
     * 关闭过期巡查任务逻辑已内置至 scheduleCreatePatrolQuotaTasks 等入口。
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void scheduleCreatePatrolQuotaTasks() {
        runDailyDistributedTask(
            "生成防御响应任务",
            CREATE_PATROL_QUOTA_LOCK_PREFIX,
            CREATE_PATROL_QUOTA_SUCCESS_PREFIX,
            CREATE_PATROL_QUOTA_LOCK_HOURS,
            "default",
            this::scheduleCreatePatrolQuotaTasksData
        );
    }


    private boolean generateDefRespPlanData() {
        try {
            return executeGenerateDefRespPlan("schedule", null) != null;
        } catch (Exception e) {
            log.error("生成防御响应方案定时任务执行失败", e);
            return false;
        }
    }

    private boolean closeExpiredPendingAlarmsData() {
        try {
            int count = dataAlarmService.closeExpiredUntriggeredAlarms(new Date());
            if (count > 0) {
                log.info("关闭已过有效期未触发预警完成, 本次关闭: {}", count);
            }
            return true;
        } catch (Exception e) {
            log.error("关闭已过有效期未触发预警执行失败", e);
            return false;
        }
    }

    private boolean scheduleCreatePatrolQuotaTasksData() {
        try {
            return dzAutomationService.runDailyPatrolGenerationIfDue();
        } catch (Exception e) {
            log.error("每日8点30分生成防御响应任务执行失败", e);
            return false;
        }
    }

    public enum TaskTriggerResult {
        SUCCESS,
        ALREADY_SUCCESS,
        RUNNING,
        FAILED
    }
}
