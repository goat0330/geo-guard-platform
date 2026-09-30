/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.orchestrator;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeomorphologicalVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFaultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFoldVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataWaterSystemGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HousePointMatchVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceStatByHazardVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PointHouseInfoVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataGeologicalFaultService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataGeologicalFoldService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataGeomorphologicalService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataHouseService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataLandPlanningService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataPersonService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadGeologyService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadService;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.IHouseSlopeService;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorDeviceService;
import cn.edu.pku.whai.geological.disaster.data.service.IPersonSlopeService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.service.IWaterSystemGeologyService;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.GeologyPointInfoVo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 地质空间查询编排层
 *
 * @author zhuzc
 * @date 2026-06-09
 */
@Slf4j
@Component
public class GeologyInfoOrchestrator {

    /**
     * 点查询容差半径（米）
     */
    private static final double HOUSE_PERSON_POINT_RADIUS_METERS = 10.0d;

    private final Executor pointQueryExecutor;
    private final ISlopeUnitService slopeUnitService;
    private final IGeologyInfoService geologyInfoService;
    private final IWaterSystemGeologyService waterSystemService;
    private final IDataRoadGeologyService roadService;
    private final IHazardPointService hazardPointService;
    private final IHouseSlopeService houseSlopeService;
    private final IPersonSlopeService personSlopeService;
    private final IDataRoadService dataRoadService;
    private final IDataHouseService dataHouseService;
    private final IDataPersonService dataPersonService;
    private final IDataGeomorphologicalService dataGeomorphologicalService;

    private final IMonitorDeviceService monitorDeviceService;

    private final IDataGeologicalFoldService dataGeologicalFoldService;
    private final IDataGeologicalFaultService dataGeologicalFaultService;
    private final IDataLandPlanningService dataLandPlanningService;

    public GeologyInfoOrchestrator(@Qualifier("geologyPointQueryExecutor") Executor pointQueryExecutor,
                                   ISlopeUnitService slopeUnitService,
                                   IGeologyInfoService geologyInfoService,
                                   IWaterSystemGeologyService waterSystemService,
                                   IDataRoadGeologyService roadService,
                                   IHazardPointService hazardPointService,
                                   IHouseSlopeService houseSlopeService,
                                   IPersonSlopeService personSlopeService,
                                   IDataRoadService dataRoadService,
                                   IDataHouseService dataHouseService,
                                   IDataPersonService dataPersonService,
                                   IDataGeomorphologicalService dataGeomorphologicalService,
                                   IMonitorDeviceService monitorDeviceService,
                                   IDataGeologicalFoldService dataGeologicalFoldService,
                                   IDataGeologicalFaultService dataGeologicalFaultService,
                                   IDataLandPlanningService dataLandPlanningService) {
        this.pointQueryExecutor = pointQueryExecutor;
        this.slopeUnitService = slopeUnitService;
        this.geologyInfoService = geologyInfoService;
        this.waterSystemService = waterSystemService;
        this.roadService = roadService;
        this.hazardPointService = hazardPointService;
        this.houseSlopeService = houseSlopeService;
        this.personSlopeService = personSlopeService;
        this.dataRoadService = dataRoadService;
        this.dataHouseService = dataHouseService;
        this.dataPersonService = dataPersonService;
        this.dataGeomorphologicalService = dataGeomorphologicalService;
        this.monitorDeviceService = monitorDeviceService;
        this.dataGeologicalFoldService = dataGeologicalFoldService;
        this.dataGeologicalFaultService = dataGeologicalFaultService;
        this.dataLandPlanningService = dataLandPlanningService;
    }

