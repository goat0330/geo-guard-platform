/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模拟预警同步导致斜坡单元风险等级升高结果。
 */
@Data
public class ThirdPartyWarningRiskIncreaseSimulateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long riskAssessmentId;

    private String slopeUnitId;

    private Integer originalDynamicRiskLevel;

    private Integer updatedDynamicRiskLevel;

    private Boolean triggeredMonitorWarningTask;
}
