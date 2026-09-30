/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 任务派发清单对象 dz_task_dist_list
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
@Data
@TableName("dz_task_dist_list")
public class DzTaskDistList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 计划类型：人员安置
     */
    public static final Integer PLAN_TYPE_RESETTLEMENT = 1;

    /**
     * 计划类型：警示防护
     */
    public static final Integer PLAN_TYPE_PROTECTION = 2;

    /**
     * 计划类型：监测巡查（群测群防）
     */
    public static final Integer PLAN_TYPE_MONITORING = 3;

    /**
     * 计划类型：排危除险
     */
    public static final Integer PLAN_TYPE_HAZARD_REMOVAL = 4;

    /**
     * 计划类型：工程治理
     */
    public static final Integer PLAN_TYPE_ENGINEERING = 5;

    /**
     * 计划类型：宣传告知
     */
    public static final Integer PLAN_TYPE_PUBLICITY = 6;

    /**
     * 计划类型：交通管制
     */
    public static final Integer PLAN_TYPE_TRAFFIC_CONTROL = 7;

    /**
     * 计划类型：其他建议
     */
    public static final Integer PLAN_TYPE_OTHER_SUGGESTIONS = 8;

    /**
     * 计划类型：监测巡查（仪器监测）
     */
    public static final Integer PLAN_TYPE_INSTRUMENT_MONITORING = 9;

    /**
     * 任务状态：未推送
     */
    public static final Integer STATUS_UNPUSHED = 1;

    /**
     * 任务状态：未核查
     */
    public static final Integer STATUS_UNINSPECTED = 2;

    /**
     * 任务状态：核查中
     */
    public static final Integer STATUS_INSPECTING = 3;

    /**
     * 任务状态：已关闭
     */
    public static final Integer STATUS_CLOSED = 4;

    /**
     * 任务状态：已反馈
     */
    public static final Integer STATUS_FEEDBACKED = 5;

    /**
     * 任务状态：申请技术协查
     */
    public static final Integer STATUS_TECH_ASSISTANCE = 6;

    /**
     * 任务状态：已过期
     */
    public static final Integer STATUS_OVERDUE = 7;

    /**
     * 来源：系统评估
     */
    public static final Integer SOURCE_TYPE_EVAL = 1;

    /**
     * 来源：群众上报
     */
    public static final Integer SOURCE_TYPE_REPORT = 2;

    /**
     * 来源：防御响应
     */
    public static final Integer SOURCE_TYPE_DEF_RESP = 3;


    /**
     * 来源：应急处置
     */
    public static final Integer SOURCE_TYPE_EMERGENCY = 4;

    /**
     * 来源：监测预警
     */
    public static final Integer SOURCE_TYPE_MONITOR_WARNING = 5;

    /**
     * 来源：技术协查
     */
    public static final Integer SOURCE_TYPE_TECH_ASSISTANCE = 6;

    /**
     * 来源：手动添加
     */
    public static final Integer SOURCE_TYPE_MANUAL = 0;

    /**
     * 逾期状态：未逾期
     */
    public static final Integer OVERDUE_NO = 0;

    /**
     * 逾期状态：已逾期
     */
    public static final Integer OVERDUE_YES = 1;

    /**
     * APP推送类型：人工推送
     */
    public static final Integer APP_PUSH_TYPE_MANUAL = 1;

    /**
     * APP推送类型：系统推送
     */
    public static final Integer APP_PUSH_TYPE_SYSTEM = 2;

    /**
     * 任务创建方式：人工创建
     */
    public static final Integer TASK_CREATE_TYPE_MANUAL = 1;

    /**
     * 任务创建方式：系统创建
     */
    public static final Integer TASK_CREATE_TYPE_SYSTEM = 2;

    public static final String TASK_TYPE_INSPECTION = "巡查任务";
    public static final String TASK_TYPE_AI_VERIFY = "AI险情核实";
    public static final String TASK_TYPE_DEF_RESP = "防御响应";
    public static final String TASK_TYPE_EMERGENCY = "现场处置";
    public static final String TASK_TYPE_MONITOR_WARNING = "监测预警";
    public static final String TASK_TYPE_EMERGENCY_INVESTIGATION = "应急调查";
    public static final String TASK_TYPE_PUBLIC_REPORT = "群众报灾";

    /**
     * 监测员任务 planName
     */
    public static final String PLAN_NAME_MONITOR = "监测员任务";

    /**
     * 风险区巡查员任务 planName
     */
    public static final String PLAN_NAME_PATROL = "风险区巡查员任务";

    /**
     * 日常巡逻任务 planName
     */
    public static final String PLAN_NAME_DAILY_PATROL = "日常巡逻任务";

    public static final List<String> PATROL_PLAN_NAMES = List.of(
        PLAN_NAME_PATROL,
        PLAN_NAME_DAILY_PATROL
    );

    /**
     * 仍处于待处理的任务状态
     */
    public static final List<Integer> OPEN_STATUSES = List.of(
        STATUS_UNPUSHED,
        STATUS_UNINSPECTED,
        STATUS_INSPECTING,
        STATUS_TECH_ASSISTANCE
    );

    /**
     * APP 允许提交到的任务状态
     */
    public static final List<Integer> APP_SUBMIT_TARGET_STATUSES = List.of(
        STATUS_INSPECTING,
        STATUS_CLOSED,
        STATUS_FEEDBACKED,
        STATUS_TECH_ASSISTANCE
    );

    /**
     * id
     */
    private Long id;

    /**
     * 斜坡单元id
     */
    private String unitId;

    /**
     * 派发人员id
     */
    private Long userId;

    /**
     * 风险评估id
     */
    private Long riskId;

    /**
     * 巡查要求
     */
    private String submitRequire;

    /**
     * 巡查建议
     */
    private String inspectionSuggestion;

    /**
     * 巡查建议备份
     */
    private String inspectionSuggestionBackup;
    /**
     * 现场照片（多条的话，分割）
     */
    private String scenePhoto;

    /**
     * 文字记录
     */
    private String textRecord;

    /**
     * 任务状态（1：未推送 2：未核查 3：核查中 4：已关闭 5：已反馈 6：申请技术协查 7：已过期）
     */
    private Integer status;

    /**
     * APP推送类型：1人工推送 2系统推送
     */
    private Integer appPushType;

    /**
     * APP推送用户ID；系统推送固定为地象智能体(0)
     */
    private Long appPushUserId;

    /**
     * APP推送时间
     */
    private Date appPushTime;

    /**
     * 任务创建方式：1人工创建 2系统创建
     */
    private Integer taskCreateType;

    /**
     * 创建用户ID；系统创建固定为地象智能体(0)
     */
    private Long taskCreateUserId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 检查时间
     */
    private Date checkTime;

    /**
     * 检查坐标中心点
     */
    private String checkCenter;

    /**
     * 打卡坐标生成的地点信息
     */
    private String checkCenterLocation;

    /**
     * 提交时间
     */
    private Date submitTime;

    /**
     * 来源（0：手动添加 1：系统评估 2：群众上报 3：防御响应 4：应急处置 5：监测预警 6：技术协查）
     */
    private Integer sourceType;

    /**
     * 群众上报时报告id
     */
    private Long reportId;

    /**
     * 方案编号(关联方案表唯一标识)
     */
    private Long handleId;

    /**
     * 防御响应方案id
     */
    private Long defId;

    /**
     * 上报信息JSON（仅险情核实任务使用）
     */
    private String reportInfo;

    /**
     * 任务来源业务描述
     */
    private String taskSource;

    /**
     * 上游任务节点ID（技术协查任务填写原任务id）
     */
    private Long relatedTaskId;

    /**
     * 任务类型（巡查任务 / AI险情核实 / 防御响应 / 现场处置 / 监测预警 / 应急调查 / 群众报灾）
     */
    private String taskType;

    /**
     * 任务名称
     */
    private String planName;

    /**
     * 任务类型: 1.人员安置/2.警示防护/3.监测巡查（群测群防）/4.排危除险/5.工程治理/6.宣传告知/7.交通管制/8.其他建议/9.监测巡查（仪器监测）
     */
    private Integer planType;

    /**
     * 责任人
     */
    private String responsiblePerson;

    /**
     * 责任人电话号码
     */
    private String responsiblePersonPhone;

    /**
     * 详细地址
     */
    private String detailedAddress;

    /**
     * 短信正文快照
     */
    private String smsContent;

    /**
     * 删除状态: 0. 未删除 1.已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer delete;

    /**
     * 是否逾期: 0. 未逾期 1.已逾期
     */
    private Integer overdue;

    /**
     * 配额已消耗: 0.未消耗 1.已消耗（仅风险区巡查员任务有效，用户有效完成时置1，与status解耦避免混用）
     */
    public static final Integer QUOTA_CONSUMED_NO = 0;
    public static final Integer QUOTA_CONSUMED_YES = 1;
    private Integer quotaConsumed;

    /**
     * 最后一次催办时间
     */
    private Date lastRemindTime;

    /**
     * 催办次数
     */
    private Integer reminderCount;

    /**
     * 任务关闭原因：如区域防御随乡镇/县级归档、范围缩小等由业务写入可读文案
     */
    private String closeReason;

    /**
     * 任务关闭时间：与 closeReason 同时写入，用于审计与列表展示
     */
    private Date closedTime;

    /**
     * 备注
     */
    private String remark;

    public static boolean isOpenStatus(Integer status) {
        return status != null && OPEN_STATUSES.contains(status);
    }

    public static boolean isPatrolTask(String planName) {
        return planName != null && PATROL_PLAN_NAMES.contains(planName);
    }

    public static boolean isMonitoringPlanType(Integer planType) {
        return PLAN_TYPE_MONITORING.equals(planType) || PLAN_TYPE_INSTRUMENT_MONITORING.equals(planType);
    }
}