    /**
     * 点查询聚合
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 点查询结果
     */
    public GeologyPointInfoVo query(Double lon, Double lat) {
        GeologyPointInfoVo vo = new GeologyPointInfoVo();
        vo.setLongitude(lon);
        vo.setLatitude(lat);

        CompletableFuture<GeologyAdRegionVo> adRegionFuture = CompletableFuture.supplyAsync(
            () -> timed("点位行政区划", () -> queryPointAdRegion(lon, lat)), pointQueryExecutor);
        CompletableFuture<PointHouseInfoResult> houseFuture = CompletableFuture.supplyAsync(
            () -> timed("房屋点", () -> queryPointHouseInfo(lon, lat)), pointQueryExecutor);
        CompletableFuture<GeomorphologyResult> geomorphologyFuture = CompletableFuture.supplyAsync(
            () -> timed("地貌", () -> queryGeomorphology(lon, lat)), pointQueryExecutor);
        CompletableFuture<DataGeologicalFoldVo> foldFuture = CompletableFuture.supplyAsync(
            () -> timed("褶皱", () -> queryGeologicalFold(lon, lat)), pointQueryExecutor);
        CompletableFuture<DataGeologicalFaultVo> faultFuture = CompletableFuture.supplyAsync(
            () -> timed("断层", () -> queryGeologicalFault(lon, lat)), pointQueryExecutor);
        CompletableFuture<DataWaterSystemGeologyVo> waterFuture = CompletableFuture.supplyAsync(
            () -> timed("水系", () -> queryWaterSystem(lon, lat)), pointQueryExecutor);
        CompletableFuture<String> pointLandPlanningFuture = CompletableFuture.supplyAsync(
            () -> timed("点位用地类型", () -> queryPointLandPlanning(lon, lat)), pointQueryExecutor);

        await(adRegionFuture, houseFuture, geomorphologyFuture, foldFuture, faultFuture, waterFuture,
            pointLandPlanningFuture);
        mergeFirstStagePointInfo(
            vo,
            adRegionFuture.join(),
            houseFuture.join(),
            geomorphologyFuture.join(),
            foldFuture.join(),
            faultFuture.join(),
            waterFuture.join(),
            pointLandPlanningFuture.join()
        );

        if (waterFuture.join() != null) {
            stripPointGeometry(vo);
            return vo;
        }

        CompletableFuture<SlopeUnit> slopeUnitFuture = CompletableFuture.supplyAsync(
            () -> timed("斜坡单元命中", () -> queryMatchedSlopeUnit(lon, lat)), pointQueryExecutor);
        CompletableFuture<SlopeUnitGeologyVo> slopeUnitGeologyFuture = slopeUnitFuture.thenApplyAsync(
            slopeUnit -> slopeUnit == null ? null : timed(
                "斜坡单元地质",
                () -> querySlopeUnitGeology(slopeUnit.getId())),
            pointQueryExecutor);
        CompletableFuture<GeologyInfoVo> slopeUnitWktGeologyFuture = slopeUnitFuture.thenApplyAsync(
            slopeUnit -> slopeUnit == null || StringUtils.isBlank(slopeUnit.getWkt()) ? null : timed(
                "斜坡单元范围地质",
                () -> querySlopeUnitWktGeology(slopeUnit.getWkt())),
            pointQueryExecutor);
        CompletableFuture<SlopeUnitVo> slopeUnitVoFuture = slopeUnitFuture.thenApplyAsync(
            slopeUnit -> slopeUnit == null ? null : timed(
                "斜坡单元统计",
                () -> querySlopeUnitVo(slopeUnit.getId())),
            pointQueryExecutor);
        CompletableFuture<RoadStatisticsResult> slopeUnitRoadFuture = slopeUnitFuture.thenApplyAsync(
            slopeUnit -> timed("斜坡单元道路统计", () -> querySlopeUnitRoadInfo(slopeUnit)),
            pointQueryExecutor);
        CompletableFuture<List<LandPlanningVo>> slopeUnitLandPlanningFuture = slopeUnitFuture.thenApplyAsync(
            slopeUnit -> timed("斜坡单元用地类型统计", () -> querySlopeUnitLandPlanningStatistics(slopeUnit)),
            pointQueryExecutor);
        CompletableFuture<String> roadFuture = CompletableFuture.supplyAsync(
            () -> timed("最近道路", () -> queryMatchedRoadName(lon, lat)), pointQueryExecutor);
        CompletableFuture<HazardPointVo> hazardPointFuture = CompletableFuture.supplyAsync(
            () -> timed("隐患点", () -> queryMatchedHazardPoint(lon, lat)), pointQueryExecutor);

        await(slopeUnitGeologyFuture, slopeUnitWktGeologyFuture, slopeUnitVoFuture,
            slopeUnitRoadFuture, slopeUnitLandPlanningFuture, roadFuture, hazardPointFuture);
        mergeSecondStagePointInfo(
            vo,
            buildSlopeUnitInfo(slopeUnitFuture.join(), slopeUnitGeologyFuture.join(),
                slopeUnitWktGeologyFuture.join(), slopeUnitVoFuture.join()),
            roadFuture.join(),
            hazardPointFuture.join(),
            slopeUnitRoadFuture.join(),
            slopeUnitLandPlanningFuture.join()
        );
        stripPointGeometry(vo);
        return vo;
    }

