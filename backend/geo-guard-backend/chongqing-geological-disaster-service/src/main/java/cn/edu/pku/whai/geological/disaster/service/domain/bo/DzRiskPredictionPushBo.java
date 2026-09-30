/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 地灾风险预测推送业务对象 dz_risk_prediction_push
 *
 * @author system
 * @date 2026-03-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzRiskPredictionPush.class, reverseConvertGenerate = false)
public class DzRiskPredictionPushBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 风险预测的批次id
     */
    @NotNull(message = "风险预测批次id不能为空", groups = {AddGroup.class, EditGroup.class})
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
