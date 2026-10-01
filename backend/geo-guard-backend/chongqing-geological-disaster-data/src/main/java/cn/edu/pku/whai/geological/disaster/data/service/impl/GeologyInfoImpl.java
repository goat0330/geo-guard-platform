/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.GeologyInfoMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataHouseService;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 地质信息
 *
 * @author lizheng
 */
@RequiredArgsConstructor
@Service
public class GeologyInfoImpl implements IGeologyInfoService {

    private final GeologyInfoMapper geologyInfoMapper;

    private final SlopeUnitMapper slopeUnitMapper;

    private final IDataHouseService dataHouseService;

    private final DataPersonMapper dataPersonMapper;

    @Override
    public GeologyInfoVo queryByPoint(double lon, double lat) {
        GeologyInfoVo geologyInfoVo = new GeologyInfoVo();

        EngineeringGeology engineeringGeology = geologyInfoMapper.queryEngineeringGeologyByPoint(lon, lat);
        if (engineeringGeology != null) {
            geologyInfoVo.getEngineeringGeologies().add(engineeringGeology);
        }

        HydroGeology hydroGeology = geologyInfoMapper.queryHydroGeologyByPoint(lon, lat);
        if (hydroGeology != null) {
            geologyInfoVo.getHydroGeologies().add(hydroGeology);
        }

        Stratum stratum = geologyInfoMapper.queryStratumByPoint(lon, lat);
        if (stratum != null) {
            geologyInfoVo.getStratums().add(stratum);
        }

        GeologyAdRegionVo adRegion = geologyInfoMapper.queryAdRegionByPoint(lon, lat);
        if (adRegion != null) {
            geologyInfoVo.setAdRegion(adRegion);
        }
        return geologyInfoVo;
    }

    @Override
    public GeologyAdRegionVo queryAdRegionByPoint(double lon, double lat) {
        return geologyInfoMapper.queryAdRegionByPoint(lon, lat);
    }

    @Override
    public GeologyInfoVo queryByBounds(String wkt) {
        GeologyInfoVo geologyInfoVo = new GeologyInfoVo();
        geologyInfoVo.setScopeWkt(wkt);
        appendGeologyByBounds(geologyInfoVo, wkt);
        return geologyInfoVo;
    }

    @Override
    public GeologyInfoVo queryByRegions(String regions) {
        GeologyInfoVo geologyInfoVo = new GeologyInfoVo();

        List<String> regionList = Arrays.stream(regions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        if (regionList.isEmpty()) {
            return geologyInfoVo;
        }

        List<EngineeringGeology> engineeringGeologyList = geologyInfoMapper.queryEngineeringGeologyByRegions(regionList);
        if (!engineeringGeologyList.isEmpty()) {
            geologyInfoVo.getEngineeringGeologies().addAll(engineeringGeologyList);
        }

        List<HydroGeology> hydroGeologyList = geologyInfoMapper.queryHydroGeologyByRegions(regionList);
        if (!hydroGeologyList.isEmpty()) {
            geologyInfoVo.getHydroGeologies().addAll(hydroGeologyList);
        }

        List<Stratum> stratumList = geologyInfoMapper.queryStratumByRegions(regionList);
        if (!stratumList.isEmpty()) {
            geologyInfoVo.getStratums().addAll(stratumList);
        }

        String regionUnionWkt = geologyInfoMapper.queryRegionUnionWkt(regionList);
        geologyInfoVo.setScopeWkt(regionUnionWkt);
        return geologyInfoVo;
    }

    @Override
    public GeologyInfoVo queryBySlopeUnitId(String id) {
        SlopeUnit slopeUnit = slopeUnitMapper.selectById(id);
        if (slopeUnit == null) {
            return new GeologyInfoVo();
        }

        GeologyInfoVo geologyInfoVo = new GeologyInfoVo();
        geologyInfoVo.setSlopeUnitId(id);
        geologyInfoVo.setSlopeUnitName(slopeUnit.getName());
        geologyInfoVo.setScopeWkt(slopeUnit.getWkt());
        appendGeologyByBounds(geologyInfoVo, slopeUnit.getWkt());
        return geologyInfoVo;
    }

    @Override
    public GeologyInfoVo queryBySlopeUnitWkt(String wkt) {
        if (wkt == null || wkt.isEmpty()) {
            throw new ServiceException("斜坡单元WKT不能为空");
        }
        GeologyInfoVo vo = new GeologyInfoVo();
        vo.setScopeWkt(wkt);
        appendGeologyByBounds(vo, wkt);
        return vo;
    }

    /**
     * 复用面查询逻辑，聚合工程地质、水文地质和地层数据。
     */
    private void appendGeologyByBounds(GeologyInfoVo geologyInfoVo, String wkt) {
        List<EngineeringGeology> engineeringGeologyList = geologyInfoMapper.queryEngineeringGeologyByBounds(wkt);
        if (!engineeringGeologyList.isEmpty()) {
            geologyInfoVo.getEngineeringGeologies().addAll(engineeringGeologyList);
        }

        List<HydroGeology> hydroGeologyList = geologyInfoMapper.queryHydroGeologyByBounds(wkt);
        if (!hydroGeologyList.isEmpty()) {
            geologyInfoVo.getHydroGeologies().addAll(hydroGeologyList);
        }

        List<Stratum> stratumList = geologyInfoMapper.queryStratumByBounds(wkt);
        if (!stratumList.isEmpty()) {
            geologyInfoVo.getStratums().addAll(stratumList);
        }
    }
}
