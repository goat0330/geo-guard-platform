package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务处置审批业务对象 dz_task_handle_approval
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskHandleApproval.class, reverseConvertGenerate = false)
public class DzTaskHandleApprovalBo extends BaseEntity {

    /**
     * id
     */
    private Long id;

    /**
     * handle_id
     */
    private Long handleId;

    /**
     * 会议类型: 1.处置管理  2.防御响应  3.预测模式
     */
    private Integer meetingType;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String nickname;

    /**
     * 审批类型（1：打勾 2：审批）
     */
    private Integer type;

    /**
     * 处置任务步骤
     */
    private Integer process;

    /**
     * 审批状态（0：未审批 1：已审批）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 所属轮次；回退后重新进入审批节点时递增。
     */
    private Integer roundNo;


    /**
     * 会议id
     */
    private Long meetingId;
}
