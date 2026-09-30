/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.sms.EvacuationSmsTemplate;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通过替换模板占位符生成撤离短信正文
 *
 * @author whai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvacuationSmsContentServiceImpl implements IEvacuationSmsContentService {

    private static final String DEFAULT_EMPTY = "暂无";

    /**
     * 事件类型：1=滑坡 2=崩塌 3=地面塌陷 4=泥石流 5=危岩 100=其他
     */
    private static final Map<Integer, String> EVENT_TYPE_MAP = new LinkedHashMap<>();
    /**
     * 响应级别/防御响应：0=未启动 1~4=1级~4级
     */
    private static final Map<Integer, String> RESP_LEVEL_MAP = new LinkedHashMap<>();

    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;

    static {
        EVENT_TYPE_MAP.put(1, "滑坡");
        EVENT_TYPE_MAP.put(2, "崩塌");
        EVENT_TYPE_MAP.put(3, "地面塌陷");
        EVENT_TYPE_MAP.put(4, "泥石流");
        EVENT_TYPE_MAP.put(5, "危岩");
        EVENT_TYPE_MAP.put(100, "其他");
        RESP_LEVEL_MAP.put(0, "未启动");
        RESP_LEVEL_MAP.put(1, "1");
        RESP_LEVEL_MAP.put(2, "2");
        RESP_LEVEL_MAP.put(3, "3");
        RESP_LEVEL_MAP.put(4, "4");
    }

    @Override
    public String generate(DzTaskHandle handle, EvacuationPlan plan) {
        Map<String, String> placeholders = buildPlaceholders(handle, plan);
        String body = EvacuationSmsTemplate.MASSES;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String key = "{{" + e.getKey() + "}}";
            body = body.replace(key, e.getValue() != null ? e.getValue() : DEFAULT_EMPTY);
        }
        return body;
    }

    /**
     * 组装占位符 key -> 替换值（key 不含花括号）
     */
    private Map<String, String> buildPlaceholders(DzTaskHandle handle, EvacuationPlan plan) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("command_region", firstNonBlank(handle.getCounty(), handle.getStreet(), "恩施市"));
        m.put("event_name", buildEventName(handle));
        m.put("management_unit", buildManagementUnit(handle));
        m.put("route_guide", plan.getEvacuationRoute());
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(handle.getSlopeUnitId().trim());
        m.put("special_manager_phone", relationVo.getSpecialManagerPhone());
        m.put("resettlement_address", plan.getResettlementLocation());
        return m;
    }

    private static String nullToDefault(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : DEFAULT_EMPTY;
    }

    /**
     * 事件类型编码转中文
     */
    private static String resolveEventType(Integer eventType) {
        if (eventType == null) {
            return DEFAULT_EMPTY;
        }
        return EVENT_TYPE_MAP.getOrDefault(eventType, String.valueOf(eventType));
    }
    
    private String buildEventName(DzTaskHandle handle) {
        String location = firstNonBlank(handle.getVillage(), handle.getStreet(), handle.getCounty(), nullToDefault(handle.getSlopeUnitId()) + "号斜坡单元");
        String eventType = resolveEventType(handle.getEventType());
        return location + eventType + "灾险情";
    }

    private String buildManagementUnit(DzTaskHandle handle) {
        String unit = joinNonBlank(handle.getCounty(), handle.getStreet(), handle.getVillage());
        return unit.isBlank() ? nullToDefault(handle.getSlopeUnitId()) + "号斜坡单元所在区域" : unit;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return DEFAULT_EMPTY;
    }

    private static String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }
}
