/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务流程链路业务类型。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessBizTypeEnum {

    UNKNOWN(0, "未知", "UNKNOWN"),
    TASK(1, "任务", "TASK"),
    REPORT(2, "报灾/报告", "REPORT"),
    HANDLE(3, "处置管理", "HANDLE"),
    DEF_RESP(4, "防御响应", "DEF_RESP"),
    ALARM(5, "预警信息", "ALARM"),
    MONITOR_WARNING(6, "监测预警", "MONITOR_WARNING");

    private final Integer code;
    private final String label;
    private final String legacyCode;

    public static TaskProcessBizTypeEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (TaskProcessBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static TaskProcessBizTypeEnum fromLegacy(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        String normalized = value.trim();
        try {
            return of(Integer.valueOf(normalized));
        } catch (NumberFormatException ignored) {
        }
        for (TaskProcessBizTypeEnum item : values()) {
            if (item.legacyCode.equalsIgnoreCase(normalized)
                || item.label.equals(normalized)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static String legacyCode(Integer code) {
        return of(code).legacyCode;
    }

    public static String label(Integer code) {
        return of(code).label;
    }
}
