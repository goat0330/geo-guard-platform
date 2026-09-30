/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.RegionDefRespCloseReason;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespAlarmLevelUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespRegionAlarmDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IDzTaskDistListService dzTaskDistListService;

    DzDefRespRegionAlarmDelegate(DzDefRespPlanServiceImpl service, IDzTaskDistListService dzTaskDistListService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzTaskDistListService = dzTaskDistListService;
    }

    private DefRespPlan getDefRespPlanById(Long defId) { return service.getDefRespPlanById(defId); }

    private void validateCountyRegionConstraints(String county, String streets, Long currentCountyId) { service.validateCountyRegionConstraints(county, streets, currentCountyId); }

    private DefRespPlan findTownByCountyAndStreet(Long countyId, String street) { return service.findTownByCountyAndStreet(countyId, street); }

    private boolean isTownRowActive(DefRespPlan town) { return service.isTownRowActive(town); }

    private DefRespPlan getActiveCountyRegionPlanById(Long countyDefId) { return service.getActiveCountyRegionPlanById(countyDefId); }

    private List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return service.listTownPlansByCountyId(countyDefId); }

    private DefRespPlan requireUniqueEffectiveCountyRegionPlan(String county) { return service.requireUniqueEffectiveCountyRegionPlan(county); }

    private List<String> splitStreets(String streets) { return DzDefRespPlanServiceImpl.splitStreets(streets); }

    private Map<Long, DzDefRespSmsEventFactory.CountySmsState> captureStartedCountySmsStates() {
        return service.captureStartedCountySmsStates();
    }

    private void triggerAdjustedSmsForCapturedStates(
        Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeStates, Date actionTime) {
        service.triggerAdjustedSmsForCapturedStates(beforeStates, actionTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public DefRespPlan updateCountyRegionFromAlarm(DefRespPlanVo countyVo, DataAlarmVo dataAlarmVo) {
        if (countyVo == null || countyVo.getId() == null || dataAlarmVo == null) {
            return null;
        }
        DefRespPlan county = getDefRespPlanById(countyVo.getId());
        if (!service.isCountyRegionPlan(county)) {
            return null;
        }
        Integer expectedRoundNo = county.getCurrentRoundNo();
        Integer expectedStatus = county.getStatus();
        Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeSmsStates = captureStartedCountySmsStates();
        List<String> oldStreets = splitStreets(county.getStreets());
        List<String> newStreets = resolveAlarmStreets(dataAlarmVo);
        Set<String> oldSet = new LinkedHashSet<>(oldStreets);
        Set<String> newSet = new LinkedHashSet<>(newStreets);
        Date now = new Date();
        Integer newLevel = resolveAlarmHighestDefenseLevel(dataAlarmVo);
        validateCountyRegionLevelImmutable(county, newLevel);
        validateCountyRegionConstraints(county.getCounty(), String.join(",", newSet), county.getId());

        // 预警街道集合相对县级当前 streets：减少 → 关乡镇；增加 → 插乡镇
        for (String s : oldSet) {
            if (!newSet.contains(s)) {
                DefRespPlan town = findTownByCountyAndStreet(county.getId(), s);
                if (isTownRowActive(town)) {
                    service.closeTownPlan(town, RegionDefRespCloseReason.TOWN_RANGE_SHRINK, now);
                }
            }
        }
        for (String s : newSet) {
            if (!oldSet.contains(s)) {
                service.insertTownPlanUnderCounty(county, dataAlarmVo, s, now);
            }
        }
        // 写回县级权威字段；乡镇等级不独立流转，只继承当前关联县级的等级与触发条件。
        county.setStreets(String.join(",", newSet));
        county.setTriggerCondition(buildRegionTriggerCondition(dataAlarmVo));
        county.setSourceAlarmId(dataAlarmVo.getId());
        county.setUpdateDate(now);
        advanceCountyRoundNoIfNeeded(county, !Objects.equals(oldSet, newSet), false);
        syncActiveTownInheritedFields(county.getId(), county.getLevel(), county.getTriggerCondition(), now);
        persistCountyAggregatedFieldsOrThrow(
            county, now, "ALARM_STREET_SYNC", expectedRoundNo, expectedStatus);
        syncActiveTownNonLevelInheritedFieldsFromCounty(county.getId(), county, now);
        triggerAdjustedSmsForCapturedStates(beforeSmsStates, now);
        return county;
    }


    /**
     * 根据预警生成或更新区域方案
     */
    @Transactional(rollbackFor = Exception.class)
    public DefRespPlan generateOrUpdateRegionPlansFromAlarm(DataAlarmVo dataAlarmVo) {
        if (dataAlarmVo == null || StringUtils.isBlank(dataAlarmVo.getCity())) {
            return null;
        }
        Map<String, Integer> streetWarningLevels = resolveAlarmStreetWarningLevelMap(dataAlarmVo);
        if (streetWarningLevels.isEmpty()) {
            return null;
        }
        Date now = new Date();
        DefRespPlan countyPlan = findExistingCountyRegionPlanForAlarm(dataAlarmVo);
        if (countyPlan == null) {
            log.info("当前不存在可更新的县级区域防御响应，跳过预警联动, alarmId={}, county={}",
                dataAlarmVo.getId(), dataAlarmVo.getCity());
            return null;
        }
        Integer expectedRoundNo = countyPlan.getCurrentRoundNo();
        Integer expectedStatus = countyPlan.getStatus();
        Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeSmsStates = captureStartedCountySmsStates();
        Set<String> oldCountyStreets = new LinkedHashSet<>(splitStreets(countyPlan.getStreets()));
        Set<Long> changedTownDefIds = new LinkedHashSet<>();
        Set<Long> formerCountyIds = new LinkedHashSet<>();
        boolean townAssociationChanged = false;
        boolean townLevelChanged = false;
        for (Map.Entry<String, Integer> entry : streetWarningLevels.entrySet()) {
            String street = entry.getKey();
            Integer defRespLevel = resolveDefenseResponseLevel(dataAlarmVo, entry.getValue());
            if (defRespLevel == null) {
                continue;
            }
            DefRespPlan townPlan = findEffectiveTownRegionPlanByCountyAndStreet(dataAlarmVo.getCity(), street);
            if (townPlan == null) {
                DefRespPlan createdTown = service.insertTownPlanUnderCounty(countyPlan, dataAlarmVo, street, defRespLevel, now);
                if (createdTown != null && createdTown.getId() != null) {
                    changedTownDefIds.add(createdTown.getId());
                    townAssociationChanged = true;
                }
                continue;
            }
            boolean changed = false;
            if (!Objects.equals(townPlan.getParentDefId(), countyPlan.getId())) {
                Long formerParentId = townPlan.getParentDefId();
                townPlan.setParentDefId(countyPlan.getId());
                if (formerParentId != null && !Objects.equals(formerParentId, countyPlan.getId())) {
                    formerCountyIds.add(formerParentId);
                }
                townAssociationChanged = true;
                changed = true;
            }
            if (!Objects.equals(townPlan.getLevel(), defRespLevel)) {
                townPlan.setLevel(defRespLevel);
                townLevelChanged = true;
                changed = true;
            }
            changed = syncTownPlanWithCounty(townPlan, countyPlan, now) || changed;
            if (changed) {
                service.updateActiveTownRegionPlanOrThrow(townPlan, "更新乡镇区域防御响应失败");
                if (townPlan.getId() != null) {
                    changedTownDefIds.add(townPlan.getId());
                }
            }
        }
        updateCountyPlanByAlarm(countyPlan, dataAlarmVo, now);
        Integer aggregatedLevel = resolveCountyLevelFromActiveTowns(countyPlan.getId());
        boolean countyLevelChanged = !Objects.equals(countyPlan.getLevel(), aggregatedLevel);
        countyPlan.setLevel(aggregatedLevel);
        applyCountyRegionPlanName(countyPlan, aggregatedLevel, now);
        boolean streetsExpanded = splitStreets(countyPlan.getStreets()).stream().anyMatch(street -> !oldCountyStreets.contains(street));
        boolean scopeOrLevelChanged = townAssociationChanged || streetsExpanded || townLevelChanged || countyLevelChanged;
        if (scopeOrLevelChanged) {
            advanceCountyRoundNoIfNeeded(countyPlan, true, false);
            persistCountyAggregatedFieldsOrThrow(
                countyPlan, now, "ALARM_LINKAGE_NO_RESET", expectedRoundNo, expectedStatus);
            syncActiveTownNonLevelInheritedFieldsFromCounty(countyPlan.getId(), countyPlan, now);
        } else {
            service.updateActiveCountyRegionPlanOrThrow(countyPlan, "更新县级区域防御响应失败");
        }
        for (Long formerCountyId : formerCountyIds) {
            if (!Objects.equals(formerCountyId, countyPlan.getId())) {
                reconcileCountyRegionFromChildTowns(formerCountyId, now, true);
            }
        }
        if (!changedTownDefIds.isEmpty()) {
            dzTaskDistListService.refreshDefenseTaskSuggestionByDefIds(changedTownDefIds);
        }
        triggerAdjustedSmsForCapturedStates(beforeSmsStates, now);
        return countyPlan;
    }

    /**
     * 将预警映射为防御响应等级（取 level_streets_json 中最高预警等级）
     */
    Integer resolveDefenseResponseLevel(DataAlarmVo dataAlarmVo) {
        return resolveDefenseResponseLevel(dataAlarmVo, resolveAlarmHighestWarningLevel(dataAlarmVo));
    }

    /**
     * 将指定预警等级映射为防御响应等级
     */
    Integer resolveDefenseResponseLevel(DataAlarmVo dataAlarmVo, Integer warningLevel) {
        if (dataAlarmVo == null || warningLevel == null) {
            return null;
        }
        return DefRespAlarmLevelUtil.resolveDefenseResponseLevel(dataAlarmVo, warningLevel);
    }

    List<String> resolveAlarmStreets(DataAlarmVo dataAlarmVo) {
        if (dataAlarmVo == null) {
            return List.of();
        }
        return DataAlarmLevelStreetsUtil.flattenStreets(dataAlarmVo.getLevelStreetsJson());
    }

    Map<String, Integer> resolveAlarmStreetWarningLevelMap(DataAlarmVo dataAlarmVo) {
        if (dataAlarmVo == null) {
            return Map.of();
        }
        return DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(dataAlarmVo.getLevelStreetsJson());
    }

    Integer resolveAlarmHighestWarningLevel(DataAlarmVo dataAlarmVo) {
        return DataAlarmLevelStreetsUtil.resolveHighestLevel(
            dataAlarmVo == null ? null : dataAlarmVo.getLevelStreetsJson());
    }

    Integer resolveAlarmHighestDefenseLevel(DataAlarmVo dataAlarmVo) {
        Map<String, Integer> streetLevels = resolveAlarmStreetWarningLevelMap(dataAlarmVo);
        return streetLevels.values().stream()
                           .map(level -> resolveDefenseResponseLevel(dataAlarmVo, level))
                           .filter(Objects::nonNull)
                           .max(Integer::compareTo)
                           .orElse(null);
    }

    /**
     * 查找预警对应的有效县级方案
     */
    DefRespPlan findExistingCountyRegionPlanForAlarm(DataAlarmVo dataAlarmVo) {
        if (dataAlarmVo == null || StringUtils.isBlank(dataAlarmVo.getCity())) {
            return null;
        }
        return requireUniqueEffectiveCountyRegionPlan(dataAlarmVo.getCity());
    }

    /**
     * 根据乡镇子行反推并校正县级方案
     */
    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged) {
        reconcileCountyRegionFromChildTowns(countyDefId, now, relationChanged, true, false);
    }

    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged, boolean allowCountyReset) {
        reconcileCountyRegionFromChildTowns(countyDefId, now, relationChanged, allowCountyReset, false);
    }

    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged,
                                             boolean allowCountyReset, boolean roundAlreadyAdvanced) {
        if (countyDefId == null) {
            log.warn("reconcileCountyRegionFromChildTowns - countyDefId为空，跳过");
            return;
        }
        log.info("reconcileCountyRegionFromChildTowns - 开始 countyDefId={}, relationChanged={}, allowCountyReset={}, roundAlreadyAdvanced={}",
            countyDefId, relationChanged, allowCountyReset, roundAlreadyAdvanced);
        DefRespPlan county = getActiveCountyRegionPlanById(countyDefId);
        if (county == null) {
            logInactiveCountySkipReason(countyDefId);
            return;
        }
        Integer expectedRoundNo = county.getCurrentRoundNo();
        Integer expectedStatus = county.getStatus();
        List<DefRespPlan> activeTowns = listActiveTownPlansForCountyAggregation(countyDefId);
        Set<String> oldStreets = new LinkedHashSet<>(splitStreets(county.getStreets()));
        Set<String> newStreets = collectTownStreetsByCountyId(countyDefId);
        Integer oldLevel = county.getLevel();
        Integer aggregatedLevel = resolveCountyLevelFromActiveTowns(countyDefId);
        boolean levelChanged = !Objects.equals(oldLevel, aggregatedLevel);
        boolean streetsChanged = !Objects.equals(oldStreets, newStreets);
        log.info("reconcileCountyRegionFromChildTowns - 聚合计算 countyDefId={}, county={}, status={}, executeStatus={}, "
                + "roundNo={}, oldStreets={}, newStreets={}, oldLevel={}, newLevel={}, "
                + "streetsChanged={}, levelChanged={}, relationChanged={}, activeTownCount={}, activeTowns={}",
            countyDefId, county.getCounty(), county.getStatus(), county.getExecuteStatus(), county.getCurrentRoundNo(),
            oldStreets, newStreets, oldLevel, aggregatedLevel,
            streetsChanged, levelChanged, relationChanged, activeTowns.size(), describeTownPlansForAggregation(activeTowns));
        if (!streetsChanged && !relationChanged && !levelChanged) {
            log.info("reconcileCountyRegionFromChildTowns - 无变化且非关系变更，短路返回 countyDefId={}", countyDefId);
            return;
        }
        county.setStreets(String.join(",", newStreets));
        county.setLevel(aggregatedLevel);
        county.setUpdateDate(now);
        applyCountyRegionPlanName(county, aggregatedLevel, now);
        if (levelChanged) {
            county.setTriggerCondition(aggregatedLevel != null ? buildRegionTriggerConditionByLevel(aggregatedLevel) : null);
        }
        advanceCountyRoundNoIfNeeded(county, true, roundAlreadyAdvanced);
        if (Objects.equals(county.getStatus(), DefRespPlanStatusEnum.UNSTARTED.getCode())) {
            log.info("reconcileCountyRegionFromChildTowns - 分支=UNSTARTED仅落库 countyDefId={}", countyDefId);
            persistCountyAggregatedFieldsOrThrow(
                county, now, "UNSTARTED", expectedRoundNo, expectedStatus);
            syncActiveTownNonLevelInheritedFieldsFromCounty(countyDefId, county, now);
            return;
        }
        log.info("reconcileCountyRegionFromChildTowns - 分支=仅落库不回退 countyDefId={}, allowCountyReset={}",
            countyDefId, allowCountyReset);
        persistCountyAggregatedFieldsOrThrow(
            county, now, "NO_STATUS_RESET", expectedRoundNo, expectedStatus);
        syncActiveTownNonLevelInheritedFieldsFromCounty(countyDefId, county, now);
    }

    void logInactiveCountySkipReason(Long countyDefId) {
        DefRespPlan raw = baseMapper.selectById(countyDefId);
        if (raw == null) {
            log.warn("reconcileCountyRegionFromChildTowns - 县级记录不存在, countyDefId={}", countyDefId);
            return;
        }
        log.warn("reconcileCountyRegionFromChildTowns - 县级不可校正(executeStatus须为0/1且status!=6), "
                + "countyDefId={}, county={}, status={}, executeStatus={}, deleted={}, type={}, regionScopeType={}",
            countyDefId, raw.getCounty(), raw.getStatus(), raw.getExecuteStatus(), raw.getDeleted(),
            raw.getType(), raw.getRegionScopeType());
    }

    String describeTownPlansForAggregation(List<DefRespPlan> towns) {
        if (towns == null || towns.isEmpty()) {
            return "[]";
        }
        return towns.stream()
                    .map(town -> town.getId() + ":" + town.getStreets()
                        + "(level=" + town.getLevel()
                        + ",parent=" + town.getParentDefId()
                        + ",exec=" + town.getExecuteStatus() + ")")
                    .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 显式落库县级聚合字段（含 level/streets 置空），避免实体 update 跳过 null 字段。
     */
    void persistCountyAggregatedFieldsOrThrow(DefRespPlan county, Date now) {
        persistCountyAggregatedFieldsOrThrow(county, now, "DEFAULT");
    }

    void persistCountyAggregatedFieldsOrThrow(DefRespPlan county, Date now, String branch) {
        persistCountyAggregatedFieldsOrThrow(
            county, now, branch, county == null ? null : county.getCurrentRoundNo(),
            county == null ? null : county.getStatus());
    }

    void persistCountyAggregatedFieldsOrThrow(DefRespPlan county, Date now, String branch,
                                              Integer expectedRoundNo, Integer expectedStatus) {
        if (county == null || county.getId() == null) {
            throw new ServiceException("更新县级区域防御响应失败");
        }
        log.info("persistCountyAggregatedFields - branch={}, countyDefId={}, county={}, streets={}, level={}, roundNo={}, name={}",
            branch, county.getId(), county.getCounty(), county.getStreets(), county.getLevel(), county.getCurrentRoundNo(), county.getName());
        LambdaUpdateWrapper<DefRespPlan> wrapper = Wrappers.<DefRespPlan>lambdaUpdate()
                                                           .eq(DefRespPlan::getId, county.getId())
                                                           .in(DefRespPlan::getExecuteStatus,
                                                               DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                                                               DefRespExecuteStatusEnum.RUNNING.getCode())
                                                           .set(DefRespPlan::getStreets, county.getStreets())
                                                           .set(DefRespPlan::getLevel, county.getLevel())
                                                           .set(DefRespPlan::getName, county.getName())
                                                           .set(DefRespPlan::getTriggerCondition, county.getTriggerCondition())
                                                           .set(DefRespPlan::getSourceAlarmId, county.getSourceAlarmId())
                                                           .set(DefRespPlan::getCurrentRoundNo, county.getCurrentRoundNo())
                                                           .set(DefRespPlan::getUpdateDate, now);
        if (expectedRoundNo == null) {
            wrapper.isNull(DefRespPlan::getCurrentRoundNo);
        } else {
            wrapper.eq(DefRespPlan::getCurrentRoundNo, expectedRoundNo);
        }
        if (expectedStatus == null) {
            wrapper.isNull(DefRespPlan::getStatus);
        } else {
            wrapper.eq(DefRespPlan::getStatus, expectedStatus);
        }
        int updated = baseMapper.update(null, wrapper);
        if (updated <= 0) {
            log.error("persistCountyAggregatedFields - 更新失败 branch={}, countyDefId={}, executeStatus须为0/1",
                branch, county.getId());
            throw new ServiceException("更新县级区域防御响应失败");
        }
        log.info("persistCountyAggregatedFields - 更新成功 branch={}, countyDefId={}, affectedRows={}",
            branch, county.getId(), updated);
    }

    /**
     * 解析乡镇关联的县级 defId（关闭/换绑后标记待校正县级）。
     */
    Long resolveCountyDefIdForTownReconcile(DefRespPlan townPlan) {
        if (townPlan == null) {
            return null;
        }
        if (townPlan.getParentDefId() != null) {
            return townPlan.getParentDefId();
        }
        if (StringUtils.isBlank(townPlan.getCounty())) {
            log.warn("resolveCountyDefIdForTownReconcile - 乡镇缺少parentDefId与county, townId={}, street={}",
                townPlan.getId(), townPlan.getStreets());
            return null;
        }
        DefRespPlan countyPlan = requireUniqueEffectiveCountyRegionPlan(townPlan.getCounty());
        Long resolvedCountyDefId = countyPlan != null ? countyPlan.getId() : null;
        log.info("resolveCountyDefIdForTownReconcile - parentDefId为空，按县名回退 townId={}, street={}, county={}, resolvedCountyDefId={}",
            townPlan.getId(), townPlan.getStreets(), townPlan.getCounty(), resolvedCountyDefId);
        return resolvedCountyDefId;
    }

    /**
     * 汇总县级下参与聚合的有效乡镇行：同县名下活跃乡镇，且 parent 为空或指向本县。
     */
    List<DefRespPlan> listActiveTownPlansForCountyAggregation(Long countyDefId) {
        if (countyDefId == null) {
            return List.of();
        }
        DefRespPlan county = baseMapper.selectById(countyDefId);
        if (county == null || StringUtils.isBlank(county.getCounty())) {
            return listTownPlansByCountyId(countyDefId).stream()
                                                       .filter(this::isTownRowActive)
                                                       .toList();
        }
        return service.listEffectiveRegionPlans(county.getCounty(), RegionScopeTypeEnum.TOWN.getCode(), null).stream()
                                                                                                     .filter(this::isTownRowActive)
                                                                                                     .filter(town -> Objects.equals(town.getParentDefId(), countyDefId)
                                                                                                         || town.getParentDefId() == null)
                                                                                                     .toList();
    }

    /**
     * 汇总县级下有效乡镇街道集合
     */
    Set<String> collectTownStreetsByCountyId(Long countyDefId) {
        return listActiveTownPlansForCountyAggregation(countyDefId).stream()
                                                                   .map(DefRespPlan::getStreets)
                                                                   .filter(StringUtils::isNotBlank)
                                                                   .map(String::trim)
                                                                   .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 从有效乡镇行聚合县级等级：取子乡镇等级中的最小值（如同时有 1、2、3 级，取 1 级）。
     */
    Integer resolveCountyLevelFromActiveTowns(Long countyDefId) {
        return listActiveTownPlansForCountyAggregation(countyDefId).stream()
                                                                   .map(DefRespPlan::getLevel)
                                                                   .filter(Objects::nonNull)
                                                                   .min(Integer::compareTo)
                                                                   .orElse(null);
    }

    /**
     * 县级聚合字段调整时增加轮次，但不改状态。
     */
    void incrementCountyRoundNo(DefRespPlan county) {
        if (county == null) {
            return;
        }
        county.setCurrentRoundNo(nextRoundNo(county.getCurrentRoundNo()));
    }

    /**
     * 已启动的县级区域响应在范围或乡镇等级真实变化时才进入下一轮。
     */
    boolean advanceCountyRoundNoIfNeeded(DefRespPlan county, boolean scopeOrLevelChanged,
                                         boolean roundAlreadyAdvanced) {
        if (county == null || !scopeOrLevelChanged || roundAlreadyAdvanced
            || county.getStatus() == null
            || county.getStatus() <= DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode()) {
            return false;
        }
        incrementCountyRoundNo(county);
        return true;
    }

    /**
     * 计算下一轮次号
     */
    int nextRoundNo(Integer currentRoundNo) {
        return currentRoundNo == null ? 2 : currentRoundNo + 1;
    }

    /**
     * 按预警更新县级方案字段
     */
    void updateCountyPlanByAlarm(DefRespPlan countyPlan, DataAlarmVo dataAlarmVo, Date now) {
        Set<String> streetSet = new LinkedHashSet<>(collectTownStreetsByCountyId(countyPlan.getId()));
        streetSet.addAll(resolveAlarmStreets(dataAlarmVo));
        countyPlan.setStreets(String.join(",", streetSet));
        countyPlan.setTriggerCondition(buildRegionTriggerCondition(dataAlarmVo));
        countyPlan.setSourceAlarmId(dataAlarmVo.getId());
        countyPlan.setUpdateDate(now);
    }

    /**
     * 构建县级区域防御响应名称，如：恩施市Ⅱ级区域防御响应2026-06-01
     */
    String buildCountyRegionPlanName(String county, Integer level, Date referenceDate) {
        if (StringUtils.isBlank(county) || level == null) {
            return null;
        }
        Date date = referenceDate != null ? referenceDate : new Date();
        return county + LevelCodeUtil.resolveDefenseResponseRomanLevel(level) + "区域防御响应"
            + new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    void applyCountyRegionPlanName(DefRespPlan county, Integer level, Date now) {
        if (county == null) {
            return;
        }
        String name = buildCountyRegionPlanName(county.getCounty(), level, now);
        if (StringUtils.isNotBlank(name)) {
            county.setName(name);
        }
    }

    /**
     * 构建区域方案触发条件文案
     */
    String buildRegionTriggerCondition(DataAlarmVo dataAlarmVo) {
        return buildRegionTriggerCondition(
            dataAlarmVo == null ? null : dataAlarmVo.getSource(),
            resolveAlarmHighestDefenseLevel(dataAlarmVo));
    }

    /**
     * 按防御响应等级构建区域方案触发条件文案
     */
    String buildRegionTriggerConditionByLevel(Integer level) {
        return buildRegionTriggerCondition(null, level);
    }

    String buildRegionTriggerCondition(String source, Integer level) {
        if (level == null) {
            return null;
        }
        return "湖北省" + StringUtils.defaultIfBlank(source, "预警信息") + "的"
            + LevelCodeUtil.resolveDefenseResponseRomanLevel(level) + "预警";
    }

    /**
     * 同步县级非等级继承字段到有效乡镇行（不覆盖乡镇独立等级）
     */
    void syncActiveTownNonLevelInheritedFieldsFromCounty(Long countyDefId, DefRespPlan county, Date now) {
        if (countyDefId == null || county == null) {
            return;
        }
        List<DefRespPlan> changedTowns = new ArrayList<>();
        for (DefRespPlan town : listTownPlansByCountyId(countyDefId)) {
            if (!isTownRowActive(town)) {
                continue;
            }
            if (syncTownPlanWithCounty(town, county, now)) {
                changedTowns.add(town);
            }
        }
        if (!changedTowns.isEmpty()) {
            service.updateActiveTownRegionPlansOrThrow(changedTowns, "更新乡镇区域防御响应失败");
        }
    }

    /**
     * 查找县内指定街道的有效乡镇方案
     */
    DefRespPlan findEffectiveTownRegionPlanByCountyAndStreet(String county, String street) {
        if (StringUtils.isBlank(county) || StringUtils.isBlank(street)) {
            return null;
        }
        String targetStreet = street.trim();
        return service.listEffectiveRegionPlans(county, RegionScopeTypeEnum.TOWN.getCode(), null).stream()
                                                                                         .filter(plan -> targetStreet.equals(StringUtils.trim(plan.getStreets())))
                                                                                         .findFirst()
                                                                                         .orElse(null);
    }

    /**
     * 同步乡镇方案与县级继承字段
     */
    boolean syncTownPlanWithCounty(DefRespPlan townPlan, DefRespPlan countyPlan, Date now) {
        boolean changed = false;
        if (!Objects.equals(townPlan.getTriggerCondition(), countyPlan.getTriggerCondition())) {
            townPlan.setTriggerCondition(countyPlan.getTriggerCondition());
            changed = true;
        }
        if (!Objects.equals(townPlan.getResponsibilityUnit(), countyPlan.getResponsibilityUnit())) {
            townPlan.setResponsibilityUnit(countyPlan.getResponsibilityUnit());
            changed = true;
        }
        if (!Objects.equals(townPlan.getResponsiblePerson(), countyPlan.getResponsiblePerson())) {
            townPlan.setResponsiblePerson(countyPlan.getResponsiblePerson());
            changed = true;
        }
        if (!Objects.equals(townPlan.getResponsiblePersonPhone(), countyPlan.getResponsiblePersonPhone())) {
            townPlan.setResponsiblePersonPhone(countyPlan.getResponsiblePersonPhone());
            changed = true;
        }
        if (!Objects.equals(townPlan.getCurrentRoundNo(), countyPlan.getCurrentRoundNo())) {
            townPlan.setCurrentRoundNo(countyPlan.getCurrentRoundNo());
            changed = true;
        }
        if (!Objects.equals(townPlan.getStatus(), countyPlan.getStatus())) {
            townPlan.setStatus(countyPlan.getStatus());
            changed = true;
        }
        if (changed) {
            townPlan.setUpdateDate(now);
        }
        return changed;
    }

    /**
     * 批量同步有效乡镇继承字段
     */
    void syncActiveTownInheritedFields(Long countyDefId, Integer level, String triggerCondition, Date now) {
        if (countyDefId == null) {
            return;
        }
        List<DefRespPlan> changedTowns = new ArrayList<>();
        for (DefRespPlan town : listTownPlansByCountyId(countyDefId)) {
            if (!isTownRowActive(town)) {
                continue;
            }
            boolean changed = false;
            if (!Objects.equals(town.getLevel(), level)) {
                town.setLevel(level);
                changed = true;
            }
            if (!Objects.equals(town.getTriggerCondition(), triggerCondition)) {
                town.setTriggerCondition(triggerCondition);
                changed = true;
            }
            if (changed) {
                town.setUpdateDate(now);
                changedTowns.add(town);
            }
        }
        if (!changedTowns.isEmpty()) {
            service.updateActiveTownRegionPlansOrThrow(changedTowns, "更新乡镇区域防御响应失败");
        }
    }

    /**
     * 校验县级等级不可变
     */
    void validateCountyRegionLevelImmutable(DefRespPlan countyPlan, Integer targetLevel) {
        if (!service.isCountyRegionPlan(countyPlan) || targetLevel == null) {
            return;
        }
        Integer currentLevel = countyPlan.getLevel();
        if (currentLevel != null && !Objects.equals(currentLevel, targetLevel)) {
            throw new ServiceException("县级区域防御响应等级创建后不可修改，请改为关联或创建对应等级的县级响应");
        }
    }
}
