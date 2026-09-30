package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 风险预测对象 dz_risk_prediction
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
@Data
@TableName("dz_risk_prediction")
public class DzRiskPrediction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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


}
