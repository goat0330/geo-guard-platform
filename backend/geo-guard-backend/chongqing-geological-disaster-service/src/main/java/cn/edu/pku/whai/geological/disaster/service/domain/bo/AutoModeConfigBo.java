/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 自动模式配置入参。
 */
@Data
public class AutoModeConfigBo {

    /**
     * 每一步之间的间隔秒数。
     */
    @Min(value = 1, message = "步骤间隔必须大于0秒")
    private Integer stepIntervalSeconds;

    /**
     * 自动执行人名称。
     */
    private String actorName;

    /**
     * 自动执行人用户ID。
     */
    private Long actorUserId;
}
