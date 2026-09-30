/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.constant.CacheConstants;
import org.dromara.common.core.domain.dto.UserOnlineDTO;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import cn.hutool.core.convert.Convert;
import org.redisson.api.RBatch;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RFuture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线用户辅助工具，复用 Sa-Token 在线令牌与 Redis 的公共能力。
 */
public class OnlineUtil {

    private static final String REALTIME_ONLINE_USER = "realtime:online:user:";

    /**
     * 生成实时在线标记 key，统一 key 前缀以便批量查询和过期清理。
     */
    private static String getRealtimeOnlineUserKey(Long userId) {
        return REALTIME_ONLINE_USER + userId;
    }

    /**
     * 按用户名聚合当前未过期的在线令牌，过滤已失效令牌以避免返回陈旧在线状态。
     */
    public static Map<String, List<UserOnlineDTO>> onlineUsers() {
        // 获取所有未过期的 token
        Collection<String> keys = RedisUtils.keys(CacheConstants.ONLINE_TOKEN_KEY + "*");

        List<UserOnlineDTO> userOnlineDTOList = new ArrayList<>();
        for (String key : keys) {
            String token = StringUtils.substringAfterLast(key, ":");
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                continue;
            }
            userOnlineDTOList.add(RedisUtils.getCacheObject(CacheConstants.ONLINE_TOKEN_KEY + token));
        }

        Map<String, List<UserOnlineDTO>> map = StreamUtils.groupByKey(userOnlineDTOList, UserOnlineDTO::getUserName);
        return map;
    }

    /**
     * 写入 20 秒实时在线标记；短 TTL 使心跳停止后状态自然失效。
     */
    public static void addRealtimeOnlineUser(Long userId) {
        RedisUtils.getClient()
                .getBucket(getRealtimeOnlineUserKey(userId))
                .setAsync(userId, Duration.ofSeconds(20));
    }

    /**
     * 扫描实时在线标记并解析用户 ID，保持返回集合去重。
     */
    public static Set<Long> getRealtimeOnlineUser() {
        return RedisUtils.keys(REALTIME_ONLINE_USER + "*").stream()
                .map(key -> key.substring(REALTIME_ONLINE_USER.length()))
                .map(Convert::toLong)
                .collect(Collectors.toSet());
    }

    /**
     * 批量判断指定用户的实时在线标记，使用 Redisson batch 降低逐用户查询的网络往返。
     */
    public static Set<Long> getRealtimeOnlineUser(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return Collections.emptySet();
        }

        RBatch batch = RedisUtils.getClient().createBatch();
        Map<Long, RFuture<Boolean>> futureMap = new HashMap<>(distinctUserIds.size());
        for (Long userId : distinctUserIds) {
            RBucketAsync<Long> bucket = batch.getBucket(getRealtimeOnlineUserKey(userId));
            futureMap.put(userId, bucket.isExistsAsync());
        }
        batch.execute();

        return futureMap.entrySet().stream()
                .filter(entry -> {
                    try {
                        return Boolean.TRUE.equals(entry.getValue().get());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
