/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.bo;


import lombok.Data;

import java.util.List;

@Data
public class CallParticipantsBo {
    private Long meetingId;
    private List<Long> userIds;
}
