/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 斜坡单元风险评估对象 dz_risk_assessment
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@TableName("dz_risk_assessment")
public class DzRiskAssessment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 批次id
     */
    private Long batchId;

    /**
     * 斜坡单元id（data_slope_unit 表的id）
     */
    private String slopeUnitId;

    /**
     * 易发性
     */
    private BigDecimal susceptibility;

    /**
     * 危险性
     */
    private BigDecimal hazard;

    /**
     * 易损性
     */
    private BigDecimal vulnerability;

    /**
     * 风险性
     */
    private BigDecimal risk;

    /**
     * 动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer dynamicRiskLevel;

    /**
     * 临时动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer temDynamicRiskLevel;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 动态风险值
     */
    private BigDecimal dynamicRiskValue;

    /**
     * 易发性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer susceptibilityLevel;

    /**
     * 危险性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer hazardLevel;

    /**
     * 易损性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer vulnerabilityLevel;

    /**
     * 当前斜坡风险级别，正式动态风险等级的冗余/兼容字段(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer riskLevel;
    /**
     * 1.易发性值高-自身易发
     * 2.危险性高-降雨超阈值
     * 3.易损性高-承灾体密度大
     * 4.动态风险等级高（出现了灾险情）-出现灾险情
     */
    private String dynamicRiskSuggest;

}
