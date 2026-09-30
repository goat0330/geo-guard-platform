package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报灾人工反馈业务对象
 *
 * @author kongweiguang
 * @date 2026-05-14
 */
@Data
public class DzReportDisasterFeedbackBo {

    /**
     * 报灾记录id
     */
    @NotNull(message = "报灾记录id不能为空")
    private Long id;

    /**
     * 人工修正后的风险等级（1：低 2：中 3：高 4：极高）
     */
    @NotNull(message = "人工修正风险等级不能为空")
    private Integer manualRiskLevel;

    /**
     * 人工反馈备注
     */
    private String manualRiskRemark;
}
