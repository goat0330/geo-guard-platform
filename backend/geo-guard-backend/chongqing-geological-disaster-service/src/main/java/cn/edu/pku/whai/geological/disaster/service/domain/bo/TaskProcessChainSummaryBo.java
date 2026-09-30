/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 任务流程链路最新节点列表查询条件。
 */
@Data
public class TaskProcessChainSummaryBo {

    /**
     * 兼容旧任务列表的业务 id
     */
    private Long id;

    /**
     * 最新流程节点 id
     */
    private Long nodeId;

    /**
     * 展示任务 id
     */
    private Long taskId;

    /**
     * 当前节点所在链路 id
     */
    private String chainId;

    /**
     * 完整业务链根链路 id
     */
    private String rootChainId;

    /**
     * 完整业务根链路闭环标志：0未结束 1已结束
     */
    private Integer rootChainClosed;

    /**
     * 列表行代表的业务类型
     */
    private Integer displayBizType;

    /**
     * 列表行代表的业务 id
     */
    private Long displayBizId;

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
     * 链路展示分段类型
     */
    private Integer chainSegmentType;

    /**
     * 多个链路展示分段类型
     */
    private List<Integer> chainSegmentTypes;

    /**
     * 当前环节名称
     */
    private String linkName;

    /**
     * 当前状态编码
     */
    private Integer currentStatus;

    /**
     * 多个当前状态编码
     */
    private List<Integer> currentStatusList;

    /**
     * 统一风险等级
     */
    private Integer riskLevel;

    /**
     * 多个统一风险等级
     */
    private List<Integer> riskLevels;

    /**
     * 风险等级来源
     */
    private Integer riskLevelSource;

    /**
     * 斜坡单元 id
     */
    private String unitId;

    /**
     * 区县
     */
    private String county;

    /**
     * 乡镇街道
     */
    private String street;

    /**
     * 村社区
     */
    private String village;

    /**
     * 试点区域 1
     */
    private Integer pilotArea1;

    /**
     * 试点区域 2
     */
    private Integer pilotArea2;

    /**
     * 当前环节责任人
     */
    private String responsiblePerson;

    /**
     * 当前环节责任人手机号
     */
    private String responsiblePersonPhone;

    /**
     * 流程来源类型
     */
    private Integer sourceType;

    /**
     * 多个流程来源类型
     */
    private List<Integer> sourceTypeList;

    /**
     * 当前节点所在链路范围待推送任务数量
     */
    private Integer pendingPushTaskCount;

    /**
     * 兼容动态风险等级
     */
    private Integer dynamicRiskLevel;

    /**
     * 多个兼容动态风险等级
     */
    private List<Integer> dynamicRiskLevels;

    /**
     * 防御响应 id
     */
    private Long defId;

    /**
     * 处置 id
     */
    private Long handleId;

    /**
     * 风险评估 id
     */
    private Long riskId;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateDate;

    /**
     * 节点触发时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date triggerTime;

    /**
     * 升序排序字段
     */
    private List<String> asc;

    /**
     * 降序排序字段
     */
    private List<String> desc;
}
