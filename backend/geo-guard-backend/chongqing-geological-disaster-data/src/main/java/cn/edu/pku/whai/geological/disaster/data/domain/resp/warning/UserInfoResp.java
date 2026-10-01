/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信息实体类
 */
@Data
public class UserInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Alias("userid")
    private String userId;

    /**
     * 用户名
     */
    @Alias("username")
    private String userName;

    /**
     * 人员姓名
     */
    @Alias("personname")
    private String personName;

    /**
     * 加密密码
     */
    @Alias("hpassword")
    private String encryptPassword;

    /**
     * 用户类型
     */
    @Alias("usertype")
    private String userType;

    /**
     * 手机号（加密）
     */
    @Alias("mobile")
    private String mobile;

    /**
     * 邮箱
     */
    @Alias("email")
    private String email;

    /**
     * 身份证号
     */
    @Alias("identification")
    private String identification;

    /**
     * 状态
     */
    @Alias("status")
    private String status;

    /**
     * 头像
     */
    @Alias("icon")
    private String icon;

    /**
     * 描述
     */
    @Alias("description")
    private String description;

    /**
     * 创建人ID
     */
    @Alias("cuserid")
    private String createUserId;

    /**
     * 创建时间
     */
    @Alias("ctime")
    private String createTime;

    /**
     * 更新时间
     */
    @Alias("utime")
    private String updateTime;

    /**
     * 序号
     */
    @Alias("seqno")
    private String seqNo;

    /**
     * 是否启用
     */
    @Alias("enable")
    private String enable;

    /**
     * 凭证
     */
    @Alias("credential")
    private String credential;

    /**
     * 验证码
     */
    @Alias("vc")
    private String verifyCode;

    /**
     * 会话ID
     */
    @Alias("session")
    private String sessionId;

    /**
     * 客户端ID
     */
    @Alias("cid")
    private String clientId;

    /**
     * CBC MAC值（加密相关）
     */
    @Alias("cbcmac")
    private String cbcMac;

    /**
     * 区域ID列表
     */
    @Alias("regionids")
    private String regionIds;

    /**
     * 区域名称列表
     */
    @Alias("regionNames")
    private String regionNames;

    /**
     * 机构ID列表
     */
    @Alias("orgids")
    private String orgIds;

    /**
     * 用户扩展信息（包含角色）
     */
    @Alias("info")
    private UserExtraInfoResp extraInfo;
}