/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 短信动态配置展示对象。
 */
@Data
@ExcelIgnoreUnannotated
public class SmsConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台总开关 */
    @ExcelProperty(value = "平台总开关")
    private Boolean platformEnabled;

    /** 短信平台基础地址 */
    @ExcelProperty(value = "平台基础地址")
    private String baseUrl;

    /** 平台账号是否已配置，不返回账号原值 */
    @ExcelProperty(value = "平台账号已配置")
    private Boolean accountConfigured;

    /** 平台接口密码是否已配置，不返回密码原值 */
    @ExcelProperty(value = "平台密码已配置")
    private Boolean pwdConfigured;

    /** 短信签名 */
    @ExcelProperty(value = "短信签名")
    private String signature;

    /** 单条发送接口路径 */
    @ExcelProperty(value = "单条发送接口路径")
    private String sendPath;

    /** 批量发送接口路径 */
    @ExcelProperty(value = "批量发送接口路径")
    private String batchSendPath;

    /** 状态报告接口路径 */
    @ExcelProperty(value = "状态报告接口路径")
    private String reportPath;

    /** 重复发送拦截窗口，单位为分钟，0 表示关闭去重 */
    @ExcelProperty(value = "去重分钟数")
    private Integer dedupMinutes;

    /** 业务短信总开关 */
    @ExcelProperty(value = "业务短信总开关")
    private Boolean businessEnabled;

    /** 任务推送短信开关 */
    @ExcelProperty(value = "任务推送短信开关")
    private Boolean taskPushEnabled;

    /** 允许发送任务短信的任务来源编码，空列表表示全部停发 */
    private List<Integer> taskPushAllowedSourceTypes;

    /** 群众撤离短信开关 */
    @ExcelProperty(value = "群众撤离短信开关")
    private Boolean evacuationEnabled;

    /** 防御响应启动短信开关 */
    @ExcelProperty(value = "防御响应启动短信开关")
    private Boolean defRespStartEnabled;

    /** 会议邀请短信开关 */
    @ExcelProperty(value = "会议邀请短信开关")
    private Boolean meetingInviteEnabled;

    /** 行政审批短信开关 */
    @ExcelProperty(value = "行政审批短信开关")
    private Boolean adminApprovalEnabled;

    /** 短信验证码开关 */
    @ExcelProperty(value = "短信验证码开关")
    private Boolean captchaEnabled;

    /** 测试短信开关 */
    @ExcelProperty(value = "测试短信开关")
    private Boolean testEnabled;

    /** 最后修改人用户 ID */
    @ExcelProperty(value = "最后修改人")
    private Long updatedBy;

    /** 最后修改时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "最后修改时间")
    private Date updatedAt;
}
