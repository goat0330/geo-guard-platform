/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.defresp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 区域防御响应短信事件快照。
 *
 * <p>乡镇等级映射按行政区展示顺序传入，并在创建快照时冻结，避免异步发送时读取到后续轮次的数据。</p>
 */
public record DefRespSmsEventSnapshot(
    Action action,
    Source source,
    Long defId,
    Integer roundNo,
    LocalDateTime localActionTime,
    String county,
    Map<String, Integer> beforeTownLevels,
    Map<String, Integer> afterTownLevels,
    AlarmSnapshot beforeAlarm,
    AlarmSnapshot afterAlarm
) {

    public DefRespSmsEventSnapshot {
        Objects.requireNonNull(action, "短信动作不能为空");
        Objects.requireNonNull(source, "短信来源不能为空");
        Objects.requireNonNull(defId, "防御响应ID不能为空");
        Objects.requireNonNull(roundNo, "轮次不能为空");
        Objects.requireNonNull(localActionTime, "本地动作时间不能为空");
        if (defId <= 0) {
            throw new IllegalArgumentException("防御响应ID必须大于0");
        }
        if (roundNo <= 0) {
            throw new IllegalArgumentException("轮次必须大于0");
        }
        county = requireText(county, "所属区县不能为空");
        beforeTownLevels = immutableTownLevels(beforeTownLevels);
        afterTownLevels = immutableTownLevels(afterTownLevels);
    }

    /**
     * 判断事件前后的乡镇响应范围或等级是否实际发生变化。
     */
    public boolean hasActualChange() {
        return !beforeTownLevels.equals(afterTownLevels);
    }

    /**
     * 构造发送记录使用的短信类型，包含动作、来源和轮次。
     */
    public String smsType() {
        return "def_resp_" + action.name().toLowerCase() + "_" + source.name().toLowerCase() + "_r" + roundNo;
    }

    /**
     * 生成当前事件完整快照的稳定摘要，用于预览缓存和幂等上下文。
     */
    public String snapshotHash() {
        String canonical = action + "|" + source + "|" + defId + "|" + roundNo + "|" + localActionTime
            + "|" + county + "|" + mapText(beforeTownLevels) + "|" + mapText(afterTownLevels)
            + "|" + alarmText(beforeAlarm) + "|" + alarmText(afterAlarm);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }

    private static Map<String, Integer> immutableTownLevels(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> copy = new LinkedHashMap<>();
        source.forEach((town, level) -> {
            String normalizedTown = requireText(town, "乡镇名称不能为空");
            validateLevel(level, "乡镇响应等级");
            copy.put(normalizedTown, level);
        });
        return Collections.unmodifiableMap(copy);
    }

    private static String mapText(Map<String, Integer> levels) {
        StringBuilder text = new StringBuilder();
        levels.forEach((town, level) -> text.append(town).append('=').append(level).append(';'));
        return text.toString();
    }

    private static String alarmText(AlarmSnapshot alarm) {
        if (alarm == null) {
            return "-";
        }
        return Objects.toString(alarm.validStart(), "-") + ','
            + Objects.toString(alarm.validEnd(), "-") + ','
            + Objects.toString(alarm.alarmId(), "-") + ','
            + Objects.toString(alarm.sourceType(), "-") + ','
            + Objects.toString(alarm.highestWarningLevel(), "-") + ','
            + Objects.toString(alarm.defenseResponseLevel(), "-");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static void validateLevel(Integer level, String fieldName) {
        if (level == null || level < 1 || level > 4) {
            throw new IllegalArgumentException(fieldName + "必须为1至4");
        }
    }

    /**
     * 短信动作。
     */
    public enum Action {
        START,
        END,
        ADJUST
    }

    /**
     * 区域防御响应来源。
     */
    public enum Source {
        ALARM,
        DIRECT
    }

    /**
     * 关联预警在事件发生时的必要快照。
     *
     * @param validStart 预警有效开始时间
     * @param validEnd 预警有效结束时间
     * @param alarmId 关联预警主键
     * @param sourceType 预警来源类型，1人工补录、2解析报告
     * @param highestWarningLevel 最高原始预警等级，1蓝色、2黄色、3橙色、4红色
     * @param defenseResponseLevel 按来源映射后的防御响应等级
     */
    public record AlarmSnapshot(LocalDateTime validStart,
                                LocalDateTime validEnd,
                                Long alarmId,
                                Integer sourceType,
                                Integer highestWarningLevel,
                                Integer defenseResponseLevel) {

        public AlarmSnapshot {
            if (defenseResponseLevel != null) {
                validateLevel(defenseResponseLevel, "防御响应等级");
            }
        }

        /**
         * 保留旧的构造方式；第三参数仍表示旧语义的原始最高预警等级。
         * 来源类型和映射后的防御响应等级留空，使预警类事件按缺少来源 fail-closed。
         * 新建预警快照时应优先使用完整构造器，以冻结来源、原始等级和映射等级。
         */
        public AlarmSnapshot(LocalDateTime validStart, LocalDateTime validEnd, Integer highestWarningLevel) {
            this(validStart, validEnd, null, null, highestWarningLevel, null);
        }

        /**
         * 兼容旧调用方的等级访问器；返回的是原始最高预警等级。
         */
        @Deprecated
        public Integer highestLevel() {
            return highestWarningLevel;
        }
    }
}
