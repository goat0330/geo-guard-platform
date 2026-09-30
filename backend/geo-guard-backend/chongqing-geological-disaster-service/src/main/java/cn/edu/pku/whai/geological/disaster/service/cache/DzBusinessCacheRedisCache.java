/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.cache;

import org.dromara.common.redis.utils.RedisUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通用业务当天缓存
 */
public class DzBusinessCacheRedisCache {

    private static final String DZ_BUSINESS_CACHE_KEY = "dz_business_cache:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 工具类不允许实例化，避免误用对象状态。
     */
    private DzBusinessCacheRedisCache() {
    }

    /**
     * 写入当天有效的业务缓存标记，并把 TTL 设为当天结束。
     */
    public static void record(String businessType, Long userId, String businessContent) {
        LocalDateTime now = LocalDateTime.now();
        RedisUtils.setCacheObject(buildKey(businessType, userId, now), businessContent, expireDuration(now));
    }

    /**
     * 查询当天是否存在业务缓存标记。
     */
    public static boolean hasRecord(String businessType, Long userId) {
        return RedisUtils.hasKey(buildKey(businessType, userId, LocalDateTime.now()));
    }

    /**
     * 生成按用户、业务类型和日期隔离的缓存 key。
     */
    public static String buildKey(String businessType, Long userId, LocalDateTime now) {
        return DZ_BUSINESS_CACHE_KEY + businessType + ":" + userId + ":" + now.format(DATE_FORMATTER);
    }

    /**
     * 计算到次日零点的过期时间，确保业务标记不会跨自然日复用。
     */
    public static Duration expireDuration(LocalDateTime now) {
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay());
    }
}
