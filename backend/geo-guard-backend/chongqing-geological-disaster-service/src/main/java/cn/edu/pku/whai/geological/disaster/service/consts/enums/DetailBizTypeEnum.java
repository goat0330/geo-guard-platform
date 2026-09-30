/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处置详情内容业务类型。
 */
@Getter
@AllArgsConstructor
public enum DetailBizTypeEnum {

    /**
     * 应急处置任务。
     */
    TASK_HANDLE(1, "dz_task_handle"),

    /**
     * 防御响应方案。
     */
    DEF_RESP_PLAN(2, "dz_def_resp_plan");

    private final Integer code;

    private final String desc;
}
