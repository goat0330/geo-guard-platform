/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量调整乡镇级区域防御响应关联关系入参。
 */
@Data
public class DefRespPlanBatchRelateBo {
    /**
     * 需要重新关联的乡镇名称列表。
     */
    @NotEmpty(message = "streets不能为空")
    private List<String> streets;

    /**
     * 目标县级区域防御响应等级。
     */
    @NotNull(message = "newLevel不能为空")
    private Integer newLevel;

    /**
     * 关联气象预警 id（来自会商确认 planContentJson）。
     */
    private Long alarmId;
}
