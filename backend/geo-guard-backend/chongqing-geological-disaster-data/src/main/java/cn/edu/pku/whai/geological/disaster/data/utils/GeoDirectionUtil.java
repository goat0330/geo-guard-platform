/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

/**
 * 地理方位角度 → 8 方位名工具。
 * 0°=N（北），顺时针：N/NE/E/SE/S/SW/W/NW。
 *
 * @author zhuzc
 * @date 2026-06-17
 */
public final class GeoDirectionUtil {

    private GeoDirectionUtil() {
    }

    /**
     * 将方位角度归类为 8 方位名。
     * 边界规则：以 22.5°/67.5°/112.5°/157.5°/202.5°/247.5°/292.5°/337.5° 为分界。
     * 负数或大于 360 的角度先模 360 归一化。
     *
     * @param degrees 方位角度（度，0=北，顺时针）
     * @return 8 方位名之一：N/NE/E/SE/S/SW/W/NW
     */
    public static String azimuthDegreesTo8Dir(double degrees) {
        double d = (degrees % 360 + 360) % 360;
        if (d >= 337.5 || d < 22.5) {
            return "N";
        }
        if (d < 67.5) {
            return "NE";
        }
        if (d < 112.5) {
            return "E";
        }
        if (d < 157.5) {
            return "SE";
        }
        if (d < 202.5) {
            return "S";
        }
        if (d < 247.5) {
            return "SW";
        }
        if (d < 292.5) {
            return "W";
        }
        return "NW";
    }
}
