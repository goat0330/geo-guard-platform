/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskZoneService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.handle.props.HandleProcessProps;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendBatchService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.IEvacuationPlanGenerateService;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskHandleProcessChainNodeTest {

    @Test
    void handleStatusCompleteRateUsesRatioWithTwoDecimals() {
        assertThat(DzTaskHandleServiceImpl.formatCompleteRate(2, 3)).isEqualByComparingTo(new BigDecimal("0.67"));
        assertThat(DzTaskHandleServiceImpl.formatCompleteRate(0, 0)).isEqualByComparingTo(new BigDecimal("0.0"));
    }

    @Test
    void handleArchiveNodeUsesEmergencyBatchNodeAsParent() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskProcessChainNode emergencyBatchNode = new DzTaskProcessChainNode();
        emergencyBatchNode.setId(900L);
        emergencyBatchNode.setChainId("TASK-ROOT");
        when(chainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        )).thenReturn(emergencyBatchNode);
        DzTaskHandleServiceImpl service = service(chainNodeService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setResponsiblePerson("负责人");
        Date now = new Date();

        service.recordHandleArchiveProcessNode(handle, 100L, "测试", now);

        verify(chainNodeService).recordBizNode(
            eq("TASK-ROOT"),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(100L),
            eq("测试"),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode()),
            eq(900L),
            isNull(),
            eq(TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED.getCode()),
            eq(TaskProcessStageTypeEnum.ARCHIVE.getCode())
        );
    }

    @Test
    void handleArchiveNodeUsesLegacyEmergencyBatchLinkNameAsParent() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskProcessChainNode emergencyBatchNode = new DzTaskProcessChainNode();
        emergencyBatchNode.setId(901L);
        emergencyBatchNode.setChainId("TASK-ROOT");
        when(chainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        )).thenReturn(null);
        when(chainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            "批量下发处置任务",
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        )).thenReturn(emergencyBatchNode);
        DzTaskHandleServiceImpl service = service(chainNodeService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        Date now = new Date();

        service.recordHandleArchiveProcessNode(handle, 100L, "测试", now);

        verify(chainNodeService).recordBizNode(
            eq("TASK-ROOT"),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(100L),
            eq("测试"),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode()),
            eq(901L),
            isNull(),
            eq(TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED.getCode()),
            eq(TaskProcessStageTypeEnum.ARCHIVE.getCode())
        );
    }

    @Test
    void handleArchiveNodeThrowsWhenEmergencyBatchNodeMissing() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskHandleServiceImpl service = service(chainNodeService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);

        assertThatThrownBy(() -> service.recordHandleArchiveProcessNode(handle, 100L, "测试", new Date()))
            .isInstanceOf(org.dromara.common.core.exception.ServiceException.class);
    }

    @Test
    void handleArchiveValidationRequiresAllBoundTasksFeedbacked() {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistList task = new DzTaskDistList();
        task.setId(1001L);
        task.setStatus(DzTaskDistList.STATUS_CLOSED);
        when(taskDistListMapper.selectList(any())).thenReturn(List.of(task));
        DzTaskHandleServiceImpl service = service(mock(IDzTaskProcessChainNodeService.class), taskDistListMapper);

        assertThatThrownBy(() -> service.validateHandleArchiveTasksFeedbacked(66L))
            .isInstanceOf(org.dromara.common.core.exception.ServiceException.class)
            .hasMessageContaining("未反馈任务ID：1001");
    }

    @Test
    void handleArchiveValidationPassesWhenNoUnfeedbackedTaskExists() {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        when(taskDistListMapper.selectList(any())).thenReturn(List.of());
        DzTaskHandleServiceImpl service = service(mock(IDzTaskProcessChainNodeService.class), taskDistListMapper);

        assertThatCode(() -> service.validateHandleArchiveTasksFeedbacked(66L))
            .doesNotThrowAnyException();
    }

    @Test
    void emergencyFeedbackAutoArchivesHandleWhenAllTasksFeedbackedAndNoSingleDefenseNode() {
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzSingleDefProgressService singleDefProgressService = mock(IDzSingleDefProgressService.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleServiceImpl service = service(handleMapper, chainNodeService, taskDistListMapper,
            singleDefProgressService, notifyService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setResponsiblePerson("负责人");
        handle.setHandleProcess(HandleProcessEnum.RESPONSE_EXECUTION.getCode());
        DzTaskProcessChainNode batchNode = new DzTaskProcessChainNode();
        batchNode.setId(900L);
        batchNode.setChainId("TASK-ROOT");

        when(handleMapper.selectById(66L)).thenReturn(handle);
        when(chainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L)).thenReturn("HANDLE-66");
        when(chainNodeService.queryChainByChainId("HANDLE-66")).thenReturn(List.of());
        when(taskDistListMapper.selectCount(any())).thenReturn(0L);
        when(chainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        )).thenReturn(batchNode);

        assertThat(service.tryAutoArchiveAfterEmergencyTaskFeedback(66L, 100L, "测试")).isTrue();

        verify(handleMapper).updateById(argThat((DzTaskHandle updated) -> updated != null
            && Long.valueOf(66L).equals(updated.getId())
            && HandleProcessEnum.CLOSED_ARCHIVED.getCode().equals(updated.getHandleProcess())));
        verify(singleDefProgressService).recordEnded(eq(66L), any(Date.class));
        verify(chainNodeService).recordBizNode(
            eq("TASK-ROOT"),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(100L),
            eq("测试"),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode()),
            eq(900L),
            isNull(),
            eq(TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED.getCode()),
            eq(TaskProcessStageTypeEnum.ARCHIVE.getCode())
        );
        verify(notifyService).notifyChangedAfterCommit(
            "task_handle_process",
            "handle_auto_archived_after_emergency_feedback",
            "dz_task_handle",
            66L,
            null
        );
    }

    @Test
    void emergencyFeedbackDoesNotAutoArchiveWhenSingleDefenseNodeExists() {
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzSingleDefProgressService singleDefProgressService = mock(IDzSingleDefProgressService.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleServiceImpl service = service(handleMapper, chainNodeService, taskDistListMapper,
            singleDefProgressService, notifyService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setHandleProcess(HandleProcessEnum.RESPONSE_EXECUTION.getCode());
        TaskProcessChainNodeVo singleDefNode = new TaskProcessChainNodeVo();
        singleDefNode.setNodeCategory(TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START.getCode());
        singleDefNode.setLinkName(TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName());

        when(handleMapper.selectById(66L)).thenReturn(handle);
        when(chainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L)).thenReturn("HANDLE-66");
        when(chainNodeService.queryChainByChainId("HANDLE-66")).thenReturn(List.of(singleDefNode));

        assertThat(service.tryAutoArchiveAfterEmergencyTaskFeedback(66L, 100L, "测试")).isFalse();

        verify(taskDistListMapper, never()).selectCount(any());
        verify(handleMapper, never()).updateById(any(DzTaskHandle.class));
        verify(singleDefProgressService, never()).recordEnded(eq(66L), any(Date.class));
    }

    @Test
    void emergencyFeedbackDoesNotAutoArchiveWhenUnfeedbackedTaskStillExists() {
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzSingleDefProgressService singleDefProgressService = mock(IDzSingleDefProgressService.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleServiceImpl service = service(handleMapper, chainNodeService, taskDistListMapper,
            singleDefProgressService, notifyService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setHandleProcess(HandleProcessEnum.RESPONSE_EXECUTION.getCode());

        when(handleMapper.selectById(66L)).thenReturn(handle);
        when(chainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L)).thenReturn("HANDLE-66");
        when(chainNodeService.queryChainByChainId("HANDLE-66")).thenReturn(List.of());
        when(taskDistListMapper.selectCount(any())).thenReturn(1L);

        assertThat(service.tryAutoArchiveAfterEmergencyTaskFeedback(66L, 100L, "测试")).isFalse();

        verify(handleMapper, never()).updateById(any(DzTaskHandle.class));
        verify(singleDefProgressService, never()).recordEnded(eq(66L), any(Date.class));
    }

    @Test
    void emergencyFeedbackDoesNotAutoArchiveWhenHandleAlreadyArchived() {
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzSingleDefProgressService singleDefProgressService = mock(IDzSingleDefProgressService.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleServiceImpl service = service(handleMapper, chainNodeService, taskDistListMapper,
            singleDefProgressService, notifyService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setHandleProcess(HandleProcessEnum.CLOSED_ARCHIVED.getCode());

        when(handleMapper.selectById(66L)).thenReturn(handle);

        assertThat(service.tryAutoArchiveAfterEmergencyTaskFeedback(66L, 100L, "测试")).isFalse();

        verify(chainNodeService, never()).resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L);
        verify(handleMapper, never()).updateById(any(DzTaskHandle.class));
    }

    @Test
    void emergencyFeedbackDoesNotAutoArchiveWhenArchiveNodeAlreadyExists() {
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzSingleDefProgressService singleDefProgressService = mock(IDzSingleDefProgressService.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleServiceImpl service = service(handleMapper, chainNodeService, taskDistListMapper,
            singleDefProgressService, notifyService);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setHandleProcess(HandleProcessEnum.RESPONSE_EXECUTION.getCode());
        DzTaskProcessChainNode archiveNode = new DzTaskProcessChainNode();
        archiveNode.setId(901L);

        when(handleMapper.selectById(66L)).thenReturn(handle);
        when(chainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(),
            null
        )).thenReturn(archiveNode);

        assertThat(service.tryAutoArchiveAfterEmergencyTaskFeedback(66L, 100L, "测试")).isFalse();

        verify(chainNodeService, never()).resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L);
        verify(handleMapper, never()).updateById(any(DzTaskHandle.class));
        verify(singleDefProgressService, never()).recordEnded(eq(66L), any(Date.class));
    }

    @Test
    void sceneRecordWithoutTaskIdStartsNewHandleChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskHandleServiceImpl service = service(chainNodeService);
        DzTaskHandleSceneRecordReq req = new DzTaskHandleSceneRecordReq();
        req.setHandleId(66L);
        req.setReporter("上报人");
        when(chainNodeService.generateChainId()).thenReturn("20260629-001");

        service.recordSceneRecordHandleProcessNode(req, null, new Date());

        InOrder inOrder = inOrder(chainNodeService);
        inOrder.verify(chainNodeService).resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L);
        inOrder.verify(chainNodeService).generateChainId();
        inOrder.verify(chainNodeService).recordBizNode(
            eq("20260629-001"),
            eq(TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            isNull(),
            eq("上报人"),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode())
        );
        inOrder.verify(chainNodeService).recordBizNode(
            eq("20260629-001"),
            eq(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            isNull(),
            eq("上报人"),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode())
        );
    }

    private DzTaskHandleServiceImpl service(IDzTaskProcessChainNodeService chainNodeService) {
        return service(chainNodeService, mock(DzTaskDistListMapper.class));
    }

    private DzTaskHandleServiceImpl service(IDzTaskProcessChainNodeService chainNodeService,
                                            DzTaskDistListMapper taskDistListMapper) {
        return service(
            mock(DzTaskHandleMapper.class),
            chainNodeService,
            taskDistListMapper,
            mock(IDzSingleDefProgressService.class),
            mock(AiHostingOverviewNotifyService.class)
        );
    }

    private DzTaskHandleServiceImpl service(DzTaskHandleMapper handleMapper,
                                            IDzTaskProcessChainNodeService chainNodeService,
                                            DzTaskDistListMapper taskDistListMapper,
                                            IDzSingleDefProgressService singleDefProgressService,
                                            AiHostingOverviewNotifyService notifyService) {
        return new DzTaskHandleServiceImpl(
            handleMapper,
            mock(DifyAgentClient.class),
            mock(DzTaskHandleSceneRecordMapper.class),
            mock(DzTaskHandleApprovalMapper.class),
            taskDistListMapper,
            mock(IHazardPointService.class),
            mock(IRiskZoneService.class),
            mock(IDzTaskDistListService.class),
            mock(IEvacuationSmsSendService.class),
            mock(ISlopeUnitService.class),
            mock(IDzTaskHandleDetailContentService.class),
            mock(IDzTaskHandleSceneRecordService.class),
            mock(IEvacuationPlanGenerateService.class),
            mock(IReceiveService.class),
            mock(IGeologyInfoService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(IAdRegionService.class),
            mock(IDzUserAdRegionService.class),
            mock(IDzDefRespPlanService.class),
            singleDefProgressService,
            mock(IDzSmsSendBatchService.class),
            notifyService,
            mock(DzDefRespPlanMapper.class),
            mock(DataHouseMapper.class),
            mock(DataPersonMapper.class),
            mock(DisasterDetailedAddressResolver.class),
            mock(HandleProcessProps.class),
            mock(DzTaskHandleSceneSupport.class),
            mock(DzTaskHandlePermissionSupport.class),
            mock(DzTaskHandleRiskOverviewSupport.class),
            mock(DzTaskHandleAiReportSupport.class),
            chainNodeService
        );
    }
}
