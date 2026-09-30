/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentWarningRelation;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
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
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.TaskSmsGroupKey;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskDistListPushAuditTest {

    @Test
    void manualPushAuditRecordsOperatorUserAndTime() {
        DzTaskDistList update = new DzTaskDistList();
        Date now = new Date();

        DzTaskDistListServiceImpl.markManualAppPushAudit(update, 42L, now);

        assertThat(update.getAppPushType()).isEqualTo(DzTaskDistList.APP_PUSH_TYPE_MANUAL);
        assertThat(update.getAppPushUserId()).isEqualTo(42L);
        assertThat(update.getAppPushTime()).isSameAs(now);
    }

    @Test
    void systemPushAuditRecordsAutoAgentAndTime() {
        DzTaskDistList update = new DzTaskDistList();
        Date now = new Date();

        DzTaskDistListServiceImpl.markSystemAppPushAudit(update, now);

        assertThat(update.getAppPushType()).isEqualTo(DzTaskDistList.APP_PUSH_TYPE_SYSTEM);
        assertThat(update.getAppPushUserId()).isEqualTo(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID);
        assertThat(update.getAppPushTime()).isSameAs(now);
    }

    @Test
    void manualTaskCreateAuditRecordsCreatorUser() {
        DzTaskDistList task = new DzTaskDistList();

        DzTaskDistListServiceImpl.markManualTaskCreateAudit(task, 42L);

        assertThat(task.getTaskCreateType()).isEqualTo(DzTaskDistList.TASK_CREATE_TYPE_MANUAL);
        assertThat(task.getTaskCreateUserId()).isEqualTo(42L);
    }

    @Test
    void systemTaskCreateAuditRecordsAutoAgent() {
        DzTaskDistList task = new DzTaskDistList();

        DzTaskDistListServiceImpl.markSystemTaskCreateAudit(task);

        assertThat(task.getTaskCreateType()).isEqualTo(DzTaskDistList.TASK_CREATE_TYPE_SYSTEM);
        assertThat(task.getTaskCreateUserId()).isEqualTo(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID);
    }

    @Test
    void systemNewTaskDefaultsFillLatestTaskColumnsWithoutTouchingPushAudit() {
        DzTaskDistList task = new DzTaskDistList();
        Date now = new Date();

        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);

        assertThat(task.getStatus()).isEqualTo(DzTaskDistList.STATUS_UNPUSHED);
        assertThat(task.getDelete()).isZero();
        assertThat(task.getOverdue()).isEqualTo(DzTaskDistList.OVERDUE_NO);
        assertThat(task.getQuotaConsumed()).isEqualTo(DzTaskDistList.QUOTA_CONSUMED_NO);
        assertThat(task.getReminderCount()).isZero();
        assertThat(task.getCreateDate()).isSameAs(now);
        assertThat(task.getUpdateDate()).isSameAs(now);
        assertThat(task.getTaskCreateType()).isEqualTo(DzTaskDistList.TASK_CREATE_TYPE_SYSTEM);
        assertThat(task.getTaskCreateUserId()).isEqualTo(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID);
        assertThat(task.getAppPushType()).isNull();
        assertThat(task.getAppPushUserId()).isNull();
        assertThat(task.getAppPushTime()).isNull();
    }

    @Test
    void sourceTypeSixWithoutStoredTaskTypeIsEmergencyInvestigationTask() {
        DzTaskDistList task = new DzTaskDistList();
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);

        assertThat(DzTaskDistListServiceImpl.isEmergencyInvestigationTask(task)).isTrue();
    }

    @Test
    void emergencyMonitoringTaskSubTypeDistinguishesInspectorAndDirector() {
        DzTaskDistList inspectorTask = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        inspectorTask.setPlanType(DzTaskDistList.PLAN_TYPE_MONITORING);
        inspectorTask.setResponsiblePerson("测试巡查员");

        DzTaskDistList directorTask = task(101L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        directorTask.setPlanType(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        directorTask.setResponsiblePerson("测试自规所长");

        assertThat(DzPushSmsDelegate.resolveTaskSubType(inspectorTask)).isEqualTo("群测群防巡查");
        assertThat(DzPushSmsDelegate.resolveTaskSubType(directorTask)).isEqualTo("仪器监测");
        assertThat(new TaskSmsGroupKey(1L, 4, 3, "现场处置", "群测群防巡查"))
            .isNotEqualTo(new TaskSmsGroupKey(1L, 4, 9, "现场处置", "仪器监测"));
    }

    @Test
    void pushRolesKeepSameRoleForEachUser() {
        DzTaskDistList firstTask = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        firstTask.setUserId(1L);
        DzTaskDistList secondTask = task(101L, DzTaskDistList.SOURCE_TYPE_EVAL);
        secondTask.setUserId(2L);

        List<String> roles = DzPushSmsDelegate.resolvePushRoles(
            List.of(firstTask, secondTask),
            List.of(user(1L, "张三"), user(2L, "李四"))
        );

        assertThat(roles).containsExactly("风险区巡查员", "风险区巡查员");
    }

    @Test
    void pushRolesMergeDifferentRolesOfSameUserWithSlash() {
        DzTaskDistList inspectorTask = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        inspectorTask.setUserId(1L);
        DzTaskDistList directorTask = task(101L, DzTaskDistList.SOURCE_TYPE_REPORT);
        directorTask.setUserId(1L);
        directorTask.setTaskType(DzTaskDistList.TASK_TYPE_AI_VERIFY);

        List<String> roles = DzPushSmsDelegate.resolvePushRoles(
            List.of(inspectorTask, directorTask),
            List.of(user(1L, "张三"))
        );

        assertThat(roles).containsExactly("风险区巡查员/乡自规所所长");
    }

    @Test
    void pushRolesKeepBlankPlaceholderWhenUserHasNoMatchedTask() {
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        task.setUserId(1L);

        List<String> roles = DzPushSmsDelegate.resolvePushRoles(
            List.of(task),
            List.of(user(1L, "张三"), user(2L, "李四"))
        );

        assertThat(roles).containsExactly("风险区巡查员", "");
    }

    @Test
    void pushSummaryFallsBackToTaskResponsiblePersonAndRoleWhenSystemUserIsMissing() {
        DzTaskDistList validUserTask = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        validUserTask.setUserId(1L);
        validUserTask.setResponsiblePerson("任务责任人一");

        DzTaskDistList missingUserTask = task(101L, DzTaskDistList.SOURCE_TYPE_REPORT);
        missingUserTask.setUserId(2L);
        missingUserTask.setTaskType(DzTaskDistList.TASK_TYPE_AI_VERIFY);
        missingUserTask.setResponsiblePerson("任务责任人二");

        List<DzTaskDistList> tasks = List.of(validUserTask, missingUserTask);
        List<Long> userIds = List.of(1L, 2L);
        Map<Long, SysUserVo> userMap = Map.of(1L, user(1L, "系统昵称"));

        assertThat(DzPushSmsDelegate.resolvePushNames(tasks, userIds, userMap))
            .containsExactly("系统昵称", "任务责任人二");
        assertThat(DzPushSmsDelegate.resolvePushRoles(tasks, userIds, userMap,
            Map.of(1L, "实际系统角色")))
            .containsExactly("实际系统角色", "乡自规所所长");
    }

    @Test
    void emergencyInvestigationTaskMustNotConflictWithHandleId() {
        DzTaskDistList task = new DzTaskDistList();
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        task.setTaskType(DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION);
        task.setHandleId(100L);

        assertThat(DzTaskDistListServiceImpl.isLinkedEmergencyInvestigationTask(task, 100L)).isTrue();
        assertThat(DzTaskDistListServiceImpl.isLinkedEmergencyInvestigationTask(task, 101L)).isFalse();
    }

    @Test
    void reportTaskPushUsesReportChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.REPORT.getCode(), 9L))
            .thenReturn("REPORT-9");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_REPORT);
        task.setReportId(9L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("REPORT-9");
    }

    @Test
    void reportTaskPushPrefersCurrentTaskChainOverReportChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(2070551056644780034L))
            .thenReturn("TASK-2070550521430618115");
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.REPORT.getCode(), 2070550951053176834L))
            .thenReturn("REPORT-2070550951053176834");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(2070551056644780034L, DzTaskDistList.SOURCE_TYPE_REPORT);
        task.setReportId(2070550951053176834L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("TASK-2070550521430618115");
    }

    @Test
    void defRespTaskPushUsesAlarmMainChainWhenPlanHasAlarm() {
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        DefRespPlan plan = new DefRespPlan();
        plan.setId(30L);
        plan.setSourceAlarmId(88L);
        when(defRespPlanMapper.selectById(30L)).thenReturn(plan);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.ALARM.getCode(), 88L))
            .thenReturn("ALARM-88");
        DzTaskDistListServiceImpl service = service(defRespPlanMapper, chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(30L);
        task.setHandleId(66L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("ALARM-88");
    }

    @Test
    void defRespTaskPushUsesExistingMainChainWithoutSavedChildChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L))
            .thenReturn("TASK-MAIN");
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.DEF_RESP.getCode(), 31L))
            .thenReturn("ALARM-MAIN");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList taskWithHandle = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        taskWithHandle.setDefId(30L);
        taskWithHandle.setHandleId(66L);

        DzTaskDistList taskWithDefResp = task(101L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        taskWithDefResp.setDefId(31L);

        assertThat(service.buildTaskPushProcessChainId(taskWithHandle)).isEqualTo("TASK-MAIN");
        assertThat(service.buildTaskPushProcessChainId(taskWithDefResp)).isEqualTo("ALARM-MAIN");
    }

    @Test
    void defRespTaskPushPrefersSavedTaskChildChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-10");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(30L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("TASK-10");
    }

    @Test
    void defRespTaskPushPrefersAnySavedTaskChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("ALARM-88");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(30L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("ALARM-88");
    }

    @Test
    void emergencyTaskPushUsesHandleMainChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L))
            .thenReturn("TASK-MAIN");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setHandleId(66L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("TASK-MAIN");
    }

    @Test
    void emergencyTaskPushPrefersSavedTaskChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-10");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setHandleId(66L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("TASK-10");
    }

    @Test
    void dailyPatrolProcessNodeGeneratesChainWithoutRetryingTaskChainLookup() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of());
        when(chainNodeService.generateChainId()).thenReturn("20260630-001");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        task.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);

        service.recordDailyPatrolProcessNode(task);

        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
        verify(chainNodeService).generateChainId();
        verify(chainNodeService).recordTaskNode(
            eq("20260630-001"),
            eq(TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getTriggerReason()),
            same(task),
            isNull(),
            isNull()
        );
    }

    @Test
    void emergencyTaskProcessNodeUsesHandleMainChainWhenNotBatchChild() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of());
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L))
            .thenReturn("TASK-MAIN");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setHandleId(66L);
        Date now = new Date();
        task.setCreateDate(now);

        service.recordEmergencyTaskProcessNode(task);

        verify(chainNodeService).recordTaskNode(
            eq("TASK-MAIN"),
            eq("下发应急处置任务"),
            eq("处置管理方案生成并下发应急处置任务"),
            same(task),
            isNull(),
            isNull()
        );
        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
    }

    @Test
    void defRespTaskProcessNodeUsesMainChainWithoutRetryingTaskChainLookup() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of());
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L))
            .thenReturn("DEF-30");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(30L);

        service.recordDefRespTaskProcessNode(task);

        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
        verify(chainNodeService).recordTaskNode(
            eq("DEF-30"),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getTriggerReason()),
            same(task),
            isNull(),
            isNull()
        );
    }

    @Test
    void emergencyBatchProcessNodeCreatesParentAndIndependentChildChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), 66L))
            .thenReturn("TASK-MAIN");
        when(chainNodeService.queryLatestNodeByTaskIds(List.of(100L))).thenReturn(Map.of());
        Date now = new Date();
        when(chainNodeService.recordBizNode(
            eq("TASK-MAIN"),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.EMERGENCY.getCode()),
            isNull(),
            eq("AI智能体"),
            eq(TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()),
            isNull(),
            isNull()
        )).thenReturn(900L);
        when(chainNodeService.generateChainId()).thenReturn("20260629-001");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setHandleId(66L);
        task.setCreateDate(now);

        service.recordEmergencyBatchProcessNodes(66L, List.of(task), now);

        verify(chainNodeService).recordTaskNode(
            eq("20260629-001"),
            eq("下发应急处置任务"),
            eq("处置管理方案生成并下发应急处置任务"),
            same(task),
            eq(900L),
            isNull()
        );
        verify(chainNodeService).queryLatestNodeByTaskIds(List.of(100L));
        verify(chainNodeService, never()).resolveSavedChainIdByTaskId(100L);
    }

    @Test
    void defRespBatchProcessNodeCreatesParentAndIndependentChildChain() {
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        Date now = new Date();
        DefRespPlan plan = new DefRespPlan();
        plan.setId(30L);
        plan.setSourceAlarmId(88L);
        DzTaskProcessChainNode alarmDefRespStart = new DzTaskProcessChainNode();
        alarmDefRespStart.setChainId("ALARM-88");
        when(defRespPlanMapper.selectById(30L)).thenReturn(plan);
        when(chainNodeService.resolveSavedNodeByBizAndLink(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            30L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        )).thenReturn(alarmDefRespStart);
        when(chainNodeService.recordBizNode(
            eq("ALARM-88"),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.DEF_RESP.getCode()),
            eq(30L),
            isNull(),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.DEF_RESP.getCode()),
            isNull(),
            eq("AI智能体"),
            eq(TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode()),
            isNull(),
            isNull()
        )).thenReturn(901L);
        when(chainNodeService.generateChainId()).thenReturn("20260629-002");
        DzTaskDistListServiceImpl service = service(defRespPlanMapper, chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(30L);
        task.setCreateDate(now);

        service.recordDefRespBatchProcessNodes(30L, List.of(task), now);

        verify(chainNodeService).recordTaskNode(
            eq("20260629-002"),
            eq("下发防御响应任务"),
            eq("防御响应方案生成并下发任务"),
            same(task),
            eq(901L),
            isNull()
        );
    }

    @Test
    void townDefRespBatchProcessNodeUsesCountyDefIdForMainChain() {
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        Date now = new Date();
        DefRespPlan townPlan = regionPlan(31L, RegionScopeTypeEnum.TOWN.getCode());
        townPlan.setParentDefId(30L);
        DefRespPlan countyPlan = regionPlan(30L, RegionScopeTypeEnum.COUNTY.getCode());
        when(defRespPlanMapper.selectById(31L)).thenReturn(townPlan);
        when(defRespPlanMapper.selectById(30L)).thenReturn(countyPlan);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L))
            .thenReturn("DEF_RESP-30");
        when(chainNodeService.recordBizNode(
            eq("DEF_RESP-30"),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.DEF_RESP.getCode()),
            eq(30L),
            isNull(),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.DEF_RESP.getCode()),
            isNull(),
            eq("AI智能体"),
            eq(TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode()),
            isNull(),
            isNull()
        )).thenReturn(902L);
        when(chainNodeService.generateChainId()).thenReturn("20260629-003");
        DzTaskDistListServiceImpl service = service(defRespPlanMapper, chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(101L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(31L);
        task.setCreateDate(now);

        service.recordDefRespBatchProcessNodes(31L, List.of(task), now);

        verify(chainNodeService).recordTaskNode(
            eq("20260629-003"),
            eq("下发防御响应任务"),
            eq("防御响应方案生成并下发任务"),
            same(task),
            eq(902L),
            isNull()
        );
        assertThat(task.getDefId()).isEqualTo(31L);
    }

    @Test
    void townDefRespBatchProcessNodeDoesNotFallbackWhenCountyParentInvalid() {
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DefRespPlan townPlan = regionPlan(31L, RegionScopeTypeEnum.TOWN.getCode());
        townPlan.setParentDefId(30L);
        DefRespPlan invalidParent = regionPlan(30L, RegionScopeTypeEnum.TOWN.getCode());
        when(defRespPlanMapper.selectById(31L)).thenReturn(townPlan);
        when(defRespPlanMapper.selectById(30L)).thenReturn(invalidParent);
        DzTaskDistListServiceImpl service = service(defRespPlanMapper, chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(101L, DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        task.setDefId(31L);

        assertThatThrownBy(() -> service.recordDefRespBatchProcessNodes(31L, List.of(task), new Date()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("生成防御响应任务未找到已有主链")
            .hasMessageContaining("defId=31")
            .hasMessageContaining("mainDefId=31")
            .hasMessageContaining("parentDefId=30");
    }

    @Test
    void monitorWarningTaskPushUsesWarningEventChain() {
        IDzRiskAssessmentWarningRelationService warningRelationService = mock(IDzRiskAssessmentWarningRelationService.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.MONITOR_WARNING.getCode(), 99L))
            .thenReturn("MONITOR_WARNING-99");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService, warningRelationService);
        DzRiskAssessmentWarningRelation relation = new DzRiskAssessmentWarningRelation();
        relation.setWarningEventId(99L);
        when(warningRelationService.getByRiskAssessmentId(20L)).thenReturn(relation);
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING);
        task.setRiskId(20L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("MONITOR_WARNING-99");
    }

    @Test
    void relatedTaskPushDoesNotGenerateRootTaskChainWhenMissing() {
        DzTaskDistListServiceImpl service = service();
        DzTaskDistList task = task(200L, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        task.setRelatedTaskId(100L);

        assertThat(service.buildTaskPushProcessChainId(task)).isNull();
    }

    @Test
    void relatedTaskPushPrefersSavedRelatedTaskChain() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-90");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(200L, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        task.setRelatedTaskId(100L);

        assertThat(service.buildTaskPushProcessChainId(task)).isEqualTo("TASK-90");
    }

    @Test
    void systemTaskPushNodeUsesAutoAgentAndTaskBiz() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_EVAL);
        Date now = new Date();

        service.recordTaskPushProcessNode(task, false, now, "自动化运营任务");

        verify(chainNodeService).recordBizNode(
            eq("TASK-100"),
            eq(DzTaskDistListServiceImpl.TASK_PUSH_LINK_NAME),
            eq("自动化运营任务自动推送任务到 APP"),
            eq(TaskProcessBizTypeEnum.TASK.getCode()),
            eq(100L),
            eq(100L),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.EVAL.getCode()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void manualTaskPushNodeFallsBackToAppPushUserWhenLoginMissing() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_MANUAL);
        task.setAppPushUserId(42L);
        Date now = new Date();

        service.recordTaskPushProcessNode(task, true, now, null);

        verify(chainNodeService).recordBizNode(
            eq("TASK-100"),
            eq(DzTaskDistListServiceImpl.TASK_PUSH_LINK_NAME),
            eq(DzTaskDistListServiceImpl.TASK_PUSH_REASON_MANUAL),
            eq(TaskProcessBizTypeEnum.TASK.getCode()),
            eq(100L),
            eq(100L),
            eq(42L),
            isNull(),
            eq(TaskProcessSourceTypeEnum.MANUAL.getCode()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void emergencyBatchPushNodeWaitsUntilAllBatchTasksHavePushNode() {
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");
        DzTaskDistList pushedTask = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        pushedTask.setHandleId(66L);
        DzTaskDistList waitingTask = task(101L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        waitingTask.setHandleId(66L);
        when(taskMapper.selectList(any())).thenReturn(List.of(pushedTask, waitingTask));
        when(chainNodeService.queryChainByTaskId(100L)).thenReturn(List.of(vo("任务推送")));
        when(chainNodeService.queryChainByTaskId(101L)).thenReturn(List.of(vo("下发应急处置任务")));
        DzTaskDistListServiceImpl service = service(taskMapper, mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));

        service.recordTaskPushProcessNode(pushedTask, false, new Date(), "自动化运营任务");

        verify(chainNodeService, never()).resolveSavedNodeByBizAndLink(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode())
        );
        verify(chainNodeService, never()).recordBizNode(
            eq("HANDLE-66"),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName()),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void emergencyBatchPushNodeRecordsOnceAllBatchTasksHavePushNode() {
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.resolveSavedChainIdByTaskId(100L)).thenReturn("TASK-100");
        DzTaskDistList taskA = task(100L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        taskA.setHandleId(66L);
        DzTaskDistList taskB = task(101L, DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        taskB.setHandleId(66L);
        when(taskMapper.selectList(any())).thenReturn(List.of(taskA, taskB));
        when(chainNodeService.queryChainByTaskId(100L)).thenReturn(List.of(vo("任务推送")));
        when(chainNodeService.queryChainByTaskId(101L)).thenReturn(List.of(vo("任务推送")));
        DzTaskProcessChainNode batchNode = new DzTaskProcessChainNode();
        batchNode.setId(900L);
        batchNode.setChainId("HANDLE-66");
        when(chainNodeService.resolveSavedNodeByBizAndLink(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            66L,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        )).thenReturn(batchNode);
        DzTaskDistListServiceImpl service = service(taskMapper, mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));

        service.recordTaskPushProcessNode(taskA, false, new Date(), "自动化运营任务");

        verify(chainNodeService).recordBizNode(
            eq("HANDLE-66"),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.EMERGENCY.getCode()),
            eq(900L),
            eq("AI智能体"),
            eq(TaskProcessNodeCategoryEnum.TASK_PUSH.getCode()),
            isNull(),
            isNull()
        );
    }

    @Test
    void manualTaskCreateRecordsInitialProcessNode() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        when(chainNodeService.generateChainId()).thenReturn("20260629-004");
        DzTaskDistListServiceImpl service = service(mock(DzDefRespPlanMapper.class), chainNodeService,
            mock(IDzRiskAssessmentWarningRelationService.class));
        DzTaskDistList task = task(100L, DzTaskDistList.SOURCE_TYPE_MANUAL);
        Date now = new Date();
        task.setCreateDate(now);

        service.recordManualTaskCreateProcessNode(task);

        verify(chainNodeService).recordTaskNode(
            eq("20260629-004"),
            eq(TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getTriggerReason()),
            same(task),
            isNull(),
            isNull()
        );
    }

    private DzTaskDistList task(Long id, Integer sourceType) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(id);
        task.setSourceType(sourceType);
        return task;
    }

    private SysUserVo user(Long userId, String nickName) {
        SysUserVo user = new SysUserVo();
        user.setUserId(userId);
        user.setNickName(nickName);
        return user;
    }

    private DefRespPlan regionPlan(Long id, Integer regionScopeType) {
        DefRespPlan plan = new DefRespPlan();
        plan.setId(id);
        plan.setType(DefRespPlanTypeEnum.REGION.getCode());
        plan.setRegionScopeType(regionScopeType);
        return plan;
    }

    private DzTaskDistListServiceImpl service() {
        return service(mock(DzDefRespPlanMapper.class), mock(IDzTaskProcessChainNodeService.class),
            mock(IDzRiskAssessmentWarningRelationService.class));
    }

    private DzTaskDistListServiceImpl service(DzDefRespPlanMapper defRespPlanMapper,
                                              IDzTaskProcessChainNodeService chainNodeService,
                                              IDzRiskAssessmentWarningRelationService warningRelationService) {
        return service(mock(DzTaskDistListMapper.class), defRespPlanMapper, chainNodeService, warningRelationService);
    }

    private DzTaskDistListServiceImpl service(DzTaskDistListMapper taskMapper,
                                              DzDefRespPlanMapper defRespPlanMapper,
                                              IDzTaskProcessChainNodeService chainNodeService,
                                              IDzRiskAssessmentWarningRelationService warningRelationService) {
        return new DzTaskDistListServiceImpl(
            taskMapper,
            mock(DzTaskDistListAddMapper.class),
            mock(DzTaskDistListRemarkMapper.class),
            mock(DzTaskDistListHistoryMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(ISysUserService.class),
            mock(DzRiskAssessmentMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(IAppTaskService.class),
            mock(AppTaskProps.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(IDzUserAdRegionService.class),
            mock(IDzTaskHandleSceneRecordService.class),
            mock(IDzTaskHandleDetailContentService.class),
            defRespPlanMapper,
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
            mock(IDzTaskProcessChainSummaryService.class),
            warningRelationService,
            mock(DifyAgentClient.class)
        );
    }

    private TaskProcessChainNodeVo vo(String linkName) {
        TaskProcessChainNodeVo vo = new TaskProcessChainNodeVo();
        vo.setLinkName(linkName);
        return vo;
    }
}
