/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 风险评估预警联动记录对象 dz_risk_assessment_warning_relation
 */
@Data
@TableName("dz_risk_assessment_warning_relation")
public class DzRiskAssessmentWarningRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * data_tp_warning_event.id
     */
    private Long warningEventId;

    /**
     * dz_risk_assessment.id
     */
    private Long riskAssessmentId;

    /**
     * 斜坡单元ID
     */
    private String slopeUnitId;

    /**
     * 最近一次已处理的处置类型
     */
    private String lastDisposalType;

    /**
     * 最近一次已处理的处置时间
     */
    private Date lastDisposalTime;

    /**
     * 最近一次已处理的有效预警标识
     */
    private Integer lastValidWarning;

    private Date createTime;

    private Date updateTime;
}
