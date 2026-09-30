/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consultation;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.consts.DefRespPlanTemplateContent;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.task.DefRespScheduledTask;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 会商确认乡镇结果处理工具（planContentJson 数组元素结构）。
 */
public final class DefRespConsultationConfirmItems {

    private DefRespConsultationConfirmItems() {
    }

    @Data
    public static class Item {
        private List<String> streets;
        private Integer newLevel;
        /**
         * 预警等级（气象预警原始等级，非防御响应映射等级）。
         */
        private Integer geoAdviceLevel;
        /**
         * 关联气象预警 id（前端以字符串传递）。
         */
        private String alarmId;

        public Long resolveAlarmId() {
            if (StringUtils.isBlank(alarmId)) {
                return null;
            }
            try {
                return Long.parseLong(alarmId.trim());
            } catch (NumberFormatException e) {
                throw new ServiceException("alarmId格式异常: " + alarmId);
            }
        }
    }

    @Data
    public static class NormalizedItem {
        /**
         * 归一化后的单个乡镇名称。
         */
        private String street;

        /**
         * 归一化后的响应等级。
         */
        private Integer finalConfirm;
    }

    public static void validate(List<NormalizedItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("会商确认数据不能为空");
        }
        for (NormalizedItem item : items) {
            if (item == null || StringUtils.isBlank(item.getStreet())) {
                throw new ServiceException("乡镇不能为空");
            }
            String street = item.getStreet().trim();
            if (!DefRespScheduledTask.isEnshiStreet(street)) {
                throw new ServiceException("乡镇不存在: " + street);
            }
            if (item.getFinalConfirm() == null) {
                throw new ServiceException("最终确认等级不能为空");
            }
        }
    }

    public static List<NormalizedItem> normalizeIncomingItems(List<Item> items) {
        return normalizeIncomingItems(items, null);
    }

    public static List<NormalizedItem> normalizeIncomingItems(List<Item> items,
                                                              BiFunction<Long, Integer, Integer> geoAdviceLevelMapper) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("会商确认数据不能为空");
        }
        List<NormalizedItem> normalized = new ArrayList<>();
        for (Item item : items) {
            if (item == null) {
                throw new ServiceException("会商确认数据不能为空");
            }
            List<String> streetList = resolveStreetList(item);
            Integer level = item.newLevel;
            if (level == null) {
                continue;
            }
            Integer finalConfirm = normalizeLegacyFinalConfirm(level);
            for (String street : streetList) {
                NormalizedItem normalizedItem = new NormalizedItem();
                normalizedItem.setStreet(street);
                normalizedItem.setFinalConfirm(finalConfirm);
                normalized.add(normalizedItem);
            }
        }
        if (normalized.isEmpty()) {
            return normalized;
        }
        validate(normalized);
        return normalized;
    }

    public static String buildReportContentFromPlanContentJson(
        String planContentJson,
        String city,
        String triggerCondition,
        String responsibilityUnit,
        Date planCreateDate
    ) {
        return buildReportContentFromPlanContentJson(
            planContentJson, city, triggerCondition, responsibilityUnit, planCreateDate, null);
    }

    public static String buildReportContentFromPlanContentJson(
        String planContentJson,
        String city,
        String triggerCondition,
        String responsibilityUnit,
        Date planCreateDate,
        BiFunction<Long, Integer, Integer> geoAdviceLevelMapper
    ) {
        if (StringUtils.isBlank(planContentJson)) {
            return null;
        }
        List<Item> items = JacksonUtil.toList(planContentJson, Item.class);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("会商确认数据格式异常");
        }
        List<NormalizedItem> normalizedItems = normalizeIncomingItems(items, geoAdviceLevelMapper);
        if (normalizedItems.isEmpty()) {
            return null;
        }
        return buildReportContentFromTemplate(
            city,
            triggerCondition,
            responsibilityUnit,
            planCreateDate,
            normalizedItems
        );
    }

    public static String buildReportContentFromTemplate(
        String city,
        String triggerCondition,
        String responsibilityUnit,
        Date planCreateDate,
        List<NormalizedItem> items
    ) {
        validate(items);
        Date safePlanCreateDate = planCreateDate == null ? new Date() : planCreateDate;
        Map<String, String> params = new HashMap<>();
        String safeCity = StringUtils.defaultIfBlank(city, "恩施市");
        String streetsText = String.join(",", collectStreets(items));
        String areaName = safeCity + streetsText;
        params.put(DefRespPlanTemplateContent.KEY_DEFENSE_AREA, areaName);
        params.put(DefRespPlanTemplateContent.KEY_TRIGGER_CONDITION,
            StringUtils.defaultIfBlank(triggerCondition, "经过专家组已会商确认"));
        params.put(DefRespPlanTemplateContent.KEY_EXPLAIN_UNIT, StringUtils.defaultIfBlank(responsibilityUnit, "湖北省自然资源厅,湖北省气象局"));
        params.put(DefRespPlanTemplateContent.KEY_COMPILE_UNIT, StringUtils.defaultIfBlank(responsibilityUnit, "湖北省自然资源厅,湖北省气象局"));
        params.put(DefRespPlanTemplateContent.KEY_COMPILE_DATE, new SimpleDateFormat("yyyy年MM月dd日").format(safePlanCreateDate));
        params.put(DefRespPlanTemplateContent.KEY_START_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        params.put(DefRespPlanTemplateContent.KEY_SPECIFIC_AREA_NAME, areaName);
        params.put(DefRespPlanTemplateContent.KEY_REVISION_BASIS_AREA, areaName);
        Integer highestDisplayLevel = resolveOverallDisplayLevel(items);
        params.put(DefRespPlanTemplateContent.KEY_DEFAULT_LEVEL_TEXT, "处于" + toRomanLevelText(highestDisplayLevel));
        params.put(DefRespPlanTemplateContent.KEY_RESPONSE_LEVEL_TEXT, buildGroupedResponseLevelText(items));
        putDefaultMonitorFrequencyParams(params);
        Set<Integer> activeLevels = items.stream()
                                         .map(NormalizedItem::getFinalConfirm)
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toCollection(LinkedHashSet::new));
        return DefRespPlanTemplateContent.getFilledRegPlanContent(params, activeLevels);
    }

    private static Set<String> collectStreets(List<NormalizedItem> items) {
        Set<String> streets = new LinkedHashSet<>();
        for (NormalizedItem item : items) {
            streets.add(item.getStreet().trim());
        }
        return streets;
    }

    private static Integer resolveOverallDisplayLevel(List<NormalizedItem> items) {
        return items.stream()
                    .map(NormalizedItem::getFinalConfirm)
                    .filter(Objects::nonNull)
                    .min(Integer::compareTo)
                    .orElse(1);
    }

    private static String buildGroupedResponseLevelText(List<NormalizedItem> items) {
        Map<Integer, List<String>> streetsByLevel = new LinkedHashMap<>();
        for (NormalizedItem item : items) {
            streetsByLevel.computeIfAbsent(item.getFinalConfirm(), ignored -> new ArrayList<>())
                          .add(item.getStreet().trim());
        }
        List<Integer> levels = new ArrayList<>(streetsByLevel.keySet());
        levels.sort(Integer::compareTo);
        List<String> parts = new ArrayList<>();
        for (Integer level : levels) {
            parts.add(String.join("、", streetsByLevel.get(level)) + "处于" + toRomanLevelText(level));
        }
        return String.join("，", parts);
    }

    private static Integer normalizeLegacyFinalConfirm(Integer finalConfirm) {
        return finalConfirm;
    }

    private static String toRomanLevelText(Integer displayLevel) {
        return StringUtils.defaultIfBlank(LevelCodeUtil.resolveDefenseResponseRomanLevel(displayLevel), displayLevel + "级");
    }

    private static List<String> resolveStreetList(Item item) {
        List<String> streets = item.getStreets();
        if (streets == null || streets.isEmpty()) {
            throw new ServiceException("乡镇不能为空");
        }
        List<String> normalizedStreets = streets.stream()
                                                .filter(StringUtils::isNotBlank)
                                                .map(String::trim)
                                                .distinct()
                                                .collect(Collectors.toList());
        if (normalizedStreets.isEmpty()) {
            throw new ServiceException("乡镇不能为空");
        }
        for (String street : normalizedStreets) {
            if (!DefRespScheduledTask.isEnshiStreet(street)) {
                throw new ServiceException("乡镇不存在: " + street);
            }
        }
        return normalizedStreets;
    }

    private static void putDefaultMonitorFrequencyParams(Map<String, String> params) {
        params.put(DefRespPlanTemplateContent.KEY_RED_MONITOR_FREQUENCY, "每4小时1次");
        params.put(DefRespPlanTemplateContent.KEY_RED_PATROL_TIMES, "6");
        params.put(DefRespPlanTemplateContent.KEY_ORANGE_MONITOR_FREQUENCY, "每8小时1次");
        params.put(DefRespPlanTemplateContent.KEY_ORANGE_PATROL_TIMES, "3");
        params.put(DefRespPlanTemplateContent.KEY_YELLOW_MONITOR_FREQUENCY, "每12小时1次");
        params.put(DefRespPlanTemplateContent.KEY_YELLOW_PATROL_TIMES, "2");
        params.put(DefRespPlanTemplateContent.KEY_BLUE_MONITOR_FREQUENCY, "每24小时1次");
        params.put(DefRespPlanTemplateContent.KEY_BLUE_PATROL_TIMES, "1");
    }
}