    private void await(CompletableFuture<?>... futures) {
        CompletableFuture.allOf(futures).join();
    }

    private void mergeFirstStagePointInfo(GeologyPointInfoVo vo,
                                          GeologyAdRegionVo adRegion,
                                          PointHouseInfoResult houseInfo,
                                          GeomorphologyResult geomorphology,
                                          DataGeologicalFoldVo fold,
                                          DataGeologicalFaultVo fault,
                                          DataWaterSystemGeologyVo waterSystem,
                                          String pointLandPlanningType) {
        if (adRegion != null) {
            vo.setAdRegion(adRegion);
        }
        if (houseInfo != null) {
            vo.setPointHouseInfo(houseInfo.pointHouseInfo());
            vo.setHouseCountByPoint(houseInfo.houseCountByPoint());
            vo.setPersonCountByPoint(houseInfo.personCountByPoint());
        }
        if (geomorphology != null) {
            vo.setGeomorphologyType(geomorphology.type());
            vo.setGeomorphologyFeature(geomorphology.feature());
        }
        if (fold != null) {
            vo.setFold(fold);
        }
        if (fault != null) {
            vo.setFault(fault);
        }
        if (waterSystem != null) {
            vo.setWaterSystemName(waterSystem.getWaterSystemName());
        }
        if (pointLandPlanningType != null) {
            vo.setLandPlanningType(pointLandPlanningType);
        }
    }

    private void mergeSecondStagePointInfo(GeologyPointInfoVo vo,
                                           SlopeUnitInfoResult slopeUnitInfo,
                                           String roadName,
                                           HazardPointVo hazardPoint,
                                           RoadStatisticsResult roadStatistics,
                                           List<LandPlanningVo> slopeUnitLandPlanningStatistics) {
        if (slopeUnitInfo != null && slopeUnitInfo.pointInfo() != null) {
            BeanUtil.copyProperties(slopeUnitInfo.pointInfo(), vo, CopyOptions.create().setIgnoreNullValue(true));
        }
        if (roadName != null) {
            vo.setRoadName(roadName);
        }
        if (hazardPoint != null) {
            vo.setHazardPoint(hazardPoint);
        }
        if (roadStatistics != null) {
            vo.setRoadStatistics(roadStatistics.statistics());
            vo.setRoadTotalLengthMeters(roadStatistics.totalLengthMeters());
        }
        if (slopeUnitLandPlanningStatistics != null) {
            vo.setSlopeUnitLandPlanningStatistics(slopeUnitLandPlanningStatistics);
        }
    }

    private record PointHouseInfoResult(PointHouseInfoVo pointHouseInfo,
                                        Long houseCountByPoint,
                                        Long personCountByPoint) {
    }

    private record GeomorphologyResult(String type, String feature) {
    }

    private record SlopeUnitInfoResult(SlopeUnit slopeUnit, GeologyPointInfoVo pointInfo) {
    }

    private record RoadStatisticsResult(List<RoadStatisticVo> statistics, BigDecimal totalLengthMeters) {
    }

