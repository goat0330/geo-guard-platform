/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service.impl;

import cn.edu.pku.whai.geological.disaster.service.meeting.service.MeetingSmsSender;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将会议邀请短信接入统一短信服务，保留会议侧的业务类型和对象信息用于审计及动态开关。
 */
@Component
@RequiredArgsConstructor
public class MeetingSmsSenderImpl implements MeetingSmsSender {

    private static final String MEETING_INVITE_SMS_TYPE = "专家会商邀请";

    private final SmsSendService smsSendService;

    /**
     * 统一使用会议邀请场景发送并记录短信，避免会议模块直接依赖短信实现细节。
     *
     * @param content       短信正文
     * @param phone         目标手机号
     * @param bizType       业务类型
     * @param bizId         业务对象 ID
     * @param receiverName  接收人姓名
     * @return 真实短信通道是否确认发送成功
     */
    @Override
    public boolean send(String content, String phone, Integer bizType, Long bizId, String receiverName) {
        Boolean sent = smsSendService.send(content, phone, SmsSendContext.builder()
            .scene(SmsScene.MEETING_INVITE)
            .bizType(bizType)
            .bizId(bizId)
            .smsType(MEETING_INVITE_SMS_TYPE)
            .receiverName(receiverName)
            .build());
        return Boolean.TRUE.equals(sent);
    }
}
