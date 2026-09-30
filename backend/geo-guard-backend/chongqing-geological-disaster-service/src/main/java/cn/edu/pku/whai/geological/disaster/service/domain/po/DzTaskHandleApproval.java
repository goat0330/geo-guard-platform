package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务处置审批对象 dz_task_handle_approval
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
@Data
@TableName("dz_task_handle_approval")
public class DzTaskHandleApproval implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 所属轮次；同一 handle_id/meeting_type/process 在不同轮次允许重复出现。
     */
    private Integer roundNo;


}
