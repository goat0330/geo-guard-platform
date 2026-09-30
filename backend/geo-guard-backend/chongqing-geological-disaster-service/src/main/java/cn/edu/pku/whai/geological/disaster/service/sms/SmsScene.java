/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

/**
 * 短信发送业务场景。
 *
 * @author system
 */
public enum SmsScene {

    /**
     * 任务推送。
     */
    TASK_PUSH,

    /**
     * 群众撤离。
     */
    EVACUATION,

    /**
     * 防御响应启动。
     */
    DEF_RESP_START,

    /**
     * 会议邀请。
     */
    MEETING_INVITE,

    /**
     * 行政审批。
     */
    ADMIN_APPROVAL,

    /**
     * 短信验证码。
     */
    CAPTCHA,

    /**
     * 测试短信。
     */
    TEST_SEND
}
