/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.EvacuationPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.EvacuationPlanMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IEvacuationPlanService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 撤离方案表 Service 实现
 *
 * @author whai
 * @date 2026-02-04
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EvacuationPlanServiceImpl implements IEvacuationPlanService {

    private static final String ENSHI_BUILDING_NAME_PREFIX = "湖北省恩施土家族苗族自治州恩施市";
    private static final int ELDERLY_AGE_LIMIT = 65;
    private static final int CHILDREN_AGE_LIMIT = 18;

    private final EvacuationPlanMapper baseMapper;
    private final DataHouseMapper dataHouseMapper;
    private final DataPersonMapper dataPersonMapper;

    @Override
    public EvacuationPlanVo queryById(Long id) {
        return baseMapper.selectEvacuationPlanVoById(id);
    }

    @Override
    public List<EvacuationPlan> listByPlanId(Long handleId) {
        LambdaQueryWrapper<EvacuationPlan> lqw = Wrappers.lambdaQuery();
        lqw.eq(EvacuationPlan::getHandleId, handleId)
           .orderByAsc(EvacuationPlan::getId);
        return baseMapper.selectList(lqw);
    }

    @Override
    public EvacuationPlanRouteResultVo queryRoutesByHandleId(Long handleId) {
        LambdaQueryWrapper<EvacuationPlan> lqw = Wrappers.lambdaQuery();
        lqw.eq(EvacuationPlan::getHandleId, handleId)
           .orderByAsc(EvacuationPlan::getId);
        List<EvacuationPlanVo> planList = baseMapper.selectEvacuationPlanVoList(lqw);

        EvacuationPlanRouteResultVo result = new EvacuationPlanRouteResultVo();
        result.setHandleId(handleId);
        EvacuationPlanSlopeTerrainVo slopeTerrain = baseMapper.selectSlopeTerrainByHandleId(handleId);
        if (slopeTerrain != null) {
            result.setSlopeDirection(slopeTerrain.getSlopeDirection());
            result.setSlopeAngle(slopeTerrain.getSlopeAngle());
        }
        if (planList == null || planList.isEmpty()) {
            result.setDisasterPolygonsWktList(List.of());
            result.setResettlementAreas(List.of());
            result.setRoutes(List.of());
            return result;
        }

        result.setDisasterPolygonsWktList(resolveDisasterPolygonsList(planList));
        result.setResettlementAreas(resolveResettlementAreas(planList));
        List<EvacuationPlanRouteDetailVo> routes = new ArrayList<>(planList.size());
        for (EvacuationPlanVo plan : planList) {
            EvacuationPlanRouteDetailVo route = new EvacuationPlanRouteDetailVo();
            route.setEvacuationArea(plan.getEvacuationArea());
            route.setResettlementPoint(plan.getResettlementLocation());
            route.setDistanceKm(plan.getDistanceKm());
            ResidentsInfoVo residentsInfo = resolveResidentsInfo(plan);
            Integer estimatedTimeMinutes = plan.getEstimatedTimeMinutes();
            if (estimatedTimeMinutes == null && residentsInfo != null) {
                estimatedTimeMinutes = residentsInfo.getEstimatedTimeMinutes();
            }
            route.setEstimatedTimeMinutes(estimatedTimeMinutes);
            route.setEvacuationDirection(plan.getEvacuationRoute());
            route.setEvacuationRoad(plan.getEvacuationRoad());
            route.setEvacuationAreaToRoad(plan.getEvacuationAreaToRoad());
            route.setRoadToResettlement(plan.getRoadToResettlement());
            route.setResidentsInfo(residentsInfo);
            routes.add(route);
        }
        result.setRoutes(routes);
        return result;
    }

    @Override
    public TableDataInfo<EvacuationPlanVo> queryPageList(EvacuationPlanBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<EvacuationPlan> lqw = buildQueryWrapper(bo);
        Page<EvacuationPlanVo> page = pageQuery.build();
        IPage<EvacuationPlanVo> result = baseMapper.selectEvacuationPlanVoPage(page, lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<EvacuationPlanVo> queryList(EvacuationPlanBo bo) {
        LambdaQueryWrapper<EvacuationPlan> lqw = buildQueryWrapper(bo);
        return baseMapper.selectEvacuationPlanVoList(lqw);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<EvacuationPlan> buildQueryWrapper(EvacuationPlanBo bo) {
        return Wrappers.<EvacuationPlan>lambdaQuery()
                       .eq(bo.getId() != null, EvacuationPlan::getId, bo.getId())
                       .eq(bo.getHandleId() != null, EvacuationPlan::getHandleId, bo.getHandleId())
                       .like(StringUtils.isNotBlank(bo.getEvacuationArea()), EvacuationPlan::getEvacuationArea, bo.getEvacuationArea())
                       .like(StringUtils.isNotBlank(bo.getResettlementLocation()), EvacuationPlan::getResettlementLocation, bo.getResettlementLocation())
                       .like(StringUtils.isNotBlank(bo.getEvacuationRoute()), EvacuationPlan::getEvacuationRoute, bo.getEvacuationRoute())
                       .orderByDesc(EvacuationPlan::getCreateTime);
    }

    private List<String> resolveDisasterPolygonsList(List<EvacuationPlanVo> planList) {
        for (EvacuationPlanVo plan : planList) {
            if (plan == null || StringUtils.isBlank(plan.getEvacuationAreaWkt())) {
                continue;
            }
            return resolvePolygonWkts(plan.getEvacuationAreaWkt());
        }
        return List.of();
    }

    private List<EvacuationPlanRouteResultVo.ResettlementAreaItem> resolveResettlementAreas(List<EvacuationPlanVo> planList) {
        if (planList == null || planList.isEmpty()) {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        List<EvacuationPlanRouteResultVo.ResettlementAreaItem> result = new ArrayList<>();
        for (EvacuationPlanVo plan : planList) {
            if (plan == null) {
                continue;
            }
            String name = plan.getResettlementLocation();
            String wkt = plan.getResettlementWkt();
            String pointJson = plan.getResettlementPoint();
            if (StringUtils.isBlank(name) && StringUtils.isBlank(wkt) && StringUtils.isBlank(pointJson)) {
                continue;
            }
            String key = (name == null ? "" : name.trim())
                + "|" + (wkt == null ? "" : wkt.trim())
                + "|" + (pointJson == null ? "" : pointJson.trim());
            if (keys.contains(key)) {
                continue;
            }
            keys.add(key);
            EvacuationPlanRouteResultVo.ResettlementAreaItem item = new EvacuationPlanRouteResultVo.ResettlementAreaItem();
            item.setName(name == null ? null : name.trim());
            item.setAreaWkt(wkt == null ? null : wkt.trim());
            item.setPoint(parseGeoPoint(pointJson));
            result.add(item);
        }
        return result;
    }

    /**
     * 兼容单个WKT和JSON数组字符串两种格式。
     */
    private List<String> resolvePolygonWkts(String polygonWkt) {
        String text = polygonWkt == null ? null : polygonWkt.trim();
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        if (!text.startsWith("[")) {
            return List.of(text);
        }
        List<String> values = JacksonUtil.parseObject(text, new TypeReference<>() {
        });
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private GeoPointVo parseGeoPoint(String pointJson) {
        String text = pointJson == null ? null : pointJson.trim();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return JacksonUtil.objectMapper.readValue(text, GeoPointVo.class);
        } catch (JsonProcessingException e) {
            log.warn("解析安置点坐标失败，pointJson={}", pointJson, e);
            return null;
        }
    }

    private ResidentsInfoVo parseResidentsInfo(String residentsInfoJson) {
        String text = residentsInfoJson == null ? null : residentsInfoJson.trim();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return JacksonUtil.objectMapper.readValue(text, ResidentsInfoVo.class);
        } catch (JsonProcessingException e) {
            log.warn("解析住户信息失败，residentsInfoJson={}", residentsInfoJson, e);
            return null;
        }
    }

    private ResidentsInfoVo resolveResidentsInfo(EvacuationPlanVo plan) {
        if (plan == null) {
            return null;
        }
        ResidentsInfoVo fallback = parseResidentsInfo(plan.getResidentsInfo());
        List<String> candidateBuildingNames = buildCandidateBuildingNames(plan.getEvacuationArea());
        if (candidateBuildingNames.isEmpty()) {
            return patchResidentsInfoCount(fallback, List.of());
        }
        List<House> houses = dataHouseMapper.selectList(
            Wrappers.<House>lambdaQuery().in(House::getBuildingName, candidateBuildingNames)
        );
        if (houses == null || houses.isEmpty()) {
            return patchResidentsInfoCount(fallback, List.of());
        }
        Set<String> houseUnitIds = new LinkedHashSet<>();
        Set<String> buildingCodes = new LinkedHashSet<>();
        for (House house : houses) {
            if (house == null) {
                continue;
            }
            if (StringUtils.isNotBlank(house.getHouseUnitId())) {
                houseUnitIds.add(house.getHouseUnitId().trim());
            }
            if (StringUtils.isNotBlank(house.getBuildingCode())) {
                buildingCodes.add(house.getBuildingCode().trim());
            }
        }
        if (houseUnitIds.isEmpty() && buildingCodes.isEmpty()) {
            return patchResidentsInfoCount(fallback, List.of());
        }
        List<Person> persons;
        if (!houseUnitIds.isEmpty()) {
            persons = dataPersonMapper.selectList(
                Wrappers.<Person>lambdaQuery().in(Person::getHouseUnitId, houseUnitIds)
            );
        } else {
            persons = dataPersonMapper.selectList(
                Wrappers.<Person>lambdaQuery().in(Person::getBuildingCode, buildingCodes)
            );
        }
        return patchResidentsInfoCount(fallback, persons);
    }

    private ResidentsInfoVo patchResidentsInfoCount(ResidentsInfoVo fallback, List<Person> persons) {
        if ((persons == null || persons.isEmpty())) {
            return fallback;
        }
        int totalCount = persons.size();
        int elderlyCount = 0;
        int childrenCount = 0;
        for (Person person : persons) {
            Integer age = person.getAge();
            if (age == null) {
                continue;
            }
            if (age >= ELDERLY_AGE_LIMIT) {
                elderlyCount++;
            }
            if (age < CHILDREN_AGE_LIMIT) {
                childrenCount++;
            }
        }
        ResidentsInfoVo result = fallback == null ? new ResidentsInfoVo() : fallback;
        result.setTotalCount(totalCount);
        result.setElderlyCount(elderlyCount);
        result.setChildrenCount(childrenCount);
        result.setHasResidents(totalCount > 0);
        if (StringUtils.isBlank(result.getHouseholdHeadName())) {
            result.setHouseholdHeadName(extractFirstNonBlank(persons, true));
        }
        if (StringUtils.isBlank(result.getHouseholdHeadPhone())) {
            result.setHouseholdHeadPhone(extractFirstNonBlank(persons, false));
        }
        return result;
    }

    private String extractFirstNonBlank(List<Person> persons, boolean useName) {
        if (persons == null || persons.isEmpty()) {
            return null;
        }
        return persons.stream()
                      .filter(Objects::nonNull)
                      .map(person -> useName ? person.getName() : person.getPhoneNumber())
                      .filter(StringUtils::isNotBlank)
                      .map(String::trim)
                      .findFirst()
                      .orElse(null);
    }

    private List<String> buildCandidateBuildingNames(String evacuationArea) {
        String trimmed = evacuationArea == null ? null : evacuationArea.trim();
        if (StringUtils.isBlank(trimmed)) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(trimmed);
        String stripped = stripEnshiBuildingNamePrefix(trimmed);
        if (StringUtils.isNotBlank(stripped)) {
            names.add(stripped);
        }
        if (!trimmed.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            names.add(ENSHI_BUILDING_NAME_PREFIX + trimmed);
        }
        if (StringUtils.isNotBlank(stripped) && !stripped.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            names.add(ENSHI_BUILDING_NAME_PREFIX + stripped);
        }
        return new ArrayList<>(names);
    }

    private String stripEnshiBuildingNamePrefix(String buildingName) {
        if (StringUtils.isBlank(buildingName)) {
            return buildingName;
        }
        String trimmed = buildingName.trim();
        if (trimmed.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            return trimmed.substring(ENSHI_BUILDING_NAME_PREFIX.length()).trim();
        }
        return trimmed;
    }
}
