/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.BuildingPopulationStatsVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataPersonVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 人员信息Mapper接口
 *
 * @author system
 * @date 2024
 */
public interface DataPersonMapper extends BaseMapperPlus<Person, DataPersonVo> {

    /**
     * 按条件统计去重后的户室数量（一户一 house_unit_id）
     *
     * @param queryWrapper 查询条件
     * @return 去重后的户数
     */
    @Select("SELECT COUNT(DISTINCT house_unit_id) FROM public.data_person ${ew.customSqlSegment}")
    Long countDistinctHouseUnitId(@Param("ew") Wrapper<Person> queryWrapper);

    /**
     * 根据建筑编码统计人员数量。
     *
     * @param buildingCode 建筑编码
     * @return 人员数量
     */
    @Select("""
        SELECT COUNT(1)
        FROM public.data_person
        WHERE building_code = #{buildingCode}
        """)
    Long countByBuildingCode(@Param("buildingCode") String buildingCode);

    /**
     * 根据范围WKT统计人口数量
     *
     * @param polygonWkt 范围WKT
     * @return 人口数量
     */
    @Select("""
        SELECT COUNT(1)
        FROM public.data_person
        WHERE longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND ST_Contains(
                ST_GeomFromText(#{polygonWkt}, 4490),
                ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)
              )
        """)
    Long countByPolygonWkt(@Param("polygonWkt") String polygonWkt);

    /**
     * 根据范围WKT统计户数（按户室唯一ID去重）
     *
     * @param polygonWkt 范围WKT
     * @return 户数
     */
    @Select("""
        SELECT COUNT(DISTINCT house_unit_id)
        FROM public.data_person
        WHERE house_unit_id IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND ST_Contains(
                ST_GeomFromText(#{polygonWkt}, 4490),
                ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)
              )
        """)
    Long countHouseholdByPolygonWkt(@Param("polygonWkt") String polygonWkt);

    /**
     * 根据范围WKT统计60岁及以上人口数量
     *
     * @param polygonWkt 范围WKT
     * @return 60岁及以上人口数量
     */
    @Select("""
        SELECT COUNT(1)
        FROM public.data_person
        WHERE birthday IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude IS NOT NULL
          AND birthday <= CURRENT_DATE - INTERVAL '60 years'
          AND ST_Contains(
                ST_GeomFromText(#{polygonWkt}, 4490),
                ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490)
              )
        """)
    Long countAgeSixtyAndAboveByPolygonWkt(@Param("polygonWkt") String polygonWkt);

    /**
     * 按建筑编码统计人员信息。
     *
     * @param buildingCode 建筑编码
     * @return 人员统计
     */
    @Select("""
        SELECT COUNT(1) AS personCount,
               COALESCE(SUM(CASE WHEN age >= 0 AND age < 18 THEN 1 ELSE 0 END), 0) AS age0To18Count,
               COALESCE(SUM(CASE WHEN age >= 18 AND age < 45 THEN 1 ELSE 0 END), 0) AS age18To45Count,
               COALESCE(SUM(CASE WHEN age >= 45 AND age < 65 THEN 1 ELSE 0 END), 0) AS age45To65Count,
               COALESCE(SUM(CASE WHEN age >= 65 THEN 1 ELSE 0 END), 0) AS age65AndAboveCount,
               COALESCE(SUM(CASE WHEN UPPER(BTRIM(COALESCE(gender, ''))) IN ('男', '1', 'M', 'MALE', 'MAN') THEN 1 ELSE 0 END), 0) AS maleCount,
               COALESCE(SUM(CASE WHEN UPPER(BTRIM(COALESCE(gender, ''))) IN ('女', '2', 'F', 'FEMALE', 'WOMAN') THEN 1 ELSE 0 END), 0) AS femaleCount
        FROM public.data_person
        WHERE building_code = #{buildingCode}
        """)
    BuildingPopulationStatsVo selectBuildingPopulationStats(@Param("buildingCode") String buildingCode);
}
