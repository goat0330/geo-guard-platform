/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl.helper;

import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;

import java.util.Map;

public record AdminApprovalNoticeContext(MeetingInfoVo meetingInfo, DzUserContactVo userContact, String contentTemplate, Map<String, String> params) {}
