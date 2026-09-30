package cn.edu.pku.whai.geological.disaster.service.dify.controller;


import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatBo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiAgentChatService;
import io.github.imfangs.dify.client.model.chat.SuggestedQuestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ai-agent对话
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/dizai/ai/agent")
@RequiredArgsConstructor
public class AiAgentChatController {
    private final IAiAgentChatService aiAgentChatService;

    /**
     * 对话接口
     *
     * @return 流式结果
     */
    @Log(title = "AI对话", businessType = BusinessType.INSERT, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatBo bo) {
        SseEmitter sseEmitter = new SseEmitter();
        aiAgentChatService.chat(sseEmitter, bo);
        return sseEmitter;
    }

    /**
     * 停止对话
     *
     * @return
     */
    @Log(title = "AI对话停止", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("stopChatMessage")
    public R<Void> stopChatMessage(String taskId, String user) {
        aiAgentChatService.stopChatMessage(taskId, user);
        return R.ok();
    }


    /**
     * 获取建议问题
     *
     * @param messageId 消息 ID
     * @param user      用户标识
     * @return
     */
    @PostMapping("getSuggestedQuestions")
    public R<SuggestedQuestionsResponse> getSuggestedQuestions(String messageId, String user) {
        SuggestedQuestionsResponse response = aiAgentChatService.getSuggestedQuestions(messageId, user);
        return R.ok(response);
    }

}
