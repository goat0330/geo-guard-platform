/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 斜坡单元风险评估视图对象 dz_risk_assessment
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzRiskAssessment.class)
public class DzRiskAssessmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 批次id
     */
    @ExcelProperty(value = "批次id")
    private Long batchId;

    /**
     * 斜坡单元id（data_slope_unit 表的id）
     */
    @ExcelProperty(value = "斜坡单元id", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "d=ata_slope_unit,表=的id")
    private String slopeUnitId;

    /**
     * 易发性
     */
    @ExcelProperty(value = "易发性")
    private BigDecimal susceptibility;

    /**
     * 危险性
     */
    @ExcelProperty(value = "危险性")
    private BigDecimal hazard;

    /**
     * 易损性
     */
    @ExcelProperty(value = "易损性")
    private BigDecimal vulnerability;

    /**
     * 风险性
     */
    @ExcelProperty(value = "风险性")
    private BigDecimal risk;

    /**
     * 动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer dynamicRiskLevel;

    /**
     * 临时动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "临时动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer temDynamicRiskLevel;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 动态风险值
     */
    @ExcelProperty(value = "动态风险值")
    private BigDecimal dynamicRiskValue;

    /**
     * 易发性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "易发性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer susceptibilityLevel;

    /**
     * 危险性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "危险性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer hazardLevel;

    /**
     * 易损性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "易损性级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer vulnerabilityLevel;

    /**
     * 当前斜坡风险级别，正式动态风险等级的冗余/兼容字段(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @ExcelProperty(value = "当前斜坡风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)")
    private Integer riskLevel;

    /**
     * 斜坡单元信息
     */
    private SlopeUnitVo slopeUnit;

    private String dynamicRiskSuggest;

}
