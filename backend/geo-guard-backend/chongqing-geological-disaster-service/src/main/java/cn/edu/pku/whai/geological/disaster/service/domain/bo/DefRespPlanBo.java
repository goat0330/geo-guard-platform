/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DefRespPlan.class, reverseConvertGenerate = false)
public class DefRespPlanBo extends BaseEntity {

    /**
     * 主键 id
     */
    private Long id;

    /**
     * 方案编码
     */
    private String code;

    /**
     * 方案名称
     */
    private String name;

    /**
     * 方案类型（如单点、区域，取值见业务字典/枚举）
     */
    private Integer type;

    /**
     * 流程状态（0～6：未启动、方案生成、专家确认、行政审批、启动防御响应、任务生成与推送、响应结束；县级为权威状态）
     */
    private Integer status;

    /**
     * 方案id
     */
    private Long handleId;

    /**
     * 单点方案关联的乡镇级区域防御方案 id（仅 region_scope_type=乡镇 的 def_id；不指向县级）
     */
    private Long regId;

    /**
     * 中心点
     */
    private String center;

    /**
     * 触发条件
     */
    private String triggerCondition;

    /**
     * 所属区/县/县级市全称
     */
    private String county;

    /**
     * 所属乡镇/街道/区；县级可为多街道逗号拼接，乡镇行一般为单街道
     */
    private String streets;

    /**
     * 责任单位
     */
    private String responsibilityUnit;

    /**
     * 责任人
     */
    private String responsiblePerson;

    /**
     * 责任人手机号
     */
    private String responsiblePersonPhone;

    /**
     * 响应级别（预警/方案等级）；乡镇行可能为建镇时快照，列表层 enrich 会以县级 level 覆盖乡镇行展示
     */
    private Integer level;

    /**
     * 任务发布总数
     */
    private Long taskPublishTotal;

    /**
     * 任务完成总数
     */
    private Long taskCompleteTotal;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 删除状态: 0.未删除 1.已删除
     */
    private Integer deleted;

    /**
     * 区域层级：1 县级 2 乡镇（type=区域时须填写）
     */
    private Integer regionScopeType;

    /**
     * 父级防御方案 id：乡镇行必填，指向县级方案主键
     */
    private Long parentDefId;

    /**
     * 区域行执行状态：0 未执行 1 执行中 2 已关闭 3 已结束（与流程 status 区分，用于乡镇生命周期）
     */
    private Integer executeStatus;

    /**
     * 区域行关闭时间（范围缩小、随县级归档等）
     */
    private Date closeTime;

    /**
     * 区域行关闭原因文案（业务常量或可读说明）
     */
    private String closeReason;

    /**
     * 来源预警主键（定时任务由 data_alarm 生成县级方案时写入）
     */
    private Long sourceAlarmId;

    /**
     * 当前生效轮次；状态回退并重新进入后续节点时递增。
     */
    private Integer currentRoundNo;

    /**
     * 审批状态：0-无，1-待审批
     */
    private Integer approvalStatus;
}
