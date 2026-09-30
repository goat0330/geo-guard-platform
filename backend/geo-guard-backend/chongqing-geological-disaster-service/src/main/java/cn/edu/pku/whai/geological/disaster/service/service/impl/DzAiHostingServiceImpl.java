/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionStatus;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeMinuteSummary;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordGroupVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutoModeStatusVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSysConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleDetailContentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAiHostingService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeAiHostingRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeExecutionTraceService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import cn.edu.pku.whai.geological.disaster.service.utils.AiVisionStatusUtils;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 托管记录展示服务实现。
 */
@RequiredArgsConstructor
@Service
public class DzAiHostingServiceImpl implements IDzAiHostingService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;
    private static final int MAX_HISTORY_LIMIT = 200;
    private static final int RECENT_LIMIT = 10;
    private static final int PANEL_LIMIT = 10;
    private static final int QUERY_FACTOR = 3;
    private static final int GROUP_HISTORY_QUERY_FACTOR = 5;
    private static final int MAX_GROUP_HISTORY_QUERY_LIMIT = 1000;
    private static final int DEFAULT_RECORD_WINDOW_MINUTES = 1;
    private static final int MAX_RECORD_WINDOW_MINUTES = 1440;
    private static final int REPORT_STATUS_PENDING = 1;
    private static final int APPROVAL_STATUS_APPROVED = 1;
    private static final String KEY_RECORDS_WINDOW_MINUTES = "dizai.ai_hosting.records_window_minutes";
    private static final String STATUS_COMPLETED = "已完成";
    private static final String STATUS_IN_PROGRESS = "正在处理";
    private static final String STATUS_FAILED = "执行失败";
    private static final String STATUS_PENDING_CONFIRM = "待人工确认";
    private static final String SOURCE_CHAIN_NODE = "流程链路";
    private static final String SOURCE_AI_VISION = "AI识图";
    private static final String SOURCE_REPORT = "报告生成";
    private static final Integer BIZ_TYPE_UNKNOWN = TaskProcessBizTypeEnum.UNKNOWN.getCode();
    private static final Integer BIZ_TYPE_TASK = TaskProcessBizTypeEnum.TASK.getCode();
    private static final Integer BIZ_TYPE_REPORT = TaskProcessBizTypeEnum.REPORT.getCode();
    private static final Integer BIZ_TYPE_HANDLE = TaskProcessBizTypeEnum.HANDLE.getCode();
    private static final Integer BIZ_TYPE_DEF_RESP = TaskProcessBizTypeEnum.DEF_RESP.getCode();
    private static final Integer BIZ_TYPE_MONITOR_WARNING = TaskProcessBizTypeEnum.MONITOR_WARNING.getCode();
    private static final List<String> DISPATCH_LINK_NAMES = List.of(
        TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.PUBLIC_REPORT_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK.getLinkName(),
        TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName(),
        TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getLinkName(),
        TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(),
        TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH.getLinkName()
    );

    private final IDzAutoModeService dzAutoModeService;
    private final DzTaskProcessChainNodeMapper chainNodeMapper;
    private final DzReportDisasterMapper reportDisasterMapper;
    private final DzSysConfigMapper sysConfigMapper;
    private final DzTaskHandleDetailContentMapper detailContentMapper;
    private final DzTaskHandleMapper taskHandleMapper;
    private final DzTaskHandleApprovalMapper approvalMapper;
    private final IDzAutoModeExecutionTraceService executionTraceService;
    private final IDzAutoModeAiHostingRecordService aiHostingRecordService;

    /**
     * 查询 AI 托管概览数据。
     *
     * @return 托管运行状态、统计摘要、正在处理、最近记录和待确认记录
     */
    @Override
    public AiHostingOverviewVo getOverview(Long recordId) {
        if (recordId != null) {
            return getRecordOverview(recordId);
        }
        AiHostingRecordSessionVo latestRecord = aiHostingRecordService.queryLatest();
        if (latestRecord != null) {
            return getRecordOverview(latestRecord);
        }
        AutoModeStatusVo status = dzAutoModeService.getStatus();
        Date now = new Date();
        Date startTime = resolveStartTime(status, now);
        Date endTime = resolveEndTime(status, now);
        boolean running = status != null && Boolean.TRUE.equals(status.getEnabled());

        return buildOverview(startTime, endTime, running, null);
    }

    @Override
    public TableDataInfo<AiHostingRecordSessionVo> listRecords(PageQuery pageQuery, Integer limit) {
        TableDataInfo<AiHostingRecordSessionVo> tableData = aiHostingRecordService.queryPageList(pageQuery);
        List<AiHostingRecordSessionVo> records = tableData.getData();
        if (records == null || records.isEmpty()) {
            return tableData;
        }
        Date now = new Date();
        int groupLimit = normalizeLimit(limit);
        int windowMinutes = recordWindowMinutes();
        records.stream()
            .filter(Objects::nonNull)
            .forEach(record -> fillRecordDetail(record, groupLimit, windowMinutes, now));
        return tableData;
    }

    private AiHostingOverviewVo getRecordOverview(Long recordId) {
        AiHostingRecordSessionVo record = aiHostingRecordService.queryById(recordId);
        return getRecordOverview(record);
    }

    private AiHostingOverviewVo getRecordOverview(AiHostingRecordSessionVo record) {
        Date now = new Date();
        Date startTime = record.getOpenedAt();
        Date endTime = record.getClosedAt() == null ? now : record.getClosedAt();
        boolean running = record.getClosedAt() == null;
        return buildOverview(startTime, endTime, running, shallowRecord(record));
    }

    private void fillRecordDetail(AiHostingRecordSessionVo record, int groupLimit, int windowMinutes, Date now) {
        record.setOverview(getRecordOverview(record));
        Date endTime = record.getClosedAt() == null ? now : record.getClosedAt();
        Date startTime = record.getOpenedAt() == null ? DateUtil.beginOfDay(endTime) : record.getOpenedAt();
        int historyLimit = recordsHistoryQueryLimit(groupLimit);
        record.setHistoryRecords(groupRecords(listHistoryInternal(startTime, endTime, historyLimit), groupLimit, windowMinutes));
    }

    private AiHostingRecordSessionVo shallowRecord(AiHostingRecordSessionVo source) {
        if (source == null) {
            return null;
        }
        AiHostingRecordSessionVo target = new AiHostingRecordSessionVo();
        target.setId(source.getId());
        target.setRecordName(source.getRecordName());
        target.setOpenedAt(source.getOpenedAt());
        target.setClosedAt(source.getClosedAt());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setDurationText(source.getDurationText());
        target.setRunning(source.getRunning());
        target.setOpenUserId(source.getOpenUserId());
        target.setOpenUserName(source.getOpenUserName());
        target.setOpenUserRole(source.getOpenUserRole());
        target.setCloseUserId(source.getCloseUserId());
        target.setCloseUserName(source.getCloseUserName());
        target.setCloseUserRole(source.getCloseUserRole());
        return target;
    }

    private AiHostingOverviewVo buildOverview(Date startTime, Date endTime, boolean running, AiHostingRecordSessionVo hostingRecord) {
        AiHostingOverviewVo overview = new AiHostingOverviewVo();
        overview.setRunning(running);
        overview.setStatusText(running ? "运行中" : "已关闭");
        overview.setStartTime(startTime);
        overview.setEndTime(endTime);
        long durationSeconds = runningDurationSeconds(startTime, endTime);
        overview.setRunningDurationSeconds(durationSeconds);
        overview.setRunningDurationText(formatDurationText(durationSeconds));
        overview.setHostingRecord(hostingRecord);

        List<AiHostingRecordVo> pendingConfirmations = buildPendingConfirmations(MAX_HISTORY_LIMIT);
        AiHostingSummaryVo summary = buildSummary(startTime, endTime, pendingConfirmations.size());
        overview.setSummary(summary);
        overview.setInProgress(buildInProgress(PANEL_LIMIT));
        overview.setRecentRecords(listRecentAgentChainNodeRecords(startTime, endTime, RECENT_LIMIT));
        overview.setPendingConfirmations(limitRecords(pendingConfirmations, PANEL_LIMIT));
        return overview;
    }

    private AiHostingSummaryVo buildSummary(Date startTime, Date endTime, int pendingConfirmCount) {
        long agentChainNodeCount = countAgentChainNodesInWindow(startTime, endTime);
        long aiVisionCompletedCount = countAiVisionCompleted(startTime, endTime);
        long generatedReportCount = countGeneratedReports(startTime, endTime);

        AiHostingSummaryVo summary = new AiHostingSummaryVo();
        summary.setAssistedCount(agentChainNodeCount);
        summary.setAutoDispatchCount(countAutoDispatch(startTime, endTime));
        summary.setAiVisionCompletedCount(aiVisionCompletedCount);
        summary.setGeneratedReportCount(generatedReportCount);
        summary.setPendingConfirmCount((long) pendingConfirmCount);
        summary.setEndedEventCount(countEndedEvents(startTime, endTime));
        return summary;
    }

    private long countAgentChainNodesInWindow(Date startTime, Date endTime) {
        return chainNodeMapper.selectCount(applyNodeWindow(agentDisplayNodeQuery(), startTime, endTime));
    }

    private long countAutoDispatch(Date startTime, Date endTime) {
        return executionTraceService.sumSuccessfulExecutions(
            startTime,
            endTime,
            AutoModeExecutionActions.AUTO_DISPATCH_ACTION_TYPES
        );
    }

    private long countAiVisionCompleted(Date startTime, Date endTime) {
        return reportDisasterMapper.selectCount(applyReportWindow(aiVisionCompletedQuery(), startTime, endTime));
    }

    private long countGeneratedReports(Date startTime, Date endTime) {
        return detailContentMapper.selectCount(applyContentWindow(generatedReportQuery(), startTime, endTime));
    }

    private long countEndedEvents(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        Long count = chainNodeMapper.selectEndedMainChainCount(startTime, endTime);
        return count == null ? 0L : count;
    }

    private List<AiHostingRecordVo> buildInProgress(int limit) {
        List<AiHostingRecordVo> records = new ArrayList<>();
        records.addAll(queryAutoModeRunningRecords());
        records.addAll(queryAiVisionInProgress(Math.max(limit * QUERY_FACTOR, limit)));
        records.addAll(queryReportInProgress(Math.max(limit * QUERY_FACTOR, limit)));
        return sortDeduplicateLimit(records, limit);
    }

    private List<AiHostingRecordVo> queryAutoModeRunningRecords() {
        return executionTraceService.listRunningTraces().stream()
            .map(this::runningTraceRecord)
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryAiVisionInProgress(int limit) {
        LambdaQueryWrapper<DzReportDisaster> wrapper = Wrappers.<DzReportDisaster>lambdaQuery()
            .isNotNull(DzReportDisaster::getPhotos)
            .ne(DzReportDisaster::getPhotos, "")
            .isNull(DzReportDisaster::getAiRiskLevel)
            .and(w -> w.isNull(DzReportDisaster::getAiReportDetail)
                .or()
                .eq(DzReportDisaster::getAiReportDetail, ""))
            .and(w -> w.isNull(DzReportDisaster::getAiVisionProps)
                .or()
                .apply("ai_vision_props = '{}'::jsonb"))
            .orderByDesc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            .last("limit " + limit);
        return reportDisasterMapper.selectList(wrapper).stream()
            .filter(DzAiHostingServiceImpl::isAiVisionInProgress)
            .map(report -> reportRecord(report, "AI识图处理中", "等待AI识图回填风险等级与报告详情", STATUS_IN_PROGRESS))
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryReportInProgress(int queryLimit) {
        List<DzTaskHandle> handles = taskHandleMapper.selectList(
            Wrappers.<DzTaskHandle>lambdaQuery()
                .eq(DzTaskHandle::getHandleProcess, HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode())
                .orderByDesc(DzTaskHandle::getUpdateDate, DzTaskHandle::getCreateDate, DzTaskHandle::getId)
                .last("limit " + queryLimit)
        );
        if (handles.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> handleIds = handles.stream()
            .map(DzTaskHandle::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> generatedHandleIds = queryGeneratedReportHandleIds(handleIds);
        return handles.stream()
            .filter(handle -> handle.getId() != null && !generatedHandleIds.contains(handle.getId()))
            .map(handle -> taskHandleRecord(handle, "待生成应急调查报告初稿", "处置事件仍处于应急调查环节，等待AI生成调查报告", STATUS_IN_PROGRESS))
            .collect(Collectors.toList());
    }

    private Set<Long> queryGeneratedReportHandleIds(Set<Long> handleIds) {
        if (handleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return detailContentMapper.selectList(
                generatedReportQuery()
                    .in(DzTaskHandleDetailContent::getBizId, handleIds)
            ).stream()
            .map(DzTaskHandleDetailContent::getBizId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private List<AiHostingRecordVo> buildPendingConfirmations(int limit) {
        List<AiHostingRecordVo> records = new ArrayList<>();
        records.addAll(queryAiVisionFailedPending(limit));
        records.addAll(queryHighRiskReportPending(limit));
        records.addAll(queryGeneratedReportPending(limit));
        sortRecords(records);
        return limitRecords(records, limit);
    }

    private List<AiHostingRecordVo> queryAiVisionFailedPending(int limit) {
        LambdaQueryWrapper<DzReportDisaster> wrapper = aiVisionPresentCandidateQuery()
            .eq(DzReportDisaster::getStatus, REPORT_STATUS_PENDING)
            .orderByDesc(DzReportDisaster::getUpdateDate, DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            .last("limit " + Math.max(limit * QUERY_FACTOR, limit));
        return reportDisasterMapper.selectList(wrapper).stream()
            .filter(DzAiHostingServiceImpl::isAiVisionFailed)
            .map(report -> reportRecord(report, "AI识图失败待人工介入", "AI识图字段不完整或存在兜底结果，等待人工核实", STATUS_PENDING_CONFIRM))
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryHighRiskReportPending(int limit) {
        LambdaQueryWrapper<DzReportDisaster> wrapper = Wrappers.<DzReportDisaster>lambdaQuery()
            .in(DzReportDisaster::getAiRiskLevel, List.of(3, 4))
            .eq(DzReportDisaster::getStatus, REPORT_STATUS_PENDING)
            .orderByDesc(DzReportDisaster::getUpdateDate, DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            .last("limit " + Math.max(limit * QUERY_FACTOR, limit));
        return reportDisasterMapper.selectList(wrapper).stream()
            .filter(DzAiHostingServiceImpl::isAiVisionCompleted)
            .map(report -> reportRecord(report, "高风险AI识图待人工确认", "AI识图判定为高或极高风险，等待人工确认", STATUS_PENDING_CONFIRM))
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryGeneratedReportPending(int limit) {
        List<DzTaskHandleDetailContent> contents = detailContentMapper.selectList(
            generatedReportQuery()
                .orderByDesc(DzTaskHandleDetailContent::getUpdateDate, DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
                .last("limit " + limit)
        );
        if (contents.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DzTaskHandleDetailContent> latestByHandle = new LinkedHashMap<>();
        for (DzTaskHandleDetailContent content : contents) {
            if (content.getBizId() != null) {
                latestByHandle.putIfAbsent(content.getBizId(), content);
            }
        }
        if (latestByHandle.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> handleIds = latestByHandle.keySet();
        Map<Long, DzTaskHandle> handles = taskHandleMapper.selectList(
                Wrappers.<DzTaskHandle>lambdaQuery()
                    .in(DzTaskHandle::getId, handleIds)
                    .eq(DzTaskHandle::getHandleProcess, HandleProcessEnum.CONSULTATION_JUDGMENT.getCode())
            ).stream()
            .collect(Collectors.toMap(DzTaskHandle::getId, handle -> handle, (left, right) -> left));
        if (handles.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> approvedHandleIds = approvalMapper.selectList(
                Wrappers.<DzTaskHandleApproval>lambdaQuery()
                    .in(DzTaskHandleApproval::getHandleId, handles.keySet())
                    .eq(DzTaskHandleApproval::getProcess, HandleProcessEnum.CONSULTATION_JUDGMENT.getCode())
                    .eq(DzTaskHandleApproval::getType, DzTaskHandleApprovalTypeEnum.EXPERT_CONSULTATION.getCode())
                    .eq(DzTaskHandleApproval::getStatus, APPROVAL_STATUS_APPROVED)
            ).stream()
            .map(DzTaskHandleApproval::getHandleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<AiHostingRecordVo> records = new ArrayList<>();
        for (Map.Entry<Long, DzTaskHandleDetailContent> entry : latestByHandle.entrySet()) {
            Long handleId = entry.getKey();
            if (!handles.containsKey(handleId) || approvedHandleIds.contains(handleId)) {
                continue;
            }
            records.add(contentRecord(entry.getValue(), "应急调查报告待会商确认", "已生成应急调查报告初稿，等待会商研判确认", STATUS_PENDING_CONFIRM));
        }
        return records;
    }

    private List<AiHostingRecordVo> listHistoryInternal(Date startTime, Date endTime, int limit) {
        int queryLimit = Math.max(limit * QUERY_FACTOR, limit);
        List<AiHostingRecordVo> records = new ArrayList<>();
        records.addAll(queryAutoModeSummaryRecords(startTime, endTime, queryLimit));
        records.addAll(queryAutoModeTraceRecords(startTime, endTime, queryLimit));
        records.addAll(queryChainNodeRecords(startTime, endTime, queryLimit));
        records.addAll(queryAiVisionCompletedRecords(startTime, endTime, queryLimit));
        records.addAll(queryGeneratedReportRecords(startTime, endTime, queryLimit));
        return sortDeduplicateLimit(records, limit);
    }

    private List<AiHostingRecordVo> listRecentAgentChainNodeRecords(Date startTime, Date endTime, int limit) {
        int queryLimit = Math.max(limit * QUERY_FACTOR, limit);
        return sortDeduplicateLimit(queryAgentChainNodeRecords(startTime, endTime, queryLimit), limit);
    }

    private List<AiHostingRecordVo> queryAutoModeSummaryRecords(Date startTime, Date endTime, int limit) {
        return executionTraceService.listSummaries(startTime, endTime, limit).stream()
            .filter(this::shouldDisplaySummary)
            .map(this::summaryRecord)
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryAutoModeTraceRecords(Date startTime, Date endTime, int limit) {
        return executionTraceService.listDisplayTraces(startTime, endTime, limit).stream()
            .map(this::traceRecord)
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryChainNodeRecords(Date startTime, Date endTime, int limit) {
        return chainNodeMapper.selectList(
                applyNodeWindow(displayNodeQuery(), startTime, endTime)
                    .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                    .last("limit " + limit)
            ).stream()
            .map(this::chainRecord)
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryAgentChainNodeRecords(Date startTime, Date endTime, int limit) {
        return chainNodeMapper.selectList(
                applyNodeWindow(agentDisplayNodeQuery(), startTime, endTime)
                    .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                    .last("limit " + limit)
            ).stream()
            .filter(this::isAgentChainNode)
            .map(this::chainRecord)
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryAiVisionCompletedRecords(Date startTime, Date endTime, int limit) {
        return reportDisasterMapper.selectList(
                applyReportWindow(aiVisionCompletedQuery(), startTime, endTime)
                    .orderByDesc(DzReportDisaster::getUpdateDate, DzReportDisaster::getCreateDate, DzReportDisaster::getId)
                    .last("limit " + limit)
            ).stream()
            .map(report -> reportRecord(report, buildAiVisionTitle(report), "AI已回填识图结果", STATUS_COMPLETED))
            .collect(Collectors.toList());
    }

    private List<AiHostingRecordVo> queryGeneratedReportRecords(Date startTime, Date endTime, int limit) {
        return detailContentMapper.selectList(
                applyContentWindow(generatedReportQuery(), startTime, endTime)
                    .orderByDesc(DzTaskHandleDetailContent::getUpdateDate, DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
                    .last("limit " + limit)
            ).stream()
            .map(content -> contentRecord(content, buildReportTitle(content), "AI已生成报告内容", STATUS_COMPLETED))
            .collect(Collectors.toList());
    }

    private AiHostingRecordVo runningTraceRecord(DzAutoModeExecutionTrace trace) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(trace.getStartedAt(), trace.getCreateDate(), trace.getUpdateDate()));
        record.setTitle(StringUtils.blankToDefault(trace.getActionName(), "自动托管任务") + "正在处理");
        record.setDescription("自动模式正在执行该动作");
        record.setBizType(resolveTraceBizType(trace.getBizType()));
        record.setBizId(firstNonNull(trace.getBizId(), trace.getId()));
        record.setTaskId(trace.getTaskId());
        record.setTaskCount(resolveTaskCount(trace.getTaskId(), trace.getExecuteCount()));
        record.setSource(AutoModeExecutionActions.SOURCE_NAME);
        record.setStatus(STATUS_IN_PROGRESS);
        return record;
    }

    private AiHostingRecordVo summaryRecord(DzAutoModeMinuteSummary summary) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(summary.getWindowStart(), summary.getUpdateDate(), summary.getCreateDate()));
        record.setTitle("已完成" + StringUtils.blankToDefault(summary.getActionName(), "自动托管处理"));
        record.setDescription("本分钟成功执行 " + Math.max(0, Objects.requireNonNullElse(summary.getExecuteCount(), 0)) + " 次");
        record.setBizType(resolveSummaryBizType(summary.getActionType()));
        record.setBizId(summary.getId());
        record.setTaskCount(positiveCount(summary.getExecuteCount()));
        record.setSource(AutoModeExecutionActions.SOURCE_NAME);
        record.setStatus(STATUS_COMPLETED);
        return record;
    }

    private AiHostingRecordVo traceRecord(DzAutoModeExecutionTrace trace) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        boolean failed = Objects.equals(trace.getStatus(), AutoModeExecutionStatus.FAILED.getCode());
        record.setTime(firstNonNull(trace.getEndedAt(), trace.getStartedAt(), trace.getUpdateDate(), trace.getCreateDate()));
        record.setTitle(failed
            ? StringUtils.blankToDefault(trace.getActionName(), "自动托管处理") + "执行失败"
            : "已完成" + StringUtils.blankToDefault(trace.getActionName(), "自动托管处理"));
        record.setDescription(failed
            ? StringUtils.blankToDefault(trace.getFailureReason(), "自动模式动作执行失败")
            : "成功执行 " + Math.max(0, Objects.requireNonNullElse(trace.getExecuteCount(), 0)) + " 次");
        record.setBizType(resolveTraceBizType(trace.getBizType()));
        record.setBizId(firstNonNull(trace.getBizId(), trace.getId()));
        record.setTaskId(trace.getTaskId());
        record.setTaskCount(resolveTaskCount(trace.getTaskId(), trace.getExecuteCount()));
        record.setSource(AutoModeExecutionActions.SOURCE_NAME);
        record.setStatus(failed ? STATUS_FAILED : STATUS_COMPLETED);
        return record;
    }

    private AiHostingRecordVo chainRecord(DzTaskProcessChainNode node) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(node.getTriggerTime(), node.getCreateDate(), node.getUpdateDate()));
        record.setTitle(buildChainTitle(node.getLinkName()));
        record.setDescription(StringUtils.blankToDefault(node.getTriggerReason(), node.getLinkName()));
        record.setBizType(normalizeBizType(node.getBizType()));
        record.setBizId(node.getBizId());
        record.setTaskId(node.getTaskId());
        record.setTaskCount(resolveTaskCount(node.getTaskId(), null));
        record.setSource(StringUtils.blankToDefault(TaskProcessSourceTypeEnum.label(node.getSourceType()), SOURCE_CHAIN_NODE));
        record.setStatus(STATUS_COMPLETED);
        return record;
    }

    private AiHostingRecordVo reportRecord(DzReportDisaster report, String title, String description, String status) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(report.getUpdateDate(), report.getCreateDate(), report.getCheckTime()));
        record.setTitle(title);
        record.setDescription(description);
        record.setBizType(BIZ_TYPE_REPORT);
        record.setBizId(report.getId());
        record.setTaskId(report.getTaskId());
        record.setTaskCount(resolveTaskCount(report.getTaskId(), null));
        record.setSource(resolveReportSource(report.getSourceType()));
        record.setStatus(status);
        return record;
    }

    private AiHostingRecordVo taskHandleRecord(DzTaskHandle handle, String title, String description, String status) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(handle.getUpdateDate(), handle.getCreateDate(), handle.getReporterDate()));
        record.setTitle(title);
        record.setDescription(description);
        record.setBizType(BIZ_TYPE_HANDLE);
        record.setBizId(handle.getId());
        record.setTaskCount(resolveTaskCount(null, null));
        record.setSource(SOURCE_REPORT);
        record.setStatus(status);
        return record;
    }

    private AiHostingRecordVo contentRecord(DzTaskHandleDetailContent content, String title, String description, String status) {
        AiHostingRecordVo record = new AiHostingRecordVo();
        record.setTime(firstNonNull(content.getUpdateDate(), content.getCreateDate()));
        record.setTitle(title);
        record.setDescription(description);
        record.setBizType(resolveContentBizType(content.getBizType()));
        record.setBizId(content.getBizId());
        record.setTaskCount(resolveTaskCount(null, null));
        record.setSource(SOURCE_REPORT);
        record.setStatus(status);
        return record;
    }

    private LambdaQueryWrapper<DzTaskProcessChainNode> baseNodeQuery() {
        return Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getDeleted, 0);
    }

    private LambdaQueryWrapper<DzTaskProcessChainNode> displayNodeQuery() {
        return baseNodeQuery()
            .notIn(DzTaskProcessChainNode::getLinkName, DISPATCH_LINK_NAMES)
            .notLike(DzTaskProcessChainNode::getLinkName, "批量下发");
    }

    private LambdaQueryWrapper<DzTaskProcessChainNode> agentDisplayNodeQuery() {
        return displayNodeQuery()
            .eq(DzTaskProcessChainNode::getOperatorId, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID)
            .eq(DzTaskProcessChainNode::getOperatorName, DzTaskDistListServiceImpl.AUTO_AGENT_NAME);
    }

    private LambdaQueryWrapper<DzTaskProcessChainNode> applyNodeWindow(LambdaQueryWrapper<DzTaskProcessChainNode> wrapper, Date startTime, Date endTime) {
        return wrapper.and(w -> w.ge(DzTaskProcessChainNode::getTriggerTime, startTime)
            .le(DzTaskProcessChainNode::getTriggerTime, endTime)
            .or(inner -> inner.isNull(DzTaskProcessChainNode::getTriggerTime)
                .ge(DzTaskProcessChainNode::getCreateDate, startTime)
                .le(DzTaskProcessChainNode::getCreateDate, endTime)));
    }

    private LambdaQueryWrapper<DzReportDisaster> aiVisionCompletedQuery() {
        return Wrappers.<DzReportDisaster>lambdaQuery()
            .isNotNull(DzReportDisaster::getAiRiskLevel)
            .isNotNull(DzReportDisaster::getAiReportDetail)
            .ne(DzReportDisaster::getAiReportDetail, "")
            .isNotNull(DzReportDisaster::getAiVisionProps)
            .apply("ai_vision_props <> '{}'::jsonb")
            .apply("NOT (jsonb_exists(ai_vision_props, 'fallbackApplied')"
                + " OR jsonb_exists(ai_vision_props, 'fallbackReason')"
                + " OR jsonb_exists(ai_vision_props, 'fallbackMessage'))");
    }

    private LambdaQueryWrapper<DzReportDisaster> aiVisionPresentCandidateQuery() {
        return Wrappers.<DzReportDisaster>lambdaQuery()
            .and(w -> w.isNotNull(DzReportDisaster::getAiRiskLevel)
                .or()
                .isNotNull(DzReportDisaster::getAiReportDetail)
                .or()
                .isNotNull(DzReportDisaster::getAiVisionProps));
    }

    private LambdaQueryWrapper<DzReportDisaster> applyReportWindow(LambdaQueryWrapper<DzReportDisaster> wrapper, Date startTime, Date endTime) {
        return wrapper.and(w -> w.ge(DzReportDisaster::getUpdateDate, startTime)
            .le(DzReportDisaster::getUpdateDate, endTime)
            .or(inner -> inner.isNull(DzReportDisaster::getUpdateDate)
                .ge(DzReportDisaster::getCreateDate, startTime)
                .le(DzReportDisaster::getCreateDate, endTime)));
    }

    private LambdaQueryWrapper<DzTaskHandleDetailContent> generatedReportQuery() {
        return Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
            .eq(DzTaskHandleDetailContent::getBizType, DetailBizTypeEnum.TASK_HANDLE.getCode())
            .in(DzTaskHandleDetailContent::getContentType, List.of(
                DetailContentTypeEnum.AI_REPORT.getCode(),
                DetailContentTypeEnum.REVIEW_REPORT.getCode()
            ))
            .eq(DzTaskHandleDetailContent::getDeleted, 0)
            .and(w -> w.and(inner -> inner.isNotNull(DzTaskHandleDetailContent::getPlanContent)
                    .ne(DzTaskHandleDetailContent::getPlanContent, ""))
                .or(inner -> inner.isNotNull(DzTaskHandleDetailContent::getPlanContentJson)
                    .ne(DzTaskHandleDetailContent::getPlanContentJson, "")));
    }

    private LambdaQueryWrapper<DzTaskHandleDetailContent> applyContentWindow(LambdaQueryWrapper<DzTaskHandleDetailContent> wrapper, Date startTime, Date endTime) {
        return wrapper.and(w -> w.ge(DzTaskHandleDetailContent::getUpdateDate, startTime)
            .le(DzTaskHandleDetailContent::getUpdateDate, endTime)
            .or(inner -> inner.isNull(DzTaskHandleDetailContent::getUpdateDate)
                .ge(DzTaskHandleDetailContent::getCreateDate, startTime)
                .le(DzTaskHandleDetailContent::getCreateDate, endTime)));
    }

    /**
     * 解析本次托管窗口开始时间。
     *
     * @param status 自动模式状态
     * @param now 当前时间
     * @return 开始时间；无配置时回退到当天零点
     */
    static Date resolveStartTime(AutoModeStatusVo status, Date now) {
        Date start = null;
        if (status != null) {
            start = status.getOpenedAt() != null ? status.getOpenedAt() : status.getUpdatedAt();
        }
        if (start != null) {
            return start;
        }
        return DateUtil.beginOfDay(now == null ? new Date() : now);
    }

    /**
     * 解析本次托管窗口结束时间。
     *
     * @param status 自动模式状态
     * @param now 当前时间
     * @return 运行中返回当前时间；已关闭优先返回关闭时间，老数据回退最后更新时间
     */
    static Date resolveEndTime(AutoModeStatusVo status, Date now) {
        Date safeNow = now == null ? new Date() : now;
        if (status == null || Boolean.TRUE.equals(status.getEnabled())) {
            return safeNow;
        }
        if (status.getClosedAt() != null) {
            return status.getClosedAt();
        }
        if (status.getUpdatedAt() != null) {
            return status.getUpdatedAt();
        }
        return safeNow;
    }

    /**
     * 计算运行时长秒数。
     *
     * @param startTime 开始时间
     * @param now 当前时间
     * @return 非负运行秒数
     */
    static long runningDurationSeconds(Date startTime, Date now) {
        if (startTime == null || now == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(startTime.toInstant(), now.toInstant()).toSeconds());
    }

    /**
     * 格式化运行时长文案。
     *
     * @param seconds 秒数
     * @return 天小时分钟秒级时长文案
     */
    static String formatDurationText(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long days = safeSeconds / (24L * 60L * 60L);
        long hours = (safeSeconds % (24L * 60L * 60L)) / (60L * 60L);
        long minutes = (safeSeconds % (60L * 60L)) / 60L;
        long remainSeconds = safeSeconds % 60L;
        return days + "天" + hours + "小时" + minutes + "分钟" + remainSeconds + "秒";
    }

    /**
     * 规范化历史记录返回条数。
     *
     * @param limit 请求条数
     * @return 默认 50、最大 200 的安全条数
     */
    static int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_HISTORY_LIMIT;
        }
        return Math.min(limit, MAX_HISTORY_LIMIT);
    }

    static int normalizeRecordWindowMinutes(Integer minutes) {
        if (minutes == null || minutes < DEFAULT_RECORD_WINDOW_MINUTES) {
            return DEFAULT_RECORD_WINDOW_MINUTES;
        }
        return Math.min(minutes, MAX_RECORD_WINDOW_MINUTES);
    }

    static Date truncateToRecordWindow(Date time, int windowMinutes) {
        if (time == null) {
            return null;
        }
        long windowMillis = normalizeRecordWindowMinutes(windowMinutes) * 60_000L;
        return new Date(time.getTime() - Math.floorMod(time.getTime(), windowMillis));
    }

    static List<AiHostingRecordGroupVo> groupRecords(List<AiHostingRecordVo> records, int limit, int windowMinutes) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        int groupLimit = normalizeLimit(limit);
        List<AiHostingRecordVo> sorted = records.stream()
            .filter(Objects::nonNull)
            .filter(record -> record.getTime() != null)
            .sorted(DzAiHostingServiceImpl::compareRecordTimeDesc)
            .toList();
        Map<Long, AiHostingRecordGroupVo> grouped = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<String, AiHostingRecordVo>> groupedRecords = new LinkedHashMap<>();
        for (AiHostingRecordVo record : sorted) {
            Date timestamp = truncateToRecordWindow(record.getTime(), windowMinutes);
            if (timestamp == null) {
                continue;
            }
            long timestampMillis = timestamp.getTime();
            AiHostingRecordGroupVo group = grouped.get(timestampMillis);
            if (group == null) {
                if (grouped.size() >= groupLimit) {
                    continue;
                }
                group = new AiHostingRecordGroupVo();
                group.setTimestamp(timestamp);
                group.setRecords(new ArrayList<>());
                grouped.put(timestampMillis, group);
                groupedRecords.put(timestampMillis, new LinkedHashMap<>());
            }
            mergeRecordByTitle(groupedRecords.get(timestampMillis), record);
        }
        grouped.forEach((timestampMillis, group) ->
            group.setRecords(new ArrayList<>(groupedRecords.get(timestampMillis).values())));
        return new ArrayList<>(grouped.values());
    }

    private static void mergeRecordByTitle(Map<String, AiHostingRecordVo> recordsByTitle, AiHostingRecordVo record) {
        AiHostingRecordVo existing = recordsByTitle.get(record.getTitle());
        if (existing == null) {
            recordsByTitle.put(record.getTitle(), copyRecord(record));
            return;
        }
        existing.setTaskCount(sumTaskCount(existing.getTaskCount(), record.getTaskCount()));
    }

    private static AiHostingRecordVo copyRecord(AiHostingRecordVo source) {
        AiHostingRecordVo target = new AiHostingRecordVo();
        target.setTime(source.getTime() == null ? null : new Date(source.getTime().getTime()));
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setBizType(source.getBizType());
        target.setBizId(source.getBizId());
        target.setTaskId(source.getTaskId());
        target.setTaskCount(displayTaskCount(source.getTaskCount()));
        target.setSource(source.getSource());
        target.setStatus(source.getStatus());
        return target;
    }

    private static Integer sumTaskCount(Integer left, Integer right) {
        int sum = Objects.requireNonNullElse(displayTaskCount(left), 0)
            + Objects.requireNonNullElse(displayTaskCount(right), 0);
        return positiveCount(sum);
    }

    private static Integer displayTaskCount(Integer count) {
        if (count == null) {
            return 1;
        }
        return positiveCount(count);
    }

    static List<AiHostingRecordVo> deduplicateSortedRecords(List<AiHostingRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<String, AiHostingRecordVo> unique = new LinkedHashMap<>();
        for (AiHostingRecordVo record : records) {
            if (record == null) {
                continue;
            }
            unique.putIfAbsent(recordKey(record), record);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 根据链路环节名称生成展示标题。
     *
     * @param linkName 链路环节名称
     * @return 展示标题
     */
    static String buildChainTitle(String linkName) {
        if (StringUtils.isBlank(linkName)) {
            return "已完成自动托管处理";
        }
        String normalized = linkName.trim();
        if (normalized.startsWith("下发")) {
            return "已生成" + normalized.substring("下发".length());
        }
        if (normalized.startsWith("报送")) {
            return "已" + normalized;
        }
        if (normalized.endsWith("任务")) {
            return "已生成" + normalized;
        }
        return "已完成" + normalized;
    }

    private String buildAiVisionTitle(DzReportDisaster report) {
        return "已完成" + resolveReportSource(report == null ? null : report.getSourceType()) + "AI识图";
    }

    private String buildReportTitle(DzTaskHandleDetailContent content) {
        if (DetailContentTypeEnum.REVIEW_REPORT.getCode().equals(content.getContentType())) {
            return "已生成复盘报告初稿";
        }
        return "已生成应急调查报告初稿";
    }

    private String resolveReportSource(Integer sourceType) {
        if (Objects.equals(sourceType, 1)) {
            return "现场反馈";
        }
        if (Objects.equals(sourceType, 2)) {
            return TaskProcessChainNodeTextEnum.PUBLIC_REPORT.getLinkName();
        }
        return SOURCE_AI_VISION;
    }

    static Integer resolveContentBizType(Integer bizType) {
        if (DetailBizTypeEnum.TASK_HANDLE.getCode().equals(bizType)) {
            return BIZ_TYPE_HANDLE;
        }
        if (DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bizType)) {
            return BIZ_TYPE_DEF_RESP;
        }
        return BIZ_TYPE_UNKNOWN;
    }

    static Integer resolveTraceBizType(Integer bizType) {
        return normalizeBizType(bizType);
    }

    static Integer resolveSummaryBizType(Integer actionType) {
        if (actionType == null) {
            return BIZ_TYPE_UNKNOWN;
        }
        if (Objects.equals(actionType, AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_CODE)
            || Objects.equals(actionType, AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_CODE)
            || Objects.equals(actionType, AutoModeExecutionActions.TASK_AUTO_REMIND_CODE)
            || Objects.equals(actionType, AutoModeExecutionActions.DAILY_PATROL_GENERATE_CODE)) {
            return BIZ_TYPE_TASK;
        }
        if (Objects.equals(actionType, AutoModeExecutionActions.REPORT_AUTO_HANDLE_CODE)
            || Objects.equals(actionType, AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_CODE)) {
            return BIZ_TYPE_REPORT;
        }
        if (Objects.equals(actionType, AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_CODE)) {
            return BIZ_TYPE_HANDLE;
        }
        if (Objects.equals(actionType, AutoModeExecutionActions.RISK_PREDICTION_AUTO_PUSH_CODE)) {
            return BIZ_TYPE_MONITOR_WARNING;
        }
        return BIZ_TYPE_UNKNOWN;
    }

    static Integer normalizeBizType(Integer bizType) {
        return TaskProcessBizTypeEnum.of(bizType).getCode();
    }

    static Integer resolveTaskCount(Long taskId, Integer executeCount) {
        if (taskId != null) {
            return 1;
        }
        return positiveCount(executeCount);
    }

    private static Integer positiveCount(Integer count) {
        if (count == null || count < 1) {
            return null;
        }
        return count;
    }

    static boolean isAiVisionInProgress(DzReportDisaster report) {
        return AiVisionStatusUtils.isInProgress(report);
    }

    static boolean isAiVisionFailed(DzReportDisaster report) {
        return AiVisionStatusUtils.isFailed(report);
    }

    static boolean isAiVisionCompleted(DzReportDisaster report) {
        return AiVisionStatusUtils.isCompleted(report);
    }

    private boolean isAgentChainNode(DzTaskProcessChainNode node) {
        return node != null
            && Objects.equals(node.getOperatorId(), DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID)
            && Objects.equals(node.getOperatorName(), DzTaskDistListServiceImpl.AUTO_AGENT_NAME);
    }

    private boolean shouldDisplaySummary(DzAutoModeMinuteSummary summary) {
        return summary != null
            && !Objects.equals(summary.getActionType(), AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_CODE);
    }

    private void sortRecords(List<AiHostingRecordVo> records) {
        records.sort(DzAiHostingServiceImpl::compareRecordTimeDesc);
    }

    private static int compareRecordTimeDesc(AiHostingRecordVo left, AiHostingRecordVo right) {
        return compareDateDesc(
            left == null ? null : left.getTime(),
            right == null ? null : right.getTime()
        );
    }

    private static int compareDateDesc(Date left, Date right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int recordWindowMinutes() {
        DzSysConfig config = sysConfigMapper.selectOne(
            Wrappers.<DzSysConfig>lambdaQuery()
                .eq(DzSysConfig::getConfigKey, KEY_RECORDS_WINDOW_MINUTES)
                .last("limit 1")
        );
        if (config == null || StringUtils.isBlank(config.getConfigValue())) {
            return DEFAULT_RECORD_WINDOW_MINUTES;
        }
        try {
            return normalizeRecordWindowMinutes(Integer.parseInt(config.getConfigValue().trim()));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RECORD_WINDOW_MINUTES;
        }
    }

    private static int recordsHistoryQueryLimit(int groupLimit) {
        return Math.min(MAX_GROUP_HISTORY_QUERY_LIMIT, Math.max(groupLimit, groupLimit * GROUP_HISTORY_QUERY_FACTOR));
    }

    private List<AiHostingRecordVo> sortDeduplicateLimit(List<AiHostingRecordVo> records, int limit) {
        sortRecords(records);
        return limitRecords(deduplicateSortedRecords(records), limit);
    }

    private List<AiHostingRecordVo> limitRecords(List<AiHostingRecordVo> records, int limit) {
        if (records.size() <= limit) {
            return records;
        }
        return new ArrayList<>(records.subList(0, limit));
    }

    private static String recordKey(AiHostingRecordVo record) {
        String bizKey = String.valueOf(record.getBizType()) + "|" + record.getBizId() + "|" + record.getTaskId();
        if (record.getBizType() != null && (record.getBizId() != null || record.getTaskId() != null)) {
            return record.getSource() + "|" + record.getStatus() + "|" + bizKey + "|" + record.getTitle();
        }
        long time = record.getTime() == null ? 0L : record.getTime().getTime();
        return record.getSource() + "|" + record.getStatus() + "|" + record.getTitle() + "|" + time;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
