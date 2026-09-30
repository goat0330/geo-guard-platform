package cn.edu.pku.whai.geological.disaster.service.utils;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 报告等级相关工具。
 * 统一负责从 ai_report 中提取关键指标，并复用同一套事件等级计算规则。
 *
 * @author kongweiguang
 */
public final class AiReportEventLevelSupport {

    private static final BigDecimal EPSILON = new BigDecimal("0.0001");

    private static final Pattern DEAD_PEOPLE_FIELD_PATTERN = Pattern.compile("(?i)(?:\"?deadPeople\"?|死亡人数|死亡人口|死亡失踪|死亡)\\s*[:：=]?\\s*([0-9]+)");
    private static final Pattern THREAT_PEOPLE_FIELD_PATTERN = Pattern.compile("(?i)(?:\"?threatPeople\"?|威胁人口|威胁人数|受威胁人数|威胁人员)\\s*[:：=]?\\s*([0-9]+)");
    private static final Pattern THREAT_PEOPLE_CONTEXT_PATTERN = Pattern.compile("威胁对象(?:涉及|为)?\\s*[0-9]+\\s*户\\s*([0-9]+)\\s*人");
    private static final Pattern DIRECT_LOSS_FIELD_PATTERN = Pattern.compile("(?i)(?:\"?directLoss\"?|直接经济损失|直接损失)\\s*[:：=]?\\s*([><=＜＞≤≥]*?)\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern THREAT_ASSET_FIELD_PATTERN = Pattern.compile("(?i)(?:\"?threatAsset\"?|威胁资产|受威胁资产)\\s*[:：=]?\\s*([><=＜＞≤≥]*?)\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern EVENT_LEVEL_FIELD_PATTERN = Pattern.compile("([ⅠⅡⅢⅣIV1-4])级防御响应");

    private AiReportEventLevelSupport() {
    }

    /**
     * 解析 AI 报告中的等级判定指标。
     *
     * @param aiReport AI 报告文本
     * @return 解析结果
     */
    public static AiReportMetrics parseMetrics(String aiReport) {
        if (StringUtils.isBlank(aiReport)) {
            throw new ServiceException("参数 aiReport 不能为空");
        }
        String normalized = normalize(aiReport);
        return new AiReportMetrics(
            parseEventLevel(normalized),
            parseDeadPeople(normalized),
            parseThreatPeople(normalized),
            parseDecimal(normalized, DIRECT_LOSS_FIELD_PATTERN),
            parseDecimal(normalized, THREAT_ASSET_FIELD_PATTERN)
        );
    }

    /**
     * 根据指标计算事件等级。
     *
     * @param deadPeople   死亡人数
     * @param threatPeople 威胁人数
     * @param directLoss   直接经济损失
     * @param threatAsset  威胁资产
     * @return 事件等级，统一采用数值越大表示灾情规模越大
     */
    public static Integer calculateLevel(Integer deadPeople, Integer threatPeople, BigDecimal directLoss, BigDecimal threatAsset) {
        if (deadPeople != null && deadPeople >= 30 || directLoss != null && directLoss.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return 4;
        } else if (deadPeople != null && deadPeople >= 10 || directLoss != null && directLoss.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return 3;
        } else if (deadPeople != null && deadPeople >= 3 || directLoss != null && directLoss.compareTo(BigDecimal.valueOf(100)) >= 0
            || threatPeople != null && threatPeople >= 1000 || threatAsset != null && threatAsset.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            return 2;
        } else {
            return 1;
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n")
                   .replace('\r', '\n')
                   .replace("**", "")
                   .replace("（", "(")
                   .replace("）", ")")
                   .replace("，", ",")
                   .replace("。", ".")
                   .replace("：", ":");
    }

    private static Integer parseDeadPeople(String text) {
        if (StringUtils.containsAny(text, "无人员伤亡", "未造成人员伤亡", "未出现人员伤亡", "无死亡", "死亡0人", "死亡 0 人")) {
            return 0;
        }
        Matcher matcher = DEAD_PEOPLE_FIELD_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static Integer parseThreatPeople(String text) {
        Matcher matcher = THREAT_PEOPLE_FIELD_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        Matcher contextMatcher = THREAT_PEOPLE_CONTEXT_PATTERN.matcher(text);
        if (contextMatcher.find()) {
            return Integer.parseInt(contextMatcher.group(1));
        }
        return null;
    }

    private static Integer parseEventLevel(String text) {
        Matcher matcher = EVENT_LEVEL_FIELD_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return mapDefenseLevelToEventLevel(matcher.group(1));
    }

    private static Integer mapDefenseLevelToEventLevel(String defenseLevelText) {
        if (StringUtils.isBlank(defenseLevelText)) {
            return null;
        }
        return switch (defenseLevelText.trim().toUpperCase()) {
            case "Ⅰ", "I", "1" -> 4;
            case "Ⅱ", "II", "2" -> 3;
            case "Ⅲ", "III", "3" -> 2;
            case "Ⅳ", "IV", "4" -> 1;
            default -> null;
        };
    }

    private static BigDecimal parseDecimal(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        BigDecimal value = new BigDecimal(matcher.group(2));
        String operator = matcher.group(1);
        if (StringUtils.isBlank(operator)) {
            return value;
        }
        String normalizedOperator = operator.replace(" ", "");
        if (">".equals(normalizedOperator) || "＞".equals(normalizedOperator)) {
            return value.add(EPSILON);
        }
        return value;
    }

    /**
     * AI 报告中可用于事件等级判定的指标。
     *
     * @param eventLevel   灾情等级（1：小型 2：中型 3：大型 4：特大型）
     * @param deadPeople   死亡人数
     * @param threatPeople 威胁人数
     * @param directLoss   直接经济损失
     * @param threatAsset  威胁资产
     */
    public record AiReportMetrics(Integer eventLevel, Integer deadPeople, Integer threatPeople, BigDecimal directLoss, BigDecimal threatAsset) {

        /**
         * 是否解析出全部指标。
         *
         * @return 是否存在有效指标
         */
        public boolean hasNoMetric() {
            return eventLevel == null && deadPeople == null && threatPeople == null && directLoss == null && threatAsset == null;
        }
    }
}
