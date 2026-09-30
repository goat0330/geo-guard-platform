/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.service.impl;

import org.dromara.common.core.service.OssService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionSubmitReq;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListHistory;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysUserService;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.domain.vo.SysUserVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class AppTaskServiceImplPublicReportIdempotencyTest {

    @Test
    void skipsInsertWhenPublicReportParamsAlreadyExistInWindow() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzReportDisaster existed = new DzReportDisaster();
        existed.setId(100L);
        existed.setProcessType(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        when(reportMapper.selectList(anyReportWrapper())).thenReturn(List.of(existed));
        Fixture fixture = fixture(reportMapper);

        fixture.service().submitTask(publicReportReq());

        verify(reportMapper, never()).insert(any(DzReportDisaster.class));
        verify(fixture.chainService(), never()).recordBizNode(anyString(), anyString(), anyString(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void insertsPublicReportWhenNoRecentDuplicateExists() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        when(reportMapper.selectList(anyReportWrapper())).thenReturn(List.of());
        doAnswer(invocation -> {
            DzReportDisaster report = invocation.getArgument(0);
            report.setId(101L);
            return 1;
        }).when(reportMapper).insert(any(DzReportDisaster.class));
        Fixture fixture = fixture(reportMapper);

        fixture.service().submitTask(publicReportReq());

        ArgumentCaptor<DzReportDisaster> captor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(reportMapper).insert(captor.capture());
        DzReportDisaster inserted = captor.getValue();
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        assertThat(inserted.getTaskId()).isNull();
        assertThat(inserted.getUserId()).isEqualTo(7L);
        assertThat(inserted.getUserPhone()).isEqualTo("13800000000");
        assertThat(inserted.getCheckCenter()).isEqualTo("POINT(109.1 30.2)");
        assertThat(inserted.getSceneTextRecord()).isEqualTo("发现裂缝");
    }

    @Test
    void exactPublicRoleMatchIgnoresForgedRequestRoleAndPersistsPublicReport() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 110L);
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.selectUserExportList(any())).thenReturn(List.of(userExport(7L, "13800000000")));
        when(userService.selectUserById(7L)).thenReturn(userWithRoles(SysRoleEnum.DZ_QZ));
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);

        InspectionSubmitReq req = publicReportReq();
        req.setRole(SysRoleEnum.DZ_FXQXCY.getRoleName());
        fixture.service().submitTask(req);

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        assertThat(inserted.getUserRole()).isEqualTo(SysRoleEnum.DZ_QZ.getRoleName());
    }

    @Test
    void skipsRecentTaskFeedbackReportDuplicateWithoutTaskId() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzReportDisaster existed = new DzReportDisaster();
        existed.setId(111L);
        existed.setSourceType(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        existed.setProcessType(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        existed.setTaskId(null);
        when(reportMapper.selectList(anyReportWrapper())).thenReturn(List.of(existed));
        Fixture fixture = fixture(reportMapper);

        fixture.service().submitTask(publicReportReq());

        verify(reportMapper, never()).insert(any(DzReportDisaster.class));
        verify(fixture.chainService(), never()).recordBizNode(anyString(), anyString(), anyString(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void patrolRoleRoutesReportAsDirectTownAndCreatesPatrolRootDespiteForgedRequestRole() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        when(reportMapper.selectList(anyReportWrapper())).thenReturn(List.of());
        doAnswer(invocation -> {
            DzReportDisaster report = invocation.getArgument(0);
            report.setId(102L);
            return 1;
        }).when(reportMapper).insert(any(DzReportDisaster.class));
        ISysUserService userService = mock(ISysUserService.class);
        SysUserExportVo matched = new SysUserExportVo();
        matched.setUserId(7L);
        matched.setPhonenumber("13800000000");
        SysUserVo user = new SysUserVo();
        SysRoleVo role = new SysRoleVo();
        role.setRoleKey(SysRoleEnum.DZ_FXQXCY.getRoleKey());
        role.setRoleName(SysRoleEnum.DZ_FXQXCY.getRoleName());
        user.setRoles(List.of(role));
        when(userService.selectUserExportList(any())).thenReturn(List.of(matched));
        when(userService.selectUserById(7L)).thenReturn(user);
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);
        when(fixture.chainService().generateChainId()).thenReturn("REPORT-102");

        InspectionSubmitReq req = publicReportReq();
        req.setRole("群众");
        fixture.service().submitTask(req);

        ArgumentCaptor<DzReportDisaster> reportCaptor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(reportMapper).insert(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(reportCaptor.getValue().getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        assertThat(reportCaptor.getValue().getUserRole()).isEqualTo(SysRoleEnum.DZ_FXQXCY.getRoleName());
        verify(fixture.chainService()).recordBizNode(eq("REPORT-102"),
            eq(TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.REPORT.getCode()), eq(102L), isNull(), eq(7L), eq("张三"),
            eq(TaskProcessSourceTypeEnum.REPORT.getCode()), isNull(), isNull(),
            eq(TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode()), isNull(), isNull());
    }

    @Test
    void mismatchedInspectorIdDowngradesToPublicReport() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 103L);
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.selectUserExportList(any())).thenReturn(List.of(userExport(77L, "13800000000")));
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);

        fixture.service().submitTask(publicReportReq());

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void partiallyMatchingInspectorPhoneDowngradesToPublicReport() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 104L);
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.selectUserExportList(any())).thenReturn(List.of(userExport(7L, "1380000000")));
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);

        fixture.service().submitTask(publicReportReq());

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void blankInspectorPhoneDowngradesToPublicReportWithoutUserLookup() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 105L);
        ISysUserService userService = mock(ISysUserService.class);
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);
        InspectionSubmitReq req = publicReportReq();
        req.setInspectorPhone("  ");

        fixture.service().submitTask(req);

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        verify(userService, never()).selectUserExportList(any());
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void missingInspectorIdDowngradesToPublicReportWithoutUserLookup() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 109L);
        ISysUserService userService = mock(ISysUserService.class);
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);
        InspectionSubmitReq req = publicReportReq();
        req.setInspectorId(null);

        fixture.service().submitTask(req);

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        verify(userService, never()).selectUserExportList(any());
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void laterExactMatchIsUsedWhenFirstExportedUserDoesNotMatch() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 106L);
        ISysUserService userService = mock(ISysUserService.class);
        SysUserVo patrolUser = userWithRoles(SysRoleEnum.DZ_FXQXCY);
        when(userService.selectUserExportList(any())).thenReturn(List.of(
            userExport(77L, "13800000000"), userExport(7L, " 13800000000 ")));
        when(userService.selectUserById(7L)).thenReturn(patrolUser);
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);
        InspectionSubmitReq req = publicReportReq();
        req.setInspectorPhone(" 13800000000 ");

        fixture.service().submitTask(req);

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        verify(userService).selectUserById(7L);
    }

    @Test
    void multipleRolesIncludingPatrolInspectorRouteAsTaskFeedback() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 107L);
        ISysUserService userService = mock(ISysUserService.class);
        SysRoleVo manager = new SysRoleVo();
        manager.setRoleKey("ordinary");
        manager.setRoleName("普通用户");
        SysUserVo user = userWithRoles(SysRoleEnum.DZ_FXQXCY);
        user.setRoles(List.of(manager, user.getRoles().getFirst()));
        when(userService.selectUserExportList(any())).thenReturn(List.of(userExport(7L, "13800000000")));
        when(userService.selectUserById(7L)).thenReturn(user);
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);

        fixture.service().submitTask(publicReportReq());

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
    }

    @Test
    void roleLookupExceptionDowngradesToPublicReportWithoutBlockingInsert() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        stubReportInsert(reportMapper, 108L);
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.selectUserExportList(any())).thenReturn(List.of(userExport(7L, "13800000000")));
        when(userService.selectUserById(7L)).thenThrow(new IllegalStateException("role lookup failed"));
        Fixture fixture = fixture(reportMapper, mock(DzTaskDistListMapper.class),
            mock(DzTaskDistListHistoryMapper.class), userService);

        fixture.service().submitTask(publicReportReq());

        DzReportDisaster inserted = insertedReport(reportMapper);
        assertThat(inserted.getSourceType()).isEqualTo(DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT);
        assertThat(inserted.getProcessType()).isEqualTo(DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY);
        verify(reportMapper).insert(any(DzReportDisaster.class));
    }

    @Test
    void inspectingSubmitUsesHistoryIdAsProcessNodeBizId() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistListHistoryMapper historyMapper = mock(DzTaskDistListHistoryMapper.class);
        DzTaskDistList task = emergencyTask(DzTaskDistList.STATUS_UNINSPECTED);
        when(taskMapper.selectById(100L)).thenReturn(task);
        doAnswer(invocation -> {
            DzTaskDistListHistory history = invocation.getArgument(0);
            history.setId(9001L);
            return 1;
        }).when(historyMapper).insertEntity(any(DzTaskDistListHistory.class));
        Fixture fixture = fixture(reportMapper, taskMapper, historyMapper);
        when(fixture.chainService().resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");

        fixture.service().submitTask(taskSubmitReq(DzTaskDistList.STATUS_INSPECTING));

        ArgumentCaptor<DzTaskDistListHistory> historyCaptor = ArgumentCaptor.forClass(DzTaskDistListHistory.class);
        InOrder inOrder = inOrder(historyMapper, fixture.chainService());
        inOrder.verify(historyMapper).insertEntity(historyCaptor.capture());
        inOrder.verify(fixture.chainService()).recordBizNode(eq("TASK-100"), eq("任务核查中"),
            eq(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.TASK.getCode()), eq(9001L), eq(100L), eq(7L), eq("测试巡查员"),
            eq(TaskProcessSourceTypeEnum.EMERGENCY.getCode()), ArgumentMatchers.<Long>isNull(), eq("巡查员"),
            eq(TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode()), ArgumentMatchers.<Integer>isNull(),
            ArgumentMatchers.<String>isNull());
        assertThat(historyCaptor.getValue().getTaskId()).isEqualTo(100L);
        assertThat(historyCaptor.getValue().getSubmitStatus()).isEqualTo(DzTaskDistList.STATUS_INSPECTING);
    }

    @Test
    void nonInspectingSubmitStillUsesTaskIdAsProcessNodeBizId() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistListHistoryMapper historyMapper = mock(DzTaskDistListHistoryMapper.class);
        DzTaskDistList task = emergencyTask(DzTaskDistList.STATUS_UNINSPECTED);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_MANUAL);
        when(taskMapper.selectById(100L)).thenReturn(task);
        doAnswer(invocation -> {
            DzTaskDistListHistory history = invocation.getArgument(0);
            history.setId(9002L);
            return 1;
        }).when(historyMapper).insertEntity(any(DzTaskDistListHistory.class));
        Fixture fixture = fixture(reportMapper, taskMapper, historyMapper);
        when(fixture.chainService().resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");

        fixture.service().submitTask(taskSubmitReq(DzTaskDistList.STATUS_CLOSED));

        verify(fixture.chainService()).recordBizNode(eq("TASK-100"),
            eq(TaskProcessChainNodeTextEnum.TASK_CLOSE_BY_APP.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.TASK_CLOSE_BY_APP.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.TASK.getCode()), eq(100L), eq(100L), eq(7L), eq("测试巡查员"),
            eq(TaskProcessSourceTypeEnum.MANUAL.getCode()), ArgumentMatchers.<Long>isNull(), eq("巡查员"),
            eq(TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode()), ArgumentMatchers.<Integer>isNull(),
            ArgumentMatchers.<String>isNull());
    }

    @Test
    void emergencyFeedbackSubmitTriggersHandleAutoArchiveAttempt() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
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
        Fixture fixture = fixture(reportMapper, taskMapper, historyMapper);
        when(fixture.chainService().resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");

        fixture.service().submitTask(taskSubmitReq(DzTaskDistList.STATUS_FEEDBACKED));

        verify(fixture.handleService()).tryAutoArchiveAfterEmergencyTaskFeedback(66L, 7L, "测试巡查员");
    }

    @SuppressWarnings("unchecked")
    private static Wrapper<DzReportDisaster> anyReportWrapper() {
        return any(Wrapper.class);
    }

    private static void stubReportInsert(DzReportDisasterMapper reportMapper, long reportId) {
        when(reportMapper.selectList(anyReportWrapper())).thenReturn(List.of());
        doAnswer(invocation -> {
            DzReportDisaster report = invocation.getArgument(0);
            report.setId(reportId);
            return 1;
        }).when(reportMapper).insert(any(DzReportDisaster.class));
    }

    private static DzReportDisaster insertedReport(DzReportDisasterMapper reportMapper) {
        ArgumentCaptor<DzReportDisaster> captor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(reportMapper).insert(captor.capture());
        return captor.getValue();
    }

    private static SysUserExportVo userExport(long userId, String phone) {
        SysUserExportVo user = new SysUserExportVo();
        user.setUserId(userId);
        user.setPhonenumber(phone);
        return user;
    }

    private static SysUserVo userWithRoles(SysRoleEnum... roleEnums) {
        SysUserVo user = new SysUserVo();
        user.setRoles(List.of(roleEnums).stream().map(roleEnum -> {
            SysRoleVo role = new SysRoleVo();
            role.setRoleKey(roleEnum.getRoleKey());
            role.setRoleName(roleEnum.getRoleName());
            return role;
        }).toList());
        return user;
    }

    private static Fixture fixture(DzReportDisasterMapper reportMapper) {
        return fixture(reportMapper, mock(DzTaskDistListMapper.class), mock(DzTaskDistListHistoryMapper.class),
            mock(ISysUserService.class));
    }

    private static Fixture fixture(DzReportDisasterMapper reportMapper, DzTaskDistListMapper taskMapper,
                                   DzTaskDistListHistoryMapper historyMapper) {
        return fixture(reportMapper, taskMapper, historyMapper, mock(ISysUserService.class));
    }

    private static Fixture fixture(DzReportDisasterMapper reportMapper, DzTaskDistListMapper taskMapper,
                                   DzTaskDistListHistoryMapper historyMapper, ISysUserService userService) {
        DisasterDetailedAddressResolver addressResolver = mock(DisasterDetailedAddressResolver.class);
        when(addressResolver.resolve(anyString())).thenReturn("测试详细地址");
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
            reportMapper,
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class),
            historyMapper,
            mock(DifyAgentClient.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            userService,
            mock(ITaskSmsContentService.class),
            addressResolver,
            taskHandleServiceProvider,
            chainService,
            mock(IDzTaskProcessChainSummaryService.class),
            mock(AiHostingOverviewNotifyService.class)
        );
        return new Fixture(service, chainService, handleService);
    }

    private static InspectionSubmitReq publicReportReq() {
        InspectionSubmitReq req = new InspectionSubmitReq();
        req.setInspectorId(7L);
        req.setInspectorName("张三");
        req.setInspectorPhone("13800000000");
        req.setRole("群众");
        req.setCheckInTime("2026-06-25 10:00:00");
        req.setCheckInCenter("POINT(109.1 30.2)");
        req.setTextRecord("发现裂缝");
        req.setSubmitTime("2026-06-25 10:01:00");
        req.setStatus(5);
        req.setCheckCenterLocation("测试位置");
        return req;
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
        req.setTextRecord("处置任务继续执行");
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
