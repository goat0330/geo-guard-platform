/* @author kongweiguang */
package cn.edu.pku.whai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 默认保留原业务定时执行；迁移验收可单独停用触发器，而不关闭或替换任何业务 Bean。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessSchedulingConfiguration {
}
