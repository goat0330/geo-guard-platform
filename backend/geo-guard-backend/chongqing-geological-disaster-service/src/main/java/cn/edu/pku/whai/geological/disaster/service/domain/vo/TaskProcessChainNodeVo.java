/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 任务流程链路节点展示对象
 */
@Data
public class TaskProcessChainNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点 id
     */
    @ExcelProperty(value = "节点id")
    private Long nodeId;

    /**
     * 链路 id
     */
    @ExcelProperty(value = "链路id")
    private String chainId;

    /**
     * 父节点所在链路 id
     */
    @ExcelProperty(value = "父节点所在链路id")
    private String parentChainId;

    /**
     * 上游节点 id
     */
    @ExcelProperty(value = "上游节点id")
    private Long parentNodeId;

    /**
     * 环节名称
     */
    @ExcelProperty(value = "环节名称")
    private String linkName;

    /**
     * 环节触发原因
     */
    @ExcelProperty(value = "环节触发原因")
    private String triggerReason;

    /**
     * 操作人 id
     */
    @ExcelProperty(value = "操作人id")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @ExcelProperty(value = "操作人名称")
    private String operatorName;

    /**
     * 操作人角色
     */
    @ExcelProperty(value = "操作人角色")
    private String operatorRole;

    /**
     * 触发时机
     */
    @ExcelProperty(value = "触发时机")
    private Date triggerTime;

    /**
     * 业务类型编码
     */
    @ExcelProperty(value = "业务类型编码")
    private Integer type;

    /**
     * 业务类型编码
     */
    @ExcelProperty(value = "业务类型编码")
    private Integer bizType;

    /**
     * 业务类型展示名
     */
    @ExcelProperty(value = "业务类型展示名")
    private String bizTypeLabel;

    /**
     * 业务 id
     */
    @ExcelProperty(value = "业务id")
    private Long bizId;

    /**
     * 任务 id
     */
    @ExcelProperty(value = "任务id")
    private Long taskId;

    /**
     * 任务状态
     */
    @ExcelProperty(value = "任务状态")
    private Integer taskStatus;

    /**
     * 具体任务类型展示文案
     */
    @ExcelProperty(value = "具体任务类型")
    private String taskType;

    /**
     * 方案类型编码：1人员安置/2警示防护/3监测巡查（群测群防）/4排危除险/5工程治理/6宣传告知/7交通管制/8其他建议/9监测巡查（仪器监测）
     */
    @ExcelProperty(value = "方案类型编码")
    private Integer planType;

    /**
     * 任务来源或流程来源编码
     */
    @ExcelProperty(value = "任务来源或流程来源编码")
    private Integer sourceType;

    /**
     * 任务来源或流程来源编码
     */
    @ExcelProperty(value = "任务来源或流程来源编码")
    private Integer sourceTypeCode;

    /**
     * 任务来源或流程来源展示名
     */
    @ExcelProperty(value = "任务来源或流程来源展示名")
    private String sourceTypeLabel;

    /**
     * 流程节点类别
     */
    @ExcelProperty(value = "流程节点类别")
    private Integer nodeCategory;

    /**
     * 流程节点类别展示名
     */
    @ExcelProperty(value = "流程节点类别展示名")
    private String nodeCategoryLabel;

    /**
     * 业务阶段分类编码
     */
    @ExcelProperty(value = "业务阶段分类编码")
    private Integer stageType;

    /**
     * 业务阶段分类展示名
     */
    @ExcelProperty(value = "业务阶段分类展示名")
    private String stageTypeLabel;

    /**
     * 完整业务链根链路 id
     */
    @ExcelProperty(value = "完整业务链根链路id")
    private String rootChainId;

    /**
     * 完整业务根链路闭环标志：0未结束 1已结束
     */
    @ExcelProperty(value = "完整业务根链路闭环标志")
    private Integer rootChainClosed;

    /**
     * 列表展示业务类型
     */
    @ExcelProperty(value = "列表展示业务类型")
    private Integer displayBizType;

    /**
     * 列表展示业务 id
     */
    @ExcelProperty(value = "列表展示业务id")
    private Long displayBizId;

    /**
     * 链路展示分段类型
     */
    @ExcelProperty(value = "链路展示分段类型")
    private Integer chainSegmentType;

    /**
     * 链路展示分段类型展示名
     */
    @ExcelProperty(value = "链路展示分段类型展示名")
    private String chainSegmentTypeLabel;

    /**
     * 根来源业务类型
     */
    @ExcelProperty(value = "根来源业务类型")
    private Integer rootBizType;

    /**
     * 根来源业务 id
     */
    @ExcelProperty(value = "根来源业务id")
    private Long rootBizId;

    /**
     * 根来源类型
     */
    @ExcelProperty(value = "根来源类型")
    private Integer rootSourceType;

    /**
     * 节点创建时间
     */
    @ExcelProperty(value = "节点创建时间")
    private Date createDate;

    /**
     * 节点更新时间
     */
    @ExcelProperty(value = "节点更新时间")
    private Date updateDate;

    /**
     * 批量节点下的独立子任务链路
     */
    private List<List<TaskProcessChainNodeVo>> childChains;
}
