/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpertJudgeBo {
    /**
     * 会议id
     */
    @NotNull(message = "会议id不能为空")
    private Long meetingId;
    /**
     * 专家id
     */
    @NotNull(message = "专家id不能为空")
    private Long expertId;
    /**
     * 0:未确认 1:确认
     */
    @NotNull(message = "确认状态不能为空")
    private Integer judgeStatus;
}
