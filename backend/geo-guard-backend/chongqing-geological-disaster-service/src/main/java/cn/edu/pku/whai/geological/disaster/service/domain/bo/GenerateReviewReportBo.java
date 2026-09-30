/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成复盘报告入参。
 */
@Data
public class GenerateReviewReportBo {

    /**
     * 处置任务ID。
     */
    @NotNull(message = "handleId不能为空")
    private Long handleId;

    /**
     * 生成提示词。
     */
    private String prompt = "生成复盘报告";
}
