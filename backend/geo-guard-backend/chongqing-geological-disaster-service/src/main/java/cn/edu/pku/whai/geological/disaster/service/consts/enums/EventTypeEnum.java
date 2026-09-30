/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 事件类型枚举
 * 对应DzTaskHandle.eventType字段
 *
 * @author qoder
 */
@Getter
@AllArgsConstructor
public enum EventTypeEnum {

    LANDSLIDE(1, "滑坡"),
    ROCKFALL(2, "崩塌"),
    GROUND_COLLAPSE(3, "地面塌陷"),
    DEBRIS_FLOW(4, "泥石流"),
    UNSTABLE_ROCK(5, "危岩"),
    OTHER(100, "其他");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param code 枚举值
     * @return 枚举对象
     */
    public static EventTypeEnum getByCode(Integer code) {
        for (EventTypeEnum eventType : values()) {
            if (eventType.getCode().equals(code)) {
                return eventType;
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
        for (EventTypeEnum eventType : values()) {
            if (eventType.getName().equals(name)) {
                return eventType.getCode();
            }
        }
        return null;
    }
}
