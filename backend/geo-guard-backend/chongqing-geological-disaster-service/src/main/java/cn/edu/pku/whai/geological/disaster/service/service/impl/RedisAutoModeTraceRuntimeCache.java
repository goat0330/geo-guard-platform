/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 自动模式运行中留痕 Redis 镜像。
 */
@Component
class RedisAutoModeTraceRuntimeCache implements AutoModeTraceRuntimeCache {

    private static final String RUNNING_KEY_PREFIX = "dizai:auto-mode:trace:running:";
    private static final Duration RUNNING_TTL = Duration.ofHours(6);

    @Override
    public void putRunning(DzAutoModeExecutionTrace trace) {
        if (trace == null || trace.getId() == null) {
            return;
        }
        RedisUtils.setCacheObject(key(trace.getId()), trace, RUNNING_TTL);
    }

    @Override
    public void deleteRunning(Long traceId) {
        if (traceId == null) {
            return;
        }
        RedisUtils.deleteObject(key(traceId));
    }

    @Override
    public List<DzAutoModeExecutionTrace> listRunning() {
        Collection<String> keys = RedisUtils.keys(RUNNING_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
            .map(RedisUtils::<DzAutoModeExecutionTrace>getCacheObject)
            .filter(Objects::nonNull)
            .toList();
    }

    private String key(Long traceId) {
        return RUNNING_KEY_PREFIX + traceId;
    }
}
