/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.PersonSlopeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.PersonSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PersonSlopeVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.PersonSlopeMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IPersonSlopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 人员表与边坡单元表空间关联Service业务层处理 v_person_slope
 *
 * @author system
 * @date 2026-01-21
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PersonSlopeServiceImpl implements IPersonSlopeService {

    private final PersonSlopeMapper baseMapper;

    /**
     * 查询人员表与边坡单元表空间关联
     *
     * @param personId 人员ID
     * @return 人员表与边坡单元表空间关联
     */
    @Override
    public PersonSlopeVo queryById(String personId) {
        return baseMapper.selectVoById(personId);
    }

    /**
     * 分页查询人员表与边坡单元表空间关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 人员表与边坡单元表空间关联分页列表
     */
    @Override
    public TableDataInfo<PersonSlopeVo> queryPageList(PersonSlopeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PersonSlope> lqw = buildQueryWrapper(bo);
        Page<PersonSlopeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的人员表与边坡单元表空间关联列表
     *
     * @param bo 查询条件
     * @return 人员表与边坡单元表空间关联列表
     */
    @Override
    public List<PersonSlopeVo> queryList(PersonSlopeBo bo) {
        LambdaQueryWrapper<PersonSlope> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据边坡单元ID查询人员列表
     *
     * @param slopeUnitId 边坡单元ID
     * @return 人员列表
     */
    @Override
    public List<PersonSlopeVo> queryBySlopeUnitId(String slopeUnitId) {
        LambdaQueryWrapper<PersonSlope> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(slopeUnitId), PersonSlope::getSlopeUnitId, slopeUnitId);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 统计坐标点指定半径范围内的人口数量。
     *
     * @param lon          经度
     * @param lat          纬度
     * @param radiusMeters 容差半径（米）
     * @return 人口数量；未命中返回 0
     */
    @Override
    public Long countByPoint(Double lon, Double lat, Double radiusMeters) {
        validatePoint(lon, lat);
        double safeRadius = (radiusMeters == null || radiusMeters <= 0d) ? 10.0d : radiusMeters;
        try {
            Long count = baseMapper.countByPoint(lon, lat, safeRadius);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("人员点查询失败: lon={}, lat={}, radiusMeters={}", lon, lat, safeRadius, e);
            return 0L;
        }
    }

    @Override
    public Long countByHouse(String houseUnitId, String buildingCode) {
        if (StringUtils.isBlank(houseUnitId) && StringUtils.isBlank(buildingCode)) {
            return 0L;
        }
        try {
            Long count = baseMapper.countByHouse(houseUnitId, buildingCode);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("按房屋统计人口失败: houseUnitId={}, buildingCode={}", houseUnitId, buildingCode, e);
            return 0L;
        }
    }

    @Override
    public Long countHouseholdByHouse(String houseUnitId, String buildingCode) {
        if (StringUtils.isBlank(houseUnitId) && StringUtils.isBlank(buildingCode)) {
            return 0L;
        }
        try {
            Long count = baseMapper.countHouseholdByHouse(houseUnitId, buildingCode);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("按房屋统计户数失败: houseUnitId={}, buildingCode={}", houseUnitId, buildingCode, e);
            return 0L;
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

    /**
     * 构建查询条件
     *
     * @param bo 查询条件
     * @return LambdaQueryWrapper
     */
    private LambdaQueryWrapper<PersonSlope> buildQueryWrapper(PersonSlopeBo bo) {
        LambdaQueryWrapper<PersonSlope> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getPersonId()), PersonSlope::getPersonId, bo.getPersonId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), PersonSlope::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getPhoneNumber()), PersonSlope::getPhoneNumber, bo.getPhoneNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getGender()), PersonSlope::getGender, bo.getGender());
        lqw.eq(bo.getBirthday() != null, PersonSlope::getBirthday, bo.getBirthday());
        lqw.eq(StringUtils.isNotBlank(bo.getIdType()), PersonSlope::getIdType, bo.getIdType());
        lqw.eq(StringUtils.isNotBlank(bo.getIdNumber()), PersonSlope::getIdNumber, bo.getIdNumber());
        lqw.like(StringUtils.isNotBlank(bo.getResidenceAddress()), PersonSlope::getResidenceAddress, bo.getResidenceAddress());
        lqw.like(StringUtils.isNotBlank(bo.getHouseholdAddress()), PersonSlope::getHouseholdAddress, bo.getHouseholdAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getHouseUnitId()), PersonSlope::getHouseUnitId, bo.getHouseUnitId());
        lqw.eq(StringUtils.isNotBlank(bo.getBuildingCode()), PersonSlope::getBuildingCode, bo.getBuildingCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonProvinceCode()), PersonSlope::getPersonProvinceCode, bo.getPersonProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonCityCode()), PersonSlope::getPersonCityCode, bo.getPersonCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonCountyCode()), PersonSlope::getPersonCountyCode, bo.getPersonCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonStreetCode()), PersonSlope::getPersonStreetCode, bo.getPersonStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonCommunityCode()), PersonSlope::getPersonCommunityCode, bo.getPersonCommunityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonVillageCode()), PersonSlope::getPersonVillageCode, bo.getPersonVillageCode());
        lqw.eq(StringUtils.isNotBlank(bo.getGridCode()), PersonSlope::getGridCode, bo.getGridCode());
        lqw.like(StringUtils.isNotBlank(bo.getGridName()), PersonSlope::getGridName, bo.getGridName());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitId()), PersonSlope::getSlopeUnitId, bo.getSlopeUnitId());
        lqw.like(StringUtils.isNotBlank(bo.getSlopeUnitName()), PersonSlope::getSlopeUnitName, bo.getSlopeUnitName());
        lqw.eq(bo.getPilotArea1() != null, PersonSlope::getPilotArea1, bo.getPilotArea1());
        lqw.eq(bo.getPilotArea2() != null, PersonSlope::getPilotArea2, bo.getPilotArea2());
        lqw.eq(bo.getUrbanArea() != null, PersonSlope::getUrbanArea, bo.getUrbanArea());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitProvince()), PersonSlope::getSlopeUnitProvince, bo.getSlopeUnitProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCity()), PersonSlope::getSlopeUnitCity, bo.getSlopeUnitCity());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCounty()), PersonSlope::getSlopeUnitCounty, bo.getSlopeUnitCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitStreet()), PersonSlope::getSlopeUnitStreet, bo.getSlopeUnitStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitVillage()), PersonSlope::getSlopeUnitVillage, bo.getSlopeUnitVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCommunity()), PersonSlope::getSlopeUnitCommunity, bo.getSlopeUnitCommunity());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitProvinceCode()), PersonSlope::getSlopeUnitProvinceCode, bo.getSlopeUnitProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCityCode()), PersonSlope::getSlopeUnitCityCode, bo.getSlopeUnitCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitCountyCode()), PersonSlope::getSlopeUnitCountyCode, bo.getSlopeUnitCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitStreetCode()), PersonSlope::getSlopeUnitStreetCode, bo.getSlopeUnitStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitVillageCode()), PersonSlope::getSlopeUnitVillageCode, bo.getSlopeUnitVillageCode());
        return lqw;
    }
}
