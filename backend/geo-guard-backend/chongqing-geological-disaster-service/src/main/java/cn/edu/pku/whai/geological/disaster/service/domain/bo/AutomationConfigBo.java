/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 自动化运营配置入参。
 */
@Data
public class AutomationConfigBo {

    /**
     * 日常巡逻任务定时生成开关。
     */
    private Boolean dailyPatrolGenerateEnabled;

    /**
     * 日常巡逻任务生成时间，格式为 HH:mm。
     */
    private String dailyPatrolGenerateTime;

    /**
     * 日常巡逻任务生成后自动推送开关。
     */
    private Boolean dailyPatrolAutoPushEnabled;

    /**
     * 未推送任务自动推送开关。
     */
    private Boolean unpushedAutoPushEnabled;

    /**
     * 允许自动推送的未推送任务来源类型列表。
     */
    private List<Integer> unpushedSourceTypes;

    /**
     * 自动催办开关。
     */
    private Boolean reminderEnabled;

    /**
     * 自动催办执行时间，格式为 HH:mm。
     */
    private String reminderTime;

    /**
     * 允许自动催办的任务来源类型列表。
     */
    private List<Integer> reminderSourceTypes;

    /**
     * 预警监测任务自动推送开关。
     */
    private Boolean warningAutoPushMonitorTaskEnabled;

    /**
     * 报灾自动处理开关。
     */
    private Boolean reportAutoHandleEnabled;

    /**
     * 报灾任务自动推送开关。
     */
    private Boolean reportAutoPushTaskEnabled;

    /**
     * 风险预测自动创建推送记录开关。
     */
    private Boolean predictionAutoCreatePushEnabled;

    /**
     * 风险预测自动推送开关。
     */
    private Boolean predictionAutoPushEnabled;

    /**
     * 普通任务推送后自动发送短信开关。
     */
    private Boolean taskSmsEnabled;

    /**
     * 消息中心通知自动生成开关。
     */
    private Boolean msgNoticeEnabled;

    /**
     * 同步失败自动重试开关。
     */
    private Boolean syncRetryEnabled;

    /**
     * 同步异常自动告警开关。
     */
    private Boolean syncAlertEnabled;
}
