/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo;

import lombok.Data;

@Data
public class MeetingParticipantsStatus {
    private Long userId;
    private String nickName;
    /**
     * 0:未进入 1:已进入
     */
    private Integer status;
}
