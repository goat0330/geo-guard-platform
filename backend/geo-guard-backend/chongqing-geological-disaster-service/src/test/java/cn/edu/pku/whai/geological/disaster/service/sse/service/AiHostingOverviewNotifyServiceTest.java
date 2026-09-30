/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sse.service;

import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("dev")
class AiHostingOverviewNotifyServiceTest {

    @Test
    void notifyChangedBuildsExpectedSsePayload() {
        SseEmitterManager emitterManager = mock(SseEmitterManager.class);
        AiHostingOverviewNotifyService service = new AiHostingOverviewNotifyService(
            emitterManager,
            mock(ScheduledExecutorService.class)
        );

        service.notifyChanged("task_handle_process", "handle_created", "dz_task_handle", 66L, null);

        ArgumentCaptor<SseResp.SseRespBuilder> captor = ArgumentCaptor.forClass(SseResp.SseRespBuilder.class);
        verify(emitterManager).sendMessage(captor.capture());
        SseResp message = captor.getValue().build();
        assertThat(message.getType()).isEqualTo(AiHostingOverviewNotifyService.EVENT_TYPE);
        assertThat(message.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) message.getData();
        assertThat(payload)
            .containsEntry("scene", "task_handle_process")
            .containsEntry("reason", "handle_created")
            .containsEntry("bizType", "dz_task_handle")
            .containsEntry("bizId", 66L)
            .containsEntry("taskId", null);
        assertThat(payload.get("changedAt")).isNotNull();
    }

    @Test
    void notifyChangedAfterCommitDefersBroadcastUntilCommit() {
        SseEmitterManager emitterManager = mock(SseEmitterManager.class);
        AiHostingOverviewNotifyService service = new AiHostingOverviewNotifyService(
            emitterManager,
            mock(ScheduledExecutorService.class)
        );
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.notifyChangedAfterCommit("auto_mode_trace", "trace_started", "HANDLE", 1L, 2L);

            verifyNoInteractions(emitterManager);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(emitterManager).sendMessage(org.mockito.ArgumentMatchers.any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void notifyChangedThrottlesUpdatesWithinHalfMinuteAndFlushesLatestPayload() {
        SseEmitterManager emitterManager = mock(SseEmitterManager.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(executor).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        List<SseResp> sentMessages = new ArrayList<>();
        doAnswer(invocation -> {
            SseResp.SseRespBuilder builder = invocation.getArgument(0);
            sentMessages.add(builder.build());
            return null;
        }).when(emitterManager).sendMessage(any());
        AiHostingOverviewNotifyService service = new AiHostingOverviewNotifyService(emitterManager, executor);

        service.notifyChanged("auto_mode_trace", "trace_started", "HANDLE", 1L, 10L);
        service.notifyChanged("ai_vision", "ai_picture_result_updated", "dz_report_disaster", 2L, null);
        service.notifyChanged("report_content", "ai_report_saved", "dz_task_handle", 3L, null);

        verify(emitterManager, times(1)).sendMessage(any());
        assertThat(sentMessages).hasSize(1);
        assertPayload(sentMessages.get(0), "auto_mode_trace", "trace_started", "HANDLE", 1L, 10L);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).schedule(
            runnableCaptor.capture(),
            org.mockito.ArgumentMatchers.longThat(delay -> delay > 0L && delay <= AiHostingOverviewNotifyService.NOTIFY_INTERVAL_MILLIS),
            eq(TimeUnit.MILLISECONDS)
        );

        runnableCaptor.getValue().run();

        verify(emitterManager, times(2)).sendMessage(any());
        assertThat(sentMessages).hasSize(2);
        assertPayload(sentMessages.get(1), "report_content", "ai_report_saved", "dz_task_handle", 3L, null);
    }

    private void assertPayload(SseResp message, String scene, String reason, String bizType, Long bizId, Long taskId) {
        assertThat(message.getType()).isEqualTo(AiHostingOverviewNotifyService.EVENT_TYPE);
        assertThat(message.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) message.getData();
        assertThat(payload)
            .containsEntry("scene", scene)
            .containsEntry("reason", reason)
            .containsEntry("bizType", bizType)
            .containsEntry("bizId", bizId)
            .containsEntry("taskId", taskId);
        assertThat(payload.get("changedAt")).isNotNull();
    }
}
