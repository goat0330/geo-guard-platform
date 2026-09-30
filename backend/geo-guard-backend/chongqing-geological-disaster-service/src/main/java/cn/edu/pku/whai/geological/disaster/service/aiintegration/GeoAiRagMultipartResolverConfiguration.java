package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class GeoAiRagMultipartResolverConfiguration {
    @Bean("multipartResolver")
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver() {
            @Override
            public boolean isMultipart(HttpServletRequest request) {
                String requestUri = request.getRequestURI();
                String contextPath = request.getContextPath();
                String path = requestUri.substring(Math.min(contextPath.length(), requestUri.length()));
                if (path.equals("/dizai/ai/rag") || path.startsWith("/dizai/ai/rag/")) {
                    return false;
                }
                return super.isMultipart(request);
            }
        };
    }
}
