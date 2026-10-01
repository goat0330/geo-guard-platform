/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFoldVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataGeologicalFoldMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataGeologicalFoldService;
import cn.edu.pku.whai.geological.disaster.data.utils.GeoDirectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 地质褶皱 Service 实现
 *
 * @author zhuzc
 * @date 2026-06-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataGeologicalFoldServiceImpl implements IDataGeologicalFoldService {

    private final DataGeologicalFoldMapper baseMapper;

    @Override
    public DataGeologicalFoldVo queryNearestByPoint(Double lon, Double lat) {
        // 1) 入参校验：经纬度非空且在合法范围
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
        try {
            // 2) 查询最近褶皱
            DataGeologicalFoldVo vo = baseMapper.selectNearestByPoint(lon, lat);
            if (vo != null && vo.getAzimuth() != null) {
                // 3) 弧度转角度 + 8 方位名
                double degrees = Math.toDegrees(vo.getAzimuth());
                vo.setAzimuth(degrees);
                vo.setAzimuthName(GeoDirectionUtil.azimuthDegreesTo8Dir(degrees));
            }
            return vo;
        } catch (Exception e) {
            // 4) 异常降级：记 warn 并返回 null，不影响调用方整体流程
            log.warn("褶皱查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }
}
