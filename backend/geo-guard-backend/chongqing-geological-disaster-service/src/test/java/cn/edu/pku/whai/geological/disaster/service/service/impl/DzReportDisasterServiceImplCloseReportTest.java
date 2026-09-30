/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterFeedbackBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzReportDisasterServiceImplCloseReportTest {

    @Test
    void closePublicReportRecordsClosedNodeAfterGeneratedReport() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzReportDisaster report = report(100L, 2, null);
        report.setProcessType(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        LoginUser loginUser = loginUser(88L, "关闭人", "close-user");
        DzTaskProcessChainNode parent = parentNode(12L, "REPORT-100",
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName());
        when(reportMapper.selectById(100L)).thenReturn(report);
        when(chainNodeService.resolveSavedNodeByBizAndLink(TaskProcessBizTypeEnum.REPORT.getCode(), 100L,
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName(), null)).thenReturn(parent);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getLoginUser).thenReturn(loginUser);

            service(reportMapper, chainNodeService).closeReport(100L);
        }

        verify(reportMapper).updateById(any(DzReportDisaster.class));
        verify(chainNodeService).recordBizNode("REPORT-100",
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 100L, null, 88L, "关闭人",
            TaskProcessSourceTypeEnum.REPORT.getCode(), 12L, null,
            TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED.getCode());
    }

    @Test
    void closeTaskFeedbackReportFallsBackToReportUserWhenLoginUnavailable() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzReportDisaster report = report(101L, 1, 200L);
        report.setProcessType(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        DzTaskProcessChainNode parent = parentNode(22L, "TASK-200", "日常巡查任务反馈报告");
        when(reportMapper.selectById(101L)).thenReturn(report);
        when(chainNodeService.resolveSavedNodeByBizAndCategory(TaskProcessBizTypeEnum.REPORT.getCode(), 101L,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())).thenReturn(parent);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getLoginUser).thenThrow(new IllegalStateException("not login"));

            service(reportMapper, chainNodeService).closeReport(101L);
        }

        verify(chainNodeService).recordBizNode("TASK-200",
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 101L, 200L, 7L, "上报人",
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), 22L, "群众",
            TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED.getCode());
    }

    @Test
    void closeReportThrowsWhenReportMissing() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(reportMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service(reportMapper, chainNodeService).closeReport(404L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("报灾记录不存在");

        verify(reportMapper, never()).updateById(any(DzReportDisaster.class));
        verify(chainNodeService, never()).recordBizNode(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void closeTaskFeedbackReportDoesNotBlockWhenParentNodeMissing() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzReportDisaster report = report(102L, 1, 200L);
        when(reportMapper.selectById(102L)).thenReturn(report);
        when(chainNodeService.resolveSavedNodeByBizAndCategory(TaskProcessBizTypeEnum.REPORT.getCode(), 102L,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())).thenReturn(null);

        service(reportMapper, chainNodeService).closeReport(102L);

        ArgumentCaptor<DzReportDisaster> captor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(reportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(3);
        verify(chainNodeService, never()).recordBizNode(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addFeedbackRefreshesReportRiskLevelSummaryAfterSavingManualRisk() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        when(reportMapper.selectById(103L)).thenReturn(report(103L, 1, 200L));
        DzReportDisasterFeedbackBo bo = new DzReportDisasterFeedbackBo();
        bo.setId(103L);
        bo.setManualRiskLevel(4);
        bo.setManualRiskRemark("人工确认高风险");

        service(reportMapper, chainNodeService, chainSummaryService).addFeedback(bo);

        ArgumentCaptor<DzReportDisaster> captor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(reportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getManualRiskLevel()).isEqualTo(4);
        verify(chainSummaryService).refreshReportRiskLevelIfCurrent(103L);
    }

    private DzReportDisasterServiceImpl service(DzReportDisasterMapper reportMapper,
                                                IDzTaskProcessChainNodeService chainNodeService) {
        return service(reportMapper, chainNodeService, mock(IDzTaskProcessChainSummaryService.class));
    }

    private DzReportDisasterServiceImpl service(DzReportDisasterMapper reportMapper,
                                                IDzTaskProcessChainNodeService chainNodeService,
                                                IDzTaskProcessChainSummaryService chainSummaryService) {
        return new DzReportDisasterServiceImpl(
            reportMapper,
            mock(DzTaskDistListMapper.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(ISysUserService.class),
            mock(SmsSendService.class),
            mock(IDzUserAdRegionService.class),
            mock(ITaskSmsContentService.class),
            mock(DisasterDetailedAddressResolver.class),
            mock(DzRiskAssessmentMapper.class),
            chainNodeService,
            chainSummaryService,
            mock(AiHostingOverviewNotifyService.class),
            mock(Environment.class)
        );
    }

    private DzReportDisaster report(Long id, Integer sourceType, Long taskId) {
        DzReportDisaster report = new DzReportDisaster();
        report.setId(id);
        report.setSourceType(sourceType);
        report.setTaskId(taskId);
        report.setProcessType(taskId == null
            ? DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY
            : DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        report.setUserId(7L);
        report.setUserName("上报人");
        report.setUserRole("群众");
        return report;
    }

    private LoginUser loginUser(Long userId, String nickname, String username) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setNickname(nickname);
        loginUser.setUsername(username);
        return loginUser;
    }

    private DzTaskProcessChainNode parentNode(Long id, String chainId, String linkName) {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(id);
        node.setChainId(chainId);
        node.setLinkName(linkName);
        return node;
    }
}
