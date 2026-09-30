/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.defresp;

import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.AlarmSnapshot;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.Source;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 区域防御响应启动、结束及调整短信正文构建器。
 */
@Component
public final class DefRespLifecycleSmsContentBuilder {

    private static final DateTimeFormatter CHINESE_HOUR = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时");
    private static final String STATE_BUREAU = "恩施州自然资源和城乡建设局";
    private static final String LOCAL_BUREAU = "恩施市自然资源和规划局";
    private static final String PLAN_NAME = "《恩施州自然资源和规划局地质灾害防御响应工作方案（试行）》";
    private static final String START_REQUIREMENTS = "请各相关单位按照有关工作要求和职责及时做好地质灾害风险防范应对工作。"
        + "请技术单位加强趋势分析、风险预警和专家驻守，及时为防御地质灾害提供技术支持。";
    private static final String END_REQUIREMENTS = "请相关单位按照有关工作要求和职责做好调整。"
        + "请技术单位继续跟踪了解情况，及时提供专业技术支撑，做好复盘总结。";
    private static final String ADJUST_REQUIREMENTS = "请相关单位按照有关工作要求和职责做好调整。"
        + "请技术单位继续跟踪了解情况，指导地方做好地质灾害防范，及时提供专业技术支撑。";

    /**
     * 根据冻结事件构建短信正文；预警类事件缺少必要预警数据或事件缺少适用乡镇时返回空。
     */
    public Optional<String> build(DefRespSmsEventSnapshot event) {
        if (event == null) {
            return Optional.empty();
        }
        if (!hasRequiredScope(event)) {
            return Optional.empty();
        }
        if (event.source() == Source.ALARM && !hasRequiredAlarmData(event)) {
            return Optional.empty();
        }
        String content = switch (event.action()) {
            case START -> event.source() == Source.ALARM ? buildAlarmStart(event) : buildDirectStart(event);
            case END -> event.source() == Source.ALARM ? buildAlarmEnd(event) : buildDirectEnd(event);
            case ADJUST -> event.source() == Source.ALARM ? buildAlarmAdjust(event) : buildDirectAdjust(event);
        };
        return Optional.of(content + "\n\n" + LOCAL_BUREAU);
    }

    private boolean hasRequiredScope(DefRespSmsEventSnapshot event) {
        return switch (event.action()) {
            case START -> !event.afterTownLevels().isEmpty();
            case END -> !event.beforeTownLevels().isEmpty();
            case ADJUST -> event.hasActualChange();
        };
    }

    private boolean hasRequiredAlarmData(DefRespSmsEventSnapshot event) {
        return switch (event.action()) {
            case START -> hasStartAndLevel(event.afterAlarm());
            case END -> hasEndAndLevel(event.beforeAlarm());
            case ADJUST -> hasLevel(event.beforeAlarm()) && hasStartAndLevel(event.afterAlarm());
        };
    }

    private String buildAlarmStart(DefRespSmsEventSnapshot event) {
        AlarmSnapshot alarm = event.afterAlarm();
        return STATE_BUREAU + "已于" + format(alarm.validStart()) + "，对" + event.county()
            + "启动地质灾害防御" + romanLevel(alarm.defenseResponseLevel()) + "响应。按照" + PLAN_NAME
            + "，经专家会商研判，我局决定于" + format(event.localActionTime()) + "启动"
            + scopeGroups(event.county(), event.afterTownLevels(), "") + "。" + START_REQUIREMENTS;
    }

    private String buildAlarmEnd(DefRespSmsEventSnapshot event) {
        AlarmSnapshot alarm = event.beforeAlarm();
        return STATE_BUREAU + "已于" + format(alarm.validEnd()) + "，终止" + event.county()
            + "地质灾害防御" + romanLevel(alarm.defenseResponseLevel()) + "响应，结合最新气象数据和地质灾害气象风险预警结果，按照"
            + PLAN_NAME + "，经专家会商研判，我局决定于" + format(event.localActionTime()) + "终止"
            + scopeGroups(event.county(), event.beforeTownLevels(), "") + "，转为常态化防灾状态。" + END_REQUIREMENTS;
    }

    private String buildAlarmAdjust(DefRespSmsEventSnapshot event) {
        Integer beforeLevel = event.beforeAlarm().defenseResponseLevel();
        Integer afterLevel = event.afterAlarm().defenseResponseLevel();
        String lead = beforeLevel.equals(afterLevel)
            ? STATE_BUREAU + "已于" + format(event.afterAlarm().validStart()) + "，维持" + event.county()
                + "地质灾害防御" + romanLevel(afterLevel) + "响应。"
            : STATE_BUREAU + "已于" + format(event.afterAlarm().validStart()) + "，将" + event.county()
                + "地质灾害防御响应等级由" + romanLevel(beforeLevel) + "调整为"
                + romanLevel(afterLevel) + "。";
        return lead + "按照" + PLAN_NAME
            + "，经专家会商研判，我局决定于" + format(event.localActionTime())
            + adjustmentText(event) + "。" + ADJUST_REQUIREMENTS;
    }

    private String buildDirectStart(DefRespSmsEventSnapshot event) {
        return "据恩施州地质灾害气象风险预警显示，" + directWarningGroups(event) + "。按照" + PLAN_NAME
            + "，经专家会商研判，并报" + STATE_BUREAU + "批准，决定于" + format(event.localActionTime()) + "启动"
            + scopeGroups(event.county(), event.afterTownLevels(), "") + "。" + START_REQUIREMENTS;
    }

