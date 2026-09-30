package org.dromara.common.web.enums;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import lombok.Getter;

/** Supported graphical CAPTCHA code generators. */
@Getter
public enum CaptchaType {
    MATH(MathGenerator.class),
    CHAR(RandomGenerator.class);

    private final Class<? extends CodeGenerator> clazz;

    CaptchaType(Class<? extends CodeGenerator> clazz) {
        this.clazz = clazz;
    }
}
