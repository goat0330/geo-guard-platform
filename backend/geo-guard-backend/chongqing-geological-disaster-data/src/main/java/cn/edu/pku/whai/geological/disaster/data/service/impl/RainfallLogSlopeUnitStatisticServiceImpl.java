/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallForecastRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallForecastDailyStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallHourlySeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticResultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitTodayHourlyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.TodayRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallEntityGridMappingService;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallLogSlopeUnitStatisticService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.DailyActualAccumulator;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.GridLocation;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.RainfallAccumulator;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.SlopeUnitForecastTarget;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.ShortTermTemporaryRainfallSeriesOverlay;
import cn.edu.pku.whai.geological.disaster.data.service.impl.helper.TimeQueryRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 斜坡单元实况雨量统计 Service 实现（门面）。
 * 复杂逻辑委托给 {@link RainfallStatisticHelper}。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RainfallLogSlopeUnitStatisticServiceImpl implements IRainfallLogSlopeUnitStatisticService {

    private final IAdRegionService adRegionService;
    private final IRainfallEntityGridMappingService rainfallEntityGridMappingService;
    private final ISlopeUnitService slopeUnitService;
    private final RainfallStatisticHelper h;
    private final ShortTermTemporaryRainfallSeriesOverlay shortTermTemporaryRainfallSeriesOverlay;

    // ==================== 公开接口方法 ====================

    @Override
    public RainfallLogSlopeUnitStatisticResultVo queryPageListBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo) {
        h.validateDateRange(bo);
        if (StringUtils.isBlank(bo.getSlopeUnitId())) throw new ServiceException("斜坡单元ID不能为空");
        RainfallLogSlopeUnitStatisticQueryBo actualQuery = h.buildActualQueryRange(bo);
        SlopeUnitVo slopeUnit = slopeUnitService.queryById(bo.getSlopeUnitId());
        if (slopeUnit == null) throw new ServiceException("斜坡单元不存在");
        RainfallLogSlopeUnitStatisticResultVo result = new RainfallLogSlopeUnitStatisticResultVo();
        result.setActualStatistics(actualQuery == null ? new ArrayList<>() : buildSlopeUnitActualStatistics(actualQuery, slopeUnit));
        result.setForecastStatistics(calculateForecastStatisticsByCenters(
            h.buildCenterPoints(List.of(slopeUnit.getCenter())), LocalDate.now()));
        return result;
    }

    @Override
    public RainfallLogSlopeUnitStatisticResultVo queryPageListByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo) {
        h.validateDateRange(bo);
        if (h.hasNoAdRegionCondition(bo)) throw new ServiceException("行政区划名称或行政区划编码至少传一个");
        RainfallLogSlopeUnitStatisticQueryBo actualQuery = h.buildActualQueryRange(bo);
        RainfallLogSlopeUnitStatisticResultVo result = new RainfallLogSlopeUnitStatisticResultVo();
        result.setActualStatistics(actualQuery == null ? new ArrayList<>() : buildAdRegionActualStatistics(actualQuery));
        List<AdRegionVo> matchedAdRegions = listAdRegionsByQuery(bo);
        result.setForecastStatistics(calculateForecastStatisticsByCenters(
            h.buildCenterPoints(matchedAdRegions.stream().map(AdRegionVo::getCenter).toList()), LocalDate.now()));
        return result;
    }

    @Override
    public List<SlopeUnitDailyRainfallSeriesVo> queryDailyListByAllSlopeUnits(RainfallLogSlopeUnitStatisticQueryBo bo) {
        RainfallLogSlopeUnitStatisticQueryBo actualQuery = h.buildRecentActualQueryRangeExcludeToday(bo);
        List<RainfallEntityGridMapping> mappings = rainfallEntityGridMappingService.listSlopeUnitMappings();
        if (mappings == null || mappings.isEmpty()) return new ArrayList<>();
        return buildSlopeUnitDailyStatistics(mappings,
            actualQuery == null ? null : actualQuery.getStartDate(),
            actualQuery == null ? null : actualQuery.getEndDate(), LocalDate.now());
    }

    @Override
    public List<TodayRainfallVo> queryTodayRainfall() {
        LocalDate today = LocalDate.now();
        LocalDateTime startTime = today.atStartOfDay(), endTime = today.plusDays(1).atStartOfDay();
        List<RainfallEntityGridMapping> streetMappings = rainfallEntityGridMappingService.listEnshiStreetMappings();
        if (streetMappings == null || streetMappings.isEmpty()) return new ArrayList<>();
        return buildTodayRainfallList(streetMappings, startTime, endTime);
    }

    @Override
    public SlopeUnitTodayHourlyRainfallVo queryTodayHourlyRainfallBySlopeUnit(String slopeUnitId) {
        return buildSlopeUnitDailyRainfall(slopeUnitId, LocalDate.now());
    }

    @Override
    public List<SlopeUnitForecastRainfallSeriesVo> queryForecastRainfallBySlopeUnit(String slopeUnitId) {
        LocalDateTime baseTime = LocalDate.now().atTime(RainfallStatisticHelper.RAINFALL_PERIOD_HOUR, 0);
        LocalDateTime actualStartTime = baseTime.minusDays(RainfallStatisticHelper.ACTUAL_MAX_DAYS);
        LocalDateTime forecastEndTime = baseTime.plusDays(RainfallStatisticHelper.FORECAST_DAYS);
        List<SlopeUnitForecastTarget> targets = listForecastQueryTargets(slopeUnitId);
        if (targets.isEmpty()) return new ArrayList<>();

        Map<String, RainfallEntityGridMapping> mappingBySlopeUnitId = rainfallEntityGridMappingService
            .listSlopeUnitMappings().stream()
            .filter(m -> StringUtils.isBlank(slopeUnitId) || slopeUnitId.equals(m.getSlopeUnitId()))
            .filter(m -> h.hasResolvedGrid(m) || h.hasResolvedForecastGrid(m))
            .filter(m -> StringUtils.isNotBlank(m.getSlopeUnitId()))
            .collect(java.util.stream.Collectors.toMap(RainfallEntityGridMapping::getSlopeUnitId, m -> m, (l, r) -> l));

        List<RainfallForecastRasterUnit> referenceCells = h.listForecastReferenceCells();
        Map<String, GridLocation> actualGridBySlopeUnitId = new LinkedHashMap<>();
        Map<String, GridLocation> forecastGridBySlopeUnitId = new LinkedHashMap<>();
        for (SlopeUnitForecastTarget target : targets) {
            RainfallEntityGridMapping mapping = mappingBySlopeUnitId.get(target.slopeUnitId());
            if (mapping != null && h.hasResolvedGrid(mapping))
                actualGridBySlopeUnitId.put(target.slopeUnitId(), new GridLocation(mapping.getGridLon(), mapping.getGridLat()));
            GridLocation fgl = mapping != null && h.hasResolvedForecastGrid(mapping)
                ? new GridLocation(mapping.getForecastGridLon(), mapping.getForecastGridLat())
                : h.resolveForecastGridLocation(h.parsePoint(target.center()), referenceCells);
            if (fgl != null) forecastGridBySlopeUnitId.put(target.slopeUnitId(), fgl);
        }

        List<RainfallLogRasterUnit> actualUnits = h.listRasterUnitsByLocations(
            actualGridBySlopeUnitId.values().stream().distinct().toList(), actualStartTime, baseTime);
        List<RainfallForecastRasterUnit> forecastUnits = h.listForecastUnitsByLocations(
            forecastGridBySlopeUnitId.values().stream().distinct().toList(), baseTime, forecastEndTime);
        Map<GridLocation, Map<LocalDateTime, Double>> arm = h.buildActualPeriodRainfallMap(actualUnits, actualStartTime, baseTime);
        Map<GridLocation, Map<LocalDateTime, Double>> ahrm = h.buildActualHourlyRainfallMap(actualUnits, actualStartTime, baseTime);
        Map<GridLocation, Map<LocalDateTime, Double>> frm = h.buildForecastPeriodRainfallMap(forecastUnits, baseTime, forecastEndTime);

        List<SlopeUnitForecastRainfallSeriesVo> result = new ArrayList<>(targets.size());
        for (SlopeUnitForecastTarget target : targets) {
            SlopeUnitForecastRainfallSeriesVo item = new SlopeUnitForecastRainfallSeriesVo();
            item.setSlopeUnitId(target.slopeUnitId());
            List<SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem> rainfallSeries = h.buildSlopeUnitRainfallSeries(
                actualGridBySlopeUnitId.get(target.slopeUnitId()), forecastGridBySlopeUnitId.get(target.slopeUnitId()),
                arm, frm, actualStartTime, baseTime, forecastEndTime);
            shortTermTemporaryRainfallSeriesOverlay.apply(
                rainfallSeries,
                mappingBySlopeUnitId.get(target.slopeUnitId()),
                ahrm.getOrDefault(actualGridBySlopeUnitId.get(target.slopeUnitId()), Map.of()));
            item.setRainfallSeries(rainfallSeries);
            result.add(item);
        }
        return result;
    }

    @Override
    public Double queryForecastDailyRainfallBySlopeUnit(String slopeUnitId, LocalDate statDate) {
        if (StringUtils.isBlank(slopeUnitId)) throw new ServiceException("斜坡单元ID不能为空");
        if (statDate == null) throw new ServiceException("统计日期不能为空");
        RainfallEntityGridMapping mapping = rainfallEntityGridMappingService.querySlopeUnitMapping(slopeUnitId);
        GridLocation fgl;
        if (mapping != null) {
            fgl = h.requireForecastGridLocation(mapping);
        } else {
            SlopeUnitVo su = requireSlopeUnit(slopeUnitId);
            fgl = h.requireForecastGridLocation(h.requireCenterPoint(su.getCenter()));
        }
        LocalDateTime startTime = statDate.atStartOfDay(), endTime = statDate.plusDays(1).atStartOfDay();
        List<RainfallForecastRasterUnit> fus = h.listForecastUnitsByLocations(List.of(fgl), startTime, endTime);
        double daily = 0D;
        for (RainfallForecastRasterUnit u : fus) daily += h.defaultValue(u.getRainfall());
        return h.roundRainfall(daily);
    }

    @Override
    public RainfallHourlySeriesVo queryHourlyRainfallBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo) {
        if (StringUtils.isBlank(bo.getSlopeUnitId())) throw new ServiceException("斜坡单元ID不能为空");
        RainfallEntityGridMapping mapping = rainfallEntityGridMappingService.querySlopeUnitMapping(bo.getSlopeUnitId());
        if (mapping == null || mapping.getGridLon() == null || mapping.getGridLat() == null)
            throw new ServiceException("斜坡单元未绑定雨量栅格");
        TimeQueryRange qr = h.resolveTimeQueryRange(bo);
        RainfallHourlySeriesVo result = qr.dayPrecision()
            ? buildDailySeriesByMappings(List.of(mapping), qr.startTime(), qr.endTime())
            : buildHourlySeriesByMappings(List.of(mapping), qr.startTime(), qr.endTime());
        result.setSlopeUnitId(bo.getSlopeUnitId());
        return result;
    }

    @Override
    public RainfallHourlySeriesVo queryHourlyRainfallByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo) {
        if (h.hasNoAdRegionCondition(bo)) throw new ServiceException("行政区划名称或行政区划编码至少传一个");
        List<RainfallEntityGridMapping> mappings = rainfallEntityGridMappingService.listAdRegionMappings(bo);
        if (mappings == null || mappings.isEmpty()) throw new ServiceException("未匹配到行政区划栅格映射");
        TimeQueryRange qr = h.resolveTimeQueryRange(bo);
        return qr.dayPrecision()
            ? buildDailySeriesByMappings(mappings, qr.startTime(), qr.endTime())
            : buildHourlySeriesByMappings(mappings, qr.startTime(), qr.endTime());
    }

    // ==================== 私有方法（保留复杂编排逻辑） ====================

    private SlopeUnitTodayHourlyRainfallVo buildSlopeUnitDailyRainfall(String slopeUnitId, LocalDate statDate) {
        if (StringUtils.isBlank(slopeUnitId)) throw new ServiceException("斜坡单元ID不能为空");
        if (statDate == null) throw new ServiceException("统计日期不能为空");
        RainfallEntityGridMapping mapping = requireSlopeUnitMapping(slopeUnitId);
        LocalDateTime startTime = statDate.atStartOfDay(), endTime = statDate.plusDays(1).atStartOfDay();
        Date startDate = h.toDate(startTime), endDate = h.toDate(endTime);

        List<RainfallLogRasterUnit> hus = h.listRasterUnitsByLocations(
            List.of(new GridLocation(mapping.getGridLon(), mapping.getGridLat())), startTime, endTime);
        Map<Integer, Double> historyHourlyRainfallMap = new HashMap<>();
        for (RainfallLogRasterUnit u : hus) {
            if (u.getLogTime() == null) continue;
            int hour = h.toLocalDateTime(u.getLogTime()).getHour();
            historyHourlyRainfallMap.merge(hour, h.defaultValue(u.getRainfall()), Double::sum);
        }

        Map<GridLocation, Integer> fgac = h.buildForecastGridAssignmentCount(List.of(mapping));
        List<RainfallForecastRasterUnit> fus = h.listForecastUnitsByLocations(
            new ArrayList<>(fgac.keySet()), startTime, endTime);
        Map<Integer, Double> forecastHourlyRainfallMap = new HashMap<>();
        for (RainfallForecastRasterUnit u : fus) {
            if (u.getForecastTime() == null) continue;
            Integer ac = fgac.get(new GridLocation(u.getLon(), u.getLat()));
            if (ac == null || ac <= 0) continue;
            LocalDateTime ist = h.toLocalDateTime(u.getForecastTime()).truncatedTo(ChronoUnit.HOURS);
            double hourlyRainfall = h.defaultValue(u.getRainfall()) / 3D;
            for (int offset = 0; offset < 3; offset++) {
                LocalDateTime ht = ist.plusHours(offset);
                if (ht.isBefore(startTime) || !ht.isBefore(endTime)) continue;
                forecastHourlyRainfallMap.merge(ht.getHour(), hourlyRainfall * ac, Double::sum);
            }
        }

        List<SlopeUnitTodayHourlyRainfallVo.HourlyRainfallItem> hourlySeries = new ArrayList<>(24);
        int hhc = 0, fhc = 0, mhc = 0; double dtr = 0D;
        for (int hour = 0; hour < 24; hour++) {
            SlopeUnitTodayHourlyRainfallVo.HourlyRainfallItem item = new SlopeUnitTodayHourlyRainfallVo.HourlyRainfallItem();
            item.setHourTime(startTime.plusHours(hour));
            Double hv = historyHourlyRainfallMap.get(hour);
            if (hv != null) { double rr = h.roundRainfall(hv); item.setRainfall(rr); item.setSource(RainfallStatisticHelper.SOURCE_HISTORY); hhc++; dtr += rr; hourlySeries.add(item); continue; }
            Double fv = forecastHourlyRainfallMap.get(hour);
            if (fv != null) { double rr = h.roundRainfall(fv); item.setRainfall(rr); item.setSource(RainfallStatisticHelper.SOURCE_FORECAST_FALLBACK); fhc++; dtr += rr; hourlySeries.add(item); continue; }
            item.setRainfall(0D); item.setSource(RainfallStatisticHelper.SOURCE_MISSING); mhc++; hourlySeries.add(item);
        }

        SlopeUnitTodayHourlyRainfallVo result = new SlopeUnitTodayHourlyRainfallVo();
        result.setSlopeUnitId(slopeUnitId); result.setStatDate(statDate); result.setHourlySeries(hourlySeries);
        result.setDailyTotalRainfall(h.roundRainfall(dtr)); result.setHistoryHourCount(hhc);
        result.setForecastFallbackHourCount(fhc); result.setMissingHourCount(mhc);
        return result;
    }

    private RainfallEntityGridMapping requireSlopeUnitMapping(String slopeUnitId) {
        RainfallEntityGridMapping m = rainfallEntityGridMappingService.querySlopeUnitMapping(slopeUnitId);
        if (m == null || m.getGridLon() == null || m.getGridLat() == null) throw new ServiceException("斜坡单元未绑定雨量栅格");
        return m;
    }

    private SlopeUnitVo requireSlopeUnit(String slopeUnitId) {
        SlopeUnitVo su = slopeUnitService.queryById(slopeUnitId);
        if (su == null) throw new ServiceException("斜坡单元不存在");
        return su;
    }

    private List<SlopeUnitForecastTarget> listForecastQueryTargets(String slopeUnitId) {
        List<SlopeUnit> sus = slopeUnitService.listAllWithCenter();
        if (sus == null || sus.isEmpty()) return new ArrayList<>();
        List<SlopeUnitForecastTarget> result = new ArrayList<>(sus.size());
        for (SlopeUnit su : sus) {
            if (su == null || StringUtils.isBlank(su.getId())) continue;
            if (StringUtils.isNotBlank(slopeUnitId) && !slopeUnitId.equals(su.getId())) continue;
            result.add(new SlopeUnitForecastTarget(su.getId(), su.getCenter()));
        }
        return result;
    }

    // --- actual statistics builders ---
    private List<RainfallLogSlopeUnitStatisticVo> buildSlopeUnitActualStatistics(
        RainfallLogSlopeUnitStatisticQueryBo bo, SlopeUnitVo slopeUnit) {
        RainfallEntityGridMapping m = rainfallEntityGridMappingService.querySlopeUnitMapping(bo.getSlopeUnitId());
        if (m == null) return new ArrayList<>();
        return buildActualStatistics(List.of(m), bo.getStartDate(), bo.getEndDate(), slopeUnit.getName());
    }

    private List<RainfallLogSlopeUnitStatisticVo> buildAdRegionActualStatistics(RainfallLogSlopeUnitStatisticQueryBo bo) {
        List<RainfallEntityGridMapping> ms = rainfallEntityGridMappingService.listAdRegionMappings(bo);
        if (ms == null || ms.isEmpty()) return new ArrayList<>();
        return buildActualStatistics(ms, bo.getStartDate(), bo.getEndDate(), null);
    }

    private List<TodayRainfallVo> buildTodayRainfallList(
        List<RainfallEntityGridMapping> streetMappings, LocalDateTime startTime, LocalDateTime endTime) {
        List<RainfallEntityGridMapping> valid = streetMappings.stream().filter(h::hasResolvedGrid).toList();
        List<RainfallLogRasterUnit> rus = h.listRasterUnitsByMappings(valid, startTime, endTime);
        Map<String, List<RainfallEntityGridMapping>> mbg = h.groupMappingsByGrid(valid);
        Map<String, TodayRainfallVo> rm = new HashMap<>();
        for (RainfallEntityGridMapping m : streetMappings) {
            String key = h.resolveMappingKey(m);
            TodayRainfallVo vo = new TodayRainfallVo(); vo.setStreet(h.resolveStreetName(m)); vo.setRainfall(0D);
            if (m.getCenterLon() != null && m.getCenterLat() != null) vo.setCoordinates(Arrays.asList(m.getCenterLon(), m.getCenterLat()));
            rm.put(key, vo);
        }
        for (RainfallLogRasterUnit ru : rus) {
            List<RainfallEntityGridMapping> mm = mbg.get(h.buildGridKey(ru.getLon(), ru.getLat()));
            if (mm == null || mm.isEmpty()) continue;
            for (RainfallEntityGridMapping m : mm) {
                TodayRainfallVo vo = rm.get(h.resolveMappingKey(m));
                if (vo == null) continue;
                vo.setRainfall(h.defaultValue(vo.getRainfall()) + h.defaultValue(ru.getRainfall()));
            }
        }
        List<TodayRainfallVo> result = new ArrayList<>(rm.values());
        result.forEach(item -> item.setRainfall(h.roundRainfall(h.defaultValue(item.getRainfall()))));
        result.sort(Comparator.comparing(TodayRainfallVo::getStreet, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    // --- hourly/daily series ---
    private RainfallHourlySeriesVo buildHourlySeriesByMappings(
        List<RainfallEntityGridMapping> mappings, LocalDateTime startTime, LocalDateTime endTime) {
        List<RainfallEntityGridMapping> valid = mappings.stream().filter(h::hasResolvedGrid).toList();
        if (valid.isEmpty()) throw new ServiceException("未匹配到有效栅格映射");
        Map<String, List<RainfallEntityGridMapping>> mbg = h.groupMappingsByGrid(valid);
        List<RainfallLogRasterUnit> hus = h.listRasterUnitsByMappings(valid, startTime, endTime);
        Map<LocalDateTime, RainfallAccumulator> hbh = new HashMap<>();
        for (RainfallLogRasterUnit hu : hus) {
            if (hu.getLogTime() == null) continue;
            LocalDateTime ht = h.toLocalDateTime(hu.getLogTime()).truncatedTo(ChronoUnit.HOURS);
            List<RainfallEntityGridMapping> mm = mbg.get(h.buildGridKey(hu.getLon(), hu.getLat()));
            if (mm == null || mm.isEmpty()) continue;
            RainfallAccumulator acc = hbh.computeIfAbsent(ht, k -> new RainfallAccumulator());
            for (int i = 0; i < mm.size(); i++) acc.add(hu.getRainfall());
        }
        Map<LocalDateTime, Double> hrm = new HashMap<>(); hbh.forEach((k, v) -> hrm.put(k, h.roundRainfall(v.average())));

        Map<GridLocation, Integer> fgac = h.buildForecastGridAssignmentCount(valid);
        List<RainfallForecastRasterUnit> fus = h.listForecastUnitsByLocations(new ArrayList<>(fgac.keySet()), startTime, endTime);
        Map<LocalDateTime, RainfallAccumulator> fbh = new HashMap<>();
        for (RainfallForecastRasterUnit fu : fus) {
            if (fu.getForecastTime() == null) continue;
            LocalDateTime ist = h.toLocalDateTime(fu.getForecastTime()).truncatedTo(ChronoUnit.HOURS);
            if (ist.isBefore(startTime) || !ist.isBefore(endTime)) continue;
            Integer ac = fgac.get(new GridLocation(fu.getLon(), fu.getLat()));
            if (ac == null || ac <= 0) continue;
            double hourlyRainfall = h.defaultValue(fu.getRainfall()) / 3D;
            for (int offset = 0; offset < 3; offset++) {
                LocalDateTime ht = ist.plusHours(offset);
                if (ht.isBefore(startTime) || !ht.isBefore(endTime)) continue;
                RainfallAccumulator acc = fbh.computeIfAbsent(ht, k -> new RainfallAccumulator());
                for (int i = 0; i < ac; i++) acc.add(hourlyRainfall);
            }
        }
        Map<LocalDateTime, Double> frm = new HashMap<>(); fbh.forEach((k, v) -> frm.put(k, h.roundRainfall(v.average())));

        List<RainfallHourlySeriesVo.HourlyRainfallItem> hourlySeries = new ArrayList<>();
        int hhc = 0, fhc = 0, mhc = 0; double tr = 0D;
        for (LocalDateTime ht = startTime; ht.isBefore(endTime); ht = ht.plusHours(1)) {
            RainfallHourlySeriesVo.HourlyRainfallItem item = new RainfallHourlySeriesVo.HourlyRainfallItem();
            item.setHourTime(ht);
            Double hv = hrm.get(ht);
            if (hv != null) { item.setRainfall(hv); item.setSource(RainfallStatisticHelper.SOURCE_HISTORY); hhc++; tr += hv; hourlySeries.add(item); continue; }
            Double fv = frm.get(ht);
            if (fv != null) { item.setRainfall(fv); item.setSource(RainfallStatisticHelper.SOURCE_FORECAST_FALLBACK); fhc++; tr += fv; hourlySeries.add(item); continue; }
            item.setRainfall(null); item.setSource(RainfallStatisticHelper.SOURCE_MISSING); mhc++; hourlySeries.add(item);
        }
        RainfallHourlySeriesVo result = new RainfallHourlySeriesVo();
        result.setStartTime(startTime); result.setEndTime(endTime); result.setHourlySeries(hourlySeries);
        result.setTotalRainfall(h.roundRainfall(tr)); result.setHistoryHourCount(hhc);
        result.setForecastFallbackHourCount(fhc); result.setMissingHourCount(mhc);
        return result;
    }

    private RainfallHourlySeriesVo buildDailySeriesByMappings(
        List<RainfallEntityGridMapping> mappings, LocalDateTime startTime, LocalDateTime endTime) {
        List<RainfallEntityGridMapping> valid = mappings.stream().filter(h::hasResolvedGrid).toList();
        if (valid.isEmpty()) throw new ServiceException("未匹配到有效栅格映射");
        Map<String, List<RainfallEntityGridMapping>> mbg = h.groupMappingsByGrid(valid);
        List<RainfallLogRasterUnit> hus = h.listRasterUnitsByMappings(valid, startTime, endTime);
        Map<LocalDate, RainfallAccumulator> dm = new HashMap<>();
        for (RainfallLogRasterUnit hu : hus) {
            if (hu.getLogTime() == null) continue;
            List<RainfallEntityGridMapping> mm = mbg.get(h.buildGridKey(hu.getLon(), hu.getLat()));
            if (mm == null || mm.isEmpty()) continue;
            LocalDate day = h.toLocalDateTime(hu.getLogTime()).toLocalDate();
            RainfallAccumulator acc = dm.computeIfAbsent(day, k -> new RainfallAccumulator());
            for (int i = 0; i < mm.size(); i++) acc.add(hu.getRainfall());
        }
        List<RainfallHourlySeriesVo.HourlyRainfallItem> dailySeries = new ArrayList<>();
        int hdc = 0, mdc = 0; double tr = 0D;
        for (LocalDate day = startTime.toLocalDate(); day.isBefore(endTime.toLocalDate()); day = day.plusDays(1)) {
            RainfallHourlySeriesVo.HourlyRainfallItem item = new RainfallHourlySeriesVo.HourlyRainfallItem();
            item.setHourTime(day.atStartOfDay());
            RainfallAccumulator acc = dm.get(day);
            if (acc != null) { double rf = h.roundRainfall(acc.average()); item.setRainfall(rf); item.setSource(RainfallStatisticHelper.SOURCE_HISTORY); hdc++; tr += rf; }
            else { item.setRainfall(null); item.setSource(RainfallStatisticHelper.SOURCE_MISSING); mdc++; }
            dailySeries.add(item);
        }
        RainfallHourlySeriesVo result = new RainfallHourlySeriesVo();
        result.setStartTime(startTime); result.setEndTime(endTime); result.setHourlySeries(dailySeries);
        result.setTotalRainfall(h.roundRainfall(tr)); result.setHistoryHourCount(hdc);
        result.setForecastFallbackHourCount(0); result.setMissingHourCount(mdc);
        return result;
    }

    private List<RainfallLogSlopeUnitStatisticVo> buildActualStatistics(
        List<RainfallEntityGridMapping> mappings, LocalDate startDate, LocalDate endDate, String slopeUnitName) {
        if (mappings == null || mappings.isEmpty()) return new ArrayList<>();
        List<RainfallEntityGridMapping> valid = mappings.stream().filter(h::hasResolvedGrid).toList();
        if (valid.isEmpty()) return new ArrayList<>();
        List<RainfallLogRasterUnit> rus = h.listRasterUnitsByMappings(valid, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (rus.isEmpty()) return new ArrayList<>();
        Map<String, List<RainfallEntityGridMapping>> mbg = h.groupMappingsByGrid(valid);
        Map<LocalDate, DailyActualAccumulator> am = new HashMap<>();
        for (RainfallLogRasterUnit ru : rus) {
            LocalDateTime lt = h.toLocalDateTime(ru.getLogTime());
            List<RainfallEntityGridMapping> mm = mbg.get(h.buildGridKey(ru.getLon(), ru.getLat()));
            if (mm == null || mm.isEmpty()) continue;
            for (RainfallEntityGridMapping m : mm) {
                LocalDate sd = lt.toLocalDate();
                DailyActualAccumulator acc = am.computeIfAbsent(sd, k -> new DailyActualAccumulator(m, sd));
                acc.add(lt, ru.getRainfall());
            }
        }
        if (am.isEmpty()) return new ArrayList<>();
        List<RainfallLogSlopeUnitStatisticVo> result = new ArrayList<>(am.size());
        for (Map.Entry<LocalDate, DailyActualAccumulator> e : am.entrySet()) {
            DailyActualAccumulator acc = e.getValue();
            RainfallLogSlopeUnitStatisticVo vo = new RainfallLogSlopeUnitStatisticVo();
            vo.setSlopeUnitId(acc.mapping.getSlopeUnitId()); vo.setSlopeUnitName(slopeUnitName);
            vo.setStatDate(e.getKey()); vo.setHourlyRainfall(h.roundRainfall(acc.latestHourRainfall));
            vo.setDailyCumulativeRainfall(h.roundRainfall(acc.dailyCumulativeRainfall));
            vo.setLastHourTime(acc.latestHourTime);
            vo.setProvince(acc.mapping.getProvince()); vo.setCity(acc.mapping.getCity());
            vo.setCounty(acc.mapping.getCounty()); vo.setStreet(acc.mapping.getStreet());
            vo.setVillage(acc.mapping.getVillage()); vo.setCommunity(acc.mapping.getCommunity());
            vo.setProvinceCode(acc.mapping.getProvinceCode()); vo.setCityCode(acc.mapping.getCityCode());
            vo.setCountyCode(acc.mapping.getCountyCode()); vo.setStreetCode(acc.mapping.getStreetCode());
            vo.setVillageCode(acc.mapping.getVillageCode()); vo.setUpdateTime(acc.mapping.getUpdateTime());
            result.add(vo);
        }
        result.sort(Comparator.comparing(RainfallLogSlopeUnitStatisticVo::getStatDate).reversed()
            .thenComparing(RainfallLogSlopeUnitStatisticVo::getLastHourTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<SlopeUnitDailyRainfallSeriesVo> buildSlopeUnitDailyStatistics(
        List<RainfallEntityGridMapping> mappings, LocalDate startDate, LocalDate endDate, LocalDate forecastStartDate) {
        if (mappings == null || mappings.isEmpty()) return new ArrayList<>();
        List<RainfallEntityGridMapping> valid = mappings.stream().filter(h::hasResolvedGrid)
            .filter(m -> StringUtils.isNotBlank(m.getSlopeUnitId())).toList();
        if (valid.isEmpty()) return new ArrayList<>();
        Map<String, List<SlopeUnitDailyRainfallVo>> arm = buildSlopeUnitActualRainfallMap(valid, startDate, endDate);
        Map<String, List<RainfallForecastDailyStatisticVo>> frm = buildSlopeUnitForecastRainfallMap(valid, forecastStartDate);
        List<String> ids = valid.stream().map(RainfallEntityGridMapping::getSlopeUnitId).distinct().sorted().toList();
        List<SlopeUnitDailyRainfallSeriesVo> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            SlopeUnitDailyRainfallSeriesVo sv = new SlopeUnitDailyRainfallSeriesVo();
            sv.setSlopeUnitId(id); sv.setDailyRainfallList(arm.getOrDefault(id, new ArrayList<>()));
            sv.setDailyForcastList(frm.getOrDefault(id, h.initForecastStatistics(forecastStartDate)));
            result.add(sv);
        }
        return result;
    }

    private Map<String, List<SlopeUnitDailyRainfallVo>> buildSlopeUnitActualRainfallMap(
        List<RainfallEntityGridMapping> valid, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return new HashMap<>();
        List<RainfallLogRasterUnit> rus = h.listRasterUnitsByMappings(valid, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (rus.isEmpty()) return new HashMap<>();
        Map<String, List<RainfallEntityGridMapping>> mbg = h.groupMappingsByGrid(valid);
        Map<String, DailyActualAccumulator> am = new HashMap<>();
        for (RainfallLogRasterUnit ru : rus) {
            LocalDateTime lt = h.toLocalDateTime(ru.getLogTime());
            List<RainfallEntityGridMapping> mm = mbg.get(h.buildGridKey(ru.getLon(), ru.getLat()));
            if (mm == null || mm.isEmpty()) continue;
            for (RainfallEntityGridMapping m : mm) {
                LocalDate sd = lt.toLocalDate();
                DailyActualAccumulator acc = am.computeIfAbsent(h.buildSlopeUnitDailyKey(m.getSlopeUnitId(), sd), k -> new DailyActualAccumulator(m, sd));
                acc.add(lt, ru.getRainfall());
            }
        }
        if (am.isEmpty()) return new HashMap<>();
        Map<String, List<SlopeUnitDailyRainfallVo>> drm = new HashMap<>();
        for (DailyActualAccumulator acc : am.values()) {
            SlopeUnitDailyRainfallVo vo = new SlopeUnitDailyRainfallVo();
            vo.setStatDate(acc.statDate); vo.setDailyCumulativeRainfall(h.roundRainfall(acc.dailyCumulativeRainfall));
            drm.computeIfAbsent(acc.mapping.getSlopeUnitId(), k -> new ArrayList<>()).add(vo);
        }
        drm.values().forEach(l -> l.sort(Comparator.comparing(SlopeUnitDailyRainfallVo::getStatDate).reversed()));
        return drm;
    }

    private Map<String, List<RainfallForecastDailyStatisticVo>> buildSlopeUnitForecastRainfallMap(
        List<RainfallEntityGridMapping> valid, LocalDate startDate) {
        List<RainfallForecastRasterUnit> refs = h.listForecastReferenceCells();
        if (refs.isEmpty()) return new HashMap<>();
        Map<String, GridLocation> sfgm = new HashMap<>();
        for (RainfallEntityGridMapping m : valid) {
            if (h.hasResolvedForecastGrid(m)) {
                sfgm.put(m.getSlopeUnitId(), new GridLocation(m.getForecastGridLon(), m.getForecastGridLat()));
                continue;
            }
            double[] cp = h.resolveCenterPoint(m); if (cp == null) continue;
            RainfallForecastRasterUnit sel = h.selectLeftLowerForecastCell(refs, cp[0], cp[1]);
            if (sel == null || sel.getLon() == null || sel.getLat() == null) continue;
            sfgm.put(m.getSlopeUnitId(), new GridLocation(sel.getLon(), sel.getLat()));
        }
        if (sfgm.isEmpty()) return new HashMap<>();
        List<RainfallForecastRasterUnit> fus = h.listForecastUnitsByLocations(
            sfgm.values().stream().distinct().toList(), startDate.atStartOfDay(), startDate.plusDays(RainfallStatisticHelper.FORECAST_DAYS).atStartOfDay());
        Map<GridLocation, Map<LocalDate, Double>> fdtm = new HashMap<>();
        for (RainfallForecastRasterUnit fu : fus) {
            if (fu.getForecastTime() == null || fu.getLon() == null || fu.getLat() == null) continue;
            fdtm.computeIfAbsent(new GridLocation(fu.getLon(), fu.getLat()), k -> new HashMap<>())
                .merge(h.toLocalDateTime(fu.getForecastTime()).toLocalDate(), h.defaultValue(fu.getRainfall()), Double::sum);
        }
        Map<String, List<RainfallForecastDailyStatisticVo>> result = new HashMap<>();
        for (Map.Entry<String, GridLocation> e : sfgm.entrySet()) {
            List<RainfallForecastDailyStatisticVo> stats = h.initForecastStatistics(startDate);
            Map<LocalDate, Double> fdm = fdtm.getOrDefault(e.getValue(), new HashMap<>());
            for (RainfallForecastDailyStatisticVo s : stats) s.setDailyForecastRainfall(h.roundRainfall(fdm.getOrDefault(s.getStatDate(), 0D)));
            result.put(e.getKey(), stats);
        }
        return result;
    }

    private List<RainfallForecastDailyStatisticVo> calculateForecastStatisticsByCenters(List<double[]> cps, LocalDate startDate) {
        List<RainfallForecastDailyStatisticVo> result = h.initForecastStatistics(startDate);
        if (cps == null || cps.isEmpty()) return result;
        double maxLon = cps.stream().mapToDouble(i -> i[0]).max().orElse(Double.MAX_VALUE);
        double maxLat = cps.stream().mapToDouble(i -> i[1]).max().orElse(Double.MAX_VALUE);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = startDate.plusDays(RainfallStatisticHelper.FORECAST_DAYS).atStartOfDay();
        List<RainfallForecastRasterUnit> forecastCells = h.selectForecastCellsByBounds(startTime, endTime, maxLon, maxLat);
        if (forecastCells.isEmpty()) return result;

        Map<LocalDateTime, List<RainfallForecastRasterUnit>> cft = new HashMap<>();
        for (RainfallForecastRasterUnit c : forecastCells) {
            if (c.getForecastTime() == null) continue;
            cft.computeIfAbsent(h.toLocalDateTime(c.getForecastTime()), k -> new ArrayList<>()).add(c);
        }
        Map<LocalDate, Double> drm = new HashMap<>();
        for (RainfallForecastDailyStatisticVo item : result) drm.put(item.getStatDate(), 0D);
        for (Map.Entry<LocalDateTime, List<RainfallForecastRasterUnit>> e : cft.entrySet()) {
            LocalDate sd = e.getKey().toLocalDate();
            if (!drm.containsKey(sd)) continue;
            RainfallAccumulator acc = new RainfallAccumulator();
            for (double[] cp : cps) {
                RainfallForecastRasterUnit sel = h.selectLeftLowerForecastCell(e.getValue(), cp[0], cp[1]);
                acc.add(sel == null ? 0D : sel.getRainfall());
            }
            drm.put(sd, h.roundRainfall(drm.get(sd) + acc.average()));
        }
        for (RainfallForecastDailyStatisticVo item : result) item.setDailyForecastRainfall(h.roundRainfall(drm.getOrDefault(item.getStatDate(), 0D)));
        return result;
    }

    private List<AdRegionVo> listAdRegionsByQuery(RainfallLogSlopeUnitStatisticQueryBo bo) {
        AdRegionBo query = new AdRegionBo();
        Integer tl = h.resolveAdRegionTargetLevel(bo);
        query.setLevel(tl);
        query.setProvince(bo.getProvince()); query.setCity(bo.getCity()); query.setCounty(bo.getCounty());
        query.setStreet(bo.getStreet()); query.setVillage(bo.getVillage());
        query.setId(resolveAdRegionTargetCode(bo, tl));
        if (tl != null) {
            switch (tl) {
                case 1 -> query.setName(bo.getProvince()); case 2 -> query.setName(bo.getCity());
                case 3 -> query.setName(bo.getCounty()); case 4 -> query.setName(bo.getStreet());
                case 5 -> query.setName(bo.getVillage());
            }
        }
        return adRegionService.queryList(query).stream().filter(item -> StringUtils.isNotBlank(item.getCenter())).toList();
    }

    private String resolveAdRegionTargetCode(RainfallLogSlopeUnitStatisticQueryBo bo, Integer tl) {
        if (tl == null) return null;
        return switch (tl) {
            case 1 -> bo.getProvinceCode(); case 2 -> bo.getCityCode(); case 3 -> bo.getCountyCode();
            case 4 -> bo.getStreetCode(); case 5 -> bo.getVillageCode(); default -> null;
        };
    }
}
