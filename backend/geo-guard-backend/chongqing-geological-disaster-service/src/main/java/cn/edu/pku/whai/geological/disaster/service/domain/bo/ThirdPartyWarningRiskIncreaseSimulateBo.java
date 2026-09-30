/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模拟预警同步导致斜坡单元风险等级升高请求。
 */
@Data
public class ThirdPartyWarningRiskIncreaseSimulateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元 id。
     */
    @NotBlank(message = "斜坡单元id不能为空")
    private String slopeUnitId;

    /**
     * 提高级数。
     */
    @NotNull(message = "提高等级不能为空")
    @Min(value = 1, message = "提高等级必须大于0")
    private Integer increaseLevel;
}
