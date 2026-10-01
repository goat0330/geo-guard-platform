/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HazardPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.HazardPointDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HazardPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.HazardPointMapper;
import cn.edu.pku.whai.geological.disaster.data.props.DisasterPreventionPlatformProps;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 隐患点基本情况Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class HazardPointServiceImpl implements IHazardPointService {

    private final HazardPointMapper baseMapper;

    private final DisasterPreventionPlatformProps disasterPreventionPlatformProps;

    /**
     * 查询隐患点基本情况
     *
     * @param id 主键
     * @return 隐患点基本情况
     */
    @Override
    public HazardPointVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询隐患点基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 隐患点基本情况分页列表
     */
    @Override
    public TableDataInfo<HazardPointVo> queryPageList(HazardPointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HazardPoint> lqw = buildQueryWrapper(bo);
        Page<HazardPointVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HazardPointVo> queryList(HazardPointBo bo) {
        LambdaQueryWrapper<HazardPoint> lqw = buildQueryWrapper(bo);
        List<HazardPointVo> result = baseMapper.selectVoList(lqw);
        return result;
    }

    @Override
    public List<HazardPointVo> queryByWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        QueryWrapper<HazardPoint> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("longitude")
            .isNotNull("latitude")
            .and(wrapper -> wrapper.apply(
                "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
                normalizedWkt
            ));
        return baseMapper.selectVoList(queryWrapper);
    }

    @Override
    public void syncData() {
        try {
            List<HazardPointDto> hazardPointDtoList = new ArrayList<>();

            ObjectNode paramNode = JacksonUtil.objectMapper.createObjectNode();
            paramNode.put("code", "4228");
            paramNode.put("dataType", "A");
            paramNode.put("token", disasterPreventionPlatformProps.getToken());
            String response = Req
                    .post(disasterPreventionPlatformProps.getUrl())
                    .path("/hbdz-yhgl/yhd/getYhDataByParams")
                    .json(paramNode.toString())
                    .timeout(Duration.ofMinutes(3))
                    .ok()
                    .str();

            JsonNode root = JacksonUtil.objectMapper.readTree(response);
            JsonNode dataNode = root.get("data");

            if (dataNode != null && dataNode.isArray()) {
                hazardPointDtoList = JacksonUtil.objectMapper.convertValue(
                        dataNode,
                        JacksonUtil.objectMapper.getTypeFactory().constructCollectionType(List.class, HazardPointDto.class)
                );
            }

            List<HazardPoint> hazardPointList = new ArrayList<>();
            hazardPointDtoList.forEach(dto -> {
                hazardPointList.add(MapstructUtils.convert(dto, HazardPoint.class));
            });

            baseMapper.insertOrUpdateBatch(hazardPointList);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private LambdaQueryWrapper<HazardPoint> buildQueryWrapper(HazardPointBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<HazardPoint> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(HazardPoint::getId);
        lqw.like(StringUtils.isNotBlank(bo.getName()), HazardPoint::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getTypeCode()), HazardPoint::getTypeCode, bo.getTypeCode());
        lqw.eq(StringUtils.isNotBlank(bo.getGridCode()), HazardPoint::getGridCode, bo.getGridCode());
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueDisasterId()), HazardPoint::getUniqueDisasterId, bo.getUniqueDisasterId());
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), HazardPoint::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), HazardPoint::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), HazardPoint::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), HazardPoint::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), HazardPoint::getVillage, bo.getVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getGridGroup()), HazardPoint::getGridGroup, bo.getGridGroup());
        lqw.eq(bo.getXCoordinate() != null, HazardPoint::getXCoordinate, bo.getXCoordinate());
        lqw.eq(bo.getYCoordinate() != null, HazardPoint::getYCoordinate, bo.getYCoordinate());
        lqw.eq(bo.getLongitude() != null, HazardPoint::getLongitude, bo.getLongitude());
        lqw.eq(bo.getLatitude() != null, HazardPoint::getLatitude, bo.getLatitude());
        lqw.eq(StringUtils.isNotBlank(bo.getWkt()), HazardPoint::getWkt, bo.getWkt());
        lqw.eq(bo.getLengthM() != null, HazardPoint::getLengthM, bo.getLengthM());
        lqw.eq(bo.getWidthM() != null, HazardPoint::getWidthM, bo.getWidthM());
        lqw.eq(bo.getHeightM() != null, HazardPoint::getHeightM, bo.getHeightM());
        lqw.eq(bo.getAreaSqm() != null, HazardPoint::getAreaSqm, bo.getAreaSqm());
        lqw.eq(bo.getVolumeCbm() != null, HazardPoint::getVolumeCbm, bo.getVolumeCbm());
        lqw.eq(StringUtils.isNotBlank(bo.getScaleGrade()), HazardPoint::getScaleGrade, bo.getScaleGrade());
        lqw.eq(StringUtils.isNotBlank(bo.getManagementLevel()), HazardPoint::getManagementLevel, bo.getManagementLevel());
        lqw.eq(bo.getThreatenedPopulation() != null, HazardPoint::getThreatenedPopulation, bo.getThreatenedPopulation());
        lqw.eq(bo.getThreatenedPropertyValue() != null, HazardPoint::getThreatenedPropertyValue, bo.getThreatenedPropertyValue());
        lqw.eq(StringUtils.isNotBlank(bo.getRiskGrade()), HazardPoint::getRiskGrade, bo.getRiskGrade());
        lqw.eq(StringUtils.isNotBlank(bo.getDisasterHistoryTime()), HazardPoint::getDisasterHistoryTime, bo.getDisasterHistoryTime());
        lqw.eq(StringUtils.isNotBlank(bo.getGeologicalEnvironment()), HazardPoint::getGeologicalEnvironment, bo.getGeologicalEnvironment());
        lqw.eq(StringUtils.isNotBlank(bo.getDeformationFeatures()), HazardPoint::getDeformationFeatures, bo.getDeformationFeatures());
        lqw.eq(StringUtils.isNotBlank(bo.getStabilityAnalysis()), HazardPoint::getStabilityAnalysis, bo.getStabilityAnalysis());
        lqw.eq(StringUtils.isNotBlank(bo.getStabilityStatus()), HazardPoint::getStabilityStatus, bo.getStabilityStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getStabilityTrend()), HazardPoint::getStabilityTrend, bo.getStabilityTrend());
        lqw.eq(StringUtils.isNotBlank(bo.getTriggerFactors()), HazardPoint::getTriggerFactors, bo.getTriggerFactors());
        lqw.eq(StringUtils.isNotBlank(bo.getPotentialHazards()), HazardPoint::getPotentialHazards, bo.getPotentialHazards());
        lqw.eq(StringUtils.isNotBlank(bo.getPreDisasterPrediction()), HazardPoint::getPreDisasterPrediction, bo.getPreDisasterPrediction());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringMethod()), HazardPoint::getMonitoringMethod, bo.getMonitoringMethod());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringPersonId()), HazardPoint::getMonitoringPersonId, bo.getMonitoringPersonId());
        lqw.eq(bo.getReportDate() != null, HazardPoint::getReportDate, bo.getReportDate());
        lqw.eq(StringUtils.isNotBlank(bo.getHistorySn()), HazardPoint::getHistorySn, bo.getHistorySn());
        lqw.eq(bo.getDataFlag() != null, HazardPoint::getDataFlag, bo.getDataFlag());
        lqw.eq(bo.getIsCancelled() != null, HazardPoint::getIsCancelled, bo.getIsCancelled());
        lqw.eq(StringUtils.isNotBlank(bo.getOperationType()), HazardPoint::getOperationType, bo.getOperationType());
        lqw.eq(StringUtils.isNotBlank(bo.getReviewStatus()), HazardPoint::getReviewStatus, bo.getReviewStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getCreatedBy()), HazardPoint::getCreatedBy, bo.getCreatedBy());
        lqw.eq(bo.getCreatedTime() != null, HazardPoint::getCreatedTime, bo.getCreatedTime());
        lqw.eq(bo.getHasSurveyData() != null, HazardPoint::getHasSurveyData, bo.getHasSurveyData());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), HazardPoint::getCategory, bo.getCategory());
        lqw.eq(bo.getPilotArea1() != null, HazardPoint::getPilotArea1, bo.getPilotArea1());
        lqw.eq(bo.getPilotArea2() != null, HazardPoint::getPilotArea2, bo.getPilotArea2());
        lqw.eq(StringUtils.isNotBlank(bo.getProvinceCode()), HazardPoint::getProvinceCode, bo.getProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCityCode()), HazardPoint::getCityCode, bo.getCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCountyCode()), HazardPoint::getCountyCode, bo.getCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStreetCode()), HazardPoint::getStreetCode, bo.getStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getVillageCode()), HazardPoint::getVillageCode, bo.getVillageCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), HazardPoint::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), HazardPoint::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), HazardPoint::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), HazardPoint::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), HazardPoint::getVillage, bo.getVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitId()), HazardPoint::getSlopeUnitId, bo.getSlopeUnitId());
        return lqw;
    }


    @Override
    public List<HazardPointVo> queryBySlopeUnitId(String id, String area) {
        QueryWrapper<HazardPoint> lqw = Wrappers.query();
        lqw.eq("slope_unit_id", id);
        lqw.like(StringUtils.isNotBlank(area), "concat(province, city, county, street, village, community)", area);

        List<HazardPointVo> hazardPointVos = baseMapper.selectVoList(lqw);
        return hazardPointVos;
    }


    @Override
    public Long countAll() {
        return baseMapper.selectCount(null);
    }

    @Override
    public Long countBySlopeUnitIds(List<String> slopeUnitIds) {
        if (slopeUnitIds == null || slopeUnitIds.isEmpty()) {
            return 0L;
        }
        return baseMapper.countBySlopeUnitIds(slopeUnitIds);
    }

    /**
     * 按经纬度点匹配隐患点范围，命中多条时取中心点最近的 1 条。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近一条隐患点VO（含 geologicalEnvironment）；未命中返回 null
     */
    @Override
    public HazardPointVo queryByContainingPointNearestCenter(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        try {
            return baseMapper.selectContainingPointNearestCenter(lon, lat);
        } catch (Exception e) {
            log.warn("隐患点查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    private String validateGeometryWkt(String wkt) {
        if (StringUtils.isBlank(wkt)) {
            throw new ServiceException("wkt不能为空");
        }
        String normalized = wkt.trim();
        try {
            Geometry geometry = new WKTReader().read(normalized);
            if (geometry == null || geometry.isEmpty() || geometry.getDimension() < 2) {
                throw new ServiceException("wkt格式错误");
            }
            return normalized;
        } catch (ParseException e) {
            throw new ServiceException("wkt格式错误");
        }
    }

}
