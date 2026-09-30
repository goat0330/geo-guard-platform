/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionStatus;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AutoModeTraceContext;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeMinuteSummary;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeExecutionTraceMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeMinuteSummaryMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeExecutionTraceService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动模式执行留痕内部服务实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzAutoModeExecutionTraceServiceImpl implements IDzAutoModeExecutionTraceService {

    static final Integer STATUS_RUNNING = AutoModeExecutionStatus.RUNNING.getCode();
    static final Integer STATUS_SUCCESS = AutoModeExecutionStatus.SUCCESS.getCode();
    static final Integer STATUS_FAILED = AutoModeExecutionStatus.FAILED.getCode();

    private static final int FAILURE_REASON_MAX_LENGTH = 1000;
    private static final int DEFAULT_QUERY_LIMIT = 100;
    private static final int MAX_QUERY_LIMIT = 500;

    private final DzAutoModeExecutionTraceMapper traceMapper;
    private final DzAutoModeMinuteSummaryMapper summaryMapper;
    private final AutoModeTraceRuntimeCache runtimeCache;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(AutoModeTraceContext context) {
        if (context == null || context.getActionType() == null) {
            return null;
        }
        Date now = new Date();
        DzAutoModeExecutionTrace trace = new DzAutoModeExecutionTrace();
        trace.setId(IdUtil.getSnowflakeNextId());
        trace.setActionType(context.getActionType());
        trace.setActionName(resolveActionName(context));
        trace.setBizType(context.getBizType());
        trace.setBizId(context.getBizId());
        trace.setTaskId(context.getTaskId());
        trace.setBatchFlag(Boolean.TRUE.equals(context.getBatchFlag()));
        trace.setExecuteCount(normalizeExecuteCount(context.getExecuteCount()));
        trace.setStatus(STATUS_RUNNING);
        trace.setStartedAt(now);
        trace.setDetailJson(safeDetail(context.getDetailJson()));
        trace.setCreateDate(now);
        trace.setUpdateDate(now);
        try {
            traceMapper.insert(trace);
        } catch (Exception e) {
            log.warn("自动模式执行留痕开始记录失败, actionType={}", context.getActionType(), e);
            return null;
        }
        try {
            runtimeCache.putRunning(trace);
        } catch (Exception e) {
            log.warn("自动模式运行中留痕写入 Redis 失败, traceId={}", trace.getId(), e);
        }
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_started",
            resolveBizType(context),
            context.getBizId(),
            context.getTaskId()
        );
        return trace.getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(Long traceId, AutoModeTraceContext context, int executeCount, Map<String, Object> detailJson) {
        if (traceId == null || context == null) {
            return;
        }
        Date endedAt = new Date();
        int safeCount = normalizeExecuteCount(executeCount);
        Map<String, Object> mergedDetail = mergeDetail(context.getDetailJson(), detailJson);
        try {
            DzAutoModeExecutionTrace update = new DzAutoModeExecutionTrace();
            update.setId(traceId);
            update.setStatus(STATUS_SUCCESS);
            update.setExecuteCount(safeCount);
            update.setEndedAt(endedAt);
            update.setDetailJson(mergedDetail);
            update.setUpdateDate(endedAt);
            traceMapper.updateById(update);
            if (safeCount > 0) {
                summaryMapper.upsertIncrement(buildSummary(context, safeCount, mergedDetail, endedAt));
            }
        } catch (Exception e) {
            log.warn("自动模式执行留痕成功记录失败, traceId={}, actionType={}", traceId, context.getActionType(), e);
        } finally {
            deleteRunningQuietly(traceId);
        }
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_succeeded",
            resolveBizType(context),
            context.getBizId(),
            context.getTaskId()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long traceId, AutoModeTraceContext context, Throwable throwable, Map<String, Object> detailJson) {
        fail(traceId, context, failureMessage(throwable), detailJson);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long traceId, AutoModeTraceContext context, String failureReason, Map<String, Object> detailJson) {
        if (traceId == null || context == null) {
            return;
        }
        Date endedAt = new Date();
        try {
            DzAutoModeExecutionTrace update = new DzAutoModeExecutionTrace();
            update.setId(traceId);
            update.setStatus(STATUS_FAILED);
            update.setExecuteCount(0);
            update.setEndedAt(endedAt);
            update.setFailureReason(limitFailureReason(failureReason));
            update.setDetailJson(mergeDetail(context.getDetailJson(), detailJson));
            update.setUpdateDate(endedAt);
            traceMapper.updateById(update);
        } catch (Exception e) {
            log.warn("自动模式执行留痕失败记录失败, traceId={}, actionType={}", traceId, context.getActionType(), e);
        } finally {
            deleteRunningQuietly(traceId);
        }
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_failed",
            resolveBizType(context),
            context.getBizId(),
            context.getTaskId()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(AutoModeTraceContext context, Throwable throwable, Map<String, Object> detailJson) {
        Long traceId = start(context);
        fail(traceId, context, throwable, detailJson);
    }

    @Override
    public List<DzAutoModeExecutionTrace> listRunningTraces() {
        Map<Long, DzAutoModeExecutionTrace> merged = new LinkedHashMap<>();
        try {
            for (DzAutoModeExecutionTrace trace : runtimeCache.listRunning()) {
                if (trace != null && trace.getId() != null) {
                    merged.put(trace.getId(), trace);
                }
            }
        } catch (Exception e) {
            log.warn("查询自动模式 Redis 运行中留痕失败", e);
        }
        try {
            List<DzAutoModeExecutionTrace> dbRunning = traceMapper.selectList(
                Wrappers.<DzAutoModeExecutionTrace>lambdaQuery()
                    .eq(DzAutoModeExecutionTrace::getStatus, STATUS_RUNNING)
                    .orderByDesc(DzAutoModeExecutionTrace::getStartedAt, DzAutoModeExecutionTrace::getId)
                    .last("limit " + MAX_QUERY_LIMIT)
            );
            for (DzAutoModeExecutionTrace trace : dbRunning) {
                if (trace != null && trace.getId() != null) {
                    merged.put(trace.getId(), trace);
                }
            }
        } catch (Exception e) {
            log.warn("查询自动模式数据库运行中留痕失败", e);
        }
        List<DzAutoModeExecutionTrace> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(DzAutoModeExecutionTrace::getStartedAt,
            Comparator.nullsLast(Date::compareTo)).reversed());
        return result;
    }

    @Override
    public List<DzAutoModeMinuteSummary> listSummaries(Date startTime, Date endTime, int limit) {
        Date[] window = normalizeWindow(startTime, endTime);
        return summaryMapper.selectList(
            Wrappers.<DzAutoModeMinuteSummary>lambdaQuery()
                .ge(DzAutoModeMinuteSummary::getWindowStart, minuteWindow(window[0]))
                .le(DzAutoModeMinuteSummary::getWindowStart, minuteWindow(window[1]))
                .orderByDesc(DzAutoModeMinuteSummary::getWindowStart, DzAutoModeMinuteSummary::getId)
                .last("limit " + normalizeLimit(limit))
        );
    }

    @Override
    public List<DzAutoModeExecutionTrace> listDisplayTraces(Date startTime, Date endTime, int limit) {
        Date[] window = normalizeWindow(startTime, endTime);
        return traceMapper.selectList(
            Wrappers.<DzAutoModeExecutionTrace>lambdaQuery()
                .and(w -> w.eq(DzAutoModeExecutionTrace::getStatus, STATUS_FAILED)
                    .or(inner -> inner.eq(DzAutoModeExecutionTrace::getBatchFlag, false)
                        .eq(DzAutoModeExecutionTrace::getStatus, STATUS_SUCCESS)))
                .and(w -> w.ge(DzAutoModeExecutionTrace::getEndedAt, window[0])
                    .le(DzAutoModeExecutionTrace::getEndedAt, window[1]))
                .orderByDesc(DzAutoModeExecutionTrace::getEndedAt, DzAutoModeExecutionTrace::getStartedAt, DzAutoModeExecutionTrace::getId)
                .last("limit " + normalizeLimit(limit))
        );
    }

    @Override
    public long sumSuccessfulExecutions(Date startTime, Date endTime, Collection<Integer> actionTypes) {
        Date[] window = normalizeWindow(startTime, endTime);
        Long count = summaryMapper.sumExecuteCount(minuteWindow(window[0]), minuteWindow(window[1]), actionTypes);
        return count == null ? 0L : count;
    }

    static Date minuteWindow(Date date) {
        Date safeDate = date == null ? new Date() : date;
        long millis = safeDate.getTime();
        return new Date(millis - millis % 60000L);
    }

    private DzAutoModeMinuteSummary buildSummary(AutoModeTraceContext context, int executeCount,
                                                 Map<String, Object> detailJson, Date endedAt) {
        Date now = new Date();
        DzAutoModeMinuteSummary summary = new DzAutoModeMinuteSummary();
        summary.setId(IdUtil.getSnowflakeNextId());
        summary.setWindowStart(minuteWindow(endedAt));
        summary.setActionType(context.getActionType());
        summary.setActionName(resolveActionName(context));
        summary.setExecuteCount(executeCount);
        summary.setDetailJson(safeDetail(detailJson));
        summary.setCreateDate(now);
        summary.setUpdateDate(now);
        return summary;
    }

    private String resolveActionName(AutoModeTraceContext context) {
        if (context == null) {
            return null;
        }
        return StringUtils.blankToDefault(context.getActionName(), AutoModeExecutionActions.defaultName(context.getActionType()));
    }

    private int normalizeExecuteCount(Integer executeCount) {
        if (executeCount == null) {
            return 0;
        }
        return Math.max(0, executeCount);
    }

    private int normalizeExecuteCount(int executeCount) {
        return Math.max(0, executeCount);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_QUERY_LIMIT;
        }
        return Math.min(limit, MAX_QUERY_LIMIT);
    }

    private Date[] normalizeWindow(Date startTime, Date endTime) {
        Date safeEnd = endTime == null ? new Date() : endTime;
        Date safeStart = startTime == null ? minuteWindow(safeEnd) : startTime;
        if (safeStart.after(safeEnd)) {
            return new Date[]{safeEnd, safeStart};
        }
        return new Date[]{safeStart, safeEnd};
    }

    private Map<String, Object> mergeDetail(Map<String, Object> base, Map<String, Object> incoming) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (base != null) {
            result.putAll(base);
        }
        if (incoming != null) {
            result.putAll(incoming);
        }
        return result;
    }

    private Map<String, Object> safeDetail(Map<String, Object> detailJson) {
        if (detailJson == null || detailJson.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(detailJson);
    }

    static String failureMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知异常";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return StringUtils.blankToDefault(root.getMessage(), root.getClass().getSimpleName());
    }

    static String limitFailureReason(String failureReason) {
        String reason = StringUtils.blankToDefault(failureReason, "未知异常");
        if (reason.length() <= FAILURE_REASON_MAX_LENGTH) {
            return reason;
        }
        return reason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    private void deleteRunningQuietly(Long traceId) {
        try {
            runtimeCache.deleteRunning(traceId);
        } catch (Exception e) {
            log.warn("自动模式运行中留痕删除 Redis 失败, traceId={}", traceId, e);
        }
    }

    private String resolveBizType(AutoModeTraceContext context) {
        if (context == null) {
            return null;
        }
        return TaskProcessBizTypeEnum.legacyCode(context.getBizType());
    }
}
