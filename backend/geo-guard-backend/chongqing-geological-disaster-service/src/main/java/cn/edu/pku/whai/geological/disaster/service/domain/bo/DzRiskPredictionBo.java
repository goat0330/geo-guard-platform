package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPrediction;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 风险预测业务对象 dz_risk_prediction
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzRiskPrediction.class, reverseConvertGenerate = false)
public class DzRiskPredictionBo extends BaseEntity {

    /**
     * id
     */
    private Long id;

    /**
     * 批次号
     */
    private Long batchId;

    /**
     * 斜坡单元id
     */
    private String slopeUnitId;

    /**
     * 预测分数
     */
    private BigDecimal predictionScoreW;

    /**
     * 风险性级别(0：无风险，1：极低风险 2：低风险 3：中风险 :4：高风险 5：极高风险)
     */
    private Integer riskLevel;

    /**
     * 风险性级别(0：无风险，1：极低风险 2：低风险 3：中风险 :4：高风险 5：极高风险)
     */
    private List<Integer> riskLevelList;

    /**
     * prediction date
     */
    private Date predictionDate;

    /**
     * prediction date str
     */
    private String predictionDateStr;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 预测类型（1：日 2：周 3：月 4：季 5：年）
     */
    private Integer type;

    /**
     * 是否查最新的 0 否 1是
     */
    private Integer latest;


    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属地级市
     */
    private String city;

    /**
     * 所属区/县
     */
    private String county;

    /**
     * 所属乡镇/街道
     */
    private String street;

    /**
     * 所属行政村
     */
    private String village;

}
