package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPrediction;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 风险预测视图对象 dz_risk_prediction
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzRiskPrediction.class)
public class DzRiskPredictionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 批次�?
     */
    @ExcelProperty(value = "批次号")
    private Long batchId;

    /**
     * 斜坡单元id
     */
    @ExcelProperty(value = "斜坡单元id")
    private String slopeUnitId;

    /**
     * 预测分数
     */
    @ExcelProperty(value = "预测分数")
    private BigDecimal predictionScoreW;

    /**
     * 风险性级别(0：无风险，1：极低风险 2：低风险 3：中风险 :4：高风险 5：极高风险)
     */
    @ExcelProperty(value = "风险性级别(0：无风险，1：极低风险 2：低风险 3：中风险 :4：高风险 5：极高风险)")
    private Integer riskLevel;

    /**
     * prediction date
     */
    @ExcelProperty(value = "预测日期")
    private Date predictionDate;

    /**
     * prediction date str
     */
    private String predictionDateStr;
    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 预测类型�?：日 2：周 3：月 4：季 5：年�?
     */
    @ExcelProperty(value = "预测类型（1：日 2：周 3：月 4：季 5：年）")
    private Integer type;

    /**
     * Slope unit detail.
     */
    private SlopeUnitVo slopeUnit;


}


