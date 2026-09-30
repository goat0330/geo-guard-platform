/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import org.dromara.common.core.exception.ServiceException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordinateConverter {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+\\.?\\d*");
    private static final Pattern POINT_2D_PATTERN = Pattern.compile(
            "^POINT\\s*\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern POINT_3D_PATTERN = Pattern.compile(
            "^POINT\\s*Z\\s*\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 将 WKT 格式的 POINT 字符串转换为东经北纬描述。
     *
     * @param wktPoint 输入格式如 "POINT Z(120.123456 30.654321 15.0)" 或 "POINT(120.123456 30.654321)"
     * @return 格式化后的经纬度字符串
     */
    public static String convertWktToDisplay(String wktPoint) {
        if (wktPoint == null || wktPoint.isEmpty()) {
            return wktPoint;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(wktPoint);
        if (!matcher.find()) {
            return "解析失败，请检查输入格式";
        }
        String longitude = matcher.group();
        if (!matcher.find()) {
            return "解析失败，请检查输入格式";
        }
        String latitude = matcher.group();
        String altitude = matcher.find() ? matcher.group() : null;
        return formatCoordinate(longitude, latitude, altitude);
    }

    public static String validatePointWkt(String wktPoint, String fieldName) {
        if (wktPoint == null || wktPoint.trim().isEmpty()) {
            throw new ServiceException(fieldName + "不能为空");
        }

        String normalized = wktPoint.trim();
        Matcher point2dMatcher = POINT_2D_PATTERN.matcher(normalized);
        if (point2dMatcher.matches()) {
            validateLngLat(point2dMatcher.group(1), point2dMatcher.group(2), fieldName);
            return normalized;
        }

        Matcher point3dMatcher = POINT_3D_PATTERN.matcher(normalized);
        if (point3dMatcher.matches()) {
            validateLngLat(point3dMatcher.group(1), point3dMatcher.group(2), fieldName);
            return normalized;
        }

        String upperValue = normalized.toUpperCase(Locale.ROOT);
        if (upperValue.startsWith("POINT Z")) {
            throw new ServiceException(fieldName + "格式错误，POINT Z 必须包含经度、纬度和高程三个值");
        }
        throw new ServiceException(fieldName + "格式错误，支持 POINT(lon lat) 或 POINT Z(lon lat z)");
    }

    public static String validateGeometryWkt(String wkt, String fieldName) {
        if (wkt == null || wkt.trim().isEmpty()) {
            throw new ServiceException(fieldName + "不能为空");
        }
        String normalized = wkt.trim();
        try {
            Geometry geometry = new WKTReader().read(normalized);
            if (geometry == null || geometry.isEmpty() || geometry.getDimension() < 2) {
                throw new ServiceException(fieldName + "格式错误");
            }
            return normalized;
        } catch (ParseException e) {
            throw new ServiceException(fieldName + "格式错误");
        }
    }

    public static void validatePointWithinGeometry(String pointWkt, String geometryWkt, String errorMessage) {
        try {
            Geometry geometry = new WKTReader().read(geometryWkt);
            Geometry pointGeometry = new WKTReader().read(pointWkt);
            if (!(pointGeometry instanceof Point point) || !geometry.covers(point)) {
                throw new ServiceException(errorMessage);
            }
        } catch (ParseException e) {
            throw new ServiceException("坐标范围格式错误");
        }
    }

    public static void validateGeometryWithinGeometry(String innerGeometryWkt, String outerGeometryWkt, String errorMessage) {
        try {
            Geometry innerGeometry = new WKTReader().read(innerGeometryWkt);
            Geometry outerGeometry = new WKTReader().read(outerGeometryWkt);
            if (!outerGeometry.covers(innerGeometry)) {
                throw new ServiceException(errorMessage);
            }
        } catch (ParseException e) {
            throw new ServiceException("坐标范围格式错误");
        }
    }

    private static String formatCoordinate(String lonStr, String latStr, String altitudeStr) {
        double lon = Double.parseDouble(lonStr);
        double lat = Double.parseDouble(latStr);

        String lonDir = lon >= 0 ? "东经" : "西经";
        String latDir = lat >= 0 ? "北纬" : "南纬";

        StringBuilder builder = new StringBuilder();
        builder.append(lonDir).append(formatDegreeMinuteSecond(Math.abs(lon)));
        builder.append("; ").append(latDir).append(formatDegreeMinuteSecond(Math.abs(lat)));
        if (altitudeStr != null) {
            builder.append("; 海拔高程").append(formatAltitude(altitudeStr)).append("m");
        }
        return builder.toString();
    }

    private static String formatDegreeMinuteSecond(double value) {
        int degree = (int) Math.floor(value);
        double minuteValue = (value - degree) * 60;
        int minute = (int) Math.floor(minuteValue);
        double second = (minuteValue - minute) * 60;
        BigDecimal secondDecimal = BigDecimal.valueOf(second).setScale(2, RoundingMode.HALF_UP);

        if (secondDecimal.compareTo(BigDecimal.valueOf(60)) >= 0) {
            secondDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            minute++;
        }
        if (minute >= 60) {
            minute = 0;
            degree++;
        }
        return degree + "°" + minute + "'" + secondDecimal.toPlainString() + "\"";
    }

    private static String formatAltitude(String altitudeStr) {
        return new BigDecimal(altitudeStr).stripTrailingZeros().toPlainString();
    }

    private static void validateLngLat(String lonStr, String latStr, String fieldName) {
        double lon = Double.parseDouble(lonStr);
        double lat = Double.parseDouble(latStr);
        if (lon < -180 || lon > 180) {
            throw new ServiceException(fieldName + "格式错误，经度应在 -180 到 180 之间");
        }
        if (lat < -90 || lat > 90) {
            throw new ServiceException(fieldName + "格式错误，纬度应在 -90 到 90 之间");
        }
    }

    public static void main(String[] args) {
        // 测试不同的 POINT 格式
        String rawData = "POINT Z(121.4737 31.2304 10.5)";
        System.out.println("原始数据: " + rawData);
        System.out.println("转换结果: " + convertWktToDisplay(rawData));

        String rawData2 = "POINT(113.2644 23.1291)";
        System.out.println("转换结果: " + convertWktToDisplay(rawData2));
    }
}
