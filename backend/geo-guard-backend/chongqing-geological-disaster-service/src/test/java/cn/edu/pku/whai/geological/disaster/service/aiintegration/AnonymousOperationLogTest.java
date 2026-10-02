package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.aspect.LogAspect;
import org.dromara.common.log.event.OperLogEvent;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnonymousOperationLogTest {
    @Log(title = "Auth action", isSaveRequestData = false, isSaveResponseData = false)
    void action() {}

    @Test
    void anonymousOrLoggedOutRequestRecordsLogWithoutOpeningAnInvalidTokenSession() throws Exception {
        verifyOperationLog(false);
    }

    @Test
    void authenticatedRequestPreservesOperatorIdentity() throws Exception {
        verifyOperationLog(true);
    }

    private void verifyOperationLog(boolean authenticated) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/code");
        when(request.getMethod()).thenReturn("GET");
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("action");
        when(joinPoint.getTarget()).thenReturn(this);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Log annotation = getClass().getDeclaredMethod("action").getAnnotation(Log.class);
        try (var login = mockStatic(LoginHelper.class);
             var servlet = mockStatic(ServletUtils.class);
             var spring = mockStatic(SpringUtils.class)) {
            login.when(LoginHelper::getUserId).thenReturn(authenticated ? 1L : null);
            if (authenticated) {
                LoginUser user = new LoginUser();
                user.setUsername("admin");
                user.setDeptName("local");
                login.when(LoginHelper::getLoginUser).thenReturn(user);
            } else {
                login.when(LoginHelper::getLoginUser).thenThrow(new IllegalStateException("Invalid token session"));
            }
            servlet.when(ServletUtils::getRequest).thenReturn(request);
            servlet.when(ServletUtils::getClientIP).thenReturn("127.0.0.1");
            spring.when(SpringUtils::context).thenReturn(context);
            LogAspect aspect = new LogAspect();
            aspect.doBefore(joinPoint, annotation);
            aspect.doAfterReturning(joinPoint, annotation, null);

            ArgumentCaptor<OperLogEvent> event = ArgumentCaptor.forClass(OperLogEvent.class);
            verify(context).publishEvent(event.capture());
            assertThat(event.getValue().getTitle()).isEqualTo("Auth action");
            assertThat(event.getValue().getOperName()).isEqualTo(authenticated ? "admin" : null);
            if (!authenticated) login.verify(LoginHelper::getLoginUser, never());
        }
    }
}
