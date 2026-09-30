/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GeoserverController {

    @Value("${dizai.geoserver.url}")
    private String geoserverUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(60))
            .readTimeout(java.time.Duration.ofSeconds(60))
            .build();

    /**
     * 代理 GeoServer 地图请求，目标地址来自配置并透传协议响应，避免业务层复制 GeoServer 契约。
     */
    @SaIgnore
    @CrossOrigin
    @RequestMapping("/geoserver/**")
    public void proxyWms(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUri = request.getRequestURI();
        String pathSuffix = requestUri.substring(requestUri.indexOf("/geoserver"));
        String targetUrl = geoserverUrl + pathSuffix;

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            targetUrl += "?" + queryString;
        }

        Request.Builder reqBuilder = new Request.Builder().url(targetUrl);

        String method = request.getMethod();
        RequestBody body = null;

        if ("POST".equalsIgnoreCase(method)) {
            byte[] content = request.getInputStream().readAllBytes();
            String contentType = request.getContentType();
            MediaType mediaType = contentType != null ? MediaType.get(contentType) : null;
            body = RequestBody.create(content, mediaType);
            reqBuilder.post(body);
        } else {
            reqBuilder.get();
        }

        Request okRequest = reqBuilder.build();

        try (Response okResponse = httpClient.newCall(okRequest).execute()) {
            if (!okResponse.isSuccessful()) {
                response.setStatus(okResponse.code());
                return;
            }

            ResponseBody responseBody = okResponse.body();
            if (responseBody == null) return;

            Headers headers = okResponse.headers();
            for (String name : headers.names()) {
                if (!"transfer-encoding".equalsIgnoreCase(name)
                        && !"connection".equalsIgnoreCase(name)) {
                    response.setHeader(name, headers.get(name));
                }
            }

            response.setStatus(okResponse.code());

            try (var out = response.getOutputStream()) {
                responseBody.source().readAll(Okio.sink(out));
                out.flush();
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "WMS Proxy Error");
        }
    }
}
