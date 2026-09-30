/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class TaskSmsContentServiceImplTest {

    @Test
    void emergencyMonitoringSmsUsesInspectorSubtypeAndCurrentResponsiblePerson() {
        TaskSmsContentServiceImpl service = service();
        DzTaskDistList task = emergencyMonitoringTask("测试巡查员", "18771950949");
        task.setInspectionSuggestion("1. 落实群测群防监测网络，明确测试自规所长（电话：18771954444）为责任人，负责巡查和数据上报；");

        String content = service.generateSmsContent(task);

        assertThat(content).contains("请测试巡查员立即");
        assertThat(content).contains("任务类型：群测群防巡查");
        assertThat(content).contains("明确测试巡查员（电话：18771950949）为责任人");
        assertThat(content).doesNotContain("明确测试自规所长（电话：18771954444）为责任人");
    }

    @Test
    void emergencyMonitoringSmsUsesInstrumentSubtypeForTownNaturalResourceDirector() {
        TaskSmsContentServiceImpl service = service();
        DzTaskDistList task = emergencyMonitoringTask("测试自规所长", "18771954444");
        task.setPlanType(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        task.setInspectionSuggestion("1. 落实群测群防监测网络，明确测试自规所长（电话：18771954444）为责任人，负责巡查和数据上报；");

        String content = service.generateSmsContent(task);

        assertThat(content).contains("请测试自规所长立即");
        assertThat(content).contains("任务类型：仪器监测");
        assertThat(content).contains("明确测试自规所长（电话：18771954444）为责任人");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsSummarizesRiskCountsAndLimitsLocations() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        List<SlopeUnit> slopeUnits = new ArrayList<>();
        List<DzRiskAssessment> risks = new ArrayList<>();
        List<DzTaskDistList> tasks = new ArrayList<>();
        Date now = new Date();
        for (int i = 1; i <= 6; i++) {
            SlopeUnit slopeUnit = new SlopeUnit();
            slopeUnit.setId("U" + i);
            slopeUnit.setStreet("乡镇" + i);
            slopeUnit.setVillage("村" + i);
            slopeUnits.add(slopeUnit);

            DzRiskAssessment risk = new DzRiskAssessment();
            risk.setId(100L + i);
            risk.setDynamicRiskLevel(i <= 2 ? 4 : 3);
            risk.setCreateDate(now);
            risks.add(risk);

            DzTaskDistList task = new DzTaskDistList();
            task.setId((long) i);
            task.setUnitId("U" + i);
            task.setRiskId(100L + i);
            task.setCreateDate(now);
            task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
            task.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);
            task.setResponsiblePerson(switch (i) {
                case 1 -> "巡查员甲";
                case 2 -> " 巡查员甲 ";
                case 5 -> "巡查员乙";
                case 6 -> "巡查员丙";
                default -> null;
            });
            tasks.add(task);
        }
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(slopeUnits);
        when(riskMapper.selectByIds(anyList())).thenReturn(risks);
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("共有6个巡查、监测任务");
        assertThat(content).contains("巡查任务6个、监测任务0个");
        assertThat(content).contains("极高风险2处、高风险4处");
        assertThat(content).contains("乡镇1村1U1号斜坡单元");
        assertThat(content).contains("乡镇1村1U1号斜坡单元等6处");
        assertThat(content).doesNotContain("乡镇6村6U6号斜坡单元");
        assertThat(content).contains("涉及人员：巡查员：巡查员甲（电话待核定）、待核定、巡查员乙（电话待核定）、巡查员丙（电话待核定）。");
        assertThat(content).doesNotContain("监测员：");
        assertThat(content).doesNotContain("{{");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsLimitsInspectorNamesAndShowsTotal() {
        List<DzTaskDistList> tasks = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            DzTaskDistList task = new DzTaskDistList();
            task.setId((long) i);
            task.setResponsiblePerson("巡查员" + i);
            task.setCreateDate(new Date());
            task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
            task.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);
            tasks.add(task);
        }

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("涉及人员：巡查员：巡查员1（电话待核定）、巡查员2（电话待核定）、巡查员3（电话待核定）、巡查员4（电话待核定）、巡查员5（电话待核定）等12人。");
        assertThat(content).doesNotContain("巡查员6、巡查员7");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsFallsBackWhenInspectorNameMissing() {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(1L);
        task.setCreateDate(new Date());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        task.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(List.of(task));

        assertThat(content).contains("重点点位：相关斜坡单元。");
        assertThat(content).contains("涉及人员：巡查员：待核定。");
        assertThat(content).doesNotContain("监测员：");
    }

    @Test
    void patrolMonitorDirectorGroupedSmsSeparatesRolesAndDeduplicatesRiskUnits() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit firstUnit = slopeUnit("U1", "乡镇1", "村1");
        SlopeUnit secondUnit = slopeUnit("U2", "乡镇2", "村2");
        DzRiskAssessment extremeRisk = risk(101L, 4);
        DzRiskAssessment highRisk = risk(102L, 3);
        DzTaskDistList patrolTask = directorTask(1L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_DEF_RESP, DzTaskDistList.PLAN_NAME_PATROL, "巡查员甲");
        DzTaskDistList duplicateUnitMonitorTask = directorTask(2L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_DEF_RESP, DzTaskDistList.PLAN_NAME_MONITOR, "监测员甲");
        DzTaskDistList secondMonitorTask = directorTask(3L, "U2", 102L,
            DzTaskDistList.SOURCE_TYPE_DEF_RESP, DzTaskDistList.PLAN_NAME_MONITOR, " 监测员甲 ");
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(firstUnit, secondUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(extremeRisk, highRisk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(
            List.of(patrolTask, duplicateUnitMonitorTask, secondMonitorTask));

        assertThat(content).contains("共有3个巡查、监测任务");
        assertThat(content).contains("巡查任务1个、监测任务2个");
        assertThat(content).contains("极高风险1处、高风险1处");
        assertThat(content).contains("涉及人员：巡查员：巡查员甲（电话待核定）；监测员：监测员甲（电话待核定）。");
        assertThat(content).contains("乡镇1村1U1号斜坡单元等2处");
    }

    @Test
    void patrolMonitorDirectorGroupedSmsShowsSameContactIndependentlyForEachRole() {
        DzTaskDistList patrolTask = directorTaskWithPhone(1L, "张三", "13800138000");
        DzTaskDistList monitorTask = directorTaskWithPhone(2L, "张三", "13800138000");
        monitorTask.setPlanName(DzTaskDistList.PLAN_NAME_MONITOR);

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(List.of(patrolTask, monitorTask));

        assertThat(content).contains("巡查任务1个、监测任务1个");
        assertThat(content).contains("涉及人员：巡查员：张三（电话：13800138000）；监测员：张三（电话：13800138000）。");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsShowsSingleLocationWithout等1处() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit unit = slopeUnit("U1", "乡镇1", "村1");
        DzRiskAssessment risk = risk(101L, 4);
        DzTaskDistList task = directorTask(9L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员甲");
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(unit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(risk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(List.of(task));

        assertThat(content).contains("重点点位：乡镇1村1U1号斜坡单元。");
        assertThat(content).doesNotContain("等1处");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsSortsLocationsByRiskThenTaskId() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit firstUnit = slopeUnit("U1", "乡镇1", "村1");
        SlopeUnit secondUnit = slopeUnit("U2", "乡镇2", "村2");
        DzRiskAssessment highRisk = risk(101L, 3);
        DzTaskDistList laterIdTask = directorTask(20L, "U2", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员乙");
        DzTaskDistList earlierIdTask = directorTask(10L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员甲");
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(firstUnit, secondUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(highRisk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(List.of(laterIdTask, earlierIdTask));

        assertThat(content).contains("重点点位：乡镇1村1U1号斜坡单元等2处");
        assertThat(content).doesNotContain("乡镇2村2U2号斜坡单元、");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsPlacesExtremeRiskUnitFirstRegardlessOfInputOrder() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit highRiskUnit = slopeUnit("U1", "乡镇1", "村1");
        SlopeUnit extremeRiskUnit = slopeUnit("U2", "乡镇2", "村2");
        DzRiskAssessment highRisk = risk(101L, 3);
        DzRiskAssessment extremeRisk = risk(102L, 4);
        DzTaskDistList highRiskTask = directorTask(20L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员甲");
        DzTaskDistList extremeRiskTask = directorTask(10L, "U2", 102L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员乙");
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(highRiskUnit, extremeRiskUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(highRisk, extremeRisk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(
            List.of(highRiskTask, extremeRiskTask));

        assertThat(content).contains("重点点位：乡镇2村2U2号斜坡单元等2处");
        assertThat(content).doesNotContain("乡镇1村1U1号斜坡单元");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsDeduplicatesTrimmedUnitsAndIgnoresInvalidUnits() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit unit = slopeUnit("U1", "乡镇1", "村1");
        DzRiskAssessment risk = risk(101L, 4);
        DzTaskDistList first = directorTask(1L, " U1 ", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员甲");
        DzTaskDistList blank = directorTask(2L, "  ", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员乙");
        DzTaskDistList duplicate = directorTask(3L, "U1", 101L,
            DzTaskDistList.SOURCE_TYPE_EVAL, DzTaskDistList.PLAN_NAME_DAILY_PATROL, "巡查员丙");
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(unit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(risk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generatePatrolMonitorDirectorGroupedSmsContent(List.of(first, blank, duplicate));

        assertThat(content).contains("极高风险1处、高风险0处");
        assertThat(content).contains("重点点位：乡镇1村1U1号斜坡单元。");
        assertThat(content).doesNotContain("等1处");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsFormatsAllContactCombinations() {
        List<DzTaskDistList> tasks = new ArrayList<>();
        tasks.add(directorTaskWithPhone(1L, "张三", "13800138000"));
        tasks.add(directorTaskWithPhone(2L, "李四", null));
        tasks.add(directorTaskWithPhone(3L, "李四", "13900139000"));
        tasks.add(directorTaskWithPhone(4L, null, "13600136000"));
        tasks.add(directorTaskWithPhone(5L, null, null));

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("涉及人员：巡查员：张三（电话：13800138000）、李四（电话待核定）、李四（电话：13900139000）、待核定（电话：13600136000）、待核定。");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsShowsSameNameWithDifferentPhones() {
        List<DzTaskDistList> tasks = List.of(
            directorTaskWithPhone(1L, "张三", "13800138000"),
            directorTaskWithPhone(2L, "张三", "13900139000")
        );

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("涉及人员：巡查员：张三（电话：13800138000）、张三（电话：13900139000）。");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsNormalizesPhonesAndFillsMissingName() {
        List<DzTaskDistList> tasks = new ArrayList<>();
        tasks.add(directorTaskWithPhone(1L, null, " 13800138000 "));
        tasks.add(directorTaskWithPhone(2L, " 张三 ", "13800138000"));
        tasks.add(directorTaskWithPhone(3L, "李四", "13800138000"));
        tasks.add(directorTaskWithPhone(4L, "王五", null));
        tasks.add(directorTaskWithPhone(5L, " 王五 ", null));
        tasks.add(directorTaskWithPhone(6L, "王五", "13900139000"));

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("涉及人员：巡查员：张三（电话：13800138000）、王五（电话待核定）、王五（电话：13900139000）。");
        assertThat(content).doesNotContain("李四");
    }

    @Test
    void dailyPatrolDirectorGroupedSmsLimitsFiveUniqueContactsAndCountsDeduplicatedTotal() {
        List<DzTaskDistList> tasks = new ArrayList<>();
        tasks.add(directorTaskWithPhone(1L, "甲", "13800000001"));
        tasks.add(directorTaskWithPhone(2L, "甲", "13800000001"));
        tasks.add(directorTaskWithPhone(3L, "乙", "13800000002"));
        tasks.add(directorTaskWithPhone(4L, "丙", "13800000003"));
        tasks.add(directorTaskWithPhone(5L, "丁", "13800000004"));
        tasks.add(directorTaskWithPhone(6L, "戊", "13800000005"));
        tasks.add(directorTaskWithPhone(7L, "己", "13800000006"));
        tasks.add(directorTaskWithPhone(8L, "己", "13800000006"));

        String content = service().generatePatrolMonitorDirectorGroupedSmsContent(tasks);

        assertThat(content).contains("涉及人员：巡查员：甲（电话：13800000001）、乙（电话：13800000002）、丙（电话：13800000003）、丁（电话：13800000004）、戊（电话：13800000005）等6人。");
        assertThat(content).doesNotContain("己（电话：13800000006）");
    }

    @Test
    void dailyPatrolGroupedSmsDoesNotRenderDefaultTextWhenNoHighRiskUnitExists() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId("HTX-HTX14");
        slopeUnit.setStreet("红土乡");
        slopeUnit.setVillage("红土溪村");
        DzRiskAssessment risk = new DzRiskAssessment();
        risk.setId(101L);
        risk.setDynamicRiskLevel(1);
        risk.setCreateDate(new Date());
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId("HTX-HTX14");
        task.setRiskId(101L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(slopeUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(risk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generateGroupedSmsContent(List.of(task));

        assertThat(content).contains("），请您在今日巡查、排查、核查中");
        assertThat(content).contains("红土乡红土溪村HTX-HTX14号斜坡单元");
        assertThat(content).doesNotContain("暂无请您");
    }

    @Test
    void dailyPatrolGroupedSmsKeepsKeypointAreaSentenceWhenHighRiskUnitExists() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId("HTX-HTX14");
        slopeUnit.setStreet("红土乡");
        slopeUnit.setVillage("红土溪村");
        DzRiskAssessment risk = new DzRiskAssessment();
        risk.setId(102L);
        risk.setDynamicRiskLevel(3);
        risk.setCreateDate(new Date());
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId("HTX-HTX14");
        task.setRiskId(102L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(slopeUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(risk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generateGroupedSmsContent(List.of(task));

        assertThat(content).contains("需要高度关注的地质灾害风险区集中在红土溪村，请您在今日巡查、排查、核查中");
        assertThat(content).doesNotContain("暂无请您");
    }

    @Test
    void monitorWarningSingleSmsHighlightsRiskIncreaseReason() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId("HTX-HTX14");
        slopeUnit.setCounty("恩施市");
        slopeUnit.setStreet("红土乡");
        slopeUnit.setVillage("红土溪村");
        DzRiskAssessment risk = new DzRiskAssessment();
        risk.setId(201L);
        risk.setDynamicRiskLevel(3);
        risk.setCreateDate(new Date());
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId("HTX-HTX14");
        task.setRiskId(201L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING);
        when(slopeUnitService.listPoByIds(List.of("HTX-HTX14"))).thenReturn(List.of(slopeUnit));
        when(riskMapper.selectById(201L)).thenReturn(risk);
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generateSmsContent(task);

        assertThat(content).contains("监测预警任务");
        assertThat(content).contains("因监测设备发出预警");
        assertThat(content).contains("恩施市红土乡红土溪村HTX-HTX14号斜坡单元动态风险等级已上升为高风险");
        assertThat(content).contains("请您立即开展现场监测巡查");
        assertThat(content).doesNotContain("{{");
    }

    @Test
    void monitorWarningGroupedSmsListsEveryUnitAndResultingRiskLevel() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzRiskAssessmentMapper riskMapper = mock(DzRiskAssessmentMapper.class);
        SlopeUnit highRiskUnit = new SlopeUnit();
        highRiskUnit.setId("HTX-HTX14");
        highRiskUnit.setStreet("红土乡");
        highRiskUnit.setVillage("红土溪村");
        SlopeUnit extremeRiskUnit = new SlopeUnit();
        extremeRiskUnit.setId("LFX-LFX01");
        extremeRiskUnit.setStreet("龙凤镇");
        extremeRiskUnit.setVillage("龙凤村");
        DzRiskAssessment highRisk = new DzRiskAssessment();
        highRisk.setId(301L);
        highRisk.setDynamicRiskLevel(3);
        highRisk.setCreateDate(new Date());
        DzRiskAssessment extremeRisk = new DzRiskAssessment();
        extremeRisk.setId(302L);
        extremeRisk.setDynamicRiskLevel(4);
        extremeRisk.setCreateDate(new Date());
        DzTaskDistList highRiskTask = monitorWarningTask("HTX-HTX14", 301L);
        DzTaskDistList extremeRiskTask = monitorWarningTask("LFX-LFX01", 302L);
        when(slopeUnitService.listPoByIds(anyList())).thenReturn(List.of(highRiskUnit, extremeRiskUnit));
        when(riskMapper.selectByIds(anyList())).thenReturn(List.of(highRisk, extremeRisk));
        TaskSmsContentServiceImpl service = service(slopeUnitService, riskMapper, mock(DzTaskHandleMapper.class));

        String content = service.generateGroupedSmsContent(List.of(highRiskTask, extremeRiskTask));

        assertThat(content).contains("监测预警任务");
        assertThat(content).contains("因监测设备发出预警，您负责的以下斜坡单元动态风险等级已上升");
        assertThat(content).contains("红土乡红土溪村HTX-HTX14号斜坡单元（已上升为高风险）");
        assertThat(content).contains("龙凤镇龙凤村LFX-LFX01号斜坡单元（已上升为极高风险）");
        assertThat(content).doesNotContain("需要高度关注的地质灾害风险区");
        assertThat(content).doesNotContain("{{");
    }

    private TaskSmsContentServiceImpl service() {
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId("DXG-MG06");
        slopeUnit.setCounty("恩施市");
        slopeUnit.setStreet("沐抚办事处");
        slopeUnit.setVillage("木贡村");
        when(slopeUnitService.listPoByIds(List.of("DXG-MG06"))).thenReturn(List.of(slopeUnit));
        DzTaskHandle handle = new DzTaskHandle();
        handle.setCounty("恩施市");
        handle.setStreet("沐抚办事处");
        when(handleMapper.selectById(20260703141620L)).thenReturn(handle);
        return service(slopeUnitService, mock(DzRiskAssessmentMapper.class), handleMapper);
    }

    private TaskSmsContentServiceImpl service(ISlopeUnitService slopeUnitService,
                                              DzRiskAssessmentMapper riskMapper,
                                              DzTaskHandleMapper handleMapper) {
        return new TaskSmsContentServiceImpl(
            slopeUnitService,
            mock(ISlopeUnitGridMemberRelationService.class),
            riskMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzDefRespPlanMapper.class),
            handleMapper
        );
    }

    private DzTaskDistList emergencyMonitoringTask(String responsiblePerson, String responsiblePersonPhone) {
        DzTaskDistList task = new DzTaskDistList();
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setPlanType(DzTaskDistList.PLAN_TYPE_MONITORING);
        task.setUnitId("DXG-MG06");
        task.setHandleId(20260703141620L);
        task.setResponsiblePerson(responsiblePerson);
        task.setResponsiblePersonPhone(responsiblePersonPhone);
        task.setSubmitRequire("2小时内反馈");
        return task;
    }

    private DzTaskDistList monitorWarningTask(String unitId, Long riskId) {
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId(unitId);
        task.setRiskId(riskId);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING);
        return task;
    }

    private DzTaskDistList directorTask(Long id, String unitId, Long riskId,
                                        Integer sourceType, String planName, String responsiblePerson) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(id);
        task.setUnitId(unitId);
        task.setRiskId(riskId);
        task.setSourceType(sourceType);
        task.setPlanName(planName);
        task.setResponsiblePerson(responsiblePerson);
        task.setCreateDate(new Date());
        return task;
    }

    private DzTaskDistList directorTaskWithPhone(Long id, String responsiblePerson, String responsiblePersonPhone) {
        DzTaskDistList task = directorTask(id, null, null,
            DzTaskDistList.SOURCE_TYPE_DEF_RESP, DzTaskDistList.PLAN_NAME_PATROL, responsiblePerson);
        task.setResponsiblePersonPhone(responsiblePersonPhone);
        return task;
    }

    private SlopeUnit slopeUnit(String unitId, String street, String village) {
        SlopeUnit slopeUnit = new SlopeUnit();
        slopeUnit.setId(unitId);
        slopeUnit.setStreet(street);
        slopeUnit.setVillage(village);
        return slopeUnit;
    }

    private DzRiskAssessment risk(Long riskId, Integer riskLevel) {
        DzRiskAssessment risk = new DzRiskAssessment();
        risk.setId(riskId);
        risk.setDynamicRiskLevel(riskLevel);
        risk.setCreateDate(new Date());
        return risk;
    }
}
