/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainSegmentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessRiskLevelSourceEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainSummaryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainSummary;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainSummaryMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskProcessChainSummaryServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DzTaskProcessChainSummary.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DzTaskProcessChainSummary.class
            );
        }
    }

    @Test
    void latestProcessNodeUsesLikeForChainIdUnitIdAndResponsiblePerson() throws Exception {
        TaskProcessChainSummaryBo bo = new TaskProcessChainSummaryBo();
        bo.setChainId("20260629");
        bo.setUnitId("001");
        bo.setResponsiblePerson("张");

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(bo);
        String sqlSegment = wrapper.getSqlSegment();

        assertThat(sqlSegment)
            .contains("chain_id LIKE")
            .contains("unit_id LIKE")
            .contains("responsible_person LIKE")
            .doesNotContain("chain_id =")
            .doesNotContain("unit_id =");
    }

    @Test
    void latestProcessNodeSupportsRootChainClosedFilter() throws Exception {
        TaskProcessChainSummaryBo bo = new TaskProcessChainSummaryBo();
        bo.setRootChainClosed(1);

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(bo);

        assertThat(wrapper.getSqlSegment()).contains("root_chain_closed =");
    }

    @Test
    void latestProcessNodeSupportsTaskIdAndHandleIdFilters() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        when(chainNodeMapper.selectMainChainIdsByTaskId(1001L)).thenReturn(List.of("chain-main-1"));
        when(chainNodeMapper.selectMainChainIdsByHandleId(2002L, TaskProcessBizTypeEnum.HANDLE.getCode()))
            .thenReturn(List.of("chain-main-1", "chain-main-2"));

        TaskProcessChainSummaryBo bo = new TaskProcessChainSummaryBo();
        bo.setTaskId(1001L);
        bo.setHandleId(2002L);

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(bo, chainNodeMapper);

        assertThat(wrapper.getSqlSegment())
            .contains("chain_id IN")
            .doesNotContain("task_id =")
            .doesNotContain("handle_id =");
        assertThat(wrapper.getParamNameValuePairs())
            .containsValue("chain-main-1")
            .doesNotContainValue("chain-main-2");
        verify(chainNodeMapper).selectMainChainIdsByTaskId(1001L);
        verify(chainNodeMapper).selectMainChainIdsByHandleId(2002L, TaskProcessBizTypeEnum.HANDLE.getCode());
    }

    @Test
    void latestProcessNodeCreateDateUsesWholeDayRange() throws Exception {
        TaskProcessChainSummaryBo bo = new TaskProcessChainSummaryBo();
        bo.setCreateDate(DateUtil.parseDateTime("2026-07-02 10:30:45"));

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(bo);

        assertThat(wrapper.getSqlSegment())
            .contains("create_date >=")
            .contains("create_date <=");
        assertThat(wrapper.getParamNameValuePairs())
            .containsValue(DateUtil.beginOfDay(bo.getCreateDate()))
            .containsValue(DateUtil.endOfDay(bo.getCreateDate()));
    }

    @Test
    void latestProcessNodeReturnsEmptyWhenTaskIdMatchesNoChainNode() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        when(chainNodeMapper.selectMainChainIdsByTaskId(1001L)).thenReturn(List.of());

        TaskProcessChainSummaryBo bo = new TaskProcessChainSummaryBo();
        bo.setTaskId(1001L);

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(bo, chainNodeMapper);

        assertThat(wrapper.getSqlSegment())
            .contains("1 = 0")
            .doesNotContain("task_id =");
    }

    @Test
    void refreshReportRiskLevelIfCurrentRefreshesDisplayRowUsingReportRisk() {
        DzTaskProcessChainSummaryMapper summaryMapper = mock(DzTaskProcessChainSummaryMapper.class);
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzReportDisaster report = feedbackReport(10L, 100L, null, 3);
        DzTaskDistList task = task(100L, 10L);
        DzTaskProcessChainNode reportNode = node(1000L, "chain-1", TaskProcessBizTypeEnum.REPORT.getCode(), 10L,
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName());

        when(reportMapper.selectById(10L)).thenReturn(report);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(summaryMapper.selectList(any())).thenReturn(List.of());
        when(chainNodeMapper.selectList(any())).thenReturn(List.of(reportNode));
        when(chainNodeMapper.selectOne(any())).thenReturn(reportNode);
        when(summaryMapper.selectPendingPushTaskCountByChainId("chain-1")).thenReturn(0);
        DzTaskProcessChainSummary existing = new DzTaskProcessChainSummary();
        existing.setId(500L);
        when(summaryMapper.selectOne(any())).thenReturn(existing);
        DzTaskProcessChainSummaryServiceImpl service = newService(
            summaryMapper,
            chainNodeMapper,
            taskMapper,
            reportMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        service.refreshReportRiskLevelIfCurrent(10L);

        ArgumentCaptor<DzTaskProcessChainSummary> captor = ArgumentCaptor.forClass(DzTaskProcessChainSummary.class);
        verify(summaryMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRiskLevel()).isEqualTo(3);
        assertThat(captor.getValue().getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_AI.getCode());
    }

    @Test
    void refreshReportRiskLevelIfCurrentSkipsDisplayRowWhenLatestNodeUsesHandleRisk() {
        DzTaskProcessChainSummaryMapper summaryMapper = mock(DzTaskProcessChainSummaryMapper.class);
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        DzReportDisaster report = feedbackReport(10L, 100L, 4, 3);
        DzTaskDistList task = task(100L, 10L);
        DzTaskProcessChainNode reportNode = node(1000L, "chain-1", TaskProcessBizTypeEnum.REPORT.getCode(), 10L,
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName());
        DzTaskProcessChainSummary handleSummary = new DzTaskProcessChainSummary();
        handleSummary.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleSummary.setDisplayBizId(200L);
        DzTaskProcessChainNode latestHandleNode = node(1001L, "chain-1", TaskProcessBizTypeEnum.HANDLE.getCode(), 200L,
            TaskProcessBizTypeEnum.HANDLE.getCode(), 200L, TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName());
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(200L);
        handle.setEventLevel(1);

        when(reportMapper.selectById(10L)).thenReturn(report);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(summaryMapper.selectList(any())).thenReturn(List.of(), List.of(), List.of(handleSummary));
        when(chainNodeMapper.selectList(any())).thenReturn(List.of(reportNode));
        when(chainNodeMapper.selectOne(any())).thenReturn(latestHandleNode);
        when(handleMapper.selectById(200L)).thenReturn(handle);
        DzTaskProcessChainSummaryServiceImpl service = newService(
            summaryMapper,
            chainNodeMapper,
            taskMapper,
            reportMapper,
            handleMapper,
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        service.refreshReportRiskLevelIfCurrent(10L);

        verify(summaryMapper, never()).updateById(any(DzTaskProcessChainSummary.class));
        verify(summaryMapper, never()).insert(any(DzTaskProcessChainSummary.class));
    }

    @Test
    void resolveRootChainClosedReturnsClosedWhenAllLatestChainsFinished() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNode finishedTask = new DzTaskProcessChainNode();
        finishedTask.setLinkName("任务关闭");
        DzTaskProcessChainNode finishedHandle = new DzTaskProcessChainNode();
        finishedHandle.setLinkName("结束归档");
        when(chainNodeMapper.selectLatestNodesByChainTree("root-1"))
            .thenReturn(List.of(finishedTask, finishedHandle));

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("resolveRootChainClosed", String.class);
        method.setAccessible(true);

        Integer closed = (Integer) method.invoke(service, "root-1");

        assertThat(closed).isEqualTo(1);
    }

    @Test
    void resolveRootChainClosedUsesConfirmedRootClosedNodeSet() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        when(chainNodeMapper.selectLatestNodesByChainTree("root-terminal"))
            .thenReturn(List.of(
                node("日常巡查任务过期"),
                node("现场处置任务反馈"),
                node("AI险情核实任务反馈"),
                node("灾险情关闭")
            ));

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("resolveRootChainClosed", String.class);
        method.setAccessible(true);

        Integer closed = (Integer) method.invoke(service, "root-terminal");

        assertThat(closed).isEqualTo(1);
    }

    @Test
    void resolveRootChainClosedReturnsOpenWhenAnyLatestChainNotFinished() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNode finishedTask = new DzTaskProcessChainNode();
        finishedTask.setLinkName("任务关闭");
        DzTaskProcessChainNode runningTask = new DzTaskProcessChainNode();
        runningTask.setLinkName("任务核查中");
        when(chainNodeMapper.selectLatestNodesByChainTree("root-2"))
            .thenReturn(List.of(finishedTask, runningTask));

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("resolveRootChainClosed", String.class);
        method.setAccessible(true);

        Integer closed = (Integer) method.invoke(service, "root-2");

        assertThat(closed).isEqualTo(0);
    }

    private DzTaskProcessChainNode node(String linkName) {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(linkName);
        return node;
    }

    @Test
    void buildSummaryKeepsStageRiskButUsesSlopeDynamicRiskForReportNode() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(10L);
        task.setUnitId("0015");
        when(taskDistListMapper.selectById(10L)).thenReturn(task);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(20L);
        report.setManualRiskLevel(4);
        when(reportDisasterMapper.selectById(20L)).thenReturn(report);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(100L);
        assessment.setDynamicRiskLevel(2);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.selectById(100L)).thenReturn(assessment);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setBizId(20L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(10L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(4);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(2);
    }

    @Test
    void buildSummaryFallsBackToLatestSlopeDynamicRiskWhenTaskRiskIdMissing() throws Exception {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(30L);
        handle.setSlopeUnitId("15");
        handle.setEventLevel(4);
        when(taskHandleMapper.selectById(30L)).thenReturn(handle);

        DzRiskAssessment latestAssessment = new DzRiskAssessment();
        latestAssessment.setId(101L);
        latestAssessment.setDynamicRiskLevel(2);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(latestAssessment);
        when(riskAssessmentMapper.selectById(101L)).thenReturn(latestAssessment);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setBizId(30L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setDisplayBizId(30L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            taskHandleMapper,
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(4);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.HANDLE_EVENT_LEVEL.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(2);
    }

    @Test
    void buildSummaryMatchesSlopeUnitByReportCheckCenterWhenNoTaskOrHandleUnit() throws Exception {
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(50L);
        report.setCheckCenter("POINT(109.517924 30.631124)");
        when(reportDisasterMapper.selectById(50L)).thenReturn(report);

        SlopeUnit matched = new SlopeUnit();
        matched.setId("SU-001");
        when(slopeUnitService.queryPoByCenter(report.getCheckCenter())).thenReturn(matched);

        SlopeUnitVo slopeUnitVo = new SlopeUnitVo();
        slopeUnitVo.setId("SU-001");
        slopeUnitVo.setProvince("湖北省");
        slopeUnitVo.setCity("恩施州");
        slopeUnitVo.setCounty("恩施市");
        slopeUnitVo.setStreet("舞阳坝街道");
        slopeUnitVo.setVillage("测试村");
        slopeUnitVo.setPilotArea1(1);
        slopeUnitVo.setPilotArea2(0);
        when(slopeUnitService.queryNoWktByIds(List.of("SU-001"))).thenReturn(List.of(slopeUnitVo));

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setBizId(50L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setDisplayBizId(50L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class),
            slopeUnitService
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getUnitId()).isEqualTo("SU-001");
        assertThat(summary.getCounty()).isEqualTo("恩施市");
        assertThat(summary.getStreet()).isEqualTo("舞阳坝街道");
        assertThat(summary.getPilotArea1()).isEqualTo(1);
        assertThat(summary.getPilotArea2()).isZero();
    }

    @Test
    void buildSummaryUsesArchiveOperatorAsResponsiblePerson() throws Exception {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(30L);
        handle.setResponsiblePerson("处置负责人");
        handle.setResponsiblePersonPhone("13900000000");
        when(taskHandleMapper.selectById(30L)).thenReturn(handle);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName());
        node.setOperatorId(1001L);
        node.setOperatorName("值班员");
        node.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setBizId(30L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setDisplayBizId(30L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            taskHandleMapper,
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getOperatorName()).isEqualTo("值班员");
        assertThat(summary.getResponsiblePerson()).isEqualTo("值班员");
        assertThat(summary.getResponsiblePersonPhone()).isNull();
    }

    @Test
    void buildSummaryUsesTaskResponsiblePersonForTaskLifecycleNode() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistList task = new DzTaskDistList();
        task.setId(40L);
        task.setUserId(5001L);
        task.setResponsiblePerson("任务责任人");
        task.setResponsiblePersonPhone("13800000000");
        when(taskDistListMapper.selectById(40L)).thenReturn(task);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName());
        node.setOperatorId(1001L);
        node.setOperatorName("反馈操作人");
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(40L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(40L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getOperatorName()).isEqualTo("反馈操作人");
        assertThat(summary.getResponsiblePerson()).isEqualTo("任务责任人");
        assertThat(summary.getResponsiblePersonPhone()).isEqualTo("13800000000");
    }

    @Test
    void buildSummaryUsesNodeOperatorForNonTaskNodeEvenWhenTaskContextExists() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(40L);
        task.setResponsiblePerson("任务责任人");
        task.setResponsiblePersonPhone("13800000000");
        when(taskDistListMapper.selectById(40L)).thenReturn(task);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(50L);
        report.setTaskId(40L);
        report.setUserName("报告人");
        when(reportDisasterMapper.selectById(50L)).thenReturn(report);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName());
        node.setOperatorId(1001L);
        node.setOperatorName("节点操作人");
        node.setBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setBizId(50L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(40L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getResponsiblePerson()).isEqualTo("节点操作人");
        assertThat(summary.getResponsiblePersonPhone()).isNull();
    }

    @Test
    void buildSummaryPrefersManualRiskLevelForFeedbackReportNode() throws Exception {
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(60L);
        report.setManualRiskLevel(4);
        report.setAiRiskLevel(2);
        when(reportDisasterMapper.selectById(60L)).thenReturn(report);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName());
        node.setBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setBizId(60L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        node.setDisplayBizId(60L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(4);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
    }

    @Test
    void buildSummaryUsesFeedbackReportRiskBeforeHandleEventLevel() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(70L);
        when(taskDistListMapper.selectById(70L)).thenReturn(task);

        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(80L);
        handle.setEventLevel(4);
        when(taskHandleMapper.selectById(80L)).thenReturn(handle);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(90L);
        report.setTaskId(70L);
        report.setSourceType(1);
        report.setManualRiskLevel(2);
        when(reportDisasterMapper.selectOne(any())).thenReturn(report);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName());
        node.setTaskId(70L);
        node.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setBizId(80L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setDisplayBizId(80L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            reportDisasterMapper,
            taskHandleMapper,
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(2);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
    }

    @Test
    void buildSummaryPrefersLinkedAiReportRiskForAiVerifyDispatchNode() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(70L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        task.setReportId(80L);
        task.setUnitId("0015");
        when(taskDistListMapper.selectById(70L)).thenReturn(task);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(80L);
        report.setAiRiskLevel(3);
        when(reportDisasterMapper.selectById(80L)).thenReturn(report);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(90L);
        assessment.setDynamicRiskLevel(1);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.selectById(90L)).thenReturn(assessment);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName());
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(70L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(70L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(3);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_AI.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(1);
    }

    @Test
    void buildSummaryInheritsRiskLevelFromParentForTechAssistDispatchNode() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList originTask = new DzTaskDistList();
        originTask.setId(70L);
        originTask.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        originTask.setReportId(80L);
        originTask.setUnitId("0015");
        when(taskDistListMapper.selectById(70L)).thenReturn(originTask);

        DzTaskDistList techAssistTask = new DzTaskDistList();
        techAssistTask.setId(71L);
        techAssistTask.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        techAssistTask.setUnitId("0015");
        when(taskDistListMapper.selectById(71L)).thenReturn(techAssistTask);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(80L);
        report.setManualRiskLevel(3);
        when(reportDisasterMapper.selectById(80L)).thenReturn(report);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(90L);
        assessment.setDynamicRiskLevel(1);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.selectById(90L)).thenReturn(assessment);

        DzTaskProcessChainNode parentNode = new DzTaskProcessChainNode();
        parentNode.setId(100L);
        parentNode.setLinkName(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName());
        parentNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        parentNode.setBizId(70L);
        parentNode.setTaskId(70L);
        parentNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        parentNode.setDisplayBizId(70L);
        when(chainNodeMapper.selectById(100L)).thenReturn(parentNode);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(101L);
        node.setParentNodeId(100L);
        node.setLinkName(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName());
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(71L);
        node.setTaskId(71L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(71L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(3);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(1);
    }

    @Test
    void buildSummaryInheritsRiskLevelFromParentForTechAssistPushNode() throws Exception {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList originTask = new DzTaskDistList();
        originTask.setId(70L);
        originTask.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        originTask.setReportId(80L);
        originTask.setUnitId("0015");
        when(taskDistListMapper.selectById(70L)).thenReturn(originTask);

        DzTaskDistList techAssistTask = new DzTaskDistList();
        techAssistTask.setId(71L);
        techAssistTask.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        techAssistTask.setUnitId("0015");
        when(taskDistListMapper.selectById(71L)).thenReturn(techAssistTask);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(80L);
        report.setAiRiskLevel(3);
        when(reportDisasterMapper.selectById(80L)).thenReturn(report);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(90L);
        assessment.setDynamicRiskLevel(1);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.selectById(90L)).thenReturn(assessment);

        DzTaskProcessChainNode aiDispatchNode = new DzTaskProcessChainNode();
        aiDispatchNode.setId(100L);
        aiDispatchNode.setLinkName(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName());
        aiDispatchNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        aiDispatchNode.setBizId(70L);
        aiDispatchNode.setTaskId(70L);
        aiDispatchNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        aiDispatchNode.setDisplayBizId(70L);

        DzTaskProcessChainNode techDispatchNode = new DzTaskProcessChainNode();
        techDispatchNode.setId(101L);
        techDispatchNode.setParentNodeId(100L);
        techDispatchNode.setLinkName(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName());
        techDispatchNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        techDispatchNode.setBizId(71L);
        techDispatchNode.setTaskId(71L);
        techDispatchNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        techDispatchNode.setDisplayBizId(71L);

        when(chainNodeMapper.selectById(101L)).thenReturn(techDispatchNode);
        when(chainNodeMapper.selectById(100L)).thenReturn(aiDispatchNode);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(102L);
        node.setParentNodeId(101L);
        node.setLinkName("技术协查任务推送");
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(71L);
        node.setTaskId(71L);
        node.setSourceType(TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode());
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(71L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(3);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_AI.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(1);
    }

    @Test
    void buildSummaryPrefersLinkedReportRiskForAiVerifyFeedbackNode() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(70L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        task.setTaskType(DzTaskDistList.TASK_TYPE_AI_VERIFY);
        task.setReportId(80L);
        when(taskDistListMapper.selectById(70L)).thenReturn(task);

        DzReportDisaster report = new DzReportDisaster();
        report.setId(80L);
        report.setManualRiskLevel(4);
        report.setAiRiskLevel(2);
        when(reportDisasterMapper.selectById(80L)).thenReturn(report);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setLinkName("AI险情核实任务反馈");
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(70L);
        node.setTaskId(70L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(70L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            reportDisasterMapper,
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getCurrentStatus()).isEqualTo(6);
        assertThat(summary.getRiskLevel()).isEqualTo(4);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
    }

    @Test
    void buildSummaryUsesSlopeDynamicRiskForMonitorWarningNode() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(40L);
        task.setUnitId("0015");
        when(taskDistListMapper.selectById(40L)).thenReturn(task);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(100L);
        assessment.setDynamicRiskLevel(2);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.selectById(100L)).thenReturn(assessment);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setBizType(TaskProcessBizTypeEnum.MONITOR_WARNING.getCode());
        node.setBizId(50L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(40L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getRiskLevel()).isEqualTo(2);
        assertThat(summary.getRiskLevelSource()).isEqualTo(TaskProcessRiskLevelSourceEnum.RISK_ASSESSMENT.getCode());
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(2);
    }

    @Test
    void buildSummaryStoresPendingPushTaskCountFromChainScope() throws Exception {
        DzTaskProcessChainSummaryMapper summaryMapper = mock(DzTaskProcessChainSummaryMapper.class);
        when(summaryMapper.selectPendingPushTaskCountByChainId("chain-1")).thenReturn(2);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setChainId("chain-1");
        node.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        node.setBizId(30L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            summaryMapper,
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getPendingPushTaskCount()).isEqualTo(2);
    }

    @Test
    void buildSummaryResolvesDefAndHandleIdsFromChainButKeepsTaskRiskSnapshot() throws Exception {
        DzTaskProcessChainSummaryMapper summaryMapper = mock(DzTaskProcessChainSummaryMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        when(summaryMapper.selectLatestBizIdByChainIdAndType("chain-2", TaskProcessBizTypeEnum.DEF_RESP.getCode()))
            .thenReturn(300L);
        when(summaryMapper.selectLatestBizIdByChainIdAndType("chain-2", TaskProcessBizTypeEnum.HANDLE.getCode()))
            .thenReturn(null);

        DefRespPlan defRespPlan = new DefRespPlan();
        defRespPlan.setId(300L);
        defRespPlan.setHandleId(200L);
        when(defRespPlanMapper.selectById(300L)).thenReturn(defRespPlan);

        DzRiskAssessment taskAssessment = new DzRiskAssessment();
        taskAssessment.setId(997L);
        taskAssessment.setDynamicRiskLevel(3);
        when(riskAssessmentMapper.selectById(997L)).thenReturn(taskAssessment);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(10L);
        task.setUnitId("0015");
        task.setDefId(999L);
        task.setHandleId(998L);
        task.setRiskId(997L);
        when(taskDistListMapper.selectById(10L)).thenReturn(task);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setChainId("chain-2");
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(10L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            summaryMapper,
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            defRespPlanMapper,
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getDefId()).isEqualTo(300L);
        assertThat(summary.getHandleId()).isEqualTo(200L);
        assertThat(summary.getRiskId()).isEqualTo(997L);
        assertThat(summary.getRiskLevel()).isEqualTo(3);
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(3);
    }

    @Test
    void buildSummaryKeepsPreviousDayTaskRiskWhenTaskExpiresAfterLatestRiskChanged() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList previousDayTask = new DzTaskDistList();
        previousDayTask.setId(20L);
        previousDayTask.setUnitId("DXG-MG07");
        previousDayTask.setRiskId(200L);
        when(taskDistListMapper.selectById(20L)).thenReturn(previousDayTask);

        DzRiskAssessment previousDayAssessment = new DzRiskAssessment();
        previousDayAssessment.setId(200L);
        previousDayAssessment.setDynamicRiskLevel(3);
        when(riskAssessmentMapper.selectById(200L)).thenReturn(previousDayAssessment);

        DzRiskAssessment currentDayAssessment = new DzRiskAssessment();
        currentDayAssessment.setId(201L);
        currentDayAssessment.setDynamicRiskLevel(1);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(currentDayAssessment);

        DzTaskProcessChainNode overdueNode = new DzTaskProcessChainNode();
        overdueNode.setLinkName(TaskProcessChainNodeTextEnum.TASK_OVERDUE_BY_SYSTEM.getLinkName());
        overdueNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        overdueNode.setBizId(20L);
        overdueNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        overdueNode.setDisplayBizId(20L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, overdueNode);

        assertThat(summary.getRiskId()).isEqualTo(200L);
        assertThat(summary.getRiskLevel()).isEqualTo(3);
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(3);
        verify(riskAssessmentMapper, never()).selectOne(any());
    }

    @Test
    void buildSummaryFallsBackToLatestSlopeRiskWhenTaskRiskIdMissing() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(21L);
        task.setUnitId("DXG-MG08");
        when(taskDistListMapper.selectById(21L)).thenReturn(task);

        DzRiskAssessment latestAssessment = new DzRiskAssessment();
        latestAssessment.setId(202L);
        latestAssessment.setDynamicRiskLevel(2);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(latestAssessment);
        when(riskAssessmentMapper.selectById(202L)).thenReturn(latestAssessment);

        DzTaskProcessChainNode taskNode = new DzTaskProcessChainNode();
        taskNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        taskNode.setBizId(21L);
        taskNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        taskNode.setDisplayBizId(21L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        DzTaskProcessChainSummary summary = buildSummary(service, taskNode);

        assertThat(summary.getRiskId()).isEqualTo(202L);
        assertThat(summary.getRiskLevel()).isEqualTo(2);
        assertThat(summary.getDynamicRiskLevel()).isEqualTo(2);
    }

    @Test
    void buildSummaryStoresSubmitRequireSeparately() throws Exception {
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(66L);
        task.setSubmitRequire("提交要求A");
        when(taskDistListMapper.selectById(66L)).thenReturn(task);

        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(66L);
        node.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setDisplayBizId(66L);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );

        DzTaskProcessChainSummary summary = buildSummary(service, node);

        assertThat(summary.getSubmitRequire()).isEqualTo("提交要求A");
        assertThat(summary.getInspectingRequire()).isNull();
    }

    @Test
    void toVoUsesIndependentSubmitRequireWhenPresent() throws Exception {
        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        DzTaskProcessChainSummary summary = new DzTaskProcessChainSummary();
        summary.setInspectingRequire("巡查要求A");
        summary.setSubmitRequire("提交要求A");

        TaskProcessChainSummaryVo vo = toVo(service, summary);

        assertThat(vo.getInspectingRequire()).isEqualTo("巡查要求A");
        assertThat(vo.getSubmitRequire()).isEqualTo("提交要求A");
    }

    @Test
    void toVoFallsBackToInspectingRequireWhenSubmitRequireMissing() throws Exception {
        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            mock(DzTaskProcessChainNodeMapper.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        DzTaskProcessChainSummary summary = new DzTaskProcessChainSummary();
        summary.setInspectingRequire("历史要求A");

        TaskProcessChainSummaryVo vo = toVo(service, summary);

        assertThat(vo.getSubmitRequire()).isEqualTo("历史要求A");
    }

    @Test
    void refreshDynamicRiskLevelByRiskAssessmentRefreshesRiskAndUnitMatchedSummaries() {
        DzTaskProcessChainSummaryMapper summaryMapper = mock(DzTaskProcessChainSummaryMapper.class);
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskDistListMapper = mock(DzTaskDistListMapper.class);
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);

        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(100L);
        assessment.setSlopeUnitId("15");
        assessment.setDynamicRiskLevel(3);
        when(riskAssessmentMapper.selectById(100L)).thenReturn(assessment);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);

        DzTaskProcessChainSummary riskMatched = new DzTaskProcessChainSummary();
        riskMatched.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        riskMatched.setDisplayBizId(101L);
        riskMatched.setRiskId(100L);
        riskMatched.setUnitId("0015");
        DzTaskProcessChainSummary unitOnlyMatched = new DzTaskProcessChainSummary();
        unitOnlyMatched.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        unitOnlyMatched.setDisplayBizId(202L);
        unitOnlyMatched.setUnitId("15");
        when(summaryMapper.selectList(any())).thenReturn(List.of(riskMatched), List.of(unitOnlyMatched));

        DzTaskProcessChainNode taskNode = new DzTaskProcessChainNode();
        taskNode.setId(1001L);
        taskNode.setDisplayBizType(TaskProcessBizTypeEnum.TASK.getCode());
        taskNode.setDisplayBizId(101L);
        taskNode.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        taskNode.setBizId(101L);
        DzTaskProcessChainNode handleNode = new DzTaskProcessChainNode();
        handleNode.setId(1002L);
        handleNode.setDisplayBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setDisplayBizId(202L);
        handleNode.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setBizId(202L);
        when(chainNodeMapper.selectOne(any())).thenReturn(taskNode, handleNode);

        DzTaskDistList task = new DzTaskDistList();
        task.setId(101L);
        task.setUnitId("0015");
        task.setRiskId(100L);
        when(taskDistListMapper.selectById(101L)).thenReturn(task);

        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(202L);
        handle.setSlopeUnitId("15");
        handle.setEventLevel(4);
        when(taskHandleMapper.selectById(202L)).thenReturn(handle);

        DzTaskProcessChainSummary existingTaskSummary = new DzTaskProcessChainSummary();
        existingTaskSummary.setId(1L);
        DzTaskProcessChainSummary existingHandleSummary = new DzTaskProcessChainSummary();
        existingHandleSummary.setId(2L);
        when(summaryMapper.selectOne(any())).thenReturn(existingTaskSummary, existingHandleSummary);
        when(summaryMapper.updateById(any(DzTaskProcessChainSummary.class))).thenReturn(1);

        DzTaskProcessChainSummaryServiceImpl service = newService(
            summaryMapper,
            chainNodeMapper,
            taskDistListMapper,
            mock(DzReportDisasterMapper.class),
            taskHandleMapper,
            mock(DzDefRespPlanMapper.class),
            riskAssessmentMapper
        );

        service.refreshDynamicRiskLevelByRiskAssessment(100L);

        ArgumentCaptor<DzTaskProcessChainSummary> captor = ArgumentCaptor.forClass(DzTaskProcessChainSummary.class);
        verify(summaryMapper, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(DzTaskProcessChainSummary::getDisplayBizId, DzTaskProcessChainSummary::getDynamicRiskLevel)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(101L, 3),
                org.assertj.core.groups.Tuple.tuple(202L, 3)
            );
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<DzTaskProcessChainSummary> buildQueryWrapper(TaskProcessChainSummaryBo bo) throws Exception {
        return buildQueryWrapper(bo, mock(DzTaskProcessChainNodeMapper.class));
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<DzTaskProcessChainSummary> buildQueryWrapper(TaskProcessChainSummaryBo bo,
                                                                      DzTaskProcessChainNodeMapper chainNodeMapper)
        throws Exception {
        DzTaskProcessChainSummaryServiceImpl service = newService(
            mock(DzTaskProcessChainSummaryMapper.class),
            chainNodeMapper,
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class)
        );
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("buildQueryWrapper", TaskProcessChainSummaryBo.class);
        method.setAccessible(true);
        return (QueryWrapper<DzTaskProcessChainSummary>) method.invoke(service, bo);
    }

    private DzTaskProcessChainSummary buildSummary(DzTaskProcessChainSummaryServiceImpl service, DzTaskProcessChainNode node)
        throws Exception {
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("buildSummary", DzTaskProcessChainNode.class);
        method.setAccessible(true);
        return (DzTaskProcessChainSummary) method.invoke(service, node);
    }

    private DzReportDisaster feedbackReport(Long id, Long taskId, Integer manualRiskLevel, Integer aiRiskLevel) {
        DzReportDisaster report = new DzReportDisaster();
        report.setId(id);
        report.setSourceType(1);
        report.setTaskId(taskId);
        report.setManualRiskLevel(manualRiskLevel);
        report.setAiRiskLevel(aiRiskLevel);
        return report;
    }

    private DzTaskDistList task(Long id, Long reportId) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(id);
        task.setReportId(reportId);
        task.setDelete(0);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        task.setStatus(DzTaskDistList.STATUS_FEEDBACKED);
        return task;
    }

    private DzTaskProcessChainNode node(Long id, String chainId, Integer bizType, Long bizId,
                                        Integer displayBizType, Long displayBizId, String linkName) {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(id);
        node.setChainId(chainId);
        node.setRootChainId(chainId);
        node.setLinkName(linkName);
        node.setBizType(bizType);
        node.setBizId(bizId);
        node.setDisplayBizType(displayBizType);
        node.setDisplayBizId(displayBizId);
        node.setChainSegmentType(TaskProcessChainSegmentTypeEnum.MAIN_CHAIN.getCode());
        node.setDeleted(0);
        return node;
    }

    private TaskProcessChainSummaryVo toVo(DzTaskProcessChainSummaryServiceImpl service, DzTaskProcessChainSummary summary)
        throws Exception {
        Method method = DzTaskProcessChainSummaryServiceImpl.class
            .getDeclaredMethod("toVo", DzTaskProcessChainSummary.class);
        method.setAccessible(true);
        return (TaskProcessChainSummaryVo) method.invoke(service, summary);
    }

    private DzTaskProcessChainSummaryServiceImpl newService(DzTaskProcessChainSummaryMapper summaryMapper,
                                                            DzTaskProcessChainNodeMapper chainNodeMapper,
                                                            DzTaskDistListMapper taskDistListMapper,
                                                            DzReportDisasterMapper reportDisasterMapper,
                                                            DzTaskHandleMapper taskHandleMapper,
                                                            DzDefRespPlanMapper defRespPlanMapper,
                                                            DzRiskAssessmentMapper riskAssessmentMapper) {
        return newService(
            summaryMapper,
            chainNodeMapper,
            taskDistListMapper,
            reportDisasterMapper,
            taskHandleMapper,
            defRespPlanMapper,
            riskAssessmentMapper,
            mock(ISlopeUnitService.class)
        );
    }

    private DzTaskProcessChainSummaryServiceImpl newService(DzTaskProcessChainSummaryMapper summaryMapper,
                                                            DzTaskProcessChainNodeMapper chainNodeMapper,
                                                            DzTaskDistListMapper taskDistListMapper,
                                                            DzReportDisasterMapper reportDisasterMapper,
                                                            DzTaskHandleMapper taskHandleMapper,
                                                            DzDefRespPlanMapper defRespPlanMapper,
                                                            DzRiskAssessmentMapper riskAssessmentMapper,
                                                            ISlopeUnitService slopeUnitService) {
        return new DzTaskProcessChainSummaryServiceImpl(
            summaryMapper,
            chainNodeMapper,
            taskDistListMapper,
            reportDisasterMapper,
            taskHandleMapper,
            defRespPlanMapper,
            riskAssessmentMapper,
            slopeUnitService,
            mock(IDzUserAdRegionService.class)
        );
    }
}
