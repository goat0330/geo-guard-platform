/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 地质信息
 *
 * @author lizheng
 */
public interface GeologyInfoMapper {

    EngineeringGeology queryEngineeringGeologyByPoint(@Param("lon") double lon, @Param("lat") double lat);

    HydroGeology queryHydroGeologyByPoint(@Param("lon") double lon, @Param("lat") double lat);

    Stratum queryStratumByPoint(@Param("lon") double lon, @Param("lat") double lat);

    GeologyAdRegionVo queryAdRegionByPoint(@Param("lon") double lon, @Param("lat") double lat);

    List<EngineeringGeology> queryEngineeringGeologyByBounds(@Param("wkt") String wkt);

    List<HydroGeology> queryHydroGeologyByBounds(@Param("wkt") String wkt);

    List<Stratum> queryStratumByBounds(@Param("wkt") String wkt);

    List<EngineeringGeology> queryEngineeringGeologyByRegions(@Param("regionList") List<String> regionList);

    List<HydroGeology> queryHydroGeologyByRegions(@Param("regionList") List<String> regionList);

    List<Stratum> queryStratumByRegions(@Param("regionList") List<String> regionList);

    String queryRegionUnionWkt(@Param("regionList") List<String> regionList);
}
