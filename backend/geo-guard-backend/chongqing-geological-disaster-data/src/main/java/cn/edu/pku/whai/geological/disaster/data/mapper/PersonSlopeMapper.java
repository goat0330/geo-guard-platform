/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.PersonSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PersonSlopeVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 人员表与边坡单元表空间关联Mapper接口 v_person_slope
 *
 * @author system
 * @date 2026-01-21
 */
public interface PersonSlopeMapper extends BaseMapperPlus<PersonSlope, PersonSlopeVo> {

    /**
     * 统计人口总数
     *
     * @param queryWrapper 查询条件
     * @return 人口总数
     */
    @Select("SELECT COUNT(*) FROM v_person_slope ${ew.customSqlSegment}")
    Long countBySlopeUnitId(@Param("ew") Wrapper<PersonSlope> queryWrapper);

    /**
     * 统计坐标点 10m 范围内的人口数量。
     *
     * @param longitude    经度
     * @param latitude     纬度
     * @param radiusMeters 容差半径（米）
     * @return 人口数量
     */
    @Select("""
        SELECT COUNT(*)
        FROM public.v_person_slope vps
        WHERE vps.person_geom IS NOT NULL
          AND ST_DWithin(
                ST_SetSRID(vps.person_geom, 4490)::geography,
                ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4490)::geography,
                #{radiusMeters}
              )
        """)
    Long countByPoint(@Param("longitude") Double longitude,
                      @Param("latitude") Double latitude,
                      @Param("radiusMeters") Double radiusMeters);

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM public.v_person_slope vps
        <where>
            <choose>
                <when test="buildingCode != null and buildingCode != ''">
                    vps.building_code = #{buildingCode}
                </when>
                <when test="houseUnitId != null and houseUnitId != ''">
                    vps.house_unit_id = #{houseUnitId}
                </when>
                <otherwise>
                    1 = 0
                </otherwise>
            </choose>
        </where>
        </script>
        """)
    Long countByHouse(@Param("houseUnitId") String houseUnitId,
                      @Param("buildingCode") String buildingCode);

    @Select("""
        <script>
        SELECT COUNT(DISTINCT vps.house_unit_id)
        FROM public.v_person_slope vps
        <where>
            <choose>
                <when test="buildingCode != null and buildingCode != ''">
                    vps.building_code = #{buildingCode}
                </when>
                <when test="houseUnitId != null and houseUnitId != ''">
                    vps.house_unit_id = #{houseUnitId}
                </when>
                <otherwise>
                    1 = 0
                </otherwise>
            </choose>
        </where>
        </script>
        """)
    Long countHouseholdByHouse(@Param("houseUnitId") String houseUnitId,
                               @Param("buildingCode") String buildingCode);
}
