/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.cache;

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
    private static String getDefKey() {
        return DEFENSE_RESP_CACHE;
    }

    /**
     * 设置防御响应缓存
     *
     * @param value 缓存值
     */

    public static void setDefenseRespCache(Integer value) {
        RedisUtils.setCacheObject(getDefKey(), value);
    }

    /**
     * 获取防御响应缓存
     *
     * @return 缓存值
     */
    public static Integer getDefenseRespCache() {
        return RedisUtils.getCacheObject(getDefKey());
    }
}
