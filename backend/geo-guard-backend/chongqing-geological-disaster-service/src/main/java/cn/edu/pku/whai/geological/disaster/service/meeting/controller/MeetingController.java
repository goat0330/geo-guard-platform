package cn.edu.pku.whai.geological.disaster.service.meeting.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.anno.ReqLogIgnore;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.StartMeetingBo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.bo.CallParticipantsBo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.MeetingJoinResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 会议相关
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@ReqLogIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/meeting")
public class MeetingController {
    private final IMeetingService meetingService;


    /**
     * 开启会议室
     */
    @Log(title = "会议", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/startMeeting")
    public R<MeetingInfoVo> startMeeting(@RequestBody StartMeetingBo bo) {
        MeetingInfoVo vo = meetingService.startMeeting(bo);
        return R.ok(vo);
    }

    /**
     * 通过handleId获取会议信息
     */
    @PostMapping("/getMeetingId/{id}/{process}")
    public R<Long> getMeetingId(@PathVariable Long id, @PathVariable Integer process) {
        Long vo = meetingService.getMeetingId(id, process);
        return R.ok(vo);
    }

    /**
     * 获取会议信息
     */
    @PostMapping("/getMeetingInfo/{meetingId}")
    public R<MeetingInfoVo> getMeetingInfo(@PathVariable Long meetingId) {
        MeetingInfoVo vo = meetingService.getMeetingInfo(meetingId);
        return R.ok(vo);
    }

    /**
     * 关闭会议
     */
    @Log(title = "会议", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/closeMeeting/{meetingId}")
    public R<Void> closeMeeting(@PathVariable Long meetingId) {
        meetingService.closeMeeting(meetingId);
        return R.ok();
    }

    /**
     * 加入会议
     */
    @Log(title = "会议加入", businessType = BusinessType.INSERT, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/joinMeeting/{meetingId}")
    public R<MeetingJoinResp> joinMeeting(@PathVariable Long meetingId) {
        MeetingJoinResp resp = meetingService.joinMeeting(meetingId);
        return R.ok(resp);
    }

    /**
     * 退出会议
     */
    @Log(title = "会议退出", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/exitMeeting/{meetingId}")
    public R<Void> exitMeeting(@PathVariable Long meetingId) {
        meetingService.exitMeeting(meetingId);
        return R.ok();
    }

    /**
     * 添加参与者
     */
    @Log(title = "会议参与者", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/addParticipants")
    public R<Void> addParticipants(@RequestBody CallParticipantsBo bo) {
        meetingService.addParticipants(bo);
        return R.ok();
    }

    /**
     * 重新呼叫参与者
     */
    @Log(title = "会议参与者重呼", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/recallParticipants")
    public R<Void> recallParticipants(@RequestBody CallParticipantsBo bo) {
        meetingService.recallParticipants(bo);
        return R.ok();
    }

    /**
     * 移除参与者
     */
    @Log(title = "会议参与者", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/removeParticipants")
    public R<Void> removeParticipants(@RequestBody CallParticipantsBo bo) {
        meetingService.removeParticipants(bo);
        return R.ok();
    }


}
