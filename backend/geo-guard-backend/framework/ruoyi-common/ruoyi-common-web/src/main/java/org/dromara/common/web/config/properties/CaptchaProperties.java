package org.dromara.common.web.config.properties;

import lombok.Data;
import org.dromara.common.web.enums.CaptchaCategory;
import org.dromara.common.web.enums.CaptchaType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码 配置属性
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    private Boolean enable = false;

    /**
     * 验证码类型
     */
    private CaptchaType type = CaptchaType.MATH;

    /**
     * 验证码图片样式
     */
    private CaptchaCategory category = CaptchaCategory.CIRCLE;

    /**
     * 数字验证码位数
     */
    private Integer numberLength = 1;

    /**
     * 字符验证码长度
     */
    private Integer charLength = 4;
}
