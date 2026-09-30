/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程链路列表展示分段类型。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessChainSegmentTypeEnum {

    UNKNOWN(0, "未知", false),
    SOURCE_PRE_CHAIN(1, "来源前置链", false),
    MAIN_CHAIN(2, "普通业务主链", true),
    DEF_RESP_TASK_MAIN_CHAIN(3, "防御响应任务主链", true),
    HANDLE_MAIN_CHAIN(4, "处置管理主链", true),
    EMERGENCY_TASK_CHILD_CHAIN(5, "应急处置任务子链", false),
    OTHER_DERIVED_CHILD_CHAIN(9, "其他派生子链", false);

    private final Integer code;
    private final String label;
    private final boolean displayable;

    public static TaskProcessChainSegmentTypeEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (TaskProcessChainSegmentTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static boolean displayable(Integer code) {
        return of(code).displayable;
    }

    public static String label(Integer code) {
        return of(code).label;
    }
}
