package cn.edu.pku.whai.geological.disaster.service.app.config;

import cn.edu.pku.whai.geological.disaster.service.app.interceptor.AppAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 拦截器配置类
 * 配置需要进行认证校验的路径
 *
 * @author kongweiguang
 */
@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final AppAuthInterceptor appAuthInterceptor;
    // APP任务相关接口
    // 需要进行认证校验的路径前缀
    private static final List<String> AUTH_PATHS = List.of(
            "/v1/tasks/**"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appAuthInterceptor)
                .addPathPatterns(AUTH_PATHS)
                // 可以排除不需要认证的路径
                .excludePathPatterns("/v1/tasks/public/**");
    }
}