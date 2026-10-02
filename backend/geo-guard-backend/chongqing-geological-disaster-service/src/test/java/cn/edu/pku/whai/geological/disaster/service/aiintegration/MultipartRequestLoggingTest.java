package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MultipartRequestLoggingTest {
    @Test
    void multipartLoggingDoesNotParseOrConsumeTheUpload() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/dizai/ai/rag/api/v1/documents/upload");
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=test");
        PlusWebInvokeTimeInterceptor interceptor = new PlusWebInvokeTimeInterceptor();
        try {
            assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
            verify(request, never()).getParameterMap();
            verify(request, never()).getInputStream();
            verify(request, never()).getParts();
        } finally {
            interceptor.afterCompletion(request, response, new Object(), null);
        }
    }
}
