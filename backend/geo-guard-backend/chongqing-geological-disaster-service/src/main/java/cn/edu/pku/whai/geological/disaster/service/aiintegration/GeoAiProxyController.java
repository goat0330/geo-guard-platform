package cn.edu.pku.whai.geological.disaster.service.aiintegration;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/dizai/ai")
public class GeoAiProxyController {
    private final GeoAiProxyService proxyService;
    private final IHazardPointService hazardPointService;
    private final ObjectMapper objectMapper;

    public GeoAiProxyController(GeoAiProxyService proxyService, IHazardPointService hazardPointService,
                                ObjectMapper objectMapper) {
        this.proxyService = proxyService;
        this.hazardPointService = hazardPointService;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(value = {"/rag", "/rag/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
                    RequestMethod.DELETE, RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> proxyRagApi(HttpServletRequest request) {
        try {
            GeoAiProxyService.ProxyApiResponse upstream = proxyService.proxyRagApi(request);
            int status = upstream.status();
            if (status == 204 || status == 304 || request.getMethod().equalsIgnoreCase("HEAD")) {
                upstream.close();
                return ResponseEntity.status(status).headers(upstream.headers()).build();
            }
            StreamingResponseBody stream = output -> copyAndFlush(upstream.body(), output);
            return ResponseEntity.status(status).headers(upstream.headers()).body(stream);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException exception) {
            throw unavailable(exception);
        }
    }
    @PostMapping("/rag/retrieve")
    public ResponseEntity<byte[]> retrieve(@RequestBody JsonNode body) {
        return postRag("/api/v1/retrieve", body);
    }

    @PostMapping("/rag/debug/retrieve")
    public ResponseEntity<byte[]> debugRetrieve(@RequestBody JsonNode body) {
        return postRag("/api/v1/debug/retrieve", body);
    }

    @PostMapping("/graph/run")
    public ResponseEntity<byte[]> runGraph(@RequestBody JsonNode body) {
        return postGraph("/api/v1/run", withBusinessContext(body));
    }

    @PostMapping(value = "/graph/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamGraph(@RequestBody JsonNode body) {
        try {
            GeoAiProxyService.ProxyStream upstream = proxyService.streamGraph("/api/v1/run/stream", withBusinessContext(body));
            StreamingResponseBody stream = output -> copyAndFlush(upstream.body(), output);
            return ResponseEntity.status(upstream.status())
                    .contentType(upstream.contentType())
                    .header("Cache-Control", "no-cache")
                    .header("X-Accel-Buffering", "no")
                    .body(stream);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException exception) {
            throw unavailable(exception);
        }
    }

    private ResponseEntity<byte[]> postRag(String path, JsonNode body) {
        try {
            return proxyService.postRag(path, body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException exception) {
            throw unavailable(exception);
        }
    }

    private ResponseEntity<byte[]> postGraph(String path, JsonNode body) {
        try {
            return proxyService.postGraph(path, body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException exception) {
            throw unavailable(exception);
        }
    }

    private JsonNode withBusinessContext(JsonNode request) {
        ObjectNode body = request != null && request.isObject() ? ((ObjectNode) request.deepCopy()) : objectMapper.createObjectNode();
        ObjectNode hazardInput = objectNode(body, "hazard_input");
        ObjectNode context = objectNode(body, "context");
        String hazardId = hazardInput.path("hazard_id").asText("").trim();
        if (hazardId.isEmpty() || context.hasNonNull("current_record")) {
            return body;
        }

        HazardPointVo hazard = hazardPointService.queryById(hazardId);
        if (hazard == null) {
            context.put("business_source", "spring-business-db");
            context.put("business_record_lookup", "not_found");
            return body;
        }
        JsonNode hazardJson = objectMapper.valueToTree(hazard);
        ObjectNode current = objectMapper.createObjectNode();
        for (String field : new String[]{"id", "name", "uniqueDisasterId", "typeCode", "province", "city",
                "county", "street", "village", "gridCode", "longitude", "latitude", "scaleGrade",
                "managementLevel", "threatenedPopulation", "threatenedPropertyValue", "riskGrade",
                "disasterHistoryTime", "geologicalEnvironment", "deformationFeatures", "stabilityAnalysis",
                "stabilityStatus", "stabilityTrend"}) {
            if (hazardJson.has(field) && !hazardJson.get(field).isNull()) {
                current.set(field, hazardJson.get(field));
            }
        }
        context.set("current_record", current);
        context.set("candidate_hazards", objectMapper.createArrayNode().add(current));
        context.put("business_source", "spring-business-db");
        context.put("business_record_lookup", "matched");
        return body;
    }

    private ObjectNode objectNode(ObjectNode parent, String name) {
        JsonNode value = parent.get(name);
        if (value instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode result = objectMapper.createObjectNode();
        parent.set(name, result);
        return result;
    }

    private static void copyAndFlush(InputStream input, OutputStream output) throws IOException {
        try (input) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                output.flush();
            }
        }
    }

    private static ResponseStatusException unavailable(Exception cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geo AI service unavailable", cause);
    }
}
