/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 预警处置记录监测对象 v_warning_disposal_record_monitor
 **/
@Data
@TableName("v_warning_disposal_record_monitor")
public class WarningDisposalRecordMonitor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预警处置记录唯一主键ID（业务唯一标识）
     */
    @TableId(value = "id")
    private String id;

    /**
     * 监测点预警ID（关联预警记录表JC_CA10_CGQYJ）
     */
    private String monitoringPointWarningId;

    /**
     * 预警等级（如C1/C2/C3/C4，与YJDJ字段语义一致）
     */
    private String warningLevel;

    /**
     * 预警发生时间（含年月日时分秒的时间戳）
     */
    private Date warningOccurTime;

    /**
     * 处置类型编码（1=人工核查，2=系统自动处置，3=派单处置等）
     */
    private String disposalTypeCode;

    /**
     * 备份预警图片数量（处置时留存的预警现场图片数）
     */
    private Long backupWarningPicCount;

    /**
     * 处置评语（处置结果、现场情况等文字说明）
     */
    private String disposalComments;

    /**
     * 是否完毕（处置流程）（0=未完毕，1=已完毕）
     */
    private Long isDisposalCompleted;

    /**
     * 是否处理良好（0=否，1=是；评价处置效果）
     */
    private Long isDisposalEffective;

    /**
     * 是否关闭预警（0=未关闭，1=已关闭；预警闭环标识）
     */
    private Long isWarningClosed;

    /**
     * 处置人（执行处置操作的人员姓名/ID）
     */
    private String disposalPerson;

    /**
     * 处置时间（完成处置操作的时间戳）
     */
    private Date disposalTime;


}
