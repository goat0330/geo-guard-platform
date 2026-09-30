/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务流程链路来源类型。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessSourceTypeEnum {

    UNKNOWN(0, "未知"),
    TASK_FEEDBACK(1, "任务反馈"),
    REPORT(2, "群众报灾"),
    ALARM(3, "预警信息"),
    MONITOR_WARNING(4, "监测预警"),
    EVAL(5, "系统评估"),
    MANUAL(6, "手动添加"),
    TECH_ASSISTANCE(7, "技术协查"),
    HANDLE(8, "处置管理"),
    DEF_RESP(9, "防御响应"),
    EMERGENCY(10, "应急处置");

    private final Integer code;
    private final String label;

    public static TaskProcessSourceTypeEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (TaskProcessSourceTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static TaskProcessSourceTypeEnum fromTaskSourceType(Integer sourceType) {
        if (sourceType == null) {
            return UNKNOWN;
        }
        if (DzTaskDistList.SOURCE_TYPE_MANUAL.equals(sourceType)) {
            return MANUAL;
        }
        if (DzTaskDistList.SOURCE_TYPE_EVAL.equals(sourceType)) {
            return EVAL;
        }
        if (DzTaskDistList.SOURCE_TYPE_REPORT.equals(sourceType)) {
            return REPORT;
        }
        if (DzTaskDistList.SOURCE_TYPE_DEF_RESP.equals(sourceType)) {
            return DEF_RESP;
        }
        if (DzTaskDistList.SOURCE_TYPE_EMERGENCY.equals(sourceType)) {
            return EMERGENCY;
        }
        if (DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING.equals(sourceType)) {
            return MONITOR_WARNING;
        }
        if (DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE.equals(sourceType)) {
            return TECH_ASSISTANCE;
        }
        return UNKNOWN;
    }

    public static TaskProcessSourceTypeEnum fromReportSourceType(Integer sourceType) {
        if (Integer.valueOf(1).equals(sourceType)) {
            return TASK_FEEDBACK;
        }
        if (Integer.valueOf(2).equals(sourceType)) {
            return REPORT;
        }
        return UNKNOWN;
    }

    public static TaskProcessSourceTypeEnum fromBizType(Integer bizType) {
        TaskProcessBizTypeEnum type = TaskProcessBizTypeEnum.of(bizType);
        return switch (type) {
            case DEF_RESP -> DEF_RESP;
            case ALARM -> ALARM;
            case HANDLE -> HANDLE;
            case MONITOR_WARNING -> MONITOR_WARNING;
            default -> UNKNOWN;
        };
    }

    public static TaskProcessSourceTypeEnum fromLegacy(String value, Integer bizType) {
        if (value == null || value.trim().isEmpty()) {
            return fromBizType(bizType);
        }
        String normalized = value.trim();
        if (TaskProcessBizTypeEnum.REPORT.getCode().equals(bizType)) {
            if ("1".equals(normalized)) {
                return TASK_FEEDBACK;
            }
            if ("2".equals(normalized)) {
                return REPORT;
            }
        }
        return switch (normalized) {
            case "0", "未知" -> UNKNOWN;
            case "1", "任务反馈" -> TASK_FEEDBACK;
            case "2", "群众报灾", "群众上报", "灾险情报告", "REPORT" -> REPORT;
            case "3", "预警信息", "ALARM" -> ALARM;
            case "4", "监测预警", "MONITOR_WARNING" -> MONITOR_WARNING;
            case "5", "系统评估" -> EVAL;
            case "6", "手动添加", "任务", "TASK" -> MANUAL;
            case "7", "技术协查" -> TECH_ASSISTANCE;
            case "8", "处置管理", "HANDLE" -> HANDLE;
            case "9", "防御响应", "DEF_RESP" -> DEF_RESP;
            case "10", "应急处置" -> EMERGENCY;
            default -> fromBizType(bizType);
        };
    }

    public static String label(Integer code) {
        return of(code).label;
    }
}
