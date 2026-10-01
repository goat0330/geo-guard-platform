/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Road;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 道路信息Mapper接口
 *
 * @author system
 * @date 2026-05-15
 */
public interface DataRoadMapper extends BaseMapperPlus<Road, DataRoadVo> {

    /**
     * 按斜坡单元统计关联道路在斜坡面内的明细（按 road_name + road_level 累加）。
     * <p>
     * 同一道路在表中可能被切成多段，按 (road_name, road_level) GROUP BY 后多段合并为 1 行，
     * lengthInWkt 为各段面内长度的 SUM。
     * 长度 = SUM( ST_Length( ST_Intersection(斜坡面, 道路段)::geography ) )。
     *
     * @param slopeUnitId   斜坡单元ID
     * @param slopeUnitWkt  斜坡单元范围WKT（用于 ST_Intersection）
     * @return 道路面内明细列表（按 name+level 累加）
     */
    @Select("""
        SELECT road_name AS roadName,
               road_level AS roadLevel,
               ROUND(
                 (COALESCE(
                   SUM(
                     ST_Length(
                       ST_Intersection(
                         ST_GeomFromText(#{slopeUnitWkt}, 4490),
                         ST_SetSRID(ST_GeomFromText(wkt, 4490), 4490)
                       )::geography
                     )
                   ), 0
                 ))::numeric,
                 2
               ) AS lengthInWkt
        FROM public.data_road
        WHERE slope_unit_id = #{slopeUnitId}
          AND wkt IS NOT NULL
          AND ST_Intersects(
                ST_GeomFromText(#{slopeUnitWkt}, 4490),
                ST_SetSRID(ST_GeomFromText(wkt, 4490), 4490)
              )
        GROUP BY road_name, road_level
        ORDER BY road_level NULLS LAST, road_name NULLS LAST
        """)
    List<RoadStatisticVo> selectStatisticsBySlopeUnitId(@Param("slopeUnitId") String slopeUnitId,
                                                       @Param("slopeUnitWkt") String slopeUnitWkt);

    /**
     * 按查询面 WKT 统计道路在面内的明细（按 road_name + road_level 累加）。
     * <p>
     * 长度 = SUM( ST_Length( ST_Intersection(查询面, 道路段)::geography ) )。
     *
     * @param wkt 查询面 WKT
     * @return 道路面内明细列表（按 name+level 累加）
     */
    @Select("""
        SELECT road_name AS roadName,
               road_level AS roadLevel,
               ROUND(
                 (COALESCE(
                   SUM(
                     ST_Length(
                       ST_Intersection(
                         ST_GeomFromText(#{wkt}, 4490),
                         ST_SetSRID(ST_GeomFromText(wkt, 4490), 4490)
                       )::geography
                     )
                   ), 0
                 ))::numeric,
                 2
               ) AS lengthInWkt
        FROM public.data_road
        WHERE wkt IS NOT NULL
          AND ST_Intersects(
                ST_GeomFromText(#{wkt}, 4490),
                ST_SetSRID(ST_GeomFromText(wkt, 4490), 4490)
              )
        GROUP BY road_name, road_level
        ORDER BY road_level NULLS LAST, road_name NULLS LAST
        """)
    List<RoadStatisticVo> selectStatisticsByWkt(@Param("wkt") String wkt);
}
