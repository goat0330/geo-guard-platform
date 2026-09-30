package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeoAiProxyControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer upstream;
    private GeoAiProxyController controller;

    @BeforeEach
    void setUp() throws Exception {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (exchange.getRequestURI().getPath().endsWith("/run/stream")) {
                exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("event: trace\ndata: {\"status\":\"running\"}\n\n".getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                exchange.getResponseBody().write("event: done\ndata: {}\n\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        upstream.start();
        String baseUrl = "http://127.0.0.1:" + upstream.getAddress().getPort();
        GeoAiProxyService proxy = new GeoAiProxyService(
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                baseUrl,
                baseUrl);
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        controller = new GeoAiProxyController(proxy, hazardPointService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        upstream.stop(0);
    }

    @Test
    void ragRouteForwardsJsonToRagService() throws Exception {
        JsonNode body = objectMapper.readTree("{\"query\":\"rainfall\"}");

        var response = controller.retrieve(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestedPath.get()).isEqualTo("/api/v1/retrieve");
        assertThat(objectMapper.readTree(requestBody.get()).path("query").asText()).isEqualTo("rainfall");
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void graphStreamPreservesSseEvents() throws Exception {
        JsonNode body = objectMapper.readTree("{\"query\":\"hazard\",\"workflow\":\"hazard_review\"}");

        var response = controller.streamGraph(body);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamingResponseBody stream = response.getBody();
        assertThat(stream).isNotNull();
        stream.writeTo(output);

        String sse = output.toString(StandardCharsets.UTF_8);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).startsWith("text/event-stream");
        assertThat(requestedPath.get()).isEqualTo("/api/v1/run/stream");
        assertThat(sse).contains("event: trace", "\"status\":\"running\"", "event: done");
    }

    @Test
    void graphRunLoadsMinimalHazardPointContextWhenRequested() throws Exception {
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        HazardPointVo hazard = new HazardPointVo();
        hazard.setId("HZ-7");
        hazard.setName("Test slope");
        hazard.setRiskGrade("一般");
        when(hazardPointService.queryById("HZ-7")).thenReturn(hazard);
        GeoAiProxyService proxy = new GeoAiProxyService(
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                "http://127.0.0.1:" + upstream.getAddress().getPort(),
                "http://127.0.0.1:" + upstream.getAddress().getPort());
        controller = new GeoAiProxyController(proxy, hazardPointService, objectMapper);

        controller.runGraph(objectMapper.readTree("{\"query\":\"review\",\"hazard_input\":{\"hazard_id\":\"HZ-7\"}}"));

        JsonNode forwarded = objectMapper.readTree(requestBody.get());
        assertThat(requestedPath.get()).isEqualTo("/api/v1/run");
        assertThat(forwarded.path("context").path("business_source").asText()).isEqualTo("spring-business-db");
        assertThat(forwarded.path("context").path("current_record").path("name").asText()).isEqualTo("Test slope");
        assertThat(forwarded.path("context").path("candidate_hazards").get(0).path("id").asText()).isEqualTo("HZ-7");
    }
}
