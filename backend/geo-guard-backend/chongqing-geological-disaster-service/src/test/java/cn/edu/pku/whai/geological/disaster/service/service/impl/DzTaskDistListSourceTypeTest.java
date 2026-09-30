/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessCapabilityVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskDistListSourceTypeTest {

    @Test
    void manualSourceTypeUsesZeroEnum() {
        assertThat(DzTaskDistList.SOURCE_TYPE_MANUAL).isZero();
        assertThat(DzTaskDistListServiceImpl.resolveTaskSource(0)).isEqualTo("手动添加");
    }

    @Test
    void aiProcessChainResponseDefIdNormalizesTownRegionToCountyRegion() {
        DefRespPlan townPlan = defPlan(10L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.TOWN.getCode(), 20L);
        DefRespPlan countyPlan = defPlan(20L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.COUNTY.getCode(), null);

        Long result = DzTaskDistListServiceImpl.resolveAiProcessChainResponseDefId(10L, townPlan, countyPlan);

        assertThat(result).isEqualTo(20L);
    }

    @Test
    void aiProcessChainResponseDefIdKeepsCountyRegion() {
        DefRespPlan countyPlan = defPlan(20L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.COUNTY.getCode(), null);

        Long result = DzTaskDistListServiceImpl.resolveAiProcessChainResponseDefId(20L, countyPlan, null);

        assertThat(result).isEqualTo(20L);
    }

    @Test
    void aiProcessChainResponseDefIdKeepsSingleDefResp() {
        DefRespPlan singlePlan = defPlan(30L, DefRespPlanTypeEnum.SINGLE.getCode(), null, 20L);
        DefRespPlan countyPlan = defPlan(20L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.COUNTY.getCode(), null);

        Long result = DzTaskDistListServiceImpl.resolveAiProcessChainResponseDefId(30L, singlePlan, countyPlan);

        assertThat(result).isEqualTo(30L);
    }

    @Test
    void aiProcessChainResponseDefIdKeepsTownRegionWhenParentMissingOrInvalid() {
        DefRespPlan townPlan = defPlan(10L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.TOWN.getCode(), 20L);
        DefRespPlan invalidParent = defPlan(20L, DefRespPlanTypeEnum.REGION.getCode(),
            RegionScopeTypeEnum.TOWN.getCode(), null);

        assertThat(DzTaskDistListServiceImpl.resolveAiProcessChainResponseDefId(10L, townPlan, null))
            .isEqualTo(10L);
        assertThat(DzTaskDistListServiceImpl.resolveAiProcessChainResponseDefId(10L, townPlan, invalidParent))
            .isEqualTo(10L);
    }

    @Test
    void processCapabilitiesIgnoreNonTaskNodes() {
        TaskProcessChainNodeVo nonTask = node(null, TaskProcessBizTypeEnum.DEF_RESP.getCode(), 1, null);

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of(nonTask));

        assertThat(result.getCurrentCapabilities()).isEmpty();
        assertThat(result.getNextCapabilities()).isEmpty();
    }

    @Test
    void processCapabilitiesReturnEmptyWhenSavedChainMissing() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of());

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.querySavedProcessChainCapabilities(chainNodeService, 100L);

        assertThat(result.getCurrentCapabilities()).isEmpty();
        assertThat(result.getNextCapabilities()).isEmpty();
        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
        verify(chainNodeService, never()).queryChainByTaskId(100L);
        verify(chainNodeService, never()).queryChainByChainId(null);
    }

    @Test
    void processCapabilitiesUseSavedChainWithoutFallback() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        TaskProcessChainNodeVo latestNode = node(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1,
            TaskProcessSourceTypeEnum.MANUAL.getCode());
        latestNode.setChainId("TASK-100");
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of(100L, latestNode));
        when(chainNodeService.queryChainByChainId("TASK-100")).thenReturn(List.of(
            linkNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1, TaskProcessSourceTypeEnum.MANUAL.getCode(),
                TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName())
        ));

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.querySavedProcessChainCapabilities(chainNodeService, 100L);

        assertThat(result.getCurrentCapabilities()).containsExactly("APP端", "短信系统");
        assertThat(result.getNextCapabilities()).containsExactly("APP端", "地图定位");
        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
        verify(chainNodeService).queryChainByChainId("TASK-100");
        verify(chainNodeService, never()).queryChainByTaskId(100L);
    }

    @Test
    void processCapabilitiesUseTaskPushNodeCapabilitiesOnly() {
        TaskProcessChainNodeVo task = linkNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1,
            TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName());

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of(task));

        assertThat(result.getCurrentCapabilities()).containsExactly("APP端", "短信系统");
        assertThat(result.getNextCapabilities()).containsExactly("APP端", "地图定位");
    }

    @Test
    void processNodeCategoryResolveTaskGenerateAndPushLinks() {
        assertThat(TaskProcessNodeCategoryEnum.fromLabel(TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName()))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_GENERATE);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName()))
            .isEqualTo(TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName()))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_PUSH);
        assertThat(TaskProcessNodeCategoryEnum.fromLabel(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()))
            .isEqualTo(TaskProcessNodeCategoryEnum.TASK_PUSH);
    }

    @Test
    void processCapabilitiesGroupByStage() {
        List<TaskProcessChainNodeVo> chain = List.of(
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), TaskProcessStageTypeEnum.START_TASK.getCode(),
                TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(),
                TaskProcessNodeCategoryEnum.TASK_GENERATE.getCode()),
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), TaskProcessStageTypeEnum.START_TASK.getCode(),
                TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
                TaskProcessNodeCategoryEnum.TASK_PUSH.getCode()),
            categoryNode(100L, TaskProcessBizTypeEnum.REPORT.getCode(), TaskProcessStageTypeEnum.DANGER_VERIFY.getCode(),
                TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(),
                TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())
        );

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(chain);

        assertThat(result.getCurrentCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析", "APP端", "短信系统", "多模态大模型");
        assertThat(result.getNextCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析");
        assertThat(result.getStageCapabilities()).hasSize(2);
        assertThat(result.getStageCapabilities().get(0).getStageTypeLabel()).isEqualTo("起始任务");
        assertThat(result.getStageCapabilities().get(0).getCurrentCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析", "APP端", "短信系统");
        assertThat(result.getStageCapabilities().get(0).getNextCapabilities()).containsExactly("APP端", "地图定位");
        assertThat(result.getStageCapabilities().get(1).getStageTypeLabel()).isEqualTo("险情核实");
        assertThat(result.getStageCapabilities().get(1).getCurrentCapabilities()).containsExactly("多模态大模型");
        assertThat(result.getStageCapabilities().get(1).getNextCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析");
    }

    @Test
    void processCapabilitiesClassifyEmergencyBatchDispatchAsTaskGenerate() {
        TaskProcessChainNodeVo task = categoryNode(null, TaskProcessBizTypeEnum.HANDLE.getCode(),
            TaskProcessStageTypeEnum.SINGLE_DEF_RESP.getCode(), TaskProcessSourceTypeEnum.HANDLE.getCode(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of(task));

        assertThat(result.getCurrentCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析");
        assertThat(result.getNextCapabilities()).containsExactly("APP端", "短信系统");
    }

    @Test
    void processCapabilitiesUseRegionDefRespNodeCapabilities() {
        TaskProcessChainNodeVo task = categoryNode(null, TaskProcessBizTypeEnum.DEF_RESP.getCode(), 0,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of(task));

        assertThat(result.getCurrentCapabilities()).containsExactly("报告解读模型", "大语言模型", "AI会商系统", "短信系统");
        assertThat(result.getNextCapabilities()).isEmpty();
    }

    @Test
    void processCapabilitiesAdvanceAfterInspectingNode() {
        List<TaskProcessChainNodeVo> chain = List.of(
            linkNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1, TaskProcessSourceTypeEnum.MANUAL.getCode(),
                TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()),
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1, TaskProcessSourceTypeEnum.MANUAL.getCode(),
                TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode())
        );

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(chain);

        assertThat(result.getCurrentCapabilities()).containsExactly("APP端", "短信系统", "地图定位");
        assertThat(result.getNextCapabilities()).containsExactly("APP端", "地图定位", "多模态大模型");
    }

    @Test
    void processCapabilitiesAddMultimodalAfterFeedbackReport() {
        List<TaskProcessChainNodeVo> chain = List.of(
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1, TaskProcessSourceTypeEnum.MANUAL.getCode(),
                TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode()),
            categoryNode(100L, TaskProcessBizTypeEnum.REPORT.getCode(), 2, TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(),
                TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())
        );

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(chain);

        assertThat(result.getCurrentCapabilities()).containsExactly("APP端", "地图定位", "多模态大模型");
        assertThat(result.getNextCapabilities()).containsExactly("大语言模型", "风险评价算法", "空间分析");
    }

    @Test
    void processCapabilitiesReturnTechnicalHandleAndSingleDefRespIncrements() {
        List<TaskProcessChainNodeVo> chain = List.of(
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 2, TaskProcessSourceTypeEnum.REPORT.getCode(),
                TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode()),
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 3, TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(),
                TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), TaskProcessNodeCategoryEnum.TECH_ASSIST_APPLY.getCode()),
            categoryNode(null, TaskProcessBizTypeEnum.HANDLE.getCode(), 4, TaskProcessSourceTypeEnum.HANDLE.getCode(),
                TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), TaskProcessNodeCategoryEnum.HANDLE_START.getCode()),
            categoryNode(null, TaskProcessBizTypeEnum.DEF_RESP.getCode(), 5, TaskProcessSourceTypeEnum.EMERGENCY.getCode(),
                TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(), TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START.getCode())
        );

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(chain);

        assertThat(result.getCurrentCapabilities()).containsExactly("APP端", "地图定位", "AI会商系统", "大语言模型",
            "风险评价算法", "空间分析", "路线规划算法", "短信系统");
        assertThat(result.getNextCapabilities()).isEmpty();
    }

    @Test
    void processCapabilitiesWalkNestedChildChains() {
        TaskProcessChainNodeVo parent = node(null, TaskProcessBizTypeEnum.HANDLE.getCode(), 4,
            TaskProcessSourceTypeEnum.HANDLE.getCode());
        parent.setChildChains(List.of(List.of(
            categoryNode(400L, TaskProcessBizTypeEnum.TASK.getCode(), 5, TaskProcessSourceTypeEnum.EMERGENCY.getCode(),
                TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode())
        )));

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of(parent));

        assertThat(result.getCurrentCapabilities()).contains("大语言模型", "风险评价算法", "空间分析");
        assertThat(result.getNextCapabilities()).containsExactly("APP端", "短信系统");
    }

    @Test
    void processCapabilitiesIgnoreUnknownNodeAndTerminalHasNoNext() {
        List<TaskProcessChainNodeVo> chain = List.of(
            linkNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 1, TaskProcessSourceTypeEnum.MANUAL.getCode(), "未知节点"),
            categoryNode(100L, TaskProcessBizTypeEnum.TASK.getCode(), 6, TaskProcessSourceTypeEnum.MANUAL.getCode(),
                TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode())
        );

        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(chain);

        assertThat(result.getCurrentCapabilities()).isEmpty();
        assertThat(result.getNextCapabilities()).isEmpty();
    }

    @Test
    void processCapabilitiesReturnEmptyForEmptyChain() {
        TaskProcessCapabilityVo result = DzTaskDistListServiceImpl.resolveProcessChainCapabilities(List.of());

        assertThat(result.getCurrentCapabilities()).isEmpty();
        assertThat(result.getNextCapabilities()).isEmpty();
    }

    private DefRespPlan defPlan(Long id, Integer type, Integer regionScopeType, Long parentDefId) {
        DefRespPlan plan = new DefRespPlan();
        plan.setId(id);
        plan.setType(type);
        plan.setRegionScopeType(regionScopeType);
        plan.setParentDefId(parentDefId);
        return plan;
    }

    private TaskProcessChainNodeVo node(Long taskId, Integer bizType, Integer stageType, Integer sourceType) {
        TaskProcessChainNodeVo node = new TaskProcessChainNodeVo();
        node.setTaskId(taskId);
        node.setBizType(bizType);
        node.setType(bizType);
        node.setStageType(stageType);
        node.setSourceType(sourceType);
        node.setSourceTypeCode(sourceType);
        node.setSourceTypeLabel(TaskProcessSourceTypeEnum.label(sourceType));
        return node;
    }

    private TaskProcessChainNodeVo linkNode(Long taskId, Integer bizType, Integer stageType, Integer sourceType,
                                            String linkName) {
        TaskProcessChainNodeVo node = node(taskId, bizType, stageType, sourceType);
        node.setLinkName(linkName);
        return node;
    }

    private TaskProcessChainNodeVo categoryNode(Long taskId, Integer bizType, Integer stageType, Integer sourceType,
                                                String linkName, Integer nodeCategory) {
        TaskProcessChainNodeVo node = linkNode(taskId, bizType, stageType, sourceType, linkName);
        node.setNodeCategory(nodeCategory);
        node.setNodeCategoryLabel(TaskProcessNodeCategoryEnum.label(nodeCategory));
        return node;
    }
}
