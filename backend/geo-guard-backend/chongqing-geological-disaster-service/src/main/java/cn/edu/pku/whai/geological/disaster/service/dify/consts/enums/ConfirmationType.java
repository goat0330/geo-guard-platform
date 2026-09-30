/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 确认类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConfirmationType {
    /**
     * 是/否
     */
    BOOL("bool"),
    /**
     * 输入内容
     */
    OBJ("obj");

    private final String code;
}
