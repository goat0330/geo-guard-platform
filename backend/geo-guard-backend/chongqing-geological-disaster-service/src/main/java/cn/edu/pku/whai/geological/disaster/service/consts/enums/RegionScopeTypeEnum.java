/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 区域防御响应层级：仅 type=区域 时有效。
 */
@Getter
@AllArgsConstructor
public enum RegionScopeTypeEnum {

    COUNTY(1, "县级"),
    TOWN(2, "乡镇");

    private final Integer code;
    private final String name;

    public static RegionScopeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RegionScopeTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
