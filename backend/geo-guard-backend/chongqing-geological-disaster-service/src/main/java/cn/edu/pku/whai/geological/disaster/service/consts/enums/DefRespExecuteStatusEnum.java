/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 区域防御响应执行状态（县/乡镇行可用）。
 */
@Getter
@AllArgsConstructor
public enum DefRespExecuteStatusEnum {

    NOT_STARTED(0, "未执行"),
    RUNNING(1, "执行中"),
    CLOSED(2, "已关闭"),
    ENDED(3, "已结束");

    private final Integer code;
    private final String name;

    public static DefRespExecuteStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DefRespExecuteStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
