/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import io.github.imfangs.dify.client.event.MessageEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatVo extends MessageEvent {
    private String type;
}
