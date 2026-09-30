/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopeActionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainFilterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainScopeActionResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainPreviewResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistSmsPreviewItemVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListAddMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListRemarkMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespStartSmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentWarningRelationService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskChainPushScopeDelegateTest {

    @Test
    void previewSmsByChainScopeUsesIntersectionAndBuildsSummary() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(baseMapper, chainNodeService, chainSummaryService));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setChainIds(List.of("CHAIN-A", "CHAIN-B"));
        TaskProcessChainFilterBo filterBo = new TaskProcessChainFilterBo();
        filterBo.setRootChainClosed(0);
        bo.setLatestProcessNodeFilter(filterBo);

        when(chainSummaryService.queryChainIdsByFilter(filterBo)).thenReturn(Set.of("CHAIN-B", "CHAIN-C"));
        when(chainNodeService.queryRelatedTaskIdsByChainIds(Set.of("CHAIN-B"))).thenReturn(List.of(1L, 1L, 2L, 3L));
        when(baseMapper.selectBatchIds(any())).thenReturn(List.of(
            task(1L, DzTaskDistList.STATUS_UNPUSHED, 0),
            task(2L, DzTaskDistList.STATUS_UNINSPECTED, 0),
            task(3L, DzTaskDistList.STATUS_UNPUSHED, 0)
        ));
        doNothing().when(service).validateTaskPushAccess();
        doAnswer(invocation -> {
            DzTaskDistList task = invocation.getArgument(0);
            if (Long.valueOf(3L).equals(task.getId())) {
                throw new ServiceException("no permission");
            }
            return null;
        }).when(service).validatePushTaskAccess(any(DzTaskDistList.class));
        doReturn(List.of(new TaskDistSmsPreviewItemVo())).when(service).previewSms(any());

        TaskDistChainPreviewResultVo result = delegate.previewSmsByChainScope(bo);

        assertEquals("PARTIAL_SUCCESS", result.getResultCode());
        assertEquals(2, result.getSummary().getInputChainIdCount());
        assertEquals(2, result.getSummary().getFilteredChainIdCount());
        assertEquals(1, result.getSummary().getFinalChainIdCount());
        assertEquals(4, result.getSummary().getExpandedTaskCount());
        assertEquals(2, result.getSummary().getStatusOneTaskCount());
        assertEquals(3, result.getSummary().getDeduplicatedTaskCount());
        assertEquals(1, result.getSummary().getFinalMatchedTaskCount());
        assertEquals(1, result.getSummary().getDeniedTaskCount());
        assertEquals(1, result.getPreviewItems().size());
    }

    @Test
    void previewSmsByChainScopeReturnsNoChainMatchWhenIntersectionEmpty() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(mock(DzTaskDistListMapper.class), chainNodeService, chainSummaryService));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setChainIds(List.of("CHAIN-A"));
        TaskProcessChainFilterBo filterBo = new TaskProcessChainFilterBo();
        filterBo.setRootChainClosed(1);
        bo.setLatestProcessNodeFilter(filterBo);

        when(chainSummaryService.queryChainIdsByFilter(filterBo)).thenReturn(Set.of("CHAIN-B"));
        doNothing().when(service).validateTaskPushAccess();

        TaskDistChainPreviewResultVo result = delegate.previewSmsByChainScope(bo);

        assertEquals("NO_CHAIN_MATCH", result.getResultCode());
        assertTrue(result.getPreviewItems().isEmpty());
        assertEquals(0, result.getSummary().getFinalChainIdCount());
    }

    @Test
    void previewSmsByChainScopeMapsRootHandleIdToLatestProcessNodeFilter() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(mock(DzTaskDistListMapper.class), chainNodeService, chainSummaryService));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setHandleId(66L);
        doNothing().when(service).validateTaskPushAccess();
        when(chainSummaryService.queryChainIdsByFilter(any(TaskProcessChainFilterBo.class))).thenReturn(Set.of());

        TaskDistChainPreviewResultVo result = delegate.previewSmsByChainScope(bo);

        assertEquals("NO_CHAIN_MATCH", result.getResultCode());
        verify(chainSummaryService).queryChainIdsByFilter(any(TaskProcessChainFilterBo.class));
    }

    @Test
    void previewSmsCompatibleAcceptsTopLevelPilotAreaAndCreateDateFilter() {
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(
            mock(DzTaskDistListMapper.class),
            mock(IDzTaskProcessChainNodeService.class),
            chainSummaryService
        ));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);
        Date createDate = new Date(1782950400000L);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setPilotArea1(1);
        bo.setCreateDate(createDate);
        when(chainSummaryService.queryChainIdsByFilter(any(TaskProcessChainFilterBo.class))).thenReturn(Set.of());

        List<TaskDistSmsPreviewItemVo> result = delegate.previewSmsCompatible(bo);

        assertTrue(result.isEmpty());
        org.mockito.ArgumentCaptor<TaskProcessChainFilterBo> captor =
            org.mockito.ArgumentCaptor.forClass(TaskProcessChainFilterBo.class);
        verify(chainSummaryService).queryChainIdsByFilter(captor.capture());
        assertEquals(1, captor.getValue().getPilotArea1());
        assertEquals(createDate, captor.getValue().getCreateDate());
    }

    @Test
    void previewSmsCompatibleAcceptsTopLevelLatestProcessNodeFields() {
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(
            mock(DzTaskDistListMapper.class),
            mock(IDzTaskProcessChainNodeService.class),
            chainSummaryService
        ));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setRootSourceType(6);
        bo.setCurrentStatusList(List.of(1, 3));
        bo.setRiskLevel(4);
        when(chainSummaryService.queryChainIdsByFilter(any(TaskProcessChainFilterBo.class))).thenReturn(Set.of());

        List<TaskDistSmsPreviewItemVo> result = delegate.previewSmsCompatible(bo);

        assertTrue(result.isEmpty());
        org.mockito.ArgumentCaptor<TaskProcessChainFilterBo> captor =
            org.mockito.ArgumentCaptor.forClass(TaskProcessChainFilterBo.class);
        verify(chainSummaryService).queryChainIdsByFilter(captor.capture());
        assertEquals(6, captor.getValue().getRootSourceType());
        assertEquals(List.of(1, 3), captor.getValue().getCurrentStatusList());
        assertEquals(4, captor.getValue().getRiskLevel());
    }

    @Test
    void previewSmsCompatiblePrefersNestedFilterAndUsesTopLevelAsFallback() {
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzTaskDistListServiceImpl service = spy(service(
            mock(DzTaskDistListMapper.class),
            mock(IDzTaskProcessChainNodeService.class),
            chainSummaryService
        ));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);
        Date createDate = new Date(1782950400000L);

        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        bo.setRootSourceType(6);
        bo.setCreateDate(createDate);
        TaskProcessChainFilterBo filterBo = new TaskProcessChainFilterBo();
        filterBo.setRootSourceType(9);
        filterBo.setCurrentStatusList(List.of(2));
        bo.setLatestProcessNodeFilter(filterBo);
        when(chainSummaryService.queryChainIdsByFilter(any(TaskProcessChainFilterBo.class))).thenReturn(Set.of());

        List<TaskDistSmsPreviewItemVo> result = delegate.previewSmsCompatible(bo);

        assertTrue(result.isEmpty());
        org.mockito.ArgumentCaptor<TaskProcessChainFilterBo> captor =
            org.mockito.ArgumentCaptor.forClass(TaskProcessChainFilterBo.class);
        verify(chainSummaryService).queryChainIdsByFilter(captor.capture());
        assertEquals(9, captor.getValue().getRootSourceType());
        assertEquals(List.of(2), captor.getValue().getCurrentStatusList());
        assertEquals(createDate, captor.getValue().getCreateDate());
    }

    @Test
    void previewSmsCompatibleStillRejectsEmptySelectors() {
        DzTaskDistListServiceImpl service = spy(service(
            mock(DzTaskDistListMapper.class),
            mock(IDzTaskProcessChainNodeService.class),
            mock(IDzTaskProcessChainSummaryService.class)
        ));
        DzTaskChainPushScopeDelegate delegate = new DzTaskChainPushScopeDelegate(service);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> delegate.previewSmsCompatible(new TaskDistChainScopePushBo()));

        assertEquals("taskId、taskIds、chainIds和latestProcessNodeFilter不能同时为空", ex.getMessage());
    }

    @Test
    void deleteWithValidByIdsSoftDeletesAndSyncsAppClosedStatus() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        IAppTaskService appTaskService = mock(IAppTaskService.class);
        DzTaskDistListServiceImpl service = spy(service(
            baseMapper,
            mock(IDzTaskProcessChainNodeService.class),
            mock(IDzTaskProcessChainSummaryService.class),
            appTaskService
        ));
        DzTaskDistList task = task(10L, DzTaskDistList.STATUS_UNINSPECTED, 0);
        when(baseMapper.selectBatchIds(any())).thenReturn(List.of(task));
        when(baseMapper.updateBatchById(anyList())).thenReturn(true);
        doNothing().when(service).validateDutyOfficerTaskAccess(any());

        service.deleteWithValidByIds(List.of(10L), true);

        verify(appTaskService).updateTask(anyList());
        verify(baseMapper).updateBatchById(anyList());
        verify(baseMapper, never()).deleteByIds(any());
    }

    @Test
    void deleteWithValidByIdsDoesNotUpdateLocalWhenAppSyncFails() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        IAppTaskService appTaskService = mock(IAppTaskService.class);
        DzTaskDistListServiceImpl service = spy(service(
            baseMapper,
            mock(IDzTaskProcessChainNodeService.class),
            mock(IDzTaskProcessChainSummaryService.class),
            appTaskService
        ));
        DzTaskDistList task = task(11L, DzTaskDistList.STATUS_UNINSPECTED, 0);
        when(baseMapper.selectBatchIds(any())).thenReturn(List.of(task));
        doNothing().when(service).validateDutyOfficerTaskAccess(any());
        doAnswer(invocation -> {
            throw new RuntimeException("app down");
        }).when(appTaskService).updateTask(anyList());

        assertThrows(RuntimeException.class, () -> service.deleteWithValidByIds(List.of(11L), true));

        verify(baseMapper, never()).updateBatchById(anyList());
        verify(baseMapper, never()).deleteByIds(any());
    }

    @Test
    void deleteByChainScopeDeduplicatesExpandedTaskIds() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskDistListServiceImpl service = spy(service(
            baseMapper,
            chainNodeService,
            mock(IDzTaskProcessChainSummaryService.class),
            mock(IAppTaskService.class)
        ));
        TaskDistChainScopeActionBo bo = new TaskDistChainScopeActionBo();
        bo.setChainIds(List.of("CHAIN-A"));
        when(chainNodeService.queryRelatedTaskIdsByChainIds(Set.of("CHAIN-A"))).thenReturn(List.of(1L, 1L, 2L));
        when(baseMapper.selectBatchIds(any())).thenReturn(List.of(
            task(1L, DzTaskDistList.STATUS_UNPUSHED, 0),
            task(2L, DzTaskDistList.STATUS_UNPUSHED, 0)
        ));
        when(baseMapper.updateBatchById(anyList())).thenReturn(true);
        doNothing().when(service).validateDutyOfficerTaskAccess(any());

        TaskDistChainScopeActionResultVo result = service.deleteByChainScope(bo);

        assertEquals("SUCCESS", result.getResultCode());
        assertEquals(3, result.getExpandedTaskCount());
        assertEquals(2, result.getDeduplicatedTaskCount());
        assertEquals(2, result.getSuccessCount());
        verify(baseMapper, times(2)).updateBatchById(anyList());
    }

    private DzTaskDistList task(Long id, Integer status, Integer delete) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(id);
        task.setStatus(status);
        task.setDelete(delete);
        return task;
    }

    private DzTaskDistListServiceImpl service(DzTaskDistListMapper baseMapper,
                                              IDzTaskProcessChainNodeService chainNodeService,
                                              IDzTaskProcessChainSummaryService chainSummaryService) {
        return service(baseMapper, chainNodeService, chainSummaryService, mock(IAppTaskService.class));
    }

    private DzTaskDistListServiceImpl service(DzTaskDistListMapper baseMapper,
                                              IDzTaskProcessChainNodeService chainNodeService,
                                              IDzTaskProcessChainSummaryService chainSummaryService,
                                              IAppTaskService appTaskService) {
        return new DzTaskDistListServiceImpl(
            baseMapper,
            mock(DzTaskDistListAddMapper.class),
            mock(DzTaskDistListRemarkMapper.class),
            mock(DzTaskDistListHistoryMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(ISysUserService.class),
            mock(DzRiskAssessmentMapper.class),
            mock(DzReportDisasterMapper.class),
            appTaskService,
            mock(AppTaskProps.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(IDzUserAdRegionService.class),
            mock(IDzTaskHandleSceneRecordService.class),
            mock(IDzTaskHandleDetailContentService.class),
            mock(DzDefRespPlanMapper.class),
            mock(AdRegionMapper.class),
            mock(TransactionTemplate.class),
            mock(ITaskSmsContentService.class),
            mock(SmsSendService.class),
            mock(IDzDefRespStartSmsConfigService.class),
            mock(SysRoleMapper.class),
            mock(SysUserMapper.class),
            mock(SysUserRoleMapper.class),
            mock(StringRedisTemplate.class),
            chainNodeService,
            chainSummaryService,
            mock(IDzRiskAssessmentWarningRelationService.class),
            mock(DifyAgentClient.class)
        );
    }
}
