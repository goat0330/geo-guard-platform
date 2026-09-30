/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成撤离方案请求
 *
 * @author whai
 */
@Data
public class GenerateEvacuationPlanReq {

    /**
     * 处置主键，支持handle_id或handleId
     */
    @NotNull(message = "handleId不能为空")
    @JsonAlias("handle_id")
    private Long handleId;

    /**
     * 现有方案文本（可选；未传时使用当前轮次最新撤离方案）
     */
    private String schemes;

    /**
     * 专家意见（可选）
     */
    @JsonAlias({"expert_opinion"})
    private String expertOpinion;
}
