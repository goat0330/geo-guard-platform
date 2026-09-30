/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.NearestHouseCimVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 根据坐标解析灾情详细地址。
 */
@Component
@RequiredArgsConstructor
public class DisasterDetailedAddressResolver {

    public static final String CHECK_CENTER_LOCATION_FALLBACK = "打卡坐标数据异常";
    private static final String ENSHI_CITY = "恩施市";
    private static final int[] SEARCH_RADIUS_METERS = {1000, 5000};
    private static final String[] DIRECTION_LABELS = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};

    private final DataHouseMapper dataHouseMapper;

    public String resolve(String pointWkt) {
        double[] coordinates = GeoDistanceUtil.parsePoint(pointWkt);
        if (coordinates == null || coordinates.length < 2) {
            return null;
        }
        BigDecimal longitude = BigDecimal.valueOf(coordinates[0]);
        BigDecimal latitude = BigDecimal.valueOf(coordinates[1]);
        NearestHouseCimVo nearestHouse = findNearestHouse(longitude, latitude);
        if (nearestHouse == null || nearestHouse.getLongitude() == null || nearestHouse.getLatitude() == null) {
            return null;
        }
        return composeDetailedAddress(nearestHouse, coordinates[0], coordinates[1]);
    }

    public String resolveCheckCenterLocation(String pointWkt) {
        String detailedAddress = resolve(pointWkt);
        return StrUtil.blankToDefault(detailedAddress, CHECK_CENTER_LOCATION_FALLBACK);
    }

    private NearestHouseCimVo findNearestHouse(BigDecimal longitude, BigDecimal latitude) {
        for (int radiusMeter : SEARCH_RADIUS_METERS) {
            NearestHouseCimVo nearestHouse = dataHouseMapper.selectNearestHouseByPoint(longitude, latitude, radiusMeter);
            if (nearestHouse != null) {
                return nearestHouse;
            }
        }
        return null;
    }

    static String composeDetailedAddress(NearestHouseCimVo nearestHouse, double targetLongitude, double targetLatitude) {
        if (nearestHouse == null || nearestHouse.getLongitude() == null || nearestHouse.getLatitude() == null) {
            return null;
        }
        return composeDetailedAddress(
            nearestHouse.getBuildingName(),
            nearestHouse.getLongitude().doubleValue(),
            nearestHouse.getLatitude().doubleValue(),
            targetLongitude,
            targetLatitude
        );
    }

    static String composeDetailedAddress(String buildingName, double houseLongitude, double houseLatitude,
                                         double targetLongitude, double targetLatitude) {
        String displayName = extractBuildingDisplayName(buildingName);
        if (StrUtil.isBlank(displayName)) {
            return null;
        }
        long distanceMeters = Math.max(1L, Math.round(GeoDistanceUtil.distanceMeters(
            houseLongitude,
            houseLatitude,
            targetLongitude,
            targetLatitude
        )));
        String direction = resolveDirection(houseLongitude, houseLatitude, targetLongitude, targetLatitude);
        return displayName + direction + "方向" + distanceMeters + "米";
    }

    static String extractBuildingDisplayName(String buildingName) {
        String normalizedName = StrUtil.trim(buildingName);
        if (StrUtil.isBlank(normalizedName)) {
            return null;
        }
        int cityIndex = normalizedName.indexOf(ENSHI_CITY);
        if (cityIndex >= 0) {
            String suffix = StrUtil.trim(normalizedName.substring(cityIndex + ENSHI_CITY.length()));
            if (StrUtil.isNotBlank(suffix)) {
                return suffix;
            }
        }
        return normalizedName;
    }

    static String resolveDirection(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        double deltaLongitude = Math.abs(toLongitude - fromLongitude);
        double deltaLatitude = Math.abs(toLatitude - fromLatitude);
        if (deltaLongitude < 1e-9 && deltaLatitude < 1e-9) {
            return "北";
        }
        double fromLatitudeRad = Math.toRadians(fromLatitude);
        double toLatitudeRad = Math.toRadians(toLatitude);
        double deltaLongitudeRad = Math.toRadians(toLongitude - fromLongitude);
        double y = Math.sin(deltaLongitudeRad) * Math.cos(toLatitudeRad);
        double x = Math.cos(fromLatitudeRad) * Math.sin(toLatitudeRad)
            - Math.sin(fromLatitudeRad) * Math.cos(toLatitudeRad) * Math.cos(deltaLongitudeRad);
        double bearing = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        int index = (int) Math.round(bearing / 45.0) % DIRECTION_LABELS.length;
        return DIRECTION_LABELS[index];
    }
}
