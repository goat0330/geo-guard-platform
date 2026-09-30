/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;

/**
 * 气象预警等级与防御响应等级映射工具。
 */
public final class DefRespAlarmLevelUtil {

    private static final int ALARM_SOURCE_TYPE_MANUAL = 1;
    private static final int ALARM_SOURCE_TYPE_REPORT_ANALYSIS = 2;

    private DefRespAlarmLevelUtil() {
    }

    /**
     * 将指定来源和原始预警等级严格映射为防御响应等级。
     *
     * <p>该方法是预警类防御响应短信及相关业务使用等级映射的唯一入口。
     * 未知来源、空值、越界等级以及报告解析来源的黄色/蓝色预警均不允许
     * 回退为原始等级，统一返回 {@code null}。</p>
     *
     * @param sourceType 预警来源类型，1人工补录，2解析报告
     * @param warningLevel 原始预警等级，1蓝色、2黄色、3橙色、4红色
     * @return 防御响应等级；无法映射时返回 {@code null}
     */
    public static Integer resolveDefenseResponseLevel(Integer sourceType, Integer warningLevel) {
        if (sourceType == null || warningLevel == null) {
            return null;
        }
        if (sourceType == ALARM_SOURCE_TYPE_REPORT_ANALYSIS) {
            return switch (warningLevel) {
                case 4 -> 3;
                case 3 -> 1;
                case 2, 1 -> null;
                default -> null;
            };
        }
        if (sourceType == ALARM_SOURCE_TYPE_MANUAL) {
            if (warningLevel < 1 || warningLevel > 4) {
                return null;
            }
            return warningLevel;
        }
        return null;
    }

    /**
     * 将数据预警对象中的来源类型与原始预警等级委托至严格映射入口。
     */
    public static Integer resolveDefenseResponseLevel(DataAlarmVo dataAlarmVo, Integer warningLevel) {
        if (dataAlarmVo == null) {
            return null;
        }
        return resolveDefenseResponseLevel(dataAlarmVo.getSourceType(), warningLevel);
    }

    /**
     * 判断来源类型是否属于当前防御响应等级映射边界。
     */
    public static boolean isSupportedAlarmSourceType(Integer sourceType) {
        return sourceType != null
            && (sourceType == ALARM_SOURCE_TYPE_MANUAL || sourceType == ALARM_SOURCE_TYPE_REPORT_ANALYSIS);
    }
}
