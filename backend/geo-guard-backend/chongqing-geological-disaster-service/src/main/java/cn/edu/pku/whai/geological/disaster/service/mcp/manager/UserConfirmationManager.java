/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.mcp.manager;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.dify.consts.enums.ConfirmationType;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.dto.ConfirmationTaskDto;
import cn.edu.pku.whai.geological.disaster.service.mcp.cache.McpToolsRedisCache;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 用户确认管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserConfirmationManager {
    public static final Map<String, SseEmitter> tokenEmitter = new ConcurrentHashMap<>();

    /**
     * 发起确认任务；日志仅保留任务标识，避免记录用户提示内容。
     *
     * @param prompt 提示语
     * @param type   确认类型
     * @return 任务ID
     */
    public String startTask(String prompt, ConfirmationType type) {
        String taskId = IdUtil.fastSimpleUUID();
        // 初始化任务状态，使用"0"表示未处理
        McpToolsRedisCache.setTaskKey(taskId, "0");

        try {
            // 构建SSE消息数据
            ConfirmationTaskDto taskData = ConfirmationTaskDto.builder()
                    .taskId(taskId)
                    .prompt(prompt)
                    .type(type.getCode())
                    .build();

            SseResp.SseRespBuilder builder = SseResp.builder()
                    .type("reply")
                    .data(taskData);

            String token = StpUtil.getTokenValue();
            SseEmitter sseEmitter = tokenEmitter.get(token);
            if (ObjectUtil.isNotEmpty(sseEmitter)) {
                SseEmitter.SseEventBuilder message = SseEmitter.event()
                        .name("message")
                        .data(builder.id(IdUtil.fastSimpleUUID()).build().toStr());

                sseEmitter.send(message);
            }
            log.info("Sent confirmation request. TaskId: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to send confirmation request", e);
            throw new ServiceException("发起用户确认失败");
        }
        return taskId;
    }

    /**
     * 等待用户确认结果；回复内容只交给调用方，不进入日志。
     *
     * @param taskId         任务ID
     * @param timeoutSeconds 超时时间(秒)
     * @return 用户回复内容
     */
    public String waitForResult(String taskId, int timeoutSeconds) {
        int counter = 0;
        // 每次睡眠3秒，计算循环次数
        int maxAttempts = (timeoutSeconds * 1000) / 3000;

        while (counter < maxAttempts) {
            log.info("Waiting for user confirmation... Attempt {}/{}", counter + 1, maxAttempts);
            ThreadUtil.sleep(3000);

            String result = McpToolsRedisCache.getTaskKey(taskId);
            // "0" 表示初始状态，不为 "0" 且不为空表示有结果
            if (result != null && !"0".equals(result)) {
                log.info("User confirmed. TaskId: {}", taskId);
                McpToolsRedisCache.delTaskKey(taskId);
                return result;
            }
            counter++;
        }

        log.warn("User confirmation timed out. TaskId: {}", taskId);
        throw new ServiceException("用户确认超时或失败");
    }

    /**
     * 执行带确认的业务逻辑
     *
     * @param prompt   提示语
     * @param type     确认类型
     * @param function 业务执行函数，入参为用户回复
     * @param <R>      返回值类型
     * @return 业务执行结果
     */
    public <R> R executeWithConfirmation(String prompt, ConfirmationType type, Function<String, R> function) {
        String taskId = startTask(prompt, type);
        String result = waitForResult(taskId, 60); // 默认60秒超时
        return function.apply(result);
    }

    public static void sendChatMsg(String type, String data) {

        try {
            SseResp.SseRespBuilder builder = SseResp.builder()
                    .type(type)
                    .data(data);

            String token = StpUtil.getTokenValue();
            SseEmitter sseEmitter = tokenEmitter.get(token);
            if (ObjectUtil.isNotEmpty(sseEmitter)) {
                SseEmitter.SseEventBuilder message = SseEmitter.event()
                        .name("message")
                        .data(builder.id(IdUtil.fastSimpleUUID()).build().toStr());

                sseEmitter.send(message);
            }
        } catch (IOException e) {
            log.error("Failed to send chat message", e);
        }
    }
}
