/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeAiHostingRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeMinuteSummary;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordGroupVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutoModeStatusVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeAiHostingRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSysConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleDetailContentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeAiHostingRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeExecutionTraceService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzAutoModeAiHostingLinkageTest {

    @Test
    void aiHostingRecordSessionVoMapsEntityFields() throws Exception {
        DzAutoModeAiHostingRecord entity = new DzAutoModeAiHostingRecord();
        entity.setId(30L);
        entity.setRecordName("20260627-001AI托管记录");

        Method mapper = DzAutoModeAiHostingRecordServiceImpl.class
            .getDeclaredMethod("toVo", DzAutoModeAiHostingRecord.class);
        mapper.setAccessible(true);
        AiHostingRecordSessionVo result = (AiHostingRecordSessionVo) mapper.invoke(
            new DzAutoModeAiHostingRecordServiceImpl(mock(DzAutoModeAiHostingRecordMapper.class)),
            entity
        );

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getRecordName()).isEqualTo("20260627-001AI托管记录");
    }

    @Test
    void minuteWindowTruncatesSecondsAndMillis() {
        Date input = Date.from(Instant.parse("2026-06-25T04:34:56.789Z"));

        Date result = DzAutoModeExecutionTraceServiceImpl.minuteWindow(input);

        assertThat(result.getTime()).isEqualTo(input.getTime() - input.getTime() % 60000L);
        assertThat(result.getTime() % 60000L).isZero();
    }

    @Test
    void deduplicateSortedRecordsKeepsFirstRecordForSameBusinessKey() {
        AiHostingRecordVo first = record("自动模式留痕", "已完成", 1, 100L, 200L,
            "已完成未推送任务自动推送", "newer", "2026-06-25T04:35:00Z");
        AiHostingRecordVo duplicate = record("自动模式留痕", "已完成", 1, 100L, 200L,
            "已完成未推送任务自动推送", "older", "2026-06-25T04:34:00Z");
        AiHostingRecordVo other = record("自动模式留痕", "执行失败", 1, 100L, 200L,
            "未推送任务自动推送执行失败", "failed", "2026-06-25T04:33:00Z");

        List<AiHostingRecordVo> result = DzAiHostingServiceImpl.deduplicateSortedRecords(List.of(first, duplicate, other));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("newer");
        assertThat(result.get(1).getDescription()).isEqualTo("failed");
        assertThat(result).extracting(AiHostingRecordVo::getBizType).containsOnly(1);
        assertThat(result.stream().map(item -> String.valueOf(item.getBizType())).toList())
            .doesNotContain("dz_task", "dz_report_disaster", "dz_task_handle", "TASK", "HANDLE");
    }

    @Test
    void normalizeLimitUsesDefaultAndUpperBound() {
        assertThat(DzAiHostingServiceImpl.normalizeLimit(null)).isEqualTo(50);
        assertThat(DzAiHostingServiceImpl.normalizeLimit(0)).isEqualTo(50);
        assertThat(DzAiHostingServiceImpl.normalizeLimit(-1)).isEqualTo(50);
        assertThat(DzAiHostingServiceImpl.normalizeLimit(20)).isEqualTo(20);
        assertThat(DzAiHostingServiceImpl.normalizeLimit(201)).isEqualTo(200);
    }

    @Test
    void normalizeRecordWindowMinutesUsesDefaultAndUpperBound() {
        assertThat(DzAiHostingServiceImpl.normalizeRecordWindowMinutes(null)).isEqualTo(1);
        assertThat(DzAiHostingServiceImpl.normalizeRecordWindowMinutes(0)).isEqualTo(1);
        assertThat(DzAiHostingServiceImpl.normalizeRecordWindowMinutes(5)).isEqualTo(5);
        assertThat(DzAiHostingServiceImpl.normalizeRecordWindowMinutes(2000)).isEqualTo(1440);
    }

    @Test
    void recordWindowTruncatesByConfiguredMinuteInterval() {
        Date input = Date.from(Instant.parse("2026-06-25T04:34:56.789Z"));

        Date result = DzAiHostingServiceImpl.truncateToRecordWindow(input, 5);

        assertThat(result).isEqualTo(Date.from(Instant.parse("2026-06-25T04:30:00Z")));
    }

    @Test
    void groupRecordsLimitsByTimestampWindow() {
        AiHostingRecordVo latest = record("自动模式留痕", "已完成", 1, 101L, 201L,
            "最近记录", "latest", "2026-06-25T04:14:59Z");
        AiHostingRecordVo sameWindow = record("自动模式留痕", "已完成", 1, 102L, 202L,
            "同窗口记录", "same window", "2026-06-25T04:10:30Z");
        AiHostingRecordVo previousWindow = record("自动模式留痕", "已完成", 1, 103L, 203L,
            "上一窗口记录", "previous window", "2026-06-25T04:08:59Z");
        AiHostingRecordVo excludedWindow = record("自动模式留痕", "已完成", 1, 104L, 204L,
            "被限制窗口记录", "excluded window", "2026-06-25T04:03:00Z");

        List<AiHostingRecordGroupVo> result = DzAiHostingServiceImpl.groupRecords(
            List.of(previousWindow, excludedWindow, latest, sameWindow),
            2,
            5
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTimestamp()).isEqualTo(Date.from(Instant.parse("2026-06-25T04:10:00Z")));
        assertThat(result.get(0).getRecords()).extracting(AiHostingRecordVo::getTitle)
            .containsExactly("最近记录", "同窗口记录");
        assertThat(result.get(0).getRecords()).extracting(AiHostingRecordVo::getTaskCount)
            .containsExactly(1, 1);
        assertThat(result.get(1).getTimestamp()).isEqualTo(Date.from(Instant.parse("2026-06-25T04:05:00Z")));
        assertThat(result.get(1).getRecords()).extracting(AiHostingRecordVo::getTitle)
            .containsExactly("上一窗口记录");
        assertThat(result.get(1).getRecords()).extracting(AiHostingRecordVo::getTaskCount)
            .containsExactly(1);
        assertThat(result.stream().flatMap(group -> group.getRecords().stream()).map(AiHostingRecordVo::getBizType).toList())
            .containsOnly(1);
    }

    @Test
    void groupRecordsMergesSameTitleWithinTimestampWindowAndSumsTaskCount() {
        AiHostingRecordVo latest = record("自动模式留痕", "已完成", 1, 101L, 201L,
            "同标题记录", "latest", "2026-06-25T04:14:59Z");
        latest.setTaskCount(2);
        AiHostingRecordVo sameTitleZeroCount = record("自动模式留痕", "已完成", 1, 102L, 202L,
            "同标题记录", "zero count", "2026-06-25T04:12:30Z");
        sameTitleZeroCount.setTaskCount(0);
        AiHostingRecordVo differentTitle = record("自动模式留痕", "已完成", 1, 103L, 203L,
            "不同标题记录", "different title", "2026-06-25T04:11:30Z");
        AiHostingRecordVo sameTitleOlder = record("自动模式留痕", "已完成", 1, 104L, 204L,
            "同标题记录", "older", "2026-06-25T04:10:30Z");
        sameTitleOlder.setTaskCount(3);
        AiHostingRecordVo sameTitleNoCount = record("自动模式留痕", "已完成", 1, 105L, 205L,
            "同标题记录", "no count", "2026-06-25T04:10:10Z");
        AiHostingRecordVo noCount = record("自动模式留痕", "已完成", 1, 106L, 206L,
            "无计数字段记录", "no count single", "2026-06-25T04:10:00Z");

        List<AiHostingRecordGroupVo> result = DzAiHostingServiceImpl.groupRecords(
            List.of(sameTitleOlder, differentTitle, latest, sameTitleZeroCount, sameTitleNoCount, noCount),
            10,
            5
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecords()).hasSize(3);
        assertThat(result.get(0).getRecords()).extracting(AiHostingRecordVo::getTitle)
            .containsExactly("同标题记录", "不同标题记录", "无计数字段记录");
        AiHostingRecordVo merged = result.get(0).getRecords().get(0);
        assertThat(merged.getDescription()).isEqualTo("latest");
        assertThat(merged.getTaskCount()).isEqualTo(6);
        assertThat(result.get(0).getRecords().get(2).getTaskCount()).isEqualTo(1);
        assertThat(latest.getTaskCount()).isEqualTo(2);
    }

    @Test
    void aiHostingRecordDurationUsesWholeNonNegativeMinutes() {
        Date start = Date.from(Instant.parse("2026-06-26T01:00:30Z"));
        Date end = Date.from(Instant.parse("2026-06-26T02:05:29Z"));
        Date beforeStart = Date.from(Instant.parse("2026-06-26T00:59:59Z"));

        assertThat(DzAutoModeAiHostingRecordServiceImpl.durationMinutes(start, end)).isEqualTo(64);
        assertThat(DzAutoModeAiHostingRecordServiceImpl.durationMinutes(start, beforeStart)).isZero();
        assertThat(DzAutoModeAiHostingRecordServiceImpl.durationMinutes(null, end)).isZero();
    }

    @Test
    void aiHostingRecordDurationTextUsesHourMinuteText() {
        assertThat(DzAutoModeAiHostingRecordServiceImpl.formatDurationText(null)).isEqualTo("0天0小时0分钟0秒");
        assertThat(DzAutoModeAiHostingRecordServiceImpl.formatDurationText(Integer.valueOf(-1))).isEqualTo("0天0小时0分钟0秒");
        assertThat(DzAutoModeAiHostingRecordServiceImpl.formatDurationText(Integer.valueOf(5))).isEqualTo("0天0小时5分钟0秒");
        assertThat(DzAutoModeAiHostingRecordServiceImpl.formatDurationText(Integer.valueOf(65))).isEqualTo("0天1小时5分钟0秒");
        assertThat(DzAutoModeAiHostingRecordServiceImpl.formatDurationText(90061L)).isEqualTo("1天1小时1分钟1秒");
    }

    @Test
    void overviewWithoutRecordIdUsesLatestRecordWhenPresent() {
        AiHostingRecordSessionVo latest = sessionRecord(10L, "2026-06-26T01:00:00Z", "2026-06-26T02:00:00Z");
        ServiceMocks mocks = serviceMocks();
        when(mocks.recordService.queryLatest()).thenReturn(latest);

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getHostingRecord().getId()).isEqualTo(10L);
        assertThat(result.getStartTime()).isEqualTo(latest.getOpenedAt());
        assertThat(result.getEndTime()).isEqualTo(latest.getClosedAt());
    }

    @Test
    void overviewWithoutRecordFallsBackToAutoModeStatusWindow() {
        Date openedAt = Date.from(Instant.parse("2026-06-26T03:00:00Z"));
        Date closedAt = Date.from(Instant.parse("2026-06-26T03:30:00Z"));
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(false);
        status.setOpenedAt(openedAt);
        status.setClosedAt(closedAt);
        ServiceMocks mocks = serviceMocks();
        when(mocks.recordService.queryLatest()).thenReturn(null);
        when(mocks.autoModeService.getStatus()).thenReturn(status);

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getHostingRecord()).isNull();
        assertThat(result.getStartTime()).isEqualTo(openedAt);
        assertThat(result.getEndTime()).isEqualTo(closedAt);
    }

    @Test
    void overviewSummaryIncludesEndedEventCount() {
        Date openedAt = Date.from(Instant.parse("2026-06-26T03:00:00Z"));
        Date closedAt = Date.from(Instant.parse("2026-06-26T03:30:00Z"));
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(false);
        status.setOpenedAt(openedAt);
        status.setClosedAt(closedAt);
        ServiceMocks mocks = serviceMocks();
        when(mocks.recordService.queryLatest()).thenReturn(null);
        when(mocks.autoModeService.getStatus()).thenReturn(status);
        when(mocks.chainNodeMapper.selectEndedMainChainCount(openedAt, closedAt)).thenReturn(3L);

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getSummary().getEndedEventCount()).isEqualTo(3L);
    }

    @Test
    void overviewSummaryAssistedCountOnlyCountsAutoAgentChainNodes() {
        Date openedAt = Date.from(Instant.parse("2026-06-26T03:00:00Z"));
        Date closedAt = Date.from(Instant.parse("2026-06-26T03:30:00Z"));
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(false);
        status.setOpenedAt(openedAt);
        status.setClosedAt(closedAt);
        ServiceMocks mocks = serviceMocks();
        when(mocks.recordService.queryLatest()).thenReturn(null);
        when(mocks.autoModeService.getStatus()).thenReturn(status);
        when(mocks.chainNodeMapper.selectCount(any())).thenReturn(2L);
        when(mocks.executionTraceService.sumSuccessfulExecutions(any(), any(), any())).thenReturn(5L);
        when(mocks.reportDisasterMapper.selectCount(any())).thenReturn(3L);
        when(mocks.detailContentMapper.selectCount(any())).thenReturn(4L);

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getSummary().getAssistedCount()).isEqualTo(2L);
        assertThat(result.getSummary().getAutoDispatchCount()).isEqualTo(5L);
        assertThat(result.getSummary().getAiVisionCompletedCount()).isEqualTo(3L);
        assertThat(result.getSummary().getGeneratedReportCount()).isEqualTo(4L);
    }

    @Test
    void endedMainChainCountSqlUsesRootClosedTerminalNodeScope() throws Exception {
        Method method = DzTaskProcessChainNodeMapper.class
            .getDeclaredMethod("selectEndedMainChainCount", Date.class, Date.class);

        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("root_chain_closed = 1");
        assertThat(sql).contains("COALESCE(NULLIF(chain_id, ''), NULLIF(root_chain_id, '')) AS main_chain_id");
        assertThat(sql).contains("node_category IN (8, 9, 13, 16)");
        assertThat(sql).contains("link_name LIKE '%任务关闭'");
        assertThat(sql).contains("link_name LIKE '%任务过期'");
        assertThat(sql).contains("灾险情关闭", "AI险情核实任务反馈", "现场处置任务反馈");
        assertThat(sql).contains("GROUP BY main_chain_id");
    }

    @Test
    void overviewRecentRecordsOnlyShowsAutoAgentChainNodes() {
        Date openedAt = Date.from(Instant.parse("2026-06-26T03:00:00Z"));
        Date closedAt = Date.from(Instant.parse("2026-06-26T03:30:00Z"));
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(false);
        status.setOpenedAt(openedAt);
        status.setClosedAt(closedAt);
        ServiceMocks mocks = serviceMocks();
        when(mocks.recordService.queryLatest()).thenReturn(null);
        when(mocks.autoModeService.getStatus()).thenReturn(status);
        when(mocks.chainNodeMapper.selectList(any())).thenReturn(List.of(
            agentChainNode("AI自动研判", "2026-06-26T03:20:00Z"),
            manualChainNode("人工会商研判", "2026-06-26T03:21:00Z")
        ));

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getRecentRecords()).hasSize(1);
        assertThat(result.getRecentRecords().get(0).getTitle()).isEqualTo("已完成AI自动研判");
    }

    @Test
    void listRecordsKeepsPagedSessionsAndLimitsGroupedHistoryPerSession() {
        ServiceMocks mocks = serviceMocks();
        PageQuery pageQuery = mock(PageQuery.class);
        AiHostingRecordSessionVo session = sessionRecord(20L, "2026-06-25T04:00:00Z", "2026-06-25T04:30:00Z");
        DzSysConfig config = new DzSysConfig();
        config.setConfigValue("5");
        when(mocks.recordService.queryPageList(pageQuery)).thenReturn(new TableDataInfo<>(List.of(session), 1L));
        when(mocks.sysConfigMapper.selectOne(any())).thenReturn(config);
        when(mocks.executionTraceService.listDisplayTraces(any(), any(), anyInt())).thenReturn(List.of(
            trace(1L, 0, "2026-06-25T04:14:59Z"),
            trace(2L, 0, "2026-06-25T04:10:30Z"),
            trace(3L, 0, "2026-06-25T04:08:59Z"),
            trace(4L, 0, "2026-06-25T04:03:00Z")
        ));

        TableDataInfo<AiHostingRecordSessionVo> result = mocks.service.listRecords(pageQuery, 2);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getData()).containsExactly(session);
        AiHostingRecordSessionVo row = result.getData().get(0);
        assertThat(row.getOverview()).isNotNull();
        assertThat(row.getOverview().getHostingRecord().getId()).isEqualTo(20L);
        assertThat(row.getOverview().getHostingRecord().getOverview()).isNull();
        assertThat(row.getHistoryRecords()).hasSize(2);
        assertThat(row.getHistoryRecords().get(0).getTimestamp()).isEqualTo(Date.from(Instant.parse("2026-06-25T04:10:00Z")));
        assertThat(row.getHistoryRecords().get(0).getRecords()).hasSize(1);
        assertThat(row.getHistoryRecords().get(0).getRecords().get(0).getTaskCount()).isEqualTo(2);
        assertThat(row.getHistoryRecords().get(1).getTimestamp()).isEqualTo(Date.from(Instant.parse("2026-06-25T04:05:00Z")));
        assertThat(row.getHistoryRecords().get(1).getRecords()).hasSize(1);
    }

    @Test
    void listRecordsMapsHistoryBizTypesToIntegers() {
        ServiceMocks mocks = serviceMocks();
        PageQuery pageQuery = mock(PageQuery.class);
        AiHostingRecordSessionVo session = sessionRecord(30L, "2026-06-26T03:00:00Z", "2026-06-26T03:30:00Z");
        DzSysConfig config = new DzSysConfig();
        config.setConfigValue("1");
        when(mocks.recordService.queryPageList(pageQuery)).thenReturn(new TableDataInfo<>(List.of(session), 1L));
        when(mocks.sysConfigMapper.selectOne(any())).thenReturn(config);
        when(mocks.executionTraceService.listSummaries(any(), any(), anyInt())).thenReturn(List.of(summary()));
        when(mocks.executionTraceService.listDisplayTraces(any(), any(), anyInt())).thenReturn(List.of(
            trace(31L, TaskProcessBizTypeEnum.HANDLE.getCode(), "2026-06-26T03:18:00Z")
        ));
        when(mocks.chainNodeMapper.selectList(any())).thenReturn(List.of(chainNode()));
        when(mocks.detailContentMapper.selectList(any())).thenReturn(List.of(taskHandleContent()));

        TableDataInfo<AiHostingRecordSessionVo> result = mocks.service.listRecords(pageQuery, 10);

        List<AiHostingRecordVo> records = result.getData().get(0).getHistoryRecords().stream()
            .flatMap(group -> group.getRecords().stream())
            .toList();
        assertThat(records).extracting(AiHostingRecordVo::getBizType).containsExactly(3, 3, 6, 1);
        assertThat(records).extracting(AiHostingRecordVo::getTaskId).containsExactly(null, null, 602L, null);
        assertThat(records).extracting(AiHostingRecordVo::getTaskCount).containsExactly(1, 1, 1, 2);
        assertThat(records.stream().map(item -> String.valueOf(item.getBizType())).toList())
            .doesNotContain("dz_task", "dz_report_disaster", "dz_task_handle", "TASK", "HANDLE");
    }

    @Test
    void overviewInProgressUsesIntegerBizTypesForRunningTraceReportAndTaskHandle() {
        ServiceMocks mocks = serviceMocks();
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(true);
        status.setOpenedAt(Date.from(Instant.parse("2026-06-26T03:00:00Z")));
        when(mocks.recordService.queryLatest()).thenReturn(null);
        when(mocks.autoModeService.getStatus()).thenReturn(status);
        when(mocks.executionTraceService.listRunningTraces()).thenReturn(List.of(
            runningTrace(41L, TaskProcessBizTypeEnum.TASK.getCode(), "2026-06-26T03:22:00Z")
        ));
        when(mocks.reportDisasterMapper.selectList(any())).thenReturn(List.of(inProgressReport()));
        when(mocks.taskHandleMapper.selectList(any())).thenReturn(List.of(investigationHandle()));

        AiHostingOverviewVo result = mocks.service.getOverview(null);

        assertThat(result.getInProgress()).extracting(AiHostingRecordVo::getBizType).containsExactly(3, 2, 1);
        assertThat(result.getInProgress()).extracting(AiHostingRecordVo::getTaskId).containsExactly(null, 802L, null);
        assertThat(result.getInProgress()).extracting(AiHostingRecordVo::getTaskCount).containsExactly(null, 1, null);
        assertThat(result.getInProgress().stream().map(item -> String.valueOf(item.getBizType())).toList())
            .doesNotContain("dz_task", "dz_report_disaster", "dz_task_handle", "TASK", "HANDLE");
    }

    @Test
    void resolveContentBizTypeMapsTaskHandleAndDefRespPlanToIntegerCodes() {
        assertThat(DzAiHostingServiceImpl.resolveContentBizType(DetailBizTypeEnum.TASK_HANDLE.getCode())).isEqualTo(3);
        assertThat(DzAiHostingServiceImpl.resolveContentBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode())).isEqualTo(4);
        assertThat(DzAiHostingServiceImpl.resolveContentBizType(99)).isZero();
    }

    @Test
    void resolveTaskCountPrefersSingleTaskIdThenFallsBackToExecuteCount() {
        assertThat(DzAiHostingServiceImpl.resolveTaskCount(100L, 5)).isEqualTo(1);
        assertThat(DzAiHostingServiceImpl.resolveTaskCount(null, 3)).isEqualTo(3);
        assertThat(DzAiHostingServiceImpl.resolveTaskCount(null, 0)).isNull();
    }

    @Test
    void failureMessageUsesRootCauseAndLimitsLength() {
        RuntimeException throwable = new RuntimeException(
            "top level message with sql",
            new IllegalStateException("root cause")
        );

        assertThat(DzAutoModeExecutionTraceServiceImpl.failureMessage(throwable)).isEqualTo("root cause");
        assertThat(DzAutoModeExecutionTraceServiceImpl.limitFailureReason("x".repeat(1001))).hasSize(1000);
    }

    private AiHostingRecordVo record(String source, String status, Integer bizType, Long bizId, Long taskId,
                                     String title, String description, String time) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setSource(source);
        record.setStatus(status);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setTaskId(taskId);
        record.setTitle(title);
        record.setDescription(description);
        record.setTime(Date.from(Instant.parse(time)));
        return record;
    }

    private DzAutoModeExecutionTrace trace(Long id, Integer bizType, String endedAt) {
        DzAutoModeExecutionTrace trace = new DzAutoModeExecutionTrace();
        trace.setId(id);
        trace.setStatus("SUCCESS");
        trace.setActionName("测试动作");
        trace.setBizType(bizType);
        trace.setExecuteCount(1);
        trace.setEndedAt(Date.from(Instant.parse(endedAt)));
        return trace;
    }

    private DzAutoModeExecutionTrace runningTrace(Long id, Integer bizType, String startedAt) {
        DzAutoModeExecutionTrace trace = new DzAutoModeExecutionTrace();
        trace.setId(id);
        trace.setActionName("执行中的测试动作");
        trace.setBizType(bizType);
        trace.setStartedAt(Date.from(Instant.parse(startedAt)));
        return trace;
    }

    private DzAutoModeMinuteSummary summary() {
        DzAutoModeMinuteSummary summary = new DzAutoModeMinuteSummary();
        summary.setId(51L);
        summary.setActionType(AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH);
        summary.setActionName(AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_NAME);
        summary.setExecuteCount(2);
        summary.setWindowStart(Date.from(Instant.parse("2026-06-26T03:10:00Z")));
        return summary;
    }

    private DzTaskProcessChainNode chainNode() {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(61L);
        node.setBizType(TaskProcessBizTypeEnum.MONITOR_WARNING.getCode());
        node.setBizId(601L);
        node.setTaskId(602L);
        node.setLinkName("监测预警发布");
        node.setTriggerReason("监测预警闭环");
        node.setTriggerTime(Date.from(Instant.parse("2026-06-26T03:16:00Z")));
        return node;
    }

    private DzTaskProcessChainNode agentChainNode(String linkName, String triggerTime) {
        DzTaskProcessChainNode node = chainNode();
        node.setOperatorId(DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID);
        node.setOperatorName(DzTaskDistListServiceImpl.AUTO_AGENT_NAME);
        node.setLinkName(linkName);
        node.setTriggerTime(Date.from(Instant.parse(triggerTime)));
        return node;
    }

    private DzTaskProcessChainNode manualChainNode(String linkName, String triggerTime) {
        DzTaskProcessChainNode node = chainNode();
        node.setOperatorId(100L);
        node.setOperatorName("人工用户");
        node.setLinkName(linkName);
        node.setTriggerTime(Date.from(Instant.parse(triggerTime)));
        return node;
    }

    private DzTaskHandleDetailContent taskHandleContent() {
        DzTaskHandleDetailContent content = new DzTaskHandleDetailContent();
        content.setId(71L);
        content.setBizId(701L);
        content.setBizType(DetailBizTypeEnum.TASK_HANDLE.getCode());
        content.setContentType(DetailContentTypeEnum.AI_REPORT.getCode());
        content.setPlanContent("报告内容");
        content.setDeleted(0);
        content.setUpdateDate(Date.from(Instant.parse("2026-06-26T03:20:00Z")));
        return content;
    }

    private DzReportDisaster inProgressReport() {
        DzReportDisaster report = new DzReportDisaster();
        report.setId(81L);
        report.setTaskId(802L);
        report.setSourceType(1);
        report.setStatus(1);
        report.setPhotos("oss://photo");
        report.setAiVisionProps(Map.of());
        report.setCreateDate(Date.from(Instant.parse("2026-06-26T03:23:00Z")));
        return report;
    }

    private DzTaskHandle investigationHandle() {
        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(91L);
        handle.setHandleProcess(HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode());
        handle.setUpdateDate(Date.from(Instant.parse("2026-06-26T03:24:30Z")));
        return handle;
    }

    private AiHostingRecordSessionVo sessionRecord(Long id, String openedAt, String closedAt) {
        AiHostingRecordSessionVo record = new AiHostingRecordSessionVo();
        record.setId(id);
        record.setRecordName("20260626-001AI托管记录");
        record.setOpenedAt(Date.from(Instant.parse(openedAt)));
        record.setClosedAt(closedAt == null ? null : Date.from(Instant.parse(closedAt)));
        record.setRunning(record.getClosedAt() == null);
        return record;
    }

    private ServiceMocks serviceMocks() {
        IDzAutoModeService autoModeService = mock(IDzAutoModeService.class);
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzReportDisasterMapper reportDisasterMapper = mock(DzReportDisasterMapper.class);
        DzSysConfigMapper sysConfigMapper = mock(DzSysConfigMapper.class);
        DzTaskHandleDetailContentMapper detailContentMapper = mock(DzTaskHandleDetailContentMapper.class);
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzTaskHandleApprovalMapper approvalMapper = mock(DzTaskHandleApprovalMapper.class);
        IDzAutoModeExecutionTraceService executionTraceService = mock(IDzAutoModeExecutionTraceService.class);
        IDzAutoModeAiHostingRecordService recordService = mock(IDzAutoModeAiHostingRecordService.class);
        when(chainNodeMapper.selectCount(any())).thenReturn(0L);
        when(chainNodeMapper.selectList(any())).thenReturn(List.of());
        when(chainNodeMapper.selectEndedMainChainCount(any(), any())).thenReturn(0L);
        when(reportDisasterMapper.selectCount(any())).thenReturn(0L);
        when(reportDisasterMapper.selectList(any())).thenReturn(List.of());
        when(detailContentMapper.selectCount(any())).thenReturn(0L);
        when(detailContentMapper.selectList(any())).thenReturn(List.of());
        when(taskHandleMapper.selectList(any())).thenReturn(List.of());
        when(approvalMapper.selectList(any())).thenReturn(List.of());
        when(executionTraceService.sumSuccessfulExecutions(any(), any(), any())).thenReturn(0L);
        when(executionTraceService.listRunningTraces()).thenReturn(List.of());
        when(executionTraceService.listSummaries(any(), any(), anyInt())).thenReturn(List.of());
        when(executionTraceService.listDisplayTraces(any(), any(), anyInt())).thenReturn(List.of());
        DzAiHostingServiceImpl service = new DzAiHostingServiceImpl(
            autoModeService,
            chainNodeMapper,
            reportDisasterMapper,
            sysConfigMapper,
            detailContentMapper,
            taskHandleMapper,
            approvalMapper,
            executionTraceService,
            recordService
        );
        return new ServiceMocks(service, autoModeService, executionTraceService, recordService, sysConfigMapper,
            chainNodeMapper, reportDisasterMapper, detailContentMapper, taskHandleMapper);
    }

    private record ServiceMocks(DzAiHostingServiceImpl service,
                                IDzAutoModeService autoModeService,
                                IDzAutoModeExecutionTraceService executionTraceService,
                                IDzAutoModeAiHostingRecordService recordService,
                                DzSysConfigMapper sysConfigMapper,
                                DzTaskProcessChainNodeMapper chainNodeMapper,
                                DzReportDisasterMapper reportDisasterMapper,
                                DzTaskHandleDetailContentMapper detailContentMapper,
                                DzTaskHandleMapper taskHandleMapper) {
    }
}
