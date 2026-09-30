/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.controller;

import org.dromara.common.mail.config.properties.MailProperties;
import org.dromara.common.web.config.properties.CaptchaProperties;
import cn.edu.pku.whai.geological.disaster.service.auth.service.AuthSmsSender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("dev")
class CaptchaControllerMigrationTest {
    /**
     * 缺少短信集成时容器仍可装配，且接口显式失败，不连接 Redis 或发送真实短信。
     */
    @Test
    void shouldStartWithoutSmsAndRejectUnavailableChannel() {
        try (var context = context()) {
            context.refresh();
            var result = context.getBean(CaptchaController.class).smsCode("13800000000");
            assertThat(result.getCode()).isNotEqualTo(200);
            assertThat(result.getMsg()).isEqualTo("短信验证码通道尚未配置");
        }
    }

    /**
     * 通道拒绝发送时不得缓存一个无法接收的验证码；没有 Redis 的测试环境可固定此边界。
     */
    @Test
    void shouldRejectFailedSmsDeliveryWithoutCachingCode() {
        try (var context = context()) {
            context.registerBean(AuthSmsSender.class, () -> (content, phone) -> false);
            context.refresh();
            var result = context.getBean(CaptchaController.class).smsCode("13800000000");
            assertThat(result.getCode()).isNotEqualTo(200);
            assertThat(result.getMsg()).contains("不可用");
        }
    }

    /**
     * 只装配认证入口及属性依赖，使测试不会触达共享数据库和外部消息服务。
     */
    private AnnotationConfigApplicationContext context() {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(CaptchaProperties.class, () -> mock(CaptchaProperties.class));
        context.registerBean(MailProperties.class, () -> mock(MailProperties.class));
        context.register(CaptchaController.class);
        return context;
    }
}
