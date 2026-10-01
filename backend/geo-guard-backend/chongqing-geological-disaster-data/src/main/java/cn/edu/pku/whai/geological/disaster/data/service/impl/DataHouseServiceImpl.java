/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyfwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataHouseService;
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
 * 房间信息Service业务层处理
 *
 * @author system
 * @date 2024
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataHouseServiceImpl implements IDataHouseService {

    private final DataHouseMapper baseMapper;

    /**
     * 批量保存房间数据
     *
     * @param dataList 房间数据列表
     */
    @Override
    public void saveBatch(List<YbssSyfwDto> dataList) {
        try {
            if (dataList == null || dataList.isEmpty()) {
                log.warn("房间数据列表为空，跳过保存");
                return;
            }

            List<House> poList = dataList.stream()
                .map(dto -> MapstructUtils.convert(dto, House.class))
                .toList();

            baseMapper.insertOrUpdateBatch(poList);
            log.info("成功保存 {} 条房间数据", poList.size());
        } catch (Exception e) {
            log.error("批量保存房间数据失败", e);
            throw new ServiceException("批量保存房间数据失败：" + e.getMessage());
        }
    }

    /**
     * 根据范围WKT查询房屋列表
     *
     * @param wkt 范围WKT
     * @return 房屋列表
     */
    @Override
    public List<DataHouseVo> queryByWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        QueryWrapper<House> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("longitude")
            .isNotNull("latitude")
            .and(wrapper -> wrapper.apply(
                "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
                normalizedWkt
            ))
            .orderByAsc("id");
        return baseMapper.selectVoList(queryWrapper);
    }

    /**
     * 按房屋 geometry 与查询范围相交关系查询命中房屋。
     */
    @Override
    public List<DataHouseVo> queryByGeometryWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        QueryWrapper<House> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("geometry")
            .and(wrapper -> wrapper.apply(
                "ST_Intersects(ST_SetSRID(geometry, 4490), ST_GeomFromText({0}, 4490))",
                normalizedWkt
            ))
            .orderByAsc("id");
        return baseMapper.selectVoList(queryWrapper);
    }

    /**
     * 统一校验并规范化 WKT，避免空间查询时传入非法几何对象。
     */
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
