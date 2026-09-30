/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色枚举，与表 {@code sys_role} 中数据一致（role_id / role_key / role_name）。
 */
@Getter
@AllArgsConstructor
public enum SysRoleEnum {

    DZ_FGXIANZHANG("dz_fgxianzhang", "分管县长"),
    DZ_FGXIANGZHANG("dz_fgxiangzhang", "分管乡长"),
    DZ_CZS("dz_czs", "村支书"),
    DZ_FXQXCY("dz_fxqxcy", "风险区巡查员"),
    DZ_YHDJCY("dz_yhdjcy", "监测员"),
    DZ_JSZCRY("dz_jszcry", "技术支撑单位"),
    DZ_ZJ("dz_zj", "专家"),
    DZ_XZ("dz_xz", "县长"),
    DZ_ZGJLD("dz_zgjld", "县自规局领导"),
    DZ_ZGSSZ("dz_zgssz", "乡自规所所长"),
    DZ_XGY("dz_xgy", "协管员"),
    DZ_QZ("dz_qz", "群众"),
    DZ_ZBY("dz_zby", "值班员");

    private final String roleKey;
    private final String roleName;

    public static SysRoleEnum getByRoleKey(String roleKey) {
        if (roleKey == null) {
            return null;
        }
        for (SysRoleEnum e : values()) {
            if (e.roleKey.equals(roleKey)) {
                return e;
            }
        }
        return null;
    }
}
