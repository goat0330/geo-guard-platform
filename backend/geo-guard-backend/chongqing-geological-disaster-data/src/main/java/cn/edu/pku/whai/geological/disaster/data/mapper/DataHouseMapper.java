/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.NearestHouseCimVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 房间信息Mapper接口
 *
 * @author system
 * @date 2024
 */
public interface DataHouseMapper extends BaseMapperPlus<House, DataHouseVo> {

    /**
     * 根据范围WKT统计建筑数量（按 building_code 去重）
     *
     * @param polygonWkt 范围WKT
     * @return 建筑数量
     */
    @Select("""
        SELECT COUNT(DISTINCT building_code)
        FROM public.data_house
        WHERE building_code IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND ST_Contains(
                ST_GeomFromText(#{polygonWkt}, 4490),
                ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)
              )
        """)
    Long countDistinctBuildingByPolygonWkt(@Param("polygonWkt") String polygonWkt);

    /**
     * 查询指定半径范围内最近的房屋记录。
     *
     * @param longitude    经度
     * @param latitude     纬度
     * @param radiusMeters 半径（米）
     * @return 最近房屋
     */
    @Select("""
        SELECT building_code AS buildingCode,
               building_name AS buildingName,
               area,
               longitude,
               latitude,
               ST_AsText(geometry) AS wkt
        FROM public.data_house
        WHERE building_code IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND ST_DWithin(
                ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)::geography,
                ST_SetSRID(ST_MakePoint(#{longitude}, #{latitude}), 4490)::geography,
                #{radiusMeters}
              )
        ORDER BY ST_Distance(
                     ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)::geography,
                     ST_SetSRID(ST_MakePoint(#{longitude}, #{latitude}), 4490)::geography
                 ) ASC
        LIMIT 1
        """)
    NearestHouseCimVo selectNearestHouseByPoint(@Param("longitude") BigDecimal longitude,
                                                @Param("latitude") BigDecimal latitude,
                                                @Param("radiusMeters") Integer radiusMeters);
}
