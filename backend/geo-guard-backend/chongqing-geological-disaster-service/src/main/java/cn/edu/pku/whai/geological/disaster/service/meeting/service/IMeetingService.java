/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.StartMeetingBo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.bo.CallParticipantsBo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.MeetingJoinResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;

import java.util.Map;

public interface IMeetingService {

    MeetingInfoVo startMeeting(StartMeetingBo bo);

    MeetingInfoVo getMeetingInfo(Long meetingId);

    void closeMeeting(Long meetingId);

    void exitMeeting(Long meetingId);

    Long getMeetingId(Long id, Integer process);

    MeetingJoinResp joinMeeting(Long meetingId);

    void recallParticipants(CallParticipantsBo bo);

    void removeParticipants(CallParticipantsBo bo);

    void addParticipants(CallParticipantsBo bo);

    /**
     * 防御响应归档时关闭相关会议：占位handleId(10000000)及当前handleId关联的所有会议
     */
    void closeDefRespMeetingsOnArchive(Long defRespPlanId, Long currentHandleId);

}
