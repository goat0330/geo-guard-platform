/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallEntityGridMappingMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallEntityGridMappingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 雨量统计对象与网格映射 Service 实现。
 *
 * @author system
 * @date 2026-04-22
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RainfallEntityGridMappingServiceImpl implements IRainfallEntityGridMappingService {

    private static final String ENSHI_COUNTY_NAME = "恩施市";

    private final RainfallEntityGridMappingMapper baseMapper;

    @Override
    public void saveOrUpdateMappings(List<RainfallEntityGridMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        List<String> slopeUnitIds = mappings.stream()
                                            .filter(item -> RainfallEntityGridMapping.TYPE_SLOPE_UNIT.equals(item.getEntityType()))
                                            .map(RainfallEntityGridMapping::getSlopeUnitId)
                                            .filter(StringUtils::isNotBlank)
                                            .toList();
        List<String> adRegionIds = mappings.stream()
                                           .filter(item -> RainfallEntityGridMapping.TYPE_AD_REGION.equals(item.getEntityType()))
                                           .map(RainfallEntityGridMapping::getAdRegionId)
                                           .filter(StringUtils::isNotBlank)
                                           .toList();

        Map<String, RainfallEntityGridMapping> existingMap = new HashMap<>();
        if (!slopeUnitIds.isEmpty()) {
            LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
            lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_SLOPE_UNIT)
               .in(RainfallEntityGridMapping::getSlopeUnitId, slopeUnitIds);
            for (RainfallEntityGridMapping item : baseMapper.selectList(lqw)) {
                existingMap.put(buildEntityKey(item), item);
            }
        }
        if (!adRegionIds.isEmpty()) {
            LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
            lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_AD_REGION)
               .in(RainfallEntityGridMapping::getAdRegionId, adRegionIds);
            for (RainfallEntityGridMapping item : baseMapper.selectList(lqw)) {
                existingMap.put(buildEntityKey(item), item);
            }
        }

        int insertCount = 0;
        int updateCount = 0;
        for (RainfallEntityGridMapping mapping : mappings) {
            String entityKey = buildEntityKey(mapping);
            RainfallEntityGridMapping existing = existingMap.get(entityKey);
            if (existing == null) {
                if (mapping.getCreateTime() == null) {
                    mapping.setCreateTime(LocalDateTime.now());
                }
                if (mapping.getUpdateTime() == null) {
                    mapping.setUpdateTime(LocalDateTime.now());
                }
                baseMapper.insert(mapping);
                existingMap.put(entityKey, mapping);
                insertCount++;
                continue;
            }
            if (!hasMappingChanged(existing, mapping)) {
                continue;
            }
            copyMapping(existing, mapping);
            existing.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(existing);
            updateCount++;
        }
        log.info("雨量对象网格映射保存完成，新增 {} 条，更新 {} 条", insertCount, updateCount);
    }

    @Override
    public RainfallEntityGridMapping querySlopeUnitMapping(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return null;
        }
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_SLOPE_UNIT)
           .eq(RainfallEntityGridMapping::getSlopeUnitId, slopeUnitId);
        return baseMapper.selectOne(lqw);
    }

    @Override
    public List<RainfallEntityGridMapping> listSlopeUnitMappings() {
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_SLOPE_UNIT);
        return baseMapper.selectList(lqw);
    }

    @Override
    public List<RainfallEntityGridMapping> listAdRegionMappings(RainfallLogSlopeUnitStatisticQueryBo bo) {
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_AD_REGION)
           .eq(resolveAdRegionTargetLevel(bo) != null, RainfallEntityGridMapping::getAdRegionLevel, resolveAdRegionTargetLevel(bo))
           .eq(StringUtils.isNotBlank(bo.getProvince()), RainfallEntityGridMapping::getProvince, bo.getProvince())
           .eq(StringUtils.isNotBlank(bo.getCity()), RainfallEntityGridMapping::getCity, bo.getCity())
           .eq(StringUtils.isNotBlank(bo.getCounty()), RainfallEntityGridMapping::getCounty, bo.getCounty())
           .eq(StringUtils.isNotBlank(bo.getStreet()), RainfallEntityGridMapping::getStreet, bo.getStreet())
           .eq(StringUtils.isNotBlank(bo.getVillage()), RainfallEntityGridMapping::getVillage, bo.getVillage())
           .eq(StringUtils.isNotBlank(bo.getProvinceCode()), RainfallEntityGridMapping::getProvinceCode, bo.getProvinceCode())
           .eq(StringUtils.isNotBlank(bo.getCityCode()), RainfallEntityGridMapping::getCityCode, bo.getCityCode())
           .eq(StringUtils.isNotBlank(bo.getCountyCode()), RainfallEntityGridMapping::getCountyCode, bo.getCountyCode())
           .eq(StringUtils.isNotBlank(bo.getStreetCode()), RainfallEntityGridMapping::getStreetCode, bo.getStreetCode())
           .eq(StringUtils.isNotBlank(bo.getVillageCode()), RainfallEntityGridMapping::getVillageCode, bo.getVillageCode());
        return baseMapper.selectList(lqw);
    }

    @Override
    public List<RainfallEntityGridMapping> listEnshiStreetMappings() {
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.eq(RainfallEntityGridMapping::getEntityType, RainfallEntityGridMapping.TYPE_AD_REGION)
           .eq(RainfallEntityGridMapping::getAdRegionLevel, 4)
           .eq(RainfallEntityGridMapping::getCounty, ENSHI_COUNTY_NAME)
           .orderByAsc(RainfallEntityGridMapping::getStreet);
        return baseMapper.selectList(lqw);
    }

    @Override
    public Set<String> listDistinctGridKeys() {
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.isNotNull(RainfallEntityGridMapping::getGridLon)
           .isNotNull(RainfallEntityGridMapping::getGridLat)
           .select(RainfallEntityGridMapping::getGridLon, RainfallEntityGridMapping::getGridLat);
        List<RainfallEntityGridMapping> mappings = baseMapper.selectList(lqw);
        if (mappings == null || mappings.isEmpty()) {
            return Set.of();
        }
        Set<String> gridKeys = new HashSet<>(mappings.size());
        for (RainfallEntityGridMapping mapping : mappings) {
            gridKeys.add(buildGridKey(mapping.getGridLon(), mapping.getGridLat()));
        }
        return gridKeys;
    }

    @Override
    public Set<String> listDistinctForecastGridKeys() {
        LambdaQueryWrapper<RainfallEntityGridMapping> lqw = Wrappers.lambdaQuery();
        lqw.isNotNull(RainfallEntityGridMapping::getForecastGridLon)
           .isNotNull(RainfallEntityGridMapping::getForecastGridLat)
           .select(RainfallEntityGridMapping::getForecastGridLon, RainfallEntityGridMapping::getForecastGridLat);
        List<RainfallEntityGridMapping> mappings = baseMapper.selectList(lqw);
        if (mappings == null || mappings.isEmpty()) {
            return Set.of();
        }
        Set<String> gridKeys = new HashSet<>(mappings.size());
        for (RainfallEntityGridMapping mapping : mappings) {
            gridKeys.add(buildGridKey(mapping.getForecastGridLon(), mapping.getForecastGridLat()));
        }
        return gridKeys;
    }

    private Integer resolveAdRegionTargetLevel(RainfallLogSlopeUnitStatisticQueryBo bo) {
        if (bo == null) {
            return null;
        }
        if (StringUtils.isNotBlank(bo.getVillage()) || StringUtils.isNotBlank(bo.getVillageCode())) {
            return 5;
        }
        if (StringUtils.isNotBlank(bo.getStreet()) || StringUtils.isNotBlank(bo.getStreetCode())) {
            return 4;
        }
        if (StringUtils.isNotBlank(bo.getCounty()) || StringUtils.isNotBlank(bo.getCountyCode())) {
            return 3;
        }
        if (StringUtils.isNotBlank(bo.getCity()) || StringUtils.isNotBlank(bo.getCityCode())) {
            return 2;
        }
        if (StringUtils.isNotBlank(bo.getProvince()) || StringUtils.isNotBlank(bo.getProvinceCode())) {
            return 1;
        }
        return null;
    }

    private String buildEntityKey(RainfallEntityGridMapping item) {
        String slopeUnitId = item.getSlopeUnitId() == null ? "" : item.getSlopeUnitId();
        String adRegionId = item.getAdRegionId() == null ? "" : item.getAdRegionId();
        return item.getEntityType() + "|" + slopeUnitId + "|" + adRegionId;
    }

    private static String buildGridKey(Double lon, Double lat) {
        return lon + "_" + lat;
    }

    private boolean hasMappingChanged(RainfallEntityGridMapping existing, RainfallEntityGridMapping incoming) {
        return (incoming.getGridLon() != null && incoming.getGridLat() != null
                && (!Objects.equals(existing.getGridLon(), incoming.getGridLon())
                    || !Objects.equals(existing.getGridLat(), incoming.getGridLat())))
            || (incoming.getForecastGridLon() != null && incoming.getForecastGridLat() != null
                && (!Objects.equals(existing.getForecastGridLon(), incoming.getForecastGridLon())
                    || !Objects.equals(existing.getForecastGridLat(), incoming.getForecastGridLat())))
            || !Objects.equals(existing.getCenter(), incoming.getCenter())
            || !Objects.equals(existing.getCenterLon(), incoming.getCenterLon())
            || !Objects.equals(existing.getCenterLat(), incoming.getCenterLat())
            || !Objects.equals(existing.getProvince(), incoming.getProvince())
            || !Objects.equals(existing.getCity(), incoming.getCity())
            || !Objects.equals(existing.getCounty(), incoming.getCounty())
            || !Objects.equals(existing.getStreet(), incoming.getStreet())
            || !Objects.equals(existing.getVillage(), incoming.getVillage())
            || !Objects.equals(existing.getCommunity(), incoming.getCommunity())
            || !Objects.equals(existing.getProvinceCode(), incoming.getProvinceCode())
            || !Objects.equals(existing.getCityCode(), incoming.getCityCode())
            || !Objects.equals(existing.getCountyCode(), incoming.getCountyCode())
            || !Objects.equals(existing.getStreetCode(), incoming.getStreetCode())
            || !Objects.equals(existing.getVillageCode(), incoming.getVillageCode())
            || !Objects.equals(existing.getAdRegionLevel(), incoming.getAdRegionLevel());
    }

    private void copyMapping(RainfallEntityGridMapping existing, RainfallEntityGridMapping incoming) {
        existing.setEntityType(incoming.getEntityType());
        existing.setSlopeUnitId(incoming.getSlopeUnitId());
        existing.setAdRegionId(incoming.getAdRegionId());
        existing.setAdRegionLevel(incoming.getAdRegionLevel());
        existing.setCenter(incoming.getCenter());
        existing.setCenterLon(incoming.getCenterLon());
        existing.setCenterLat(incoming.getCenterLat());
        if (incoming.getGridLon() != null && incoming.getGridLat() != null) {
            existing.setGridLon(incoming.getGridLon());
            existing.setGridLat(incoming.getGridLat());
        }
        if (incoming.getForecastGridLon() != null && incoming.getForecastGridLat() != null) {
            existing.setForecastGridLon(incoming.getForecastGridLon());
            existing.setForecastGridLat(incoming.getForecastGridLat());
        }
        existing.setProvince(incoming.getProvince());
        existing.setCity(incoming.getCity());
        existing.setCounty(incoming.getCounty());
        existing.setStreet(incoming.getStreet());
        existing.setVillage(incoming.getVillage());
        existing.setCommunity(incoming.getCommunity());
        existing.setProvinceCode(incoming.getProvinceCode());
        existing.setCityCode(incoming.getCityCode());
        existing.setCountyCode(incoming.getCountyCode());
        existing.setStreetCode(incoming.getStreetCode());
        existing.setVillageCode(incoming.getVillageCode());
    }
}
