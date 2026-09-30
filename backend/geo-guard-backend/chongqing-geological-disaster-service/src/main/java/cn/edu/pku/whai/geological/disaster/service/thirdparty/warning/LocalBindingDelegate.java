/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DeviceLatestWarningInfo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusSnapshot;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventPageResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventRow;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalBindingDelegate {
    private final ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport;

    public LocalBindingDelegate(ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport) {
        this.thirdPartyWarningDataSplitSupport = thirdPartyWarningDataSplitSupport;
    }

    List<MonitorPoint> queryMonitorPoints(String regionCode, String monitorPointName) {
        LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(monitorPointName), MonitorPoint::getMonitorName, monitorPointName);
        if (StrUtil.isNotBlank(regionCode)) {
            if (regionCode.length() < 12) {
                lqw.likeRight(MonitorPoint::getAdministrativeRegionCode, regionCode);
            } else {
                lqw.eq(MonitorPoint::getAdministrativeRegionCode, regionCode);
            }
        }
        return thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectList(lqw);
    }

    List<MonitorDevice> queryMonitorDevices(Set<String> pointIds, String deviceCode, String deviceName, String manufacturer) {
        if (pointIds == null || pointIds.isEmpty()) {
            return new ArrayList<MonitorDevice>();
        }
        LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery();
        lqw.in(MonitorDevice::getMonitoringPointId, pointIds);
        lqw.like(StrUtil.isNotBlank(deviceCode), MonitorDevice::getDeviceBusinessCode, deviceCode);
        lqw.like(StrUtil.isNotBlank(deviceName), MonitorDevice::getDeviceName, deviceName);
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectList(lqw);
        if (devices == null) {
            return new ArrayList<MonitorDevice>();
        }
        if (StrUtil.isBlank(manufacturer)) {
            return new ArrayList<MonitorDevice>(devices);
        }
        return devices.stream()
                      .filter(device -> StrUtil.containsIgnoreCase(StrUtil.nullToDefault(device.getDeviceParameters(), ""), manufacturer)
                              || StrUtil.containsIgnoreCase(StrUtil.nullToDefault(device.getOperationMaintenanceUnit(), ""), manufacturer))
                      .collect(Collectors.toCollection(ArrayList::new));
    }

    List<MonitorDevice> filterBySlopeUnitId(List<MonitorDevice> devices, String slopeUnitId) {
        if (StrUtil.isBlank(slopeUnitId) || devices == null || devices.isEmpty()) {
            return devices == null ? new ArrayList<MonitorDevice>() : new ArrayList<MonitorDevice>(devices);
        }
        return devices.stream()
                      .filter(device -> device != null && StrUtil.equals(slopeUnitId, Objects.toString(device.getSlopeUnitId(), null)))
                      .collect(Collectors.toCollection(ArrayList::new));
    }

    Map<String, MonitorPoint> buildMonitorPointById(List<MonitorPoint> points) {
        Map<String, MonitorPoint> result = new LinkedHashMap<String, MonitorPoint>();
        for (MonitorPoint point : points) {
            if (point != null && StrUtil.isNotBlank(point.getId())) {
                result.putIfAbsent(point.getId(), point);
            }
        }
        return result;
    }

    Map<String, MonitorPoint> buildMonitorPointByName(List<MonitorPoint> points) {
        Map<String, MonitorPoint> result = new LinkedHashMap<String, MonitorPoint>();
        for (MonitorPoint point : points) {
            if (point != null && StrUtil.isNotBlank(point.getMonitorName())) {
                result.putIfAbsent(point.getMonitorName(), point);
            }
        }
        return result;
    }

    MonitorPoint resolveLocalMonitorPoint(Map<String, MonitorPoint> pointById, Map<String, MonitorPoint> pointByName,
                                          String monitorPointId, String monitorPointName) {
        MonitorPoint monitorPoint = StrUtil.isBlank(monitorPointId) ? null : pointById.get(monitorPointId);
        if (monitorPoint != null) {
            return monitorPoint;
        }
        return StrUtil.isBlank(monitorPointName) ? null : pointByName.get(monitorPointName);
    }

    MonitorPoint resolveRemoteLocalMonitorPoint(
            ThirdMonitorDeviceWarningResp warningResp,
            Map<String, MonitorPoint> localMonitorPointMap,
            Map<String, MonitorPoint> pointById,
            Map<String, MonitorPoint> pointByName
    ) {
        if (warningResp == null) {
            return null;
        }
        if (localMonitorPointMap != null && !localMonitorPointMap.isEmpty()) {
            MonitorPoint localMonitorPoint = localMonitorPointMap.get("ID:" + warningResp.getJcdid());
            if (localMonitorPoint != null) {
                return localMonitorPoint;
            }
            localMonitorPoint = localMonitorPointMap.get("CODE:" + warningResp.getJcdid());
            if (localMonitorPoint != null) {
                return localMonitorPoint;
            }
            localMonitorPoint = localMonitorPointMap.get("CODE:" + warningResp.getJcdbh());
            if (localMonitorPoint != null) {
                return localMonitorPoint;
            }
        }
        return resolveLocalMonitorPoint(pointById, pointByName, warningResp.getJcdid(), warningResp.getJcdname());
    }

    Map<String, MonitorDevice> loadMonitorDeviceById(Set<String> pointIds) {
        List<MonitorDevice> devices = queryMonitorDevices(pointIds, null, null, null);
        Map<String, MonitorDevice> result = new LinkedHashMap<String, MonitorDevice>();
        for (MonitorDevice device : devices) {
            if (device != null && StrUtil.isNotBlank(device.getId())) {
                result.putIfAbsent(device.getId(), device);
            }
        }
        return result;
    }

    Map<String, MonitorDevice> buildMonitorDeviceByClientId(Collection<MonitorDevice> devices) {
        Map<String, MonitorDevice> result = new LinkedHashMap<String, MonitorDevice>();
        for (MonitorDevice device : devices) {
            if (device != null && StrUtil.isNotBlank(device.getClientId())) {
                result.putIfAbsent(device.getClientId(), device);
            }
        }
        return result;
    }

    LocalStatusLookup loadLocalStatusLookup(List<MonitorDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return new LocalStatusLookup(Map.of(), Map.of());
        }
        Set<String> deviceIds = devices.stream().map(MonitorDevice::getId).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> clientIds = devices.stream().map(MonitorDevice::getClientId).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        if (deviceIds.isEmpty() && clientIds.isEmpty()) {
            return new LocalStatusLookup(Map.of(), Map.of());
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> clauses = new ArrayList<String>();
        if (!deviceIds.isEmpty()) {
            clauses.add("device_id IN (:deviceIds)");
            params.addValue("deviceIds", deviceIds);
        }
        if (!clientIds.isEmpty()) {
            clauses.add("client_id IN (:clientIds)");
            params.addValue("clientIds", clientIds);
        }
        String sql = """
                SELECT device_id, client_id, status_code, status_name, last_online_time, source_update_time
                FROM data_tp_device_status_current
                WHERE (
                """ + String.join(" OR ", clauses) + """
                )
                """;
        List<LocalStatusSnapshot> rows = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new LocalStatusSnapshot(
                        rs.getString("device_id"),
                        rs.getString("client_id"),
                        thirdPartyWarningDataSplitSupport.getNullableInt(rs, "status_code"),
                        rs.getString("status_name"),
                        rs.getTimestamp("last_online_time"),
                        rs.getTimestamp("source_update_time")
                )
        );
        Map<String, LocalStatusSnapshot> byDeviceId = new LinkedHashMap<String, LocalStatusSnapshot>();
        Map<String, LocalStatusSnapshot> byClientId = new LinkedHashMap<String, LocalStatusSnapshot>();
        for (LocalStatusSnapshot row : rows) {
            if (StrUtil.isNotBlank(row.deviceId())) {
                byDeviceId.putIfAbsent(row.deviceId(), row);
            }
            if (StrUtil.isNotBlank(row.clientId())) {
                byClientId.putIfAbsent(row.clientId(), row);
            }
        }
        return new LocalStatusLookup(byDeviceId, byClientId);
    }

    LocalWarningLookup loadLocalWarningLookup(List<MonitorDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return new LocalWarningLookup(Map.of(), Map.of(), Map.of());
        }
        Set<String> deviceIds = devices.stream().map(MonitorDevice::getId).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> clientIds = devices.stream().map(MonitorDevice::getClientId).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> pointIds = devices.stream().map(MonitorDevice::getMonitoringPointId).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        if (deviceIds.isEmpty() && clientIds.isEmpty() && pointIds.isEmpty()) {
            return new LocalWarningLookup(Map.of(), Map.of(), Map.of());
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> clauses = new ArrayList<String>();
        if (!deviceIds.isEmpty()) {
            clauses.add("device_id IN (:warningDeviceIds)");
            params.addValue("warningDeviceIds", deviceIds);
        }
        if (!clientIds.isEmpty()) {
            clauses.add("client_id IN (:warningClientIds)");
            params.addValue("warningClientIds", clientIds);
        }
        if (!pointIds.isEmpty()) {
            clauses.add("monitor_point_id IN (:warningPointIds)");
            params.addValue("warningPointIds", pointIds);
        }
        String sql = """
                SELECT device_id, client_id, monitor_point_id, warning_level, warning_time
                FROM data_tp_warning_event
                WHERE warning_time IS NOT NULL
                  AND (
                """ + String.join(" OR ", clauses) + """
                  )
                ORDER BY warning_time DESC
                """;

        List<LocalWarningEventRow> rows = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new LocalWarningEventRow(
                        null,
                        rs.getString("device_id"),
                        rs.getString("client_id"),
                        rs.getString("monitor_point_id"),
                        null,
                        null,
                        thirdPartyWarningDataSplitSupport.parseStoredWarningLevel(rs.getString("warning_level")),
                        rs.getTimestamp("warning_time"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        Map<String, DeviceLatestWarningInfo> byDeviceId = new LinkedHashMap<String, DeviceLatestWarningInfo>();
        Map<String, DeviceLatestWarningInfo> byClientId = new LinkedHashMap<String, DeviceLatestWarningInfo>();
        Map<String, DeviceLatestWarningInfo> byPointId = new LinkedHashMap<String, DeviceLatestWarningInfo>();
        for (LocalWarningEventRow row : rows) {
            DeviceLatestWarningInfo info = new DeviceLatestWarningInfo(row.warningLevel(), thirdPartyWarningDataSplitSupport.formatTimestamp(row.warningTime()));
            if (StrUtil.isNotBlank(row.deviceId())) {
                byDeviceId.putIfAbsent(row.deviceId(), info);
            }
            if (StrUtil.isNotBlank(row.clientId())) {
                byClientId.putIfAbsent(row.clientId(), info);
            }
            if (StrUtil.isNotBlank(row.monitorPointId())) {
                byPointId.putIfAbsent(row.monitorPointId(), info);
            }
        }
        return new LocalWarningLookup(byDeviceId, byClientId, byPointId);
    }

    LocalWarningEventPageResult queryLocalWarningEventPage(ThirdPartyWarningDisposalBo bo, Set<String> pointIds, Set<String> pointNames,
                                                           int pageNum, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> whereClauses = new ArrayList<String>();
        List<String> pointClauses = new ArrayList<String>();
        if (pointIds != null && !pointIds.isEmpty()) {
            pointClauses.add("monitor_point_id IN (:pointIds)");
            params.addValue("pointIds", pointIds);
        }
        if (pointNames != null && !pointNames.isEmpty()) {
            pointClauses.add("monitor_point_name IN (:pointNames)");
            params.addValue("pointNames", pointNames);
        }
        if (!pointClauses.isEmpty()) {
            whereClauses.add("(" + String.join(" OR ", pointClauses) + ")");
        }
        if (StrUtil.isNotBlank(bo.getWarningPublishTime())) {
            whereClauses.add("warning_time >= :warningPublishStart");
            whereClauses.add("warning_time <= :warningPublishEnd");
            params.addValue("warningPublishStart", Timestamp.valueOf(Objects.requireNonNull(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseStartDateTime(bo.getWarningPublishTime()))));
            params.addValue("warningPublishEnd", Timestamp.valueOf(Objects.requireNonNull(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseEndDateTime(bo.getWarningPublishTime()))));
        }
        if (StrUtil.isNotBlank(bo.getWarningDisposalTime())) {
            whereClauses.add("disposal_time >= :warningDisposalStart");
            whereClauses.add("disposal_time <= :warningDisposalEnd");
            params.addValue("warningDisposalStart", Timestamp.valueOf(Objects.requireNonNull(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseStartDateTime(bo.getWarningDisposalTime()))));
            params.addValue("warningDisposalEnd", Timestamp.valueOf(Objects.requireNonNull(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseEndDateTime(bo.getWarningDisposalTime()))));
        }
        if (bo.getWarningLevel() != null) {
            whereClauses.add("(warning_level = :warningLevel OR warning_level = :thirdPartyWarningLevel)");
            params.addValue("warningLevel", String.valueOf(bo.getWarningLevel()));
            params.addValue("thirdPartyWarningLevel", thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().convertToThirdPartyWarningLevel(bo.getWarningLevel()));
        }
        if (bo.getValidWarning() != null) {
            whereClauses.add("valid_warning = :validWarning");
            params.addValue("validWarning", bo.getValidWarning());
        }
        if (bo.getDisposalStatus() != null) {
            whereClauses.add("disposal_status = :disposalStatus");
            params.addValue("disposalStatus", bo.getDisposalStatus());
        }
        if (StrUtil.isNotBlank(bo.getDisposalType())) {
            whereClauses.add("disposal_type = :disposalType");
            params.addValue("disposalType", bo.getDisposalType());
        }

        String whereSql = whereClauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", whereClauses);
        String countSql = "SELECT COUNT(1) FROM data_tp_warning_event" + (StrUtil.isBlank(whereSql) ? "" : " " + whereSql);
        Long total = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().queryForObject(countSql, params, Long.class);

        params.addValue("limit", pageSize);
        params.addValue("offset", Math.max(0, (pageNum - 1) * pageSize));
        // noinspection SqlResolve
        String dataSql = """
                SELECT warning_id, device_id, client_id, monitor_point_id, monitor_point_name, warning_device_name,
                       warning_level, warning_time, disposal_status, disposal_type, disposal_time,
                       disposal_person, valid_warning, source_update_time
                FROM data_tp_warning_event
                """ + (StrUtil.isBlank(whereSql) ? "" : whereSql + "\n") + """
                ORDER BY warning_time DESC NULLS LAST, monitor_point_name NULLS LAST
                LIMIT :limit OFFSET :offset
                """;
        List<LocalWarningEventRow> rows = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().query(dataSql, params, (rs, rowNum) ->
                new LocalWarningEventRow(
                        rs.getString("warning_id"),
                        rs.getString("device_id"),
                        rs.getString("client_id"),
                        rs.getString("monitor_point_id"),
                        rs.getString("monitor_point_name"),
                        rs.getString("warning_device_name"),
                        thirdPartyWarningDataSplitSupport.parseStoredWarningLevel(rs.getString("warning_level")),
                        rs.getTimestamp("warning_time"),
                        thirdPartyWarningDataSplitSupport.getNullableInt(rs, "disposal_status"),
                        rs.getString("disposal_type"),
                        rs.getTimestamp("disposal_time"),
                        rs.getString("disposal_person"),
                        thirdPartyWarningDataSplitSupport.getNullableInt(rs, "valid_warning"),
                        rs.getTimestamp("source_update_time")
                )
        );
        return new LocalWarningEventPageResult(total, rows);
    }

    LocalStatusSnapshot resolveLocalStatus(MonitorDevice device, LocalStatusLookup lookup) {
        if (device == null || lookup == null) {
            return null;
        }
        LocalStatusSnapshot status = StrUtil.isBlank(device.getId()) ? null : lookup.byDeviceId().get(device.getId());
        if (status != null) {
            return status;
        }
        return StrUtil.isBlank(device.getClientId()) ? null : lookup.byClientId().get(device.getClientId());
    }

    DeviceLatestWarningInfo resolveLatestWarningInfo(MonitorDevice device, LocalWarningLookup lookup) {
        if (device == null || lookup == null) {
            return null;
        }
        if (StrUtil.isNotBlank(device.getId())) {
            DeviceLatestWarningInfo byDeviceId = lookup.byDeviceId().get(device.getId());
            if (byDeviceId != null) {
                return byDeviceId;
            }
        }
        if (StrUtil.isNotBlank(device.getClientId())) {
            DeviceLatestWarningInfo byClientId = lookup.byClientId().get(device.getClientId());
            if (byClientId != null) {
                return byClientId;
            }
        }
        return StrUtil.isBlank(device.getMonitoringPointId()) ? null : lookup.byPointId().get(device.getMonitoringPointId());
    }

    void enrichMonitorWarningDisposalRuntimeFields(
            ThirdPartyWarningDisposalVo vo,
            MonitorDevice device,
            LocalStatusSnapshot status,
            DeviceLatestWarningInfo latestWarningInfo,
            String fallbackWarningTime
    ) {
        if (vo == null) {
            return;
        }
        if (device != null) {
            vo.setDeviceId(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().firstNonBlank(vo.getDeviceId(), device.getId()));
            vo.setId(device.getId());
            vo.setDeviceName(device.getDeviceName());
            vo.setDeviceStatus(device.getDeviceStatus());
            vo.setClientId(device.getClientId());
            vo.setDeviceSerialNumber(device.getDeviceSerialNumber());
            vo.setDeviceLoginUsername(device.getDeviceLoginUsername());
            vo.setGlobalUniqueId(device.getGlobalUniqueId());
            vo.setDeviceParameters(device.getDeviceParameters());
            vo.setDeviceBusinessCode(device.getDeviceBusinessCode());
            vo.setMonitoringPointId(device.getMonitoringPointId());
            vo.setAccessProtocol(device.getAccessProtocol());
            vo.setCommunicationMethod(device.getCommunicationMethod());
            vo.setDeviceType(device.getDeviceType());
            vo.setDeviceModel(device.getDeviceModel());
            vo.setDeviceInstallAddress(device.getDeviceInstallAddress());
            vo.setIotCardNumber(device.getIotCardNumber());
            vo.setMonitoringType(device.getMonitoringType());
            vo.setIsEnabled(device.getIsEnabled());
            vo.setDeviceEnableTime(device.getDeviceEnableTime());
            vo.setDeviceLastOnlineTime(device.getDeviceLastOnlineTime());
            vo.setCreatedBy(device.getCreatedBy());
            vo.setCreatedTime(device.getCreatedTime());
            vo.setUpdatedBy(device.getUpdatedBy());
            vo.setUpdatedTime(device.getUpdatedTime());
            vo.setDeviceCategory(device.getDeviceCategory());
            vo.setIsSyncToScreen(device.getIsSyncToScreen());
            vo.setDeviceLatestOnlineTime(device.getDeviceLatestOnlineTime());
            vo.setBatchSyncDeviceCode(device.getBatchSyncDeviceCode());
            vo.setGatewayDeviceSn(device.getGatewayDeviceSn());
            vo.setDisasterType(device.getDisasterType());
            vo.setOperationMaintenanceUnit(device.getOperationMaintenanceUnit());
            vo.setSlopeUnitId(device.getSlopeUnitId());
            vo.setSlopeUnitName(device.getSlopeUnitName());
            vo.setPilotArea1(device.getPilotArea1());
            vo.setPilotArea2(device.getPilotArea2());
            vo.setProvince(device.getProvince());
            vo.setCity(device.getCity());
            vo.setCounty(device.getCounty());
            vo.setStreet(device.getStreet());
            vo.setVillage(device.getVillage());
            vo.setProvinceCode(device.getProvinceCode());
            vo.setCityCode(device.getCityCode());
            vo.setCountyCode(device.getCountyCode());
            vo.setStreetCode(device.getStreetCode());
            vo.setVillageCode(device.getVillageCode());
        }
        vo.setWarningTime(latestWarningInfo == null ? fallbackWarningTime : latestWarningInfo.warningTime());
        String statusName = status == null ? null : thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().firstNonBlank(status.statusName(), thirdPartyWarningDataSplitSupport.buildStatusName(status.statusCode()));
        vo.setOnlineStatus(thirdPartyWarningDataSplitSupport.resolveOnlineStatus(statusName));
    }

    /**
     * 构建本地监测点映射
     */
    Map<String, MonitorPoint> buildLocalMonitorPointMap(List<MonitorPoint> localMonitorPoints) {
        Map<String, MonitorPoint> result = new HashMap<String, MonitorPoint>();
        for (MonitorPoint localMonitorPoint : localMonitorPoints) {
            if (StrUtil.isNotBlank(localMonitorPoint.getId())) {
                result.put("ID:" + localMonitorPoint.getId(), localMonitorPoint);
            }
            if (StrUtil.isNotBlank(localMonitorPoint.getMonitorCode())) {
                result.put("CODE:" + localMonitorPoint.getMonitorCode(), localMonitorPoint);
            }
        }
        return result;
    }
}
