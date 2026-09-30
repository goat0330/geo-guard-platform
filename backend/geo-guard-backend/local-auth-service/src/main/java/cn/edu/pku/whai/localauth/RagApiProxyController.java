package cn.edu.pku.whai.localauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class RagApiProxyController {
    private static final String PREFIX = "/dizai/ai/rag";
    private static final String RAG_API_PREFIX = "/geo-ai-rag";
    private static final String KNOWLEDGE_PREFIX = "/api/knowledge";
    private static final String EVALUATION_PREFIX = "/api/evaluation";
    private static final String GRAPH_PREFIX = "/api/graph";
    private static final Set<String> REQUEST_HEADERS = Set.of("accept", "content-type");
    private static final Set<String> RESPONSE_HEADERS = Set.of(
        "content-type", "content-disposition", "cache-control", "etag", "last-modified", "retry-after"
    );

    private final HttpClient client;
    private final String ragBaseUrl;

    public RagApiProxyController(
        @Value("$" + "{geo.ai.rag.base-url:http://127.0.0.1:8010}") String ragBaseUrl
    ) {
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();
        this.ragBaseUrl = ragBaseUrl.replaceAll("/+$", "");
    }

    @RequestMapping(
        path = {PREFIX, PREFIX + "/**", RAG_API_PREFIX, RAG_API_PREFIX + "/**",
            KNOWLEDGE_PREFIX, KNOWLEDGE_PREFIX + "/**",
            EVALUATION_PREFIX, EVALUATION_PREFIX + "/**",
            GRAPH_PREFIX, GRAPH_PREFIX + "/**"},
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
            RequestMethod.DELETE, RequestMethod.HEAD}
    )
    public ResponseEntity<StreamingResponseBody> proxy(HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            String path;
            if (uri.startsWith(KNOWLEDGE_PREFIX)) {
                path = uri;
            } else if (uri.startsWith(EVALUATION_PREFIX)) {
                path = uri;
            } else if (uri.startsWith(GRAPH_PREFIX)) {
                path = uri;
            } else if (uri.startsWith(RAG_API_PREFIX)) {
                path = uri.substring(RAG_API_PREFIX.length());
                if (path.isEmpty()) path = "/";
            } else if (uri.startsWith(PREFIX)) {
                path = uri.substring(PREFIX.length());
                if (path.isEmpty()) path = "/";
                if (path.equals("/retrieve")) path = "/api/v1/retrieve";
                else if (path.equals("/debug/retrieve")) path = "/api/v1/debug/retrieve";
            } else {
                return gatewayError("RAG 请求路径无效");
            }
            String query = request.getQueryString();
            URI target = URI.create(ragBaseUrl + path + (query == null ? "" : "?" + query));

            HttpRequest.Builder builder = HttpRequest.newBuilder(target);
            request.getHeaderNames().asIterator().forEachRemaining(name -> {
                if (REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    request.getHeaders(name).asIterator().forEachRemaining(value -> builder.header(name, value));
                }
            });
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            if (accept != null && !accept.toLowerCase(Locale.ROOT).contains("text/event-stream")) {
                builder.timeout(Duration.ofMinutes(5));
            }

            String method = request.getMethod();
            ProxyResponse upstream = send(request, builder, method);

            HttpHeaders headers = new HttpHeaders();
            upstream.headers().map().forEach((name, values) -> {
                if (RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    values.forEach(value -> headers.add(name, value));
                }
            });
            headers.set("X-Accel-Buffering", "no");

            int status = upstream.statusCode();
            if (status == 204 || status == 304 || method.equalsIgnoreCase("HEAD")) {
                upstream.body().close();
                return ResponseEntity.status(status).headers(headers).build();
            }
            StreamingResponseBody stream = output -> {
                try (InputStream input = upstream.body()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        output.flush();
                    }
                }
            };
            return ResponseEntity.status(status).headers(headers).body(stream);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return gatewayError("RAG 服务请求被中断");
        } catch (IOException | IllegalArgumentException exception) {
            return gatewayError("RAG 服务暂不可用");
        }
    }

    private ProxyResponse send(HttpServletRequest request, HttpRequest.Builder builder, String method)
        throws IOException, InterruptedException {
        if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD")) {
            HttpResponse<InputStream> response = client.send(
                builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofInputStream());
            return new ProxyResponse(response.statusCode(), response.headers(), response.body());
        }
        Path bodyFile = Files.createTempFile("geo-rag-proxy-", ".body");
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
            HttpResponse<InputStream> response = client.send(
                builder.method(method, fileBody).build(),
                HttpResponse.BodyHandlers.ofInputStream());
            InputStream responseBody = new FilterInputStream(response.body()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        Files.deleteIfExists(bodyFile);
                    }
                }
            };
            return new ProxyResponse(response.statusCode(), response.headers(), responseBody);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            Files.deleteIfExists(bodyFile);
            throw exception;
        }
    }

    private record ProxyResponse(int statusCode, java.net.http.HttpHeaders headers, InputStream body) {}

    private static ResponseEntity<StreamingResponseBody> gatewayError(String message) {
        byte[] body = ("{\"detail\":\"" + message + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StreamingResponseBody stream = output -> output.write(body);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .contentType(MediaType.APPLICATION_JSON)
            .body(stream);
    }
}

