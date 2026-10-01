/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

import org.dromara.common.core.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * data_alarm.level_streets_json 的统一解析工具。
 */
public final class DataAlarmLevelStreetsUtil {

    private static final TypeReference<Map<String, List<String>>> LEVEL_STREETS_TYPE = new TypeReference<>() {
    };

    private DataAlarmLevelStreetsUtil() {
    }

    public static String toJson(Map<Integer, ? extends Collection<String>> levelStreetsMap) {
        if (levelStreetsMap == null || levelStreetsMap.isEmpty()) {
            return null;
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        levelStreetsMap.entrySet().stream()
                       .filter(entry -> entry.getKey() != null)
                       .sorted((left, right) -> Integer.compare(right.getKey(), left.getKey()))
                       .forEach(entry -> {
                           List<String> streets = normalizeStreetList(entry.getValue());
                           if (!streets.isEmpty()) {
                               normalized.put(String.valueOf(entry.getKey()), streets);
                           }
                       });
        return normalized.isEmpty() ? null : JacksonUtil.toJson(normalized);
    }

    public static Map<Integer, LinkedHashSet<String>> parse(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> parsed = JacksonUtil.parseObject(json, LEVEL_STREETS_TYPE);
        if (parsed == null || parsed.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, LinkedHashSet<String>> result = new LinkedHashMap<>();
        parsed.entrySet().stream()
              .map(entry -> Map.entry(parseLevel(entry.getKey()), entry.getValue()))
              .filter(entry -> entry.getKey() != null)
              .sorted((left, right) -> Integer.compare(right.getKey(), left.getKey()))
              .forEach(entry -> {
                  LinkedHashSet<String> streets = new LinkedHashSet<>(normalizeStreetList(entry.getValue()));
                  if (!streets.isEmpty()) {
                      result.put(entry.getKey(), streets);
                  }
              });
        return result;
    }

    public static List<String> flattenStreets(String json) {
        Set<String> streets = new LinkedHashSet<>();
        parse(json).values().forEach(streets::addAll);
        return new ArrayList<>(streets);
    }

    public static Integer resolveHighestLevel(String json) {
        return parse(json).keySet().stream().filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
    }

    public static Map<String, Integer> toStreetHighestLevelMap(String json) {
        Map<String, Integer> result = new LinkedHashMap<>();
        parse(json).forEach((level, streets) -> {
            if (level == null || streets == null) {
                return;
            }
            for (String street : streets) {
                result.merge(street, level, Math::max);
            }
        });
        return result;
    }

    private static Integer parseLevel(String levelText) {
        if (StringUtils.isBlank(levelText)) {
            return null;
        }
        try {
            return Integer.parseInt(levelText.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> normalizeStreetList(Collection<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String street : streets) {
            if (StringUtils.isBlank(street)) {
                continue;
            }
            normalized.add(street.trim());
        }
        return new ArrayList<>(normalized);
    }
}
