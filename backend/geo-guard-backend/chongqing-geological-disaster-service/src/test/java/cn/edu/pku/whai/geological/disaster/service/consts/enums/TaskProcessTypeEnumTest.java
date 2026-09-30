/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class TaskProcessTypeEnumTest {

    @Test
    void bizTypeResolvesCodeAndLegacyCode() {
        assertThat(TaskProcessBizTypeEnum.fromLegacy("TASK")).isEqualTo(TaskProcessBizTypeEnum.TASK);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("REPORT")).isEqualTo(TaskProcessBizTypeEnum.REPORT);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("HANDLE")).isEqualTo(TaskProcessBizTypeEnum.HANDLE);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("DEF_RESP")).isEqualTo(TaskProcessBizTypeEnum.DEF_RESP);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("ALARM")).isEqualTo(TaskProcessBizTypeEnum.ALARM);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("MONITOR_WARNING")).isEqualTo(TaskProcessBizTypeEnum.MONITOR_WARNING);
        assertThat(TaskProcessBizTypeEnum.fromLegacy("999")).isEqualTo(TaskProcessBizTypeEnum.UNKNOWN);

        assertThat(TaskProcessBizTypeEnum.legacyCode(TaskProcessBizTypeEnum.TASK.getCode())).isEqualTo("TASK");
        assertThat(TaskProcessBizTypeEnum.label(TaskProcessBizTypeEnum.REPORT.getCode())).isEqualTo("报灾/报告");
    }

    @Test
    void sourceTypeMapsTaskTableSourceIntoChainSource() {
        assertThat(TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode()).isEqualTo(1);
        assertThat(TaskProcessSourceTypeEnum.REPORT.getCode()).isEqualTo(2);
        assertThat(TaskProcessSourceTypeEnum.ALARM.getCode()).isEqualTo(3);
        assertThat(TaskProcessSourceTypeEnum.MONITOR_WARNING.getCode()).isEqualTo(4);
        assertThat(TaskProcessSourceTypeEnum.EVAL.getCode()).isEqualTo(5);
        assertThat(TaskProcessSourceTypeEnum.MANUAL.getCode()).isEqualTo(6);
        assertThat(TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode()).isEqualTo(7);
        assertThat(TaskProcessSourceTypeEnum.HANDLE.getCode()).isEqualTo(8);
        assertThat(TaskProcessSourceTypeEnum.DEF_RESP.getCode()).isEqualTo(9);
        assertThat(TaskProcessSourceTypeEnum.EMERGENCY.getCode()).isEqualTo(10);

        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_MANUAL))
            .isEqualTo(TaskProcessSourceTypeEnum.MANUAL);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_EVAL))
            .isEqualTo(TaskProcessSourceTypeEnum.EVAL);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_REPORT))
            .isEqualTo(TaskProcessSourceTypeEnum.REPORT);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP))
            .isEqualTo(TaskProcessSourceTypeEnum.DEF_RESP);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY))
            .isEqualTo(TaskProcessSourceTypeEnum.EMERGENCY);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING))
            .isEqualTo(TaskProcessSourceTypeEnum.MONITOR_WARNING);
        assertThat(TaskProcessSourceTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE))
            .isEqualTo(TaskProcessSourceTypeEnum.TECH_ASSISTANCE);
    }

    @Test
    void sourceTypeSeparatesReportSourceFromTaskSource() {
        assertThat(TaskProcessSourceTypeEnum.fromReportSourceType(1)).isEqualTo(TaskProcessSourceTypeEnum.TASK_FEEDBACK);
        assertThat(TaskProcessSourceTypeEnum.fromReportSourceType(2)).isEqualTo(TaskProcessSourceTypeEnum.REPORT);

        assertThat(TaskProcessSourceTypeEnum.fromLegacy("1", TaskProcessBizTypeEnum.TASK.getCode()))
            .isEqualTo(TaskProcessSourceTypeEnum.TASK_FEEDBACK);
        assertThat(TaskProcessSourceTypeEnum.fromLegacy("1", TaskProcessBizTypeEnum.REPORT.getCode()))
            .isEqualTo(TaskProcessSourceTypeEnum.TASK_FEEDBACK);
    }

    @Test
    void nodeCategoryResolvesCommonLinkNames() {
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务核查中"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_INSPECTING);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("现场处置核查中"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_INSPECTING);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务开始核查"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_INSPECTING);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务反馈"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_FEEDBACK);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("日常巡查任务反馈"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_FEEDBACK);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("防御响应反馈"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_FEEDBACK);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("申请技术协查"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TECH_ASSIST_APPLY);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("开启处置管理"))
            .isEqualTo(TaskProcessNodeCategoryEnum.HANDLE_START);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("开启单点防御响应"))
            .isEqualTo(TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("批量下发处置任务"))
            .isEqualTo(TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("批量下发防御响应任务"))
            .isEqualTo(TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("群众报灾报告"))
            .isEqualTo(TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务反馈报告"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("灾险情关闭"))
            .isEqualTo(TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("根据预警信息开启区域防御响应"))
            .isEqualTo(TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("开启区域防御响应"))
            .isEqualTo(TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("启动区域防御响应"))
            .isEqualTo(TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务关闭"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_CLOSE);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务过期"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_OVERDUE);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务反馈并上报现场处置报告")).isNull();
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("上报现场处置报告")).isNull();
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("任务推送"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_PUSH);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel("下发应急处置任务"))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_PUSH);
        assertThat(TaskProcessNodeCategoryEnum.label(TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode()))
            .isEqualTo("任务反馈");
        assertThat(TaskProcessNodeCategoryEnum.label(11)).isEqualTo("群众报灾报告");
        assertThat(TaskProcessNodeCategoryEnum.label(12)).isEqualTo("任务反馈报告");
        assertThat(TaskProcessNodeCategoryEnum.label(16)).isEqualTo("灾险情关闭");
    }

    @Test
    void stageTypeResolvesByLinkNameAndTaskSource() {
        assertThat(TaskProcessStageTypeEnum.DEF_RESP.getCode()).isZero();
        assertThat(TaskProcessStageTypeEnum.START_TASK.getCode()).isEqualTo(1);
        assertThat(TaskProcessStageTypeEnum.DANGER_VERIFY.getCode()).isEqualTo(2);
        assertThat(TaskProcessStageTypeEnum.EMERGENCY_SURVEY.getCode()).isEqualTo(3);
        assertThat(TaskProcessStageTypeEnum.HANDLE.getCode()).isEqualTo(4);
        assertThat(TaskProcessStageTypeEnum.SINGLE_DEF_RESP.getCode()).isEqualTo(5);
        assertThat(TaskProcessStageTypeEnum.ARCHIVE.getCode()).isEqualTo(6);
        assertThat(TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode()).isEqualTo(10);

        assertThat(TaskProcessStageTypeEnum.fromLinkName("上传预警报告"))
            .isEqualTo(TaskProcessStageTypeEnum.DEF_RESP);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("群众报灾"))
            .isEqualTo(TaskProcessStageTypeEnum.START_TASK);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("监测预警"))
            .isEqualTo(TaskProcessStageTypeEnum.START_TASK);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("群众报灾报告"))
            .isEqualTo(TaskProcessStageTypeEnum.DANGER_VERIFY);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("AI险情核实"))
            .isEqualTo(TaskProcessStageTypeEnum.DANGER_VERIFY);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("任务反馈报告"))
            .isEqualTo(TaskProcessStageTypeEnum.DANGER_VERIFY);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("批量下发防御响应任务"))
            .isEqualTo(TaskProcessStageTypeEnum.START_TASK);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("险情核实任务"))
            .isEqualTo(TaskProcessStageTypeEnum.DANGER_VERIFY);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("申请技术协查"))
            .isEqualTo(TaskProcessStageTypeEnum.EMERGENCY_SURVEY);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("开启处置管理"))
            .isEqualTo(TaskProcessStageTypeEnum.HANDLE);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("任务反馈并上报现场处置报告"))
            .isEqualTo(TaskProcessStageTypeEnum.HANDLE);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("上报现场处置报告"))
            .isEqualTo(TaskProcessStageTypeEnum.HANDLE);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("批量下发处置任务"))
            .isEqualTo(TaskProcessStageTypeEnum.SINGLE_DEF_RESP);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("防御响应结束归档"))
            .isEqualTo(TaskProcessStageTypeEnum.ARCHIVE);
        assertThat(TaskProcessStageTypeEnum.fromLinkName("灾险情关闭"))
            .isEqualTo(TaskProcessStageTypeEnum.ARCHIVE);

        assertThat(TaskProcessStageTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_REPORT))
            .isEqualTo(TaskProcessStageTypeEnum.DANGER_VERIFY);
        assertThat(TaskProcessStageTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE))
            .isEqualTo(TaskProcessStageTypeEnum.EMERGENCY_SURVEY);
        assertThat(TaskProcessStageTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY))
            .isEqualTo(TaskProcessStageTypeEnum.SINGLE_DEF_RESP);
        assertThat(TaskProcessStageTypeEnum.fromTaskSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP))
            .isEqualTo(TaskProcessStageTypeEnum.START_TASK);
        assertThat(TaskProcessStageTypeEnum.fromNode("任务推送", TaskProcessBizTypeEnum.TASK.getCode(),
            TaskProcessSourceTypeEnum.EMERGENCY.getCode())).isEqualTo(TaskProcessStageTypeEnum.SINGLE_DEF_RESP);
    }

    @Test
    void displayStatusResolvesFixedProductMapping() {
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务关闭")).isEqualTo(TaskProcessDisplayStatusEnum.FINISHED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("结束归档")).isEqualTo(TaskProcessDisplayStatusEnum.FINISHED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("防御响应结束归档")).isEqualTo(TaskProcessDisplayStatusEnum.FINISHED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("灾险情关闭")).isEqualTo(TaskProcessDisplayStatusEnum.FINISHED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务过期")).isEqualTo(TaskProcessDisplayStatusEnum.OVERDUE);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务核查中")).isEqualTo(TaskProcessDisplayStatusEnum.EXECUTING);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("现场处置任务执行中")).isEqualTo(TaskProcessDisplayStatusEnum.EXECUTING);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务开始核查")).isEqualTo(TaskProcessDisplayStatusEnum.EXECUTING);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务反馈")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("巡查任务反馈")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("日常巡查任务反馈")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("AI险情核实任务反馈")).isEqualTo(TaskProcessDisplayStatusEnum.FINISHED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务反馈报告")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("上报现场处置报告")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务反馈并上报现场处置报告")).isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("技术协查任务反馈并上报现场处置报告"))
            .isEqualTo(TaskProcessDisplayStatusEnum.FEEDBACKED);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("申请技术协查")).isEqualTo(TaskProcessDisplayStatusEnum.TECH_ASSIST);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("下发技术协查任务")).isEqualTo(TaskProcessDisplayStatusEnum.TECH_ASSIST);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("技术协查任务推送")).isEqualTo(TaskProcessDisplayStatusEnum.TECH_ASSIST);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("任务推送")).isEqualTo(TaskProcessDisplayStatusEnum.FLOWING);
        assertThat(TaskProcessDisplayStatusEnum.fromLinkName("监测预警推送")).isEqualTo(TaskProcessDisplayStatusEnum.FLOWING);
    }

    @Test
    void rootClosedLinkNameResolvesConfirmedTerminalNodesOnly() {
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("灾险情关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("AI险情核实任务反馈")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("结束归档")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("防御响应结束归档")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("群众报灾任务关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("手动添加任务关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("日常巡查任务关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("日常巡查任务过期")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("防御响应任务关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("防御响应任务过期")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("监测预警任务关闭")).isTrue();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("现场处置任务反馈")).isTrue();

        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("任务反馈")).isFalse();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("任务反馈报告")).isFalse();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("日常巡查任务反馈")).isFalse();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("任务核查中")).isFalse();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("任务推送")).isFalse();
        assertThat(TaskProcessChainNodeTextEnum.isRootClosedLinkName("申请技术协查")).isFalse();
    }

    @Test
    void refineTaskLinkNameAlignsTaskScopedTextWithTaskType() {
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务推送", TaskProcessSourceTypeEnum.EVAL.getCode()))
            .isEqualTo("日常巡查任务推送");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务核查中", TaskProcessSourceTypeEnum.EMERGENCY.getCode()))
            .isEqualTo("现场处置任务执行中");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务反馈",
            TaskProcessSourceTypeEnum.REPORT.getCode(), DzTaskDistList.TASK_TYPE_PUBLIC_REPORT))
            .isEqualTo("群众报灾任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务反馈",
            TaskProcessSourceTypeEnum.EVAL.getCode(), DzTaskDistList.TASK_TYPE_INSPECTION))
            .isEqualTo("日常巡查任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName("日常巡查任务反馈"))
            .isEqualTo("任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务反馈",
            TaskProcessSourceTypeEnum.MONITOR_WARNING.getCode(), DzTaskDistList.TASK_TYPE_MONITOR_WARNING))
            .isEqualTo("监测预警任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务反馈",
            TaskProcessSourceTypeEnum.REPORT.getCode()))
            .isEqualTo("AI险情核实任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("险情核实任务", TaskProcessSourceTypeEnum.REPORT.getCode()))
            .isEqualTo("AI险情核实");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("下发技术协查任务", TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode()))
            .isEqualTo("下发技术协查任务");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName("任务反馈",
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION))
            .isEqualTo("技术协查任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskFeedbackReportLinkName(
            TaskProcessSourceTypeEnum.EVAL.getCode(), DzTaskDistList.TASK_TYPE_INSPECTION))
            .isEqualTo("日常巡查任务反馈报告");
        assertThat(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName("防御响应任务反馈报告"))
            .isEqualTo("任务反馈报告");
    }

    @Test
    void chainSegmentTypeMarksDisplayableSegments() {
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.MAIN_CHAIN.getCode())).isTrue();
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.DEF_RESP_TASK_MAIN_CHAIN.getCode())).isTrue();
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.HANDLE_MAIN_CHAIN.getCode())).isTrue();
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.SOURCE_PRE_CHAIN.getCode())).isFalse();
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode())).isFalse();
        assertThat(TaskProcessChainSegmentTypeEnum.displayable(TaskProcessChainSegmentTypeEnum.OTHER_DERIVED_CHILD_CHAIN.getCode())).isFalse();
        assertThat(TaskProcessChainSegmentTypeEnum.label(3)).isEqualTo("防御响应任务主链");
    }
}
