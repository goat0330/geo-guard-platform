/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSchemeItemVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.MonitorFrequencyParams;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.PlanUnitTaskContext;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.RiskUnitOnDate;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

class DzBatchGenerateDelegate {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DzBatchGenerateDelegate.class);
    private static final String INSPECTING_REQUIRE_STANDARD = DzTaskDistListServiceImpl.INSPECTING_REQUIRE_STANDARD;
    private static final String MONITORING_KEYWORD_MASS_PREVENTION = "群测群防";
    private static final String MONITORING_KEYWORD_INSTRUMENT = "仪器监测";

    private final DzTaskDistListServiceImpl service;
    private final DzTaskDistListMapper baseMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final ISlopeUnitService slopeUnitService;
    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final ITaskSmsContentService taskSmsContentService;

    DzBatchGenerateDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzTaskHandleMapper = service.dzTaskHandleMapper;
        this.slopeUnitService = service.slopeUnitService;
        this.dzTaskHandleSceneRecordService = service.dzTaskHandleSceneRecordService;
        this.dzTaskHandleDetailContentService = service.dzTaskHandleDetailContentService;
        this.dzDefRespPlanMapper = service.dzDefRespPlanMapper;
        this.taskSmsContentService = service.taskSmsContentService;
    }

    public Boolean batchGenerateByHandleId(Long handleId) {
        return batchGenerateByHandleId(handleId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean batchGenerateByHandleId(Long handleId, Integer planType) {
        DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
        if (Objects.isNull(dzTaskHandle)) {
            log.error("处置记录不存在, handleId: {}", handleId);
            return false;
        }
        if (StringUtils.isBlank(dzTaskHandle.getSlopeUnitId())) {
            log.error("单点防御响应处置记录缺少斜坡单元, handleId: {}", handleId);
            return false;
        }
        String slopeUnitId = dzTaskHandle.getSlopeUnitId().trim();
        Long riskId = service.findLatestRiskIdBySlopeUnitId(slopeUnitId);
        Date date = new Date();
        List<AbstractMap.SimpleEntry<PlanTypeEnum, String>> measureEntries;
        String monitoringDispatchContent = null;
        Integer isSkipped = dzTaskHandle.getIsSkipped();
        if (isSkipped != null && isSkipped == 1) {
            DzTaskHandleSceneRecordVo byHandleId = dzTaskHandleSceneRecordService.getByHandleId(handleId);
            if (Objects.isNull(byHandleId)) {
                log.error("方案不存在, isSkipped=1时需从现场记录获取");
                return false;
            }
            monitoringDispatchContent = byHandleId.getMeasuresMonitoring();
            measureEntries = Arrays.asList(
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.RESETTLEMENT, byHandleId.getMeasuresEvacuation()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.PROTECTION, byHandleId.getMeasuresProtection()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.MONITORING, byHandleId.getMeasuresMonitoring()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.HAZARD_REMOVAL, byHandleId.getMeasuresHazardRemoval()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.ENGINEERING, byHandleId.getMeasuresEngineering()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.PUBLICITY, byHandleId.getMeasuresPublicity()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.TRAFFIC_CONTROL, byHandleId.getMeasuresTrafficControl()),
                new AbstractMap.SimpleEntry<>(PlanTypeEnum.OTHER_SUGGESTIONS, byHandleId.getMeasuresOtherSuggestions())
            );
        } else {
            DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
            monitoringDispatchContent = sceneRecord == null ? null : sceneRecord.getMeasuresMonitoring();
            DzTaskHandleDetailContentVo latestDetail = dzTaskHandleDetailContentService.queryLatest(
                DetailBizTypeEnum.TASK_HANDLE.getCode(),
                handleId,
                DetailContentTypeEnum.EVACUATION_PLAN.getCode(),
                null
            );
            String aiSchemeJson = latestDetail == null ? null : latestDetail.getPlanContentJson();
            if (StringUtils.isBlank(aiSchemeJson)) {
                log.error("方案不存在, isSkipped为空或0时需从处置详情aiSchemeJson获取");
                return false;
            }
            List<EvacuationSchemeItemVo> schemeItems = JacksonUtil.toList(aiSchemeJson, EvacuationSchemeItemVo.class);
            if (ObjUtil.isEmpty(schemeItems)) {
                log.error("aiSchemeJson解析为空, handleId: {}", handleId);
                return false;
            }
            measureEntries = new ArrayList<>();
            if (schemeItems != null) {
                for (EvacuationSchemeItemVo item : schemeItems) {
                    String planTypeName = item.getPlanType();
                    PlanTypeEnum planTypeEnum = PlanTypeEnum.getByName(planTypeName);
                    if (planTypeEnum == null) {
                        planTypeEnum = PlanTypeEnum.OTHER_SUGGESTIONS;
                    }
                    String content = item.getSpecificMeasures();
                    measureEntries.add(new AbstractMap.SimpleEntry<>(planTypeEnum, content));
                }
            }
        }
        List<DzTaskDistList> tasks = buildEmergencyTasksInParallel(
            measureEntries,
            handleId,
            slopeUnitId,
            riskId,
            dzTaskHandle.getDetailedAddress(),
            monitoringDispatchContent,
            date,
            planType,
            Objects.equals(isSkipped, 1) && Objects.equals(planType, PlanTypeEnum.MONITORING.getCode())
        );
        List<DzTaskDistList> filteredTasks = service.filterOpenDuplicateTasks(tasks, date);
        if (filteredTasks.isEmpty()) {
            log.info("批量生成任务跳过重复后无新增, handleId: {}", handleId);
            return true;
        }
        service.normalizeTaskPlanFields(filteredTasks);
        filteredTasks.forEach(taskSmsContentService::populateSmsContent);
        boolean result = baseMapper.insertBatch(filteredTasks);
        if (result) {
            service.recordEmergencyBatchProcessNodes(handleId, filteredTasks, date);
        }
        log.info("批量生成任务完成，handleId: {}, 新增任务数: {}", handleId, filteredTasks.size());
        return result;
    }


    @Transactional(rollbackFor = Exception.class)
    public Integer batchGenerateByRange(List<String> streets, Long defId) {
        if (ObjUtil.isEmpty(streets) || defId == null) {
            return 0;
        }
        List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(streets);
        if (ObjUtil.isEmpty(slopeUnits)) {
            log.warn("区域不存在斜坡单元, streets: {}", streets);
            return 0;
        }
        List<String> unitIds = slopeUnits.stream()
                                         .map(SlopeUnitVo::getId)
                                         .filter(StringUtils::isNotBlank)
                                         .map(String::trim)
                                         .distinct()
                                         .toList();
        return batchGenerateByUnitIdsCore(unitIds, defId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer batchGenerateByUnitIds(List<String> unitIds, Long defId) {
        return batchGenerateByUnitIdsCore(unitIds, defId);
    }

    Integer batchGenerateByUnitIdsCore(List<String> unitIds, Long defId) {
        if (ObjUtil.isEmpty(unitIds) || defId == null) {
            return 0;
        }
        List<String> validUnitIds = unitIds.stream()
                                           .filter(StringUtils::isNotBlank)
                                           .map(String::trim)
                                           .distinct()
                                           .toList();
        if (validUnitIds.isEmpty()) {
            return 0;
        }
        Date today = new Date();
        LambdaQueryWrapper<DzTaskDistList> existingTaskWrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                                         .in(DzTaskDistList::getUnitId, validUnitIds)
                                                                         .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
                                                                         .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                                                                                                .or()
                                                                                                .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_DAILY_PATROL))
                                                                          .eq(DzTaskDistList::getDelete, 0)
                                                                          .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                                                                          .between(DzTaskDistList::getCreateDate,
                                                                              DateUtil.beginOfDay(today),
                                                                              DateUtil.endOfDay(today));
        List<DzTaskDistList> executingTasks = baseMapper.selectList(existingTaskWrapper);
        if (executingTasks.isEmpty()) {
            log.info("斜坡单元范围内无仍在执行的日常巡逻任务, defId: {}", defId);
            return 0;
        }
        updateExistingTasksWithDefenseSuggest(executingTasks, defId);
        log.info("根据斜坡单元更新日常巡逻任务完成, defId: {}, 更新任务数: {}", defId, executingTasks.size());
        return executingTasks.size();
    }

    @Transactional(rollbackFor = Exception.class)
    public int closeOpenDefRespTasksByDefId(Long defId, String reason) {
        if (defId == null) {
            return 0;
        }
        String r = StringUtils.isNotBlank(reason) ? reason : "关闭";
        Date now = new Date();
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getDefId, defId)
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                    .eq(DzTaskDistList::getDelete, 0)
                    .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
        );
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        List<Long> appSyncTaskIds = service.collectPushedUnfinishedTaskIds(tasks);
        for (DzTaskDistList t : tasks) {
            t.setStatus(DzTaskDistList.STATUS_CLOSED);
            t.setCloseReason(r);
            t.setClosedTime(now);
            t.setUpdateDate(now);
        }
        service.updateTasksBatchAndSyncAppIfNeeded(tasks, appSyncTaskIds, "批量关闭防御响应任务失败");
        tasks.forEach(t -> service.recordSystemTaskStatusProcessNode(
            t,
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(),
            r,
            now,
            TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode()
        ));
        return tasks.size();
    }

    @Transactional(rollbackFor = Exception.class)
    public int refreshDefenseTaskSuggestionByDefIds(Collection<Long> defIds) {
        if (defIds == null || defIds.isEmpty()) {
            return 0;
        }
        List<Long> validDefIds = defIds.stream()
                                       .filter(Objects::nonNull)
                                       .distinct()
                                       .toList();
        if (validDefIds.isEmpty()) {
            return 0;
        }
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .in(DzTaskDistList::getDefId, validDefIds)
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                    .eq(DzTaskDistList::getDelete, 0)
                    .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
        );
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        Map<Long, List<DzTaskDistList>> tasksByDefId = tasks.stream()
                                                            .filter(task -> task.getDefId() != null)
                                                            .collect(Collectors.groupingBy(DzTaskDistList::getDefId));
        int updatedCount = 0;
        for (Map.Entry<Long, List<DzTaskDistList>> entry : tasksByDefId.entrySet()) {
            List<DzTaskDistList> taskGroup = entry.getValue();
            if (taskGroup == null || taskGroup.isEmpty()) {
                continue;
            }
            updateExistingTasksWithDefenseSuggest(taskGroup, entry.getKey());
            updatedCount += taskGroup.size();
        }
        return updatedCount;
    }

    private void updateExistingTasksWithDefenseSuggest(List<DzTaskDistList> existingTasks, Long id) {
        if (id == null) {
            throw new ServiceException("defId不能为空");
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectOne(Wrappers.<DefRespPlan>lambdaQuery().eq(DefRespPlan::getId, id));
        if (defRespPlan == null) {
            throw new ServiceException("防御响应方案不存在, id: " + id);
        }
        Date now = new Date();
        String defense = "该单元当前处于" + defRespPlan.getName() + "的范围内,加强巡查与反馈频次并优先上报异常现象。";
        for (DzTaskDistList task : existingTasks) {
            String backupSuggestion = Objects.requireNonNullElse(task.getInspectionSuggestionBackup(),
                Objects.requireNonNullElse(task.getInspectionSuggestion(), ""));
            task.setDefId(id);
            task.setInspectionSuggestion(defense + backupSuggestion);
            task.setUpdateDate(now);
            taskSmsContentService.populateSmsContent(task);
        }
        service.updateTaskInfosBatchAndSyncAppIfNeeded(existingTasks, "更新防御响应任务建议失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateAlertTasksByDefId(Long defId) {
        if (defId == null) {
            return 0;
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(defId);
        if (defRespPlan == null) {
            log.warn("防御响应方案不存在, defId: {}", defId);
            return 0;
        }
        if (service.isTownDefRespPlan(defRespPlan)) {
            log.info("跳过非乡镇级防御响应任务生成, defId: {}, regionScopeType: {}", defId, defRespPlan.getRegionScopeType());
            return 0;
        }
        List<String> unitIds = service.resolveSlopeUnitIds(defRespPlan);
        return batchCreateAlertTasksByUnitIds(defId, unitIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateAlertTasksByUnitIds(Long defId, List<String> unitIds) {
        if (defId == null || ObjUtil.isEmpty(unitIds)) {
            return 0;
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(defId);
        if (defRespPlan == null) {
            log.warn("防御响应方案不存在, defId: {}", defId);
            return 0;
        }
        if (service.isTownDefRespPlan(defRespPlan)) {
            log.info("跳过非乡镇级防御响应任务生成, defId: {}, regionScopeType: {}", defId, defRespPlan.getRegionScopeType());
            return 0;
        }
        List<String> validUnitIds = unitIds.stream()
                                           .filter(StringUtils::isNotBlank)
                                           .map(String::trim)
                                           .distinct()
                                           .toList();
        if (validUnitIds.isEmpty()) {
            log.warn("防御响应方案指定范围内无有效斜坡单元, defId: {}", defId);
            return 0;
        }
        MonitorFrequencyParams freqParams = service.resolveMonitorFrequencyParams(defRespPlan.getLevel());
        Date now = new Date();
        Date todayStart = DateUtil.beginOfDay(now);
        Date todayEnd = DateUtil.endOfDay(now);
        List<RiskUnitOnDate> highRiskUnits = service.listUnitIdsWithHighAndVeryHighRiskOnDate(validUnitIds, now);
        Set<String> highRiskUnitIdSet = highRiskUnits.stream()
                                                     .map(RiskUnitOnDate::unitId)
                                                     .collect(Collectors.toCollection(LinkedHashSet::new));
        if (highRiskUnitIdSet.isEmpty()) {
            return 0;
        }
        Long handleId = defRespPlan.getHandleId();
        Set<String> existingMonitorUnitIds = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getDefId, defId)
                    .in(DzTaskDistList::getUnitId, highRiskUnitIdSet)
                    .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_MONITOR)
                                           .or()
                                           .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_MONITOR))
                    .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                    .eq(DzTaskDistList::getDelete, 0)
                    .between(DzTaskDistList::getCreateDate, todayStart, todayEnd)
        ).stream().map(DzTaskDistList::getUnitId).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<String> existingPatrolUnitIds = listExistingDefRespTaskUnitIds(
            defId,
            highRiskUnitIdSet,
            DzTaskDistList.PLAN_NAME_PATROL,
            todayStart,
            todayEnd
        );
        Set<String> existingOpenDailyPatrolUnitIds = service.listExistingOpenDailyPatrolTaskUnitIds(highRiskUnitIdSet, now);
        /*
         * 旧逻辑：按监测频次、巡查配额生成任务。
         * Map<String, Long> existingOpenPatrolCountByUnitId = countTasksByUnitId(...);
         * Map<String, Long> consumedPatrolCountByUnitId = countTasksByUnitId(...);
         * List<DzTaskDistList> tasks = new ArrayList<>(buildAlertTasksInParallel(
         *     highRiskUnits, defId, handleId, freqParams, now,
         *     existingMonitorUnitIds, existingOpenPatrolCountByUnitId, consumedPatrolCountByUnitId, patrolTimes
         * ));
         */
        List<DzTaskDistList> tasks = new ArrayList<>(buildDefRespSingleTasksInParallel(
            highRiskUnits,
            defId,
            handleId,
            freqParams,
            now,
            existingMonitorUnitIds,
            existingPatrolUnitIds,
            existingOpenDailyPatrolUnitIds
        ));
        if (tasks.isEmpty()) {
            return 0;
        }
        service.normalizeTaskPlanFields(tasks);
        if (!baseMapper.insertBatch(tasks)) {
            throw new ServiceException("批量创建告警任务失败");
        }
        service.recordDefRespBatchProcessNodes(defId, tasks, now);
        log.info("批量创建告警任务完成, defId: {}, 任务数: {}", defId, tasks.size());
        return tasks.size();
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer scheduleCreateMonitorTasks() {
        LambdaQueryWrapper<DefRespPlan> planWrapper = Wrappers.<DefRespPlan>lambdaQuery()
                                                              .eq(DefRespPlan::getStatus, DefRespPlanStatusEnum.TASK_PUBLISHED.getCode())
                                                              .eq(DefRespPlan::getDeleted, 0);
        List<DefRespPlan> plans = dzDefRespPlanMapper.selectList(planWrapper);
        if (ObjUtil.isEmpty(plans)) {
            return 0;
        }
        Date now = new Date();
        PlanUnitTaskContext context = service.buildPlanUnitTaskContext(plans);
        int createdCount = 0;
        for (DefRespPlan plan : plans) {
            MonitorFrequencyParams freqParams = context.frequencyByPlanId().get(plan.getId());
            List<String> unitIds = context.unitIdsByPlanId().getOrDefault(plan.getId(), List.of());
            if (unitIds.isEmpty()) {
                continue;
            }
            Long taskHandleId = context.handleIdByPlanId().get(plan.getId());
            List<RiskUnitOnDate> highRiskUnits = service.listUnitIdsWithHighAndVeryHighRiskOnDate(unitIds, now);
            List<String> highRiskUnitIds = highRiskUnits.stream()
                                                        .map(RiskUnitOnDate::unitId)
                                                        .toList();
            if (highRiskUnitIds.isEmpty()) {
                continue;
            }
            Date todayStart = DateUtil.beginOfDay(now);
            Date todayEnd = DateUtil.endOfDay(now);
            Set<String> existingMonitorUnitIds = listExistingDefRespTaskUnitIds(
                plan.getId(),
                highRiskUnitIds,
                DzTaskDistList.PLAN_NAME_MONITOR,
                todayStart,
                todayEnd
            );
            /*
             * 旧逻辑：按监测频次循环补生成任务，并自动关闭上一轮未完成任务。
             * List<DzTaskDistList> planTasks = baseMapper.selectList(...);
             * Map<String, List<DzTaskDistList>> tasksByUnitId = groupTasksByUnitId(planTasks);
             * List<DzTaskDistList> overdueTasksToClose = new ArrayList<>();
             * for (RiskUnitOnDate riskUnit : highRiskUnits) { ... }
             * if (!overdueTasksToClose.isEmpty() && !baseMapper.updateBatchById(overdueTasksToClose)) { ... }
             */
            List<DzTaskDistList> tasksToInsert = highRiskUnits.stream()
                                                              .filter(Objects::nonNull)
                                                              .filter(riskUnit -> !existingMonitorUnitIds.contains(riskUnit.unitId()))
                                                              .map(riskUnit -> service.buildMonitorTask(
                                                                  plan.getId(),
                                                                  taskHandleId,
                                                                  riskUnit.unitId(),
                                                                  riskUnit.riskId(),
                                                                  freqParams,
                                                                  now
                                                              ))
                                                              .filter(Objects::nonNull)
                                                              .toList();
            service.normalizeTaskPlanFields(tasksToInsert);
            if (!tasksToInsert.isEmpty() && !baseMapper.insertBatch(tasksToInsert)) {
                throw new ServiceException("定时生成监测员任务失败");
            }
            service.recordDefRespBatchProcessNodes(plan.getId(), tasksToInsert, now);
            createdCount += tasksToInsert.size();
        }
        if (createdCount > 0) {
            log.info("定时生成监测员任务完成, 本次创建: {}", createdCount);
        }
        return createdCount;
    }

    List<DzTaskDistList> buildEmergencyTasksInParallel(List<AbstractMap.SimpleEntry<PlanTypeEnum, String>> measureEntries,
                                                       Long handleId,
                                                       String slopeUnitId,
                                                       Long riskId,
                                                       String detailedAddress,
                                                       Date now,
                                                       Integer planType) {
        return buildEmergencyTasksInParallel(
            measureEntries,
            handleId,
            slopeUnitId,
            riskId,
            detailedAddress,
            null,
            now,
            planType
        );
    }

    List<DzTaskDistList> buildEmergencyTasksInParallel(List<AbstractMap.SimpleEntry<PlanTypeEnum, String>> measureEntries,
                                                       Long handleId,
                                                       String slopeUnitId,
                                                       Long riskId,
                                                       String detailedAddress,
                                                       String monitoringDispatchContent,
                                                       Date now,
                                                       Integer planType) {
        return buildEmergencyTasksInParallel(
            measureEntries,
            handleId,
            slopeUnitId,
            riskId,
            detailedAddress,
            monitoringDispatchContent,
            now,
            planType,
            false
        );
    }

    List<DzTaskDistList> buildEmergencyTasksInParallel(List<AbstractMap.SimpleEntry<PlanTypeEnum, String>> measureEntries,
                                                       Long handleId,
                                                       String slopeUnitId,
                                                       Long riskId,
                                                       String detailedAddress,
                                                       String monitoringDispatchContent,
                                                       Date now,
                                                       Integer planType,
                                                       boolean defaultMonitoringToInspector) {
        if (ObjUtil.isEmpty(measureEntries)) {
            return List.of();
        }
        List<CompletableFuture<List<DzTaskDistList>>> futures = measureEntries.stream()
                                                                              .filter(Objects::nonNull)
                                                                              .filter(entry -> entry.getKey() != null)
                                                                              .filter(entry -> matchesRequestedPlanType(entry.getKey(), planType))
                                                                              .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                                                                              .map(entry -> CompletableFuture.supplyAsync(
                                                                                  () -> buildEmergencyTasksForEntry(
                                                                                      handleId,
                                                                                      slopeUnitId,
                                                                                      riskId,
                                                                                      detailedAddress,
                                                                                      monitoringDispatchContent,
                                                                                      now,
                                                                                      planType,
                                                                                      entry.getKey(),
                                                                                      entry.getValue(),
                                                                                      defaultMonitoringToInspector
                                                                                  ),
                                                                                  Run.executor
                                                                              ))
                                                                              .toList();
        return awaitNestedTaskBuildResults(futures);
    }

    private boolean matchesRequestedPlanType(PlanTypeEnum planTypeEnum, Integer requestedPlanType) {
        if (requestedPlanType == null) {
            return true;
        }
        if (planTypeEnum == null) {
            return false;
        }
        if (Objects.equals(planTypeEnum.getCode(), requestedPlanType)) {
            return true;
        }
        return planTypeEnum == PlanTypeEnum.MONITORING
            && Objects.equals(requestedPlanType, DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
    }

    private List<DzTaskDistList> buildEmergencyTasksForEntry(Long handleId,
                                                             String slopeUnitId,
                                                             Long riskId,
                                                             String detailedAddress,
                                                             String monitoringDispatchContent,
                                                             Date now,
                                                             Integer requestedPlanType,
                                                             PlanTypeEnum planTypeEnum,
                                                             String content,
                                                             boolean defaultMonitoringToInspector) {
        List<AbstractMap.SimpleEntry<PlanTypeEnum, DzUserContactVo>> assignees = resolveEmergencyTaskAssignees(
            handleId,
            slopeUnitId,
            planTypeEnum,
            content,
            monitoringDispatchContent,
            requestedPlanType,
            defaultMonitoringToInspector
        );
        if (ObjUtil.isEmpty(assignees)) {
            return List.of();
        }
        return assignees.stream()
                        .map(entry -> buildEmergencyTask(
                            handleId,
                            slopeUnitId,
                            riskId,
                            entry.getValue(),
                            detailedAddress,
                            now,
                            entry.getKey(),
                            content
                        ))
                        .toList();
    }

    private List<AbstractMap.SimpleEntry<PlanTypeEnum, DzUserContactVo>> resolveEmergencyTaskAssignees(Long handleId,
                                                                                                      String slopeUnitId,
                                                                                                      PlanTypeEnum planTypeEnum,
                                                                                                      String content,
                                                                                                      String monitoringDispatchContent,
                                                                                                      Integer requestedPlanType,
                                                                                                      boolean defaultMonitoringToInspector) {
        if (planTypeEnum == null) {
            return List.of();
        }
        if (isVillageSecretaryPlan(planTypeEnum)) {
            return List.of(new AbstractMap.SimpleEntry<>(planTypeEnum,
                service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_CZS.getRoleKey())));
        }
        if (isTownNaturalResourceDirectorPlan(planTypeEnum)) {
            return List.of(new AbstractMap.SimpleEntry<>(planTypeEnum,
                service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_ZGSSZ.getRoleKey())));
        }
        if (planTypeEnum == PlanTypeEnum.INSTRUMENT_MONITORING) {
            return List.of(new AbstractMap.SimpleEntry<>(planTypeEnum,
                service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_ZGSSZ.getRoleKey())));
        }
        if (planTypeEnum == PlanTypeEnum.MONITORING) {
            String dispatchContent = resolveMonitoringDispatchContent(handleId, slopeUnitId, content, monitoringDispatchContent);
            List<AbstractMap.SimpleEntry<PlanTypeEnum, DzUserContactVo>> assignees = new ArrayList<>();
            boolean allowMassPrevention = requestedPlanType == null
                || Objects.equals(requestedPlanType, DzTaskDistList.PLAN_TYPE_MONITORING);
            boolean allowInstrument = requestedPlanType == null
                || Objects.equals(requestedPlanType, DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
            if (allowMassPrevention && dispatchContent != null && dispatchContent.contains(MONITORING_KEYWORD_MASS_PREVENTION)) {
                assignees.add(new AbstractMap.SimpleEntry<>(PlanTypeEnum.MONITORING,
                    service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_FXQXCY.getRoleKey())));
            }
            if (allowInstrument && dispatchContent != null && dispatchContent.contains(MONITORING_KEYWORD_INSTRUMENT)) {
                assignees.add(new AbstractMap.SimpleEntry<>(PlanTypeEnum.INSTRUMENT_MONITORING,
                    service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_ZGSSZ.getRoleKey())));
            }
            if (allowMassPrevention && assignees.isEmpty() && defaultMonitoringToInspector && StringUtils.isNotBlank(dispatchContent)) {
                assignees.add(new AbstractMap.SimpleEntry<>(PlanTypeEnum.MONITORING,
                    service.resolveSlopeUnitAssignee(slopeUnitId, SysRoleEnum.DZ_FXQXCY.getRoleKey())));
            }
            if (assignees.isEmpty()) {
                log.warn("监测巡查措施未包含派送关键词，跳过生成处置任务, slopeUnitId: {}, dispatchContent: {}, taskContent: {}",
                    slopeUnitId, dispatchContent, content);
            }
            return assignees;
        }
        return List.of();
    }

    private String resolveMonitoringDispatchContent(Long handleId,
                                                    String slopeUnitId,
                                                    String taskContent,
                                                    String monitoringDispatchContent) {
        if (StringUtils.isBlank(monitoringDispatchContent)) {
            return taskContent;
        }
        if (StringUtils.isNotBlank(taskContent) && !Objects.equals(monitoringDispatchContent, taskContent)) {
            log.info("监测巡查派送判断内容与任务内容不一致, handleId: {}, slopeUnitId: {}, dispatchContent: {}, taskContent: {}",
                handleId, slopeUnitId, monitoringDispatchContent, taskContent);
        }
        return monitoringDispatchContent;
    }

    private boolean isVillageSecretaryPlan(PlanTypeEnum planTypeEnum) {
        return planTypeEnum == PlanTypeEnum.RESETTLEMENT
            || planTypeEnum == PlanTypeEnum.PUBLICITY
            || planTypeEnum == PlanTypeEnum.TRAFFIC_CONTROL
            || planTypeEnum == PlanTypeEnum.PROTECTION
            || planTypeEnum == PlanTypeEnum.OTHER_SUGGESTIONS;
    }

    private boolean isTownNaturalResourceDirectorPlan(PlanTypeEnum planTypeEnum) {
        return planTypeEnum == PlanTypeEnum.HAZARD_REMOVAL
            || planTypeEnum == PlanTypeEnum.ENGINEERING;
    }

    private DzTaskDistList buildEmergencyTask(Long handleId,
                                              String slopeUnitId,
                                              Long riskId,
                                              DzUserContactVo assignee,
                                              String detailedAddress,
                                              Date now,
                                              PlanTypeEnum planTypeEnum,
                                              String content) {
        DzTaskDistList task = new DzTaskDistList();
        task.setHandleId(handleId);
        task.setUnitId(slopeUnitId);
        task.setRiskId(riskId);
        task.setPlanName(planTypeEnum.getName());
        task.setPlanType(planTypeEnum.getCode());
        task.setTaskSource(service.buildEmergencyTaskSource(slopeUnitId));
        task.setSubmitRequire(INSPECTING_REQUIRE_STANDARD);
        task.setInspectionSuggestion(content);
        task.setUserId(assignee.getUserId());
        task.setResponsiblePerson(assignee.getUserName());
        task.setResponsiblePersonPhone(assignee.getPhoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setDetailedAddress(detailedAddress);
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        service.populateStoredTaskType(task);
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    Set<String> listExistingDefRespTaskUnitIds(Long defId,
                                                       Collection<String> unitIds,
                                                       String planName,
                                                       Date start,
                                                       Date end) {
        if (defId == null || ObjUtil.isEmpty(unitIds) || StringUtils.isBlank(planName) || start == null || end == null) {
            return Set.of();
        }
        return baseMapper.selectList(
                             Wrappers.<DzTaskDistList>lambdaQuery()
                                     .eq(DzTaskDistList::getDefId, defId)
                                     .in(DzTaskDistList::getUnitId, unitIds)
                                     .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, planName)
                                                            .or()
                                                            .eq(DzTaskDistList::getTaskSource, planName))
                                     .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                                     .eq(DzTaskDistList::getDelete, 0)
                                     .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                                     .between(DzTaskDistList::getCreateDate, start, end)
                         ).stream()
                         .map(DzTaskDistList::getUnitId)
                         .filter(StringUtils::isNotBlank)
                         .map(String::trim)
                         .collect(Collectors.toSet());
    }

    List<DzTaskDistList> buildDefRespSingleTasksInParallel(List<RiskUnitOnDate> highRiskUnits,
                                                                   Long defId,
                                                                   Long handleId,
                                                                   MonitorFrequencyParams freqParams,
                                                                   Date now,
                                                                   Set<String> existingMonitorUnitIds,
                                                                   Set<String> existingPatrolUnitIds,
                                                                   Set<String> existingOpenDailyPatrolUnitIds) {
        if (ObjUtil.isEmpty(highRiskUnits)) {
            return List.of();
        }
        List<CompletableFuture<List<DzTaskDistList>>> futures = highRiskUnits.stream()
                                                                             .filter(Objects::nonNull)
                                                                             .map(riskUnit -> CompletableFuture.supplyAsync(
                                                                                 () -> buildDefRespSingleTasksForRiskUnit(
                                                                                     riskUnit,
                                                                                     defId,
                                                                                     handleId,
                                                                                     freqParams,
                                                                                     now,
                                                                                     existingMonitorUnitIds,
                                                                                     existingPatrolUnitIds,
                                                                                     existingOpenDailyPatrolUnitIds
                                                                                 ),
                                                                                 Run.executor
                                                                             ))
                                                                             .toList();
        return awaitNestedTaskBuildResults(futures);
    }

    private List<DzTaskDistList> buildDefRespSingleTasksForRiskUnit(RiskUnitOnDate riskUnit,
                                                                    Long defId,
                                                                    Long handleId,
                                                                    MonitorFrequencyParams freqParams,
                                                                    Date now,
                                                                    Set<String> existingMonitorUnitIds,
                                                                    Set<String> existingPatrolUnitIds,
                                                                    Set<String> existingOpenDailyPatrolUnitIds) {
        String unitId = riskUnit.unitId();
        List<DzTaskDistList> unitTasks = new ArrayList<>(2);
        if (!existingMonitorUnitIds.contains(unitId)) {
            DzTaskDistList monitorTask = service.buildMonitorTask(defId, handleId, unitId, riskUnit.riskId(), freqParams, now);
            if (monitorTask != null) {
                unitTasks.add(monitorTask);
            }
        }
        if (!existingPatrolUnitIds.contains(unitId) && !existingOpenDailyPatrolUnitIds.contains(unitId)) {
            DzTaskDistList patrolTask = service.buildPatrolTask(defId, handleId, unitId, riskUnit.riskId(), freqParams, now);
            if (patrolTask != null) {
                unitTasks.add(patrolTask);
            }
        }
        return unitTasks;
    }

    private List<DzTaskDistList> awaitNestedTaskBuildResults(List<CompletableFuture<List<DzTaskDistList>>> futures) {
        return awaitTaskBuildResults(futures, "并行生成任务失败").stream()
                                                                 .filter(Objects::nonNull)
                                                                 .flatMap(Collection::stream)
                                                                 .filter(Objects::nonNull)
                                                                 .toList();
    }

    private <T> List<T> awaitTaskBuildResults(List<CompletableFuture<T>> futures, String errorMessage) {
        if (ObjUtil.isEmpty(futures)) {
            return List.of();
        }
        try {
            return futures.stream()
                          .map(CompletableFuture::join)
                          .filter(Objects::nonNull)
                          .toList();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException(errorMessage + ": " + cause.getMessage());
        }
    }


}
