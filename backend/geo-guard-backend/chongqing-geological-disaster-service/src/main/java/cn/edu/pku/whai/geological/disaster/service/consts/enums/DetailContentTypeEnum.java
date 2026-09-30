/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处置详情内容类型。
 */
@Getter
@AllArgsConstructor
public enum DetailContentTypeEnum {

    /**
     * 应急调查报告。
     */
    AI_REPORT(1, "应急调查报告"),

    /**
     * 撤离方案。
     */
    EVACUATION_PLAN(2, "撤离方案"),

    /**
     * 防御响应方案。
     */
    DEF_RESP_PLAN(3, "防御响应方案"),

    /**
     * 初始报告。
     */
    INITIAL_REPORT(4, "初始报告"),

    /**
     * 最终报告。
     */
    FINAL_REPORT(5, "最终报告"),

    /**
     * 复盘报告。
     */
    REVIEW_REPORT(6, "复盘报告"),

    /**
     * 会商确认。
     */
    CONSULTATION_CONFIRM(7, "会商确认");

    private final Integer code;

    private final String desc;
}
