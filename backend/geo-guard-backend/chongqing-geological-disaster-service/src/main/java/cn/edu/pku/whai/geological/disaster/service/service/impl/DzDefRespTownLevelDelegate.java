/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataAlarmMapper;
import cn.edu.pku.whai.geological.disaster.service.consts.RegionDefRespCloseReason;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespConsultationConfirmStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBatchRelateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.BatchUpdateTownLevelsExecutionResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespTownLevelDelegate {

    private static final int ALARM_STATUS_SUPERSEDED = DzDefRespPlanServiceImpl.ALARM_STATUS_SUPERSEDED;
    private static final String ENSHI_CITY = DzDefRespPlanServiceImpl.ENSHI_CITY;

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final DataAlarmMapper dataAlarmMapper;
    private final IDzTaskDistListService dzTaskDistListService;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;

    DzDefRespTownLevelDelegate(DzDefRespPlanServiceImpl service,
                               DataAlarmMapper dataAlarmMapper,
                               IDzTaskDistListService dzTaskDistListService,
                               IDzTaskHandleDetailContentService dzTaskHandleDetailContentService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dataAlarmMapper = dataAlarmMapper;
        this.dzTaskDistListService = dzTaskDistListService;
        this.dzTaskHandleDetailContentService = dzTaskHandleDetailContentService;
    }

    private DefRespPlan chooseLatestTownPlan(DefRespPlan left, DefRespPlan right) { return service.chooseLatestTownPlan(left, right); }

    private boolean isTownRowActive(DefRespPlan town) { return service.isTownRowActive(town); }

    private boolean isEffectiveRegionPlan(DefRespPlan plan) { return service.isEffectiveRegionPlan(plan); }

    private DefRespPlan getActiveCountyRegionPlanById(Long countyDefId) { return service.getActiveCountyRegionPlanById(countyDefId); }

    private List<DefRespPlanBatchRelateBo> buildBatchRelateBoList(String planContentJson) { return service.buildBatchRelateBoList(planContentJson); }

    private void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged, boolean allowCountyReset) { service.reconcileCountyRegionFromChildTowns(countyDefId, now, relationChanged, allowCountyReset); }

    private Long resolveCountyDefIdForTownReconcile(DefRespPlan townPlan) { return service.resolveCountyDefIdForTownReconcile(townPlan); }

    private void closeTownPlan(DefRespPlan townPlan, String reason, Date now) { service.closeTownPlan(townPlan, reason, now); }

    private List<String> splitStreets(String streets) { return DzDefRespPlanServiceImpl.splitStreets(streets); }

    private List<AdRegionVo> listTownAdRegionsByCounty(String county) { return service.listTownAdRegionsByCounty(county); }

    private List<AdRegionVo> queryTownAdRegions(List<String> streetNames) { return service.queryTownAdRegions(streetNames); }

    private String resolveAdRegionStreetName(AdRegionVo adRegionVo) { return service.resolveAdRegionStreetName(adRegionVo); }

    private String resolveAdRegionCountyName(AdRegionVo adRegionVo) { return service.resolveAdRegionCountyName(adRegionVo); }

    private DefRespPlan requireUniqueEffectiveCountyRegionPlan(String county) { return service.requireUniqueEffectiveCountyRegionPlan(county); }

    private DefRespPlan createCountyRegionPlanForTownLevelUpdate(String county, Integer level, List<String> targetStreets, Date now, Long alarmId) { return service.createCountyRegionPlanForTownLevelUpdate(county, level, targetStreets, now, alarmId); }

    private Map<String, List<DefRespPlan>> buildEffectiveTownPlansByStreet(String county) { return service.buildEffectiveTownPlansByStreet(county); }

    private DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, String oneStreet, Date now) { return service.insertTownPlanUnderCounty(county, oneStreet, now); }

    private boolean syncTownPlanInheritedFields(DefRespPlan townPlan, DefRespPlan targetCounty, Date now) { return service.syncTownPlanInheritedFields(townPlan, targetCounty, now); }

    private void updateActiveTownRegionPlansOrThrow(List<DefRespPlan> towns, String errorMessage) { service.updateActiveTownRegionPlansOrThrow(towns, errorMessage); }

    private void updateActiveCountyRegionPlanOrThrow(DefRespPlan county, String errorMessage) { service.updateActiveCountyRegionPlanOrThrow(county, errorMessage); }

    private Map<Long, DzDefRespSmsEventFactory.CountySmsState> captureStartedCountySmsStates() {
        return service.captureStartedCountySmsStates();
    }

    private void triggerAdjustedSmsForCapturedStates(
        Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeStates, Date actionTime) {
        service.triggerAdjustedSmsForCapturedStates(beforeStates, actionTime);
    }
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateTownRegionLevels(List<DefRespPlanBatchRelateBo> boList) {
        return batchUpdateTownRegionLevelsInternal(boList, true);
    }

    public int matchCirculatingTownLevels(List<DefRespPlanBatchRelateBo> boList) {
        Map<String, Integer> currentLevels = listCirculatingTownLevelByStreet();
        Map<String, Integer> expectedLevels = buildExpectedTownLevels(boList);
        return Objects.equals(currentLevels, expectedLevels) ? 1 : 0;
    }

    /**
     * 查询当前流转中乡镇级防御响应等级。
     */
    Map<String, Integer> listCirculatingTownLevelByStreet() {
        List<DefRespPlan> townPlans = baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes())
        );
        if (townPlans == null || townPlans.isEmpty()) {
            return Map.of();
        }
        Map<String, DefRespPlan> latestPlanByStreet = new LinkedHashMap<>();
        for (DefRespPlan plan : townPlans) {
            if (!isTownRowActive(plan) || StringUtils.isBlank(plan.getStreets())) {
                continue;
            }
            String street = plan.getStreets().trim();
            latestPlanByStreet.merge(street, plan, this::chooseLatestTownPlan);
        }
        Map<String, Integer> levelByStreet = new LinkedHashMap<>();
        latestPlanByStreet.forEach((street, plan) -> levelByStreet.put(street, plan.getLevel()));
        return levelByStreet;
    }

    /**
     * 将入参转换为乡镇-等级映射。
     */
    Map<String, Integer> buildExpectedTownLevels(List<DefRespPlanBatchRelateBo> boList) {
        if (boList == null || boList.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> expectedLevels = new LinkedHashMap<>();
        for (DefRespPlanBatchRelateBo bo : boList) {
            if (bo == null || bo.getStreets() == null || bo.getNewLevel() == null) {
                continue;
            }
            for (String street : bo.getStreets()) {
                if (StringUtils.isBlank(street)) {
                    continue;
                }
                expectedLevels.put(street.trim(), bo.getNewLevel());
            }
        }
        return expectedLevels;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveConsultationConfirm(DzTaskHandleDetailContentBo bo) {
        Long detailId = dzTaskHandleDetailContentService.saveConsultationConfirmByBo(bo);
        if (bo != null
            && DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bo.getBizType())
            && DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode().equals(bo.getContentType())
            && DefRespConsultationConfirmStatusEnum.PENDING_APPROVAL.getCode().equals(bo.getStatus())) {
            ensureCountyDefRespOnConsultationSubmit(bo.getBizId(), bo.getPlanContentJson());
        }
        return detailId;
    }

    void ensureCountyDefRespOnConsultationSubmit(Long defId, String planContentJson) {
        if (StringUtils.isBlank(planContentJson)) {
            throw new ServiceException("planContentJson不能为空");
        }
        if (existsEffectiveCountyDefResp(defId)) {
            return;
        }
        batchUpdateTownRegionLevelsInternal(buildBatchRelateBoList(planContentJson), true);
    }

    boolean existsEffectiveCountyDefResp(Long defId) {
        return getActiveCountyRegionPlanById(defId) != null;
    }

    int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset) {
        return batchUpdateTownRegionLevelsInternal(boList, allowCountyReset, false);
    }

    int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset,
                                            boolean roundAlreadyAdvanced) {
        if (boList == null || boList.isEmpty()) {
            log.error("batchUpdateTownRegionLevels接口 - 入参不能为空");
            throw new ServiceException("入参不能为空");
        }
        Date now = new Date();
        Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeSmsStates = captureStartedCountySmsStates();
        Map<Long, Boolean> countiesToReconcile = new LinkedHashMap<>();
        Set<Long> affectedTownDefIds = new LinkedHashSet<>();
        Set<String> allRequestedStreets = buildAllRequestedStreets(boList);
        BatchUpdateTownLevelsExecutionResult closeResult = closeTownRegionLevelsNotInRequest(allRequestedStreets, now);
        mergeCountiesToReconcile(countiesToReconcile, closeResult.getCountiesToReconcile());
        for (DefRespPlanBatchRelateBo bo : boList) {
            BatchUpdateTownLevelsExecutionResult result = executeBatchUpdateTownRegionLevels(bo, now);
            mergeCountiesToReconcile(countiesToReconcile, result.getCountiesToReconcile());
            affectedTownDefIds.addAll(result.getAffectedTownDefIds());
        }
        for (Long countyDefId : collectCountyDefIdsFromInput(boList)) {
            markCountyToReconcile(countiesToReconcile, countyDefId, false);
        }
        log.info("batchUpdateTownRegionLevelsInternal - 待校正县级 allowCountyReset={}, roundAlreadyAdvanced={}, retentionStreets={}, countiesToReconcile={}",
            allowCountyReset, roundAlreadyAdvanced, allRequestedStreets, countiesToReconcile);
        for (Map.Entry<Long, Boolean> entry : countiesToReconcile.entrySet()) {
            service.reconcileCountyRegionFromChildTowns(entry.getKey(), now, entry.getValue(),
                allowCountyReset, roundAlreadyAdvanced);
        }
        int refreshedTaskCount = dzTaskDistListService.refreshDefenseTaskSuggestionByDefIds(affectedTownDefIds);
        triggerAdjustedSmsForCapturedStates(beforeSmsStates, now);
        return refreshedTaskCount;
    }


    /**
     * 执行单批乡镇等级更新
     */
    BatchUpdateTownLevelsExecutionResult executeBatchUpdateTownRegionLevels(DefRespPlanBatchRelateBo bo, Date now) {
        if (bo == null || bo.getStreets() == null || bo.getStreets().isEmpty()) {
            throw new ServiceException("streets不能为空");
        }
        if (bo.getNewLevel() == null) {
            throw new ServiceException("newLevel不能为空");
        }
        List<String> requestedStreets = normalizeRequestedTownLevelUpdateStreets(bo.getStreets());
        if (requestedStreets.isEmpty()) {
            throw new ServiceException("streets不能为空");
        }
        if (requestedStreets.contains(ENSHI_CITY) && requestedStreets.size() > 1) {
            throw new ServiceException("乡镇区域不可与恩施市同时存在");
        }
        List<AdRegionVo> targetStreetRegions = resolveTownLevelUpdateStreetRegions(requestedStreets);
        String county = extractCountyForTownLevelUpdate(targetStreetRegions);
        List<String> targetStreets = targetStreetRegions.stream()
                                                        .map(this::resolveAdRegionStreetName)
                                                        .filter(StringUtils::isNotBlank)
                                                        .map(String::trim)
                                                        .distinct()
                                                        .toList();
        if (targetStreets.isEmpty()) {
            throw new ServiceException("未解析到有效的乡镇区域");
        }

        DefRespPlan targetCounty = requireTargetCountyRegionPlan(county, bo.getNewLevel(), targetStreets, now, bo.getAlarmId());
        Map<String, List<DefRespPlan>> effectiveTownPlansByStreet = buildEffectiveTownPlansByStreet(county);
        List<DefRespPlan> townPlansToUpdate = new ArrayList<>();
        Set<Long> affectedTownDefIds = new LinkedHashSet<>();
        Map<Long, Boolean> countiesToReconcile = new LinkedHashMap<>();
        boolean targetCountyRelationChanged = false;
        boolean townLevelChanged = false;
        for (String street : targetStreets) {
            List<DefRespPlan> townPlans = effectiveTownPlansByStreet.getOrDefault(street, List.of());
            if (townPlans.isEmpty()) {
                DefRespPlan insertedTownPlan = insertTownPlanUnderCounty(targetCounty, street, now);
                if (insertedTownPlan == null || insertedTownPlan.getId() == null) {
                    throw new ServiceException("新增乡镇级区域防御响应失败");
                }
                insertedTownPlan.setLevel(bo.getNewLevel());
                insertedTownPlan.setUpdateDate(now);
                baseMapper.updateById(insertedTownPlan);
                affectedTownDefIds.add(insertedTownPlan.getId());
                targetCountyRelationChanged = true;
                townLevelChanged = true;
                continue;
            }
            for (DefRespPlan townPlan : townPlans) {
                boolean changed = false;
                if (!Objects.equals(townPlan.getParentDefId(), targetCounty.getId())) {
                    markCountyToReconcile(countiesToReconcile, townPlan.getParentDefId(), true);
                    targetCountyRelationChanged = true;
                    townPlan.setParentDefId(targetCounty.getId());
                    changed = true;
                }
                if (!Objects.equals(townPlan.getLevel(), bo.getNewLevel())) {
                    townPlan.setLevel(bo.getNewLevel());
                    townLevelChanged = true;
                    changed = true;
                }
                changed = syncTownPlanInheritedFields(townPlan, targetCounty, now) || changed;
                if (changed) {
                    affectedTownDefIds.add(townPlan.getId());
                    townPlansToUpdate.add(townPlan);
                }
            }
        }
        if (!townPlansToUpdate.isEmpty()) {
            updateActiveTownRegionPlansOrThrow(townPlansToUpdate, "批量更新乡镇级区域防御响应失败");
        }
        markCountyToReconcile(countiesToReconcile, targetCounty.getId(), targetCountyRelationChanged || townLevelChanged);
        return new BatchUpdateTownLevelsExecutionResult(affectedTownDefIds, countiesToReconcile);
    }

    /**
     * 汇总入参涉及的有效县级方案 id
     */
    Set<Long> collectCountyDefIdsFromInput(List<DefRespPlanBatchRelateBo> boList) {
        Set<Long> countyDefIds = new LinkedHashSet<>();
        if (boList == null || boList.isEmpty()) {
            return countyDefIds;
        }
        for (DefRespPlanBatchRelateBo bo : boList) {
            if (bo == null || bo.getStreets() == null || bo.getStreets().isEmpty()) {
                continue;
            }
            List<String> requestedStreets = normalizeRequestedTownLevelUpdateStreets(bo.getStreets());
            if (requestedStreets.isEmpty()) {
                continue;
            }
            List<AdRegionVo> targetStreetRegions = resolveTownLevelUpdateStreetRegions(requestedStreets);
            String county = extractCountyForTownLevelUpdate(targetStreetRegions);
            DefRespPlan countyPlan = requireUniqueEffectiveCountyRegionPlan(county);
            if (countyPlan != null && countyPlan.getId() != null) {
                countyDefIds.add(countyPlan.getId());
            }
        }
        return countyDefIds;
    }

    /**
     * 汇总入参涉及的全部街道（覆盖处理基准集）
     */
    Set<String> buildAllRequestedStreets(List<DefRespPlanBatchRelateBo> boList) {
        Set<String> allRequestedStreets = new LinkedHashSet<>();
        for (DefRespPlanBatchRelateBo bo : boList) {
            List<String> requestedStreets = normalizeRequestedTownLevelUpdateStreets(bo.getStreets());
            if (requestedStreets.isEmpty()) {
                continue;
            }
            List<AdRegionVo> targetStreetRegions = resolveTownLevelUpdateStreetRegions(requestedStreets);
            targetStreetRegions.stream()
                               .map(this::resolveAdRegionStreetName)
                               .filter(StringUtils::isNotBlank)
                               .map(String::trim)
                               .forEach(allRequestedStreets::add);
        }
        return allRequestedStreets;
    }

    /**
     * 关闭不在入参中的有效乡镇级防御响应
     */
    BatchUpdateTownLevelsExecutionResult closeTownRegionLevelsNotInRequest(Set<String> requestedStreets, Date now) {
        if (requestedStreets == null) {
            requestedStreets = Set.of();
        }
        List<DefRespPlan> townPlans = baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
        );
        Map<Long, Boolean> countiesToReconcile = new LinkedHashMap<>();
        if (townPlans == null || townPlans.isEmpty()) {
            return new BatchUpdateTownLevelsExecutionResult(Set.of(), countiesToReconcile);
        }
        for (DefRespPlan townPlan : townPlans) {
            if (!isEffectiveRegionPlan(townPlan) || !isTownRowActive(townPlan) || StringUtils.isBlank(townPlan.getStreets())) {
                continue;
            }
            if (requestedStreets.contains(townPlan.getStreets().trim())) {
                continue;
            }
            Long resolvedCountyDefId = resolveCountyDefIdForTownReconcile(townPlan);
            log.info("closeTownRegionLevelsNotInRequest - 关闭乡镇 townId={}, street={}, parentDefId={}, county={}, resolvedCountyDefId={}",
                townPlan.getId(), townPlan.getStreets(), townPlan.getParentDefId(), townPlan.getCounty(), resolvedCountyDefId);
            closeTownPlan(townPlan, RegionDefRespCloseReason.TOWN_RANGE_SHRINK, now);
            markCountyToReconcile(countiesToReconcile, resolvedCountyDefId, true);
        }
        if (!countiesToReconcile.isEmpty()) {
            log.info("closeTownRegionLevelsNotInRequest - 关闭后待校正县级 countiesToReconcile={}", countiesToReconcile);
        }
        return new BatchUpdateTownLevelsExecutionResult(Set.of(), countiesToReconcile);
    }

    /**
     * 合并待校正的县级方案集合
     */
    void mergeCountiesToReconcile(Map<Long, Boolean> target, Map<Long, Boolean> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((countyDefId, relationChanged) -> markCountyToReconcile(target, countyDefId, relationChanged));
    }

    /**
     * 标记县级方案待校正
     */
    void markCountyToReconcile(Map<Long, Boolean> countiesToReconcile, Long countyDefId, boolean relationChanged) {
        if (countiesToReconcile == null || countyDefId == null) {
            if (countyDefId == null) {
                log.warn("markCountyToReconcile - countyDefId为空，无法标记待校正县级, relationChanged={}", relationChanged);
            }
            return;
        }
        countiesToReconcile.merge(countyDefId, relationChanged, Boolean::logicalOr);
        log.debug("markCountyToReconcile - countyDefId={}, relationChanged={}, merged={}",
            countyDefId, relationChanged, countiesToReconcile.get(countyDefId));
    }

    /**
     * 规范化批量更新请求的街道
     */
    List<String> normalizeRequestedTownLevelUpdateStreets(List<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String street : streets) {
            normalized.addAll(splitStreets(street));
        }
        return List.copyOf(normalized);
    }

    /**
     * 解析批量更新涉及的乡镇区划
     */
    List<AdRegionVo> resolveTownLevelUpdateStreetRegions(List<String> requestedStreets) {
        if (requestedStreets == null || requestedStreets.isEmpty()) {
            return List.of();
        }
        List<AdRegionVo> townRegions;
        if (requestedStreets.size() == 1 && Objects.equals(requestedStreets.getFirst(), ENSHI_CITY)) {
            townRegions = listTownAdRegionsByCounty(ENSHI_CITY);
            if (townRegions.isEmpty()) {
                throw new ServiceException("未找到恩施市下的乡镇行政区划");
            }
            return townRegions;
        }
        townRegions = queryTownAdRegions(requestedStreets);
        Map<String, AdRegionVo> regionMap = townRegions.stream()
                                                       .collect(Collectors.toMap(region -> resolveAdRegionStreetName(region).trim(),
                                                           region -> region,
                                                           (left, right) -> left,
                                                           LinkedHashMap::new));
        List<String> missingStreets = requestedStreets.stream()
                                                      .filter(street -> !regionMap.containsKey(street))
                                                      .toList();
        if (!missingStreets.isEmpty()) {
            throw new ServiceException("未找到乡镇行政区划: " + String.join("，", missingStreets));
        }
        return requestedStreets.stream()
                               .map(regionMap::get)
                               .filter(Objects::nonNull)
                               .distinct()
                               .toList();
    }

    /**
     * 从乡镇区划提取所属县
     */
    String extractCountyForTownLevelUpdate(List<AdRegionVo> townRegions) {
        String county = null;
        for (AdRegionVo townRegion : townRegions) {
            String currentCounty = resolveAdRegionCountyName(townRegion);
            if (StringUtils.isBlank(currentCounty)) {
                throw new ServiceException("乡镇行政区划缺少所属县信息");
            }
            if (StringUtils.isBlank(county)) {
                county = currentCounty;
            } else if (!Objects.equals(county, currentCounty)) {
                throw new ServiceException("传入的乡镇区域必须属于同一个县");
            }
        }
        if (StringUtils.isBlank(county)) {
            throw new ServiceException("未解析到所属县");
        }
        return county;
    }

    /**
     * 获取或创建目标县级区域方案
     */
    DefRespPlan requireTargetCountyRegionPlan(String county, Integer newLevel, List<String> targetStreets, Date now, Long alarmId) {
        DefRespPlan countyPlan = requireUniqueEffectiveCountyRegionPlan(county);
        if (countyPlan != null) {
            applyCountySourceAlarmIdIfPresent(countyPlan, alarmId, now);
            return countyPlan;
        }
        return createCountyRegionPlanForTownLevelUpdate(county, newLevel, targetStreets, now, alarmId);
    }

    /**
     * 会商 planContentJson 携带 alarmId 时，比对并切换县级 source_alarm_id。
     * 与旧值一致则跳过；不一致则将旧预警 status 置为 3（已替换），再写入新 alarmId。
     */
    void applyCountySourceAlarmIdIfPresent(DefRespPlan county, Long alarmId, Date now) {
        if (county == null || county.getId() == null || alarmId == null) {
            return;
        }
        Long oldAlarmId = county.getSourceAlarmId();
        if (Objects.equals(oldAlarmId, alarmId)) {
            return;
        }
        if (oldAlarmId != null) {
            markAlarmSuperseded(oldAlarmId, now);
        }
        DefRespPlan update = new DefRespPlan();
        update.setId(county.getId());
        update.setSourceAlarmId(alarmId);
        update.setUpdateDate(now);
        updateActiveCountyRegionPlanOrThrow(update, "更新县级区域防御响应预警来源失败");
        county.setSourceAlarmId(alarmId);
        log.info("applyCountySourceAlarmId - 县级预警来源切换 countyDefId={}, oldAlarmId={} -> newAlarmId={}",
            county.getId(), oldAlarmId, alarmId);
    }

    void markAlarmSuperseded(Long alarmId, Date now) {
        if (alarmId == null) {
            return;
        }
        LambdaUpdateWrapper<DataAlarm> wrapper = Wrappers.<DataAlarm>lambdaUpdate()
                                                         .eq(DataAlarm::getId, alarmId)
                                                         .set(DataAlarm::getStatus, ALARM_STATUS_SUPERSEDED)
                                                         .set(DataAlarm::getUpdateDate, now);
        int updated = dataAlarmMapper.update(null, wrapper);
        if (updated <= 0) {
            log.warn("markAlarmSuperseded - 旧预警状态更新失败或记录不存在, alarmId={}", alarmId);
            return;
        }
        log.info("markAlarmSuperseded - 旧预警已标记为已替换 status=3, alarmId={}", alarmId);
    }

}
