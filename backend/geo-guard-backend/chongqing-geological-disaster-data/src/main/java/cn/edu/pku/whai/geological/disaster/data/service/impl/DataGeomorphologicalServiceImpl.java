/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataGeomorphological;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeomorphologicalVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataGeomorphologicalMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataGeomorphologicalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 地貌信息 Service 实现
 *
 * <p>按经纬度点命中 data_geomorphological 表中包含该点的多边形；命中多条时取中心点最近的 1 条。
 *
 * @author zhuzc
 * @date 2026-06-11
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataGeomorphologicalServiceImpl implements IDataGeomorphologicalService {

    private final DataGeomorphologicalMapper baseMapper;

    /**
     * 按经纬度点查询命中的地貌信息。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的地貌VO；未命中或异常返回 null
     */
    @Override
    public DataGeomorphologicalVo queryByContainingPointNearestCentroid(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
        try {
            return baseMapper.selectByContainingPointNearestCentroid(lon, lat);
        } catch (Exception e) {
            log.warn("地貌查询失败: lon={}, lat={}", lon, lat, e);
            return null;
        }
    }
}
