/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;

/**
 * 响应状态枚举
 * 对应DzTaskHandle.respStatus字段
 *
 * @author qoder
 */
@Getter
@AllArgsConstructor
public enum ResponseStatusEnum {

    NOT_STARTED(0, "未启动响应"),
    LEVEL_4(1, "4级响应"),
    LEVEL_3(2, "3级响应"),
    LEVEL_2(3, "2级响应"),
    LEVEL_1(4, "1级响应");

    /**
     * 状态编码，统一采用数值越大表示响应等级越高
     */
    private final Integer code;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param code 枚举值
     * @return 枚举对象
     */
    public static ResponseStatusEnum getByCode(Integer code) {
        for (ResponseStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
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
        for (ResponseStatusEnum status : values()) {
            if (status.getName().equals(name)) {
                return status.getCode();
            }
        }
        return null;
    }

    public static String getDisplayName(Integer code) {
        if (code == null) {
            return null;
        }
        if (NOT_STARTED.getCode().equals(code)) {
            return NOT_STARTED.getName();
        }
        return LevelCodeUtil.resolveDefenseResponseDisplayName(code);
    }
}
