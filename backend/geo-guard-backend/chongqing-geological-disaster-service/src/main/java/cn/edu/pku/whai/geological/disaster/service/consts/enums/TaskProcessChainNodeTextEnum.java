/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 任务流程链路节点文案。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessChainNodeTextEnum {
    PUBLIC_REPORT("群众报灾", "群众通过 APP 上报灾险情"),
    PUBLIC_REPORT_REPORT("群众报灾报告", "群众通过 APP 上报灾险情"),
    PATROL_REPORT_REPORT("巡查员任务报灾", "巡查员通过群众报灾入口上报灾险情"),
    DISASTER_REPORT_GENERATE("生成灾情报告", "群众报灾生成灾情报告"),
    PUBLIC_REPORT_TASK_DISPATCH("下发群众报灾任务", "灾情报告下发群众报灾任务"),
    TASK_INSPECTING("任务核查中", "任务执行人员提交核查中结果"),
    TASK_FEEDBACK("任务反馈", "任务执行人员提交现场反馈"),
    TASK_CLOSE_BY_APP("任务关闭", "任务执行人员提交任务关闭"),
    TASK_CLOSE_DEFAULT("任务关闭", "任务已关闭"),
    TASK_CLOSE_BY_EDIT("任务关闭", "后台修改任务状态为已关闭"),
    TASK_OVERDUE_DEFAULT("任务过期", "任务已过期"),
    TASK_OVERDUE_BY_EDIT("任务过期", "后台修改任务状态为已过期"),
    TASK_OVERDUE_BY_SYSTEM("任务过期", "系统将今天以前未完成任务置为过期"),
    TECH_ASSIST_APPLY("申请技术协查", "任务执行人员提交技术协查申请"),
    TECH_ASSIST_TASK_DISPATCH("下发技术协查任务", "根据斜坡单元协管员自动生成技术协查任务"),
    TECH_ASSIST_TASK_FROM_FEEDBACK("下发技术协查任务", "任务反馈申请技术协查"),
    TASK_FEEDBACK_REPORT("任务反馈报告", "任务反馈生成AI识图报告"),
    TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD("技术协查任务反馈并上报现场处置报告", "技术协查任务反馈并上报现场处置报告"),
    TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD("任务反馈并上报现场处置报告", "提交现场处置报告"),
    SCENE_HANDLE_REPORT_UPLOAD("上报现场处置报告", "提交现场处置报告"),
    REPORT_TO_TOWN_TASK("报送乡镇", "报灾核实后报送乡自规所处置"),
    DANGER_VERIFY_TASK("险情核实任务", "群众报灾触发险情核实任务"),
    DANGER_VERIFY_TASK_DISPATCH("下发险情核实任务", null),
    AI_VERIFY_TASK_DISPATCH("下发AI险情核实任务", "任务反馈报告下发AI险情核实任务"),
    DAILY_PATROL_TASK("日常巡查任务", "系统生成日常巡查任务"),
    MANUAL_TASK_CREATE("手动添加任务", "后台手动添加任务"),
    DEF_RESP_TASK_DISPATCH("下发防御响应任务", "防御响应方案生成并下发任务"),
    DEF_RESP_TASK("防御响应任务", null),
    DEF_RESP_TASK_GENERATE("下发防御响应任务", "防御响应方案下发任务"),
    DEF_RESP_BATCH_DISPATCH("生成防御响应任务", "防御响应方案批量生成并下发任务"),
    EMERGENCY_TASK_DISPATCH("下发应急处置任务", "处置管理方案生成并下发应急处置任务"),
    EMERGENCY_TASK_GENERATE("下发应急处置任务", "处置管理方案下发应急处置"),
    EMERGENCY_TASK("应急处置任务", null),
    EMERGENCY_BATCH_DISPATCH("生成处置任务", "处置管理方案批量生成应急处置任务"),
    EMERGENCY_BATCH_PUSH("推送处置任务", "同一处置批次全部处置任务已推送到 APP"),
    MONITOR_WARNING("监测设备预警", "监测设备触发预警并联动生成任务"),
    MONITOR_WARNING_TASK_DISPATCH("下发监测预警任务", "监测设备预警联动下发任务"),
    ALARM_REPORT_UPLOAD("上传预警报告", "上传预警报告并写入预警信息"),
    REGION_DEF_RESP_FROM_ALARM("根据预警信息开启区域防御响应", "预警信息触发区域防御响应"),
    REGION_DEF_RESP_START("开启区域防御响应", "创建县级区域防御响应记录"),
    REGION_DEF_RESP_PUBLISHED("启动区域防御响应", "防御响应方案状态流转到任务生成与推送"),
    DEF_RESP_ARCHIVED("防御响应结束归档", "防御响应关闭或归档结束"),
    DISASTER_DANGER_CLOSED("灾险情关闭", "关闭报灾并结束灾险情报告分支"),
    HANDLE_START_FROM_REPORT("开启处置管理", "现场处置报告触发处置管理"),
    HANDLE_START_FROM_APP_SCENE_RECORD("开启处置管理", "APP提交现场记录并生成处置管理"),
    GENERATE_EMERGENCY_INVESTIGATION_REPORT("生成应急调查报告", "AI生成应急调查报告初稿"),
    HANDLE_ARCHIVED("结束归档", "处置管理结束并归档"),
    SINGLE_DEF_RESP_START("开启单点防御响应", "处置管理开启单点防御响应"),
    TASK_PUSH_MANUAL("任务推送", "人工推送任务到 APP"),
    TASK_PUSH_SYSTEM("任务推送", "系统自动推送任务到 APP"),
    GENERIC_TASK("任务", "任务生成");

    public static final String LEGACY_TASK_INSPECTING_LINK_NAME = "任务开始核查";
    public static final String LEGACY_DEF_RESP_TASK_DISPATCH_LINK_NAME = "下发防御响应";
    public static final String REFINED_PATROL_TASK = DzTaskDistList.TASK_TYPE_INSPECTION;
    public static final String REFINED_AI_VERIFY_TASK = DzTaskDistList.TASK_TYPE_AI_VERIFY;
    public static final String REFINED_DEF_RESP_TASK = DzTaskDistList.TASK_TYPE_DEF_RESP;
    public static final String REFINED_SCENE_HANDLE_TASK = DzTaskDistList.TASK_TYPE_EMERGENCY;
    public static final String REFINED_MONITOR_WARNING_TASK = DzTaskDistList.TASK_TYPE_MONITOR_WARNING;
    public static final String REFINED_EMERGENCY_SURVEY_TASK = DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION;
    public static final String REFINED_PUBLIC_REPORT_TASK = DzTaskDistList.TASK_TYPE_PUBLIC_REPORT;
    public static final String REFINED_MANUAL_TASK = "手动添加任务";
    public static final String REFINED_AI_VERIFY_DISPATCH = AI_VERIFY_TASK_DISPATCH.getLinkName();
    public static final String REFINED_DEF_RESP_DISPATCH = "下发" + REFINED_DEF_RESP_TASK;
    public static final String REFINED_SCENE_HANDLE_DISPATCH = "下发" + REFINED_SCENE_HANDLE_TASK;
    public static final String REFINED_MONITOR_WARNING_DISPATCH = "下发" + REFINED_MONITOR_WARNING_TASK;
    public static final String REFINED_EMERGENCY_SURVEY_DISPATCH = TECH_ASSIST_TASK_DISPATCH.getLinkName();
    public static final String REFINED_PUBLIC_REPORT_DISPATCH = PUBLIC_REPORT_TASK_DISPATCH.getLinkName();
    private static final List<String> REFINED_TASK_TYPES = List.of(
        REFINED_PATROL_TASK,
        REFINED_AI_VERIFY_TASK,
        REFINED_DEF_RESP_TASK,
        REFINED_SCENE_HANDLE_TASK,
        REFINED_MONITOR_WARNING_TASK,
        REFINED_EMERGENCY_SURVEY_TASK,
        REFINED_PUBLIC_REPORT_TASK,
        REFINED_MANUAL_TASK
    );

    private final String linkName;
    private final String triggerReason;

    public static List<TaskProcessChainNodeTextEnum> listByLinkName(String linkName) {
        if (linkName == null || linkName.trim().isEmpty()) {
            return List.of();
        }
        String normalized = normalizeSemanticLinkName(linkName);
        return Arrays.stream(values())
            .filter(item -> normalizeSemanticLinkName(item.linkName).equals(normalized))
            .toList();
    }

    public static String normalizeDisplayLinkName(String linkName) {
        if (linkName == null) {
            return null;
        }
        String normalized = linkName.trim();
        if (LEGACY_TASK_INSPECTING_LINK_NAME.equals(normalized)) {
            return TASK_INSPECTING.getLinkName();
        }
        if (LEGACY_DEF_RESP_TASK_DISPATCH_LINK_NAME.equals(normalized)) {
            return DEF_RESP_TASK_DISPATCH.getLinkName();
        }
        return normalized;
    }

    public static String normalizeSemanticLinkName(String linkName) {
        String normalized = normalizeDisplayLinkName(linkName);
        if (normalized == null || normalized.isEmpty()) {
            return normalized;
        }
        if (isDailyPatrolTaskStatusLink(normalized, "推送")) {
            return TASK_PUSH_SYSTEM.getLinkName();
        }
        if (isDailyPatrolTaskStatusLink(normalized, "核查中")) {
            return TASK_INSPECTING.getLinkName();
        }
        if (isDailyPatrolTaskStatusLink(normalized, "执行中")) {
            return TASK_INSPECTING.getLinkName();
        }
        if (isDailyPatrolTaskStatusLink(normalized, "反馈")) {
            return TASK_FEEDBACK.getLinkName();
        }
        if (isDailyPatrolTaskStatusLink(normalized, "关闭")) {
            return TASK_CLOSE_DEFAULT.getLinkName();
        }
        if (isDailyPatrolTaskStatusLink(normalized, "过期")) {
            return TASK_OVERDUE_DEFAULT.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "推送")) {
            return TASK_PUSH_SYSTEM.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "核查中")) {
            return TASK_INSPECTING.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "执行中")) {
            return TASK_INSPECTING.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "反馈")) {
            return TASK_FEEDBACK.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "反馈报告")) {
            return TASK_FEEDBACK_REPORT.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "关闭")) {
            return TASK_CLOSE_DEFAULT.getLinkName();
        }
        if (isRefinedTaskStatusLink(normalized, "过期")) {
            return TASK_OVERDUE_DEFAULT.getLinkName();
        }
        if (matchesAny(normalized, "手动新增任务")) {
            return REFINED_MANUAL_TASK;
        }
        if (matchesAny(normalized, REFINED_PATROL_TASK, DAILY_PATROL_TASK.getLinkName(), GENERIC_TASK.getLinkName())) {
            return REFINED_PATROL_TASK;
        }
        if (matchesAny(normalized, REFINED_MANUAL_TASK, MANUAL_TASK_CREATE.getLinkName())) {
            return REFINED_MANUAL_TASK;
        }
        if (matchesAny(normalized, REFINED_AI_VERIFY_TASK, DANGER_VERIFY_TASK.getLinkName())) {
            return REFINED_AI_VERIFY_TASK;
        }
        if (matchesAny(normalized, REFINED_AI_VERIFY_DISPATCH, DANGER_VERIFY_TASK_DISPATCH.getLinkName())) {
            return REFINED_AI_VERIFY_DISPATCH;
        }
        if (matchesAny(normalized, REFINED_PUBLIC_REPORT_DISPATCH)) {
            return REFINED_PUBLIC_REPORT_DISPATCH;
        }
        if (matchesAny(normalized, "批量下发防御响应任务")) {
            return DEF_RESP_BATCH_DISPATCH.getLinkName();
        }
        if (matchesAny(normalized, REFINED_DEF_RESP_TASK, DEF_RESP_TASK.getLinkName())) {
            return REFINED_DEF_RESP_TASK;
        }
        if (matchesAny(normalized, REFINED_DEF_RESP_DISPATCH, DEF_RESP_TASK_DISPATCH.getLinkName(),
            DEF_RESP_TASK_GENERATE.getLinkName())) {
            return REFINED_DEF_RESP_DISPATCH;
        }
        if (matchesAny(normalized, REFINED_SCENE_HANDLE_TASK, EMERGENCY_TASK.getLinkName())) {
            return REFINED_SCENE_HANDLE_TASK;
        }
        if (matchesAny(normalized, REFINED_SCENE_HANDLE_DISPATCH, EMERGENCY_TASK_DISPATCH.getLinkName(),
            EMERGENCY_TASK_GENERATE.getLinkName())) {
            return REFINED_SCENE_HANDLE_DISPATCH;
        }
        if (matchesAny(normalized, "批量下发处置任务")) {
            return EMERGENCY_BATCH_DISPATCH.getLinkName();
        }
        if (matchesAny(normalized, REFINED_MONITOR_WARNING_TASK, MONITOR_WARNING.getLinkName(), "监测预警")) {
            return REFINED_MONITOR_WARNING_TASK;
        }
        if (matchesAny(normalized, REFINED_MONITOR_WARNING_DISPATCH, MONITOR_WARNING_TASK_DISPATCH.getLinkName())) {
            return REFINED_MONITOR_WARNING_DISPATCH;
        }
        if (matchesAny(normalized, REFINED_EMERGENCY_SURVEY_DISPATCH, TECH_ASSIST_TASK_DISPATCH.getLinkName(),
            TECH_ASSIST_TASK_FROM_FEEDBACK.getLinkName())) {
            return REFINED_EMERGENCY_SURVEY_DISPATCH;
        }
        if (matchesAny(normalized, DISASTER_REPORT_GENERATE.getLinkName(),
            TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName())) {
            return normalized;
        }
        return normalized;
    }

    public static boolean semanticEquals(String left, String right) {
        String normalizedLeft = normalizeSemanticLinkName(left);
        String normalizedRight = normalizeSemanticLinkName(right);
        if (normalizedLeft == null) {
            return normalizedRight == null;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    public static boolean isRootClosedLinkName(String linkName) {
        String displayName = normalizeDisplayLinkName(linkName);
        if (displayName == null || displayName.isEmpty()) {
            return false;
        }
        if (matchesAny(displayName,
            DISASTER_DANGER_CLOSED.getLinkName(),
            "AI险情核实任务反馈",
            REFINED_AI_VERIFY_TASK + "反馈",
            HANDLE_ARCHIVED.getLinkName(),
            DEF_RESP_ARCHIVED.getLinkName(),
            "现场处置任务反馈",
            REFINED_SCENE_HANDLE_TASK + "反馈")) {
            return true;
        }
        String semanticName = normalizeSemanticLinkName(displayName);
        return TASK_CLOSE_DEFAULT.getLinkName().equals(semanticName)
            || TASK_OVERDUE_DEFAULT.getLinkName().equals(semanticName);
    }

    public static String refineTaskLinkName(String linkName, Integer sourceType) {
        return refineTaskLinkName(linkName, sourceType, null);
    }

    public static String refineTaskLinkName(String linkName, Integer sourceType, String taskType) {
        String semantic = normalizeSemanticLinkName(linkName);
        if (semantic == null || semantic.isEmpty()) {
            return semantic;
        }
        String refinedTaskType = resolveRefinedTaskType(sourceType, taskType);
        if (TASK_PUSH_SYSTEM.getLinkName().equals(semantic)) {
            return refinedTaskType == null ? normalizeDisplayLinkName(linkName) : displayTaskType(refinedTaskType) + "推送";
        }
        if (TASK_INSPECTING.getLinkName().equals(semantic)) {
            return refinedTaskType == null ? normalizeDisplayLinkName(linkName) : displayTaskType(refinedTaskType) + "执行中";
        }
        if (TASK_FEEDBACK.getLinkName().equals(semantic)) {
            return refinedTaskType == null ? normalizeDisplayLinkName(linkName) : displayTaskType(refinedTaskType) + "反馈";
        }
        if (TASK_CLOSE_DEFAULT.getLinkName().equals(semantic)) {
            return refinedTaskType == null ? normalizeDisplayLinkName(linkName) : displayTaskType(refinedTaskType) + "关闭";
        }
        if (TASK_OVERDUE_DEFAULT.getLinkName().equals(semantic)) {
            return refinedTaskType == null ? normalizeDisplayLinkName(linkName) : displayTaskType(refinedTaskType) + "过期";
        }
        if (REFINED_PATROL_TASK.equals(semantic)) {
            return REFINED_PATROL_TASK;
        }
        if (REFINED_AI_VERIFY_TASK.equals(semantic)) {
            return REFINED_AI_VERIFY_TASK;
        }
        if (REFINED_AI_VERIFY_DISPATCH.equals(semantic)) {
            return REFINED_AI_VERIFY_DISPATCH;
        }
        if (REFINED_DEF_RESP_TASK.equals(semantic)) {
            return REFINED_DEF_RESP_TASK;
        }
        if (REFINED_DEF_RESP_DISPATCH.equals(semantic)) {
            return REFINED_DEF_RESP_DISPATCH;
        }
        if (REFINED_SCENE_HANDLE_TASK.equals(semantic)) {
            return REFINED_SCENE_HANDLE_TASK;
        }
        if (REFINED_SCENE_HANDLE_DISPATCH.equals(semantic)) {
            return REFINED_SCENE_HANDLE_DISPATCH;
        }
        if (REFINED_MONITOR_WARNING_TASK.equals(semantic)) {
            return REFINED_MONITOR_WARNING_TASK;
        }
        if (REFINED_MONITOR_WARNING_DISPATCH.equals(semantic)) {
            return REFINED_MONITOR_WARNING_DISPATCH;
        }
        if (REFINED_EMERGENCY_SURVEY_DISPATCH.equals(semantic)) {
            return REFINED_EMERGENCY_SURVEY_DISPATCH;
        }
        if (REFINED_PUBLIC_REPORT_DISPATCH.equals(semantic)) {
            return REFINED_PUBLIC_REPORT_DISPATCH;
        }
        return normalizeDisplayLinkName(linkName);
    }

    public static String refineTaskFeedbackReportLinkName(Integer sourceType, String taskType) {
        String refinedTaskType = resolveRefinedTaskType(sourceType, taskType);
        return refinedTaskType == null ? TASK_FEEDBACK_REPORT.getLinkName() : displayTaskType(refinedTaskType) + "反馈报告";
    }

    private static String resolveRefinedTaskType(Integer sourceType, String taskType) {
        String normalizedTaskType = taskType == null ? null : taskType.trim();
        if (normalizedTaskType != null && REFINED_TASK_TYPES.contains(normalizedTaskType)) {
            return normalizedTaskType;
        }
        return resolveRefinedTaskTypeBySourceType(sourceType);
    }

    private static String displayTaskType(String taskType) {
        if (REFINED_MANUAL_TASK.equals(taskType)) {
            return REFINED_MANUAL_TASK;
        }
        if (REFINED_PATROL_TASK.equals(taskType)) {
            return DAILY_PATROL_TASK.getLinkName();
        }
        if (REFINED_AI_VERIFY_TASK.equals(taskType)
            || REFINED_DEF_RESP_TASK.equals(taskType)
            || REFINED_SCENE_HANDLE_TASK.equals(taskType)
            || REFINED_MONITOR_WARNING_TASK.equals(taskType)
            || REFINED_PUBLIC_REPORT_TASK.equals(taskType)) {
            return taskType + "任务";
        }
        if (REFINED_EMERGENCY_SURVEY_TASK.equals(taskType)) {
            return "技术协查任务";
        }
        if (taskType.endsWith("任务")) {
            return taskType;
        }
        return taskType + "任务";
    }

    private static boolean isRefinedTaskStatusLink(String linkName, String suffix) {
        for (String taskType : REFINED_TASK_TYPES) {
            if ((taskType + suffix).equals(linkName) || (displayTaskType(taskType) + suffix).equals(linkName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDailyPatrolTaskStatusLink(String linkName, String suffix) {
        return (DAILY_PATROL_TASK.getLinkName() + suffix).equals(linkName);
    }

    private static boolean matchesAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveRefinedTaskTypeBySourceType(Integer sourceType) {
        TaskProcessSourceTypeEnum sourceTypeEnum = TaskProcessSourceTypeEnum.of(sourceType);
        return switch (sourceTypeEnum) {
            case REPORT -> REFINED_AI_VERIFY_TASK;
            case DEF_RESP -> REFINED_DEF_RESP_TASK;
            case EMERGENCY -> REFINED_SCENE_HANDLE_TASK;
            case MONITOR_WARNING -> REFINED_MONITOR_WARNING_TASK;
            case TECH_ASSISTANCE -> REFINED_EMERGENCY_SURVEY_TASK;
            case MANUAL -> REFINED_MANUAL_TASK;
            case EVAL -> REFINED_PATROL_TASK;
            default -> null;
        };
    }
}
