/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.service;

import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatBo;
import io.github.imfangs.dify.client.model.chat.SuggestedQuestionsResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAiAgentChatService {
    void chat(SseEmitter sseEmitter, AiChatBo bo);

    void stopChatMessage(String taskId, String user);

    SuggestedQuestionsResponse getSuggestedQuestions(String messageId, String user);
}
