/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;

/**
 * 事件级别枚举
 * 对应DzTaskHandle.eventLevel字段
 *
 * @author qoder
 */
@Getter
@AllArgsConstructor
public enum EventLevelEnum {

    SMALL(1, "小型"),
    MEDIUM(2, "中型"),
    LARGE(3, "大型"),
    EXTRA_LARGE(4, "特大型");

    /**
     * 级别编码，统一采用数值越大表示灾情规模越大
     */
    private final Integer code;

    /**
     * 级别名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param code 枚举值
     * @return 枚举对象
     */
    public static EventLevelEnum getByCode(Integer code) {
        for (EventLevelEnum eventLevel : values()) {
            if (eventLevel.getCode().equals(code)) {
                return eventLevel;
            }
        }
        return null;
    }

    /**
     * 根据名称获取编码
     *
     * @param name 名称
     * @return 编码值
     */
    public static Integer getCodeByName(String name) {
        for (EventLevelEnum eventLevel : values()) {
            if (eventLevel.getName().equals(name)) {
                return eventLevel.getCode();
            }
        }
        return null;
    }

    public static String getDisplayName(Integer code) {
        return LevelCodeUtil.resolveEventLevelName(code);
    }
}
