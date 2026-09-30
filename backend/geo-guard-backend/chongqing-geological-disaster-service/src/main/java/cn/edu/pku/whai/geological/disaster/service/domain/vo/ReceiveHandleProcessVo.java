/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 处置流程回溯聚合视图。
 */
@Data
public class ReceiveHandleProcessVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处置主键。
     */
    private Long handleId;

    /**
     * 1. 工作流程。
     */
    private WorkflowInfo workflow;

    /**
     * 2. 触发条件。
     */
    private TriggerConditionInfo triggerCondition;

    /**
     * 3. 调查上报。
     */
    private InvestigationInfo investigation;

    /**
     * 4. 灾险情专家会商。
     */
    private ExpertConsultationInfo expertConsultation;

    /**
     * 5. 启动防御响应。
     */
    private DefenseResponseInfo defenseResponse;

    /**
     * 6. 防御响应的专家审核。
     */
    private DefenseReviewInfo defenseReview;

    /**
     * 7. 行政领导确认后的任务派发。
     */
    private TaskDispatchInfo taskDispatch;

    /**
     * 8. 响应处置。
     */
    private ResponseExecutionInfo responseExecution;

    /**
     * 9. 结束响应并归档。
     */
    private ArchiveInfo archive;

    @Data
    public static class WorkflowInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件发生时间。
         */
        private Date occurrenceTime;

        /**
         * 处置事件名称，通常为地点加灾险类型。
         */
        private String name;

        /**
         * 处置事件编号。
         */
        private String code;
    }

    @Data
    public static class TriggerConditionInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件所在斜坡单元 ID。
         */
        private String slopeUnitId;

        /**
         * 该斜坡近三天降雨数据。
         */
        private List<RainfallItem> recentRainfalls;

        /**
         * 该斜坡近三天风险等级。
         */
        private List<RiskLevelItem> recentRiskLevels;

        /**
         * 该斜坡近三天报灾事件。
         */
        private List<DisasterReportItem> recentDisasterReports;
    }

    @Data
    public static class InvestigationInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件的协管员。
         */
        private String assistantManager;

        /**
         * 现场应急调查填写结束时间。
         */
        private Date investigationFinishTime;

        /**
         * 现场应急调查生成报告时间。
         */
        private Date initialReportGenerateTime;

        /**
         * 初版应急调查报告内容。
         */
        private String initialReportContent;
    }

    @Data
    public static class ExpertConsultationInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件当天的值班员。
         */
        private String dutyOfficer;

        /**
         * 应急调查报告会商专家姓名列表。
         */
        private List<String> expertNames;

        /**
         * 应急调查报告会商时间。
         */
        private Date consultationTime;

        /**
         * 应急调查报告会商专家意见。
         */
        private String expertOpinion;

        /**
         * 终版应急调查报告内容。
         */
        private String finalReportContent;
    }

    @Data
    public static class DefenseResponseInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件灾情等级。
         */
        private String eventLevel;

        /**
         * 单点防御响应启动时间。
         */
        private Date responseStartTime;

        /**
         * 单点防御响应等级。
         */
        private String responseLevel;

        /**
         * 初版处置方案内容。
         */
        private String initialPlanContent;
    }

    @Data
    public static class DefenseReviewInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 处置事件当天的值班员。
         */
        private String dutyOfficer;

        /**
         * 处置方案会商专家姓名列表。
         */
        private List<String> expertNames;

        /**
         * 处置方案会商时间。
         */
        private Date consultationTime;

        /**
         * 处置方案会商专家意见。
         */
        private String expertOpinion;

        /**
         * 终版处置方案内容。
         */
        private String finalPlanContent;
    }

    @Data
    public static class TaskDispatchInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 审批角色职称和姓名列表。
         */
        private List<String> approvers;

        /**
         * 任务派发时间。
         */
        private Date dispatchTime;

        /**
         * 任务内容对应执行角色或执行人列表。
         */
        private List<TaskExecutorItem> taskExecutors;
    }

    @Data
    public static class ResponseExecutionInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 各类任务关闭时间列表。
         */
        private List<TaskCloseItem> taskCloseTimes;

        /**
         * 所有任务全部关闭时间。
         */
        private Date allTasksClosedTime;
    }

    @Data
    public static class ArchiveInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 点击结束响应的时间。
         */
        private Date finishResponseTime;
    }

    @Data
    public static class RainfallItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 降雨统计日期。
         */
        private String statDate;

        /**
         * 统计日最新一小时雨量。
         */
        private Double hourlyRainfall;

        /**
         * 统计日日累计雨量。
         */
        private Double dailyCumulativeRainfall;
    }

    @Data
    public static class RiskLevelItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 风险评估时间。
         */
        private Date time;

        /**
         * 风险等级编码。
         */
        private Integer riskLevel;

        /**
         * 风险等级名称。
         */
        private String riskLevelName;
    }

    @Data
    public static class DisasterReportItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 报灾事件 ID。
         */
        private Long reportId;

        /**
         * 报灾地点名称。
         */
        private String locationName;

        /**
         * 报灾时间。
         */
        private Date time;

        /**
         * 报灾人。
         */
        private String reporter;

        /**
         * AI 识图标签，包含灾险类型和 AI 评估等级。
         */
        private String aiTag;
    }

    @Data
    public static class TaskExecutorItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 任务内容或任务名称。
         */
        private String taskName;

        /**
         * 任务执行角色或执行人。
         */
        private String executor;
    }

    @Data
    public static class TaskCloseItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 任务内容或任务名称。
         */
        private String taskName;

        /**
         * 任务关闭时间。
         */
        private Date closedTime;
    }
}
