/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sse.service;

import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.bo.SseNotifyBo;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SseService {

    private static final String NOTIFY_CUSTOM_TYPE = "notify-custom";

    private final SseEmitterManager sseEmitterManager;

    public void notifyByUserIds(SseNotifyBo bo) {
        SseResp.SseRespBuilder message = SseResp.builder()
            .type(NOTIFY_CUSTOM_TYPE)
            .data(bo.getContent());
        for (Long userId : bo.getUserIds()) {
            sseEmitterManager.sendMessage(userId, message);
        }
    }
}
