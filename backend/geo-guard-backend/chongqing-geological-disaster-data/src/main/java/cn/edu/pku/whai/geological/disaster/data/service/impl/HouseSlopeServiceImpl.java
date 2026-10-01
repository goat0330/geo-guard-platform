/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HouseSlopeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeGeometryVo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HouseSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HousePointMatchVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.HouseSlopeMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IHouseSlopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 房屋表与边坡单元表空间关联Service业务层处理 v_house_slope
 *
 * @author system
 * @date 2026-01-21
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class HouseSlopeServiceImpl implements IHouseSlopeService {

    private static final String DEFAULT_AD_REGION_ID = "422801";

    private final HouseSlopeMapper baseMapper;
    private final IAdRegionService adRegionService;

    /**
     * 查询房屋表与边坡单元表空间关联
     *
     * @param houseId 房屋ID
     * @return 房屋表与边坡单元表空间关联
     */
    @Override
    public HouseSlopeVo queryById(String houseId) {
        return baseMapper.selectVoById(houseId);
    }

    /**
     * 分页查询房屋表与边坡单元表空间关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 房屋表与边坡单元表空间关联分页列表
     */
    @Override
    public TableDataInfo<HouseSlopeVo> queryPageList(HouseSlopeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HouseSlope> lqw = buildQueryWrapper(bo);
        Page<HouseSlopeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的房屋表与边坡单元表空间关联列表
     *
     * @param bo 查询条件
     * @return 房屋表与边坡单元表空间关联列表
     */
    @Override
    public List<HouseSlopeVo> queryList(HouseSlopeBo bo) {
        LambdaQueryWrapper<HouseSlope> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据边坡单元ID查询房屋列表
     *
     * @param slopeUnitId 边坡单元ID
     * @return 房屋列表
     */
    @Override
    public List<HouseSlopeVo> queryBySlopeUnitId(String slopeUnitId) {
        LambdaQueryWrapper<HouseSlope> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(slopeUnitId), HouseSlope::getSlopeUnitId, slopeUnitId);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<HouseSlopeGeometryVo> queryGeometryList(String adRegionId, String slopeUnitId) {
        AdRegionVo adRegion = resolveAdRegion(adRegionId);
        return baseMapper.selectGeometryList(adRegion.getId(), adRegion.getLevel(), slopeUnitId);
    }

    /**
     * 统计坐标点指定半径范围内（按 building_code 去重）建筑数量。
     *
     * @param lon          经度
     * @param lat          纬度
     * @param radiusMeters 容差半径（米）
     * @return 去重后的建筑数量；未命中返回 0
     */
    @Override
    public Long countByPoint(Double lon, Double lat, Double radiusMeters) {
        validatePoint(lon, lat);
        double safeRadius = safeRadius(radiusMeters);
        try {
            Long count = baseMapper.countByPoint(lon, lat, safeRadius);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("房屋点查询失败: lon={}, lat={}, radiusMeters={}", lon, lat, safeRadius, e);
            return 0L;
        }
    }

    @Override
    public List<HousePointMatchVo> queryPointMatches(Double lon, Double lat, Double radiusMeters, boolean buffered) {
        validatePoint(lon, lat);
        double safeRadius = safeRadius(radiusMeters);
        try {
            List<HousePointMatchVo> matches = buffered
                ? baseMapper.selectBufferedPointMatches(lon, lat, safeRadius)
                : baseMapper.selectExactPointMatches(lon, lat);
            return matches == null ? List.of() : matches;
        } catch (Exception e) {
            log.warn("房屋点匹配失败: lon={}, lat={}, radiusMeters={}, buffered={}", lon, lat, safeRadius, buffered, e);
            return List.of();
        }
    }

    private void validatePoint(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
    }

    private double safeRadius(Double radiusMeters) {
        return (radiusMeters == null || radiusMeters <= 0d) ? 10.0d : radiusMeters;
    }

    /**
     * 构建查询条件
     *
     * @param bo 查询条件
     * @return LambdaQueryWrapper
     */
    private LambdaQueryWrapper<HouseSlope> buildQueryWrapper(HouseSlopeBo bo) {
        LambdaQueryWrapper<HouseSlope> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getHouseId()), HouseSlope::getHouseId, bo.getHouseId());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseUnitId()), HouseSlope::getHouseUnitId, bo.getHouseUnitId());
        lqw.eq(StringUtils.isNotBlank(bo.getBuildingCode()), HouseSlope::getBuildingCode, bo.getBuildingCode());
        lqw.like(StringUtils.isNotBlank(bo.getBuildingName()), HouseSlope::getBuildingName, bo.getBuildingName());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseProvinceCode()), HouseSlope::getHouseProvinceCode, bo.getHouseProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseCityCode()), HouseSlope::getHouseCityCode, bo.getHouseCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseCountyCode()), HouseSlope::getHouseCountyCode, bo.getHouseCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseStreetCode()), HouseSlope::getHouseStreetCode, bo.getHouseStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseCommunityCode()), HouseSlope::getHouseCommunityCode, bo.getHouseCommunityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseVillageCode()), HouseSlope::getHouseVillageCode, bo.getHouseVillageCode());
        lqw.eq(StringUtils.isNotBlank(bo.getGridCode()), HouseSlope::getGridCode, bo.getGridCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStreetAddressCode()), HouseSlope::getStreetAddressCode, bo.getStreetAddressCode());
        lqw.like(StringUtils.isNotBlank(bo.getDoorPlateNumber()), HouseSlope::getDoorPlateNumber, bo.getDoorPlateNumber());
        lqw.like(StringUtils.isNotBlank(bo.getCommunityName()), HouseSlope::getCommunityName, bo.getCommunityName());
        lqw.eq(StringUtils.isNotBlank(bo.getBuildingNumber()), HouseSlope::getBuildingNumber, bo.getBuildingNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getUnitNumber()), HouseSlope::getUnitNumber, bo.getUnitNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getFloorNumber()), HouseSlope::getFloorNumber, bo.getFloorNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getRoomNumber()), HouseSlope::getRoomNumber, bo.getRoomNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitId()), HouseSlope::getSlopeUnitId, bo.getSlopeUnitId());
        lqw.like(StringUtils.isNotBlank(bo.getSlopeUnitName()), HouseSlope::getSlopeUnitName, bo.getSlopeUnitName());
        lqw.eq(bo.getPilotArea1() != null, HouseSlope::getPilotArea1, bo.getPilotArea1());
        lqw.eq(bo.getPilotArea2() != null, HouseSlope::getPilotArea2, bo.getPilotArea2());
        lqw.eq(bo.getUrbanArea() != null, HouseSlope::getUrbanArea, bo.getUrbanArea());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitProvince()), HouseSlope::getSlopeUnitProvince, bo.getSlopeUnitProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCity()), HouseSlope::getSlopeUnitCity, bo.getSlopeUnitCity());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCounty()), HouseSlope::getSlopeUnitCounty, bo.getSlopeUnitCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitStreet()), HouseSlope::getSlopeUnitStreet, bo.getSlopeUnitStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitVillage()), HouseSlope::getSlopeUnitVillage, bo.getSlopeUnitVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCommunity()), HouseSlope::getSlopeUnitCommunity, bo.getSlopeUnitCommunity());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitProvinceCode()), HouseSlope::getSlopeUnitProvinceCode, bo.getSlopeUnitProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCityCode()), HouseSlope::getSlopeUnitCityCode, bo.getSlopeUnitCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCountyCode()), HouseSlope::getSlopeUnitCountyCode, bo.getSlopeUnitCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitStreetCode()), HouseSlope::getSlopeUnitStreetCode, bo.getSlopeUnitStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitVillageCode()), HouseSlope::getSlopeUnitVillageCode, bo.getSlopeUnitVillageCode());
        return lqw;
    }

    private AdRegionVo resolveAdRegion(String adRegionId) {
        String targetAdRegionId = StringUtils.defaultIfBlank(adRegionId, DEFAULT_AD_REGION_ID);
        AdRegionVo adRegion = adRegionService.getInfo(targetAdRegionId);
        if (adRegion != null) {
            return adRegion;
        }

        AdRegionBo query = new AdRegionBo();
        query.setName("恩施市");
        query.setLevel(3);
        List<AdRegionVo> regions = adRegionService.queryList(query);
        if (regions != null && !regions.isEmpty()) {
            return regions.getFirst();
        }
        throw new ServiceException("未找到对应行政区划");
    }
}
