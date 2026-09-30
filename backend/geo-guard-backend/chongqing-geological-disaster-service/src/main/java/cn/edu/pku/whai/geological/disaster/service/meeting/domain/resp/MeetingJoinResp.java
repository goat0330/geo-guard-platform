/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp;

import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MeetingJoinResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private MeetingInfoVo meetingInfo;
    private LiveKitTokenResp liveKit;
}
