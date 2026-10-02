package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import org.dromara.common.satoken.config.SaTokenConfig;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaTokenAsyncContextTest {
    @Test
    void contextFilterCoversRequestAndAsyncWithoutAddingNestedErrorDispatch() throws Exception {
        SaTokenContextFilterForJakartaServlet filter = new SaTokenContextFilterForJakartaServlet();
        var registration = new SaTokenConfig().saTokenContextRegistration(filter);
        ServletContext servletContext = mock(ServletContext.class);
        FilterRegistration.Dynamic mapping = mock(FilterRegistration.Dynamic.class);
        when(servletContext.addFilter(anyString(), eq(filter))).thenReturn(mapping);

        registration.onStartup(servletContext);

        assertThat(registration.getFilter()).isSameAs(filter);
        assertThat(registration.getOrder()).isEqualTo(SaTokenConsts.SA_TOKEN_CONTEXT_FILTER_ORDER);
        verify(mapping).setAsyncSupported(true);
        verify(mapping).addMappingForUrlPatterns(
            EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "/*");
    }
}
