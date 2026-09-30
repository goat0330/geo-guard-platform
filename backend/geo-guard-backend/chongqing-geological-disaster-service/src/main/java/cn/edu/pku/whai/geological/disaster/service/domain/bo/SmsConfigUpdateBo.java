/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 短信动态配置更新参数。
 */
@Data
public class SmsConfigUpdateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台总开关 */
    private Boolean platformEnabled;

    /** 短信平台基础地址 */
    private String baseUrl;

    /** 短信平台账号，仅用于写入 */
    @JsonAlias("accountConfigured")
    private String account;

    /** 短信平台接口密码，仅用于写入 */
    @JsonAlias("pwdConfigured")
    private String pwd;

    /** 短信签名 */
    private String signature;

    /** 单条发送接口路径 */
    private String sendPath;

    /** 批量发送接口路径 */
    private String batchSendPath;

    /** 状态报告接口路径 */
    private String reportPath;

    /** 重复发送拦截窗口，单位为分钟，0 表示关闭去重 */
    private Integer dedupMinutes;

    /** 业务短信总开关 */
    private Boolean businessEnabled;

    /** 任务推送短信开关 */
    private Boolean taskPushEnabled;

    /** 允许发送任务短信的任务来源编码，空列表表示全部停发 */
    private List<Integer> taskPushAllowedSourceTypes;

    /** 群众撤离短信开关 */
    private Boolean evacuationEnabled;

    /** 防御响应启动短信开关 */
    private Boolean defRespStartEnabled;

    /** 会议邀请短信开关 */
    private Boolean meetingInviteEnabled;

    /** 行政审批短信开关 */
    private Boolean adminApprovalEnabled;

    /** 短信验证码开关 */
    private Boolean captchaEnabled;

    /** 测试短信开关 */
    private Boolean testEnabled;
}
