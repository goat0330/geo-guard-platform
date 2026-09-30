/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.service.impl;

import org.dromara.common.core.service.OssService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionSubmitReq;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListHistory;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class AppTaskServiceImplSubmitChainTest {

    @Test
    void skipsProcessChainNodeWhenTechAssistanceTaskClosed() {
        DzTaskDistList task = new DzTaskDistList();
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        task.setStatus(DzTaskDistList.STATUS_CLOSED);

        assertThat(AppTaskServiceImpl.shouldSkipSubmitTaskChainNode(task)).isTrue();
    }

    @Test
    void keepsProcessChainNodeForOtherClosedTasks() {
        DzTaskDistList task = new DzTaskDistList();
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setStatus(DzTaskDistList.STATUS_CLOSED);

        assertThat(AppTaskServiceImpl.shouldSkipSubmitTaskChainNode(task)).isFalse();
    }

    @Test
    void aiVerifyTaskAllowsCloseFeedbackOrTechAssistance() {
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, false,
            DzTaskDistList.STATUS_CLOSED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, false,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, false,
            DzTaskDistList.STATUS_TECH_ASSISTANCE)).isTrue();
    }

    @Test
    void techAssistanceTaskAllowsOnlyFeedback() {
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE, false,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE, false,
            DzTaskDistList.STATUS_CLOSED)).isFalse();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE, false,
            DzTaskDistList.STATUS_TECH_ASSISTANCE)).isFalse();
    }

    @Test
    void sceneHandleTaskAllowsOnlyInspectingOrFeedback() {
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EMERGENCY, false,
            DzTaskDistList.STATUS_INSPECTING)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EMERGENCY, false,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EMERGENCY, false,
            DzTaskDistList.STATUS_CLOSED)).isFalse();
    }

    @Test
    void manualAndDailyPatrolShareCloseOrFeedbackSubmitRule() {
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_MANUAL, false,
            DzTaskDistList.STATUS_CLOSED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_MANUAL, false,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EVAL, false,
            DzTaskDistList.STATUS_CLOSED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EVAL, false,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_MANUAL, false,
            DzTaskDistList.STATUS_TECH_ASSISTANCE)).isFalse();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_EVAL, false,
            DzTaskDistList.STATUS_TECH_ASSISTANCE)).isFalse();
    }

    @Test
    void publicReportTaskAllowsCloseOrFeedback() {
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, true,
            DzTaskDistList.STATUS_CLOSED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, true,
            DzTaskDistList.STATUS_FEEDBACKED)).isTrue();
        assertThat(AppTaskServiceImpl.isAllowedSubmitStatus(DzTaskDistList.SOURCE_TYPE_REPORT, true,
            DzTaskDistList.STATUS_TECH_ASSISTANCE)).isFalse();
    }

    @Test
    void emergencyFeedbackSubmitDoesNotUseProcessChainRetryLookups() {
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistListHistoryMapper historyMapper = mock(DzTaskDistListHistoryMapper.class);
        DzTaskDistList task = emergencyTask(DzTaskDistList.STATUS_INSPECTING);
        task.setHandleId(66L);
        when(taskMapper.selectById(100L)).thenReturn(task);
        doAnswer(invocation -> {
            DzTaskDistListHistory history = invocation.getArgument(0);
            history.setId(9003L);
            return 1;
        }).when(historyMapper).insertEntity(any(DzTaskDistListHistory.class));
        Fixture fixture = fixture(taskMapper, historyMapper);
        when(fixture.chainService().resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");

        fixture.service().submitTask(taskSubmitReq(DzTaskDistList.STATUS_FEEDBACKED));

        verify(fixture.chainService()).resolveSavedChainIdByTaskId(100L);
        verify(fixture.chainService(), never()).resolveSavedChainIdByBiz(any(), any());
        verify(fixture.chainService(), never()).resolveSavedNodeByBizAndLink(any(), any(), anyString(), any());
        verify(fixture.chainService(), never()).resolveSavedNodeByBizAndCategory(any(), any(), any());
        verify(fixture.handleService()).tryAutoArchiveAfterEmergencyTaskFeedback(66L, 7L, "测试巡查员");
    }

    private static Fixture fixture(DzTaskDistListMapper taskMapper, DzTaskDistListHistoryMapper historyMapper) {
        DisasterDetailedAddressResolver addressResolver = mock(DisasterDetailedAddressResolver.class);
        when(addressResolver.resolveCheckCenterLocation(anyString())).thenReturn("测试位置");
        IDzTaskProcessChainNodeService chainService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskHandleService handleService = mock(IDzTaskHandleService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IDzTaskHandleService> taskHandleServiceProvider = mock(ObjectProvider.class);
        when(taskHandleServiceProvider.getObject()).thenReturn(handleService);
        AppTaskServiceImpl service = new AppTaskServiceImpl(
            new AppTaskProps(),
            mock(ISysOssService.class),
            mock(OssService.class),
            taskMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class),
            historyMapper,
            mock(DifyAgentClient.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(ISysUserService.class),
            mock(ITaskSmsContentService.class),
            addressResolver,
            taskHandleServiceProvider,
            chainService,
            mock(IDzTaskProcessChainSummaryService.class),
            mock(AiHostingOverviewNotifyService.class)
        );
        return new Fixture(service, chainService, handleService);
    }

    private static InspectionSubmitReq taskSubmitReq(Integer status) {
        InspectionSubmitReq req = new InspectionSubmitReq();
        req.setTaskId(100L);
        req.setInspectorId(7L);
        req.setInspectorName("测试巡查员");
        req.setInspectorPhone("13800000000");
        req.setRole("巡查员");
        req.setCheckInTime("2026-06-25 10:00:00");
        req.setCheckInCenter("POINT(109.1 30.2)");
        req.setTextRecord("处置任务已完成反馈");
        req.setSubmitTime("2026-06-25 10:01:00");
        req.setStatus(status);
        req.setPhotos(List.of("oss-1", "oss-2"));
        return req;
    }

    private static DzTaskDistList emergencyTask(Integer status) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(100L);
        task.setUserId(7L);
        task.setResponsiblePerson("测试巡查员");
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setStatus(status);
        return task;
    }

    private record Fixture(AppTaskServiceImpl service, IDzTaskProcessChainNodeService chainService,
                           IDzTaskHandleService handleService) {
    }
}
