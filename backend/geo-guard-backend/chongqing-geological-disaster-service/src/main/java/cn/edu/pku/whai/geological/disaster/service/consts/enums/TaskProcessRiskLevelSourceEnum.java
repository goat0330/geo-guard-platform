/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程链路列表风险等级来源。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessRiskLevelSourceEnum {

    UNKNOWN(0, "未知"),
    RISK_ASSESSMENT(1, "风险评估"),
    REPORT_MANUAL(2, "报灾人工修正"),
    REPORT_AI(3, "报灾AI识别"),
    HANDLE_EVENT_LEVEL(4, "处置事件等级"),
    DEF_RESP_LEVEL(5, "防御响应等级"),
    MONITOR_WARNING_LEVEL(6, "监测预警等级"),
    ALARM_LEVEL(7, "预警报告等级");

    private final Integer code;
    private final String label;

    public static TaskProcessRiskLevelSourceEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (TaskProcessRiskLevelSourceEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static String label(Integer code) {
        return of(code).label;
    }
}
