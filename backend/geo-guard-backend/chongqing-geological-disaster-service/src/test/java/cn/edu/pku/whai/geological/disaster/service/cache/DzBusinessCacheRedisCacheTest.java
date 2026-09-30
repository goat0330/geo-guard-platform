/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.cache;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用业务缓存 key 和 TTL 的离线边界测试，不连接真实 Redis。
 */
@Tag("dev")
class DzBusinessCacheRedisCacheTest {

    /**
     * 日期参与 key 隔离，确保同一业务不同自然日不会命中同一个缓存标记。
     */
    @Test
    void shouldBuildDailyBusinessCacheKey() {
        String key = DzBusinessCacheRedisCache.buildKey("riskJudgmentMessage", 1001L,
                LocalDateTime.of(2026, 4, 13, 9, 30));

        assertThat(key).isEqualTo("dz_business_cache:riskJudgmentMessage:1001:20260413");
    }

    /**
     * TTL 应精确截止次日零点，避免业务标记跨天复用。
     */
    @Test
    void shouldCalculateExpireDurationUntilTomorrow() {
        Duration duration = DzBusinessCacheRedisCache.expireDuration(LocalDateTime.of(2026, 4, 13, 9, 30));

        assertThat(duration).isEqualTo(Duration.ofHours(14).plusMinutes(30));
    }
}
