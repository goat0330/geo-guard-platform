/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service;

import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.LiveKitTokenReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.LiveKitTokenResp;

public interface ILiveKitService {

    LiveKitTokenResp generateToken(LiveKitTokenReq req);

    void closeRoom(Long meetingId);

    void removeParticipant(Long meetingId, Long userId);

    void handleWebhook(String body, String authorization);
}
