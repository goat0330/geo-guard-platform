/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import org.dromara.system.domain.vo.SysUserVo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务流程链路最新节点列表展示对象。
 */
@Data
public class TaskProcessChainSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 兼容旧任务列表的业务 id，取 displayBizId。
     */
    @ExcelProperty(value = "业务id")
    private Long id;

    /**
     * 汇总表 id
     */
    @ExcelProperty(value = "汇总表id")
    private Long summaryId;

    /**
     * 最新流程节点 id
     */
    @ExcelProperty(value = "最新流程节点id")
    private Long nodeId;

    /**
     * 当前节点所在链路 id
     */
    @ExcelProperty(value = "当前节点所在链路id")
    private String chainId;

    /**
     * 父节点所在链路 id
     */
    @ExcelProperty(value = "父节点所在链路id")
    private String parentChainId;

    /**
     * 上游流程节点 id
     */
    @ExcelProperty(value = "上游流程节点id")
    private Long parentNodeId;

    /**
     * 当前环节名称
     */
    @ExcelProperty(value = "当前环节名称")
    private String linkName;

    /**
     * 当前环节触发原因
     */
    @ExcelProperty(value = "当前环节触发原因")
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
     * 节点触发时间
     */
    @ExcelProperty(value = "节点触发时间")
    private Date triggerTime;

    /**
     * 当前节点业务类型
     */
    @ExcelProperty(value = "当前节点业务类型")
    private Integer bizType;

    /**
     * 当前节点业务 id
     */
    @ExcelProperty(value = "当前节点业务id")
    private Long bizId;

    /**
     * 对应任务 id
     */
    @ExcelProperty(value = "对应任务id")
    private Long taskId;

    /**
     * 流程来源类型
     */
    @ExcelProperty(value = "流程来源类型")
    private Integer sourceType;

    /**
     * 流程节点类别
     */
    @ExcelProperty(value = "流程节点类别")
    private Integer nodeCategory;

    /**
     * 业务阶段分类
     */
    @ExcelProperty(value = "业务阶段分类")
    private Integer stageType;

    /**
     * 当前节点创建时间
     */
    @ExcelProperty(value = "当前节点创建时间")
    private Date nodeCreateDate;

    /**
     * 当前节点更新时间
     */
    @ExcelProperty(value = "当前节点更新时间")
    private Date nodeUpdateDate;

    /**
     * 列表行代表的业务类型
     */
    @ExcelProperty(value = "列表行代表的业务类型")
    private Integer displayBizType;

    /**
     * 列表行代表的业务 id
     */
    @ExcelProperty(value = "列表行代表的业务id")
    private Long displayBizId;

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
     * 链路展示分段类型
     */
    @ExcelProperty(value = "链路展示分段类型")
    private Integer chainSegmentType;

    /**
     * 链路展示分段名称
     */
    @ExcelProperty(value = "链路展示分段名称")
    private String chainSegmentTypeLabel;

    /**
     * 当前状态编码
     */
    @ExcelProperty(value = "当前状态编码")
    private Integer currentStatus;

    /**
     * 统一风险等级
     */
    @ExcelProperty(value = "统一风险等级")
    private Integer riskLevel;

    /**
     * 风险等级来源
     */
    @ExcelProperty(value = "风险等级来源")
    private Integer riskLevelSource;

    /**
     * 风险等级来源名称
     */
    @ExcelProperty(value = "风险等级来源名称")
    private String riskLevelSourceLabel;

    /**
     * 斜坡单元 id
     */
    @ExcelProperty(value = "斜坡单元id")
    private String unitId;

    /**
     * 省
     */
    @ExcelProperty(value = "省")
    private String province;

    /**
     * 市
     */
    @ExcelProperty(value = "市")
    private String city;

    /**
     * 区县
     */
    @ExcelProperty(value = "区县")
    private String county;

    /**
     * 乡镇街道
     */
    @ExcelProperty(value = "乡镇街道")
    private String street;

    /**
     * 村社区
     */
    @ExcelProperty(value = "村社区")
    private String village;

    /**
     * 试点区域 1
     */
    @ExcelProperty(value = "试点区域1")
    private Integer pilotArea1;

    /**
     * 试点区域 2
     */
    @ExcelProperty(value = "试点区域2")
    private Integer pilotArea2;

    /**
     * 当前环节责任人，多个用英文逗号拼接
     */
    @ExcelProperty(value = "当前环节责任人")
    private String responsiblePerson;

    /**
     * 当前环节责任人手机号，多个用英文逗号拼接
     */
    @ExcelProperty(value = "当前环节责任人手机号")
    private String responsiblePersonPhone;

    /**
     * 当前节点所在链路范围待推送任务数量
     */
    @ExcelProperty(value = "待推送任务数量")
    private Integer pendingPushTaskCount;

    /**
     * 斜坡当前动态风险等级
     */
    @ExcelProperty(value = "动态风险等级")
    private Integer dynamicRiskLevel;

    /**
     * 巡查要求
     */
    @ExcelProperty(value = "巡查要求")
    private String inspectingRequire;

    /**
     * 提交要求
     */
    @ExcelProperty(value = "提交要求")
    private String submitRequire;

    /**
     * 防御响应 id
     */
    @ExcelProperty(value = "防御响应id")
    private Long defId;

    /**
     * 处置 id
     */
    @ExcelProperty(value = "处置id")
    private Long handleId;

    /**
     * 风险评估 id，优先取任务生成时绑定的风险评估，否则取斜坡单元最新风险评估
     */
    @ExcelProperty(value = "风险评估id")
    private Long riskId;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;

    /**
     * 斜坡单元信息
     */
    private SlopeUnitVo slopeUnit;

    /**
     * 责任人用户信息
     */
    private SysUserVo sysUser;

    /**
     * 当前流程节点
     */
    private TaskProcessChainNodeVo currentNode;

    /**
     * 兼容旧字段：最新流程节点
     */
    private TaskProcessChainNodeVo latestProcessNode;
}
