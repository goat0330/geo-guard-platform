package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Component
public class GeoAiProxyService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String ragBaseUrl;
    private final String graphBaseUrl;

    @Autowired
    public GeoAiProxyService(
            ObjectMapper objectMapper,
            @Value("${geo.ai.rag.base-url:http://127.0.0.1:8010}") String ragBaseUrl,
            @Value("${geo.ai.graph.base-url:http://127.0.0.1:8011}") String graphBaseUrl) {
        this(objectMapper, HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10)).build(), ragBaseUrl, graphBaseUrl);
    }

    public GeoAiProxyService(ObjectMapper objectMapper, HttpClient httpClient, String ragBaseUrl, String graphBaseUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.ragBaseUrl = trimSlash(ragBaseUrl);
        this.graphBaseUrl = trimSlash(graphBaseUrl);
    }

    public ResponseEntity<byte[]> postRag(String path, JsonNode body) throws IOException, InterruptedException {
        return postJson(ragBaseUrl, path, body, true);
    }

    public ResponseEntity<byte[]> postGraph(String path, JsonNode body) throws IOException, InterruptedException {
        return postJson(graphBaseUrl, path, body, false);
    }

    public ProxyStream streamGraph(String path, JsonNode body) throws IOException, InterruptedException {
        HttpRequest request = request(graphBaseUrl, path, body, false);
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        MediaType contentType = contentType(response.headers().firstValue("Content-Type").orElse(null));
        InputStream responseBody = response.body();
        if (response.statusCode() >= 400) {
            byte[] errorBody;
            try (InputStream errorStream = responseBody) {
                errorBody = errorStream.readAllBytes();
            }
            responseBody = new ByteArrayInputStream(errorBody);
        }
        return new ProxyStream(response.statusCode(), contentType, responseBody);
    }

    public ProxyApiResponse proxyRagApi(HttpServletRequest servletRequest) throws IOException, InterruptedException {
        String prefix = "/dizai/ai/rag";
        String requestUri = servletRequest.getRequestURI();
        int prefixIndex = requestUri.indexOf(prefix);
        String path = prefixIndex < 0 ? "/" : requestUri.substring(prefixIndex + prefix.length());
        if (path.isEmpty()) path = "/";
        String query = servletRequest.getQueryString();
        URI target = URI.create(ragBaseUrl + path + (query == null ? "" : "?" + query));

        HttpRequest.Builder builder = HttpRequest.newBuilder(target);
        servletRequest.getHeaderNames().asIterator().forEachRemaining(name -> {
            if (name.equalsIgnoreCase(HttpHeaders.ACCEPT) || name.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                servletRequest.getHeaders(name).asIterator().forEachRemaining(value -> builder.header(name, value));
            }
        });
        addRagUserContext(builder);
        String accept = servletRequest.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && !accept.toLowerCase(Locale.ROOT).contains("text/event-stream")) {
            builder.timeout(Duration.ofMinutes(5));
        }
        String method = servletRequest.getMethod();
        GatewayResponse gateway = send(servletRequest, builder, method);
        HttpResponse<InputStream> response = gateway.response();
        InputStream responseBody = response.body();
        if (gateway.bodyFile() != null) {
            Path bodyFile = gateway.bodyFile();
            responseBody = new FilterInputStream(responseBody) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        Files.deleteIfExists(bodyFile);
                    }
                }
            };
        }
        HttpHeaders headers = new HttpHeaders();
        response.headers().map().forEach((name, values) -> {
            if (Set.of("content-type", "content-disposition", "cache-control", "etag", "last-modified", "retry-after",
                    "x-yuxi-preview-type", "x-yuxi-preview-filename")
                    .contains(name.toLowerCase(Locale.ROOT))) {
                values.forEach(value -> headers.add(name, value));
            }
        });
        headers.set("X-Accel-Buffering", "no");
        return new ProxyApiResponse(response.statusCode(), headers, responseBody);
    }

    private GatewayResponse send(HttpServletRequest request, HttpRequest.Builder builder, String method)
            throws IOException, InterruptedException {
        if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD")) {
            HttpResponse<InputStream> response = httpClient.send(
                    builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            return new GatewayResponse(response, null);
        }
        Path bodyFile = Files.createTempFile("geo-ai-rag-proxy-", ".body");
        try {
            try (InputStream body = request.getInputStream()) {
                Files.copy(body, bodyFile, StandardCopyOption.REPLACE_EXISTING);
            }
            HttpRequest.BodyPublisher fileBody = HttpRequest.BodyPublishers.ofInputStream(() -> {
                try {
                    return Files.newInputStream(bodyFile);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
            HttpResponse<InputStream> response = httpClient.send(
                    builder.method(method, fileBody).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            return new GatewayResponse(response, bodyFile);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            Files.deleteIfExists(bodyFile);
            throw exception;
        }
    }

    private record GatewayResponse(HttpResponse<InputStream> response, Path bodyFile) {}

    public record ProxyApiResponse(int status, HttpHeaders headers, InputStream body) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            body.close();
        }
    }
    private ResponseEntity<byte[]> postJson(String baseUrl, String path, JsonNode body, boolean includeUserContext)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(baseUrl, path, body, true);
        if (includeUserContext) {
            addRagUserContext(builder);
        }
        HttpResponse<byte[]> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType(response.headers().firstValue("Content-Type").orElse(null)));
        return new ResponseEntity<>(response.body(), headers, HttpStatusCode.valueOf(response.statusCode()));
    }

    private HttpRequest request(String baseUrl, String path, JsonNode body, boolean withTimeout) throws IOException {
        return requestBuilder(baseUrl, path, body, withTimeout).build();
    }

    private HttpRequest.Builder requestBuilder(String baseUrl, String path, JsonNode body, boolean withTimeout) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, withTimeout ? MediaType.APPLICATION_JSON_VALUE : MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
        if (withTimeout) {
            builder.timeout(Duration.ofMinutes(5));
        }
        return builder;
    }

    private static void addRagUserContext(HttpRequest.Builder builder) {
        String userId = LoginHelper.getUserIdStr();
        if (userId == null || userId.isBlank()) {
            // Embedded AI Studio keeps the local RAG workspace login-free.
            return;
        }

        String role = "user";
        try {
            if (LoginHelper.isSuperAdmin()) {
                role = "superadmin";
            } else if (LoginHelper.isTenantAdmin()) {
                role = "admin";
            }
        } catch (RuntimeException ignored) {
            // Keep the authenticated user at the least-privileged role if role context is unavailable.
        }
        builder.header("X-Geo-User-Uid", userId);
        builder.header("X-Geo-User-Role", role);
        Long departmentId = LoginHelper.getDeptId();
        if (departmentId != null) {
            builder.header("X-Geo-User-Department-Id", departmentId.toString());
        }
    }

    private static String trimSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static MediaType contentType(String value) {
        if (value == null || value.isBlank()) {
            return MediaType.APPLICATION_JSON;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_JSON;
        }
    }

    public record ProxyStream(int status, MediaType contentType, InputStream body) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            body.close();
        }
    }
}