    private String buildDirectEnd(DefRespSmsEventSnapshot event) {
        return "根据最新气象数据及恩施州地质灾害气象风险预警结果，按照" + PLAN_NAME
            + "，经专家会商研判，并报" + STATE_BUREAU + "批准，决定于" + format(event.localActionTime()) + "终止"
            + scopeGroups(event.county(), event.beforeTownLevels(), "") + "，转为常态化防灾状态。" + END_REQUIREMENTS;
    }

    private String buildDirectAdjust(DefRespSmsEventSnapshot event) {
        return "根据最新气象数据及恩施州地质灾害气象风险预警结果，按照" + PLAN_NAME
            + "，经专家会商研判，并报" + STATE_BUREAU + "批准，决定于" + format(event.localActionTime())
            + adjustmentText(event) + "。" + ADJUST_REQUIREMENTS;
    }

    private String directWarningGroups(DefRespSmsEventSnapshot event) {
        return groupByLevel(event.afterTownLevels()).entrySet().stream()
            .map(entry -> format(event.localActionTime()) + event.county() + String.join("、", entry.getValue())
                + "发布" + warningColor(entry.getKey()) + "预警，发生地质灾害风险" + riskDescription(entry.getKey()))
            .collect(Collectors.joining("；"));
    }

    private String adjustmentText(DefRespSmsEventSnapshot event) {
        Map<String, Integer> before = event.beforeTownLevels();
        Map<String, Integer> after = event.afterTownLevels();
        List<String> clauses = new ArrayList<>();

        Map<String, Integer> added = select(after, town -> !before.containsKey(town));
        if (!added.isEmpty()) {
            clauses.add("启动" + scopeGroups(event.county(), added, ""));
        }

        Map<String, Integer> removed = select(before, town -> !after.containsKey(town));
        if (!removed.isEmpty()) {
            clauses.add("终止" + scopeGroups(event.county(), removed, "原"));
        }

        Map<LevelChange, List<String>> changed = new LinkedHashMap<>();
        levelChanges(before, after, false).forEach(change -> changed.computeIfAbsent(change.levels(), ignored -> new ArrayList<>())
            .add(change.town()));
        changed.entrySet().stream()
            .sorted(Map.Entry.<LevelChange, List<String>>comparingByKey(LevelChange.ORDER))
            .map(entry -> "将" + event.county() + String.join("、", entry.getValue()) + "地质灾害防御响应等级由"
                + romanLevel(entry.getKey().beforeLevel()) + "调整为" + romanLevel(entry.getKey().afterLevel()))
            .forEach(clauses::add);

        Map<String, Integer> unchanged = select(after,
            town -> before.containsKey(town) && before.get(town).equals(after.get(town)));
        groupByLevel(unchanged).forEach((level, towns) -> clauses.add("将" + event.county()
            + String.join("、", towns) + "地质灾害防御响应等级由" + romanLevel(level)
            + "调整为" + romanLevel(level)));
        return String.join("；", clauses);
    }

    private List<TownLevelChange> levelChanges(Map<String, Integer> before, Map<String, Integer> after,
                                                boolean includeUnchanged) {
        List<TownLevelChange> changes = new ArrayList<>();
        after.forEach((town, afterLevel) -> {
            Integer beforeLevel = before.get(town);
            if (beforeLevel != null && (includeUnchanged || !beforeLevel.equals(afterLevel))) {
                changes.add(new TownLevelChange(town, new LevelChange(beforeLevel, afterLevel)));
            }
        });
        return changes;
    }

    private Map<String, Integer> select(Map<String, Integer> source, java.util.function.Predicate<String> predicate) {
        Map<String, Integer> selected = new LinkedHashMap<>();
        source.forEach((town, level) -> {
            if (predicate.test(town)) {
                selected.put(town, level);
            }
        });
        return selected;
    }

    private String scopeGroups(String county, Map<String, Integer> townLevels, String levelPrefix) {
        return groupByLevel(townLevels).entrySet().stream()
            .map(entry -> county + String.join("、", entry.getValue()) + "地质灾害防御" + levelPrefix
                + romanLevel(entry.getKey()) + "响应")
            .collect(Collectors.joining("；"));
    }

    private Map<Integer, List<String>> groupByLevel(Map<String, Integer> townLevels) {
        Map<Integer, List<String>> grouped = new LinkedHashMap<>();
        townLevels.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> grouped.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey()));
        return grouped;
    }

    private boolean hasStartAndLevel(AlarmSnapshot alarm) {
        return alarm != null && alarm.validStart() != null && alarm.defenseResponseLevel() != null;
    }

    private boolean hasEndAndLevel(AlarmSnapshot alarm) {
        return alarm != null && alarm.validEnd() != null && alarm.defenseResponseLevel() != null;
    }

    private boolean hasLevel(AlarmSnapshot alarm) {
        return alarm != null && alarm.defenseResponseLevel() != null;
    }

    private String format(LocalDateTime time) {
        return time.format(CHINESE_HOUR);
    }

    private String romanLevel(Integer level) {
        return LevelCodeUtil.resolveDefenseResponseRomanLevel(level);
    }

    private String warningColor(Integer level) {
        return LevelCodeUtil.resolveWarningColorName(level);
    }

    private String riskDescription(Integer level) {
        return switch (level) {
            case 4 -> "极高";
            case 3 -> "高";
            case 2 -> "中";
            case 1 -> "低";
            default -> throw new IllegalArgumentException("响应等级必须为1至4");
        };
    }

    private record TownLevelChange(String town, LevelChange levels) {
    }

    private record LevelChange(Integer beforeLevel, Integer afterLevel) {
        private static final java.util.Comparator<LevelChange> ORDER = java.util.Comparator
            .comparing(LevelChange::afterLevel, java.util.Comparator.reverseOrder())
            .thenComparing(LevelChange::beforeLevel, java.util.Comparator.reverseOrder());
    }
}
