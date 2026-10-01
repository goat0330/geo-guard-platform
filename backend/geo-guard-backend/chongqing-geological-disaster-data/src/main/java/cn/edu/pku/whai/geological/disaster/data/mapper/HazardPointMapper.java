/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HazardPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 隐患点基本情况Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface HazardPointMapper extends BaseMapperPlus<HazardPoint, HazardPointVo> {

    /**
     * 按点命中隐患点范围（以隐患点 wkt 字符串作为范围），命中多条时按中心点最近返回 1 条。
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @return 隐患点信息
     */
    @Select("""
        SELECT id,name,type_code,grid_code,unique_disaster_id,province,city,county,street,village,grid_group,
               x_coordinate,y_coordinate,longitude,latitude,wkt,length_m,width_m,height_m,area_sqm,volume_cbm,
               scale_grade,management_level,threatened_population,threatened_property_value,risk_grade,
               disaster_history_time,geological_environment,deformation_features,stability_analysis,
               stability_status,stability_trend,trigger_factors,potential_hazards,pre_disaster_prediction,
               monitoring_method,monitoring_person_id,report_date,history_sn,data_flag,is_cancelled,
               operation_type,review_status,created_by,created_time,has_survey_data,category,
               slope_unit_id,slope_unit_name,pilot_area_1,pilot_area_2,province_code,city_code,
               county_code,street_code,village_code
        FROM data_hazard_point
        WHERE wkt IS NOT NULL
          AND TRIM(wkt) <> ''
          AND longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND ST_Contains(
                ST_GeomFromText(wkt, 4490),
                ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4490)
              )
        ORDER BY ST_Distance(
                     ST_SetSRID(
                         ST_MakePoint(longitude::double precision, latitude::double precision),
                         4490
                     )::geography,
                     ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4490)::geography
                 ) ASC,
                 created_time DESC,
                 id ASC
        LIMIT 1
        """)
    HazardPointVo selectContainingPointNearestCenter(@Param("longitude") Double longitude,
                                                     @Param("latitude") Double latitude);

    /**
     * 按斜坡单元ID集合统计隐患点数量。
     *
     * @param slopeUnitIds 斜坡单元ID集合
     * @return 隐患点数量
     */
    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT id) ",
        "FROM data_hazard_point ",
        "WHERE slope_unit_id IN ",
        "<foreach collection='slopeUnitIds' item='slopeUnitId' open='(' separator=',' close=')'>",
        "#{slopeUnitId}",
        "</foreach>",
        "</script>"
    })
    Long countBySlopeUnitIds(@Param("slopeUnitIds") List<String> slopeUnitIds);
}
