/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.handle.client;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationRouteResultVo;
import cn.edu.pku.whai.geological.disaster.service.handle.props.HandleProcessProps;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 处置管理handle API客户端
 * 封装撤离方案生成、重新生成等外部API调用
 *
 * @author whai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandleProcessApiClient {

    private static final int API_SUCCESS_CODE = 200;
    private static final String API_RESPONSE_CODE = "code";
    private static final String API_RESPONSE_MESSAGE = "message";
    private static final String API_RESPONSE_DATA = "data";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final HandleProcessProps handleProcessProps;

    /**
     * 调用疏散路线接口
     *
     * @param handleId     处置主键
     * @param slopeUnitId  斜坡单元ID
     * @param hazardPoints 风险点坐标
     * @return 三方接口data节点
     */
    public EvacuationRouteResultVo evacuationRoute(Long handleId, String slopeUnitId, Map<String, Object> hazardPoints, String wkt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("handle_id", handleId);
        requestBody.put("slope_unit_id", slopeUnitId);
        requestBody.put("hazard_points", hazardPoints == null ? Map.of() : hazardPoints);
        requestBody.put("wkt", wkt);
        JsonNode dataNode = doPostDataNode(handleProcessProps.getEvacuationRouteUrl(), requestBody, handleId, "疏散路线生成");
        EvacuationRouteResultVo vo = JacksonUtil.parseObject(dataNode.toString(), EvacuationRouteResultVo.class);
        if (vo == null) {
            throw new ServiceException("疏散路线数据解析失败");
        }
        vo.normalizeLegacyEstimatedTimeMinutes();
        return vo;
    }

    private JsonNode doPostDataNode(String url, Object requestBody, Long handleId, String apiName) {
        if (url == null || url.isBlank()) {
            throw new ServiceException(apiName + "接口地址未配置");
        }
        try {
            JsonNode root = Req.post(url)
                               .json(requestBody)
                               .timeout(REQUEST_TIMEOUT)
                               .ok()
                               .node();

            if (root == null) {
                throw new ServiceException(apiName + "API返回空响应");
            }

            JsonNode codeNode = root.get(API_RESPONSE_CODE);
            int apiCode = codeNode != null && !codeNode.isNull() ? codeNode.asInt() : -1;
            if (apiCode != API_SUCCESS_CODE) {
                JsonNode messageNode = root.get(API_RESPONSE_MESSAGE);
                String apiMessage = messageNode != null && !messageNode.isNull() ? messageNode.asText() : "未知错误";
                throw new ServiceException(apiName + "API业务失败: " + apiMessage);
            }

            JsonNode dataNode = root.get(API_RESPONSE_DATA);
            if (dataNode == null || dataNode.isNull()) {
                throw new ServiceException("外部API返回data为空");
            }
            return dataNode;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}API调用异常, handleId={}", apiName, handleId, e);
            throw new ServiceException(apiName + "API调用异常: " + e.getMessage());
        }
    }
}
