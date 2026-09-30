/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AutoModeTraceContext;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeExecutionTraceMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeMinuteSummaryMapper;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzAutoModeExecutionTraceNotifyTest {

    @Test
    void startBroadcastsOverviewRefreshSignal() {
        Fixture fixture = fixture();
        AutoModeTraceContext context = context();

        fixture.service().start(context);

        ArgumentCaptor<DzAutoModeExecutionTrace> traceCaptor = ArgumentCaptor.forClass(DzAutoModeExecutionTrace.class);
        verify(fixture.traceMapper()).insert(traceCaptor.capture());
        assertThat(traceCaptor.getValue().getActionType()).isEqualTo(2);
        assertThat(traceCaptor.getValue().getStatus()).isEqualTo(1);
        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_started",
            "HANDLE",
            12L,
            34L
        );
    }

    @Test
    void successBroadcastsOverviewRefreshSignal() {
        Fixture fixture = fixture();
        AutoModeTraceContext context = context();

        fixture.service().success(100L, context, 2, Map.of("count", 2));

        ArgumentCaptor<DzAutoModeExecutionTrace> traceCaptor = ArgumentCaptor.forClass(DzAutoModeExecutionTrace.class);
        verify(fixture.traceMapper()).updateById(traceCaptor.capture());
        assertThat(traceCaptor.getValue().getStatus()).isEqualTo(2);
        assertThat(traceCaptor.getValue().getExecuteCount()).isEqualTo(2);
        verify(fixture.summaryMapper()).upsertIncrement(any());
        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_succeeded",
            "HANDLE",
            12L,
            34L
        );
    }

    @Test
    void failBroadcastsOverviewRefreshSignal() {
        Fixture fixture = fixture();
        AutoModeTraceContext context = context();

        fixture.service().fail(100L, context, "boom", Map.of("error", "boom"));

        ArgumentCaptor<DzAutoModeExecutionTrace> traceCaptor = ArgumentCaptor.forClass(DzAutoModeExecutionTrace.class);
        verify(fixture.traceMapper()).updateById(traceCaptor.capture());
        assertThat(traceCaptor.getValue().getStatus()).isEqualTo(3);
        assertThat(traceCaptor.getValue().getFailureReason()).isEqualTo("boom");
        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "auto_mode_trace",
            "trace_failed",
            "HANDLE",
            12L,
            34L
        );
    }

    private Fixture fixture() {
        DzAutoModeExecutionTraceMapper traceMapper = mock(DzAutoModeExecutionTraceMapper.class);
        DzAutoModeMinuteSummaryMapper summaryMapper = mock(DzAutoModeMinuteSummaryMapper.class);
        AutoModeTraceRuntimeCache runtimeCache = mock(AutoModeTraceRuntimeCache.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        when(summaryMapper.upsertIncrement(any())).thenReturn(1);
        return new Fixture(
            new cn.edu.pku.whai.geological.disaster.service.service.impl.DzAutoModeExecutionTraceServiceImpl(
                traceMapper,
                summaryMapper,
                runtimeCache,
                notifyService
            ),
            traceMapper,
            summaryMapper,
            notifyService
        );
    }

    private AutoModeTraceContext context() {
        return AutoModeTraceContext.builder()
            .actionType(2)
            .actionName("自动推送")
            .bizType(TaskProcessBizTypeEnum.HANDLE.getCode())
            .bizId(12L)
            .taskId(34L)
            .detailJson(Map.of("scene", "test"))
            .build();
    }

    private record Fixture(cn.edu.pku.whai.geological.disaster.service.service.impl.DzAutoModeExecutionTraceServiceImpl service,
                           DzAutoModeExecutionTraceMapper traceMapper,
                           DzAutoModeMinuteSummaryMapper summaryMapper,
                           AiHostingOverviewNotifyService notifyService) {
    }
}
