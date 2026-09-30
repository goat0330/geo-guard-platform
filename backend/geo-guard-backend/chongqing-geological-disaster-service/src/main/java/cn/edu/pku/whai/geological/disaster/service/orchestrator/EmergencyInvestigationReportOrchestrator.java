/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.orchestrator;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallHourlySeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadService;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallLogSlopeUnitStatisticService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EmergencyHistoricalRainfallConverter;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EmergencyInvestigationReportVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.GeologyPointInfoVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 事件应急调查报告编排层
 *
 * @author zhuzc
 * @date 2026-06-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyInvestigationReportOrchestrator {

    /**
     * 小时雨量时间格式
     */
    private static final DateTimeFormatter HOUR_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final GeologyInfoOrchestrator geologyInfoOrchestrator;
    private final ISlopeUnitService slopeUnitService;
    private final IRainfallLogSlopeUnitStatisticService rainfallLogSlopeUnitStatisticService;
    private final IDataRoadService dataRoadService;

    /**
     * 根据处置ID查询应急调查报告聚合信息。
     *
     * @param handleId 处置ID
     * @return 应急调查报告聚合视图
     */
    public EmergencyInvestigationReportVo queryByHandleId(Long handleId) {
        if (handleId == null) {
            throw new ServiceException("handleId不能为空");
        }
        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        if (sceneRecord == null) {
            throw new ServiceException("未查询到处置任务现场记录");
        }
        if (StringUtils.isBlank(sceneRecord.getDisasterCoordinates())) {
            throw new ServiceException("现场记录缺少灾害点坐标");
        }
        if (sceneRecord.getOccurrenceTime() == null) {
            throw new ServiceException("现场记录缺少发生时间");
        }

        double[] coordinates = GeoDistanceUtil.parsePoint(sceneRecord.getDisasterCoordinates());
        if (coordinates == null || coordinates.length < 2) {
            throw new ServiceException("灾害点坐标格式非法");
        }

        String hazardExtent = sceneRecord.getHazardExtent();
        String riskExtent = sceneRecord.getRiskExtent();

        EmergencyInvestigationReportVo vo = new EmergencyInvestigationReportVo();

        appendGeologyInfo(vo, coordinates[0], coordinates[1]);
        appendHistoricalRainfall(vo, sceneRecord.getOccurrenceTime(), vo.getSlopeUnitId());
        appendRoadStatistics(vo, hazardExtent, riskExtent);
        hideGeometryFields(sceneRecord);
        vo.setSceneRecord(sceneRecord);
        return vo;
    }

    /**
     * 基于灾害点补充地质信息。
     *
     * @param vo        应急调查报告聚合视图
     * @param longitude 经度
     * @param latitude  纬度
     * @return 无
     */
    private void appendGeologyInfo(EmergencyInvestigationReportVo vo, double longitude, double latitude) {
        GeologyPointInfoVo pointInfo = geologyInfoOrchestrator.query(longitude, latitude);
        if (pointInfo == null) {
            return;
        }
        vo.setSlopeUnitId(pointInfo.getSlopeUnitId());
        vo.setSlopeUnitName(pointInfo.getSlopeUnitName());
        vo.setEngineeringGeologies(pointInfo.getEngineeringGeologies());
        vo.setHydroGeologies(pointInfo.getHydroGeologies());
        vo.setStratums(pointInfo.getStratums());
        vo.setAdRegion(pointInfo.getAdRegion());
        if (StringUtils.isNotBlank(pointInfo.getSeismicIntensityZoning())) {
            vo.setSeismicIntensityZoning(pointInfo.getSeismicIntensityZoning());
        }
        if (StringUtils.isNotBlank(pointInfo.getSeismicPeakGroundAccelerationZoning())) {
            vo.setSeismicPeakGroundAccelerationZoning(pointInfo.getSeismicPeakGroundAccelerationZoning());
        }
        if (StringUtils.isBlank(pointInfo.getSlopeUnitId())) {
            return;
        }
        try {
            SlopeUnitGeologyVo slopeUnitGeology = slopeUnitService.queryGeologyInfoById(pointInfo.getSlopeUnitId());
            vo.setSlopeUnitGeology(slopeUnitGeology);
            if (slopeUnitGeology != null && StringUtils.isBlank(vo.getSlopeUnitName())) {
                vo.setSlopeUnitName(slopeUnitGeology.getName());
            }
        } catch (Exception e) {
            log.warn("查询斜坡单元调查信息失败: slopeUnitId={}", pointInfo.getSlopeUnitId(), e);
        }
    }

    /**
     * 追加历史降雨信息。
     *
     * @param vo             应急调查报告聚合视图
     * @param occurrenceTime 灾害发生时间
     * @param slopeUnitId    斜坡单元ID
     * @return 无
     */
    private void appendHistoricalRainfall(EmergencyInvestigationReportVo vo, java.util.Date occurrenceTime, String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId) || occurrenceTime == null) {
            return;
        }
        try {
            LocalDateTime endTime = LocalDateTime.ofInstant(occurrenceTime.toInstant(), ZoneId.systemDefault());
            LocalDateTime startTime = endTime.minusHours(7L * 24L);
            RainfallLogSlopeUnitStatisticQueryBo queryBo = new RainfallLogSlopeUnitStatisticQueryBo();
            queryBo.setSlopeUnitId(slopeUnitId);
            queryBo.setStartTime(startTime.format(HOUR_TIME_FORMATTER));
            queryBo.setEndTime(endTime.format(HOUR_TIME_FORMATTER));
            RainfallHourlySeriesVo rainfall = rainfallLogSlopeUnitStatisticService.queryHourlyRainfallBySlopeUnit(queryBo);
            vo.setHistoricalRainfall(EmergencyHistoricalRainfallConverter.convert(rainfall));
        } catch (Exception e) {
            log.warn("查询历史降雨失败: slopeUnitId={}", slopeUnitId, e);
        }
    }

    /**
     * 追加危险范围和风险范围的道路统计信息。
     *
     * @param vo          应急调查报告聚合视图
     * @param sceneRecord 现场记录
     * @return 无
     */
    private void appendRoadStatistics(EmergencyInvestigationReportVo vo, String hazardExtent, String riskExtent) {
        vo.setHazardRoadStatistics(queryRoadStatistics(hazardExtent, "危险范围"));
        vo.setRiskRoadStatistics(queryRoadStatistics(riskExtent, "风险范围"));
    }

    /**
     * 隐藏现场记录中的几何大字段，避免直接返回大段WKT文本。
     *
     * @param sceneRecord 现场记录
     * @return 无
     */
    private void hideGeometryFields(DzTaskHandleSceneRecordVo sceneRecord) {
        if (sceneRecord == null) {
            return;
        }
        sceneRecord.setHazardExtent(null);
        sceneRecord.setRiskExtent(null);
        sceneRecord.setWkt(null);
    }

    /**
     * 根据范围WKT查询道路统计信息。
     *
     * @param wkt       范围WKT
     * @param scopeName 范围名称
     * @return 道路统计信息
     */
    private List<RoadStatisticVo> queryRoadStatistics(String wkt, String scopeName) {
        if (StringUtils.isBlank(wkt)) {
            return List.of();
        }
        try {
            return dataRoadService.queryStatisticsByWkt(wkt);
        } catch (Exception e) {
            log.warn("{}道路统计失败", scopeName, e);
            return List.of();
        }
    }
}
