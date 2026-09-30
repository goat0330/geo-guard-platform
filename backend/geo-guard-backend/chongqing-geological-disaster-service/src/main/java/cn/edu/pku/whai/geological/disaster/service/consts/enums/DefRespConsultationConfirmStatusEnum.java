/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 防御响应会商确认记录状态。
 */
@Getter
@AllArgsConstructor
public enum DefRespConsultationConfirmStatusEnum {

    PENDING_SUBMIT(0, "待提交"),
    PENDING_APPROVAL(1, "已提交"),
    APPROVED(2, "通过"),
    ALARM_SYNCED(3, "预警同步");

    private final Integer code;
    private final String name;
}
