package org.dromara.common.web.config;

import org.dromara.common.web.config.properties.CaptchaProperties;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 验证码配置
 *
 * @author Lion Li
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaConfig {

    @Bean
    public CircleCaptcha circleCaptcha() {
        return new CircleCaptcha(200, 80);
    }

    @Bean
    public LineCaptcha lineCaptcha() {
        return new LineCaptcha(200, 80);
    }

    @Bean
    public ShearCaptcha shearCaptcha() {
        return new ShearCaptcha(200, 80);
    }
}
