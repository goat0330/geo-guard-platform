/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.job;


import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppTaskJob {
    public static String APP_TOKEN = "";
    private static final String APP_TOKEN_LOCK_KEY = "app_task_job_token_refresh_lock";
    private static final long APP_TOKEN_LOCK_TTL_MINUTES = 25L;

    private final AppTaskProps appTaskProps;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 定时获取 app token
     */
    @Scheduled(initialDelay = 0, fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void getToken() {
        if (!Boolean.TRUE.equals(appTaskProps.getEnabled())) {
            log.info("APP接口开关关闭，跳过刷新 app token");
            return;
        }
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
            APP_TOKEN_LOCK_KEY,
            lockValue,
            APP_TOKEN_LOCK_TTL_MINUTES,
            TimeUnit.MINUTES
        );
        if (!Boolean.TRUE.equals(locked)) {
            log.info("获取 app token 未获取到分布式锁，跳过本次执行");
            return;
        }
        try {
            JsonNode node = Req.formUrlencoded(appTaskProps.getPrefixUrl())
                    .path("/user/login")
                    .form("userName", appTaskProps.getUserName())
                    .form("password", appTaskProps.getPassword())
                    .form("type", appTaskProps.getType())
                    .ok()
                    .node()
                    .findPath("token");

            if (!node.isMissingNode()) {
                APP_TOKEN = node.asText();
                log.info("app token -> {}", APP_TOKEN);
            }
        } finally {
            releaseLock(lockValue);
        }
    }

    private void releaseLock(String lockValue) {
        String currentLockValue = stringRedisTemplate.opsForValue().get(APP_TOKEN_LOCK_KEY);
        if (lockValue != null && lockValue.equals(currentLockValue)) {
            stringRedisTemplate.delete(APP_TOKEN_LOCK_KEY);
        }
    }
}
