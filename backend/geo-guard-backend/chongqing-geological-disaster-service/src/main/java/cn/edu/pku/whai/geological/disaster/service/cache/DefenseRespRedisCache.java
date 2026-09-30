/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.cache;

import org.dromara.common.redis.utils.RedisUtils;

/**
 * 防御响应缓存
 *
 * @author kongweiguang
 */
public class DefenseRespRedisCache {
    /**
     * 防御响应缓存
     */
    private static final String DEFENSE_RESP_CACHE = "defense_resp_cache";

    /**
     * 获取防御响应缓存
     *
     * @return 防御响应缓存
     */
    private static String getDefenseRespCache() {
        return DEFENSE_RESP_CACHE;
    }

    /**
     * 设置防御响应缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */

    public static void setDefenseRespCache(String key, String value) {
        RedisUtils.setCacheObject(getDefenseRespCache(), value);
    }

    /**
     * 获取防御响应缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public static String getDefenseRespCache(String key) {
        return RedisUtils.getCacheObject(getDefenseRespCache());
    }
}
