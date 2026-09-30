package cn.edu.pku.whai.geological.disaster.service.meeting.service;

/**
 * 会议邀请短信的可选接入端口。
 *
 * @author kongweiguang
 */
@FunctionalInterface
public interface MeetingSmsSender {

    /**
     * 保持会议模块与短信审计、供应商实现解耦；只有实际通道确认发送成功时才返回 true。
     *
     * @param content       短信正文
     * @param phone         目标手机号
     * @param bizType       业务类型，1 为任务推送，3 为防御响应
     * @param bizId         业务对象 ID
     * @param receiverName  接收人姓名
     * @return 是否由真实短信通道发送成功
     */
    boolean send(String content, String phone, Integer bizType, Long bizId, String receiverName);

    int BIZ_TYPE_TASK_PUSH = 1;
    int BIZ_TYPE_DEF_RESP = 3;
}
