/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Road;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataRoadMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 道路信息Service业务层处理
 *
 * @author system
 * @date 2026-05-15
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataRoadServiceImpl implements IDataRoadService {

    private final DataRoadMapper baseMapper;

    /**
     * 根据斜坡单元ID查询道路列表
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 道路列表
     */
    @Override
    public List<DataRoadVo> queryBySlopeUnitId(String slopeUnitId) {
        LambdaQueryWrapper<Road> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(slopeUnitId), Road::getSlopeUnitId, slopeUnitId);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据范围WKT查询道路列表
     *
     * @param wkt 范围WKT
     * @return 道路列表
     */
    @Override
    public List<DataRoadVo> queryByWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        QueryWrapper<Road> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("wkt")
            .apply(
                "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_GeomFromText(wkt, 4490), 4490))",
                normalizedWkt
            );
        return baseMapper.selectVoList(queryWrapper);
    }

    @Override
    public List<RoadStatisticVo> queryStatisticsBySlopeUnitId(String slopeUnitId, String slopeUnitWkt) {
        if (StringUtils.isBlank(slopeUnitId) || StringUtils.isBlank(slopeUnitWkt)) {
            return List.of();
        }
        try {
            List<RoadStatisticVo> statistics = baseMapper.selectStatisticsBySlopeUnitId(slopeUnitId, slopeUnitWkt.trim());
            return statistics == null ? List.of() : statistics;
        } catch (Exception e) {
            log.warn("按斜坡单元统计道路面内明细失败: slopeUnitId={}", slopeUnitId, e);
            return List.of();
        }
    }

    @Override
    public List<RoadStatisticVo> queryStatisticsByWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        try {
            List<RoadStatisticVo> statistics = baseMapper.selectStatisticsByWkt(normalizedWkt);
            return statistics == null ? List.of() : statistics;
        } catch (Exception e) {
            log.warn("按 WKT 统计道路面内明细失败: wkt={}", normalizedWkt, e);
            return List.of();
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
