/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HouseSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeGeometryVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HousePointMatchVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 房屋表与边坡单元表空间关联Mapper接口 v_house_slope
 *
 * @author system
 * @date 2026-01-21
 */
public interface HouseSlopeMapper extends BaseMapperPlus<HouseSlope, HouseSlopeVo> {

    /**
     * 统计去重后的建筑代码数量
     *
     * @param queryWrapper 查询条件
     * @return 去重后的建筑数量
     */
    @Select("SELECT COUNT(DISTINCT building_code) FROM v_house_slope ${ew.customSqlSegment}")
    Long countDistinctBuildingCode(@Param("ew") Wrapper<HouseSlope> queryWrapper);

    @Select("""
        SELECT DISTINCT ON (COALESCE(vhs.building_code, vhs.house_id))
               vhs.house_id AS houseId,
               vhs.building_code AS buildingCode,
               vhs.building_name AS buildingName,
               vhs.slope_unit_id AS slopeUnitId,
               vhs.slope_unit_name AS slopeUnitName,
               ST_AsText(vhs.house_geom) AS geometry
        FROM public.v_house_slope vhs
        INNER JOIN public.data_slope_unit dsu ON dsu.id = vhs.slope_unit_id
        WHERE vhs.house_geom IS NOT NULL
          AND (
                #{adRegionId} IS NULL
                OR #{adRegionId} = ''
                OR (#{adRegionLevel} = 1 AND dsu.province_code = #{adRegionId})
                OR (#{adRegionLevel} = 2 AND dsu.city_code = #{adRegionId})
                OR (#{adRegionLevel} = 3 AND dsu.county_code = #{adRegionId})
                OR (#{adRegionLevel} = 4 AND dsu.street_code = #{adRegionId})
                OR (#{adRegionLevel} = 5 AND dsu.village_code = #{adRegionId})
              )
          AND (
                #{slopeUnitId} IS NULL
                OR #{slopeUnitId} = ''
                OR vhs.slope_unit_id = #{slopeUnitId}
              )
        ORDER BY COALESCE(vhs.building_code, vhs.house_id), vhs.update_time DESC NULLS LAST, vhs.create_time DESC NULLS LAST
        """)
    List<HouseSlopeGeometryVo> selectGeometryList(@Param("adRegionId") String adRegionId,
                                                  @Param("adRegionLevel") Integer adRegionLevel,
                                                  @Param("slopeUnitId") String slopeUnitId);

    /**
     * 统计坐标点 10m 范围内（按 building_code 去重）建筑数量。
     *
     * @param longitude    经度
     * @param latitude     纬度
     * @param radiusMeters 容差半径（米）
     * @return 去重后的建筑数量
     */
    @Select("""
        SELECT COUNT(DISTINCT vhs.building_code)
        FROM public.v_house_slope vhs
        WHERE vhs.building_code IS NOT NULL
          AND vhs.house_geom IS NOT NULL
          AND ST_DWithin(
                vhs.house_geom::geography,
                ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)::geography,
                #{radiusMeters}
              )
        """)
    Long countByPoint(@Param("longitude") Double longitude,
                      @Param("latitude") Double latitude,
                      @Param("radiusMeters") Double radiusMeters);

    @Select("""
        SELECT DISTINCT ON (COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id))
               h.id AS houseId,
               h.house_unit_id AS houseUnitId,
               h.building_code AS buildingCode,
               h.building_name AS buildingName
        FROM public.data_house h
        WHERE h.geometry IS NOT NULL
          AND h.geometry
              && ST_Envelope(
                   ST_Buffer(
                     ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)::geography,
                     #{radiusMeters}
                   )::geometry
                 )
          AND ST_DWithin(
                h.geometry::geography,
                ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)::geography,
                #{radiusMeters}
              )
        ORDER BY COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id),
                 h.update_time DESC NULLS LAST,
                 h.create_time DESC NULLS LAST
        """)
    List<HousePointMatchVo> selectBufferedPointMatches(@Param("longitude") Double longitude,
                                                       @Param("latitude") Double latitude,
                                                       @Param("radiusMeters") Double radiusMeters);

    @Select("""
        SELECT DISTINCT ON (COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id))
               h.id AS houseId,
               h.house_unit_id AS houseUnitId,
               h.building_code AS buildingCode,
               h.building_name AS buildingName
        FROM public.data_house h
        WHERE h.geometry IS NOT NULL
          AND ST_Intersects(
                h.geometry,
                ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)
              )
        ORDER BY COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id),
                 h.update_time DESC NULLS LAST,
                 h.create_time DESC NULLS LAST
        """)
    List<HousePointMatchVo> selectExactPointMatches(@Param("longitude") Double longitude,
                                                    @Param("latitude") Double latitude);
}
