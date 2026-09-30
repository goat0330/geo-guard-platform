/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.service;

/**
 * 认证短信的独立接入边界，避免登录模块强制加载地灾业务短信与审计表。
 */
public interface AuthSmsSender {
    /**
     * 仅在通道确认发送成功时返回 true；调用方据此决定是否生成有效验证码缓存。
     */
    boolean send(String content, String phone);
}
