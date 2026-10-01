/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import okhttp3.*;
import okio.Okio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GeoserverController {

    @Value("${dizai.geoserver.url}")
    private String geoserverUrl;

    // 全局复用 OkHttpClient（带连接池）
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(60))
            .readTimeout(java.time.Duration.ofSeconds(60))
            .build();

    /**
     * 转发 /wms/** 到 GeoServer，支持 GET/POST
     */
    @SaIgnore
    @CrossOrigin
    @RequestMapping("/geoserver/**")
    public void proxyWms(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 构建目标 URL
        String requestUri = request.getRequestURI();
        String pathSuffix = requestUri.substring(requestUri.indexOf("/geoserver"));
        String targetUrl = geoserverUrl + pathSuffix;

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            // 注意：不要二次编码已编码的 query string
            targetUrl += "?" + queryString;
        }

        // 构建 OkHttp Request
        Request.Builder reqBuilder = new Request.Builder().url(targetUrl);

        // 复用客户端请求方法
        String method = request.getMethod();
        RequestBody body = null;

        if ("POST".equalsIgnoreCase(method)) {
            // 读取原始 POST body（如 SLD_BODY 等）
            byte[] content = request.getInputStream().readAllBytes();
            String contentType = request.getContentType();
            MediaType mediaType = contentType != null ? MediaType.get(contentType) : null;
            body = RequestBody.create(content, mediaType);
            reqBuilder.post(body);
        } else {
            reqBuilder.get(); // 默认 GET
        }

        // 可选：传递部分 header（谨慎！避免传递 Host、Authorization 等）
        // reqBuilder.addHeader("X-Forwarded-For", request.getRemoteAddr());

        Request okRequest = reqBuilder.build();

        try (Response okResponse = httpClient.newCall(okRequest).execute()) {
            if (!okResponse.isSuccessful()) {
                response.setStatus(okResponse.code());
                return;
            }

            // 复制响应头（关键：Content-Type、Content-Length 等）
            ResponseBody responseBody = okResponse.body();
            if (responseBody == null) return;

            Headers headers = okResponse.headers();
            for (String name : headers.names()) {
                // 过滤掉 Transfer-Encoding 等 hop-by-hop headers
                if (!"transfer-encoding".equalsIgnoreCase(name)
                        && !"connection".equalsIgnoreCase(name)) {
                    response.setHeader(name, headers.get(name));
                }
            }

            // 设置状态码
            response.setStatus(okResponse.code());

            // 流式写入响应体（不加载全量到内存）
            try (var out = response.getOutputStream()) {
                responseBody.source().readAll(Okio.sink(out));
                out.flush();
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "WMS Proxy Error");
        }
    }
}
