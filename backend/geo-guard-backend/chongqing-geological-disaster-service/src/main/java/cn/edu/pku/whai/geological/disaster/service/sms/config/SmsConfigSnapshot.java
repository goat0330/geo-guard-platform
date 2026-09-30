/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.config;

import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单次短信业务使用的不可变配置快照。
 */
@Value
@Builder(toBuilder = true)
public class SmsConfigSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台总开关 */
    boolean platformEnabled;

    /** 短信平台基础地址 */
    String baseUrl;

    /** 短信平台账号 */
    String account;

    /** 短信平台接口密码 */
    String pwd;

    /** 短信签名 */
    String signature;

    /** 单条发送接口路径 */
    String sendPath;

    /** 批量发送接口路径 */
    String batchSendPath;

    /** 状态报告接口路径 */
    String reportPath;

    /** 重复发送拦截窗口，单位为分钟 */
    int dedupMinutes;

    /** 业务短信总开关 */
    boolean businessEnabled;

    /** 任务推送短信开关 */
    boolean taskPushEnabled;

    /** 允许发送任务短信的任务来源编码 */
    List<Integer> taskPushAllowedSourceTypes;

    /** 群众撤离短信开关 */
    boolean evacuationEnabled;

    /** 防御响应启动短信开关 */
    boolean defRespStartEnabled;

    /** 会议邀请短信开关 */
    boolean meetingInviteEnabled;

    /** 行政审批短信开关 */
    boolean adminApprovalEnabled;

    /** 短信验证码开关 */
    boolean captchaEnabled;

    /** 测试短信开关 */
    boolean testEnabled;

    /** 最后修改人用户 ID */
    Long updatedBy;

    /** 最后修改时间 */
    Date updatedAt;

    /**
     * 生成无可用配置时的安全停发快照。
     *
     * @return 平台关闭的默认快照
     */
    public static SmsConfigSnapshot safeDisabled() {
        return SmsConfigSnapshot.builder()
            .platformEnabled(false)
            .sendPath("/api/v1/send")
            .batchSendPath("/api/v1/batchSend")
            .reportPath("/api/v1/report")
            .dedupMinutes(1)
            .businessEnabled(true)
            .taskPushEnabled(true)
            .taskPushAllowedSourceTypes(List.of(0, 1, 2, 3, 4, 5, 6))
            .evacuationEnabled(true)
            .defRespStartEnabled(true)
            .meetingInviteEnabled(true)
            .adminApprovalEnabled(true)
            .captchaEnabled(true)
            .testEnabled(true)
            .build();
    }

    /**
     * 判断指定场景在当前快照下是否允许发送。
     *
     * @param scene 短信业务场景
     * @param taskSourceType 任务来源编码，仅任务推送场景使用
     * @return 是否允许发送
     */
    public boolean canSend(SmsScene scene, Integer taskSourceType) {
        if (scene == null || !platformEnabled) {
            return false;
        }
        return switch (scene) {
            case CAPTCHA -> captchaEnabled;
            case TEST_SEND -> testEnabled;
            case TASK_PUSH -> businessEnabled && taskPushEnabled
                && taskSourceType != null && taskPushAllowedSourceTypes.contains(taskSourceType);
            case EVACUATION -> businessEnabled && evacuationEnabled;
            case DEF_RESP_START -> businessEnabled && defRespStartEnabled;
            case MEETING_INVITE -> businessEnabled && meetingInviteEnabled;
            case ADMIN_APPROVAL -> businessEnabled && adminApprovalEnabled;
        };
    }
}
