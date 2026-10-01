/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 斜坡单元Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface SlopeUnitMapper extends BaseMapperPlus<SlopeUnit, SlopeUnitVo> {

    Page<SlopeUnitVo> selectVoPageByRiskLevel(@Param("page") Page<SlopeUnit> build,
                                              @Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    Page<SlopeUnitVo> selectVoPage(@Param("page") Page<SlopeUnit> build,
                                   @Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    Page<SlopeUnitVo> selectVoPageNoWkt(@Param("page") Page<SlopeUnit> build,
                                        @Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    Page<SlopeUnitVo> selectVoPageByRiskLevelNoWkt(@Param("page") Page<SlopeUnit> build,
                                                   @Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    List<SlopeUnitVo> selectVoListNoWkt(@Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    List<SlopeUnitVo> selectRespByNlp(@Param(Constants.WRAPPER) QueryWrapper<SlopeUnit> qew);

    @Select("""
        select *
        from data_slope_unit
        where geom is not null
          and st_contains(geom, st_setsrid(st_geomfromtext(#{center}), 4326))
        order by st_distance(
                     coalesce(
                         case
                             when center is not null and center <> '' then st_setsrid(st_geomfromtext(center), 4326)
                         end,
                         st_centroid(geom)
                     )::geography,
                     st_setsrid(st_geomfromtext(#{center}), 4326)::geography
                 ) asc,
                 id asc
        limit 1
        """)
    SlopeUnit selectSlopeUnitByCenter(@Param("center") String checkCenter);

    @Select("""
        select dynamic_risk_level
        from dz_risk_assessment
        where slope_unit_id = #{slopeUnitId}
        order by create_date desc
        limit 1
        """)
    Integer selectDynamicRiskLevelBySlopeUnitId(@Param("slopeUnitId") String slopeUnitId);

    /**
     * 按经纬度查询命中的斜坡单元（取首条）
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的斜坡单元；未命中返回 null
     */
    SlopeUnit selectByPoint(@Param("lon") Double lon, @Param("lat") Double lat);
}
