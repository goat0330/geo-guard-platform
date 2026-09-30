/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterHandleVo;
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
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.AbstractMap;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzBatchGenerateDelegateEmergencyAssigneeTest {

    private static final String UNIT_ID = "U001";

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DzRiskAssessment.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DzRiskAssessment.class
            );
        }
    }

    @Test
    void villageSecretaryPlanTypesAssignToSpecialManager() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(
                entry(PlanTypeEnum.RESETTLEMENT, "人员安置措施"),
                entry(PlanTypeEnum.PROTECTION, "警示防护措施"),
                entry(PlanTypeEnum.PUBLICITY, "宣传告知措施"),
                entry(PlanTypeEnum.TRAFFIC_CONTROL, "交通管制措施"),
                entry(PlanTypeEnum.OTHER_SUGGESTIONS, "其他建议措施")
            ),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).hasSize(5);
        assertThat(tasks).allSatisfy(task -> {
            assertThat(task.getUserId()).isEqualTo(401L);
            assertThat(task.getResponsiblePerson()).isEqualTo("专管员村支书");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000004");
            assertThat(task.getSourceType()).isEqualTo(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
            assertThat(task.getTaskType()).isEqualTo(DzTaskDistList.TASK_TYPE_EMERGENCY);
        });
    }

    @Test
    void hazardRemovalAndEngineeringAssignToTownNaturalResourceDirector() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(
                entry(PlanTypeEnum.HAZARD_REMOVAL, "排危除险措施"),
                entry(PlanTypeEnum.ENGINEERING, "工程治理措施")
            ),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).hasSize(2);
        assertThat(tasks).allSatisfy(task -> {
            assertThat(task.getUserId()).isEqualTo(301L);
            assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000003");
        });
    }

    @Test
    void monitoringMassPreventionAssignsToInspector() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "开展群测群防巡查")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
            assertThat(task.getInspectionSuggestion()).isEqualTo("开展群测群防巡查");
            assertThat(task.getUserId()).isEqualTo(201L);
            assertThat(task.getResponsiblePerson()).isEqualTo("巡查员");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000002");
        });
    }

    @Test
    void monitoringInstrumentAssignsToTownNaturalResourceDirector() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "开展仪器监测")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
            assertThat(task.getInspectionSuggestion()).isEqualTo("开展仪器监测");
            assertThat(task.getUserId()).isEqualTo(301L);
            assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000003");
        });
    }

    @Test
    void monitoringWithBothKeywordsCreatesTwoTasksForDifferentAssignees() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "同步开展群测群防和仪器监测")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(DzTaskDistList::getPlanType)
                         .containsExactly(DzTaskDistList.PLAN_TYPE_MONITORING,
                             DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        assertThat(tasks).extracting(DzTaskDistList::getResponsiblePerson)
                         .containsExactly("巡查员", "乡自规所所长");
        assertThat(tasks).extracting(DzTaskDistList::getInspectionSuggestion)
                         .containsExactly("同步开展群测群防和仪器监测", "同步开展群测群防和仪器监测");
    }

    @Test
    void requestedInstrumentMonitoringOnlyCreatesInstrumentTask() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "同步开展群测群防和仪器监测")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            "同步开展群测群防和仪器监测",
            new Date(0L),
            DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
            assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
        });
    }

    @Test
    void requestedMassPreventionMonitoringOnlyCreatesPatrolTask() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "同步开展群测群防和仪器监测")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            "同步开展群测群防和仪器监测",
            new Date(0L),
            DzTaskDistList.PLAN_TYPE_MONITORING
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
            assertThat(task.getResponsiblePerson()).isEqualTo("巡查员");
        });
    }

    @Test
    void monitoringDispatchContentCreatesTwoTasksButKeepsAiSuggestion() {
        TestContext context = context();
        String aiSuggestion = "AI方案仅描述群测群防巡查";
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, aiSuggestion)),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            "仪器监测,群测群防",
            new Date(0L),
            null
        );

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(DzTaskDistList::getPlanType)
                         .containsExactly(DzTaskDistList.PLAN_TYPE_MONITORING,
                             DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        assertThat(tasks).extracting(DzTaskDistList::getResponsiblePerson)
                         .containsExactly("巡查员", "乡自规所所长");
        assertThat(tasks).extracting(DzTaskDistList::getInspectionSuggestion)
                         .containsExactly(aiSuggestion, aiSuggestion);
    }

    @Test
    void monitoringDispatchContentInstrumentOverridesAiMassPreventionForAssigneeOnly() {
        TestContext context = context();
        String aiSuggestion = "AI方案仅描述群测群防巡查";
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, aiSuggestion)),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            "仪器监测",
            new Date(0L),
            null
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
            assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000003");
            assertThat(task.getInspectionSuggestion()).isEqualTo(aiSuggestion);
        });
    }

    @Test
    void blankMonitoringDispatchContentFallsBackToAiSuggestion() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "开展仪器监测")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            " ",
            new Date(0L),
            null
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
            assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
            assertThat(task.getInspectionSuggestion()).isEqualTo("开展仪器监测");
        });
    }

    @Test
    void monitoringWithoutKeywordDoesNotCreateTask() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "加强现场观察")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        );

        assertThat(tasks).isEmpty();
    }

    @Test
    void skippedMonitoringWithoutKeywordDefaultsToInspector() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, "加强现场观察")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            "加强现场观察",
            new Date(0L),
            PlanTypeEnum.MONITORING.getCode(),
            true
        );

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.getPlanType()).isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
            assertThat(task.getInspectionSuggestion()).isEqualTo("加强现场观察");
            assertThat(task.getUserId()).isEqualTo(201L);
            assertThat(task.getResponsiblePerson()).isEqualTo("巡查员");
            assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000002");
        });
    }

    @Test
    void skippedMonitoringBlankContentDoesNotCreateTask() {
        TestContext context = context();
        List<DzTaskDistList> tasks = context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.MONITORING, " ")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            " ",
            new Date(0L),
            PlanTypeEnum.MONITORING.getCode(),
            true
        );

        assertThat(tasks).isEmpty();
    }

    @Test
    void missingSystemUserFailsBatchBuild() {
        TestContext context = context();
        when(context.sysUserService.selectUserByPhonenumber("13000000003")).thenReturn(null);

        assertThatThrownBy(() -> context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.HAZARD_REMOVAL, "排危除险措施")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        ))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("U001号斜坡单元的乡自规所所长绑定人员未关联有效系统用户");
    }

    @Test
    void missingSpecialManagerSystemUserFailsVillageSecretaryPlanBuild() {
        TestContext context = context();
        when(context.sysUserService.selectUserByPhonenumber("13000000004")).thenReturn(null);

        assertThatThrownBy(() -> context.delegate.buildEmergencyTasksInParallel(
            List.of(entry(PlanTypeEnum.RESETTLEMENT, "人员安置措施")),
            10L,
            UNIT_ID,
            20L,
            "测试地址",
            new Date(0L),
            null
        ))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("U001号斜坡单元的村支书绑定人员未关联有效系统用户");
    }

    @Test
    void aiVerifyReportTaskAssignsToTownNaturalResourceDirector() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        ISlopeUnitGridMemberRelationService relationService = mock(ISlopeUnitGridMemberRelationService.class);
        ISysUserService sysUserService = mock(ISysUserService.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzReportDisaster report = new DzReportDisaster();
        report.setId(100L);
        report.setSourceType(1);
        report.setProcessType(DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN);
        report.setStatus(1);
        report.setTaskId(55L);
        report.setCheckCenter("POINT(1 1)");
        report.setDetailedAddress("测试地址");
        when(reportMapper.selectByIdForUpdate(100L)).thenReturn(report);
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId(UNIT_ID);
        when(slopeUnitService.queryPoByCenter("POINT(1 1)")).thenReturn(slopeUnit);
        SlopeUnitVo slopeUnitVo = new SlopeUnitVo();
        slopeUnitVo.setId(UNIT_ID);
        slopeUnitVo.setDetailedAddress("测试地址");
        when(slopeUnitService.queryById(UNIT_ID)).thenReturn(slopeUnitVo);
        when(relationService.queryByUnitId(UNIT_ID)).thenReturn(relation());
        when(sysUserService.selectUserByPhonenumber("13000000003")).thenReturn(user(301L, "乡自规所所长", "13000000003"));
        when(riskAssessmentMapper.selectList(any())).thenReturn(List.of());
        when(chainNodeService.resolveSavedChainIdByTaskId(55L)).thenReturn("CHAIN-55");
        DzTaskProcessChainNode feedbackReportNode = new DzTaskProcessChainNode();
        feedbackReportNode.setId(99L);
        feedbackReportNode.setChainId("CHAIN-55");
        when(chainNodeService.resolveSavedNodeByBizAndCategory(
            TaskProcessBizTypeEnum.REPORT.getCode(), 100L,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())).thenReturn(feedbackReportNode);
        when(taskMapper.insert(any(DzTaskDistList.class))).thenAnswer(invocation -> {
            DzTaskDistList task = invocation.getArgument(0);
            task.setId(200L);
            return 1;
        });
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        DzReportDisasterServiceImpl service = new DzReportDisasterServiceImpl(
            reportMapper,
            taskMapper,
            slopeUnitService,
            relationService,
            sysUserService,
            mock(SmsSendService.class),
            mock(IDzUserAdRegionService.class),
            mock(ITaskSmsContentService.class),
            mock(cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver.class),
            riskAssessmentMapper,
            chainNodeService,
            mock(IDzTaskProcessChainSummaryService.class),
            mock(AiHostingOverviewNotifyService.class),
            environment
        );

        DzReportDisasterHandleVo handleVo = service.handle(100L);

        ArgumentCaptor<DzTaskDistList> taskCaptor = ArgumentCaptor.forClass(DzTaskDistList.class);
        verify(taskMapper).insert(taskCaptor.capture());
        DzTaskDistList task = taskCaptor.getValue();
        assertThat(handleVo.getDispatchTargetName()).isEqualTo("乡自规所所长");
        assertThat(task.getUserId()).isEqualTo(301L);
        assertThat(task.getResponsiblePerson()).isEqualTo("乡自规所所长");
        assertThat(task.getResponsiblePersonPhone()).isEqualTo("13000000003");
        assertThat(task.getTaskType()).isEqualTo(DzTaskDistList.TASK_TYPE_AI_VERIFY);
    }

    private static AbstractMap.SimpleEntry<PlanTypeEnum, String> entry(PlanTypeEnum planType, String content) {
        return new AbstractMap.SimpleEntry<>(planType, content);
    }

    private TestContext context() {
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        ISysUserService sysUserService = mock(ISysUserService.class);
        ISlopeUnitGridMemberRelationService relationService = mock(ISlopeUnitGridMemberRelationService.class);
        ITaskSmsContentService taskSmsContentService = mock(ITaskSmsContentService.class);
        SlopeUnitGridMemberRelationVo relation = relation();
        when(relationService.queryByUnitId(UNIT_ID)).thenReturn(relation);
        when(sysUserService.selectUserByPhonenumber("13000000002")).thenReturn(user(201L, "巡查员", "13000000002"));
        when(sysUserService.selectUserByPhonenumber("13000000003")).thenReturn(user(301L, "乡自规所所长", "13000000003"));
        when(sysUserService.selectUserByPhonenumber("13000000004")).thenReturn(user(401L, "专管员村支书", "13000000004"));

        DzTaskDistListServiceImpl service = new DzTaskDistListServiceImpl(
            taskMapper,
            mock(DzTaskDistListAddMapper.class),
            mock(DzTaskDistListRemarkMapper.class),
            mock(DzTaskDistListHistoryMapper.class),
            mock(DzTaskHandleMapper.class),
            sysUserService,
            mock(DzRiskAssessmentMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(IAppTaskService.class),
            mock(AppTaskProps.class),
            mock(ISlopeUnitService.class),
            relationService,
            mock(IDzUserAdRegionService.class),
            mock(IDzTaskHandleSceneRecordService.class),
            mock(IDzTaskHandleDetailContentService.class),
            mock(DzDefRespPlanMapper.class),
            mock(AdRegionMapper.class),
            mock(TransactionTemplate.class),
            taskSmsContentService,
            mock(SmsSendService.class),
            mock(IDzDefRespStartSmsConfigService.class),
            mock(SysRoleMapper.class),
            mock(SysUserMapper.class),
            mock(SysUserRoleMapper.class),
            mock(StringRedisTemplate.class),
            mock(IDzTaskProcessChainNodeService.class),
            mock(IDzTaskProcessChainSummaryService.class),
            mock(IDzRiskAssessmentWarningRelationService.class),
            mock(DifyAgentClient.class)
        );
        return new TestContext(service, new DzBatchGenerateDelegate(service), sysUserService);
    }

    private static SlopeUnitGridMemberRelationVo relation() {
        SlopeUnitGridMemberRelationVo relation = new SlopeUnitGridMemberRelationVo();
        relation.setUnitId(UNIT_ID);
        relation.setResponsiblePerson("旧责任人");
        relation.setResponsiblePersonPhone("13000000001");
        relation.setSpecialManager("专管员村支书");
        relation.setSpecialManagerPhone("13000000004");
        relation.setInspector("巡查员");
        relation.setInspectorPhone("13000000002");
        relation.setAdminUser("乡自规所所长");
        relation.setAdminUserPhone("13000000003");
        return relation;
    }

    private static SysUserVo user(Long id, String name, String phone) {
        SysUserVo user = new SysUserVo();
        user.setUserId(id);
        user.setUserName(name);
        user.setPhonenumber(phone);
        return user;
    }

    private record TestContext(DzTaskDistListServiceImpl service,
                               DzBatchGenerateDelegate delegate,
                               ISysUserService sysUserService) {
    }
}
