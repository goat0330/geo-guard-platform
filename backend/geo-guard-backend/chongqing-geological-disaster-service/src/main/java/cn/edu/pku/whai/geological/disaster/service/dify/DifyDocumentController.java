package cn.edu.pku.whai.geological.disaster.service.dify;

import cn.edu.pku.whai.geological.disaster.service.dify.props.DifyAgentProps;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

@RestController
@RequiredArgsConstructor
public class DifyDocumentController {
    private final DifyAgentProps difyAgentProps;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @GetMapping("/dify/datasets/{datasetId}/documents/{documentId}/upload-file")
    public ResponseEntity<String> getUploadFile(
        @PathVariable String datasetId,
        @PathVariable String documentId
    ) {
        String baseUrl = difyAgentProps.getUrl();
        String apiKey = difyAgentProps.getKnowledgeBaseApiKey();
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Dify dataset API is not configured");
        }

        String apiBase = baseUrl.trim().replaceAll("/+$", "");
        if (!apiBase.endsWith("/v1")) {
            apiBase += "/v1";
        }
        try {
            URI uri = URI.create(apiBase + "/datasets/"
                + UriUtils.encodePathSegment(datasetId, StandardCharsets.UTF_8)
                + "/documents/"
                + UriUtils.encodePathSegment(documentId, StandardCharsets.UTF_8)
                + "/upload-file");
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return error(HttpStatus.BAD_GATEWAY, "Dify URL must use HTTP or HTTPS");
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.body());
        } catch (IOException exception) {
            return error(HttpStatus.BAD_GATEWAY, "Dify file request failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return error(HttpStatus.BAD_GATEWAY, "Dify file request interrupted");
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_GATEWAY, "Dify URL is invalid");
        }
    }

    private ResponseEntity<String> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"" + message + "\"}");
    }
}
