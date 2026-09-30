/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.mcp.cache;


import org.dromara.common.redis.utils.RedisUtils;

import java.time.Duration;

public class McpToolsRedisCache {

    private static final String MCP_TOOLS_REDIS_CACHE_KEY = "mcp_tools_redis_cache_key:";


    private static String genTaskKey(String id) {
        return MCP_TOOLS_REDIS_CACHE_KEY + id;
    }

    public static void setTaskKey(String id, String value) {
        RedisUtils.setCacheObject(genTaskKey(id), value, Duration.ofSeconds(60));
    }

    public static String getTaskKey(String id) {
        return RedisUtils.getCacheObject(genTaskKey(id));
    }

    public static void delTaskKey(String id) {
        RedisUtils.deleteObject(genTaskKey(id));
    }

    public static boolean hasTaskKey(String id) {
        return RedisUtils.hasKey(genTaskKey(id));
    }
}
