/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSchemeItemVo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 撤离方案解析工具。
 * 将 Markdown 文本解析为结构化方案，保证详情落库、历史落库、外部接口转换使用同一套规则。
 */
public final class EvacuationSchemeParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^##\\s+(.+?)\\s*$");

    private EvacuationSchemeParser() {
    }

    /**
     * 将 Markdown 方案解析为结构化列表。
     *
     * @param schemes Markdown 方案文本
     * @return 结构化方案列表
     */
    public static List<EvacuationSchemeItemVo> parseMarkdown(String schemes) {
        List<EvacuationSchemeItemVo> result = new ArrayList<>();
        if (StringUtils.isBlank(schemes)) {
            return result;
        }
        String normalized = schemes.replace("\r\n", "\n").replace('\r', '\n');
        String currentPlanType = null;
        StringBuilder currentMeasures = new StringBuilder();
        for (String rawLine : normalized.split("\n", -1)) {
            String line = rawLine == null ? "" : rawLine.stripTrailing();
            Matcher matcher = TITLE_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                appendCurrentItem(result, currentPlanType, currentMeasures);
                currentPlanType = matcher.group(1).trim();
                currentMeasures.setLength(0);
                continue;
            }
            if (currentPlanType == null) {
                continue;
            }
            if (currentMeasures.isEmpty() && line.isBlank()) {
                continue;
            }
            if (!currentMeasures.isEmpty()) {
                currentMeasures.append('\n');
            }
            currentMeasures.append(line);
        }
        appendCurrentItem(result, currentPlanType, currentMeasures);
        return result;
    }

    /**
     * 将 Markdown 方案转换为落库 JSON。
     *
     * @param schemes Markdown 方案文本
     * @return 结构化 JSON
     */
    public static String toJson(String schemes) {
        List<EvacuationSchemeItemVo> items = parseMarkdown(schemes);
        return items.isEmpty() ? null : JacksonUtil.toJson(items);
    }

    /**
     * 将 Markdown 方案转换为外部 API 所需格式。
     *
     * @param schemes Markdown 方案文本
     * @return 外部 API 所需结构
     */
    public static List<Map<String, String>> toApiFormat(String schemes) {
        List<Map<String, String>> result = new ArrayList<>();
        for (EvacuationSchemeItemVo item : parseMarkdown(schemes)) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("plan_type", item.getPlanType());
            entry.put("specific_measures", item.getSpecificMeasures() == null ? "" : item.getSpecificMeasures());
            result.add(entry);
        }
        return result;
    }

    private static void appendCurrentItem(List<EvacuationSchemeItemVo> result, String planType, StringBuilder measuresBuilder) {
        if (StringUtils.isBlank(planType)) {
            return;
        }
        EvacuationSchemeItemVo item = new EvacuationSchemeItemVo();
        item.setPlanType(planType.trim());
        item.setSpecificMeasures(measuresBuilder == null ? "" : measuresBuilder.toString().trim());
        result.add(item);
    }
}
