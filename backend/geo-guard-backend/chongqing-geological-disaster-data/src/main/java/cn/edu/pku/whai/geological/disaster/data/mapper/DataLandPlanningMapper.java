/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.LandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PointLandPlanningVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 国土空间规划用地类型 Mapper
 *
 * @author zhuzc
 * @date 2026-06-25
 */
public interface DataLandPlanningMapper {

    /**
     * 按经纬度点查询命中的用地类型；命中多条时取中心点最近的 1 条。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的用地类型；未命中返回 null
     */
    @Select("""
        SELECT land_type_name AS landName,
               land_category_name AS landCategoryName
        FROM data_territorial_spatial_planning
        WHERE geom IS NOT NULL
          AND ST_Contains(
                geom,
                ST_SetSRID(ST_MakePoint(#{lon}::double precision, #{lat}::double precision), 4490)
              )
        ORDER BY ST_Distance(
                     ST_Centroid(geom)::geography,
                     ST_SetSRID(ST_MakePoint(#{lon}::double precision, #{lat}::double precision), 4490)::geography
                 ) ASC,
                 id ASC
        LIMIT 1
        """)
    PointLandPlanningVo selectPointLandPlanning(@Param("lon") Double lon, @Param("lat") Double lat);

    /**
     * 查询斜坡单元涉及的用地类型统计。
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 用地类型统计列表
     */
    @Select("""
        SELECT land_name AS landName,
               ROUND(intersection_area::numeric, 2) AS intersectionArea
        FROM data_slope_unit_land_planning_stat
        WHERE slope_unit_id = #{slopeUnitId}
        ORDER BY intersection_area DESC,
                 land_name ASC
        """)
    List<LandPlanningVo> selectSlopeUnitLandPlanningStatistics(@Param("slopeUnitId") String slopeUnitId);
}
