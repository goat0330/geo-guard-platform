/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DefRespPlan.class)
public class DefRespPlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "方案编码")
    private String code;

    @ExcelProperty(value = "方案名称")
    private String name;

    @ExcelProperty(value = "方案类型")
    private Integer type;

    @ExcelProperty(value = "状态")
    private Integer status;

    @ExcelProperty(value = "方案id")
    private Long handleId;

    /**
     * 单点关联的乡镇级区域防御方案 id（县级不填此字段）
     */
    @ExcelProperty(value = "乡镇级区域防御方案id")
    private Long regId;

    @ExcelProperty(value = "中心点")
    private String center;

    @ExcelProperty(value = "触发条件")
    private String triggerCondition;

    @ExcelProperty(value = "所属区/县/县级市全称")
    private String county;

    @ExcelProperty(value = "所属乡镇/街道/区")
    private String streets;

    /**
     * 区域：按 streets 解析后的斜坡单元数量（enrich）
     */
    @ExcelProperty(value = "范围内的斜坡单元数量")
    private Integer slopeUnitCount;

    /**
     * 单点：关联处置 handle 的斜坡单元 id（enrich）
     */
    @ExcelProperty(value = "斜坡单元id")
    private String slopeUnitId;

    @ExcelProperty(value = "责任单位")
    private String responsibilityUnit;

    @ExcelProperty(value = "责任人")
    private String responsiblePerson;

    @ExcelProperty(value = "责任人手机号")
    private String responsiblePersonPhone;

    @ExcelProperty(value = "响应级别")
    private Integer level;

    @ExcelProperty(value = "任务发布总数")
    private Long taskPublishTotal;

    @ExcelProperty(value = "任务完成总数")
    private Long taskCompleteTotal;

    @ExcelProperty(value = "创建时间")
    private Date createDate;

    @ExcelProperty(value = "更新时间")
    private Date updateDate;

    @ExcelProperty(value = "删除状态: 0.未删除 1.已删除")
    private Integer deleted;

    /**
     * 区域层级：1 县级 2 乡镇（区域方案必有）
     */
    @ExcelProperty(value = "区域层级")
    private Integer regionScopeType;

    /**
     * 乡镇行父级 id，指向县级方案主键
     */
    @ExcelProperty(value = "父级防御方案id")
    private Long parentDefId;

    /**
     * 区域行执行状态：未执行/执行中/已关闭/已结束（乡镇生命周期）
     */
    @ExcelProperty(value = "区域执行状态")
    private Integer executeStatus;

    /**
     * 区域行关闭时间
     */
    @ExcelProperty(value = "区域行关闭时间")
    private Date closeTime;

    /**
     * 区域行关闭原因
     */
    @ExcelProperty(value = "区域行关闭原因")
    private String closeReason;

    /**
     * 来源预警 id
     */
    @ExcelProperty(value = "来源预警id")
    private Long sourceAlarmId;

    /**
     * 当前生效轮次；用于按当前轮次查询审批、进度、历史数据。
     */
    @ExcelProperty(value = "当前轮次")
    private Integer currentRoundNo;

    /**
     * 审批状态：0-无，1-待审批
     */
    @ExcelProperty(value = "审批状态")
    private Integer approvalStatus;
}
