/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatBo extends ChatMessage {

    /**
     * 智能体类型
     */
    private String agentType;

    /**
     * 响应模式
     */
    private ResponseMode responseMode = ResponseMode.STREAMING;
}
