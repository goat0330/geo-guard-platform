/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地灾风险预测推送视图对象 dz_risk_prediction_push
 * PO->VO 转换由 BaseMapperPlus 提供，无需 @AutoMapper 避免重复生成导致 Bean 冲突
 *
 * @author system
 * @date 2026-03-03
 */
@Data
@AutoMapper(target = DzRiskPredictionPush.class)
@ExcelIgnoreUnannotated
public class DzRiskPredictionPushVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 风险预测的批次id
     */
    @ExcelProperty(value = "风险预测批次id")
    private Long predictionBatchId;

    /**
     * 报文名称
     */
    @ExcelProperty(value = "报文名称")
    private String title;

    /**
     * 报文内容
     */
    @ExcelProperty(value = "报文内容")
    private String content;

    /**
     * 预测类型（1：日 2：周 3：月 4：季 5：年）
     */
    @ExcelProperty(value = "预测类型")
    private Integer type;

    /**
     * 推送状态（0：未推送 1：已推送）
     */
    @ExcelProperty(value = "推送状态")
    private Integer status;

    /**
     * 推送人员
     */
    @ExcelProperty(value = "推送人员")
    private String person;

    /**
     * 推送人数
     */
    @ExcelProperty(value = "推送人数")
    private Integer personCount;

    /**
     * 文件 id（ossId）
     */
    @ExcelProperty(value = "文件 id")
    private Long docId;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;
}
