/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.StatThirdWarningBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.UserInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DeviceRainfallDisplacementVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LiveRainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningStatusVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningTypeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.ThirdPartyCurveTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DisplacementPoint;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.GridLocation;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorWarningDataResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningPageResp;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.kongweiguang.http.client.Req;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class StatsMiscDelegate {
    private final ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport;

    public StatsMiscDelegate(ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport) {
        this.thirdPartyWarningDataSplitSupport = thirdPartyWarningDataSplitSupport;
    }

    /**
     * 根据设备ID获取雨量与位移统计
     */
    DeviceRainfallDisplacementVo getDeviceRainfallAndDisplacement(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            throw new ServiceException("设备id不能为空");
        }
        MonitorDevice device = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        Double lon = toDouble(device.getLongitude());
        Double lat = toDouble(device.getLatitude());
        if (lon == null || lat == null) {
            throw new ServiceException("设备经纬度为空，无法计算雨量");
        }

        DeviceRainfallDisplacementVo vo = new DeviceRainfallDisplacementVo();
        fillRainfallStats(vo, lon, lat);
        fillDisplacementStats(vo, device);
        return vo;
    }

    /**
     * 填充最近1小时/24小时/7天累计雨量
     */
    private void fillRainfallStats(DeviceRainfallDisplacementVo vo, double deviceLon, double deviceLat) {
        GridLocation nearestGrid = resolveNearestRainfallGrid(deviceLon, deviceLat);
        if (nearestGrid == null) {
            vo.setLast1HourRainfall(0D);
            vo.setLast24HoursRainfall(0D);
            vo.setLast7DaysRainfall(0D);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start7Days = now.minusDays(7);
        LambdaQueryWrapper<RainfallLogRasterUnit> lqw = Wrappers.lambdaQuery();
        lqw.eq(RainfallLogRasterUnit::getLon, nearestGrid.lon())
           .eq(RainfallLogRasterUnit::getLat, nearestGrid.lat())
           .ge(RainfallLogRasterUnit::getLogTime, DateUtil.date(start7Days))
           .le(RainfallLogRasterUnit::getLogTime, DateUtil.date(now))
           .orderByAsc(RainfallLogRasterUnit::getLogTime);
        List<RainfallLogRasterUnit> units = thirdPartyWarningDataSplitSupport.getRainfallCellMapper().selectList(lqw);
        double sum1h = 0D;
        double sum24h = 0D;
        double sum7d = 0D;
        LocalDateTime start1h = now.minusHours(1);
        LocalDateTime start24h = now.minusHours(24);
        for (RainfallLogRasterUnit unit : units) {
            LocalDateTime logTime = DateUtil.toLocalDateTime(unit.getLogTime());
            if (logTime == null) {
                continue;
            }
            double rainfall = unit.getRainfall() == null ? 0D : unit.getRainfall();
            if (!logTime.isBefore(start7Days)) {
                sum7d += rainfall;
            }
            if (!logTime.isBefore(start24h)) {
                sum24h += rainfall;
            }
            if (!logTime.isBefore(start1h)) {
                sum1h += rainfall;
            }
        }
        vo.setLast1HourRainfall(roundTo3(sum1h));
        vo.setLast24HoursRainfall(roundTo3(sum24h));
        vo.setLast7DaysRainfall(roundTo3(sum7d));
    }

    /**
     * 填充位移统计，仅对包含L1_GP的设备计算
     */
    private void fillDisplacementStats(DeviceRainfallDisplacementVo vo, MonitorDevice device) {
        if (!containsMonitorType(device.getMonitoringType()) || StrUtil.isBlank(device.getClientId())) {
            vo.setLast1DayDisplacement(ThirdPartyWarningDataSplitSupport.DISPLACEMENT_UNAVAILABLE);
            vo.setLast3DaysDisplacement(ThirdPartyWarningDataSplitSupport.DISPLACEMENT_UNAVAILABLE);
            vo.setLast7DaysDisplacement(ThirdPartyWarningDataSplitSupport.DISPLACEMENT_UNAVAILABLE);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start7Days = now.minusDays(7);
        List<DisplacementPoint> rangePoints = fetchDisplacementPoints(device.getClientId(), start7Days, now);
        vo.setLast1DayDisplacement(calculateRangeDisplacement(rangePoints, now.minusDays(1), now));
        vo.setLast3DaysDisplacement(calculateRangeDisplacement(rangePoints, now.minusDays(3), now));
        vo.setLast7DaysDisplacement(calculateRangeDisplacement(rangePoints, start7Days, now));
    }

    /**
     * 查询时间区间内的位移点，按时间升序返回
     */
    private List<DisplacementPoint> fetchDisplacementPoints(String clientId, LocalDateTime start, LocalDateTime end) {
        ThirdPartyDeviceCureBo bo = new ThirdPartyDeviceCureBo();
        bo.setType("GP");
        bo.setClientId(clientId);
        bo.setStartTime(DateUtil.date(start).getTime());
        bo.setEndTime(DateUtil.date(end).getTime());
        try {
            ThirdPartyDeviceCureVo cureVo = thirdPartyWarningDataSplitSupport.getDeviceDataDelegate().queryDeviceCure(bo);
            return parseDisplacementPoints(cureVo);
        } catch (Exception ex) {
            ThirdPartyWarningDataSplitSupport.log.warn("获取位移区间数据失败，clientId={}, start={}, end={}, err={}",
                clientId, start, end, ex.getMessage());
            return List.of();
        }
    }

    /**
     * 计算时间区间内首尾两次记录的位移值
     */
    private Double calculateRangeDisplacement(List<DisplacementPoint> allPoints, LocalDateTime start, LocalDateTime end) {
        if (allPoints == null || allPoints.isEmpty()) {
            return ThirdPartyWarningDataSplitSupport.DISPLACEMENT_UNAVAILABLE;
        }
        List<DisplacementPoint> points = allPoints.stream()
                                                  .filter(point -> !point.time().isBefore(start) && !point.time().isAfter(end))
                                                  .toList();
        if (points.size() < 2) {
            return ThirdPartyWarningDataSplitSupport.DISPLACEMENT_UNAVAILABLE;
        }
        return calculatePairDisplacement(points.getFirst(), points.getLast());
    }

    /**
     * 查询与设备最近的降雨栅格点
     */
    private GridLocation resolveNearestRainfallGrid(double deviceLon, double deviceLat) {
        List<RainfallEntityGridMapping> mappings = thirdPartyWarningDataSplitSupport.getRainfallEntityGridMappingMapper().selectList(Wrappers.lambdaQuery());
        double minDistance = Double.MAX_VALUE;
        GridLocation nearest = null;
        for (RainfallEntityGridMapping mapping : mappings) {
            if (mapping.getGridLon() == null || mapping.getGridLat() == null) {
                continue;
            }
            double distance = GeoDistanceUtil.distanceMeters(deviceLon, deviceLat, mapping.getGridLon(), mapping.getGridLat());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = new GridLocation(mapping.getGridLon(), mapping.getGridLat());
            }
        }
        return nearest;
    }

    /**
     * 解析曲线值列表，提取timestamp/x/y/z
     */
    private List<DisplacementPoint> parseDisplacementPoints(ThirdPartyDeviceCureVo cureVo) {
        if (cureVo == null || cureVo.getValues() == null || cureVo.getValues().isEmpty()) {
            return List.of();
        }
        int xIdx = -1;
        int yIdx = -1;
        int zIdx = -1;
        List<ThirdPartyDeviceCureVo.ColumnInfo> columns = cureVo.getColumns();
        if (columns != null) {
            for (int i = 0; i < columns.size(); i++) {
                String code = StrUtil.blankToDefault(columns.get(i).getCode(), "");
                if (isAxisCode(code, "X")) {
                    xIdx = i + 1;
                } else if (isAxisCode(code, "Y")) {
                    yIdx = i + 1;
                } else if (isAxisCode(code, "Z")) {
                    zIdx = i + 1;
                }
            }
        }
        if (xIdx < 1 || yIdx < 1 || zIdx < 1) {
            return List.of();
        }
        List<DisplacementPoint> points = new ArrayList<>();
        for (List<Object> row : cureVo.getValues()) {
            if (row == null || row.size() <= zIdx) {
                continue;
            }
            Long timestamp = parseLongValue(row.getFirst());
            Double x = parseDoubleValue(row.get(xIdx));
            Double y = parseDoubleValue(row.get(yIdx));
            Double z = parseDoubleValue(row.get(zIdx));
            if (timestamp == null || x == null || y == null || z == null) {
                continue;
            }
            points.add(new DisplacementPoint(
                LocalDateTime.ofInstant(new Date(timestamp).toInstant(), TimeZone.getDefault().toZoneId()),
                timestamp,
                x,
                y,
                z
            ));
        }
        points.sort(Comparator.comparing(DisplacementPoint::time));
        return points;
    }

    /**
     * 判断列编码是否为指定轴向，兼容 gX/gY/gZ 与 gpsTotalX/gpsTotalY/gpsTotalZ
     */
    private boolean isAxisCode(String code, String axis) {
        if (StrUtil.isBlank(code) || StrUtil.isBlank(axis)) {
            return false;
        }
        String normalized = code.trim().toUpperCase();
        String target = axis.trim().toUpperCase();
        return normalized.equals("G" + target)
            || normalized.endsWith(target)
            || normalized.endsWith("_" + target);
    }

    private Double calculatePairDisplacement(DisplacementPoint latest, DisplacementPoint previous) {
        if (latest == null || previous == null) {
            return 0D;
        }
        double dx = latest.x() - previous.x();
        double dy = latest.y() - previous.y();
        double dz = latest.z() - previous.z();
        return Math.max(0D, roundTo3(Math.sqrt(dx * dx + dy * dy + dz * dz)));
    }

    private boolean containsMonitorType(String monitorTypes) {
        if (StrUtil.isBlank(monitorTypes) || StrUtil.isBlank("L1_GP")) {
            return false;
        }
        return Arrays.stream(monitorTypes.split(","))
                     .map(String::trim)
                     .anyMatch("L1_GP"::equalsIgnoreCase);
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private Double parseDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
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
            return null;
        }
    }

    private Double roundTo3(double value) {
        return Math.round(value * 1000D) / 1000D;
    }

    /**
     * 检查三方接口连通性
     */
    Map<String, Object> checkThirdPartyConnectivity() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("monitorBaseUrl", thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl());
        result.put("loginOk", false);
        result.put("devicePageOk", false);
        result.put("warningPageOk", false);
        result.put("deviceCureOk", false);
        try {
            UserInfoResp loginResp = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().loginAuth();
            boolean loginOk = loginResp != null && StrUtil.isNotBlank(loginResp.getCredential());
            result.put("loginOk", loginOk);
            if (!loginOk) {
                result.put("message", "登录失败，未获取到sso凭证");
                return result;
            }
        } catch (Exception ex) {
            result.put("message", "登录接口异常: " + ex.getMessage());
            return result;
        }

        try {
            ThirdPartyWarningDataResp resp = Req.post(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl())
                                                .path("/dzapi/jcd/jcsbxxPage")
                                                .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                .json(Map.of(
                                                        "sbbh", "",
                                                        "sbname", "",
                                                        "jcdname", "",
                                                        "regionCode", "422801000000",
                                                        "sbcs", "",
                                                        "current", 1,
                                                        "size", 1
                                                ))
                                                .timeout(Duration.ofSeconds(10))
                                                .ok()
                                                .obj(ThirdPartyWarningDataResp.class);
            result.put("devicePageOk", resp != null && resp.isSuccess() && resp.getData() != null);
        } catch (Exception ex) {
            result.put("devicePageError", ex.getMessage());
        }

        String sampleJcdId = null;
        try {
            List<MonitorPoint> monitorPoints = thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectList(
                    Wrappers.<MonitorPoint>lambdaQuery()
                            .likeRight(MonitorPoint::getAdministrativeRegionCode, "422801")
                            .last("limit 1")
            );
            if (monitorPoints != null && !monitorPoints.isEmpty()) {
                sampleJcdId = monitorPoints.getFirst().getId();
            }
        } catch (Exception ex) {
            result.put("sampleMonitorPointError", ex.getMessage());
        }

        if (StrUtil.isNotBlank(sampleJcdId)) {
            try {
                ThirdMonitorDeviceWarningPageResp warningPageResp = thirdPartyWarningDataSplitSupport.getParallelFetchDelegate().fetchMonitorDeviceWarningPage(sampleJcdId, null, null, 1);
                result.put("warningPageOk", warningPageResp != null);
            } catch (Exception ex) {
                result.put("warningPageError", ex.getMessage());
            }
        } else {
            result.put("warningPageError", "未找到本地样例监测点，无法检测jcyjPage");
        }

        try {
            MonitorDevice sampleDevice = findSampleCurveDevice();
            String sampleCurveType = resolveSampleCurveType(sampleDevice);
            if (sampleDevice == null || StrUtil.isBlank(sampleCurveType)) {
                result.put("deviceCureError", "未找到可用于检测queryDeviceCure的本地样例设备");
            } else {
                long now = DateUtil.current();
                result.put("deviceCureSampleClientId", sampleDevice.getClientId());
                result.put("deviceCureSampleType", sampleCurveType);
                result.put("deviceCureSampleDeviceId", sampleDevice.getId());
                ThirdPartyWarningDataResp cureResp = Req.post(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl())
                                                        .path("/dzapi/device/queryDeviceCure")
                                                        .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                        .json(Map.of(
                                                                "type", sampleCurveType,
                                                                "clientID", sampleDevice.getClientId(),
                                                                "startTime", now - 3600_000L,
                                                                "endTime", now
                                                        ))
                                                        .timeout(Duration.ofSeconds(10))
                                                        .ok()
                                                        .obj(ThirdPartyWarningDataResp.class);
                result.put("deviceCureOk", cureResp != null && cureResp.isSuccess() && cureResp.getData() != null);
            }
        } catch (Exception ex) {
            result.put("deviceCureError", ex.getMessage());
        }

        boolean allOk = Boolean.TRUE.equals(result.get("loginOk"))
                && Boolean.TRUE.equals(result.get("devicePageOk"))
                && Boolean.TRUE.equals(result.get("warningPageOk"))
                && Boolean.TRUE.equals(result.get("deviceCureOk"));
        result.put("allOk", allOk);
        return result;
    }

    private MonitorDevice findSampleCurveDevice() {
        List<MonitorDevice> devices = thirdPartyWarningDataSplitSupport.getMonitorDeviceMapper().selectList(
            Wrappers.<MonitorDevice>lambdaQuery()
                    .isNotNull(MonitorDevice::getClientId)
                    .ne(MonitorDevice::getClientId, "")
                    .isNotNull(MonitorDevice::getMonitoringType)
                    .ne(MonitorDevice::getMonitoringType, "")
                    .last("limit 100")
        );
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        return devices.stream()
                      .filter(device -> StrUtil.isNotBlank(resolveSampleCurveType(device)))
                      .findFirst()
                      .orElse(null);
    }

    private String resolveSampleCurveType(MonitorDevice device) {
        if (device == null || StrUtil.isBlank(device.getMonitoringType())) {
            return null;
        }
        return Arrays.stream(device.getMonitoringType().split(","))
                     .map(String::trim)
                     .map(this::extractSupportedCurveType)
                     .filter(StrUtil::isNotBlank)
                     .findFirst()
                     .orElse(null);
    }

    private String extractSupportedCurveType(String monitorType) {
        if (StrUtil.isBlank(monitorType)) {
            return null;
        }
        String normalized = monitorType.trim().toUpperCase();
        if (ThirdPartyCurveTypeEnum.containsCode(normalized)) {
            return normalized;
        }
        if (!normalized.contains("_")) {
            return null;
        }
        String[] segments = normalized.split("_");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (ThirdPartyCurveTypeEnum.containsCode(segments[i])) {
                return segments[i];
            }
        }
        return null;
    }

    /**
     * 统计监测预警概览数据
     */
    StatThirdWarningVo statMonitorWarning(StatThirdWarningBo bo) {
        StatThirdWarningVo vo = new StatThirdWarningVo();

        Integer offset = bo.getOffset();
        DateTime now = DateUtil.date();
        DateTime old;
        if (offset == null || offset <= 0) {
            old = DateUtil.beginOfDay(now);
        } else {
            old = DateUtil.endOfDay(DateUtil.offsetDay(now, -offset));
        }

        DateTime end = DateUtil.endOfDay(now);

        RiskWarningRecordBo rwrb = new RiskWarningRecordBo();
        rwrb.setStartTime(DateUtil.formatDateTime(old));
        rwrb.setEndTime(DateUtil.formatDateTime(end));


        MonitorWarningDataResult monitorWarningDataResult = thirdPartyWarningDataSplitSupport.getParallelFetchDelegate().fetchMonitorWarningDataFromLocal(rwrb);


        List<MonitorWarningRecordResp> records = monitorWarningDataResult.getRecords();

        Integer total = monitorWarningDataResult.getTotal();
        List<StatThirdWarningTypeVo.StatThirdWarningTypeItem> list = records.stream()
                                                                            .collect(Collectors.groupingBy(MonitorWarningRecordResp::getWarningLevel, Collectors.counting()))
                                                                            .entrySet()
                                                                            .stream()
                                                                            .sorted(Map.Entry.comparingByKey(ThirdPartyWarningDataSplitSupport::compareStatMonitorWarningLevelKey))
                                                                            .map(e -> new StatThirdWarningTypeVo.StatThirdWarningTypeItem(e.getKey(), e.getValue()))
                                                                            .toList();
        StatThirdWarningTypeVo statThirdWarningTypeVo = new StatThirdWarningTypeVo();
        statThirdWarningTypeVo.setTotal(total);
        statThirdWarningTypeVo.setItems(list);
        vo.setStatThirdWarningTypeVo(statThirdWarningTypeVo);


        List<StatThirdWarningStatusVo.StatThirdWarningStatusItem> list1 = records.stream()
                                                                                 .collect(Collectors.groupingBy(MonitorWarningRecordResp::getWarningProcessStatus, Collectors.counting()))
                                                                                 .entrySet()
                                                                                 .stream()
                                                                                 .sorted(Map.Entry.comparingByKey(ThirdPartyWarningDataSplitSupport::compareStatMonitorProcessStatusKey))
                                                                                 .map(e -> new StatThirdWarningStatusVo.StatThirdWarningStatusItem(e.getKey(), e.getValue()))
                                                                                 .toList();

        StatThirdWarningStatusVo statThirdWarningStatusVo = new StatThirdWarningStatusVo();
        statThirdWarningStatusVo.setTotal(total);
        statThirdWarningStatusVo.setItems(list1);
        vo.setStatThirdWarningStatusVo(statThirdWarningStatusVo);
        return vo;

    }

    /**
     * 获取实时雨情数据
     */
    List<LiveRainVo> getLiveRain(String date) {

        if (StrUtil.isBlank(thirdPartyWarningDataSplitSupport.getToken())) {
            thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().loginAuth();
        }
        LocalDateTime dateTime = LocalDateTime.parse(date, ThirdPartyWarningDataSplitSupport.DATE_TIME_FORMATTER);
        LocalDateTime modifiedTime = dateTime.withMinute(0).withSecond(0);
        Map<String, Object> params = new HashMap<String, Object>();
        String time = modifiedTime.format(ThirdPartyWarningDataSplitSupport.DATE_TIME_FORMATTER);
        ThirdPartyWarningDataSplitSupport.log.info("获取雨量数据: {}", time);
        params.put("time", time);
        List<LiveRainVo> liveRainVos = new ArrayList<LiveRainVo>();
        try {

            ThirdPartyWarningDataResp resp = Req.post(thirdPartyWarningDataSplitSupport.getWarningDataProps().getUrl())
                                                .path("/qxfxyj/api/qxfxyj/rain/statistic/getLiveRain")
                                                .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                .json(params)
                                                .timeout(Duration.ofMinutes(3))
                                                .ok().obj(ThirdPartyWarningDataResp.class);
            if (ObjectUtil.isNull(resp)) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警数据失败: 响应为空");
                return buildDefaultLiveRainVos();
            }

            Object data = resp.getData();
            if (ObjectUtil.isNull(data)) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警数据失败: 响应数据为空");
                return buildDefaultLiveRainVos();
            }

            List<String> filters = Arrays.asList("恩施", "利川", "建始", "巴东", "宣恩", "鹤峰", "来凤", "来风", "咸丰");
            List<String> decides = Arrays.asList("恩施", "利川");
            JSONObject dataObj = JSONUtil.parseObj(data);
            JSONArray features = dataObj.getJSONArray("features");
            if (ObjectUtil.isNull(features)) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警数据失败: 响应数据features为空");
                return buildDefaultLiveRainVos();
            }
            for (int i = 0; i < features.size(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String name = properties.getStr("name");
                for (String filter : filters) {
                    if (filter.equals(name)) {
                        LiveRainVo liveRainVo = new LiveRainVo();
                        JSONArray coordArray = feature.getJSONObject("geometry").getJSONArray("coordinates");
                        List<Double> coordinates = new ArrayList<Double>();
                        for (int j = 0; j < coordArray.size(); j++) {
                            coordinates.add(coordArray.getDouble(j));
                        }
                        liveRainVo.setCoordinates(coordinates);
                        Double value = properties.getDouble("001");
                        liveRainVo.setValue(value);
                        if ("来风".equals(name)) {
                            name = "来凤";
                        }
                        if (decides.contains(name)) {
                            liveRainVo.setName(name + "市");
                        } else {
                            liveRainVo.setName(name + "县");
                        }
                        liveRainVos.add(liveRainVo);
                    }
                }
            }
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.error("获取监测预警数据失败: {}", e.getMessage());
            return buildDefaultLiveRainVos();
        }
        if (liveRainVos.isEmpty()) {
            return buildDefaultLiveRainVos();
        }
        return liveRainVos;
    }

    /**
     * 实时雨情无数据时返回默认行政区划点位
     */
    private List<LiveRainVo> buildDefaultLiveRainVos() {
        List<LiveRainVo> defaultList = new ArrayList<>();
        defaultList.add(buildLiveRainVo("恩施市", 109.466667, 30.283333, 0.0));
        defaultList.add(buildLiveRainVo("利川市", 108.933334, 30.283333, 0.0));
        defaultList.add(buildLiveRainVo("建始县", 109.716667, 30.6, 0.0));
        defaultList.add(buildLiveRainVo("巴东县", 110.400002, 31.066668, 0.0));
        defaultList.add(buildLiveRainVo("宣恩县", 109.48333, 30.0, 0.0));
        defaultList.add(buildLiveRainVo("鹤峰县", 110.033333, 29.9, 0.0));
        defaultList.add(buildLiveRainVo("来凤县", 109.416664, 29.516666, 0.0));
        defaultList.add(buildLiveRainVo("咸丰县", 109.150002, 29.683332, 0.0));
        return defaultList;
    }

    private LiveRainVo buildLiveRainVo(String name, Double lon, Double lat, Double value) {
        LiveRainVo vo = new LiveRainVo();
        vo.setName(name);
        vo.setCoordinates(Arrays.asList(lon, lat));
        vo.setValue(value);
        return vo;
    }
}
