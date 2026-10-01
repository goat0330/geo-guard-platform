/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PointLandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataLandPlanningMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataLandPlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 国土空间规划用地类型 Service 实现
 *
 * @author zhuzc
 * @date 2026-06-25
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataLandPlanningServiceImpl implements IDataLandPlanningService {

    private final DataLandPlanningMapper baseMapper;

    @Override
    public String queryPointLandPlanningType(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
        try {
            PointLandPlanningVo pointLandPlanning = baseMapper.selectPointLandPlanning(lon, lat);
            if (pointLandPlanning == null) {
                return null;
            }
            if (StringUtils.isNotBlank(pointLandPlanning.getLandName())) {
                return pointLandPlanning.getLandName();
            }
            return pointLandPlanning.getLandCategoryName();
        } catch (Exception e) {
            log.warn("点位用地类型查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }

    @Override
    public List<LandPlanningVo> querySlopeUnitLandPlanningStatistics(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        try {
            List<LandPlanningVo> statistics = baseMapper.selectSlopeUnitLandPlanningStatistics(slopeUnitId);
            return statistics == null ? List.of() : statistics;
        } catch (Exception e) {
            log.warn("斜坡单元用地类型统计查询失败: slopeUnitId={}", slopeUnitId, e);
            return List.of();
        }
    }
}
