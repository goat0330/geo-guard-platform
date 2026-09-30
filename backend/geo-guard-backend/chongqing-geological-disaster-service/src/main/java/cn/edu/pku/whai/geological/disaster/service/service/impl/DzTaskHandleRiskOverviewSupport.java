/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
class DzTaskHandleRiskOverviewSupport {

    private static final int DATA_NOT_DELETED = 0;
    private static final int RISK_OVERVIEW_TYPE_TASK_HANDLE = 1;
    private static final int RISK_OVERVIEW_TYPE_DEFENSE_RESPONSE = 2;
    private static final int ALERT_LEVEL_BLUE = 1;
    private static final int ALERT_LEVEL_YELLOW = 2;
    private static final int ALERT_LEVEL_ORANGE = 3;
    private static final int ALERT_LEVEL_RED = 4;

    private final DzTaskHandleMapper taskHandleMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final IDzDefRespPlanService dzDefRespPlanService;
    private final DzTaskHandlePermissionSupport permissionSupport;
    private final ISlopeUnitService slopeUnitService;
    private final IHazardPointService hazardPointService;

    DzRiskOverviewVo riskOverview(Integer type) {
        validateRiskOverviewType(type);
        List<AdRegionVo> adRegionVos = permissionSupport.listCurrentUserRegions("无法查看风险概况");
        if (adRegionVos.isEmpty()) {
            throw new ServiceException("当前用户未配置行政区划，无法查看风险概况");
        }
        Map<Integer, Map<String, String>> alertRegionMap = buildRiskOverviewAlertRegionMap(adRegionVos);
        long unstartedEventCount = countUnstartedEvents(type, adRegionVos);
        long ongoingEventCount = countOngoingEvents(type, adRegionVos);

        DzRiskOverviewVo vo = new DzRiskOverviewVo();
        vo.setRedAlertRegion(alertRegionMap.getOrDefault(ALERT_LEVEL_RED, Map.of()));
        vo.setOrangeAlertRegion(alertRegionMap.getOrDefault(ALERT_LEVEL_ORANGE, Map.of()));
        vo.setYellowAlertRegion(alertRegionMap.getOrDefault(ALERT_LEVEL_YELLOW, Map.of()));
        vo.setBlueAlertRegion(alertRegionMap.getOrDefault(ALERT_LEVEL_BLUE, Map.of()));
        vo.setUnstartedEventCount(String.valueOf(unstartedEventCount));
        vo.setOngoingEventCount(String.valueOf(ongoingEventCount));
        applyAffectedRangeStats(vo, type, adRegionVos);
        return vo;
    }

