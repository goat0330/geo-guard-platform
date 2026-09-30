/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务流程链路最新节点列表汇总读模型对象 dz_task_process_chain_summary
 */
@Data
@TableName("dz_task_process_chain_summary")
public class DzTaskProcessChainSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 汇总主键
     */
    private Long id;

    /**
     * 当前列表行对应的最新流程节点 id
     */
    private Long nodeId;

    /**
     * 当前最新节点所在链路 id
     */
    private String chainId;

    /**
     * 当前最新节点父节点所在链路 id
     */
    private String parentChainId;

    /**
     * 当前最新节点上游流程节点 id
     */
    private Long parentNodeId;

    /**
     * 当前最新节点环节名称
     */
    private String linkName;

    /**
     * 当前最新节点触发原因
     */
    private String triggerReason;

    /**
     * 当前最新节点操作人 id
     */
    private Long operatorId;

    /**
     * 当前最新节点操作人名称
     */
    private String operatorName;

    /**
     * 当前最新节点操作人角色
     */
    private String operatorRole;

    /**
     * 当前最新节点触发时间
     */
    private Date triggerTime;

    /**
     * 当前最新节点业务类型：0未知 1任务 2报灾/报告 3处置管理 4防御响应 5预警信息 6监测预警
     */
    private Integer bizType;

    /**
     * 当前最新节点业务 id；实际指向由 bizType 决定：1任务id 2报灾/报告id 3处置id 4防御响应id 5预警信息id 6监测预警id
     */
    private Long bizId;

    /**
     * 展示任务 id，优先取展示任务，兼容链路节点 task_id
     */
    private Long taskId;

    /**
     * 当前最新节点流程来源类型：0未知 1任务反馈 2群众报灾 3预警信息 4监测预警 5系统评估 6手动添加 7技术协查 8处置管理 9防御响应 10应急处置
     */
    private Integer sourceType;

    /**
     * 当前最新节点流程节点类别：1任务核查中 2任务反馈 3申请技术协查 4开启处置管理 5开启单点防御响应 6启动区域防御响应 7批量下发处置任务 8任务关闭 9任务过期 10批量下发防御响应任务 11群众报灾上报 12任务反馈报告 13结束归档
     */
    private Integer nodeCategory;

    /**
     * 当前最新节点业务阶段分类：0防御响应 1起始任务 2险情核实 3应急调查 4处置管理 5单点防御 6复盘归档
     */
    private Integer stageType;

    /**
     * 当前最新节点创建时间
     */
    private Date nodeCreateDate;

    /**
     * 当前最新节点更新时间
     */
    private Date nodeUpdateDate;

    /**
     * 列表行唯一展示业务类型，未删除数据下与 displayBizId 组成唯一口径：0未知 1任务 2报灾/报告 3处置管理 4防御响应 5预警信息 6监测预警
     */
    private Integer displayBizType;

    /**
     * 列表行唯一展示业务 id，未删除数据下与 displayBizType 组成唯一口径；实际指向由 displayBizType 决定
     */
    private Long displayBizId;

    /**
     * 完整业务链根链路 id
     */
    private String rootChainId;

    /**
     * 完整业务根链路闭环标志：0未结束 1已结束
     */
    private Integer rootChainClosed;

    /**
     * 根来源业务类型：0未知 1任务 2报灾/报告 3处置管理 4防御响应 5预警信息 6监测预警
     */
    private Integer rootBizType;

    /**
     * 根来源业务 id；实际指向由 rootBizType 决定
     */
    private Long rootBizId;

    /**
     * 根来源流程来源类型：0未知 1任务反馈 2群众报灾 3预警信息 4监测预警 5系统评估 6手动添加 7技术协查 8处置管理 9防御响应 10应急处置
     */
    private Integer rootSourceType;

    /**
     * 链路展示分段类型：0未知 1来源前置链 2普通业务主链 3防御响应任务主链 4处置管理主链 5应急处置任务子链 9其他派生子链
     */
    private Integer chainSegmentType;

    /**
     * 当前状态：1流转中 2执行中 3已反馈 4技术协查 5已过期 6已结束
     */
    private Integer currentStatus;

    /**
     * 当前阶段口径风险等级；按最新节点 bizType 解析，MONITOR_WARNING 阶段同样取斜坡当前正式风险等级
     */
    private Integer riskLevel;

    /**
     * 当前阶段口径风险等级来源：0未知 1风险评估 2报灾人工修正 3报灾AI识别 4处置事件等级 5防御响应等级 6监测预警等级(兼容历史值) 7预警报告等级
     */
    private Integer riskLevelSource;

    /**
     * 展示业务关联的斜坡单元 id
     */
    private String unitId;

    /**
     * 斜坡单元所在省
     */
    private String province;

    /**
     * 斜坡单元所在市
     */
    private String city;

    /**
     * 斜坡单元所在区县
     */
    private String county;

    /**
     * 斜坡单元所在乡镇街道
     */
    private String street;

    /**
     * 斜坡单元所在村社区
     */
    private String village;

    /**
     * 斜坡单元试点区域标识 1
     */
    @TableField(value = "pilot_area_1")
    private Integer pilotArea1;

    /**
     * 斜坡单元试点区域标识 2
     */
    @TableField(value = "pilot_area_2")
    private Integer pilotArea2;

    /**
     * 当前环节责任人名称，多个值使用英文逗号拼接
     */
    private String responsiblePerson;

    /**
     * 当前环节责任人手机号，多个值使用英文逗号拼接
     */
    private String responsiblePersonPhone;

    /**
     * 当前节点所在链路范围待推送任务数量，按未删除且 status=1 的去重任务统计
     */
    private Integer pendingPushTaskCount;

    /**
     * 斜坡当前正式动态风险等级，优先取任务 risk_id 对应风险评估，否则按 unit_id 取最新正式风险等级
     */
    private Integer dynamicRiskLevel;

    /**
     * 兼容核查/巡查要求，历史数据可能复用展示任务 dz_task_dist_list.submit_require
     */
    private String inspectingRequire;

    /**
     * 兼容提交要求，取展示任务 dz_task_dist_list.submit_require
     */
    private String submitRequire;

    /**
     * 关联防御响应 id，按当前链路关联的防御响应业务解析
     */
    private Long defId;

    /**
     * 关联处置 id，按当前链路关联的处置业务解析
     */
    private Long handleId;

    /**
     * 关联风险评估 id，优先取展示任务生成时绑定的风险评估，否则按斜坡单元最新风险评估解析
     */
    private Long riskId;

    /**
     * 逻辑删除标志：0未删除 1已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /**
     * 汇总行创建时间
     */
    private Date createDate;

    /**
     * 汇总行更新时间
     */
    private Date updateDate;
}
