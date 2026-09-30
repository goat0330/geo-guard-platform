/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务流程链路阶段分类。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessStageTypeEnum {

    DEF_RESP(0, "防御响应"),
    START_TASK(1, "起始任务"),
    DANGER_VERIFY(2, "险情核实"),
    EMERGENCY_SURVEY(3, "应急调查"),
    HANDLE(4, "处置管理"),
    SINGLE_DEF_RESP(5, "单点防御"),
    ARCHIVE(6, "复盘归档");

    private final Integer code;
    private final String label;

    public static TaskProcessStageTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskProcessStageTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static String label(Integer code) {
        TaskProcessStageTypeEnum item = of(code);
        return item == null ? null : item.label;
    }

    public static TaskProcessStageTypeEnum fromTask(String linkName, DzTaskDistList task) {
        TaskProcessStageTypeEnum byLink = fromLinkName(linkName);
        if (byLink != null) {
            return byLink;
        }
        return task == null ? null : fromTaskSourceType(task.getSourceType());
    }

    public static TaskProcessStageTypeEnum fromNode(String linkName, Integer bizType, Integer sourceType) {
        TaskProcessStageTypeEnum byLink = fromLinkName(linkName);
        if (byLink != null) {
            return byLink;
        }
        if (TaskProcessBizTypeEnum.TASK.getCode().equals(bizType)) {
            return fromChainSourceType(sourceType);
        }
        if (TaskProcessBizTypeEnum.ALARM.getCode().equals(bizType)
            || TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(bizType)) {
            return DEF_RESP;
        }
        if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(bizType)) {
            return HANDLE;
        }
        if (TaskProcessBizTypeEnum.MONITOR_WARNING.getCode().equals(bizType)) {
            return START_TASK;
        }
        return fromChainSourceType(sourceType);
    }

    public static TaskProcessStageTypeEnum fromLinkName(String linkName) {
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(linkName);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED)) {
            return DEF_RESP;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.PUBLIC_REPORT,
            TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK,
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE,
            TaskProcessChainNodeTextEnum.PUBLIC_REPORT_TASK_DISPATCH,
            TaskProcessChainNodeTextEnum.DEF_RESP_TASK,
            TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH,
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH,
            TaskProcessChainNodeTextEnum.MONITOR_WARNING,
            TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH)) {
            return START_TASK;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT,
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE,
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT,
            TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK,
            TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK_DISPATCH,
            TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH,
            TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK)) {
            return DANGER_VERIFY;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY,
            TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH)) {
            return EMERGENCY_SURVEY;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT,
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD,
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD,
            TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD,
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD)) {
            return HANDLE;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH,
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH,
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK)) {
            return SINGLE_DEF_RESP;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED,
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED)) {
            return ARCHIVE;
        }
        if (isLinkName(normalized, TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED)) {
            return ARCHIVE;
        }
        return null;
    }

    private static boolean isLinkName(String normalized, TaskProcessChainNodeTextEnum... items) {
        for (TaskProcessChainNodeTextEnum item : items) {
            if (TaskProcessChainNodeTextEnum.semanticEquals(item.getLinkName(), normalized)) {
                return true;
            }
        }
        return false;
    }

    public static TaskProcessStageTypeEnum fromTaskSourceType(Integer sourceType) {
        if (sourceType == null) {
            return null;
        }
        if (DzTaskDistList.SOURCE_TYPE_REPORT.equals(sourceType)) {
            return DANGER_VERIFY;
        }
        if (DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE.equals(sourceType)) {
            return EMERGENCY_SURVEY;
        }
        if (DzTaskDistList.SOURCE_TYPE_EMERGENCY.equals(sourceType)) {
            return SINGLE_DEF_RESP;
        }
        if (DzTaskDistList.SOURCE_TYPE_MANUAL.equals(sourceType)
            || DzTaskDistList.SOURCE_TYPE_EVAL.equals(sourceType)
            || DzTaskDistList.SOURCE_TYPE_DEF_RESP.equals(sourceType)
            || DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING.equals(sourceType)) {
            return START_TASK;
        }
        return null;
    }

    public static TaskProcessStageTypeEnum fromChainSourceType(Integer sourceType) {
        TaskProcessSourceTypeEnum chainSourceType = TaskProcessSourceTypeEnum.of(sourceType);
        return switch (chainSourceType) {
            case TASK_FEEDBACK, REPORT -> DANGER_VERIFY;
            case MANUAL, EVAL, DEF_RESP, MONITOR_WARNING -> START_TASK;
            case TECH_ASSISTANCE -> EMERGENCY_SURVEY;
            case HANDLE -> HANDLE;
            case EMERGENCY -> SINGLE_DEF_RESP;
            case ALARM -> DEF_RESP;
            default -> null;
        };
    }
}
