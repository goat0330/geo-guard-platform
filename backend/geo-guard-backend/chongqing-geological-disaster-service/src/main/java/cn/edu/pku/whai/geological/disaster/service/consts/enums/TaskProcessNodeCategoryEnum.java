/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务流程链路节点类别。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessNodeCategoryEnum {

    TASK_INSPECTING(1, TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName()),
    TASK_FEEDBACK(2, TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName()),
    TECH_ASSIST_APPLY(3, TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName()),
    HANDLE_START(4, TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
    SINGLE_DEF_RESP_START(5, TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName()),
    REGION_DEF_RESP_START(6, TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName()),
    EMERGENCY_BATCH_DISPATCH(7, TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName()),
    TASK_CLOSE(8, TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName()),
    TASK_OVERDUE(9, TaskProcessChainNodeTextEnum.TASK_OVERDUE_DEFAULT.getLinkName()),
    DEF_RESP_BATCH_DISPATCH(10, TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName()),
    PUBLIC_REPORT_REPORT(11, TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName()),
    TASK_FEEDBACK_REPORT(12, TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName()),
    HANDLE_ARCHIVED(13, TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName()),
    TASK_GENERATE(14, "任务生成"),
    TASK_PUSH(15, TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()),
    DISASTER_DANGER_CLOSED(16, TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName()),
    HANDLE_AI_REPORT_GENERATE(17, TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName());


    private final Integer code;
    private final String label;

    public static TaskProcessNodeCategoryEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskProcessNodeCategoryEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static TaskProcessNodeCategoryEnum fromLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return null;
        }
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(label);
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(), normalized)) {
            return DEF_RESP_BATCH_DISPATCH;
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(), normalized)) {
            return EMERGENCY_BATCH_DISPATCH;
        }
        if (isTaskGenerateLink(normalized)) {
            return TASK_GENERATE;
        }
        if (isTaskPushLink(normalized)) {
            return TASK_PUSH;
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(), normalized)) {
            return DEF_RESP_BATCH_DISPATCH;
        }
        for (TaskProcessNodeCategoryEnum item : values()) {
            if (TaskProcessChainNodeTextEnum.semanticEquals(item.label, normalized)) {
                return item;
            }
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(), normalized)) {
            return REGION_DEF_RESP_START;
        }
        return null;
    }

    private static boolean isTaskGenerateLink(String normalized) {
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.PUBLIC_REPORT.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_TASK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.MONITOR_WARNING.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.GENERIC_TASK.getLinkName(), normalized);
    }

    private static boolean isTaskPushLink(String normalized) {
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH.getLinkName(), normalized)
            || normalized.startsWith("下发") && normalized.endsWith("任务");
    }

    public static Integer codeFromLabel(String label) {
        TaskProcessNodeCategoryEnum item = fromLabel(label);
        return item == null ? null : item.code;
    }

    public static String label(Integer code) {
        TaskProcessNodeCategoryEnum item = of(code);
        return item == null ? null : item.label;
    }
}
