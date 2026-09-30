/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 防御响应方案类型枚举
 * 对应DefRespPlan.type字段
 *
 * @author whai
 */
@Getter
@AllArgsConstructor
public enum DefRespPlanTypeEnum {

    SINGLE(1, "单点"),
    REGION(2, "区域");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static DefRespPlanTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DefRespPlanTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
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
        if (name == null) {
            return null;
        }
        for (DefRespPlanTypeEnum e : values()) {
            if (e.getName().equals(name)) {
                return e.getCode();
            }
        }
        return null;
    }
}
