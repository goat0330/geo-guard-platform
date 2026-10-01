/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.WarningDisposalRecordMonitor;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 预警处置记录监测视图对象 v_warning_disposal_record_monitor
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WarningDisposalRecordMonitor.class)
public class WarningDisposalRecordMonitorVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预警处置记录唯一主键ID（业务唯一标识）
     */
    @ExcelProperty(value = "预警处置记录唯一主键ID")

    private String id;

    /**
     * 监测点预警ID（关联预警记录表JC_CA10_CGQYJ）
     */
    @ExcelProperty(value = "监测点预警ID")

    private String monitoringPointWarningId;

    /**
     * 预警等级（如C1/C2/C3/C4，与YJDJ字段语义一致）
     */
    @ExcelProperty(value = "预警等级")

    private String warningLevel;

    /**
     * 预警发生时间（含年月日时分秒的时间戳）
     */
    @ExcelProperty(value = "预警发生时间")

    private Date warningOccurTime;

    /**
     * 处置类型编码（1=人工核查，2=系统自动处置，3=派单处置等）
     */
    @ExcelProperty(value = "处置类型编码")

    private String disposalTypeCode;

    /**
     * 备份预警图片数量（处置时留存的预警现场图片数）
     */
    @ExcelProperty(value = "备份预警图片数量")

    private Long backupWarningPicCount;

    /**
     * 处置评语（处置结果、现场情况等文字说明）
     */
    @ExcelProperty(value = "处置评语")

    private String disposalComments;

    /**
     * 是否完毕（处置流程）（0=未完毕，1=已完毕）
     */
    @ExcelProperty(value = "是否完毕")

    private Long isDisposalCompleted;

    /**
     * 是否处理良好（0=否，1=是；评价处置效果）
     */
    @ExcelProperty(value = "是否处理良好")

    private Long isDisposalEffective;

    /**
     * 是否关闭预警（0=未关闭，1=已关闭；预警闭环标识）
     */
    @ExcelProperty(value = "是否关闭预警")

    private Long isWarningClosed;

    /**
     * 处置人（执行处置操作的人员姓名/ID）
     */
    @ExcelProperty(value = "处置人")

    private String disposalPerson;

    /**
     * 处置时间（完成处置操作的时间戳）
     */
    @ExcelProperty(value = "处置时间")

    private Date disposalTime;


}
