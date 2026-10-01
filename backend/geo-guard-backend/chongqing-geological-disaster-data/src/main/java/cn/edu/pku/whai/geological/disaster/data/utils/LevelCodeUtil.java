/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

import org.dromara.common.core.utils.StringUtils;

/**
 * 统一处理业务等级编码与展示文案的转换。
 *
 * <p>约定：内部编码统一采用数值越大表示等级越高/越严重。</p>
 */
public final class LevelCodeUtil {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;

    private LevelCodeUtil() {
    }

    /**
     * 将旧版 1=最高/最严重 的四级编码转换为统一编码。
     */
    public static Integer normalizeDescendingFourLevel(Integer legacyLevel) {
        if (legacyLevel == null || legacyLevel < MIN_LEVEL || legacyLevel > MAX_LEVEL) {
            return legacyLevel;
        }
        return MAX_LEVEL + MIN_LEVEL - legacyLevel;
    }

    /**
     * 将统一编码转换为旧版 1=最高/最严重 的四级编码。
     */
    public static Integer toDescendingFourLevel(Integer normalizedLevel) {
        return normalizeDescendingFourLevel(normalizedLevel);
    }

    public static String resolveDynamicRiskLevelName(Integer level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 0 -> "无风险";
            case 1 -> "低风险";
            case 2 -> "中风险";
            case 3 -> "高风险";
            case 4 -> "极高风险";
            default -> String.valueOf(level);
        };
    }

    public static String resolveDynamicRiskSmsName(Integer level) {
        if (level == null) {
            return "待核定";
        }
        return switch (level) {
            case 1 -> "低";
            case 2 -> "中";
            case 3 -> "高";
            case 4 -> "极高";
            default -> "待核定";
        };
    }

    public static String resolveDefenseResponseRoman(Integer level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 4 -> "Ⅰ";
            case 3 -> "Ⅱ";
            case 2 -> "Ⅲ";
            case 1 -> "Ⅳ";
            default -> null;
        };
    }

    public static String resolveDefenseResponseRomanLevel(Integer level) {
        String roman = resolveDefenseResponseRoman(level);
        return roman == null ? null : roman + "级";
    }

    public static String resolveDefenseResponseDisplayName(Integer level) {
        String roman = resolveDefenseResponseRoman(level);
        return roman == null ? null : roman + "级响应";
    }

    public static String resolveWarningColorName(Integer level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 4 -> "红色";
            case 3 -> "橙色";
            case 2 -> "黄色";
            case 1 -> "蓝色";
            default -> null;
        };
    }

    public static Integer parseWarningColorLevel(String levelText) {
        if (StringUtils.isBlank(levelText)) {
            return null;
        }
        return switch (levelText.trim().toUpperCase()) {
            case "红", "红色", "1", "一级", "I", "Ⅰ" -> 4;
            case "橙", "橙色", "2", "二级", "II", "Ⅱ" -> 3;
            case "黄", "黄色", "3", "三级", "III", "Ⅲ" -> 2;
            case "蓝", "蓝色", "4", "四级", "IV", "Ⅳ" -> 1;
            default -> null;
        };
    }

    public static String resolveEventLevelName(Integer level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 1 -> "小型";
            case 2 -> "中型";
            case 3 -> "大型";
            case 4 -> "特大型";
            default -> null;
        };
    }

    public static Integer parseEventLevelName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return switch (name.trim()) {
            case "小型" -> 1;
            case "中型" -> 2;
            case "大型" -> 3;
            case "特大型" -> 4;
            default -> null;
        };
    }

    public static String convertNormalizedWarningLevelToThirdPartyCode(Integer warningLevel) {
        if (warningLevel == null) {
            return null;
        }
        return switch (warningLevel) {
            case 1 -> "C1";
            case 2 -> "C2";
            case 3 -> "C3";
            case 4 -> "C4";
            default -> null;
        };
    }

    public static Integer convertThirdPartyCodeToNormalizedWarningLevel(String warningLevel) {
        if (StringUtils.isBlank(warningLevel)) {
            return null;
        }
        return switch (warningLevel.trim().toUpperCase()) {
            case "C1" -> 4;
            case "C2" -> 3;
            case "C3" -> 2;
            case "C4" -> 1;
            default -> null;
        };
    }
}