    Long eventCount(AdRegionVo adRegionVo, List<Integer> handleProcess) {
        LambdaQueryWrapper<DzTaskHandle> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getProvince()), DzTaskHandle::getProvince, adRegionVo.getProvince());
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getCity()), DzTaskHandle::getCity, adRegionVo.getCity());
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DzTaskHandle::getCounty, adRegionVo.getCounty());
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getStreet()), DzTaskHandle::getStreet, adRegionVo.getStreet());
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getVillage()), DzTaskHandle::getVillage, adRegionVo.getVillage());
        lqw.in(DzTaskHandle::getHandleProcess, handleProcess);
        return taskHandleMapper.selectCount(lqw);
    }

    private Map<Integer, Map<String, String>> buildRiskOverviewAlertRegionMap(List<AdRegionVo> adRegionVos) {
        return mergeAlertRegionMaps(adRegionVos.stream()
                                               .map(this::buildAlertRegionMap)
                                               .toList());
    }

    private long countUnstartedEvents(Integer type, List<AdRegionVo> adRegionVos) {
        if (Objects.equals(type, RISK_OVERVIEW_TYPE_TASK_HANDLE)) {
            return adRegionVos.stream()
                              .mapToLong(item -> eventCount(item, List.of(HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode())))
                              .sum();
        }
        return adRegionVos.stream()
                          .mapToLong(item -> dzDefRespPlanService.eventCount(
                              item,
                              List.of(DefRespPlanStatusEnum.UNSTARTED.getCode())
                          ))
                          .sum();
    }

    private long countOngoingEvents(Integer type, List<AdRegionVo> adRegionVos) {
        if (Objects.equals(type, RISK_OVERVIEW_TYPE_TASK_HANDLE)) {
            return adRegionVos.stream()
                              .mapToLong(item -> eventCount(
                                  item,
                                  List.of(
                                      HandleProcessEnum.CONSULTATION_JUDGMENT.getCode(),
                                      HandleProcessEnum.PLAN_IMPLEMENTATION.getCode(),
                                      HandleProcessEnum.RESPONSE_EXECUTION.getCode()
                                  )
                              ))
                              .sum();
        }
        return adRegionVos.stream()
                          .mapToLong(item -> dzDefRespPlanService.eventCount(
                              item,
                              List.of(
                                  DefRespPlanStatusEnum.STARTED.getCode(),
                                  DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(),
                                  DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(),
                                  DefRespPlanStatusEnum.APPROVAL_PASSED.getCode(),
                                  DefRespPlanStatusEnum.TASK_PUBLISHED.getCode()
                              )
                          ))
                          .sum();
    }

    private Map<Integer, Map<String, String>> buildAlertRegionMap(AdRegionVo adRegionVo) {
        List<Integer> startedStatuses = DefRespPlanStatusEnum.getStartedStatusCodes();
        List<DefRespPlan> allPlans = new ArrayList<>();
        allPlans.addAll(listRunningTownRegionPlans(adRegionVo, startedStatuses));
        allPlans.addAll(listRunningSinglePlans(adRegionVo, startedStatuses));

        Map<String, Map<String, Integer>> countyStreetHighestLevelMap = new LinkedHashMap<>();
        for (DefRespPlan plan : allPlans) {
            if (plan == null || plan.getLevel() == null || StringUtils.isBlank(plan.getCounty()) || StringUtils.isBlank(plan.getStreets())) {
                continue;
            }
            Map<String, Integer> streetLevelMap =
                countyStreetHighestLevelMap.computeIfAbsent(plan.getCounty(), key -> new LinkedHashMap<>());
            for (String street : splitStreetNames(plan.getStreets())) {
                Integer currentLevel = streetLevelMap.get(street);
                if (currentLevel == null || plan.getLevel() > currentLevel) {
                    streetLevelMap.put(street, plan.getLevel());
                }
            }
        }

        Map<Integer, Map<String, LinkedHashSet<String>>> levelCountyStreetMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> countyEntry : countyStreetHighestLevelMap.entrySet()) {
            for (Map.Entry<String, Integer> streetEntry : countyEntry.getValue().entrySet()) {
                Map<String, LinkedHashSet<String>> countyStreetMap =
                    levelCountyStreetMap.computeIfAbsent(streetEntry.getValue(), key -> new LinkedHashMap<>());
                LinkedHashSet<String> streetSet =
                    countyStreetMap.computeIfAbsent(countyEntry.getKey(), key -> new LinkedHashSet<>());
                streetSet.add(streetEntry.getKey());
            }
        }

        Map<Integer, Map<String, String>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<String, LinkedHashSet<String>>> levelEntry : levelCountyStreetMap.entrySet()) {
            Map<String, String> countyStreetMap = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashSet<String>> countyEntry : levelEntry.getValue().entrySet()) {
                countyStreetMap.put(countyEntry.getKey(), String.join(",", countyEntry.getValue()));
            }
            result.put(levelEntry.getKey(), countyStreetMap);
        }
        return result;
    }

    private void applyAffectedRangeStats(DzRiskOverviewVo vo, Integer type, List<AdRegionVo> adRegionVos) {
        vo.setAffectedTownCount("0");
        vo.setAffectedSlopeUnitCount("0");
        vo.setAffectedHazardPointCount("0");
        if (!Objects.equals(type, RISK_OVERVIEW_TYPE_DEFENSE_RESPONSE)) {
            return;
        }

        Set<String> affectedTowns = new LinkedHashSet<>();
        List<Integer> startedStatuses = DefRespPlanStatusEnum.getStartedStatusCodes();
        for (AdRegionVo adRegionVo : adRegionVos) {
            List<DefRespPlan> plans = listRunningTownRegionPlans(adRegionVo, startedStatuses);
            for (DefRespPlan plan : plans) {
                affectedTowns.addAll(splitStreetNames(plan.getStreets()));
            }
        }
        if (affectedTowns.isEmpty()) {
            return;
        }

        Set<String> slopeUnitIds = listAffectedSlopeUnitIds(affectedTowns);
        long affectedHazardPointCount = slopeUnitIds.isEmpty()
            ? 0L
            : Objects.requireNonNullElse(hazardPointService.countBySlopeUnitIds(new ArrayList<>(slopeUnitIds)), 0L);
        vo.setAffectedTownCount(String.valueOf(affectedTowns.size()));
        vo.setAffectedSlopeUnitCount(String.valueOf(slopeUnitIds.size()));
        vo.setAffectedHazardPointCount(String.valueOf(affectedHazardPointCount));
    }

    private Map<Integer, Map<String, String>> mergeAlertRegionMaps(List<Map<Integer, Map<String, String>>> alertRegionMaps) {
        Map<Integer, Map<String, LinkedHashSet<String>>> merged = new LinkedHashMap<>();
        for (Map<Integer, Map<String, String>> alertRegionMap : alertRegionMaps) {
            if (alertRegionMap == null) {
                continue;
            }
            for (Map.Entry<Integer, Map<String, String>> levelEntry : alertRegionMap.entrySet()) {
                Map<String, LinkedHashSet<String>> countyStreetMap =
                    merged.computeIfAbsent(levelEntry.getKey(), key -> new LinkedHashMap<>());
                for (Map.Entry<String, String> countyEntry : levelEntry.getValue().entrySet()) {
                    LinkedHashSet<String> streets =
                        countyStreetMap.computeIfAbsent(countyEntry.getKey(), key -> new LinkedHashSet<>());
                    streets.addAll(splitStreetNames(countyEntry.getValue()));
                }
            }
        }
        Map<Integer, Map<String, String>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<String, LinkedHashSet<String>>> levelEntry : merged.entrySet()) {
            Map<String, String> countyStreetMap = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashSet<String>> countyEntry : levelEntry.getValue().entrySet()) {
                countyStreetMap.put(countyEntry.getKey(), String.join(",", countyEntry.getValue()));
            }
            result.put(levelEntry.getKey(), countyStreetMap);
        }
        return result;
    }

    private Set<String> listAffectedSlopeUnitIds(Collection<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return Set.of();
        }
        List<SlopeUnitVo> slopeUnitVos = slopeUnitService.querySlopeUnitListByStreets(new ArrayList<>(streets));
        if (slopeUnitVos == null || slopeUnitVos.isEmpty()) {
            return Set.of();
        }
        Set<String> slopeUnitIds = new HashSet<>();
        for (SlopeUnitVo slopeUnitVo : slopeUnitVos) {
            if (slopeUnitVo != null && StringUtils.isNotBlank(slopeUnitVo.getId())) {
                slopeUnitIds.add(slopeUnitVo.getId().trim());
            }
        }
        return slopeUnitIds;
    }

    private List<DefRespPlan> listRunningTownRegionPlans(AdRegionVo adRegionVo, List<Integer> startedStatuses) {
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.lambdaQuery();
        lqw.select(DefRespPlan::getCounty, DefRespPlan::getStreets, DefRespPlan::getLevel);
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DefRespPlan::getCounty, adRegionVo.getCounty());
        lqw.in(DefRespPlan::getStatus, startedStatuses);
        lqw.eq(DefRespPlan::getExecuteStatus, DefRespExecuteStatusEnum.RUNNING.getCode());
        lqw.eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode());
        lqw.eq(DefRespPlan::getDeleted, DATA_NOT_DELETED);
        lqw.orderByDesc(DefRespPlan::getCreateDate);
        return filterPlansByStreet(dzDefRespPlanMapper.selectList(lqw), adRegionVo.getStreet());
    }

    private List<DefRespPlan> listRunningSinglePlans(AdRegionVo adRegionVo, List<Integer> startedStatuses) {
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.lambdaQuery();
        lqw.select(DefRespPlan::getCounty, DefRespPlan::getStreets, DefRespPlan::getLevel);
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DefRespPlan::getCounty, adRegionVo.getCounty());
        lqw.in(DefRespPlan::getStatus, startedStatuses);
        lqw.eq(DefRespPlan::getExecuteStatus, DefRespExecuteStatusEnum.RUNNING.getCode());
        lqw.isNull(DefRespPlan::getRegionScopeType);
        lqw.eq(DefRespPlan::getDeleted, DATA_NOT_DELETED);
        lqw.orderByDesc(DefRespPlan::getCreateDate);
        return filterPlansByStreet(dzDefRespPlanMapper.selectList(lqw), adRegionVo.getStreet());
    }

    private void validateRiskOverviewType(Integer type) {
        if (!Objects.equals(type, RISK_OVERVIEW_TYPE_TASK_HANDLE)
            && !Objects.equals(type, RISK_OVERVIEW_TYPE_DEFENSE_RESPONSE)) {
            throw new ServiceException("类型只能为1或2");
        }
    }

    private List<DefRespPlan> filterPlansByStreet(List<DefRespPlan> plans, String targetStreet) {
        if (StringUtils.isBlank(targetStreet) || plans == null || plans.isEmpty()) {
            return plans;
        }
        String normalizedTargetStreet = targetStreet.trim();
        List<DefRespPlan> matchedPlans = new ArrayList<>();
        for (DefRespPlan plan : plans) {
            if (containsStreet(plan, normalizedTargetStreet)) {
                matchedPlans.add(plan);
            }
        }
        return matchedPlans;
    }

    private boolean containsStreet(DefRespPlan plan, String targetStreet) {
        if (plan == null || StringUtils.isBlank(targetStreet) || StringUtils.isBlank(plan.getStreets())) {
            return false;
        }
        for (String street : plan.getStreets().split("[,，]")) {
            if (targetStreet.equals(street.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitStreetNames(String streets) {
        List<String> streetNames = new ArrayList<>();
        for (String street : streets.split("[,，]")) {
            if (StringUtils.isNotBlank(street)) {
                streetNames.add(street.trim());
            }
        }
        return streetNames;
    }
}
