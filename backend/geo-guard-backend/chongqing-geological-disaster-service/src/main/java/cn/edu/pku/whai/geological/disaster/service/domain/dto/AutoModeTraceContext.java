/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.dto;

import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 自动模式执行留痕上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoModeTraceContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 动作类型：1处置自动推进 2未推送任务自动推送 3日常巡逻自动推送 4自动催办 5报灾自动处理 6报灾任务推送 7风险预测推送 8日常巡逻生成
     */
    private Integer actionType;

    /**
     * 动作展示名称。
     */
    private String actionName;

    /**
     * 业务类型编码。
     */
    private Integer bizType;

    /**
     * 业务主键。
     */
    private Long bizId;

    /**
     * 关联任务派发 ID。
     */
    private Long taskId;

    /**
     * 是否批量动作。
     */
    private Boolean batchFlag;

    /**
     * 本次成功执行数量。
     */
    private Integer executeCount;

    /**
     * 执行补充信息。
     */
    private Map<String, Object> detailJson;

    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = AutoModeExecutionActions.resolveCode(actionType);
    }
}
