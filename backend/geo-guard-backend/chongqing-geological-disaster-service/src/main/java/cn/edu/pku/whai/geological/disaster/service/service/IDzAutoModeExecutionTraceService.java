/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.dto.AutoModeTraceContext;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeMinuteSummary;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自动模式执行留痕内部服务。
 */
public interface IDzAutoModeExecutionTraceService {

    Long start(AutoModeTraceContext context);

    void success(Long traceId, AutoModeTraceContext context, int executeCount, Map<String, Object> detailJson);

    void fail(Long traceId, AutoModeTraceContext context, Throwable throwable, Map<String, Object> detailJson);

    void fail(Long traceId, AutoModeTraceContext context, String failureReason, Map<String, Object> detailJson);

    void recordFailure(AutoModeTraceContext context, Throwable throwable, Map<String, Object> detailJson);

    List<DzAutoModeExecutionTrace> listRunningTraces();

    List<DzAutoModeMinuteSummary> listSummaries(Date startTime, Date endTime, int limit);

    List<DzAutoModeExecutionTrace> listDisplayTraces(Date startTime, Date endTime, int limit);

    long sumSuccessfulExecutions(Date startTime, Date endTime, Collection<Integer> actionTypes);
}
