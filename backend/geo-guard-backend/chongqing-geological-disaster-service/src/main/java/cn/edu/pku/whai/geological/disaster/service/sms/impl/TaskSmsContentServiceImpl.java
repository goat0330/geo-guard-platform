/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.po.*;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 按任务来源生成任务短信正文，并在创建任务时固化单任务正文快照。
 * 聚合短信仅在推送预览与批量推送时通过 {@link #generateGroupedSmsContent} 生成。
 */
@Service
@RequiredArgsConstructor
public class TaskSmsContentServiceImpl implements ITaskSmsContentService {

    private static final String DEFAULT_TEXT = "暂无";
    /** 所长监管短信每类任务人员最多展示的姓名数量。 */
    private static final int DIRECTOR_PERSON_DISPLAY_LIMIT = 5;
    private static final String DAILY_PATROL_TEMPLATE = """
        【恩施市自然资源和规划局】经“地象”系统研判，请您在今日（{{risk_time_range}}）巡查、排查、核查中，对{{location}}给予{{attention_label}}（{{risk_level}}）关注，记录斜坡变形迹象，并通过智慧防灾专业版APP反馈。如发现灾（险）情及时上报村干部和乡镇自然资源所负责同志。（试运行，请登录智慧防灾专业版APP查看任务并进行反馈）
        """;
    private static final String DAILY_PATROL_PUSH_TEMPLATE = """
        【恩施市自然资源和规划局】经“地象”系统研判，今日（{{risk_time_range}}）{{keypoint_area_sentence}}请您在今日巡查、排查、核查中，{{risk_items}}记录斜坡变形迹象，并通过智慧防灾专业版APP反馈。如发现灾（险）情及时上报村干部和乡镇自然资源所负责同志。（试运行，请登录智慧防灾专业版APP查看任务并进行反馈）
        """;
    private static final String MONITOR_WARNING_TEMPLATE = """
        【恩施市自然资源和规划局】监测预警任务：今日（{{risk_time_range}}）因监测设备发出预警，{{location}}动态风险等级已上升为{{risk_level}}风险。请您立即开展现场监测巡查，重点关注斜坡变形、裂缝、渗流等异常情况，并通过智慧防灾专业版APP反馈；如发现灾（险）情，请立即上报村干部和乡镇自然资源所负责同志。（试运行，请登录智慧防灾专业版APP查看任务并进行反馈）
        """;
    private static final String MONITOR_WARNING_PUSH_TEMPLATE = """
        【恩施市自然资源和规划局】监测预警任务：今日（{{risk_time_range}}）因监测设备发出预警，您负责的以下斜坡单元动态风险等级已上升：{{risk_items}}。请您立即开展现场监测巡查，重点关注斜坡变形、裂缝、渗流等异常情况，并通过智慧防灾专业版APP反馈；如发现灾（险）情，请立即上报村干部和乡镇自然资源所负责同志。（试运行，请登录智慧防灾专业版APP查看任务并进行反馈）
        """;
    /** 巡查、监测任务推送时发送给乡自规所所长的监管汇总短信模板。 */
    private static final String PATROL_MONITOR_DIRECTOR_PUSH_TEMPLATE = """
        【恩施市自然资源和规划局】经“地象”系统研判，今日（{{risk_time_range}}）您辖区共有{{task_count}}个巡查、监测任务，其中巡查任务{{patrol_task_count}}个、监测任务{{monitor_task_count}}个；{{risk_count_summary}}。重点点位：{{location_summary}}。涉及人员：{{person_summary}}。请您组织、督促上述任务人员于今日按要求完成巡查、监测并通过智慧防灾专业版APP及时反馈；如发现灾（险）情，请及时组织处置并按规定上报。（试运行）
        """;
    private static final String REPORT_VERIFY_TEMPLATE = """
        报灾核查：{{report_time}}接到报灾信息，经初步判断，风险性为{{risk_level}}。请立即开展核查工作。具体位置：{{location}}。重点核查确认地质灾害灾（险）情发生的具体位置、类型、已变形规模和潜在规模、直接损失、潜在的威胁对象、影响范围以及影响程度，初步判断成因、是否会进一步发生变形破坏。核查完成后，第一时间向村书记{{village_secretary_contact}}汇报。
        """;
    private static final String DEF_RESP_PATROL_TEMPLATE = """
        【恩施市自然资源和规划局】
        {{start_time}}，州自然资源和城乡建设局启动地质灾害防御{{level}}级响应。经地象系统研判，{{risk_time_range}}，{{risk_items}}**请您于24小时内完成针对风险斜坡宏观巡查，记录斜坡变形迹象及地表水异常变化等迹象，重点关注房前屋后、道路沿线的地面墙体开裂、挡土墙错动鼓胀、陡崖陡坡落石、坡面异常汇水涌水等迹象，发现险情及时上报村支书（主任）。**（试运行，请登录智慧防灾专业版APP查看并进行任务反馈）
        """;
    private static final String DEF_RESP_MONITOR_TEMPLATE = """
        【恩施市自然资源和规划局】
        {{start_time}}，州自然资源和城乡建设局启动地质灾害防御{{level}}级响应。经地象系统研判，{{risk_time_range}}，{{risk_items}}请您于24小时内完成对负责监测风险斜坡的宏观巡查，按照既定巡查路线做好沿途观测和监测变形情况记录，重点关注地表、建筑物、植被变形有无加剧或新增，地表水有无浑浊、流量异常等现象，发现险情及时上报村支书（主任）。（试运行，请登录智慧防灾专业版APP查看并进行任务反馈）
        """;
    private static final String DEF_RESP_SPECIAL_MANAGER_TEMPLATE = """
        【恩施市自然资源和规划局】
        {{start_time}}，州自然资源和城乡建设局启动地质灾害防御{{level}}级响应。经地象系统研判，{{risk_time_range}}，{{risk_items}}请您组织监督巡查员、监测员完成宏观巡查，发生险情时及时上报乡镇自然资源所所长，并配合开展应急调查、先期处置工作。（试运行）
        """;
    private static final String DEF_RESP_ASSIST_TEMPLATE = """
        【恩施市自然资源和规划局】
        {{start_time}}，州自然资源和城乡建设局启动地质灾害防御{{level}}级响应。经“地象”系统研判，{{risk_time_range}}，{{street_risk_summary}}请您立即开展应急驻守，配合指导巡查员、监测员完成宏观巡查，发生险情时及时开展应急调查和先期处置，并上报乡镇自然资源所所长。（试运行）
        """;
    private static final String DEF_RESP_RESOURCE_DIRECTOR_TEMPLATE = """
        【恩施市自然资源和规划局】
        {{start_time}}，州自然资源和城乡建设局启动地质灾害防御{{level}}级响应。经地象系统研判，{{risk_time_range}}，{{street_risk_summary}}请您立即组织指导巡查员、监测员开展宏观巡查，并对发现险情及时组织技术支撑单位开展应急调查和先期处置，视灾险情情况及时上报分管乡镇长、县自然资源和规划局。（试运行）
        """;
    private static final String EMERGENCY_TEMPLATE = """
        【地质灾害巡查任务】[{{region}}自然资源和城乡建设局]指令：请{{assignee}}立即对{{event_name}}开展加密巡查。任务要求：{{task_requirement}}记录方式：拍摄现场照片并填写 APP 系统。反馈时限：{{deadline}}。紧急联系：乡长{{command_phone}}。
        """;

    private final ISlopeUnitService slopeUnitService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;

    private record DefRespRiskUnit(Long riskId,
                                   String unitId,
                                   Integer dynamicRiskLevel,
                                   Date riskCreateDate,
                                   SlopeUnit slopeUnit,
                                   SlopeUnitGridMemberRelationVo relationVo) {
    }

    /** 所长监管短信中的去重联系人，按任务顺序保留首次出现的联系人。 */
    private static final class DirectorPersonContact {
        private String name;
        private final String phone;

        private DirectorPersonContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        private String format() {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(phone)) {
                return name + "（电话：" + phone + "）";
            }
            if (StringUtils.isNotBlank(name)) {
                return name + "（电话待核定）";
            }
            if (StringUtils.isNotBlank(phone)) {
                return "待核定（电话：" + phone + "）";
            }
            return "待核定";
        }
    }

    @Override
    public String populateSmsContent(DzTaskDistList task) {
        String content = generateSmsContent(task);
        if (task != null) {
            task.setSmsContent(content);
        }
        return content;
    }

    @Override
    public String generateSmsContent(DzTaskDistList task) {
        if (task == null) {
            return "";
        }
        SlopeUnit slopeUnit = loadSlopeUnit(task.getUnitId());
        return switch (Objects.requireNonNullElse(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL)) {
            case 0, 1, 6, 7 -> buildDailyPatrolSms(task, slopeUnit, loadRisk(task.getRiskId()));
            case 2 -> buildReportVerifySms(task, slopeUnit, loadReport(task.getReportId()));
            case 3 -> buildDefRespSms(task, slopeUnit, loadDefPlan(task.getDefId()));
            case 4 -> buildEmergencySms(task, slopeUnit, loadHandle(task.getHandleId()));
            case 5 -> buildMonitorWarningSms(task, slopeUnit, loadRisk(task.getRiskId()));
            default -> throw new IllegalArgumentException("未知的任务来源");
        };
    }

    @Override
    public String generatePushSmsContent(List<DzTaskDistList> tasks) {
        return generateGroupedSmsContent(tasks);
    }

    @Override
    public String generateGroupedSmsContent(List<DzTaskDistList> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "";
        }
        List<DzTaskDistList> validTasks = tasks.stream().filter(Objects::nonNull).toList();
        if (validTasks.isEmpty()) {
            return "";
        }
        Integer sourceType = Objects.requireNonNullElse(validTasks.getFirst().getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL);
        if (DzTaskDistList.SOURCE_TYPE_EVAL.equals(sourceType)) {
            if (validTasks.getFirst().getDefId() != null) {
                return generateDefRespGroupedSmsContent(validTasks);
            }
            return buildDailyPatrolPushSms(validTasks);
        }
        if (DzTaskDistList.SOURCE_TYPE_DEF_RESP.equals(sourceType)) {
            return generateDefRespGroupedSmsContent(validTasks);
        }
        if (DzTaskDistList.SOURCE_TYPE_REPORT.equals(sourceType)) {
            return buildReportGroupedSms(validTasks);
        }
        if (DzTaskDistList.SOURCE_TYPE_EMERGENCY.equals(sourceType)) {
            return buildEmergencyGroupedSms(validTasks);
        }
        if (DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING.equals(sourceType)) {
            return buildMonitorWarningPushSms(validTasks);
        }
        if (DzTaskDistList.SOURCE_TYPE_MANUAL.equals(sourceType)
            || DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE.equals(sourceType)) {
            return buildDailyPatrolPushSms(validTasks);
        }
        return generateSmsContent(validTasks.getFirst());
    }

    @Override
    public String generatePatrolMonitorDirectorGroupedSmsContent(List<DzTaskDistList> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "";
        }
        List<DzTaskDistList> validTasks = tasks.stream().filter(Objects::nonNull).toList();
        if (validTasks.isEmpty()) {
            return "";
        }
        Map<Long, DzRiskAssessment> riskMap = loadRiskMap(validTasks);
        Map<String, SlopeUnit> slopeUnitMap = loadSlopeUnitMap(validTasks);
        List<DzTaskDistList> uniqueUnitTasks = buildDirectorUniqueUnitTasks(validTasks, riskMap);
        long extremeRiskCount = uniqueUnitTasks.stream()
            .filter(task -> Objects.equals(resolveDailyPatrolRiskLevel(task, riskMap), 4))
            .count();
        long highRiskCount = uniqueUnitTasks.stream()
            .filter(task -> Objects.equals(resolveDailyPatrolRiskLevel(task, riskMap), 3))
            .count();
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("risk_time_range", formatDailyPatrolRiskTimeRange(resolveDailyPatrolReferenceDate(validTasks, riskMap)));
        placeholders.put("task_count", String.valueOf(validTasks.size()));
        placeholders.put("patrol_task_count", String.valueOf(validTasks.stream().filter(this::isDirectorPatrolTask).count()));
        placeholders.put("monitor_task_count", String.valueOf(validTasks.stream().filter(this::isDirectorMonitorTask).count()));
        placeholders.put("risk_count_summary", buildDailyPatrolDirectorRiskCountSummary(extremeRiskCount, highRiskCount));
        placeholders.put("location_summary", buildDailyPatrolDirectorLocationSummary(uniqueUnitTasks, riskMap, slopeUnitMap));
        placeholders.put("person_summary", buildDirectorPersonSummary(validTasks));
        return renderTemplate(PATROL_MONITOR_DIRECTOR_PUSH_TEMPLATE, placeholders);
    }

    @Override
    public String generateDefRespGroupedSmsContent(List<DzTaskDistList> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "";
        }
        List<DzTaskDistList> validTasks = tasks.stream().filter(Objects::nonNull).toList();
        if (validTasks.isEmpty()) {
            return "";
        }
        DzTaskDistList firstTask = validTasks.getFirst();
        if (firstTask.getDefId() == null) {
            return generateSmsContent(firstTask);
        }
        return generateDefRespRoleSmsContent(
            firstTask.getDefId(),
            ITaskSmsContentService.DEF_RESP_ROLE_PATROL,
            firstTask.getResponsiblePerson(),
            firstTask.getResponsiblePersonPhone(),
            firstTask.getCreateDate()
        );
    }

    @Override
    public String generateDefRespRoleSmsContent(Long defId,
                                                String roleType,
                                                String receiverName,
                                                String receiverPhone,
                                                Date referenceDate) {
        DefRespPlan plan = loadDefPlan(defId);
        List<DefRespRiskUnit> riskUnits = listDefRespRiskUnits(plan, referenceDate);
        return buildDefRespRoleSms(plan, riskUnits, roleType, receiverName, receiverPhone, referenceDate);
    }

    @Override
    public Map<String, DefRespSmsContentResult> generateDefRespRoleSmsContents(Long defId,
                                                                               List<DefRespSmsReceiver> receivers,
                                                                               Date referenceDate) {
        if (defId == null || receivers == null || receivers.isEmpty()) {
            return Map.of();
        }
        DefRespPlan plan = loadDefPlan(defId);
        List<DefRespRiskUnit> riskUnits = listDefRespRiskUnits(plan, referenceDate);
        List<CompletableFuture<DefRespSmsContentResult>> futures = receivers.stream()
                                                                            .filter(Objects::nonNull)
                                                                            .map(receiver -> CompletableFuture.supplyAsync(() ->
                                                                                    new DefRespSmsContentResult(
                                                                                        receiver.roleType(),
                                                                                        receiver.receiverName(),
                                                                                        receiver.receiverPhone(),
                                                                                        buildDefRespRoleSms(
                                                                                            plan,
                                                                                            riskUnits,
                                                                                            receiver.roleType(),
                                                                                            receiver.receiverName(),
                                                                                            receiver.receiverPhone(),
                                                                                            referenceDate
                                                                                        )
                                                                                    ),
                                                                                Run.executor))
                                                                            .toList();
        Map<String, DefRespSmsContentResult> result = new LinkedHashMap<>();
        for (CompletableFuture<DefRespSmsContentResult> future : futures) {
            DefRespSmsContentResult item = future.join();
            if (item == null || StringUtils.isBlank(item.receiverPhone())) {
                continue;
            }
            result.put(buildDefRespReceiverKey(item.roleType(), item.receiverPhone()), item);
        }
        return result;
    }

    @Override
    public Map<String, DefRespStartSmsContentResult> generateDefRespStartSmsContents(Long defId,
                                                                                      List<DefRespStartSmsReceiver> receivers,
                                                                                      Date referenceDate) {
        if (defId == null || receivers == null || receivers.isEmpty()) {
            return Map.of();
        }
        DefRespPlan plan = loadDefPlan(defId);
        Date actualReferenceDate = firstNonNull(referenceDate, plan != null ? plan.getCreateDate() : null, new Date());
        List<CompletableFuture<DefRespStartSmsContentResult>> futures = receivers.stream()
            .filter(Objects::nonNull)
            .map(receiver -> CompletableFuture.supplyAsync(() ->
                    new DefRespStartSmsContentResult(
                        receiver.bizKey(),
                        receiver.bizName(),
                        receiver.receiverName(),
                        receiver.receiverPhone(),
                        receiver.adRegionLevel(),
                        receiver.countyLevel(),
                        receiver.adRegionNames(),
                        receiver.streetNames(),
                        receiver.streetLevelSummaries(),
                        receiver.villageNames(),
                        buildDefRespStartSms(plan, receiver, actualReferenceDate)
                    ),
                Run.executor))
            .toList();
        Map<String, DefRespStartSmsContentResult> result = new LinkedHashMap<>();
        for (CompletableFuture<DefRespStartSmsContentResult> future : futures) {
            DefRespStartSmsContentResult item = future.join();
            if (item == null || StringUtils.isBlank(item.receiverPhone())) {
                continue;
            }
            result.put(buildDefRespStartSmsReceiverKey(item.bizKey(), item.receiverPhone()), item);
        }
        return result;
    }

    private String buildDailyPatrolSms(DzTaskDistList task, SlopeUnit slopeUnit, DzRiskAssessment risk) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("location", buildSlopeLocation(slopeUnit, task.getUnitId()));
        placeholders.put("risk_level", resolveRiskLevel(risk != null ? risk.getDynamicRiskLevel() : null));
        placeholders.put("attention_label", resolveDailyPatrolAttentionLabel(risk != null ? risk.getDynamicRiskLevel() : null));
        placeholders.put("risk_time_range", formatDailyPatrolRiskTimeRange(firstNonNull(
            risk != null ? risk.getCreateDate() : null,
            task.getCreateDate(),
            new Date()
        )));
        return renderTemplate(DAILY_PATROL_TEMPLATE, placeholders);
    }

    private String buildMonitorWarningSms(DzTaskDistList task, SlopeUnit slopeUnit, DzRiskAssessment risk) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("location", buildSlopeLocation(slopeUnit, task.getUnitId()));
        placeholders.put("risk_level", resolveRiskLevel(risk != null ? risk.getDynamicRiskLevel() : null));
        placeholders.put("risk_time_range", formatDailyPatrolRiskTimeRange(firstNonNull(
            risk != null ? risk.getCreateDate() : null,
            task.getCreateDate(),
            new Date()
        )));
        return renderTemplate(MONITOR_WARNING_TEMPLATE, placeholders);
    }

    private String buildMonitorWarningPushSms(List<DzTaskDistList> tasks) {
        Map<Long, DzRiskAssessment> riskMap = loadRiskMap(tasks);
        Map<String, SlopeUnit> slopeUnitMap = loadSlopeUnitMap(tasks);
        String riskItems = tasks.stream()
                                .map(task -> buildMonitorWarningRiskItem(task, riskMap, slopeUnitMap))
                                .filter(StringUtils::isNotBlank)
                                .distinct()
                                .collect(java.util.stream.Collectors.joining("；"));
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("risk_time_range", formatDailyPatrolRiskTimeRange(resolveDailyPatrolReferenceDate(tasks, riskMap)));
        placeholders.put("risk_items", defaultIfBlank(riskItems, "相关斜坡单元（风险等级待核定）"));
        return renderTemplate(MONITOR_WARNING_PUSH_TEMPLATE, placeholders);
    }

    private String buildMonitorWarningRiskItem(DzTaskDistList task,
                                               Map<Long, DzRiskAssessment> riskMap,
                                               Map<String, SlopeUnit> slopeUnitMap) {
        if (task == null) {
            return "";
        }
        String location = buildStreetVillageUnitName(resolveSlopeUnit(task, slopeUnitMap), task.getUnitId());
        Integer riskLevel = resolveDailyPatrolRiskLevel(task, riskMap);
        return location + "（已上升为" + resolveRiskLevel(riskLevel) + "风险）";
    }

    private String buildDailyPatrolPushSms(List<DzTaskDistList> tasks) {
        Map<Long, DzRiskAssessment> riskMap = loadRiskMap(tasks);
        Map<String, SlopeUnit> slopeUnitMap = loadSlopeUnitMap(tasks);
        Map<Integer, List<DzTaskDistList>> tasksByRiskLevel = tasks.stream()
                                                                   .collect(
                                                                       java.util.stream.Collectors.groupingBy(
                                                                           task -> resolveDailyPatrolRiskLevel(task, riskMap),
                                                                           LinkedHashMap::new,
                                                                           java.util.stream.Collectors.toList()
                                                                       )
                                                                   );
        String riskItems = tasksByRiskLevel.entrySet()
                                           .stream()
                                           .sorted(Map.Entry.comparingByKey(
                                               Comparator.nullsLast(Comparator.reverseOrder())
                                           ))
                                           .map(entry -> buildRiskLevelSummaryLine(entry.getKey(), entry.getValue(), slopeUnitMap))
                                           .filter(StringUtils::isNotBlank)
                                           .collect(java.util.stream.Collectors.joining(""));
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("risk_time_range", formatDailyPatrolRiskTimeRange(resolveDailyPatrolReferenceDate(tasks, riskMap)));
        placeholders.put("keypoint_area_sentence", defaultIfBlank(
            buildDailyPatrolKeypointAreaSentence(tasks, riskMap, slopeUnitMap), "，"));
        placeholders.put("risk_items", defaultIfBlank(riskItems, "请关注相关斜坡单元风险变化，"));
        return renderTemplate(DAILY_PATROL_PUSH_TEMPLATE, placeholders);
    }

    private String buildReportVerifySms(DzTaskDistList task, SlopeUnit slopeUnit, DzReportDisaster report) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("report_time", formatDateTime(report != null ? report.getCreateDate() : null));
        placeholders.put("risk_level", resolveReportRiskLevel(report));
        placeholders.put("location", buildSlopeLocation(slopeUnit, task.getUnitId()));
        placeholders.put("village_secretary_contact", buildVillageSecretaryContact(task.getUnitId()));
        return renderTemplate(REPORT_VERIFY_TEMPLATE, placeholders);
    }

    private String buildDefRespSms(DzTaskDistList task, SlopeUnit slopeUnit, DefRespPlan plan) {
        if (task == null) {
            return "";
        }
        Date referenceDate = firstNonNull(task.getCreateDate(), plan != null ? plan.getCreateDate() : null, new Date());
        return renderDefRespRoleSms(
            plan,
            listDefRespRiskUnitsForTask(task),
            resolveDefRespRoleType(task),
            task.getResponsiblePerson(),
            referenceDate
        );
    }

    private String buildDefRespStartSms(DefRespPlan plan,
                                        DefRespStartSmsReceiver receiver,
                                        Date referenceDate) {
        if (plan == null || receiver == null || StringUtils.isBlank(receiver.smsTemplate())) {
            return "";
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("start_time", formatDateTime(firstNonNull(plan.getCreateDate(), referenceDate, new Date())));
        placeholders.put("response_level", resolveRespLevel(firstNonNull(receiver.countyLevel(), plan.getLevel())));
        placeholders.put("county_name", firstNonBlank(plan.getCounty()));
        placeholders.put("street_names", joinRegionNames(receiver.streetNames()));
        placeholders.put("street_level_summary", joinRegionNames(receiver.streetLevelSummaries()));
        placeholders.put("receiver_name", receiver.receiverName());
        placeholders.put("org_name", firstNonBlank(plan.getResponsibilityUnit(), "恩施市自然资源和规划局"));
        placeholders.put("village_name", joinRegionNames(receiver.villageNames()));
        return renderStartupTemplate(receiver.smsTemplate(), placeholders);
    }

    private String buildEmergencySms(DzTaskDistList task, SlopeUnit slopeUnit, DzTaskHandle handle) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("region", joinNonBlank(handle != null ? handle.getCounty() : null, handle != null ? handle.getStreet() : null));
        placeholders.put("assignee", task.getResponsiblePerson());
        placeholders.put("event_name", buildSlopeLocation(slopeUnit, task.getUnitId()));
        placeholders.put("task_requirement", buildEmergencyTaskRequirement(task, task.getInspectionSuggestion()));
        placeholders.put("deadline", task.getSubmitRequire());
        placeholders.put("command_phone", task.getResponsiblePersonPhone());
        return renderTemplate(EMERGENCY_TEMPLATE, placeholders);
    }

    private String buildReportGroupedSms(List<DzTaskDistList> tasks) {
        List<String> locations = tasks.stream()
                                      .map(task -> buildSlopeLocation(loadSlopeUnit(task.getUnitId()), task.getUnitId()))
                                      .filter(StringUtils::isNotBlank)
                                      .distinct()
                                      .limit(5)
                                      .toList();
        String locationText = locations.isEmpty() ? DEFAULT_TEXT : String.join("、", locations);
        if (tasks.size() > locations.size()) {
            locationText += "等" + tasks.size() + "处";
        }
        DzTaskDistList firstTask = tasks.getFirst();
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("report_time", formatDateTime(firstNonNull(resolveEarliestTaskCreateDate(tasks), new Date())));
        placeholders.put("risk_level", "多处隐患");
        placeholders.put("location", locationText);
        placeholders.put("village_secretary_contact", buildVillageSecretaryContact(firstTask != null ? firstTask.getUnitId() : null));
        return renderTemplate(REPORT_VERIFY_TEMPLATE, placeholders);
    }

    private String buildEmergencyGroupedSms(List<DzTaskDistList> tasks) {
        DzTaskDistList firstTask = tasks.getFirst();
        DzTaskHandle handle = firstTask == null ? null : loadHandle(firstTask.getHandleId());
        List<String> eventNames = tasks.stream()
                                       .map(task -> buildSlopeLocation(loadSlopeUnit(task.getUnitId()), task.getUnitId()))
                                       .filter(StringUtils::isNotBlank)
                                       .distinct()
                                       .limit(5)
                                       .toList();
        String eventName = eventNames.isEmpty() ? DEFAULT_TEXT : String.join("、", eventNames);
        if (tasks.size() > eventNames.size()) {
            eventName += "等" + tasks.size() + "处";
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("region", joinNonBlank(handle != null ? handle.getCounty() : null, handle != null ? handle.getStreet() : null));
        placeholders.put("assignee", firstTask != null ? firstTask.getResponsiblePerson() : null);
        placeholders.put("event_name", eventName);
        placeholders.put("task_requirement", buildEmergencyTaskRequirement(firstTask,
            summarizeTaskField(tasks, DzTaskDistList::getInspectionSuggestion)));
        placeholders.put("deadline", summarizeTaskField(tasks, DzTaskDistList::getSubmitRequire));
        placeholders.put("command_phone", firstTask != null ? firstTask.getResponsiblePersonPhone() : null);
        return renderTemplate(EMERGENCY_TEMPLATE, placeholders);
    }

    private String buildEmergencyTaskRequirement(DzTaskDistList task, String requirement) {
        String actualRequirement = defaultIfBlank(requirement, DEFAULT_TEXT);
        if (!isEmergencyMonitoringTask(task)) {
            return actualRequirement;
        }
        actualRequirement = normalizeMonitoringResponsibility(task, actualRequirement);
        String monitoringTaskType = resolveMonitoringTaskType(task, actualRequirement);
        if (StringUtils.isBlank(monitoringTaskType) || actualRequirement.contains("任务类型：")) {
            return actualRequirement;
        }
        return "任务类型：" + monitoringTaskType + "。" + actualRequirement;
    }

    private boolean isEmergencyMonitoringTask(DzTaskDistList task) {
        return task != null
            && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)
            && DzTaskDistList.isMonitoringPlanType(task.getPlanType());
    }

    private String normalizeMonitoringResponsibility(DzTaskDistList task, String requirement) {
        if (task == null || StringUtils.isBlank(requirement)
            || StringUtils.isBlank(task.getResponsiblePerson())
            || StringUtils.isBlank(task.getResponsiblePersonPhone())) {
            return requirement;
        }
        String replacement = "明确" + task.getResponsiblePerson().trim()
            + "（电话：" + task.getResponsiblePersonPhone().trim() + "）为责任人";
        return requirement.replaceAll(
            "明确[^，。；;]*?（电话[:：][^）]*）为责任人",
            java.util.regex.Matcher.quoteReplacement(replacement)
        );
    }

    private String resolveMonitoringTaskType(DzTaskDistList task, String requirement) {
        if (task != null && Objects.equals(task.getPlanType(), DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING)) {
            return "仪器监测";
        }
        if (task != null && Objects.equals(task.getPlanType(), DzTaskDistList.PLAN_TYPE_MONITORING)) {
            return "群测群防巡查";
        }
        String responsibleName = task == null ? null : task.getResponsiblePerson();
        if (StringUtils.isNotBlank(responsibleName)) {
            String normalizedName = responsibleName.trim();
            if (normalizedName.contains("自规") || normalizedName.contains("所长")) {
                return "仪器监测";
            }
            if (normalizedName.contains("巡查")) {
                return "群测群防巡查";
            }
        }
        boolean hasMassPrevention = StringUtils.isNotBlank(requirement) && requirement.contains("群测群防");
        boolean hasInstrument = StringUtils.isNotBlank(requirement) && requirement.contains("仪器监测");
        if (hasMassPrevention && !hasInstrument) {
            return "群测群防巡查";
        }
        if (hasInstrument && !hasMassPrevention) {
            return "仪器监测";
        }
        return null;
    }

    private String buildDefRespRoleSms(DefRespPlan plan,
                                       List<DefRespRiskUnit> riskUnits,
                                       String roleType,
                                       String receiverName,
                                       String receiverPhone,
                                       Date referenceDate) {
        if (plan == null) {
            return "";
        }
        return renderDefRespRoleSms(
            plan,
            filterDefRespRiskUnitsForRole(riskUnits, roleType, receiverPhone),
            roleType,
            receiverName,
            referenceDate
        );
    }

    private String renderDefRespRoleSms(DefRespPlan plan,
                                        List<DefRespRiskUnit> scopedRiskUnits,
                                        String roleType,
                                        String receiverName,
                                        Date referenceDate) {
        if (plan == null) {
            return "";
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("start_time", formatDateTime(plan.getCreateDate()));
        placeholders.put("level", resolveRespLevel(plan.getLevel()));
        placeholders.put("risk_time_range", formatDefRespRiskTimeRange(firstNonNull(referenceDate, plan.getCreateDate(), new Date())));
        placeholders.put("receiver_name", defaultIfBlank(receiverName, DEFAULT_TEXT));
        return switch (defaultIfBlank(roleType, ITaskSmsContentService.DEF_RESP_ROLE_PATROL)) {
            case ITaskSmsContentService.DEF_RESP_ROLE_MONITOR -> {
                placeholders.put("risk_items", buildRoleRiskItems(scopedRiskUnits));
                yield renderTemplate(DEF_RESP_MONITOR_TEMPLATE, placeholders);
            }
            case ITaskSmsContentService.DEF_RESP_ROLE_SPECIAL_MANAGER -> {
                placeholders.put("risk_items", buildRoleRiskItems(scopedRiskUnits));
                yield renderTemplate(DEF_RESP_SPECIAL_MANAGER_TEMPLATE, placeholders);
            }
            case ITaskSmsContentService.DEF_RESP_ROLE_ASSISTANT_MANAGER -> {
                placeholders.put("street_risk_summary", buildStreetRiskSummary(scopedRiskUnits));
                yield renderTemplate(DEF_RESP_ASSIST_TEMPLATE, placeholders);
            }
            case ITaskSmsContentService.DEF_RESP_ROLE_RESOURCE_DIRECTOR -> {
                placeholders.put("street_risk_summary", buildStreetRiskSummary(scopedRiskUnits));
                yield renderTemplate(DEF_RESP_RESOURCE_DIRECTOR_TEMPLATE, placeholders);
            }
            default -> {
                placeholders.put("risk_items", buildRoleRiskItems(scopedRiskUnits));
                yield renderTemplate(DEF_RESP_PATROL_TEMPLATE, placeholders);
            }
        };
    }

    private List<DefRespRiskUnit> filterDefRespRiskUnitsForRole(List<DefRespRiskUnit> riskUnits,
                                                                String roleType,
                                                                String receiverPhone) {
        return switch (defaultIfBlank(roleType, ITaskSmsContentService.DEF_RESP_ROLE_PATROL)) {
            case ITaskSmsContentService.DEF_RESP_ROLE_MONITOR ->
                filterDefRespRiskUnitsByRelationPhone(riskUnits, receiverPhone, SlopeUnitGridMemberRelationVo::getMonitorPhone);
            case ITaskSmsContentService.DEF_RESP_ROLE_SPECIAL_MANAGER ->
                filterDefRespRiskUnitsByRelationPhone(riskUnits, receiverPhone, SlopeUnitGridMemberRelationVo::getSpecialManagerPhone);
            case ITaskSmsContentService.DEF_RESP_ROLE_ASSISTANT_MANAGER ->
                filterDefRespRiskUnitsByRelationPhone(riskUnits, receiverPhone, SlopeUnitGridMemberRelationVo::getAssistantManagerPhone);
            case ITaskSmsContentService.DEF_RESP_ROLE_RESOURCE_DIRECTOR ->
                filterDefRespRiskUnitsByRelationPhone(riskUnits, receiverPhone, SlopeUnitGridMemberRelationVo::getAdminUserPhone);
            default ->
                filterDefRespRiskUnitsByRelationPhone(riskUnits, receiverPhone, SlopeUnitGridMemberRelationVo::getInspectorPhone);
        };
    }

    private List<DefRespRiskUnit> listDefRespRiskUnitsForTask(DzTaskDistList task) {
        if (task == null || StringUtils.isBlank(task.getUnitId())) {
            return List.of();
        }
        String unitId = task.getUnitId().trim();
        SlopeUnit slopeUnit = loadSlopeUnit(unitId);
        if (slopeUnit == null) {
            return List.of();
        }
        DzRiskAssessment risk = loadRisk(task.getRiskId());
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(unitId);
        return List.of(new DefRespRiskUnit(
            task.getRiskId(),
            unitId,
            risk != null ? risk.getDynamicRiskLevel() : null,
            risk != null ? risk.getCreateDate() : firstNonNull(task.getCreateDate(), new Date()),
            slopeUnit,
            relationVo
        ));
    }

    private List<DefRespRiskUnit> listDefRespRiskUnits(DefRespPlan plan, Date referenceDate) {
        List<String> streets = splitStreets(plan != null ? plan.getStreets() : null);
        if (streets.isEmpty()) {
            return List.of();
        }
        List<SlopeUnit> slopeUnits = loadSlopeUnitsByStreets(streets);
        if (slopeUnits.isEmpty()) {
            return List.of();
        }
        Map<String, SlopeUnit> slopeUnitMap = slopeUnits.stream()
                                                        .filter(Objects::nonNull)
                                                        .filter(item -> StringUtils.isNotBlank(item.getId()))
                                                        .collect(java.util.stream.Collectors.toMap(
                                                            item -> item.getId().trim(),
                                                            item -> item,
                                                            (left, right) -> left,
                                                            LinkedHashMap::new
                                                        ));
        Date queryDate = firstNonNull(referenceDate, plan != null ? plan.getCreateDate() : null, new Date());
        List<DzRiskAssessment> assessments = dzRiskAssessmentMapper.selectList(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<DzRiskAssessment>lambdaQuery()
                                                          .in(DzRiskAssessment::getSlopeUnitId, slopeUnitMap.keySet())
                                                          .in(DzRiskAssessment::getDynamicRiskLevel, List.of(3, 4))
                                                          .between(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(queryDate), DateUtil.endOfDay(queryDate))
                                                          .select(DzRiskAssessment::getId,
                                                              DzRiskAssessment::getSlopeUnitId,
                                                              DzRiskAssessment::getDynamicRiskLevel,
                                                              DzRiskAssessment::getCreateDate)
        );
        if (assessments == null || assessments.isEmpty()) {
            return List.of();
        }
        List<DzRiskAssessment> latestAssessments = assessments.stream()
                                                              .filter(Objects::nonNull)
                                                              .sorted(
                                                                  Comparator.comparing(DzRiskAssessment::getCreateDate, Comparator.nullsLast(Date::compareTo))
                                                                            .thenComparing(DzRiskAssessment::getId, Comparator.nullsLast(Long::compareTo))
                                                                            .reversed()
                                                              )
                                                              .toList();
        List<String> unitIds = latestAssessments.stream()
                                                .filter(Objects::nonNull)
                                                .map(DzRiskAssessment::getSlopeUnitId)
                                                .filter(StringUtils::isNotBlank)
                                                .map(String::trim)
                                                .distinct()
                                                .toList();
        Map<String, SlopeUnitGridMemberRelationVo> relationMap = loadRelationMapByUnitIds(unitIds);
        Map<String, DefRespRiskUnit> riskUnitById = new LinkedHashMap<>();
        for (DzRiskAssessment assessment : latestAssessments) {
            if (assessment == null || StringUtils.isBlank(assessment.getSlopeUnitId())) {
                continue;
            }
            String unitId = assessment.getSlopeUnitId().trim();
            SlopeUnit slopeUnit = slopeUnitMap.get(unitId);
            if (slopeUnit == null) {
                continue;
            }
            riskUnitById.putIfAbsent(unitId, new DefRespRiskUnit(
                assessment.getId(),
                unitId,
                assessment.getDynamicRiskLevel(),
                assessment.getCreateDate(),
                slopeUnit,
                relationMap.get(unitId)
            ));
        }
        return List.copyOf(riskUnitById.values());
    }

    private Map<String, SlopeUnitGridMemberRelationVo> loadRelationMapByUnitIds(List<String> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            return Map.of();
        }
        List<CompletableFuture<Map.Entry<String, SlopeUnitGridMemberRelationVo>>> futures = unitIds.stream()
                                                                                                   .filter(StringUtils::isNotBlank)
                                                                                                   .map(String::trim)
                                                                                                   .distinct()
                                                                                                   .map(unitId -> CompletableFuture.supplyAsync(
                                                                                                       () -> nullableEntry(unitId, slopeUnitGridMemberRelationService.queryByUnitId(unitId)),
                                                                                                       Run.executor
                                                                                                   ))
                                                                                                   .toList();
        Map<String, SlopeUnitGridMemberRelationVo> result = new LinkedHashMap<>();
        for (CompletableFuture<Map.Entry<String, SlopeUnitGridMemberRelationVo>> future : futures) {
            Map.Entry<String, SlopeUnitGridMemberRelationVo> entry = future.join();
            if (entry != null && entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private List<SlopeUnit> loadSlopeUnitsByStreets(List<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return List.of();
        }
        List<cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo> slopeUnitVos = slopeUnitService.querySlopeUnitListByStreets(streets);
        if (slopeUnitVos == null || slopeUnitVos.isEmpty()) {
            return List.of();
        }
        List<String> unitIds = slopeUnitVos.stream()
                                           .map(cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo::getId)
                                           .filter(StringUtils::isNotBlank)
                                           .map(String::trim)
                                           .distinct()
                                           .toList();
        return unitIds.isEmpty() ? List.of() : slopeUnitService.listPoByIds(unitIds);
    }

    private List<DefRespRiskUnit> filterDefRespRiskUnitsByRelationPhone(List<DefRespRiskUnit> riskUnits,
                                                                        String receiverPhone,
                                                                        java.util.function.Function<SlopeUnitGridMemberRelationVo, String> phoneExtractor) {
        if (riskUnits == null || riskUnits.isEmpty() || StringUtils.isBlank(receiverPhone)) {
            return List.of();
        }
        return riskUnits.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> item.relationVo() != null)
                        .filter(item -> StringUtils.equals(
                            defaultIfBlank(phoneExtractor.apply(item.relationVo()), null),
                            receiverPhone.trim()
                        ))
                        .toList();
    }

    private String buildDefRespReceiverKey(String roleType, String receiverPhone) {
        return defaultIfBlank(roleType, "") + "|" + defaultIfBlank(receiverPhone, "");
    }

    private String buildDefRespStartSmsReceiverKey(String bizKey, String receiverPhone) {
        return defaultIfBlank(bizKey, "") + "|" + defaultIfBlank(receiverPhone, "");
    }

    private <K, V> Map.Entry<K, V> nullableEntry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private String buildRoleRiskItems(List<DefRespRiskUnit> riskUnits) {
        if (riskUnits == null || riskUnits.isEmpty()) {
            return "当前相关街道暂无地质灾害高风险斜坡。";
        }
        Map<Integer, List<DefRespRiskUnit>> grouped = riskUnits.stream()
                                                               .collect(java.util.stream.Collectors.groupingBy(
                                                                   DefRespRiskUnit::dynamicRiskLevel,
                                                                   LinkedHashMap::new,
                                                                   java.util.stream.Collectors.toList()
                                                               ));
        String content = grouped.entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                                .map(entry -> buildRiskLevelItems(entry.getKey(), entry.getValue()))
                                .filter(StringUtils::isNotBlank)
                                .collect(java.util.stream.Collectors.joining("，"));
        return StringUtils.isNotBlank(content) ? content + "。" : "当前相关街道暂无地质灾害高风险斜坡。";
    }

    private String buildRiskLevelItems(Integer level, List<DefRespRiskUnit> riskUnits) {
        LinkedHashSet<String> villageGroupedItems = riskUnits.stream()
                                                             .filter(Objects::nonNull)
                                                             .collect(
                                                                 java.util.stream.Collectors.groupingBy(
                                                                     this::buildStreetVillageKey,
                                                                     LinkedHashMap::new,
                                                                     java.util.stream.Collectors.toList()
                                                                 )
                                                             )
                                                             .values()
                                                             .stream()
                                                             .map(this::buildVillageGroupedRiskItem)
                                                             .filter(StringUtils::isNotBlank)
                                                             .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (villageGroupedItems.isEmpty()) {
            return "";
        }
        return String.join("、", villageGroupedItems) + "地质灾害风险[" + resolveRiskLevel(level) + "]";
    }

    private String buildStreetRiskSummary(List<DefRespRiskUnit> riskUnits) {
        if (riskUnits == null || riskUnits.isEmpty()) {
            return "当前相关街道暂无地质灾害高风险斜坡。";
        }
        Map<String, Map<Integer, Long>> countByStreetAndLevel = new LinkedHashMap<>();
        for (DefRespRiskUnit item : riskUnits) {
            if (item == null || item.slopeUnit() == null) {
                continue;
            }
            String street = defaultIfBlank(item.slopeUnit().getStreet(), "待核定乡镇");
            Map<Integer, Long> levelCount = countByStreetAndLevel.computeIfAbsent(street, ignore -> new LinkedHashMap<>());
            levelCount.merge(item.dynamicRiskLevel(), 1L, Long::sum);
        }
        return countByStreetAndLevel.entrySet()
                                    .stream()
                                    .map(entry -> buildStreetRiskSummaryLine(entry.getKey(), entry.getValue()))
                                    .filter(StringUtils::isNotBlank)
                                    .collect(java.util.stream.Collectors.joining("；", "", "。"));
    }

    private String buildStreetRiskSummaryLine(String street, Map<Integer, Long> levelCount) {
        long veryHighCount = levelCount.getOrDefault(4, 0L);
        long highCount = levelCount.getOrDefault(3, 0L);
        List<String> parts = new ArrayList<>();
        if (veryHighCount > 0) {
            parts.add("地质灾害[极高]风险斜坡有" + veryHighCount + "个");
        }
        if (highCount > 0) {
            parts.add("地质灾害[高]风险斜坡有" + highCount + "个");
        }
        if (parts.isEmpty()) {
            return "";
        }
        return street + String.join("，", parts);
    }

    private String buildStreetVillageKey(DefRespRiskUnit riskUnit) {
        if (riskUnit == null) {
            return null;
        }
        SlopeUnit slopeUnit = riskUnit.slopeUnit();
        String streetName = defaultIfBlank(slopeUnit != null ? slopeUnit.getStreet() : null, "待核定乡镇");
        String villageName = firstNonBlank(
            slopeUnit != null ? slopeUnit.getVillage() : null,
            slopeUnit != null ? slopeUnit.getCommunity() : null,
            "相关区域"
        );
        return streetName + "|" + villageName;
    }

    private String buildVillageGroupedRiskItem(List<DefRespRiskUnit> villageRiskUnits) {
        if (villageRiskUnits == null || villageRiskUnits.isEmpty()) {
            return "";
        }
        DefRespRiskUnit firstRiskUnit = villageRiskUnits.getFirst();
        SlopeUnit slopeUnit = firstRiskUnit != null ? firstRiskUnit.slopeUnit() : null;
        String streetName = defaultIfBlank(slopeUnit != null ? slopeUnit.getStreet() : null, "待核定乡镇");
        String villageName = firstNonBlank(
            slopeUnit != null ? slopeUnit.getVillage() : null,
            slopeUnit != null ? slopeUnit.getCommunity() : null,
            "相关区域"
        );
        LinkedHashSet<String> slopeNames = villageRiskUnits.stream()
                                                           .filter(Objects::nonNull)
                                                           .map(item -> buildSmsSlopeName(item.unitId()))
                                                           .filter(StringUtils::isNotBlank)
                                                           .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (slopeNames.isEmpty()) {
            return streetName + villageName + "相关斜坡";
        }
        return streetName + villageName + String.join("、", slopeNames);
    }

    private String buildSmsSlopeName(String unitId) {
        String normalizedUnitId = defaultIfBlank(unitId, null);
        return normalizedUnitId == null ? "相关斜坡" : normalizedUnitId + "号斜坡";
    }

    private String resolveDefRespRoleType(DzTaskDistList task) {
        if (task == null) {
            return ITaskSmsContentService.DEF_RESP_ROLE_PATROL;
        }
        if (StringUtils.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_MONITOR)) {
            return ITaskSmsContentService.DEF_RESP_ROLE_MONITOR;
        }
        return ITaskSmsContentService.DEF_RESP_ROLE_PATROL;
    }


    private SlopeUnit loadSlopeUnit(String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return null;
        }
        List<SlopeUnit> slopeUnits = slopeUnitService.listPoByIds(List.of(unitId.trim()));
        return slopeUnits == null || slopeUnits.isEmpty() ? null : slopeUnits.getFirst();
    }

    private Map<Long, DzRiskAssessment> loadRiskMap(List<DzTaskDistList> tasks) {
        List<Long> riskIds = tasks.stream()
                                  .map(DzTaskDistList::getRiskId)
                                  .filter(Objects::nonNull)
                                  .distinct()
                                  .toList();
        if (riskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dzRiskAssessmentMapper.selectByIds(riskIds)
                                     .stream()
                                     .filter(Objects::nonNull)
                                     .collect(java.util.stream.Collectors.toMap(DzRiskAssessment::getId, risk -> risk, (left, right) -> left));
    }

    private Map<String, SlopeUnit> loadSlopeUnitMap(List<DzTaskDistList> tasks) {
        List<String> unitIds = tasks.stream()
                                    .map(DzTaskDistList::getUnitId)
                                    .filter(StringUtils::isNotBlank)
                                    .map(String::trim)
                                    .distinct()
                                    .toList();
        if (unitIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return slopeUnitService.listPoByIds(unitIds)
                               .stream()
                               .filter(Objects::nonNull)
                               .filter(slopeUnit -> StringUtils.isNotBlank(slopeUnit.getId()))
                               .collect(java.util.stream.Collectors.toMap(slopeUnit -> slopeUnit.getId().trim(),
                                   slopeUnit -> slopeUnit,
                                   (left, right) -> left,
                                   LinkedHashMap::new));
    }

    private DzRiskAssessment loadRisk(Long riskId) {
        return riskId == null ? null : dzRiskAssessmentMapper.selectById(riskId);
    }

    private DzReportDisaster loadReport(Long reportId) {
        return reportId == null ? null : dzReportDisasterMapper.selectById(reportId);
    }

    private DefRespPlan loadDefPlan(Long defId) {
        return defId == null ? null : dzDefRespPlanMapper.selectById(defId);
    }

    private DzTaskHandle loadHandle(Long handleId) {
        return handleId == null ? null : dzTaskHandleMapper.selectById(handleId);
    }

    private String buildVillageSecretaryContact(String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return DEFAULT_TEXT;
        }
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(unitId.trim());
        if (relationVo == null) {
            return DEFAULT_TEXT;
        }
        String name = defaultIfBlank(relationVo.getSpecialManager(), null);
        String phone = defaultIfBlank(relationVo.getSpecialManagerPhone(), null);
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(phone)) {
            return name + "（电话" + phone + "）";
        }
        return firstNonBlank(name, phone, DEFAULT_TEXT);
    }

    private String buildSlopeLocation(SlopeUnit slopeUnit, String unitId) {
        return formatLocationWithUnitNames(buildSlopeArea(slopeUnit), List.of(buildSlopeUnitName(unitId)));
    }

    private Integer resolveDailyPatrolRiskLevel(DzTaskDistList task, Map<Long, DzRiskAssessment> riskMap) {
        if (task == null || task.getRiskId() == null) {
            return null;
        }
        DzRiskAssessment risk = riskMap.get(task.getRiskId());
        return risk != null ? risk.getDynamicRiskLevel() : null;
    }

    private String buildDailyPatrolDirectorRiskCountSummary(long extremeRiskCount, long highRiskCount) {
        if (extremeRiskCount <= 0 && highRiskCount <= 0) {
            return "风险等级待核定";
        }
        return "极高风险" + extremeRiskCount + "处、高风险" + highRiskCount + "处";
    }

    private String buildDirectorPersonSummary(List<DzTaskDistList> tasks) {
        List<String> summaries = new ArrayList<>();
        List<DzTaskDistList> patrolTasks = tasks.stream().filter(this::isDirectorPatrolTask).toList();
        if (!patrolTasks.isEmpty()) {
            summaries.add("巡查员：" + buildDirectorRolePersonSummary(patrolTasks));
        }
        List<DzTaskDistList> monitorTasks = tasks.stream().filter(this::isDirectorMonitorTask).toList();
        if (!monitorTasks.isEmpty()) {
            summaries.add("监测员：" + buildDirectorRolePersonSummary(monitorTasks));
        }
        return summaries.isEmpty() ? "任务人员待核定" : String.join("；", summaries);
    }

    private String buildDirectorRolePersonSummary(List<DzTaskDistList> tasks) {
        Map<String, DirectorPersonContact> contacts = new LinkedHashMap<>();
        for (DzTaskDistList task : tasks) {
            if (task == null) {
                continue;
            }
            String name = normalizeDirectorPersonField(task.getResponsiblePerson());
            String phone = normalizeDirectorPersonField(task.getResponsiblePersonPhone());
            String key = StringUtils.isNotBlank(phone) ? "phone:" + phone : "name:" + defaultIfBlank(name, "");
            DirectorPersonContact contact = contacts.get(key);
            if (contact == null) {
                contacts.put(key, new DirectorPersonContact(name, phone));
            } else if (StringUtils.isBlank(contact.name) && StringUtils.isNotBlank(name)) {
                contact.name = name;
            }
        }
        if (contacts.isEmpty()) {
            return "待核定";
        }
        List<DirectorPersonContact> contactList = new ArrayList<>(contacts.values());
        String summary = contactList.stream()
            .limit(DIRECTOR_PERSON_DISPLAY_LIMIT)
            .map(DirectorPersonContact::format)
            .collect(java.util.stream.Collectors.joining("、"));
        if (contactList.size() > DIRECTOR_PERSON_DISPLAY_LIMIT) {
            summary += "等" + contactList.size() + "人";
        }
        return summary;
    }

    private String normalizeDirectorPersonField(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private List<DzTaskDistList> buildDirectorUniqueUnitTasks(List<DzTaskDistList> tasks,
                                                               Map<Long, DzRiskAssessment> riskMap) {
        Map<String, DzTaskDistList> taskByUnitId = new LinkedHashMap<>();
        for (DzTaskDistList task : tasks) {
            if (task == null || StringUtils.isBlank(task.getUnitId())) {
                continue;
            }
            taskByUnitId.merge(task.getUnitId().trim(), task,
                (left, right) -> selectHigherRiskTask(left, right, riskMap));
        }
        return new ArrayList<>(taskByUnitId.values());
    }

    private DzTaskDistList selectHigherRiskTask(DzTaskDistList left,
                                                 DzTaskDistList right,
                                                 Map<Long, DzRiskAssessment> riskMap) {
        Integer leftRiskLevel = resolveDailyPatrolRiskLevel(left, riskMap);
        Integer rightRiskLevel = resolveDailyPatrolRiskLevel(right, riskMap);
        if (leftRiskLevel == null) {
            if (rightRiskLevel != null) {
                return right;
            }
            return compareDirectorTaskId(left, right) <= 0 ? left : right;
        }
        if (rightRiskLevel == null || leftRiskLevel > rightRiskLevel) {
            return left;
        }
        if (leftRiskLevel.equals(rightRiskLevel)) {
            return compareDirectorTaskId(left, right) <= 0 ? left : right;
        }
        return right;
    }

    private int compareDirectorTaskId(DzTaskDistList left, DzTaskDistList right) {
        Long leftId = left == null ? null : left.getId();
        Long rightId = right == null ? null : right.getId();
        return Comparator.nullsLast(Long::compareTo).compare(leftId, rightId);
    }

    private boolean isDirectorPatrolTask(DzTaskDistList task) {
        if (task == null) {
            return false;
        }
        boolean dailyPatrol = Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)
            && matchesPlanName(task, DzTaskDistList.PLAN_NAME_DAILY_PATROL);
        boolean defRespPatrol = Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)
            && matchesPlanName(task, DzTaskDistList.PLAN_NAME_PATROL);
        return dailyPatrol || defRespPatrol;
    }

    private boolean isDirectorMonitorTask(DzTaskDistList task) {
        return task != null
            && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)
            && matchesPlanName(task, DzTaskDistList.PLAN_NAME_MONITOR);
    }

    private boolean matchesPlanName(DzTaskDistList task, String planName) {
        return Objects.equals(task.getPlanName(), planName) || Objects.equals(task.getTaskSource(), planName);
    }

    private String buildDailyPatrolDirectorLocationSummary(List<DzTaskDistList> tasks,
                                                            Map<Long, DzRiskAssessment> riskMap,
                                                            Map<String, SlopeUnit> slopeUnitMap) {
        List<String> locations = tasks.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator
                .comparing((DzTaskDistList task) -> resolveDailyPatrolRiskLevel(task, riskMap),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DzTaskDistList::getId, Comparator.nullsLast(Long::compareTo)))
            .map(task -> buildStreetVillageUnitName(resolveSlopeUnit(task, slopeUnitMap), task.getUnitId()))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (locations.isEmpty()) {
            return "相关斜坡单元";
        }
        String summary = locations.getFirst();
        if (locations.size() > 1) {
            summary += "等" + locations.size() + "处";
        }
        return summary;
    }

    private String buildRiskLevelSummaryLine(Integer riskLevel,
                                             List<DzTaskDistList> tasks,
                                             Map<String, SlopeUnit> slopeUnitMap) {
        LinkedHashSet<String> unitNames = tasks.stream()
                                               .filter(Objects::nonNull)
                                               .map(task -> buildStreetVillageUnitName(resolveSlopeUnit(task, slopeUnitMap), task.getUnitId()))
                                               .filter(StringUtils::isNotBlank)
                                               .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (unitNames.isEmpty()) {
            return "";
        }
        String unitSummary = String.join("、", unitNames);
        return switch (Objects.requireNonNullElse(riskLevel, -1)) {
            case 4 -> "优先关注（极高风险）" + unitSummary + "，";
            case 3 -> "重点关注（高风险）" + unitSummary + "，";
            default -> "关注" + unitSummary + "（" + resolveRiskLevel(riskLevel) + "），";
        };
    }

    private SlopeUnit resolveSlopeUnit(DzTaskDistList task, Map<String, SlopeUnit> slopeUnitMap) {
        if (task == null || StringUtils.isBlank(task.getUnitId())) {
            return null;
        }
        return slopeUnitMap.get(task.getUnitId().trim());
    }

    private String buildSlopeArea(SlopeUnit slopeUnit) {
        if (slopeUnit == null) {
            return "";
        }
        return joinNonBlank(slopeUnit.getCounty(), slopeUnit.getStreet(), slopeUnit.getVillage());
    }

    private String buildSlopeUnitName(String unitId) {
        String normalizedUnitId = defaultIfBlank(unitId, null);
        return normalizedUnitId == null ? "相关斜坡单元" : normalizedUnitId + "号斜坡单元";
    }

    private String buildVillageUnitName(SlopeUnit slopeUnit, String unitId) {
        String villageName = firstNonBlank(
            slopeUnit != null ? slopeUnit.getVillage() : null,
            slopeUnit != null ? slopeUnit.getCommunity() : null
        );
        return defaultIfBlank(villageName, "相关区域") + buildSlopeUnitName(unitId);
    }

    private String buildStreetVillageUnitName(SlopeUnit slopeUnit, String unitId) {
        String streetName = defaultIfBlank(slopeUnit != null ? slopeUnit.getStreet() : null, "待核定乡镇");
        return streetName + buildVillageUnitName(slopeUnit, unitId);
    }

    private String buildDailyPatrolKeypointAreaSentence(List<DzTaskDistList> tasks,
                                                        Map<Long, DzRiskAssessment> riskMap,
                                                        Map<String, SlopeUnit> slopeUnitMap) {
        List<String> keypointAreas = tasks.stream()
                                          .filter(Objects::nonNull)
                                          .filter(task -> isHighOrVeryHighRisk(resolveDailyPatrolRiskLevel(task, riskMap)))
                                          .map(task -> resolveKeypointAreaName(resolveSlopeUnit(task, slopeUnitMap)))
                                          .filter(StringUtils::isNotBlank)
                                          .collect(java.util.stream.Collectors.groupingBy(
                                              area -> area,
                                              java.util.stream.Collectors.counting()
                                          ))
                                          .entrySet()
                                          .stream()
                                          .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                                                           .thenComparing(Map.Entry.comparingByKey()))
                                          .limit(3)
                                          .map(Map.Entry::getKey)
                                          .toList();
        if (keypointAreas.isEmpty()) {
            return "";
        }
        return "需要高度关注的地质灾害风险区集中在" + String.join("、", keypointAreas) + "，";
    }

    private String resolveKeypointAreaName(SlopeUnit slopeUnit) {
        return firstNonBlank(
            slopeUnit != null ? slopeUnit.getVillage() : null,
            slopeUnit != null ? slopeUnit.getCommunity() : null
        );
    }

    private String formatLocationWithUnitNames(String location, Collection<String> unitNames) {
        String joinedUnitNames = unitNames == null
            ? ""
            : unitNames.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(java.util.stream.Collectors.joining("、"));
        if (StringUtils.isBlank(joinedUnitNames)) {
            return DEFAULT_TEXT;
        }
        return StringUtils.isNotBlank(location) ? location + joinedUnitNames : joinedUnitNames;
    }


    private String resolveRiskLevel(Integer level) {
        return LevelCodeUtil.resolveDynamicRiskSmsName(level);
    }

    private String resolveDailyPatrolAttentionLabel(Integer level) {
        return switch (Objects.requireNonNullElse(level, -1)) {
            case 4 -> "优先";
            case 3 -> "重点";
            default -> "";
        };
    }

    private boolean isHighOrVeryHighRisk(Integer level) {
        return Objects.equals(level, 3) || Objects.equals(level, 4);
    }

    private String resolveReportRiskLevel(DzReportDisaster report) {
        if (report == null) {
            return "待核定";
        }
        return resolveRiskLevel(report.getAiRiskLevel());
    }

    private String resolveRespLevel(Integer level) {
        return StringUtils.defaultIfBlank(LevelCodeUtil.resolveDefenseResponseRoman(level), "Ⅳ");
    }

    private String formatDateTime(java.util.Date date) {
        return date == null ? DEFAULT_TEXT : DateUtil.format(date, "yyyy年M月d日H时");
    }

    private Date resolveEarliestTaskCreateDate(List<DzTaskDistList> tasks) {
        return tasks.stream()
                    .filter(Objects::nonNull)
                    .map(DzTaskDistList::getCreateDate)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);
    }

    private String summarizeTaskField(List<DzTaskDistList> tasks, java.util.function.Function<DzTaskDistList, String> extractor) {
        List<String> values = tasks.stream()
                                   .filter(Objects::nonNull)
                                   .map(extractor)
                                   .filter(StringUtils::isNotBlank)
                                   .map(String::trim)
                                   .distinct()
                                   .limit(3)
                                   .toList();
        if (values.isEmpty()) {
            return DEFAULT_TEXT;
        }
        String text = String.join("；", values);
        if (tasks.stream().filter(Objects::nonNull).map(extractor).filter(StringUtils::isNotBlank).map(String::trim).distinct().count() > values.size()) {
            text += "等";
        }
        return text;
    }

    private Date resolveDailyPatrolReferenceDate(List<DzTaskDistList> tasks, Map<Long, DzRiskAssessment> riskMap) {
        if (tasks == null || tasks.isEmpty()) {
            return new Date();
        }
        for (DzTaskDistList task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.getRiskId() != null) {
                DzRiskAssessment risk = riskMap.get(task.getRiskId());
                if (risk != null && risk.getCreateDate() != null) {
                    return risk.getCreateDate();
                }
            }
            if (task.getCreateDate() != null) {
                return task.getCreateDate();
            }
        }
        return new Date();
    }

    private String formatDailyPatrolRiskTimeRange(Date date) {
        if (date == null) {
            return DEFAULT_TEXT;
        }
        Date dayStart = DateUtil.beginOfDay(date);
        Date rangeStart = DateUtil.offsetHour(dayStart, 8);
        Date rangeEnd = DateUtil.offsetDay(rangeStart, 1);
        return DateUtil.format(rangeStart, "yyyy年M月d日H时") + "-" + DateUtil.format(rangeEnd, "M月d日H时");
    }

    private String formatDefRespRiskTimeRange(Date date) {
        if (date == null) {
            return DEFAULT_TEXT;
        }
        Date dayStart = DateUtil.beginOfDay(date);
        Date rangeStart = DateUtil.offsetHour(dayStart, 8);
        Date rangeEnd = DateUtil.offsetDay(rangeStart, 1);
        return DateUtil.format(rangeStart, "yyyy年M月d日H时") + "-" + DateUtil.format(rangeEnd, "M月d日H时");
    }

    private List<String> splitStreets(String streets) {
        if (StringUtils.isBlank(streets)) {
            return List.of();
        }
        LinkedHashSet<String> results = new LinkedHashSet<>();
        for (String street : streets.split("[,，]")) {
            if (StringUtils.isNotBlank(street)) {
                results.add(street.trim());
            }
        }
        return List.copyOf(results);
    }

    private String compact(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("**", "")
                   .replace("[", "")
                   .replace("]", "")
                   .replace("\r", "")
                   .replace("\n", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String renderTemplate(String template, Map<String, String> placeholders) {
        String text = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", defaultIfBlank(entry.getValue(), DEFAULT_TEXT));
        }
        return compact(text);
    }

    private String renderTemplatePreserveLines(String template, Map<String, String> placeholders) {
        String text = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", defaultIfBlank(entry.getValue(), DEFAULT_TEXT));
        }
        return Arrays.stream(text.replace("\r", "").split("\n"))
                     .map(line -> line == null ? "" : line.strip())
                     .filter(line -> !line.isEmpty())
                     .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String renderStartupTemplate(String template, Map<String, String> placeholders) {
        String text = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", defaultIfBlank(entry.getValue(), DEFAULT_TEXT));
        }
        return compact(text);
    }

    private String joinNonBlank(String... values) {
        return Arrays.stream(values)
                     .filter(StringUtils::isNotBlank)
                     .map(String::trim)
                     .reduce("", String::concat);
    }

    private String joinRegionNames(List<String> values) {
        if (values == null || values.isEmpty()) {
            return DEFAULT_TEXT;
        }
        return values.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .reduce((left, right) -> left + "," + right)
            .orElse(DEFAULT_TEXT);
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(StringUtils::isNotBlank).map(String::trim).findFirst().orElse(null);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
    }
}
