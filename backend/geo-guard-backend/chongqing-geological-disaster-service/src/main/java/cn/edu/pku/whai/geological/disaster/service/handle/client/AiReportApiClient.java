/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.handle.client;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.handle.props.HandleProcessProps;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 应急调查报告算法接口客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportApiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);
    private static final String DEFAULT_EXPERT_OPINION = "根据现有报告进行修改";

    private final HandleProcessProps handleProcessProps;
    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(CONNECT_TIMEOUT)
                                                    .build();

    public AiReportResponse report(AiReportRequest request) {
        return report(request, false, null);
    }

    public AiReportResponse reportStream(AiReportRequest request, Consumer<String> chunkConsumer) {
        return report(request, true, chunkConsumer);
    }

    private AiReportResponse report(AiReportRequest request, boolean stream, Consumer<String> chunkConsumer) {
        if (request == null || request.handleId() == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        String url = resolveReportUrl(request.hasExistingReport(), stream);
        Map<String, Object> body = buildRequestBody(request);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                                                 .timeout(REQUEST_TIMEOUT)
                                                 .header("Content-Type", "application/json")
                                                 .header("Accept", "text/event-stream, application/json, text/plain")
                                                 .POST(HttpRequest.BodyPublishers.ofString(JacksonUtil.toJson(body), StandardCharsets.UTF_8))
                                                 .build();
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            AiReportResponse result = consumeResponse(stream, response, chunkConsumer);
            if (result == null || StringUtils.isBlank(result.markdown())) {
                throw new ServiceException("AI未返回调查报告内容");
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("应急调查报告算法接口调用异常, handleId={}, hasExistingReport={}",
                request.handleId(), request.hasExistingReport(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ServiceException("应急调查报告算法接口调用异常: " + buildExceptionDetail(e));
        }
    }

    private String buildExceptionDetail(Exception e) {
        if (e instanceof HttpConnectTimeoutException) {
            return "连接算法服务超时，请确认算法服务地址可访问";
        }
        if (e instanceof HttpTimeoutException) {
            return "等待算法服务响应超时";
        }
        if (e instanceof ConnectException) {
            return "连接算法服务失败，请确认算法服务已启动且接口地址可访问";
        }
        String message = firstNonBlankMessage(e);
        if (StringUtils.isBlank(message)) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message;
    }

    private String firstNonBlankMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (StringUtils.isNotBlank(current.getMessage())) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return null;
    }

    private String resolveReportUrl(boolean hasExistingReport, boolean stream) {
        String url;
        if (stream) {
            url = hasExistingReport
                ? handleProcessProps.getReportReviseStreamUrl()
                : handleProcessProps.getReportStreamUrl();
        } else {
            url = hasExistingReport
                ? handleProcessProps.getReportReviseUrl()
                : handleProcessProps.getReportUrl();
        }
        if (StringUtils.isBlank(url)) {
            throw new ServiceException(buildMissingUrlMessage(hasExistingReport, stream));
        }
        return url;
    }

    private String buildMissingUrlMessage(boolean hasExistingReport, boolean stream) {
        if (hasExistingReport) {
            return stream ? "应急调查报告修订流式接口地址未配置" : "应急调查报告修订接口地址未配置";
        }
        return stream ? "应急调查报告生成流式接口地址未配置" : "应急调查报告生成接口地址未配置";
    }

    private Map<String, Object> buildRequestBody(AiReportRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_input", request.appInput() == null ? Map.of() : request.appInput());
        body.put("mcp_response", request.mcpResponse() == null ? Map.of() : request.mcpResponse());
        if (request.hasExistingReport()) {
            body.put("original_report_markdown", StringUtils.defaultString(request.originalReportMarkdown()));
            body.put("expert_opinion", StringUtils.defaultIfBlank(request.prompt(), DEFAULT_EXPERT_OPINION));
            body.put("options", buildReviseOptions());
            body.put("revision_options", buildRevisionOptions());
        }
        return body;
    }

    private Map<String, Object> buildReviseOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("use_llm", true);
        options.put("include_images", false);
        return options;
    }

    private Map<String, Object> buildRevisionOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("preserve_framework", true);
        options.put("return_change_summary", true);
        return options;
    }

    private AiReportResponse consumeResponse(boolean stream, HttpResponse<Stream<String>> response, Consumer<String> chunkConsumer) {
        StringBuilder parsedContent = new StringBuilder();
        String[] latestStreamMarkdown = new String[1];
        StringBuilder rawContent = new StringBuilder();
        String[] imagePlaceholdersJson = new String[1];
        try (Stream<String> lines = response.body()) {
            lines.forEach(line -> {
                rawContent.append(line).append('\n');
                AiReportResponse chunk = extractChunk(line);
                if (chunk != null && chunk.imagePlaceholdersJson() != null) {
                    imagePlaceholdersJson[0] = chunk.imagePlaceholdersJson();
                }
                if (chunk == null || StringUtils.isBlank(chunk.markdown())) {
                    return;
                }
                if (stream) {
                    latestStreamMarkdown[0] = chunk.markdown();
                } else {
                    parsedContent.append(chunk.markdown());
                }
                if (chunkConsumer != null) {
                    chunkConsumer.accept(chunk.markdown());
                }
            });
        }
        String raw = rawContent.toString();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException("应急调查报告算法接口调用失败: HTTP "
                + response.statusCode() + ", " + abbreviate(raw));
        }
        String parsed = stream ? latestStreamMarkdown[0] : parsedContent.toString();
        AiReportResponse rawResponse = null;
        if (StringUtils.isBlank(parsed) || imagePlaceholdersJson[0] == null) {
            rawResponse = extractReportResponse(raw);
        }
        if (StringUtils.isNotBlank(parsed)) {
            String placeholders = imagePlaceholdersJson[0] != null
                ? imagePlaceholdersJson[0]
                : rawResponse == null ? null : rawResponse.imagePlaceholdersJson();
            return new AiReportResponse(parsed.trim(), placeholders);
        }
        return rawResponse;
    }

    private AiReportResponse extractChunk(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        String payload = line.trim();
        if (payload.startsWith(":") || payload.startsWith("event:") || payload.startsWith("id:")) {
            return null;
        }
        boolean sseDataLine = payload.startsWith("data:");
        if (sseDataLine) {
            payload = payload.substring("data:".length()).trim();
        }
        if (StringUtils.isBlank(payload) || "[DONE]".equals(payload)) {
            return null;
        }
        JsonNode node = parseJsonIfStructured(payload);
        if (node != null) {
            return toAiReportResponse(node);
        }
        if (looksStructured(payload)) {
            return null;
        }
        return new AiReportResponse(sseDataLine ? payload : payload + System.lineSeparator(), null);
    }

    private AiReportResponse extractReportResponse(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        JsonNode node = parseJsonIfStructured(raw.trim());
        if (node != null) {
            return toAiReportResponse(node);
        }
        return new AiReportResponse(raw.trim(), null);
    }

    private JsonNode parseJsonIfStructured(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (!looksStructured(trimmed)) {
            return null;
        }
        return JacksonUtil.toJsonNode(trimmed);
    }

    private boolean looksStructured(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"");
    }

    private AiReportResponse toAiReportResponse(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new AiReportResponse(findReportText(node), findImagePlaceholdersJson(node));
    }

    private String findReportText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = findReportText(item);
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
            return null;
        }
        String[] textFields = {"markdown", "report", "answer", "content", "text", "chunk", "delta"};
        for (String field : textFields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.isTextual() ? value.asText() : findReportText(value);
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
        }
        JsonNode data = node.get("data");
        if (data != null && !data.isNull()) {
            String text = findReportText(data);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        }
        return null;
    }

    private String findImagePlaceholdersJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String json = findImagePlaceholdersJson(item);
                if (json != null) {
                    return json;
                }
            }
            return null;
        }
        JsonNode value = node.get("imagePlaceholders");
        if (value == null || value.isNull()) {
            value = node.get("image_placeholders");
        }
        if (value != null && !value.isNull()) {
            return value.isTextual() ? value.asText() : value.toString();
        }
        JsonNode data = node.get("data");
        if (data != null && !data.isNull()) {
            return findImagePlaceholdersJson(data);
        }
        return null;
    }

    private String abbreviate(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "...";
    }

    public record AiReportRequest(
        Long handleId,
        boolean hasExistingReport,
        Object appInput,
        Object mcpResponse,
        String prompt,
        String originalReportMarkdown
    ) {
    }

    public record AiReportResponse(
        String markdown,
        String imagePlaceholdersJson
    ) {
    }
}
