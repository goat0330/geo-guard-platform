/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceTreeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceTreeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.ThirdPartyCurveTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DeviceLatestWarningInfo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DeviceStatusFetchResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.JcdWarningFetchResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusSnapshot;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.StoredCurveRow;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.WarningFetchResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.kongweiguang.http.client.Req;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DeviceDataDelegate {
    private final ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport;

    public DeviceDataDelegate(ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport) {
        this.thirdPartyWarningDataSplitSupport = thirdPartyWarningDataSplitSupport;
    }

    /**
     * 获取三方设备监测曲线
     */
    ThirdPartyDeviceCureVo queryDeviceCure(ThirdPartyDeviceCureBo bo) {
        validateDeviceCureParams(bo);
        ThirdPartyDeviceCureVo vo = new ThirdPartyDeviceCureVo();
        String queryType = extractCurveQueryType(bo.getType());
        MonitorDevice device = findMonitorDeviceByClientId(bo.getClientId());
        vo.setDeviceInfo(buildLocalDeviceInfo(device, bo, queryType));

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("clientId", bo.getClientId());
        params.addValue("monitorType", normalizeCurveStorageMonitorType(bo.getType()));
        params.addValue("startTime", toRawLocalTimestamp(bo.getStartTime()));
        params.addValue("endTime", toRawLocalTimestamp(bo.getEndTime()));

        // noinspection SqlResolve
        String sql = """
                SELECT source_update_time, curve_columns_json, point_values_json
                FROM data_tp_device_curve_hourly
                WHERE client_id = :clientId
                  AND monitor_type = :monitorType
                  AND source_update_time >= :startTime
                  AND source_update_time <= :endTime
                ORDER BY source_update_time
                """;
        List<StoredCurveRow> storedRows = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new StoredCurveRow(
                        rs.getTimestamp("source_update_time"),
                        rs.getString("curve_columns_json"),
                        rs.getString("point_values_json")
                )
        );
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = resolveLocalCurveColumns(queryType, storedRows);
        vo.setColumns(columns);

        List<List<Object>> values = new ArrayList<List<Object>>();
        for (StoredCurveRow storedRow : storedRows) {
            List<Object> row = new ArrayList<Object>();
            row.add(toRawLocalEpochMillis(storedRow.sourceUpdateTime()));
            Map<String, Object> pointValues = parseStoredCurveValueMap(storedRow.pointValuesJson());
            for (ThirdPartyDeviceCureVo.ColumnInfo column : columns) {
                row.add(resolveStoredCurveValue(pointValues, column));
            }
            values.add(row);
        }
        vo.setValues(values);
        return vo;
    }

    private ThirdPartyDeviceCureVo.DeviceInfo buildLocalDeviceInfo(MonitorDevice device, ThirdPartyDeviceCureBo bo, String queryType) {
        ThirdPartyDeviceCureVo.DeviceInfo deviceInfo = new ThirdPartyDeviceCureVo.DeviceInfo();
        deviceInfo.setClientId(bo.getClientId());
        deviceInfo.setDicName(ThirdPartyCurveTypeEnum.resolveDicName(queryType));
        if (device == null) {
            deviceInfo.setMonitorType(queryType);
            return deviceInfo;
        }
        deviceInfo.setMonitorPointId(device.getMonitoringPointId());
        deviceInfo.setMonitorMethod(resolveMonitorMethod(device.getMonitoringType(), queryType));
        deviceInfo.setMonitorType(queryType);
        deviceInfo.setSensorName(device.getDeviceName());
        deviceInfo.setMonitorSerialNo(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().firstNonBlank(device.getDeviceBusinessCode(), device.getDeviceSerialNumber()));
        return deviceInfo;
    }

    private List<ThirdPartyDeviceCureVo.ColumnInfo> buildLocalDeviceCureColumns(String type) {
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = new ArrayList<>();
        String curveType = extractCurveQueryType(type);
        if (ThirdPartyCurveTypeEnum.GP.getDicCode().equalsIgnoreCase(curveType)) {
            columns.add(buildColumn("gpsTotalZ", "Z方向位移", "mm(毫米)"));
            columns.add(buildColumn("gpsTotalX", "X方向位移", "mm(毫米)"));
            columns.add(buildColumn("gpsTotalY", "Y方向位移", "mm(毫米)"));
            return columns;
        }
        if (ThirdPartyCurveTypeEnum.JS.getDicCode().equalsIgnoreCase(curveType)
            || ThirdPartyCurveTypeEnum.QJ.getDicCode().equalsIgnoreCase(curveType)) {
            columns.add(buildColumn("gZ", "Z方向值", ""));
            columns.add(buildColumn("gX", "X方向值", ""));
            columns.add(buildColumn("gY", "Y方向值", ""));
            return columns;
        }
        columns.add(buildColumn("value", "监测值", ""));
        return columns;
    }

    private List<ThirdPartyDeviceCureVo.ColumnInfo> resolveLocalCurveColumns(String queryType, List<StoredCurveRow> storedRows) {
        if (storedRows != null) {
            for (StoredCurveRow storedRow : storedRows) {
                List<ThirdPartyDeviceCureVo.ColumnInfo> parsedColumns = parseStoredCurveColumns(storedRow.columnsJson());
                if (!parsedColumns.isEmpty()) {
                    return parsedColumns;
                }
            }
        }
        return buildLocalDeviceCureColumns(queryType);
    }

    private List<ThirdPartyDeviceCureVo.ColumnInfo> parseStoredCurveColumns(String columnsJson) {
        if (StrUtil.isBlank(columnsJson)) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(columnsJson);
            List<ThirdPartyDeviceCureVo.ColumnInfo> columns = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject columnObj = array.getJSONObject(i);
                if (columnObj == null) {
                    continue;
                }
                String code = StrUtil.blankToDefault(StrUtil.trim(columnObj.getStr("code")), "value" + (i + 1));
                columns.add(buildColumn(
                    code,
                    StrUtil.blankToDefault(StrUtil.trim(columnObj.getStr("name")), code),
                    StrUtil.trim(columnObj.getStr("unit"))
                ));
            }
            return columns;
        } catch (Exception ex) {
            ThirdPartyWarningDataSplitSupport.log.warn("解析本地曲线列定义失败，err={}", ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> parseStoredCurveValueMap(String pointValuesJson) {
        if (StrUtil.isBlank(pointValuesJson)) {
            return Collections.emptyMap();
        }
        try {
            JSONObject jsonObject = JSONUtil.parseObj(pointValuesJson);
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : jsonObject.keySet()) {
                values.put(key, jsonObject.get(key));
            }
            return values;
        } catch (Exception ex) {
            ThirdPartyWarningDataSplitSupport.log.warn("解析本地曲线点值失败，err={}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Object resolveStoredCurveValue(Map<String, Object> pointValues,
                                           ThirdPartyDeviceCureVo.ColumnInfo column) {
        String code = StrUtil.blankToDefault(column.getCode(), "");
        Object storedValue = getIgnoreCase(pointValues, code);
        if (storedValue != null || pointValues.containsKey(code)) {
            return storedValue;
        }
        return null;
    }

    private Object getIgnoreCase(Map<String, Object> values, String key) {
        if (values == null || values.isEmpty() || StrUtil.isBlank(key)) {
            return null;
        }
        if (values.containsKey(key)) {
            return values.get(key);
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (StrUtil.equalsIgnoreCase(entry.getKey(), key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ThirdPartyDeviceCureVo.ColumnInfo buildColumn(String code, String name, String unit) {
        ThirdPartyDeviceCureVo.ColumnInfo column = new ThirdPartyDeviceCureVo.ColumnInfo();
        column.setCode(code);
        column.setName(name);
        column.setUnit(unit);
        return column;
    }

    private MonitorDevice findMonitorDeviceByClientId(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return null;
        }
        LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery();
        lqw.eq(MonitorDevice::getClientId, clientId);
        lqw.last("limit 1");
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectList(lqw);
        return devices == null || devices.isEmpty() ? null : devices.getFirst();
    }

    private String resolveMonitorMethod(String monitoringTypes, String targetType) {
        if (StrUtil.isBlank(monitoringTypes) || StrUtil.isBlank(targetType)) {
            return null;
        }
        String normalizedTargetType = extractCurveQueryType(targetType);
        for (String token : monitoringTypes.split(",")) {
            String trimmed = StrUtil.trim(token);
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }
            String upper = trimmed.toUpperCase();
            if (upper.endsWith("_" + normalizedTargetType)) {
                return StrUtil.subBefore(trimmed, "_", false);
            }
        }
        return null;
    }

    String extractCurveQueryType(String type) {
        if (StrUtil.isBlank(type)) {
            return type;
        }
        String normalized = StrUtil.trim(type).toUpperCase();
        return normalized.contains("_") ? StrUtil.subAfter(normalized, "_", true) : normalized;
    }

    String normalizeCurveStorageMonitorType(String type) {
        return extractCurveQueryType(type);
    }

    void validateDeviceCureParams(ThirdPartyDeviceCureBo bo) {
        if (bo == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (StrUtil.isBlank(bo.getType())) {
            throw new ServiceException("监测类型不能为空");
        }
        if (StrUtil.isBlank(bo.getClientId())) {
            throw new ServiceException("设备ID不能为空");
        }
        if (bo.getStartTime() == null) {
            throw new ServiceException("开始时间不能为空");
        }
        if (bo.getEndTime() == null) {
            throw new ServiceException("结束时间不能为空");
        }
        if (bo.getStartTime() > bo.getEndTime()) {
            throw new ServiceException("开始时间不能大于结束时间");
        }
    }

    private Timestamp toRawLocalTimestamp(Long tsMillis) {
        if (tsMillis == null) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.ofInstant(new Date(tsMillis).toInstant(), java.time.ZoneOffset.UTC));
    }

    private Long toRawLocalEpochMillis(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 获取三方监测设备分页
     */
    TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        ThirdPartyMonitorDeviceBo queryBo = thirdPartyWarningDataSplitSupport.prepareMonitorDeviceQuery(bo, true);
        int pageNum = thirdPartyWarningDataSplitSupport.resolvePageNum(pageQuery);
        int pageSize = thirdPartyWarningDataSplitSupport.resolvePageSize(pageQuery);
        List<MonitorPoint> localMonitorPoints = thirdPartyWarningDataSplitSupport.queryMonitorPoints(queryBo.getRegionCode(), queryBo.getMonitorPointName());
        if (localMonitorPoints.isEmpty()) {
            return TableDataInfo.build(new ArrayList<ThirdPartyMonitorDeviceVo>());
        }

        Map<String, MonitorPoint> pointById = thirdPartyWarningDataSplitSupport.buildMonitorPointById(localMonitorPoints);
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.queryMonitorDevices(pointById.keySet(), queryBo.getDeviceCode(), queryBo.getDeviceName(), queryBo.getManufacturer());
        thirdPartyWarningDataSplitSupport.sortMonitorDevicesByPointAndName(devices, pointById);

        LocalStatusLookup statusLookup = thirdPartyWarningDataSplitSupport.loadLocalStatusLookup(devices);
        List<ThirdPartyMonitorDeviceVo> allRecords = new ArrayList<ThirdPartyMonitorDeviceVo>();
        int rowId = 1;
        for (MonitorDevice device : devices) {
            MonitorPoint point = pointById.get(device.getMonitoringPointId());
            allRecords.add(thirdPartyWarningDataSplitSupport.buildLocalMonitorDeviceVo(device, point, thirdPartyWarningDataSplitSupport.resolveLocalStatus(device, statusLookup), rowId++));
        }

        TableDataInfo<ThirdPartyMonitorDeviceVo> result = TableDataInfo.build(thirdPartyWarningDataSplitSupport.paginateInMemory(allRecords, pageNum, pageSize));
        result.setTotal(allRecords.size());
        return result;
    }

    /**
     * 从三方接口拉取监测设备分页，供同步任务使用。
     */
    TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePageFromRemote(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        ThirdPartyMonitorDeviceBo queryBo = thirdPartyWarningDataSplitSupport.prepareMonitorDeviceQuery(bo, false);

        int pageNum = thirdPartyWarningDataSplitSupport.resolvePageNum(pageQuery);
        int pageSize = thirdPartyWarningDataSplitSupport.resolvePageSize(pageQuery);
        try {
            thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().ensureWarningSsoToken();
            ThirdPartyWarningDataResp resp = Req.post(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl())
                                                .path("/dzapi/jcd/jcsbxxPage")
                                                .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                .json(thirdPartyWarningDataSplitSupport.buildMonitorDevicePageParams(queryBo, pageNum, pageSize))
                                                .timeout(Duration.ofMinutes(3))
                                                .ok()
                                                .obj(ThirdPartyWarningDataResp.class);
            if (ObjectUtil.isNull(resp) || !resp.isSuccess() || ObjectUtil.isNull(resp.getData())) {
                throw thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildThirdPartyException("获取监测设备数据失败", resp);
            }
            JSONObject data = JSONUtil.parseObj(resp.getData());
            List<ThirdPartyMonitorDeviceVo> records = thirdPartyWarningDataSplitSupport.parseMonitorDeviceRecords(data.getJSONArray("records"));
            TableDataInfo<ThirdPartyMonitorDeviceVo> result = TableDataInfo.build(records);
            result.setTotal(data.getLong("total", (long) records.size()));
            return result;
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.error("获取监测设备数据失败: {}", e.getMessage(), e);
            throw new ServiceException("获取监测设备数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据监测点名称构建设备-监测器树结构
     */
    List<ThirdPartyMonitorDeviceTreeVo> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo) {
        if (bo == null || bo.getType() == null) {
            throw new ServiceException("查询类型不能为空");
        }
        if (!Objects.equals(bo.getType(), 1) && !Objects.equals(bo.getType(), 2)) {
            throw new ServiceException("查询类型仅支持1(监测点)或2(监测设备)");
        }
        String queryName = bo.getName();
        ThirdPartyMonitorDeviceBo queryBo = new ThirdPartyMonitorDeviceBo();
        if (Objects.equals(bo.getType(), 1)) {
            queryBo.setMonitorPointName(queryName);
            LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
            lqw.eq(MonitorPoint::getMonitorName, queryName);
            lqw.last("limit 1");
            List<MonitorPointVo> monitorPoints = thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectVoList(lqw);
            if (monitorPoints == null || monitorPoints.isEmpty() || StrUtil.isBlank(monitorPoints.getFirst().getAdministrativeRegionCode())) {
                throw new ServiceException("未找到监测点对应的行政区划编码");
            }
            queryBo.setRegionCode(thirdPartyWarningDataSplitSupport.normalizeRegionCode(monitorPoints.getFirst().getAdministrativeRegionCode()));
        } else {
            queryBo.setDeviceName(queryName);
            LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery();
            lqw.like(MonitorDevice::getDeviceName, queryName);
            lqw.isNotNull(MonitorDevice::getMonitoringPointId);
            lqw.last("limit 1");
            List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectList(lqw);
            if (devices == null || devices.isEmpty() || StrUtil.isBlank(devices.getFirst().getMonitoringPointId())) {
                throw new ServiceException("未找到监测设备对应的监测点");
            }

            LambdaQueryWrapper<MonitorPoint> pointLqw = Wrappers.lambdaQuery();
            pointLqw.eq(MonitorPoint::getId, devices.getFirst().getMonitoringPointId());
            pointLqw.last("limit 1");
            List<MonitorPointVo> monitorPoints = thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectVoList(pointLqw);
            if (monitorPoints == null || monitorPoints.isEmpty() || StrUtil.isBlank(monitorPoints.getFirst().getAdministrativeRegionCode())) {
                throw new ServiceException("未找到监测设备对应的行政区划编码");
            }
            queryBo.setRegionCode(thirdPartyWarningDataSplitSupport.normalizeRegionCode(monitorPoints.getFirst().getAdministrativeRegionCode()));
        }

        List<ThirdPartyMonitorDeviceVo> allDevices = thirdPartyWarningDataSplitSupport.fetchAllMonitorDevices(queryBo, false);
        Map<String, ThirdPartyMonitorDeviceTreeVo> treeMap = new LinkedHashMap<String, ThirdPartyMonitorDeviceTreeVo>();
        for (ThirdPartyMonitorDeviceVo device : allDevices) {
            if (StrUtil.isBlank(device.getClientId())) {
                continue;
            }

            ThirdPartyMonitorDeviceTreeVo treeNode = treeMap.computeIfAbsent(device.getClientId(), key -> {
                ThirdPartyMonitorDeviceTreeVo vo = new ThirdPartyMonitorDeviceTreeVo();
                vo.setId(device.getClientId());
                vo.setName(device.getDeviceName());
                vo.setMonitor(new ArrayList<ThirdPartyMonitorDeviceTreeVo.MonitorItem>());
                return vo;
            });

            Set<String> existedNames = treeNode.getMonitor().stream()
                                               .map(ThirdPartyMonitorDeviceTreeVo.MonitorItem::getName)
                                               .collect(Collectors.toSet());

            for (String monitorName : thirdPartyWarningDataSplitSupport.splitMonitorTypes(device.getMonitorType())) {
                String monitorTypeCode = thirdPartyWarningDataSplitSupport.resolveMonitorTypeCode(monitorName);
                String monitorTypeName = ThirdPartyCurveTypeEnum.resolveDicName(monitorTypeCode);
                if (existedNames.contains(monitorTypeName)) {
                    continue;
                }
                ThirdPartyMonitorDeviceTreeVo.MonitorItem item = new ThirdPartyMonitorDeviceTreeVo.MonitorItem();
                item.setName(monitorTypeName);
                item.setType(monitorTypeCode);
                treeNode.getMonitor().add(item);
            }
        }
        return new ArrayList<ThirdPartyMonitorDeviceTreeVo>(treeMap.values());
    }

    /**
     * 全量：本地 v_monitor_device、按监测点区划拉 jcsbxx、按监测点拉 jcdsbyj，合并最近预警等级与设备状态
     */
    List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId) {
        String queryRegionCode = StrUtil.blankToDefault(regionCode, ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        List<MonitorPoint> localMonitorPoints = thirdPartyWarningDataSplitSupport.queryMonitorPoints(queryRegionCode, null);
        if (localMonitorPoints.isEmpty()) {
            return new ArrayList<MonitorDeviceRuntimeVo>();
        }

        Map<String, MonitorPoint> pointById = thirdPartyWarningDataSplitSupport.buildMonitorPointById(localMonitorPoints);
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.filterBySlopeUnitId(
                thirdPartyWarningDataSplitSupport.queryMonitorDevices(pointById.keySet(), null, null, null),
                slopeUnitId
        );
        thirdPartyWarningDataSplitSupport.sortMonitorDevicesByPointAndName(devices, pointById);
        LocalStatusLookup statusLookup = thirdPartyWarningDataSplitSupport.loadLocalStatusLookup(devices);
        LocalWarningLookup warningLookup = thirdPartyWarningDataSplitSupport.loadLocalWarningLookup(devices);

        List<MonitorDeviceRuntimeVo> result = new ArrayList<MonitorDeviceRuntimeVo>();
        for (MonitorDevice device : devices) {
            MonitorDeviceRuntimeVo vo = new MonitorDeviceRuntimeVo();
            BeanUtil.copyProperties(device, vo);
            DeviceLatestWarningInfo latestWarning = thirdPartyWarningDataSplitSupport.resolveLatestWarningInfo(device, warningLookup);
            vo.setWarningLevel(latestWarning == null ? null : latestWarning.warningLevel());
            vo.setWarningTime(latestWarning == null ? null : latestWarning.warningTime());
            LocalStatusSnapshot status = thirdPartyWarningDataSplitSupport.resolveLocalStatus(device, statusLookup);
            if (status != null) {
                vo.setOnlineStatus(status.statusCode());
                if (status.lastOnlineTime() != null) {
                    Date statusTime = new Date(status.lastOnlineTime().getTime());
                    vo.setDeviceLastOnlineTime(statusTime);
                    vo.setDeviceLatestOnlineTime(statusTime);
                }
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 从三方接口拉取设备运行时状态，供同步任务使用。
     */
    List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntimeFromRemote(String regionCode) {
        String queryRegionCode = StrUtil.blankToDefault(regionCode, ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        List<MonitorPoint> pointRows = thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectIdAndAdministrativeRegionCode();
        if (pointRows == null) {
            pointRows = new ArrayList<MonitorPoint>();
        }
        Map<String, String> pointIdToAddvcd = new HashMap<String, String>();
        for (MonitorPoint p : pointRows) {
            if (p == null || StrUtil.isBlank(p.getId()) || StrUtil.isBlank(p.getAdministrativeRegionCode())) {
                continue;
            }
            if (!thirdPartyWarningDataSplitSupport.matchesRegionCode(p.getAdministrativeRegionCode(), queryRegionCode)) {
                continue;
            }
            pointIdToAddvcd.put(p.getId(), thirdPartyWarningDataSplitSupport.normalizeRegionCode(p.getAdministrativeRegionCode()));
        }
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectList(Wrappers.lambdaQuery());
        if (devices == null) {
            devices = new ArrayList<MonitorDevice>();
        }
        devices = devices.stream()
                         .filter(item -> StrUtil.isNotBlank(item.getMonitoringPointId()) && pointIdToAddvcd.containsKey(item.getMonitoringPointId()))
                         .toList();
        Set<String> regionCodes = new LinkedHashSet<String>();
        for (MonitorDevice d : devices) {
            String pid = d.getMonitoringPointId();
            if (StrUtil.isBlank(pid)) {
                continue;
            }
            String code = pointIdToAddvcd.get(pid);
            if (StrUtil.isNotBlank(code)) {
                regionCodes.add(code);
            }
        }

        List<String> jcdIds = devices.stream()
                                     .map(MonitorDevice::getMonitoringPointId)
                                     .filter(StrUtil::isNotBlank)
                                     .distinct()
                                     .toList();

        CompletableFuture<DeviceStatusFetchResult> deviceStatusStageFuture = CompletableFuture.supplyAsync(() -> {
                                                                                                  thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().ensureWarningSsoToken();
                                                                                                  Map<String, ThirdPartyMonitorDeviceVo> byId = new HashMap<String, ThirdPartyMonitorDeviceVo>();
                                                                                                  Map<String, ThirdPartyMonitorDeviceVo> byClientId = new HashMap<String, ThirdPartyMonitorDeviceVo>();
                                                                                                  List<CompletableFuture<List<ThirdPartyMonitorDeviceVo>>> deviceStatusFutures = regionCodes.stream()
                                                                                                                                                                                            .map(region -> CompletableFuture.supplyAsync(() -> thirdPartyWarningDataSplitSupport.fetchAllMonitorDevicesForRegionSafe(region), ThirdPartyWarningDataSplitSupport.WARNING_LIST_EXECUTOR))
                                                                                                                                                                                            .toList();
                                                                                                  for (CompletableFuture<List<ThirdPartyMonitorDeviceVo>> future : deviceStatusFutures) {
                                                                                                      List<ThirdPartyMonitorDeviceVo> deviceStatusList;
                                                                                                      try {
                                                                                                          deviceStatusList = future.join();
                                                                                                      } catch (Exception e) {
                                                                                                          ThirdPartyWarningDataSplitSupport.log.warn("并发拉取区划设备状态失败", e);
                                                                                                          continue;
                                                                                                      }
                                                                                                      for (ThirdPartyMonitorDeviceVo v : deviceStatusList) {
                                                                                                          if (v == null) {
                                                                                                              continue;
                                                                                                          }
                                                                                                          if (StrUtil.isNotBlank(v.getId())) {
                                                                                                              byId.putIfAbsent(v.getId(), v);
                                                                                                          }
                                                                                                          if (StrUtil.isNotBlank(v.getClientId())) {
                                                                                                              byClientId.putIfAbsent(v.getClientId(), v);
                                                                                                          }
                                                                                                      }
                                                                                                  }
                                                                                                  return new DeviceStatusFetchResult(byId, byClientId);
                                                                                              })
                                                                                              .completeOnTimeout(new DeviceStatusFetchResult(new HashMap<String, ThirdPartyMonitorDeviceVo>(), new HashMap<String, ThirdPartyMonitorDeviceVo>()), ThirdPartyWarningDataSplitSupport.RUNTIME_STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                                                                              .exceptionally(e -> {
                                                                                                  ThirdPartyWarningDataSplitSupport.log.warn("并发拉取三方设备状态异常，已降级为空结果", e);
                                                                                                  return new DeviceStatusFetchResult(new HashMap<String, ThirdPartyMonitorDeviceVo>(), new HashMap<String, ThirdPartyMonitorDeviceVo>());
                                                                                              });

        CompletableFuture<WarningFetchResult> warningStageFuture = CompletableFuture.supplyAsync(() -> {
                                                                                        List<ThirdMonitorDeviceWarningResp> allWarnings = new ArrayList<ThirdMonitorDeviceWarningResp>();
                                                                                        if (!jcdIds.isEmpty()) {
                                                                                            AtomicBoolean stopWarningFetch = new AtomicBoolean(false);
                                                                                            int maxInFlight = Math.min(ThirdPartyWarningDataSplitSupport.RUNTIME_WARNING_MAX_IN_FLIGHT, jcdIds.size());
                                                                                            ExecutorCompletionService<JcdWarningFetchResult> completionService = new ExecutorCompletionService<JcdWarningFetchResult>(ThirdPartyWarningDataSplitSupport.WARNING_LIST_EXECUTOR);
                                                                                            List<Future<JcdWarningFetchResult>> submittedFutures = new ArrayList<Future<JcdWarningFetchResult>>();
                                                                                            int submitIndex = 0;
                                                                                            int submitted = 0;
                                                                                            int completed = 0;
                                                                                            while (submitIndex < maxInFlight) {
                                                                                                String jcdId = jcdIds.get(submitIndex++);
                                                                                                submittedFutures.add(completionService.submit(() -> thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().fetchWarningsForJcdId(jcdId, stopWarningFetch::get)));
                                                                                                submitted++;
                                                                                            }
                                                                                            while (completed < submitted && !stopWarningFetch.get()) {
                                                                                                try {
                                                                                                    Future<JcdWarningFetchResult> future = completionService.take();
                                                                                                    JcdWarningFetchResult fetchResult = future.get();
                                                                                                    allWarnings.addAll(fetchResult.warnings());
                                                                                                } catch (InterruptedException e) {
                                                                                                    Thread.currentThread().interrupt();
                                                                                                    ThirdPartyWarningDataSplitSupport.log.warn("滑动窗口获取监测点预警任务被中断，终止后续请求", e);
                                                                                                    break;
                                                                                                } catch (Exception e) {
                                                                                                    if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(e)) {
                                                                                                        stopWarningFetch.set(true);
                                                                                                        for (Future<JcdWarningFetchResult> submittedFuture : submittedFutures) {
                                                                                                            if (submittedFuture != null && !submittedFuture.isDone()) {
                                                                                                                submittedFuture.cancel(true);
                                                                                                            }
                                                                                                        }
                                                                                                        ThirdPartyWarningDataSplitSupport.log.warn("三方监测预警凭证失效，终止后续监测点预警请求, completed={}, submitted={}, total={}, error={}",
                                                                                                                completed, submitted, jcdIds.size(), e.getMessage());
                                                                                                        break;
                                                                                                    }
                                                                                                    ThirdPartyWarningDataSplitSupport.log.warn("滑动窗口获取监测点预警任务失败，降级为空结果", e);
                                                                                                }
                                                                                                completed++;
                                                                                                if (!stopWarningFetch.get() && submitIndex < jcdIds.size()) {
                                                                                                    String jcdId = jcdIds.get(submitIndex++);
                                                                                                    submittedFutures.add(completionService.submit(() -> thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().fetchWarningsForJcdId(jcdId, stopWarningFetch::get)));
                                                                                                    submitted++;
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        return new WarningFetchResult(allWarnings);
                                                                                    })
                                                                                    .completeOnTimeout(new WarningFetchResult(new ArrayList<ThirdMonitorDeviceWarningResp>()), ThirdPartyWarningDataSplitSupport.RUNTIME_STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                                                                    .exceptionally(e -> {
                                                                                        ThirdPartyWarningDataSplitSupport.log.warn("并发拉取三方预警异常，已降级为空结果", e);
                                                                                        return new WarningFetchResult(new ArrayList<ThirdMonitorDeviceWarningResp>());
                                                                                    });

        DeviceStatusFetchResult deviceStatusFetchResult = deviceStatusStageFuture.join();
        Map<String, ThirdPartyMonitorDeviceVo> byId = deviceStatusFetchResult.byId();
        Map<String, ThirdPartyMonitorDeviceVo> byClientId = deviceStatusFetchResult.byClientId();

        WarningFetchResult warningFetchResult = warningStageFuture.join();
        List<ThirdMonitorDeviceWarningResp> allWarnings = warningFetchResult.allWarnings();
        Map<String, DeviceLatestWarningInfo> latestWarningBySbid = thirdPartyWarningDataSplitSupport.mergeLatestWarningBySbid(allWarnings);
        List<MonitorDeviceRuntimeVo> result = new ArrayList<MonitorDeviceRuntimeVo>();
        for (MonitorDevice d : devices) {
            result.add(thirdPartyWarningDataSplitSupport.toMonitorDeviceRuntimeVo(d, latestWarningBySbid.get(d.getId()), byId, byClientId));
        }
        return result;
    }
}
