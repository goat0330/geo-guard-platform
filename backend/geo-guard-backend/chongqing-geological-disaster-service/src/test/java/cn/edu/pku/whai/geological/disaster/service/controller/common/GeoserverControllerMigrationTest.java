/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GeoServer 代理迁移边界测试，只使用回环 HTTP fixture，不连接真实地图服务。
 */
@Tag("dev")
class GeoserverControllerMigrationTest {

    /**
     * 原始 query 已含百分号编码时，代理应保持编码文本，避免 OkHttp URL 构造产生二次编码。
     */
    @Test
    void shouldPreserveEncodedQueryWithoutDoubleEncoding() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        try (LocalGeoServer server = LocalGeoServer.start(exchange -> {
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            send(exchange, 200, "ok");
        })) {
            HttpServletRequest request = request("GET", "/geoserver/wms",
                    "CQL_FILTER=name%3D%27A%2520B%27&token=a%2Bb");
            ResponseCapture response = new ResponseCapture();

            controller(server).proxyWms(request, response.response());

            assertThat(rawQuery).hasValue("CQL_FILTER=name%3D%27A%2520B%27&token=a%2Bb");
            assertThat(response.status()).isEqualTo(200);
            assertThat(response.content()).isEqualTo("ok");
        }
    }

    /**
     * POST 请求的实体和媒体类型需要原样交给 GeoServer，以保持 WMS/WFS 表单请求契约。
     */
    @Test
    void shouldForwardPostBodyAndContentType() throws Exception {
        AtomicReference<byte[]> body = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        try (LocalGeoServer server = LocalGeoServer.start(exchange -> {
            body.set(exchange.getRequestBody().readAllBytes());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            send(exchange, 200, "accepted");
        })) {
            HttpServletRequest request = request("POST", "/geoserver/wms", null,
                    "application/x-www-form-urlencoded; charset=UTF-8",
                    "service=WMS&request=GetMap".getBytes(StandardCharsets.UTF_8));
            ResponseCapture response = new ResponseCapture();

            controller(server).proxyWms(request, response.response());

            assertThat(body.get()).containsExactly("service=WMS&request=GetMap".getBytes(StandardCharsets.UTF_8));
            assertThat(contentType).hasValue("application/x-www-form-urlencoded; charset=UTF-8");
            assertThat(response.status()).isEqualTo(200);
        }
    }

    /**
     * 上游非 2xx 响应只透传状态码，避免代理把 GeoServer 的错误语义伪装成成功响应。
     */
    @Test
    void shouldPreserveUpstreamErrorStatus() throws Exception {
        try (LocalGeoServer server = LocalGeoServer.start(exchange -> send(exchange, 503, "unavailable"))) {
            HttpServletRequest request = request("GET", "/geoserver/wms", null);
            ResponseCapture response = new ResponseCapture();

            controller(server).proxyWms(request, response.response());

            assertThat(response.status()).isEqualTo(503);
        }
    }

    /**
     * 构造只指向本地 fixture 的 Controller，并通过反射注入配置字段，避免加载完整 Spring 容器。
     */
    private GeoserverController controller(LocalGeoServer server) throws ReflectiveOperationException {
        GeoserverController controller = new GeoserverController();
        Field field = GeoserverController.class.getDeclaredField("geoserverUrl");
        field.setAccessible(true);
        field.set(controller, server.baseUrl());
        return controller;
    }

    /**
     * 创建带原始 query 字符串的 Servlet 测试请求，保证测试覆盖代理的 URL 拼接边界。
     */
    private HttpServletRequest request(String method, String uri, String queryString) throws IOException {
        return request(method, uri, queryString, null, new byte[0]);
    }

    /**
     * 创建 Mockito Servlet 请求替身，避免迁移测试引入完整 Spring Test 依赖。
     */
    private HttpServletRequest request(String method, String uri, String queryString,
                                       String contentType, byte[] content) throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getQueryString()).thenReturn(queryString);
        when(request.getContentType()).thenReturn(contentType);
        when(request.getInputStream()).thenReturn(inputStream(content));
        return request;
    }

    /**
     * 将测试请求字节包装成 Servlet 输入流，保持代理读取 request body 的方式不变。
     */
    private static ServletInputStream inputStream(byte[] content) {
        ByteArrayInputStream input = new ByteArrayInputStream(content);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 向本地 fixture 返回指定状态和文本，统一处理 HttpExchange 的响应资源。
     */
    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, content.length);
        try (var output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    /**
     * 捕获代理写入的 Servlet 状态和响应体，且不依赖 Spring Mock Web 实现。
     */
    private static final class ResponseCapture {
        private final HttpServletResponse response = mock(HttpServletResponse.class);
        private final AtomicReference<Integer> status = new AtomicReference<>(HttpServletResponse.SC_OK);
        private final ByteArrayOutputStream content = new ByteArrayOutputStream();

        private ResponseCapture() throws IOException {
            doAnswer(invocation -> {
                status.set(invocation.getArgument(0));
                return null;
            }).when(response).setStatus(anyInt());
            doAnswer(invocation -> {
                status.set(invocation.getArgument(0));
                return null;
            }).when(response).sendError(anyInt(), anyString());
            when(response.getOutputStream()).thenReturn(outputStream());
        }

        /**
         * 返回请求处理使用的 Servlet 响应替身。
         */
        private HttpServletResponse response() {
            return response;
        }

        /**
         * 返回代理最终写入的 HTTP 状态。
         */
        private int status() {
            return status.get();
        }

        /**
         * 返回代理写入的 UTF-8 响应体文本。
         */
        private String content() {
            return content.toString(StandardCharsets.UTF_8);
        }

        /**
         * 创建写入捕获缓冲区的 Servlet 输出流。
         */
        private ServletOutputStream outputStream() {
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int value) {
                    content.write(value);
                }
            };
        }
    }

    /**
     * 封装回环 GeoServer 替身的生命周期，避免测试留下监听端口或引入外部服务依赖。
     */
    private static final class LocalGeoServer implements AutoCloseable {
        private final HttpServer server;

        private LocalGeoServer(HttpServer server) {
            this.server = server;
        }

        /**
         * 在随机回环端口启动 HTTP fixture，测试之间互不共享网络状态。
         */
        private static LocalGeoServer start(com.sun.net.httpserver.HttpHandler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/geoserver", handler);
            server.start();
            return new LocalGeoServer(server);
        }

        /**
         * 返回 GeoServer 基础地址，和生产配置字段保持同样的拼接形态。
         */
        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        /**
         * 停止本地 fixture，释放随机端口。
         */
        @Override
        public void close() {
            server.stop(0);
        }
    }
}
