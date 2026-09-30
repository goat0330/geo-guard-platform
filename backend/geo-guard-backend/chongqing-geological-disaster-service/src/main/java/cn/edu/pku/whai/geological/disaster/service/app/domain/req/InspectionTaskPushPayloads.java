/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the APP /v1/tasks/push payload with the exact field set documented by taskType.
 */
public final class InspectionTaskPushPayloads {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private InspectionTaskPushPayloads() {
    }

    public static String toJson(List<InspectionTaskReq> tasks) {
        try {
            return OBJECT_MAPPER.writeValueAsString(toPayload(tasks));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务推送请求序列化失败", e);
        }
    }

    public static List<Map<String, Object>> toPayload(List<InspectionTaskReq> tasks) {
        if (tasks == null) {
            return List.of();
        }
        return tasks.stream()
                    .map(InspectionTaskPushPayloads::toPayload)
                    .toList();
    }

    public static Map<String, Object> toPayload(InspectionTaskReq task) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("dispatchTime", task.getDispatchTime());
        payload.put("dynamicRiskLevel", task.getDynamicRiskLevel());
        payload.put("inspectionSuggestion", task.getInspectionSuggestion());
        payload.put("inspectorId", task.getInspectorId());
        payload.put("inspectorName", task.getInspectorName());
        payload.put("inspectorPhone", task.getInspectorPhone());
        payload.put("locationCenter", task.getLocationCenter());
        payload.put("locationDesc", task.getLocationDesc());
        payload.put("reportInfo", task.getReportInfo());
        payload.put("relatedTaskId", task.getRelatedTaskId());
        payload.put("sourceType", task.getSourceType());
        payload.put("taskSource", task.getTaskSource());
        payload.put("slopeUnitId", task.getSlopeUnitId());
        payload.put("slopeUnitCenter", task.getSlopeUnitCenter());
        payload.put("slopeUnitWkt", task.getSlopeUnitWkt());
        payload.put("status", task.getStatus());
        payload.put("submitRequire", task.getSubmitRequire());
        payload.put("taskId", task.getTaskId());
        if (DzTaskDistList.TASK_TYPE_EMERGENCY.equals(task.getTaskType())) {
            payload.put("taskSubType", task.getTaskSubType());
        }
        payload.put("taskType", task.getTaskType());
        if (DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION.equals(task.getTaskType())) {
            payload.put("handleId", task.getHandleId());
        }
        return payload;
    }
}
