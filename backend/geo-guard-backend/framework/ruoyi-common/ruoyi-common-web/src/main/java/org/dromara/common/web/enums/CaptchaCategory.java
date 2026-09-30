package org.dromara.common.web.enums;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import lombok.Getter;

/** Image styles for graphical CAPTCHAs. */
@Getter
public enum CaptchaCategory {
    CIRCLE(CircleCaptcha.class),
    LINE(LineCaptcha.class),
    SHEAR(ShearCaptcha.class);

    private final Class<? extends AbstractCaptcha> clazz;

    CaptchaCategory(Class<? extends AbstractCaptcha> clazz) {
        this.clazz = clazz;
    }
}