    private <T> T timed(String step, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * 斜坡单元面查询聚合
     *
     * @param id 斜坡单元ID
     * @return 面查询结果
     */
    public GeologyInfoVo queryBySlopeUnitId(String id) {
        GeologyInfoVo vo = geologyInfoService.queryBySlopeUnitId(id);
        if (vo == null) {
            vo = new GeologyInfoVo();
        }
        vo.setSlopeUnitId(id);
        SlopeUnitVo slopeUnitVo = slopeUnitService.queryById(id);
        if (slopeUnitVo != null) {
            vo.setSlopeUnitName(slopeUnitVo.getName());
            vo.setHouseCount(slopeUnitVo.getBuildingCount());
            vo.setPersonCount(slopeUnitVo.getPopulationCount());
            if (StringUtils.isBlank(vo.getScopeWkt())) {
                vo.setScopeWkt(slopeUnitVo.getWkt());
            }
        }
        vo.setSlopeUnitGeology(slopeUnitService.queryGeologyInfoById(id));
        dedupAreaGeologies(vo);
        appendSlopeUnitLandPlanningStatistics(vo, id);
        enrichAreaResult(vo, vo.getScopeWkt(), id);
        stripAreaGeometry(vo);
        return vo;
    }

    /**
     * 行政区划面查询聚合
     *
     * @param regions 行政区划名称，英文逗号分隔
     * @return 面查询结果
     */
    public GeologyInfoVo queryByRegions(String regions) {
        GeologyInfoVo vo = geologyInfoService.queryByRegions(regions);
        if (vo == null) {
            vo = new GeologyInfoVo();
        }
        dedupAreaGeologies(vo);
        vo.setStratums(new ArrayList<>());
        enrichAreaResult(vo, vo.getScopeWkt(), null);
        stripAreaGeometry(vo);
        return vo;
    }

    /**
     * 剔除点查询出参中的 wkt/geom 几何字段，避免大对象下传。
     *
     * @param vo 点查询结果
     */
    private void stripPointGeometry(GeologyPointInfoVo vo) {
        if (vo == null) {
            return;
        }
        vo.setCenter(null);
        if (vo.getHazardPoint() != null) {
            vo.getHazardPoint().setWkt(null);
        }
        if (vo.getFold() != null) {
            vo.getFold().setWkt(null);
            vo.getFold().setGeom(null);
        }
        if (vo.getFault() != null) {
            vo.getFault().setWkt(null);
            vo.getFault().setGeom(null);
        }
    }

    /**
     * 剔除面查询（斜坡单元/行政区划）出参中的 wkt/geom 几何字段，避免大对象下传。
     *
     * @param vo 面查询结果
     */
    private void stripAreaGeometry(GeologyInfoVo vo) {
        if (vo == null) {
            return;
        }
        vo.setScopeWkt(null);
        if (vo.getSlopeUnitGeology() != null) {
            vo.getSlopeUnitGeology().setCenter(null);
        }
        if (vo.getHazardPoints() != null) {
            vo.getHazardPoints().forEach(hazard -> hazard.setWkt(null));
        }
    }

    private GeologyAdRegionVo queryPointAdRegion(Double lon, Double lat) {
        try {
            return geologyInfoService.queryAdRegionByPoint(lon, lat);
        } catch (Exception e) {
            log.warn("点位行政区划查询失败: lon={}, lat={}", lon, lat, e);
        }
        return null;
    }

    private PointHouseInfoResult queryPointHouseInfo(Double lon, Double lat) {
        try {
            HousePointMatchVo matchedHouse = timed("房屋点-匹配房屋", () -> selectPointMatchedHouse(lon, lat));
            if (matchedHouse == null) {
                return new PointHouseInfoResult(null, 0L, 0L);
            }
            Long personCount = timed("房屋点-统计人员", () -> dataPersonService.countByBuildingCode(matchedHouse.getBuildingCode()));
            PointHouseInfoVo pointHouseInfo = new PointHouseInfoVo();
            pointHouseInfo.setHouseId(matchedHouse.getHouseId());
            pointHouseInfo.setHouseUnitId(matchedHouse.getHouseUnitId());
            pointHouseInfo.setBuildingCode(matchedHouse.getBuildingCode());
            pointHouseInfo.setBuildingName(matchedHouse.getBuildingName());
            pointHouseInfo.setPersonCount(personCount);
            return new PointHouseInfoResult(pointHouseInfo, 1L, personCount);
        } catch (Exception e) {
            log.warn("房屋点查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    /**
     * 查询经纬度点命中的地貌信息。
     * 命中失败不影响整体返回。
     *
     * @param lon 经度
     * @param lat 纬度
     */
    private GeomorphologyResult queryGeomorphology(Double lon, Double lat) {
        try {
            DataGeomorphologicalVo geomorphology = dataGeomorphologicalService
                .queryByContainingPointNearestCentroid(lon, lat);
            if (geomorphology != null) {
                return new GeomorphologyResult(geomorphology.getType(), geomorphology.getFeature());
            }
        } catch (Exception e) {
            log.warn("地貌查询失败: lon={}, lat={}", lon, lat, e);
        }
        return null;
    }

    /**
     * 查询经纬度点最近的地质褶皱。
     * 命中失败不影响整体返回。
     *
     * @param lon 经度
     * @param lat 纬度
     */
    private DataGeologicalFoldVo queryGeologicalFold(Double lon, Double lat) {
        try {
            return dataGeologicalFoldService.queryNearestByPoint(lon, lat);
        } catch (Exception e) {
            log.warn("褶皱查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    /**
     * 查询经纬度点最近的地质断层。
     * 命中失败不影响整体返回。
     *
     * @param lon 经度
     * @param lat 纬度
     */
    private DataGeologicalFaultVo queryGeologicalFault(Double lon, Double lat) {
        try {
            return dataGeologicalFaultService.queryNearestByPoint(lon, lat);
        } catch (Exception e) {
            log.warn("断层查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private DataWaterSystemGeologyVo queryWaterSystem(Double lon, Double lat) {
        try {
            return waterSystemService.queryNearestByPoint(lon, lat);
        } catch (Exception e) {
            log.warn("水系查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private String queryPointLandPlanning(Double lon, Double lat) {
        try {
            return dataLandPlanningService.queryPointLandPlanningType(lon, lat);
        } catch (Exception e) {
            log.warn("点位用地类型查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private SlopeUnit queryMatchedSlopeUnit(Double lon, Double lat) {
        try {
            return slopeUnitService.queryByPoint(lon, lat);
        } catch (Exception e) {
            log.warn("斜坡单元命中查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private SlopeUnitGeologyVo querySlopeUnitGeology(String slopeUnitId) {
        try {
            return slopeUnitService.queryGeologyInfoById(slopeUnitId);
        } catch (Exception e) {
            log.warn("斜坡单元地质查询失败: slopeUnitId={}", slopeUnitId, e);
            return null;
        }
    }

    private GeologyInfoVo querySlopeUnitWktGeology(String wkt) {
        try {
            return geologyInfoService.queryBySlopeUnitWkt(wkt);
        } catch (Exception e) {
            log.warn("斜坡单元范围地质查询失败: wkt={}", wkt, e);
            return null;
        }
    }

    private SlopeUnitVo querySlopeUnitVo(String slopeUnitId) {
        try {
            return slopeUnitService.queryById(slopeUnitId);
        } catch (Exception e) {
            log.warn("斜坡单元统计查询失败: slopeUnitId={}", slopeUnitId, e);
            return null;
        }
    }

    private SlopeUnitInfoResult buildSlopeUnitInfo(SlopeUnit slopeUnit,
                                                   SlopeUnitGeologyVo slopeUnitGeologyVo,
                                                   GeologyInfoVo geologyInfoVo,
                                                   SlopeUnitVo slopeUnitVo) {
        if (slopeUnit == null) {
            return null;
        }
        GeologyPointInfoVo slopeUnitInfo = new GeologyPointInfoVo();
        slopeUnitInfo.setSlopeUnitId(slopeUnit.getId());
        if (slopeUnitGeologyVo != null) {
            BeanUtil.copyProperties(slopeUnitGeologyVo, slopeUnitInfo, CopyOptions.create().setIgnoreNullValue(true));
            slopeUnitInfo.setSlopeUnitName(slopeUnitGeologyVo.getName());
            slopeUnitInfo.setSlopeHydrogeology(slopeUnitGeologyVo.getHydrogeology());
        }
        if (geologyInfoVo != null) {
            BeanUtil.copyProperties(geologyInfoVo, slopeUnitInfo, CopyOptions.create().setIgnoreNullValue(true));
            dedupPointGeologies(slopeUnitInfo);
        }
        if (slopeUnitVo != null) {
            slopeUnitInfo.setHouseCount(slopeUnitVo.getBuildingCount());
            slopeUnitInfo.setPersonCount(slopeUnitVo.getPopulationCount());
            if (StringUtils.isBlank(slopeUnitInfo.getSlopeUnitName())) {
                slopeUnitInfo.setSlopeUnitName(slopeUnitVo.getName());
            }
        }
        return new SlopeUnitInfoResult(slopeUnit, slopeUnitInfo);
    }

    private String queryMatchedRoadName(Double lon, Double lat) {
        try {
            DataRoadGeologyVo road = roadService.queryNearestByPoint(lon, lat);
            if (road != null) {
                return road.getRoadName();
            }
        } catch (Exception e) {
            log.warn("道路查询失败: lon={}, lat={}", lon, lat, e);
        }
        return null;
    }

    private HazardPointVo queryMatchedHazardPoint(Double lon, Double lat) {
        try {
            HazardPointVo hazardPoint = hazardPointService.queryByContainingPointNearestCenter(lon, lat);
            if (hazardPoint != null) {
                // 单点也走批量方法，保持逻辑统一
                enrichHazardPointDeviceStats(Collections.singletonList(hazardPoint));
            }
            return hazardPoint;
        } catch (Exception e) {
            log.warn("灾害点查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private RoadStatisticsResult querySlopeUnitRoadInfo(SlopeUnit slopeUnit) {
        if (slopeUnit == null || StringUtils.isBlank(slopeUnit.getId())) {
            return null;
        }
        try {
            List<RoadStatisticVo> statistics = dataRoadService.queryStatisticsBySlopeUnitId(
                slopeUnit.getId(), slopeUnit.getWkt());
            return new RoadStatisticsResult(statistics, sumRoadLengthInWkt(statistics));
        } catch (Exception e) {
            log.warn("斜坡单元道路查询失败: slopeUnitId={}", slopeUnit.getId(), e);
            return null;
        }
    }

    private List<LandPlanningVo> querySlopeUnitLandPlanningStatistics(SlopeUnit slopeUnit) {
        if (slopeUnit == null || StringUtils.isBlank(slopeUnit.getId())) {
            return List.of();
        }
        try {
            return dataLandPlanningService.querySlopeUnitLandPlanningStatistics(slopeUnit.getId());
        } catch (Exception e) {
            log.warn("斜坡单元用地类型统计查询失败: slopeUnitId={}", slopeUnit.getId(), e);
            return List.of();
        }
    }

    /**
     * 补充斜坡单元涉及的用地类型及覆盖面积统计结果。
     *
     * @param vo          面查询结果
     * @param slopeUnitId 斜坡单元ID
     */
    private void appendSlopeUnitLandPlanningStatistics(GeologyInfoVo vo, String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return;
        }
        try {
            List<LandPlanningVo> statistics = dataLandPlanningService.querySlopeUnitLandPlanningStatistics(slopeUnitId);
            if (statistics != null) {
                vo.setSlopeUnitLandPlanningStatistics(statistics);
            }
        } catch (Exception e) {
            log.warn("面查询斜坡单元用地类型统计失败: slopeUnitId={}", slopeUnitId, e);
        }
    }

    private void enrichAreaResult(GeologyInfoVo vo, String wkt, String slopeUnitId) {
        if (StringUtils.isBlank(wkt)) {
            return;
        }
        vo.setScopeWkt(wkt);
        appendAreaHouseStats(vo, wkt);
        appendAreaRoadInfo(vo, wkt, slopeUnitId);
        appendAreaHazardPoints(vo, wkt);
    }

    private void appendAreaHouseStats(GeologyInfoVo vo, String wkt) {
        try {
            List<DataHouseVo> houses = dataHouseService.queryByGeometryWkt(wkt);
            vo.setHouseCount(countDistinct(houses, DataHouseVo::getBuildingCode));
            vo.setPersonCount(dataPersonService.countByPolygonWkt(wkt));
        } catch (Exception e) {
            log.warn("面查询房屋统计失败: wkt={}", wkt, e);
        }
    }

    private void appendAreaRoadInfo(GeologyInfoVo vo, String wkt, String slopeUnitId) {
        try {
            List<RoadStatisticVo> statistics = StringUtils.isNotBlank(slopeUnitId)
                ? dataRoadService.queryStatisticsBySlopeUnitId(slopeUnitId, wkt)
                : dataRoadService.queryStatisticsByWkt(wkt);
            vo.setRoadStatistics(statistics);
            vo.setRoadTotalLengthMeters(sumRoadLengthInWkt(statistics));
        } catch (Exception e) {
            log.warn("面查询道路统计失败: slopeUnitId={}, wkt={}", slopeUnitId, wkt, e);
        }
    }

    private void appendAreaHazardPoints(GeologyInfoVo vo, String wkt) {
        try {
            List<HazardPointVo> hazardPoints = hazardPointService.queryByWkt(wkt);
            if (hazardPoints != null && !hazardPoints.isEmpty()) {
                enrichHazardPointDeviceStats(hazardPoints);
            }
            vo.setHazardPoints(hazardPoints);
        } catch (Exception e) {
            log.warn("面查询灾害点失败: wkt={}", wkt, e);
        }
    }

    /**
     * 为灾害点列表批量填充专业监测设备统计字段。
     * 一次 SQL 按灾害点ID列表聚合统计，避免 N+1 查询。
     *
     * @param hazardPoints 灾害点列表
     */
    private void enrichHazardPointDeviceStats(List<HazardPointVo> hazardPoints) {
        if (hazardPoints == null || hazardPoints.isEmpty()) {
            return;
        }
        // distinct() 不会去重 null，因此先 distinct 再二次过滤避免将 null 拼入 SQL IN
        List<String> disasterIds = hazardPoints.stream()
            .map(HazardPointVo::getUniqueDisasterId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        // distinct 之后的 null（如果有）过滤掉
        disasterIds.removeIf(id -> !StringUtils.isNotBlank(id));
        if (disasterIds.isEmpty()) {
            return;
        }
        try {
            Map<String, MonitorDeviceStatByHazardVo> statMap = monitorDeviceService
                .statByDisasterIds(disasterIds)
                .stream()
                .collect(Collectors.toMap(
                    MonitorDeviceStatByHazardVo::getDisasterId,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
            hazardPoints.forEach(hp -> {
                String disasterId = hp.getUniqueDisasterId();
                if (StringUtils.isBlank(disasterId)) {
                    hp.setProfessionalDeviceTotal(0L);
                    hp.setGnssCount(0L);
                    hp.setCrackMeterCount(0L);
                    return;
                }
                MonitorDeviceStatByHazardVo stat = statMap.get(disasterId);
                if (stat != null) {
                    hp.setProfessionalDeviceTotal(stat.getProfessionalDeviceTotal());
                    hp.setGnssCount(stat.getGnssCount());
                    hp.setCrackMeterCount(stat.getCrackMeterCount());
                } else {
                    hp.setProfessionalDeviceTotal(0L);
                    hp.setGnssCount(0L);
                    hp.setCrackMeterCount(0L);
                }
            });
        } catch (Exception e) {
            log.warn("灾害点设备统计填充失败: disasterIds={}", disasterIds, e);
        }
    }

    private HousePointMatchVo selectPointMatchedHouse(Double lon, Double lat) {
        // 1. 先按 10m 缓冲匹配房屋
        List<HousePointMatchVo> bufferedMatches = timed("房屋点-10m缓冲匹配", () -> houseSlopeService.queryPointMatches(
            lon, lat, HOUSE_PERSON_POINT_RADIUS_METERS, true));
        if (bufferedMatches == null || bufferedMatches.isEmpty()) {
            return null;
        }
        if (bufferedMatches.size() == 1) {
            return bufferedMatches.getFirst();
        }
        // 2. 缓冲命中多个时,优先采用面内(ST_Intersects)精确匹配
        List<HousePointMatchVo> exactMatches = timed("房屋点-面内精确匹配", () -> houseSlopeService.queryPointMatches(
            lon, lat, HOUSE_PERSON_POINT_RADIUS_METERS, false));
        if (exactMatches == null || exactMatches.isEmpty()) {
            return null;
        }
        return exactMatches.getFirst();
    }

    private void dedupPointGeologies(GeologyPointInfoVo vo) {
        vo.setEngineeringGeologies(dedupList(
            vo.getEngineeringGeologies(),
            item -> item.getRockGroup() + "|" + item.getProjectName()
        ));
        vo.setHydroGeologies(dedupList(
            vo.getHydroGeologies(),
            item -> item.getAquiferRock() + "|" + item.getAquiferType()
        ));
        vo.setStratums(dedupList(
            vo.getStratums(),
            item -> item.getCode() + "|" + item.getLabel()
        ));
    }

    private void dedupAreaGeologies(GeologyInfoVo vo) {
        vo.setEngineeringGeologies(dedupList(
            vo.getEngineeringGeologies(),
            item -> item.getRockGroup() + "|" + item.getProjectName()
        ));
        vo.setHydroGeologies(dedupList(
            vo.getHydroGeologies(),
            item -> item.getAquiferRock() + "|" + item.getAquiferType()
        ));
        vo.setStratums(dedupList(
            vo.getStratums(),
            item -> item.getCode() + "|" + item.getLabel()
        ));
    }

    private <T> List<T> dedupList(List<T> source, Function<T, String> keyFunction) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        return source.stream()
            .filter(item -> item != null)
            .collect(Collectors.toMap(
                keyFunction,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private <T> long countDistinct(List<T> source, Function<T, String> keyFunction) {
        if (source == null || source.isEmpty()) {
            return 0L;
        }
        return source.stream()
            .map(keyFunction)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .count();
    }

    private BigDecimal sumRoadLengthInWkt(List<RoadStatisticVo> statistics) {
        if (statistics == null || statistics.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return statistics.stream()
            .map(RoadStatisticVo::getLengthInWkt)
            .filter(item -> item != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
