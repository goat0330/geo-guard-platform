/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口受全局模拟/联调开关控制。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimulationEnabledRequired {

    /**
     * 开关关闭时返回的业务提示。
     */
    String message() default "模拟/联调接口已关闭";
}
