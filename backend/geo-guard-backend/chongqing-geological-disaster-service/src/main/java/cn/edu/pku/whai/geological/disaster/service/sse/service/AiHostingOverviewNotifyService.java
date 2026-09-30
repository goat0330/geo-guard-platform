/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sse.service;

import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * AI 托管概览变动通知服务。
 */
@Service
@RequiredArgsConstructor
public class AiHostingOverviewNotifyService {

    public static final String EVENT_TYPE = "ai-hosting-overview-changed";
    static final long NOTIFY_INTERVAL_MILLIS = 30_000L;

    private final SseEmitterManager sseEmitterManager;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Object throttleLock = new Object();
    private Map<String, Object> pendingPayload;
    private ScheduledFuture<?> pendingFuture;
    private long lastNotifyTimeMillis;
    private long throttleVersion;

    public void notifyChangedAfterCommit(String scene, String reason, String bizType, Long bizId, Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyChanged(scene, reason, bizType, bizId, taskId);
                }
            });
            return;
        }
        notifyChanged(scene, reason, bizType, bizId, taskId);
    }

    public void notifyChanged(String scene, String reason, String bizType, Long bizId, Long taskId) {
        dispatchThrottled(buildPayload(scene, reason, bizType, bizId, taskId));
    }

    private void dispatchThrottled(Map<String, Object> payload) {
        boolean sendImmediately = false;
        synchronized (throttleLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastNotifyTimeMillis;
            if (lastNotifyTimeMillis == 0L || elapsed >= NOTIFY_INTERVAL_MILLIS) {
                lastNotifyTimeMillis = now;
                pendingPayload = null;
                cancelPendingFlush();
                throttleVersion++;
                sendImmediately = true;
            } else {
                pendingPayload = payload;
                schedulePendingFlushIfNeeded(NOTIFY_INTERVAL_MILLIS - elapsed);
            }
        }
        if (sendImmediately) {
            sendPayload(payload);
        }
    }

    private void schedulePendingFlushIfNeeded(long delayMillis) {
        if (pendingFuture != null && !pendingFuture.isDone()) {
            return;
        }
        long flushVersion = throttleVersion;
        pendingFuture = scheduledExecutorService.schedule(() -> flushPendingPayload(flushVersion), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void flushPendingPayload(long flushVersion) {
        Map<String, Object> payload;
        synchronized (throttleLock) {
            if (flushVersion != throttleVersion) {
                return;
            }
            if (pendingPayload == null) {
                pendingFuture = null;
                return;
            }
            payload = pendingPayload;
            pendingPayload = null;
            pendingFuture = null;
            lastNotifyTimeMillis = System.currentTimeMillis();
            throttleVersion++;
        }
        sendPayload(payload);
    }

    private void cancelPendingFlush() {
        if (pendingFuture != null && !pendingFuture.isDone()) {
            pendingFuture.cancel(false);
        }
        pendingFuture = null;
    }

    private void sendPayload(Map<String, Object> payload) {
        SseResp.SseRespBuilder message = SseResp.builder()
            .type(EVENT_TYPE)
            .data(payload);
        sseEmitterManager.sendMessage(message);
    }

    private Map<String, Object> buildPayload(String scene, String reason, String bizType, Long bizId, Long taskId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene", scene);
        payload.put("reason", reason);
        payload.put("bizType", bizType);
        payload.put("bizId", bizId);
        payload.put("taskId", taskId);
        payload.put("changedAt", new Date());
        return payload;
    }
}
