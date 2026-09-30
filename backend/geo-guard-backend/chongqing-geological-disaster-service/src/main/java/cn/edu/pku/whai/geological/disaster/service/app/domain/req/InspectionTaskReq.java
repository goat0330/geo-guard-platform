/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 巡查任务实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionTaskReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 派发时间 (建议格式: yyyy-MM-dd HH:mm:ss)
     */
    private String dispatchTime;

    /**
     * 动态风险等级
     */
    private Integer dynamicRiskLevel;

    /**
     * 巡查建议
     */
    private String inspectionSuggestion;

    /**
     * 巡查员唯一ID
     */
    private Long inspectorId;

    /**
     * 巡查员姓名
     */
    private String inspectorName;

    /**
     * 巡查员电话
     */
    private String inspectorPhone;

    /**
     * 位置中心点 (如: POINT(经度 纬度))
     */
    private String locationCenter;

    /**
     * 位置描述
     */
    private String locationDesc;

    /**
     * 上报信息JSON，仅险情核实任务携带
     */
    private String reportInfo;

    /**
     * 关联任务ID，群众上报、技术协查任务携带
     */
    private Long relatedTaskId;

    /**
     * 任务来源类型：0手动添加 1系统评估 2群众上报 3防御响应 4应急处置 5监测预警 6技术协查
     */
    private Integer sourceType;

    /**
     * 任务来源业务描述
     */
    private String taskSource;

    /**
     * 斜坡单元ID / 隐患点编号
     */
    private String slopeUnitId;

    /**
     * 目标斜坡单元中心点
     */
    private String slopeUnitCenter;

    /**
     * 目标斜坡单元范围WKT
     */
    private String slopeUnitWkt;

    /**
     * 任务状态 (例如: 0-待处理, 1-已完成)
     */
    private Integer status;

    /**
     * 提交要求说明
     */
    private String submitRequire;

    /**
     * 任务唯一ID
     */
    private Long taskId;

    /**
     * 处置表ID，应急调查任务携带
     */
    private Long handleId;

    /**
     * 任务子分类，如监测巡查、排危除险等
     */
    private String taskSubType;

    /**
     * 任务类型：巡查任务、AI险情核实、防御响应、现场处置、监测预警、应急调查、群众报灾
     */
    private String taskType;
}
