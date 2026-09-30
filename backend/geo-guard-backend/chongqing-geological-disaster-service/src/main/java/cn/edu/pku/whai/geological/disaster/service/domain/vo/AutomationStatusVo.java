/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 自动化运营状态。
 */
@Data
@ExcelIgnoreUnannotated
public class AutomationStatusVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自动模式总开关状态。
     */
    @ExcelProperty(value = "自动模式总开关状态")
    private Boolean autoModeEnabled;

    /**
     * 日常巡逻任务定时生成开关。
     */
    @ExcelProperty(value = "日常巡逻任务定时生成开关")
    private Boolean dailyPatrolGenerateEnabled;

    /**
     * 日常巡逻任务生成时间，格式为 HH:mm。
     */
    @ExcelProperty(value = "日常巡逻任务生成时间")
    private String dailyPatrolGenerateTime;

    /**
     * 日常巡逻任务生成后自动推送开关。
     */
    @ExcelProperty(value = "日常巡逻任务自动推送开关")
    private Boolean dailyPatrolAutoPushEnabled;

    /**
     * 未推送任务自动推送开关。
     */
    @ExcelProperty(value = "未推送任务自动推送开关")
    private Boolean unpushedAutoPushEnabled;

    /**
     * 允许自动推送的未推送任务来源类型列表。
     */
    @ExcelProperty(value = "未推送任务来源类型列表")
    private List<Integer> unpushedSourceTypes;

    /**
     * 自动催办开关。
     */
    @ExcelProperty(value = "自动催办开关")
    private Boolean reminderEnabled;

    /**
     * 自动催办执行时间，格式为 HH:mm。
     */
    @ExcelProperty(value = "自动催办执行时间")
    private String reminderTime;

    /**
     * 允许自动催办的任务来源类型列表。
     */
    @ExcelProperty(value = "自动催办来源类型列表")
    private List<Integer> reminderSourceTypes;

    /**
     * 预警监测任务自动推送开关。
     */
    @ExcelProperty(value = "预警监测任务自动推送开关")
    private Boolean warningAutoPushMonitorTaskEnabled;

    /**
     * 报灾自动处理开关。
     */
    @ExcelProperty(value = "报灾自动处理开关")
    private Boolean reportAutoHandleEnabled;

    /**
     * 报灾任务自动推送开关。
     */
    @ExcelProperty(value = "报灾任务自动推送开关")
    private Boolean reportAutoPushTaskEnabled;

    /**
     * 风险预测自动创建推送记录开关。
     */
    @ExcelProperty(value = "风险预测自动创建推送记录开关")
    private Boolean predictionAutoCreatePushEnabled;

    /**
     * 风险预测自动推送开关。
     */
    @ExcelProperty(value = "风险预测自动推送开关")
    private Boolean predictionAutoPushEnabled;

    /**
     * 普通任务推送后自动发送短信开关。
     */
    @ExcelProperty(value = "普通任务短信发送开关")
    private Boolean taskSmsEnabled;

    /**
     * 消息中心通知自动生成开关。
     */
    @ExcelProperty(value = "消息中心通知开关")
    private Boolean msgNoticeEnabled;

    /**
     * 同步失败自动重试开关。
     */
    @ExcelProperty(value = "同步失败自动重试开关")
    private Boolean syncRetryEnabled;

    /**
     * 同步异常自动告警开关。
     */
    @ExcelProperty(value = "同步异常自动告警开关")
    private Boolean syncAlertEnabled;

    /**
     * 最后更新人用户 ID。
     */
    @ExcelProperty(value = "最后更新人用户ID")
    private Long updatedBy;

    /**
     * 最后更新时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "最后更新时间")
    private Date updatedAt;
}
