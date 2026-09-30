/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 防御响应审批状态。
 */
@Getter
@AllArgsConstructor
public enum DefRespApprovalStatusEnum {

    NONE(0, "无"),
    PENDING_APPROVAL(1, "待审批");

    private final Integer code;
    private final String name;

    public static DefRespApprovalStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DefRespApprovalStatusEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
