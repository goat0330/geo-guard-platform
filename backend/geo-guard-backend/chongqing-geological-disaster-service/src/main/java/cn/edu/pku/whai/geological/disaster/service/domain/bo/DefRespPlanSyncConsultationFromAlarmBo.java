/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按指定气象预警同步会商确认内容入参。
 */
@Data
public class DefRespPlanSyncConsultationFromAlarmBo {

    /**
     * 县级区域防御响应方案 id。
     */
    @NotNull(message = "defId不能为空")
    private Long defId;

    /**
     * 气象预警 id。
     */
    @NotNull(message = "alarmId不能为空")
    private Long alarmId;
}
