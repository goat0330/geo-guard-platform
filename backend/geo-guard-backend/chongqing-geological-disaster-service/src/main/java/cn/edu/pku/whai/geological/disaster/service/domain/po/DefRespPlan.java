/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("dz_def_resp_plan")
public class DefRespPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
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
     * 方案类型（单点/区域等）
     */
    private Integer type;

    /**
     * 流程状态 0～6（0未启动 1方案生成 2专家确认 3行政审批 4启动防御响应 5任务生成与推送 6响应结束；
     * 县级行为流程权威，乡镇行由服务镜像同步）
     */
    private Integer status;

    /**
     * 方案id
     */
    private Long handleId;

    /**
     * 单点关联的乡镇级区域防御方案 id（仅指向乡镇行 def_id，不指向县级）
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
     * 所属乡镇/街道/区；县级可存多街道拼接串，乡镇行通常单街道
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
     * 响应级别；统一采用数值越大表示响应等级越高（1：IV级/蓝色 2：III级/黄色 3：II级/橙色 4：I级/红色）。
     * 乡镇行不独立定义等级，当前值用于继承所关联县级行的等级。
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
     * 父级区域防御响应ID（乡镇行指向县级 id）
     */
    private Long parentDefId;

    /**
     * 执行状态：0未执行 1执行中 2已关闭 3已结束
     */
    private Integer executeStatus;

    /**
     * 关闭时间
     */
    private Date closeTime;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 来源 data_alarm 主键
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
