/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallForecastRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallForecastDailyStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallCellMapper;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.GridLocation;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.ParsedTime;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.TimeQueryRange;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallForecastRasterUnitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 降雨统计辅助工具：时间解析、网格定位、序列构建、区划查询。
 * <p>从 RainfallLogSlopeUnitStatisticServiceImpl 提取以减少主类行数。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RainfallStatisticHelper {

    static final int ACTUAL_MAX_DAYS = 7;
    static final int FORECAST_DAYS = 3;
    static final int RAINFALL_PERIOD_HOUR = 8;
    static final String SOURCE_HISTORY = "history";
    static final String SOURCE_FORECAST_FALLBACK = "forecast_fallback";
    static final String SOURCE_MISSING = "missing";
    static final String SERIES_SOURCE_ACTUAL = "actual";
    static final String SERIES_SOURCE_FORECAST = "forecast";
    static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter HOUR_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    static final DateTimeFormatter HOUR_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .optionalStart().appendLiteral(' ').appendPattern("HH:mm")
        .optionalStart().appendLiteral(':').appendPattern("ss").optionalEnd()
        .optionalStart().appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
        .optionalEnd().toFormatter();

    private final RainfallCellMapper rainfallCellMapper;
    private final RainfallForecastRasterUnitMapper rainfallForecastRasterUnitMapper;

    // ==================== 时间工具 ====================

    LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    double defaultValue(Double value) { return value == null ? 0D : value; }

    double roundRainfall(double rainfall) { return Math.round(rainfall * 100D) / 100D; }

    void validateDateRange(RainfallLogSlopeUnitStatisticQueryBo bo) {
        if (bo.getStartDate() == null || bo.getEndDate() == null)
            throw new ServiceException("起止日期不能为空");
        if (bo.getStartDate().isAfter(bo.getEndDate()))
            throw new ServiceException("开始日期不能晚于结束日期");
    }

    boolean hasNoAdRegionCondition(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return StringUtils.isBlank(bo.getProvince()) && StringUtils.isBlank(bo.getCity())
            && StringUtils.isBlank(bo.getCounty()) && StringUtils.isBlank(bo.getStreet())
            && StringUtils.isBlank(bo.getVillage()) && StringUtils.isBlank(bo.getProvinceCode())
            && StringUtils.isBlank(bo.getCityCode()) && StringUtils.isBlank(bo.getCountyCode())
            && StringUtils.isBlank(bo.getStreetCode()) && StringUtils.isBlank(bo.getVillageCode());
    }

    TimeQueryRange resolveTimeQueryRange(RainfallLogSlopeUnitStatisticQueryBo bo) {
        ParsedTime startParsed = parseRequiredTime(bo.getStartTime());
        ParsedTime endParsed = parseNullableTime(bo.getEndTime());
        if (endParsed != null && startParsed.dayPrecision() != endParsed.dayPrecision())
            throw new ServiceException("开始时间和结束时间精度必须一致");
        if (startParsed.dayPrecision()) {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime endTime = endParsed == null ? todayStart : endParsed.time();
            if (endTime.toLocalDate().isAfter(todayStart.toLocalDate())) endTime = todayStart;
            LocalDateTime startTime = startParsed.time().toLocalDate().atStartOfDay();
            LocalDateTime endExclusive = endTime.toLocalDate().plusDays(1).atStartOfDay();
            if (!startTime.isBefore(endExclusive)) throw new ServiceException("开始时间必须早于结束时间");
            return new TimeQueryRange(startTime, endExclusive, true);
        }
        LocalDateTime startTime = startParsed.time();
        LocalDateTime latestForecastTime = queryLatestForecastTime();
        LocalDateTime endTime = endParsed == null ? latestForecastTime : endParsed.time();
        if (endTime == null) throw new ServiceException("未查询到可用预测时间");
        if (latestForecastTime != null && endTime.isAfter(latestForecastTime)) endTime = latestForecastTime;
        if (startTime.isAfter(endTime)) throw new ServiceException("开始时间不能晚于结束时间");
        return new TimeQueryRange(startTime.truncatedTo(ChronoUnit.HOURS), endTime.truncatedTo(ChronoUnit.HOURS).plusHours(1), false);
    }

    private ParsedTime parseRequiredTime(String timeText) {
        if (StringUtils.isBlank(timeText)) throw new ServiceException("开始时间不能为空");
        return parseTimeText(timeText);
    }

    private ParsedTime parseNullableTime(String timeText) {
        if (StringUtils.isBlank(timeText)) return null;
        return parseTimeText(timeText);
    }

    private ParsedTime parseTimeText(String timeText) {
        String source = normalizeTimeText(timeText);
        try { return new ParsedTime(LocalDate.parse(source, DATE_ONLY_FORMATTER).atStartOfDay(), true); } catch (DateTimeParseException ignored) {}
        try { return new ParsedTime(LocalDateTime.parse(source, HOUR_SECOND_FORMATTER), false); } catch (DateTimeParseException ignored) {}
        try { return new ParsedTime(LocalDateTime.parse(source, HOUR_MINUTE_FORMATTER), false); } catch (DateTimeParseException ignored) {}
        try { return new ParsedTime(LocalDateTime.parse(source, FLEXIBLE_DATE_TIME_FORMATTER), false); } catch (DateTimeParseException ignored) {}
        throw new ServiceException("时间格式错误，仅支持yyyy-MM-dd、yyyy-MM-dd HH:mm和yyyy-MM-dd HH:mm:ss");
    }

    private String normalizeTimeText(String timeText) {
        String source = timeText == null ? "" : timeText.trim();
        if (source.contains("%") || source.contains("+")) source = URLDecoder.decode(source, StandardCharsets.UTF_8);
        if (source.length() >= 2) {
            char first = source.charAt(0), last = source.charAt(source.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')
                || (first == '\u2018' && last == '\u2019') || (first == '\u201c' && last == '\u201d'))
                source = source.substring(1, source.length() - 1).trim();
        }
        return source.replace('\u3000', ' ').replace('T', ' ').replace('/', '-').replaceAll("\\s+", " ");
    }

    LocalDateTime queryLatestForecastTime() {
        RainfallForecastRasterUnit latest = rainfallForecastRasterUnitMapper.selectOne(
            Wrappers.<RainfallForecastRasterUnit>lambdaQuery()
                .isNotNull(RainfallForecastRasterUnit::getForecastTime)
                .orderByDesc(RainfallForecastRasterUnit::getForecastTime).last("limit 1"));
        return latest == null || latest.getForecastTime() == null ? null : toLocalDateTime(latest.getForecastTime());
    }

    // ==================== 网格/空间工具 ====================

    boolean hasResolvedGrid(RainfallEntityGridMapping mapping) {
        return mapping != null && mapping.getGridLon() != null && mapping.getGridLat() != null;
    }

    boolean hasResolvedForecastGrid(RainfallEntityGridMapping mapping) {
        return mapping != null && mapping.getForecastGridLon() != null && mapping.getForecastGridLat() != null;
    }

    String buildGridKey(Double lon, Double lat) { return lon + "_" + lat; }

    String resolveMappingKey(RainfallEntityGridMapping mapping) {
        if (StringUtils.isNotBlank(mapping.getAdRegionId())) return mapping.getAdRegionId();
        if (StringUtils.isNotBlank(mapping.getSlopeUnitId())) return mapping.getSlopeUnitId();
        return buildGridKey(mapping.getGridLon(), mapping.getGridLat());
    }

    String resolveStreetName(RainfallEntityGridMapping mapping) {
        if (StringUtils.isNotBlank(mapping.getStreet())) return mapping.getStreet();
        if (StringUtils.isNotBlank(mapping.getVillage())) return mapping.getVillage();
        return resolveMappingKey(mapping);
    }

    Map<String, List<RainfallEntityGridMapping>> groupMappingsByGrid(List<RainfallEntityGridMapping> mappings) {
        Map<String, List<RainfallEntityGridMapping>> result = new HashMap<>();
        for (RainfallEntityGridMapping m : mappings)
            result.computeIfAbsent(buildGridKey(m.getGridLon(), m.getGridLat()), k -> new ArrayList<>()).add(m);
        return result;
    }

    double[] parsePoint(String wktPoint) {
        if (StringUtils.isBlank(wktPoint)) return null;
        try {
            Geometry geometry = new WKTReader().read(wktPoint);
            if (!(geometry instanceof Point point)) return null;
            return new double[]{point.getX(), point.getY()};
        } catch (ParseException e) { log.warn("解析中心点WKT失败: {}", e.getMessage()); return null; }
    }

    double[] resolveCenterPoint(RainfallEntityGridMapping mapping) {
        if (mapping == null) return null;
        if (mapping.getCenterLon() != null && mapping.getCenterLat() != null)
            return new double[]{mapping.getCenterLon(), mapping.getCenterLat()};
        double[] parsed = parsePoint(mapping.getCenter());
        if (parsed != null) return parsed;
        if (mapping.getGridLon() != null && mapping.getGridLat() != null)
            return new double[]{mapping.getGridLon(), mapping.getGridLat()};
        return null;
    }

    double[] requireCenterPoint(String center) {
        double[] pt = parsePoint(center);
        if (pt == null) throw new ServiceException("斜坡单元中心点坐标无效");
        return pt;
    }

    GridLocation requireForecastGridLocation(double[] centerPoint) {
        GridLocation loc = resolveForecastGridLocation(centerPoint);
        if (loc == null) throw new ServiceException("未匹配到斜坡单元预报栅格");
        return loc;
    }

    GridLocation requireForecastGridLocation(RainfallEntityGridMapping mapping) {
        GridLocation loc = resolveForecastGridLocation(mapping);
        if (loc == null) throw new ServiceException("未匹配到斜坡单元预报栅格");
        return loc;
    }

    GridLocation resolveForecastGridLocation(RainfallEntityGridMapping mapping) {
        if (hasResolvedForecastGrid(mapping)) {
            return new GridLocation(mapping.getForecastGridLon(), mapping.getForecastGridLat());
        }
        return resolveForecastGridLocation(resolveCenterPoint(mapping));
    }

    GridLocation resolveForecastGridLocation(double[] centerPoint) {
        List<RainfallForecastRasterUnit> refs = listForecastReferenceCells();
        return resolveForecastGridLocation(centerPoint, refs);
    }

    GridLocation resolveForecastGridLocation(double[] centerPoint, List<RainfallForecastRasterUnit> refs) {
        if (centerPoint == null || refs == null || refs.isEmpty()) return null;
        RainfallForecastRasterUnit cell = selectLeftLowerForecastCell(refs, centerPoint[0], centerPoint[1]);
        if (cell == null || cell.getLon() == null || cell.getLat() == null) return null;
        return new GridLocation(cell.getLon(), cell.getLat());
    }

    List<RainfallForecastRasterUnit> listForecastReferenceCells() {
        LocalDateTime latest = queryLatestForecastTime();
        if (latest == null) return new ArrayList<>();
        return rainfallForecastRasterUnitMapper.selectList(
            Wrappers.<RainfallForecastRasterUnit>lambdaQuery()
                .eq(RainfallForecastRasterUnit::getForecastTime, toDate(latest))
                .orderByAsc(RainfallForecastRasterUnit::getLon).orderByAsc(RainfallForecastRasterUnit::getLat));
    }

    RainfallForecastRasterUnit selectLeftLowerForecastCell(List<RainfallForecastRasterUnit> cells, double lon, double lat) {
        RainfallForecastRasterUnit result = null;
        double min = Double.MAX_VALUE;
        for (RainfallForecastRasterUnit c : cells) {
            if (c.getLon() == null || c.getLat() == null || c.getLon() >= lon || c.getLat() >= lat) continue;
            double d = Math.pow(lon - c.getLon(), 2) + Math.pow(lat - c.getLat(), 2);
            if (d < min) { min = d; result = c; }
        }
        return result;
    }

    RainfallForecastRasterUnit selectNearestForecastCell(List<RainfallForecastRasterUnit> cells, double lon, double lat) {
        RainfallForecastRasterUnit result = null;
        double min = Double.MAX_VALUE;
        for (RainfallForecastRasterUnit c : cells) {
            if (c.getLon() == null || c.getLat() == null) continue;
            double d = Math.pow(lon - c.getLon(), 2) + Math.pow(lat - c.getLat(), 2);
            if (d < min) { min = d; result = c; }
        }
        return result;
    }

    List<double[]> buildCenterPoints(List<String> centers) {
        if (centers == null || centers.isEmpty()) return new ArrayList<>();
        List<double[]> result = new ArrayList<>(centers.size());
        for (String c : centers) { double[] pt = parsePoint(c); if (pt != null) result.add(pt); }
        return result;
    }

    // ==================== 栅格查询 ====================

    List<RainfallLogRasterUnit> listRasterUnitsByMappings(List<RainfallEntityGridMapping> mappings, LocalDateTime start, LocalDateTime end) {
        if (mappings == null || mappings.isEmpty()) return new ArrayList<>();
        List<GridLocation> locs = mappings.stream().filter(this::hasResolvedGrid)
            .map(m -> new GridLocation(m.getGridLon(), m.getGridLat())).distinct().toList();
        return listRasterUnitsByLocations(locs, start, end);
    }

    List<RainfallLogRasterUnit> listRasterUnitsByLocations(List<GridLocation> locs, LocalDateTime start, LocalDateTime end) {
        if (locs == null || locs.isEmpty()) return new ArrayList<>();
        LambdaQueryWrapper<RainfallLogRasterUnit> lqw = Wrappers.lambdaQuery();
        lqw.ge(RainfallLogRasterUnit::getLogTime, toDate(start)).lt(RainfallLogRasterUnit::getLogTime, toDate(end))
           .and(w -> { boolean f = true; for (GridLocation loc : locs) {
               if (f) { w.eq(RainfallLogRasterUnit::getLon, loc.lon()).eq(RainfallLogRasterUnit::getLat, loc.lat()); f = false; }
               else w.or(o -> o.eq(RainfallLogRasterUnit::getLon, loc.lon()).eq(RainfallLogRasterUnit::getLat, loc.lat())); }})
           .orderByAsc(RainfallLogRasterUnit::getLogTime);
        return rainfallCellMapper.selectList(lqw);
    }

    List<RainfallForecastRasterUnit> listForecastUnitsByLocations(List<GridLocation> locs, LocalDateTime start, LocalDateTime end) {
        return listForecastUnitsByLocations(locs, start, end, false);
    }

    List<RainfallForecastRasterUnit> listForecastUnitsByLocations(List<GridLocation> locs, LocalDateTime start, LocalDateTime end, boolean inclusiveEnd) {
        if (locs == null || locs.isEmpty()) return new ArrayList<>();
        LambdaQueryWrapper<RainfallForecastRasterUnit> lqw = Wrappers.lambdaQuery();
        lqw.ge(RainfallForecastRasterUnit::getForecastTime, toDate(start));
        if (inclusiveEnd) lqw.le(RainfallForecastRasterUnit::getForecastTime, toDate(end));
        else lqw.lt(RainfallForecastRasterUnit::getForecastTime, toDate(end));
        lqw.and(w -> { boolean f = true; for (GridLocation loc : locs) {
            if (f) { w.eq(RainfallForecastRasterUnit::getLon, loc.lon()).eq(RainfallForecastRasterUnit::getLat, loc.lat()); f = false; }
            else w.or(o -> o.eq(RainfallForecastRasterUnit::getLon, loc.lon()).eq(RainfallForecastRasterUnit::getLat, loc.lat())); }})
           .orderByAsc(RainfallForecastRasterUnit::getForecastTime);
        return rainfallForecastRasterUnitMapper.selectList(lqw);
    }

    // ==================== 序列构建 ====================

    LocalDateTime resolveRainfallPeriodStart(LocalDateTime time) {
        LocalDateTime dayStart = time.toLocalDate().atTime(RAINFALL_PERIOD_HOUR, 0);
        return time.isBefore(dayStart) ? dayStart.minusDays(1) : dayStart;
    }

    Map<GridLocation, Map<LocalDateTime, Double>> buildActualPeriodRainfallMap(
        List<RainfallLogRasterUnit> units, LocalDateTime start, LocalDateTime end) {
        Map<GridLocation, Map<LocalDateTime, Double>> result = new HashMap<>();
        if (units == null || units.isEmpty()) return result;
        for (RainfallLogRasterUnit u : units) {
            if (u.getLogTime() == null || u.getLon() == null || u.getLat() == null) continue;
            LocalDateTime t = toLocalDateTime(u.getLogTime());
            LocalDateTime period = resolveRainfallPeriodStart(t);
            if (period.isBefore(start) || !period.isBefore(end)) continue;
            result.computeIfAbsent(new GridLocation(u.getLon(), u.getLat()), k -> new HashMap<>())
                  .merge(period, defaultValue(u.getRainfall()), Double::sum);
        }
        return result;
    }

    Map<GridLocation, Map<LocalDateTime, Double>> buildActualHourlyRainfallMap(
        List<RainfallLogRasterUnit> units, LocalDateTime start, LocalDateTime end) {
        Map<GridLocation, Map<LocalDateTime, Double>> result = new HashMap<>();
        if (units == null || units.isEmpty()) return result;
        for (RainfallLogRasterUnit unit : units) {
            if (unit.getLogTime() == null || unit.getLon() == null || unit.getLat() == null) continue;
            LocalDateTime logTime = toLocalDateTime(unit.getLogTime()).truncatedTo(ChronoUnit.HOURS);
            if (logTime.isBefore(start) || !logTime.isBefore(end)) continue;
            result.computeIfAbsent(new GridLocation(unit.getLon(), unit.getLat()), key -> new HashMap<>())
                .merge(logTime, defaultValue(unit.getRainfall()), Double::sum);
        }
        return result;
    }

    Map<GridLocation, Map<LocalDateTime, Double>> buildForecastPeriodRainfallMap(
        List<RainfallForecastRasterUnit> units, LocalDateTime start, LocalDateTime end) {
        Map<GridLocation, Map<LocalDateTime, Double>> result = new HashMap<>();
        if (units == null || units.isEmpty()) return result;
        for (RainfallForecastRasterUnit u : units) {
            if (u.getForecastTime() == null || u.getLon() == null || u.getLat() == null) continue;
            LocalDateTime t = toLocalDateTime(u.getForecastTime());
            LocalDateTime period = resolveRainfallPeriodStart(t);
            if (period.isBefore(start) || !period.isBefore(end)) continue;
            result.computeIfAbsent(new GridLocation(u.getLon(), u.getLat()), k -> new HashMap<>())
                  .merge(period, defaultValue(u.getRainfall()), Double::sum);
        }
        return result;
    }

    List<SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem> buildSlopeUnitRainfallSeries(
        GridLocation actualGrid, GridLocation forecastGrid,
        Map<GridLocation, Map<LocalDateTime, Double>> actualMap,
        Map<GridLocation, Map<LocalDateTime, Double>> forecastMap,
        LocalDateTime actualStart, LocalDateTime baseTime, LocalDateTime forecastEnd) {
        List<SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem> result = new ArrayList<>(ACTUAL_MAX_DAYS + FORECAST_DAYS);
        Map<LocalDateTime, Double> apm = actualGrid == null ? Collections.emptyMap() : actualMap.getOrDefault(actualGrid, Collections.emptyMap());
        Map<LocalDateTime, Double> fpm = forecastGrid == null ? Collections.emptyMap() : forecastMap.getOrDefault(forecastGrid, Collections.emptyMap());
        for (LocalDateTime p = actualStart; p.isBefore(baseTime); p = p.plusDays(1))
            result.add(buildRainfallPeriodItem(p, p.plusDays(1), apm.getOrDefault(p, 0D), SERIES_SOURCE_ACTUAL));
        for (LocalDateTime p = baseTime; p.isBefore(forecastEnd); p = p.plusDays(1))
            result.add(buildRainfallPeriodItem(p, p.plusDays(1), fpm.getOrDefault(p, 0D), SERIES_SOURCE_FORECAST));
        return result;
    }

    private SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem buildRainfallPeriodItem(
        LocalDateTime start, LocalDateTime end, Double rainfall, String source) {
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item = new SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem();
        item.setStartTime(start); item.setEndTime(end);
        item.setRainfall(roundRainfall(defaultValue(rainfall))); item.setSource(source);
        return item;
    }

    Map<GridLocation, Integer> buildForecastGridAssignmentCount(List<RainfallEntityGridMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) return new HashMap<>();
        Map<GridLocation, Integer> result = new HashMap<>();
        List<RainfallEntityGridMapping> unresolved = new ArrayList<>();
        for (RainfallEntityGridMapping m : mappings) {
            if (hasResolvedForecastGrid(m)) {
                result.merge(new GridLocation(m.getForecastGridLon(), m.getForecastGridLat()), 1, Integer::sum);
            } else {
                unresolved.add(m);
            }
        }
        if (unresolved.isEmpty()) return result;
        List<RainfallForecastRasterUnit> refs = listForecastReferenceCells();
        if (refs.isEmpty()) return result;
        for (RainfallEntityGridMapping m : unresolved) {
            double[] cp = resolveCenterPoint(m);
            if (cp == null) continue;
            RainfallForecastRasterUnit nearest = selectLeftLowerForecastCell(refs, cp[0], cp[1]);
            if (nearest == null || nearest.getLon() == null || nearest.getLat() == null) continue;
            result.merge(new GridLocation(nearest.getLon(), nearest.getLat()), 1, Integer::sum);
        }
        return result;
    }

    String buildSlopeUnitDailyKey(String slopeUnitId, LocalDate statDate) { return slopeUnitId + "|" + statDate; }

    List<RainfallForecastDailyStatisticVo> initForecastStatistics(LocalDate startDate) {
        List<RainfallForecastDailyStatisticVo> result = new ArrayList<>(FORECAST_DAYS);
        for (int i = 0; i < FORECAST_DAYS; i++) {
            RainfallForecastDailyStatisticVo item = new RainfallForecastDailyStatisticVo();
            item.setStatDate(startDate.plusDays(i)); item.setDailyForecastRainfall(0D);
            result.add(item);
        }
        return result;
    }

    // ==================== 通用栅格查询 ====================

    /**
     * 查询指定时间范围和经纬度上界内的预报栅格单元。
     */
    List<RainfallForecastRasterUnit> selectForecastCellsByBounds(LocalDateTime start, LocalDateTime end, double maxLon, double maxLat) {
        LambdaQueryWrapper<RainfallForecastRasterUnit> lqw = Wrappers.lambdaQuery();
        lqw.between(RainfallForecastRasterUnit::getForecastTime, toDate(start), toDate(end))
           .lt(RainfallForecastRasterUnit::getLon, maxLon)
           .lt(RainfallForecastRasterUnit::getLat, maxLat)
           .orderByAsc(RainfallForecastRasterUnit::getForecastTime);
        return rainfallForecastRasterUnitMapper.selectList(lqw);
    }

    // ==================== 区划查询工具 ====================

    Integer resolveAdRegionTargetLevel(RainfallLogSlopeUnitStatisticQueryBo bo) {
        if (StringUtils.isNotBlank(bo.getVillage()) || StringUtils.isNotBlank(bo.getVillageCode())) return 5;
        if (StringUtils.isNotBlank(bo.getStreet()) || StringUtils.isNotBlank(bo.getStreetCode())) return 4;
        if (StringUtils.isNotBlank(bo.getCounty()) || StringUtils.isNotBlank(bo.getCountyCode())) return 3;
        if (StringUtils.isNotBlank(bo.getCity()) || StringUtils.isNotBlank(bo.getCityCode())) return 2;
        if (StringUtils.isNotBlank(bo.getProvince()) || StringUtils.isNotBlank(bo.getProvinceCode())) return 1;
        return null;
    }

    RainfallLogSlopeUnitStatisticQueryBo buildActualQueryRange(RainfallLogSlopeUnitStatisticQueryBo bo) {
        LocalDate today = LocalDate.now(), minDate = today.minusDays(ACTUAL_MAX_DAYS - 1L);
        LocalDate actualStart = bo.getStartDate().isBefore(minDate) ? minDate : bo.getStartDate();
        LocalDate actualEnd = bo.getEndDate().isAfter(today) ? today : bo.getEndDate();
        if (actualStart.isAfter(actualEnd)) return null;
        RainfallLogSlopeUnitStatisticQueryBo q = new RainfallLogSlopeUnitStatisticQueryBo();
        q.setStartDate(actualStart); q.setEndDate(actualEnd); q.setSlopeUnitId(bo.getSlopeUnitId());
        q.setProvince(bo.getProvince()); q.setCity(bo.getCity()); q.setCounty(bo.getCounty());
        q.setStreet(bo.getStreet()); q.setVillage(bo.getVillage());
        q.setProvinceCode(bo.getProvinceCode()); q.setCityCode(bo.getCityCode());
        q.setCountyCode(bo.getCountyCode()); q.setStreetCode(bo.getStreetCode()); q.setVillageCode(bo.getVillageCode());
        return q;
    }

    RainfallLogSlopeUnitStatisticQueryBo buildRecentActualQueryRangeExcludeToday(RainfallLogSlopeUnitStatisticQueryBo bo) {
        RainfallLogSlopeUnitStatisticQueryBo qb = bo == null ? new RainfallLogSlopeUnitStatisticQueryBo() : bo;
        LocalDate yesterday = LocalDate.now().minusDays(1), defaultStart = yesterday.minusDays(ACTUAL_MAX_DAYS - 1L);
        LocalDate endDate = qb.getEndDate() == null ? yesterday : qb.getEndDate();
        if (endDate.isAfter(yesterday)) endDate = yesterday;
        LocalDate startDate = qb.getStartDate() == null ? endDate.minusDays(ACTUAL_MAX_DAYS - 1L) : qb.getStartDate();
        if (startDate.isAfter(endDate)) throw new ServiceException("开始日期不能晚于结束日期");
        if (startDate.isBefore(defaultStart)) startDate = defaultStart;
        if (endDate.isBefore(defaultStart)) return null;
        RainfallLogSlopeUnitStatisticQueryBo q = new RainfallLogSlopeUnitStatisticQueryBo();
        q.setStartDate(startDate); q.setEndDate(endDate);
        return q;
    }
}
