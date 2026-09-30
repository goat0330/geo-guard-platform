/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处理进度枚举
 * 对应DzTaskHandle.handleProcess字段
 *
 * @author qoder
 */
@Getter
@AllArgsConstructor
public enum HandleProcessEnum {

    EMERGENCY_INVESTIGATION(1, "应急调查"),
    CONSULTATION_JUDGMENT(2, "会商研判"),
    PLAN_IMPLEMENTATION(3, "方案接入"),
    RESPONSE_EXECUTION(4, "响应执行"),
    CLOSED_ARCHIVED(5, "闭环归档");

    /**
     * 进度编码
     */
    private final Integer code;

    /**
     * 进度名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param code 枚举值
     * @return 枚举对象
     */
    public static HandleProcessEnum getByCode(Integer code) {
        for (HandleProcessEnum process : values()) {
            if (process.getCode().equals(code)) {
                return process;
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
        for (HandleProcessEnum process : values()) {
            if (process.getName().equals(name)) {
                return process.getCode();
            }
        }
        return null;
    }
}