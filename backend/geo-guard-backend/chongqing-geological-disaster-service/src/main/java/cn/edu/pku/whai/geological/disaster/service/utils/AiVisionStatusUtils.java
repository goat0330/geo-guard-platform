/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * AI 识图字段状态判定。
 */
public final class AiVisionStatusUtils {

    public enum Status {
        IN_PROGRESS,
        FAILED,
        COMPLETED
    }

    private static final String FALLBACK_APPLIED = "fallbackApplied";
    private static final String FALLBACK_REASON = "fallbackReason";
    private static final String FALLBACK_MESSAGE = "fallbackMessage";

    private AiVisionStatusUtils() {
    }

    public static Status resolve(DzReportDisaster report) {
        if (report == null) {
            return Status.FAILED;
        }
        boolean riskBlank = report.getAiRiskLevel() == null;
        boolean detailBlank = StrUtil.isBlank(report.getAiReportDetail());
        boolean propsBlank = isPropsBlank(report.getAiVisionProps());
        if (riskBlank && detailBlank && propsBlank) {
            return Status.IN_PROGRESS;
        }
        if (!riskBlank && !detailBlank && !propsBlank && !hasFallbackMarker(report.getAiVisionProps())) {
            return Status.COMPLETED;
        }
        return Status.FAILED;
    }

    public static boolean isInProgress(DzReportDisaster report) {
        return resolve(report) == Status.IN_PROGRESS;
    }

    public static boolean isFailed(DzReportDisaster report) {
        return resolve(report) == Status.FAILED;
    }

    public static boolean isCompleted(DzReportDisaster report) {
        return resolve(report) == Status.COMPLETED;
    }

    public static boolean isPropsBlank(Map<String, Object> props) {
        return props == null || props.isEmpty();
    }

    public static boolean hasFallbackMarker(Map<String, Object> props) {
        return props != null
            && (props.containsKey(FALLBACK_APPLIED)
            || props.containsKey(FALLBACK_REASON)
            || props.containsKey(FALLBACK_MESSAGE));
    }
}
