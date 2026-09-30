/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 地理距离计算工具类
 * 支持解析 POINT 格式坐标并计算两点间距离（Haversine公式）
 */
public final class GeoDistanceUtil {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * 匹配 POINT(lng lat) 或 POINT Z (lng lat ele) 格式
     */
    private static final Pattern POINT_PATTERN = Pattern.compile("POINT\\s*(?:Z\\s*)?\\(([\\d.+-]+)\\s+([\\d.+-]+)");

    private GeoDistanceUtil() {
    }

    /**
     * 从 POINT 格式字符串解析经纬度
     *
     * @param pointStr POINT格式字符串，如 POINT(104.098764 30.512346) 或 POINT Z (109.908 30.298 1001)
     * @return 长度为2的数组 [经度, 纬度]，解析失败返回 null
     */
    public static double[] parsePoint(String pointStr) {
        if (StrUtil.isBlank(pointStr)) {
            return null;
        }
        Matcher matcher = POINT_PATTERN.matcher(pointStr.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            double lng = Double.parseDouble(matcher.group(1));
            double lat = Double.parseDouble(matcher.group(2));
            return new double[]{lng, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 计算两点间距离（米）
     *
     * @param lng1 点1经度
     * @param lat1 点1纬度
     * @param lng2 点2经度
     * @param lat2 点2纬度
     * @return 距离（米）
     */
    public static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 判断两个 POINT 格式坐标间的距离是否在指定范围内
     *
     * @param point1   POINT格式坐标1
     * @param point2   POINT格式坐标2
     * @param maxMeters 最大允许距离（米）
     * @return 若任一坐标解析失败返回 false；距离在范围内返回 true，否则返回 false
     */
    public static boolean isWithinDistance(String point1, String point2, double maxMeters) {
        double[] coord1 = parsePoint(point1);
        double[] coord2 = parsePoint(point2);
        if (coord1 == null || coord2 == null) {
            return false;
        }
        double distance = distanceMeters(coord1[0], coord1[1], coord2[0], coord2[1]);
        return distance <= maxMeters;
    }
}
