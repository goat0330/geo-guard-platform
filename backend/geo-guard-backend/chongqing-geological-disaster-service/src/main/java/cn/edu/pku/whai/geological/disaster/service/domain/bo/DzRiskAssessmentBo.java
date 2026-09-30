/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 斜坡单元风险评估业务对象 dz_risk_assessment
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzRiskAssessment.class, reverseConvertGenerate = false)
public class DzRiskAssessmentBo extends BaseEntity {

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
    private Integer temDynamicRiskLevel;
    private List<Integer> dynamicRiskLevels;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 动态风险值
     */
    private BigDecimal dynamicRiskValue;

    /**
     * 是否补充斜坡单元数据
     */
    private Boolean withSlopeUnit;

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
