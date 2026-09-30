/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts;

import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;

/**
 * 自动模式执行留痕状态枚举。
 */
public enum AutoModeExecutionStatus {

    RUNNING(1, "RUNNING"),
    SUCCESS(2, "SUCCESS"),
    FAILED(3, "FAILED");

    private final Integer code;
    private final String legacyCode;

    AutoModeExecutionStatus(Integer code, String legacyCode) {
        this.code = code;
        this.legacyCode = legacyCode;
    }

    public Integer getCode() {
        return code;
    }

    public String getLegacyCode() {
        return legacyCode;
    }

    public static Integer resolveCode(Integer status) {
        return status;
    }

    public static Integer resolveCode(String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        String normalized = status.trim();
        for (AutoModeExecutionStatus item : values()) {
            if (item.legacyCode.equalsIgnoreCase(normalized) || String.valueOf(item.code).equals(normalized)) {
                return item.code;
            }
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String legacyCodeOf(Integer status) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(status))
            .map(AutoModeExecutionStatus::getLegacyCode)
            .findFirst()
            .orElse(null);
    }
}
