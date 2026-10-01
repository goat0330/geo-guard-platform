/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataRoadGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataRoadGeologyMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadGeologyService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 道路地质 Service 实现
 *
 * @author zhuzc
 * @date 2026-06-05
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataRoadGeologyServiceImpl implements IDataRoadGeologyService {

    /** 点查询容差半径（米） */
    private static final double POINT_QUERY_RADIUS_METERS = 10.0d;

    private final DataRoadGeologyMapper baseMapper;

    @Override
    public DataRoadGeologyVo queryNearestByPoint(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
        QueryWrapper<DataRoadGeology> wrapper = Wrappers.query();
        wrapper.isNotNull("geom").apply(
            "ST_DWithin(geom::geography, "
                + "ST_SetSRID(ST_MakePoint({0}::double precision, {1}::double precision), 4326)::geography, "
                + "{2})",
            lon, lat, POINT_QUERY_RADIUS_METERS);
        wrapper.last("ORDER BY id ASC LIMIT 1");
        try {
            return baseMapper.selectVoOne(wrapper);
        } catch (Exception e) {
            log.warn("道路查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }
}
