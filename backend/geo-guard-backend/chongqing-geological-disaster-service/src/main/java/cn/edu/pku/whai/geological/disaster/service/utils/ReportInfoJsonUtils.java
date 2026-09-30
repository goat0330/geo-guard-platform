/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * reportInfo 字段统一收敛为对外约定的最小字段集合。
 */
public final class ReportInfoJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ReportInfoJsonUtils() {
    }

    public static String toLimitedJson(DzReportDisaster report) {
        if (report == null) {
            return null;
        }
        String checkTime = report.getCheckTime() == null
            ? null
            : DateUtil.format(report.getCheckTime(), DatePattern.NORM_DATETIME_PATTERN);
        return toStrictJson(report.getTaskId(), checkTime, report.getDetailedAddress(),
            report.getCheckCenter(), report.getCheckCenterLocation(), report.getUserName(),
            report.getUserPhone(), report.getSceneTextRecord(), report.getPhotos());
    }

    public static String sanitize(String reportInfo) {
        if (StrUtil.isBlank(reportInfo)) {
            return reportInfo;
        }
        try {
            Map<String, Object> source = OBJECT_MAPPER.readValue(reportInfo, new TypeReference<>() {
            });
            return toStrictJson(source.get("taskId"), source.get("checkTime"), source.get("detailedAddress"),
                source.get("checkCenter"), source.get("checkCenterLocation"), source.get("userName"),
                source.get("userPhone"), source.get("sceneTextRecord"), source.get("photos"));
        } catch (Exception ex) {
            return reportInfo;
        }
    }

    private static String toStrictJson(Object taskId, Object checkTime, Object detailedAddress, Object checkCenter,
                                       Object checkCenterLocation, Object userName, Object userPhone,
                                       Object sceneTextRecord, Object photos) {
        Map<String, Object> limited = new LinkedHashMap<>();
        limited.put("taskId", normalizeTaskId(taskId));
        limited.put("checkTime", normalizeString(checkTime));
        limited.put("detailedAddress", normalizeString(detailedAddress));
        limited.put("checkCenter", normalizeString(checkCenter));
        limited.put("checkCenterLocation", normalizeString(checkCenterLocation));
        limited.put("userName", normalizeString(userName));
        limited.put("userPhone", normalizeString(userPhone));
        limited.put("sceneTextRecord", normalizeString(sceneTextRecord));
        limited.put("photos", normalizeString(photos));
        try {
            return OBJECT_MAPPER.writeValueAsString(limited);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("reportInfo序列化失败", e);
        }
    }

    private static Long normalizeTaskId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = normalizeString(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
