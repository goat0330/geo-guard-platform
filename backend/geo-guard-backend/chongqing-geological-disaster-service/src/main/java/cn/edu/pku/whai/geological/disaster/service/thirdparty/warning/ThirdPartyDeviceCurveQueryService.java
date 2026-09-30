/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureDirectBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorDeviceMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyDeviceCurveQueryService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.ThirdPartyCurveTypeEnum;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ThirdPartyDeviceCurveQueryService implements IThirdPartyDeviceCurveQueryService {

    private static final DateTimeFormatter DEVICE_CURE_DIRECT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DEVICE_CURE_DIRECT_UTC_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
    private static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final MonitorDeviceMapper monitorDeviceMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ThirdPartyWarningRemoteSupport remoteSupport;

    @Override
    public ThirdPartyDeviceCureVo queryDeviceCure(ThirdPartyDeviceCureBo bo) {
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

        String sql = """
            SELECT source_update_time, curve_columns_json, point_values_json
            FROM data_tp_device_curve_hourly
            WHERE client_id = :clientId
              AND monitor_type = :monitorType
              AND source_update_time >= :startTime
              AND source_update_time <= :endTime
            ORDER BY source_update_time
            """;
        List<StoredCurveRow> storedRows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) ->
            new StoredCurveRow(
                rs.getTimestamp("source_update_time"),
                rs.getString("curve_columns_json"),
                rs.getString("point_values_json")
            )
        );
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = resolveLocalCurveColumns(queryType, storedRows);
        vo.setColumns(columns);

        List<List<Object>> values = new ArrayList<>();
        for (StoredCurveRow storedRow : storedRows) {
            List<Object> row = new ArrayList<>();
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

    @Override
    public ThirdPartyWarningDataResp queryDeviceCureDirect(ThirdPartyDeviceCureDirectBo bo) {
        if (bo == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (StrUtil.isBlank(bo.getMonitorType())) {
            throw new ServiceException("monitor_type不能为空");
        }
        if (StrUtil.isBlank(bo.getClientId())) {
            throw new ServiceException("client_id不能为空");
        }
        if (StrUtil.isBlank(bo.getStartTime())) {
            throw new ServiceException("起始时间不能为空");
        }
        if (StrUtil.isBlank(bo.getEndTime())) {
            throw new ServiceException("结束时间不能为空");
        }

        ThirdPartyDeviceCureBo queryBo = new ThirdPartyDeviceCureBo();
        queryBo.setClientId(bo.getClientId());
        queryBo.setType(extractCurveQueryType(bo.getMonitorType()));
        queryBo.setStartTime(parseDirectDeviceCureTime(bo.getStartTime(), "开始时间"));
        queryBo.setEndTime(parseDirectDeviceCureTime(bo.getEndTime(), "结束时间"));
        validateDeviceCureParams(queryBo);
        Map<String, Object> params = buildDeviceCureParams(queryBo);
        String requestContext = JSONUtil.toJsonStr(params);
        try {
            ThirdPartyWarningDataResp resp = requestDeviceCureResp(params, requestContext);
            if (resp == null) {
                return buildFailedDirectDeviceCureResp("三方接口返回为空或非JSON响应");
            }
            normalizeDirectDeviceCureResp(resp);
            return resp;
        } catch (Exception e) {
            log.error("直接调用设备监测曲线接口失败, req={}, err={}", requestContext, e.getMessage());
            return buildFailedDirectDeviceCureResp("直接调用设备监测曲线接口失败，请稍后重试");
        }
    }

    @Override
    public ThirdPartyDeviceCureVo queryDeviceCureFromRemote(ThirdPartyDeviceCureBo bo) {
        validateDeviceCureParams(bo);
        Map<String, Object> params = buildDeviceCureParams(bo);
        String requestContext = JSONUtil.toJsonStr(params);
        try {
            ThirdPartyWarningDataResp resp = requestDeviceCureResp(params, requestContext);
            if (resp == null) {
                return buildEmptyDeviceCureVo(bo);
            }
            if (!resp.isSuccess() || resp.getData() == null) {
                throw remoteSupport.buildThirdPartyException("获取设备监测曲线失败", resp);
            }
            JSONObject data = toJsonObject(resp.getData());
            if (data == null) {
                log.warn("三方设备曲线data格式无法解析，按空结果处理，req={}, rawData={}",
                    requestContext,
                    rawValueForLog(resp.getData())
                );
                return buildEmptyDeviceCureVo(bo);
            }
            return buildThirdPartyDeviceCureVo(data, bo);
        } catch (Exception e) {
            log.error("获取设备监测曲线失败, req={}, err={}", requestContext, e.getMessage());
            return buildEmptyDeviceCureVo(bo);
        }
    }

    private ThirdPartyWarningDataResp requestDeviceCureResp(Map<String, Object> params, String requestContext) {
        String rawBody = Req.post(remoteSupport.getMonitorWarningUrl())
                            .path("/dzapi/device/queryDeviceCure")
                            .header("sso", remoteSupport.getToken())
                            .json(params)
                            .timeout(Duration.ofSeconds(10))
                            .ok()
                            .str();
        return parseDeviceCureResp(rawBody, requestContext);
    }

    private ThirdPartyWarningDataResp parseDeviceCureResp(String rawBody, String requestContext) {
        if (StrUtil.isBlank(rawBody)) {
            log.warn("三方设备曲线接口返回空响应，按空结果处理，req={}", requestContext);
            return null;
        }
        String text = StrUtil.trim(rawBody);
        if (!text.startsWith("{")) {
            log.warn("三方设备曲线接口返回非JSON，按空结果处理，req={}, raw={}",
                requestContext,
                rawTextForLog(text)
            );
            return null;
        }
        try {
            return JSONUtil.toBean(JSONUtil.parseObj(text), ThirdPartyWarningDataResp.class);
        } catch (Exception ex) {
            log.warn("三方设备曲线接口响应JSON解析失败，按空结果处理，req={}, raw={}, err={}",
                requestContext,
                rawTextForLog(text),
                ex.getMessage()
            );
            return null;
        }
    }

    private void validateDeviceCureParams(ThirdPartyDeviceCureBo bo) {
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

    private MonitorDevice findMonitorDeviceByClientId(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return null;
        }
        LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery();
        lqw.eq(MonitorDevice::getClientId, clientId);
        lqw.last("limit 1");
        List<MonitorDevice> devices = monitorDeviceMapper.selectList(lqw);
        return devices == null || devices.isEmpty() ? null : devices.getFirst();
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
        deviceInfo.setMonitorSerialNo(firstNonBlank(device.getDeviceBusinessCode(), device.getDeviceSerialNumber()));
        return deviceInfo;
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

    private String extractCurveQueryType(String type) {
        if (StrUtil.isBlank(type)) {
            return type;
        }
        String normalized = StrUtil.trim(type).toUpperCase();
        return normalized.contains("_") ? StrUtil.subAfter(normalized, "_", true) : normalized;
    }

    private String normalizeCurveStorageMonitorType(String type) {
        return extractCurveQueryType(type);
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
            log.warn("解析本地曲线列定义失败，err={}", ex.getMessage());
            return List.of();
        }
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

    private ThirdPartyDeviceCureVo.ColumnInfo buildColumn(String code, String name, String unit) {
        ThirdPartyDeviceCureVo.ColumnInfo column = new ThirdPartyDeviceCureVo.ColumnInfo();
        column.setCode(code);
        column.setName(name);
        column.setUnit(unit);
        return column;
    }

    private Map<String, Object> parseStoredCurveValueMap(String pointValuesJson) {
        if (StrUtil.isBlank(pointValuesJson)) {
            return Map.of();
        }
        try {
            JSONObject jsonObject = JSONUtil.parseObj(pointValuesJson);
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : jsonObject.keySet()) {
                values.put(key, jsonObject.get(key));
            }
            return values;
        } catch (Exception ex) {
            log.warn("解析本地曲线点值失败，err={}", ex.getMessage());
            return Map.of();
        }
    }

    private Object resolveStoredCurveValue(Map<String, Object> pointValues, ThirdPartyDeviceCureVo.ColumnInfo column) {
        String code = StrUtil.blankToDefault(column.getCode(), "");
        Object storedValue = getIgnoreCase(pointValues, code);
        return storedValue != null || pointValues.containsKey(code) ? storedValue : null;
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

    private Timestamp toRawLocalTimestamp(Long tsMillis) {
        if (tsMillis == null) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMillis), ZoneOffset.UTC));
    }

    /**
     * 将本地表中的时间按北京时间换算为接口返回的毫秒时间戳。
     */
    private Long toRawLocalEpochMillis(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
    }

    private Long parseDirectDeviceCureTime(String value, String fieldName) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(StrUtil.trim(value), DEVICE_CURE_DIRECT_TIME_FORMATTER);
            return dateTime.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new ServiceException(fieldName + "格式错误，应为yyyyMMddHHmmss");
        }
    }

    private void normalizeDirectDeviceCureResp(ThirdPartyWarningDataResp resp) {
        if (resp == null || resp.getData() == null) {
            return;
        }
        JSONObject data = JSONUtil.parseObj(resp.getData());
        JSONArray valuesArray = parseFlexibleJsonArray(data.get("values"), "values");
        if (valuesArray == null) {
            resp.setData(data);
            return;
        }
        for (int i = 0; i < valuesArray.size(); i++) {
            JSONArray rowArray = parseFlexibleJsonArray(valuesArray.get(i), "values[" + i + "]");
            if (rowArray == null || rowArray.isEmpty()) {
                continue;
            }
            Long rawEpochMillis = parseLongValue(rowArray.getFirst());
            if (rawEpochMillis == null) {
                continue;
            }
            rowArray.set(0, formatDirectDeviceCureTime(rawEpochMillis));
            valuesArray.set(i, rowArray);
        }
        data.set("values", valuesArray);
        resp.setData(data);
    }

    private String formatDirectDeviceCureTime(Long rawEpochMillis) {
        if (rawEpochMillis == null) {
            return null;
        }
        return DEVICE_CURE_DIRECT_UTC_DISPLAY_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(rawEpochMillis), ZoneOffset.UTC));
    }

    private Map<String, Object> buildDeviceCureParams(ThirdPartyDeviceCureBo bo) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("type", bo.getType());
        params.put("clientID", bo.getClientId());
        params.put("startTime", bo.getStartTime());
        params.put("endTime", bo.getEndTime());
        return params;
    }

    private ThirdPartyWarningDataResp buildFailedDirectDeviceCureResp(String message) {
        ThirdPartyWarningDataResp resp = new ThirdPartyWarningDataResp();
        resp.setStatus("500");
        resp.setMessage(message);
        return resp;
    }

    private ThirdPartyDeviceCureVo buildEmptyDeviceCureVo(ThirdPartyDeviceCureBo bo) {
        ThirdPartyDeviceCureVo vo = new ThirdPartyDeviceCureVo();
        String queryType = extractCurveQueryType(bo.getType());
        MonitorDevice device = findMonitorDeviceByClientId(bo.getClientId());
        vo.setDeviceInfo(buildLocalDeviceInfo(device, bo, queryType));
        vo.setColumns(buildLocalDeviceCureColumns(queryType));
        vo.setValues(List.of());
        return vo;
    }

    private ThirdPartyDeviceCureVo buildThirdPartyDeviceCureVo(JSONObject data, ThirdPartyDeviceCureBo bo) {
        if (data == null) {
            return buildEmptyDeviceCureVo(bo);
        }
        ThirdPartyDeviceCureVo vo = new ThirdPartyDeviceCureVo();
        String queryType = extractCurveQueryType(bo.getType());
        JSONObject deviceInfoObj = data.getJSONObject("deviceInfo");
        if (deviceInfoObj != null) {
            ThirdPartyDeviceCureVo.DeviceInfo deviceInfo = new ThirdPartyDeviceCureVo.DeviceInfo();
            deviceInfo.setClientId(deviceInfoObj.getStr("clientid"));
            deviceInfo.setMonitorPointId(deviceInfoObj.getStr("jcdid"));
            deviceInfo.setMonitorMethod(deviceInfoObj.getStr("jcff"));
            String monitorType = normalizeCurveStorageMonitorType(deviceInfoObj.getStr("jctype"));
            deviceInfo.setMonitorType(monitorType);
            deviceInfo.setDicName(ThirdPartyCurveTypeEnum.resolveDicName(monitorType));
            deviceInfo.setSensorName(deviceInfoObj.getStr("cgqname"));
            deviceInfo.setMonitorSerialNo(deviceInfoObj.getStr("cgqxh"));
            vo.setDeviceInfo(deviceInfo);
        }
        if (vo.getDeviceInfo() == null) {
            MonitorDevice device = findMonitorDeviceByClientId(bo.getClientId());
            vo.setDeviceInfo(buildLocalDeviceInfo(device, bo, queryType));
        }
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = parseThirdPartyColumns(parseFlexibleJsonArray(data.get("columns"), "columns"));
        if (columns.isEmpty()) {
            columns = buildLocalDeviceCureColumns(queryType);
        }
        vo.setColumns(columns);
        String requestContext = JSONUtil.toJsonStr(buildDeviceCureParams(bo));
        vo.setValues(parseThirdPartyValues(parseFlexibleJsonArray(data.get("values"), "values", requestContext), requestContext));
        return vo;
    }

    private List<ThirdPartyDeviceCureVo.ColumnInfo> parseThirdPartyColumns(JSONArray columnsArray) {
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = new ArrayList<>();
        if (columnsArray == null) {
            return columns;
        }
        for (int i = 0; i < columnsArray.size(); i++) {
            Object rawColumn = columnsArray.get(i);
            JSONObject columnObj = toJsonObject(rawColumn);
            if (columnObj == null) {
                String code = StrUtil.trim(String.valueOf(rawColumn));
                if (StrUtil.isNotBlank(code)) {
                    columns.add(buildColumn(code, code, ""));
                }
                continue;
            }
            columns.add(buildColumn(columnObj.getStr("code"), columnObj.getStr("name"), columnObj.getStr("unit")));
        }
        return columns;
    }

    private List<List<Object>> parseThirdPartyValues(JSONArray valuesArray, String requestContext) {
        List<List<Object>> values = new ArrayList<>();
        if (valuesArray == null) {
            return values;
        }
        for (int i = 0; i < valuesArray.size(); i++) {
            JSONArray rowArray = parseFlexibleJsonArray(valuesArray.get(i), "values[" + i + "]", requestContext);
            if (rowArray == null) {
                continue;
            }
            List<Object> row = new ArrayList<>(rowArray);
            values.add(row);
        }
        return values;
    }

    private JSONArray parseFlexibleJsonArray(Object rawValue, String fieldName) {
        return parseFlexibleJsonArray(rawValue, fieldName, null);
    }

    private JSONArray parseFlexibleJsonArray(Object rawValue, String fieldName, String requestContext) {
        if (rawValue == null) {
            logMalformedValuesIfNeeded(fieldName, rawValue, requestContext);
            return null;
        }
        if (rawValue instanceof JSONArray array) {
            return array;
        }
        if (rawValue instanceof Collection<?> collection) {
            return JSONUtil.parseArray(collection);
        }
        if (rawValue instanceof CharSequence sequence) {
            String text = StrUtil.trim(sequence.toString());
            if (StrUtil.isBlank(text) || "null".equalsIgnoreCase(text)) {
                logMalformedValuesIfNeeded(fieldName, sequence.toString(), requestContext);
                return null;
            }
            if (!text.startsWith("[")) {
                logMalformedCurveArray(fieldName, text, requestContext);
                return null;
            }
            try {
                return JSONUtil.parseArray(text);
            } catch (Exception ex) {
                logMalformedCurveArray(fieldName, text, requestContext);
                return null;
            }
        }
        try {
            return JSONUtil.parseArray(rawValue);
        } catch (Exception ex) {
            logMalformedCurveArray(fieldName, rawValue, requestContext);
            return null;
        }
    }

    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return parseDateTimeMillis(String.valueOf(value));
        }
    }

    private Long parseDateTimeMillis(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(text, DEVICE_CURE_DIRECT_TIME_FORMATTER);
            return dateTime.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.atZone(BEIJING_ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private JSONObject toJsonObject(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof JSONObject object) {
            return object;
        }
        if (rawValue instanceof Map<?, ?> map) {
            return JSONUtil.parseObj(map);
        }
        if (rawValue instanceof CharSequence sequence) {
            String text = StrUtil.trim(sequence.toString());
            if (StrUtil.isBlank(text) || !text.startsWith("{")) {
                return null;
            }
            try {
                return JSONUtil.parseObj(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String abbreviateRawValue(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        String text = String.valueOf(rawValue).replace('\r', ' ').replace('\n', ' ');
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

    private void logMalformedValuesIfNeeded(String fieldName, Object rawValue, String requestContext) {
        if (isValuesField(fieldName)) {
            log.warn("三方设备曲线{}为空或null，按空数组处理，req={}, rawValues={}",
                fieldName,
                StrUtil.blankToDefault(requestContext, "-"),
                rawValueForLog(rawValue)
            );
        }
    }

    private void logMalformedCurveArray(String fieldName, Object rawValue, String requestContext) {
        if (isValuesField(fieldName)) {
            log.warn("三方设备曲线{}格式无法解析，按空数组处理，req={}, rawValues={}",
                fieldName,
                StrUtil.blankToDefault(requestContext, "-"),
                rawValueForLog(rawValue)
            );
            return;
        }
        log.warn("三方设备曲线{}格式无法解析，按空数组处理，raw={}", fieldName, abbreviateRawValue(rawValue));
    }

    private boolean isValuesField(String fieldName) {
        return StrUtil.startWith(fieldName, "values");
    }

    private String rawValueForLog(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        if (rawValue instanceof CharSequence sequence) {
            return quoteForLog(sequence.toString());
        }
        try {
            return JSONUtil.toJsonStr(rawValue);
        } catch (Exception ignored) {
            return String.valueOf(rawValue);
        }
    }

    private String rawTextForLog(String rawText) {
        return quoteForLog(abbreviateRawValue(rawText));
    }

    private String quoteForLog(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n") + "\"";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record StoredCurveRow(
        Timestamp sourceUpdateTime,
        String columnsJson,
        String pointValuesJson
    ) {
    }
}
