/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地灾风险预测推送对象 dz_risk_prediction_push
 *
 * @author system
 * @date 2026-03-03
 */
@Data
@TableName("dz_risk_prediction_push")
public class DzRiskPredictionPush implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 风险预测的批次id
     */
    private Long predictionBatchId;

    /**
     * 报文名称
     */
    private String title;

    /**
     * 报文内容
     */
    private String content;

    /**
     * 预测类型（1：日 2：周 3：月 4：季 5：年）
     */
    private Integer type;

    /**
     * 推送状态（0：未推送 1：已推送）
     */
    private Integer status;

    /**
     * 推送人员
     */
    private String person;

    /**
     * 推送人数
     */
    private Integer personCount;

    /**
     * 文件 id（ossId）
     */
    private Long docId;

    /**
     * 创建时间
     */
    private Date createDate;
}
