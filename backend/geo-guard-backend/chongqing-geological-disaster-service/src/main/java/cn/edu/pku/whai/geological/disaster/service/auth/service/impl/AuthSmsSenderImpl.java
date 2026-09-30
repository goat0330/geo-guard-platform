/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.service.impl;

import cn.edu.pku.whai.geological.disaster.service.auth.service.AuthSmsSender;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将认证验证码发送接入统一短信服务，确保验证码只在真实通道发送成功后写入缓存。
 */
@Component
@RequiredArgsConstructor
public class AuthSmsSenderImpl implements AuthSmsSender {

    private final SmsSendService smsSendService;

    /**
     * 使用独立的验证码场景交给统一短信服务处理开关、去重和平台调用，避免认证模块复制通道逻辑。
     *
     * @param content 验证码短信正文
     * @param phone   目标手机号
     * @return 真实短信通道是否确认发送成功
     */
    @Override
    public boolean send(String content, String phone) {
        Boolean sent = smsSendService.send(content, phone, SmsSendContext.builder()
            .scene(SmsScene.CAPTCHA)
            .build());
        return Boolean.TRUE.equals(sent);
    }
}
