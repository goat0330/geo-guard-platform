/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务流程链路节点 dz_task_process_chain_node
 */
@Data
@TableName("dz_task_process_chain_node")
public class DzTaskProcessChainNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 同一条流程链路统一标识
     */
    private String chainId;

    /**
     * 父节点所在链路 id
     */
    private String parentChainId;

    /**
     * 上游流程节点 id
     */
    private Long parentNodeId;

    /**
     * 环节名称
     */
    private String linkName;

    /**
     * 环节触发原因
     */
    private String triggerReason;

    /**
     * 操作人 id
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 触发时机
     */
    private Date triggerTime;

    /**
     * 业务类型
     */
    private Integer bizType;

    /**
     * 业务 id
     */
    private Long bizId;

    /**
     * 对应任务 id
     */
    private Long taskId;

    /**
     * 任务来源或流程来源
     */
    private Integer sourceType;

    /**
     * 流程节点类别
     */
    private Integer nodeCategory;

    /**
     * 业务阶段分类
     */
    private Integer stageType;

    /**
     * 完整业务链根链路 id
     */
    private String rootChainId;

    /**
     * 完整业务根链路闭环标志：0未结束 1已结束
     */
    private Integer rootChainClosed;

    /**
     * 列表展示业务类型
     */
    private Integer displayBizType;

    /**
     * 列表展示业务 id
     */
    private Long displayBizId;

    /**
     * 链路展示分段类型
     */
    private Integer chainSegmentType;

    /**
     * 根来源业务类型
     */
    private Integer rootBizType;

    /**
     * 根来源业务 id
     */
    private Long rootBizId;

    /**
     * 根来源类型
     */
    private Integer rootSourceType;

    /**
     * 删除标志
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;
}
