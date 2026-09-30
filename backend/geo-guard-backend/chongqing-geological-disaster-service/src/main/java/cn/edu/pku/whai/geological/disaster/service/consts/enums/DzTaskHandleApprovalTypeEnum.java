/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务处置审批类型枚举
 * 对应DzTaskHandleApproval.type字段
 *
 * @author whai
 */
@Getter
@AllArgsConstructor
public enum DzTaskHandleApprovalTypeEnum {

    EXPERT_CONSULTATION(1, "专家会商"),
    ADMIN_APPROVAL(2, "行政审批");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static DzTaskHandleApprovalTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DzTaskHandleApprovalTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
