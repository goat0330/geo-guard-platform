/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.*;
import cn.edu.pku.whai.geological.disaster.data.domain.req.SlopeUnitReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitPersonVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.*;
import cn.edu.pku.whai.geological.disaster.data.service.IGridMemberService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 斜坡单元Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SlopeUnitServiceImpl implements ISlopeUnitService {

    private final SlopeUnitMapper baseMapper;
    private final ISysUserService sysUserService;
    private final HazardPointMapper hazardPointMapper;
    private final RiskZoneMapper riskZoneMapper;
    private final HouseSlopeMapper houseSlopeMapper;
    private final PersonSlopeMapper personSlopeMapper;
    private final IGridMemberService gridMemberService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final DataSlopeGeologyMapper dataSlopeGeologyMapper;

    /**
     * 查询斜坡单元
     *
     * @param id 主键
     * @return 斜坡单元
     */
    @Override
    public SlopeUnitVo queryById(String id) {
        SlopeUnit slopeUnit = baseMapper.selectById(id);
        SlopeUnitVo slopeUnitVo = toSlopeUnitVo(slopeUnit);
        if (slopeUnitVo == null) {
            return null;
        }

        long hazardPointCount = hazardPointMapper.selectCount(Wrappers.<HazardPoint>lambdaQuery()
            .eq(HazardPoint::getPilotArea1, 1)
            .eq(HazardPoint::getSlopeUnitId, id));
        slopeUnitVo.setHazardPointCount(hazardPointCount);

        long riskZoneCount = riskZoneMapper.selectCount(Wrappers.<RiskZone>lambdaQuery()
            .eq(RiskZone::getPilotArea1, 1)
            .eq(RiskZone::getSlopeUnitId, id)
        );
        slopeUnitVo.setRiskZoneCount(riskZoneCount);

        // 通过 v_house_slope 视图，使用 building_code 去重后计算建筑总数
        Long buildingCount = houseSlopeMapper.countDistinctBuildingCode(
            Wrappers.<HouseSlope>lambdaQuery()
                .eq(HouseSlope::getSlopeUnitId, id)
                .isNotNull(HouseSlope::getBuildingCode)
        );
        slopeUnitVo.setBuildingCount(buildingCount != null ? buildingCount : 0L);

        // 通过 v_person_slope 视图计算人口总数
        Long populationCount = personSlopeMapper.countBySlopeUnitId(
            Wrappers.<PersonSlope>lambdaQuery()
                .eq(PersonSlope::getSlopeUnitId, id)
        );
        slopeUnitVo.setPopulationCount(populationCount != null ? populationCount : 0L);

        Integer dynamicRiskLevel = baseMapper.selectDynamicRiskLevelBySlopeUnitId(id);
        slopeUnitVo.setDynamicRiskLevel(dynamicRiskLevel);

        return slopeUnitVo;
    }

    /**
     * 根据斜坡单元id查询基础信息（data_slope_unit + data_slope_geology 单次联表查询）
     *
     * @param id 斜坡单元主键
     * @return 组合视图对象；斜坡单元不存在时返回 null
     */
    @Override
    public SlopeUnitGeologyVo queryGeologyInfoById(String id) {
        return dataSlopeGeologyMapper.selectBaseInfoById(id);
    }

    /**
     * 分页查询斜坡单元列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 斜坡单元分页列表
     */
    @Override
    public TableDataInfo<SlopeUnitVo> queryPageList(SlopeUnitBo bo, PageQuery pageQuery) {
        List<Integer> dynamicRiskLevels = getValidDynamicRiskLevels(bo.getDynamicRiskLevelList());
        QueryWrapper<SlopeUnit> qew = buildQueryWrapper(bo, dynamicRiskLevels);
        Page<SlopeUnitVo> result;
        if (CollUtil.isNotEmpty(dynamicRiskLevels)) {
            // 传入风险级别时执行联表查询
            result = baseMapper.selectVoPageByRiskLevel(pageQuery.build(), qew);
        } else {
            // 未传入风险级别时执行单表查询
            result = baseMapper.selectVoPage(pageQuery.build(), qew);
        }
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<SlopeUnitVo> queryPageListNoWkt(SlopeUnitBo bo, PageQuery pageQuery) {
        List<Integer> dynamicRiskLevels = getValidDynamicRiskLevels(bo.getDynamicRiskLevelList());
        QueryWrapper<SlopeUnit> qew = buildQueryWrapper(bo, dynamicRiskLevels);
        Page<SlopeUnitVo> result = CollUtil.isNotEmpty(dynamicRiskLevels)
            ? baseMapper.selectVoPageByRiskLevelNoWkt(pageQuery.build(), qew)
            : baseMapper.selectVoPageNoWkt(pageQuery.build(), qew);
        return TableDataInfo.build(result);
    }

    private QueryWrapper<SlopeUnit> buildQueryWrapper(SlopeUnitBo bo, List<Integer> dynamicRiskLevels) {
        QueryWrapper<SlopeUnit> qew = Wrappers.query();
        qew.likeRight(ObjUtil.isNotNull(bo.getId()), "dsu.id", bo.getId());
        qew.like(StringUtils.isNotBlank(bo.getName()), "name", bo.getName());
        qew.eq(bo.getPilotArea1() != null, "pilot_area_1", bo.getPilotArea1());
        qew.eq(bo.getPilotArea2() != null, "pilot_area_2", bo.getPilotArea2());
        qew.eq(bo.getUrbanArea() != null, "urban_area", bo.getUrbanArea());
        qew.eq(StringUtils.isNotBlank(bo.getProvince()), "province", bo.getProvince());
        qew.eq(StringUtils.isNotBlank(bo.getCity()), "city", bo.getCity());
        qew.eq(StringUtils.isNotBlank(bo.getCounty()), "county", bo.getCounty());
        qew.eq(StringUtils.isNotBlank(bo.getStreet()), "street", bo.getStreet());
        qew.eq(StringUtils.isNotBlank(bo.getVillage()), "village", bo.getVillage());
        qew.eq(StringUtils.isNotBlank(bo.getCommunity()), "community", bo.getCommunity());
        qew.eq(StringUtils.isNotBlank(bo.getCenter()), "center", bo.getCenter());
        qew.eq(StringUtils.isNotBlank(bo.getWkt()), "wkt", bo.getWkt());
        qew.eq(bo.getArea() != null, "area", bo.getArea());
        qew.eq(StringUtils.isNotBlank(bo.getProvinceCode()), "province_code", bo.getProvinceCode());
        qew.eq(StringUtils.isNotBlank(bo.getCityCode()), "city_code", bo.getCityCode());
        qew.eq(StringUtils.isNotBlank(bo.getCountyCode()), "county_code", bo.getCountyCode());
        qew.eq(StringUtils.isNotBlank(bo.getStreetCode()), "street_code", bo.getStreetCode());
        qew.eq(StringUtils.isNotBlank(bo.getVillageCode()), "village_code", bo.getVillageCode());
        if (CollUtil.isNotEmpty(dynamicRiskLevels)) {
            qew.in("dra.dynamic_risk_level", dynamicRiskLevels);
            appendDynamicRiskCreateDateRange(qew, bo);
        }
        qew.orderByAsc("id");
        return qew;
    }

    private void appendDynamicRiskCreateDateRange(QueryWrapper<SlopeUnit> qew, SlopeUnitBo bo) {
        Map<String, Object> params = bo.getParams();
        Object beginTime = params == null ? null : params.get("beginTime");
        Object endTime = params == null ? null : params.get("endTime");
        if (isTimeParamPresent(beginTime) && isTimeParamPresent(endTime)) {
            qew.between("dra.create_date", parseTimeParam(beginTime), parseTimeParam(endTime));
            return;
        }
        qew.ge("dra.create_date", DateUtil.beginOfDay(new Date()));
    }

    private boolean isTimeParamPresent(Object time) {
        return time != null && (time instanceof Date || StringUtils.isNotBlank(String.valueOf(time)));
    }

    private Date parseTimeParam(Object time) {
        if (time instanceof Date date) {
            return date;
        }
        return DateUtil.parseDateTime(String.valueOf(time));
    }


    @Override
    public List<SlopeUnitVo> querySlopeUnitList(SlopeUnitReq bo) {
        List<Integer> dynamicRiskLevels = getValidDynamicRiskLevels(bo.getDynamicRiskLevelList());
        QueryWrapper<SlopeUnit> qew = Wrappers.query();
        qew.eq(bo.getId() != null, "dsu.id", bo.getId());
        qew.like(StringUtils.isNotBlank(bo.getName()), "name", bo.getName());
        qew.like(StringUtils.isNotBlank(bo.getArea()), "concat(province, city, county, street, village, community)", bo.getArea());
        if (CollUtil.isNotEmpty(dynamicRiskLevels)) {
            qew.in("dra.dynamic_risk_level", dynamicRiskLevels);
            qew.ge("dra.create_date", DateUtil.beginOfDay(new Date()));
        }
        return baseMapper.selectRespByNlp(qew);
    }

    private List<Integer> getValidDynamicRiskLevels(List<Integer> dynamicRiskLevels) {
        if (CollUtil.isEmpty(dynamicRiskLevels)) {
            return List.of();
        }
        return dynamicRiskLevels.stream()
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public SlopeUnitPersonVo getSlopeUnitPerson(String id) {
        SlopeUnitPersonVo vo = new SlopeUnitPersonVo();
        vo.setSlopeUnitId(id);
        SlopeUnit slopeUnit = baseMapper.selectById(id);
        if (slopeUnit == null) {
            throw new ServiceException("斜坡单元不存在");
        }

        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(id);
        if (relationVo != null) {
            String userName = StringUtils.isNotBlank(relationVo.getInspector())
                ? relationVo.getInspector()
                : relationVo.getSpecialManager();
            String phoneNumber = StringUtils.isNotBlank(relationVo.getInspector())
                ? relationVo.getInspectorPhone()
                : relationVo.getSpecialManagerPhone();
            if (fillPerson(vo, userName, phoneNumber)) {
                return vo;
            }
        }

        String village = slopeUnit.getVillageCode();
        if (StringUtils.isBlank(village)) {
            return vo;
        }
        var gridMember = gridMemberService.queryByGridId(village);
        if (gridMember == null) {
            return vo;
        }
        fillPerson(vo, gridMember.getUserName(), gridMember.getPhonenumber());
        return vo;
    }

    private boolean fillPerson(SlopeUnitPersonVo vo, String userName, String phoneNumber) {
        if (StringUtils.isBlank(userName) && StringUtils.isBlank(phoneNumber)) {
            return false;
        }
        vo.setUserName(userName);
        if (StringUtils.isNotBlank(phoneNumber)) {
            SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(phoneNumber);
            if (sysUserVo != null) {
                vo.setUserId(sysUserVo.getUserId());
            }
        }
        vo.setPhoneNumber(phoneNumber);
        return true;
    }

    @Override
    public SlopeUnit queryPoByCenter(String center) {
        return baseMapper.selectSlopeUnitByCenter(center);
    }

    @Override
    public List<SlopeUnit> listPoByIds(List<String> ids) {
        return baseMapper.selectByIds(ids);
    }

    @Override
    public List<SlopeUnitVo> queryNoWktByIds(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<SlopeUnit> slopeUnits = baseMapper.selectByIds(ids);
        if (CollUtil.isEmpty(slopeUnits)) {
            return List.of();
        }
        Map<String, Integer> orderMap = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (StringUtils.isNotBlank(id)) {
                orderMap.putIfAbsent(id.trim(), i);
            }
        }
        return slopeUnits.stream()
                         .filter(Objects::nonNull)
                         .sorted((left, right) -> Integer.compare(
                             orderMap.getOrDefault(StringUtils.isBlank(left.getId()) ? null : left.getId().trim(), Integer.MAX_VALUE),
                             orderMap.getOrDefault(StringUtils.isBlank(right.getId()) ? null : right.getId().trim(), Integer.MAX_VALUE)
                         ))
                         .map(this::toSlopeUnitVoWithoutWkt)
                         .toList();
    }

    @Override
    public List<SlopeUnit> listAllWithWkt() {
        LambdaQueryWrapper<SlopeUnit> lqw = Wrappers.lambdaQuery();
        lqw.isNotNull(SlopeUnit::getWkt);
        return baseMapper.selectList(lqw);
    }

    @Override
    public List<SlopeUnit> listAllWithCenter() {
        LambdaQueryWrapper<SlopeUnit> lqw = Wrappers.lambdaQuery();
        lqw.isNotNull(SlopeUnit::getCenter);
        return baseMapper.selectList(lqw);
    }

    @Override
    public Long countPilotArea1() {
        LambdaQueryWrapper<SlopeUnit> lqw = Wrappers.lambdaQuery();
        lqw.eq(SlopeUnit::getPilotArea1, 1);
        return baseMapper.selectCount(lqw);
    }

    @Override
    public Long countPilotArea1(SlopeUnitBo bo) {
        QueryWrapper<SlopeUnit> qew = Wrappers.query();
        qew.eq(StringUtils.isNotBlank(bo.getProvince()), "province", bo.getProvince());
        qew.eq(StringUtils.isNotBlank(bo.getCity()), "city", bo.getCity());
        qew.eq(StringUtils.isNotBlank(bo.getCounty()), "county", bo.getCounty());
        qew.eq(StringUtils.isNotBlank(bo.getStreet()), "street", bo.getStreet());
        qew.eq(StringUtils.isNotBlank(bo.getVillage()), "village", bo.getVillage());
        qew.eq(bo.getPilotArea1() != null, "pilot_area_1", bo.getPilotArea1());
        qew.eq(bo.getPilotArea2() != null, "pilot_area_2", bo.getPilotArea2());
        return baseMapper.selectCount(qew);
    }

    @Override
    public Long countPilotArea1ByAdRegionIds(SlopeUnitBo bo, List<String> adRegionIds) {
        QueryWrapper<SlopeUnit> qew = Wrappers.query();
        qew.eq(bo.getPilotArea1() != null, "pilot_area_1", bo.getPilotArea1());
        qew.eq(bo.getPilotArea2() != null, "pilot_area_2", bo.getPilotArea2());
        qew.and(wrapper -> wrapper.in("province_code", adRegionIds)
            .or().in("city_code", adRegionIds)
            .or().in("county_code", adRegionIds)
            .or().in("street_code", adRegionIds)
            .or().in("village_code", adRegionIds));
        return baseMapper.selectCount(qew);
    }

    @Override
    public List<SlopeUnitVo> querySlopeUnitListByStreets(List<String> result) {
        LambdaQueryWrapper<SlopeUnit> lqw = Wrappers.lambdaQuery();
        lqw.in(SlopeUnit::getStreet, result);
        return baseMapper.selectList(lqw)
                         .stream()
                         .filter(Objects::nonNull)
                         .map(this::toSlopeUnitVo)
                         .collect(Collectors.toList());
    }

    private SlopeUnitVo toSlopeUnitVo(SlopeUnit slopeUnit) {
        return slopeUnit == null ? null : BeanUtil.copyProperties(slopeUnit, SlopeUnitVo.class);
    }

    private SlopeUnitVo toSlopeUnitVoWithoutWkt(SlopeUnit slopeUnit) {
        SlopeUnitVo vo = toSlopeUnitVo(slopeUnit);
        if (vo != null) {
            vo.setWkt(null);
        }
        return vo;
    }

    /**
     * 根据经纬度查询命中的斜坡单元
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的斜坡单元；未命中返回 null
     */
    @Override
    public SlopeUnit queryByPoint(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new ServiceException("经纬度范围非法, lon=" + lon + ", lat=" + lat);
        }
        return baseMapper.selectByPoint(lon, lat);
    }
}
