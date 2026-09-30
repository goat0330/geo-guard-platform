package cn.edu.pku.whai.geological.disaster.service.app.interceptor;

import cn.edu.pku.whai.geological.disaster.service.app.utils.AppAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * APP认证拦截器
 * 对指定路径进行统一的认证校验
 *
 * @author kongweiguang
 */
@Component
public class AppAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 执行认证校验
        AppAuthUtil.isAuth();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) throws Exception {

    }
}