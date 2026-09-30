/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskReq;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskNeedAttentionEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.DailyPatrolBuildResult;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.DailyPatrolCreateResult;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.MonitorFrequencyParams;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.PlanUnitTaskContext;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.RiskUnitOnDate;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.utils.ReportInfoJsonUtils;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import org.dromara.system.domain.vo.SysUserVo;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DzDailyPatrolDelegate {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DzDailyPatrolDelegate.class);
    private static final String INSPECTING_REQUIRE_PATROL = "上传现场图像（≥2张）及文字记录";
    private static final List<Integer> HIGH_AND_VERY_HIGH_RISK_LEVELS = List.of(3, 4);
    private static final String CREATE_DAILY_PATROL_LOCK_PREFIX = "def_resp_create_daily_patrol_lock_";
    private static final long CREATE_DAILY_PATROL_LOCK_MINUTES = 30L;
    private static final long CREATE_DAILY_PATROL_LOCK_WAIT_MS = 3 * 60 * 1000L;
    private static final long CREATE_DAILY_PATROL_LOCK_RETRY_INTERVAL_MS = 500L;

    private final DzTaskDistListServiceImpl service;
    private final DzTaskDistListMapper baseMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final ISlopeUnitService slopeUnitService;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final ITaskSmsContentService taskSmsContentService;
    private final StringRedisTemplate stringRedisTemplate;

    DzDailyPatrolDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzTaskHandleMapper = service.dzTaskHandleMapper;
        this.dzRiskAssessmentMapper = service.dzRiskAssessmentMapper;
        this.slopeUnitService = service.slopeUnitService;
        this.dzDefRespPlanMapper = service.dzDefRespPlanMapper;
        this.taskSmsContentService = service.taskSmsContentService;
        this.stringRedisTemplate = service.stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public void scheduleCreatePatrolQuotaTasks() {
        closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance();
        List<DefRespPlan> taskPublishedPlans = listTaskPublishedDefRespPlansForSchedule();
        List<DefRespPlan> defenseStartedPlans = listDefenseStartedDefRespPlansForSchedule();
        Date now = new Date();
        PlanUnitTaskContext taskPublishedContext = buildPlanUnitTaskContext(taskPublishedPlans);
        PlanUnitTaskContext defenseStartedContext = buildPlanUnitTaskContext(defenseStartedPlans);
        Set<String> defRespCoveredUnitIds = buildDefRespCoveredUnitIdSet(taskPublishedContext);
        int defRespCreatedCount = createDefRespPatrolTasks(taskPublishedPlans, taskPublishedContext, now);
        DailyPatrolCreateResult dailyPatrolResult = createDailyPatrolTasks(now, defRespCoveredUnitIds);
        int defenseStartedUpdatedCount = applyDefenseStartedDailyPatrolUpdates(defenseStartedPlans, defenseStartedContext);
        int totalCreatedCount = defRespCreatedCount + dailyPatrolResult.total();
        log.info(
            "每日8点30分巡查任务生成完成, 防御响应任务: {}, 日常巡逻任务: {}, 启动响应后的日常任务更新: {}, 总计: {}",
            defRespCreatedCount,
            dailyPatrolResult.total(),
            defenseStartedUpdatedCount,
            totalCreatedCount
        );
    }

    List<String> resolveSlopeUnitIds(DefRespPlan plan) {
        return resolveSlopeUnitIds(plan, Map.of(), new HashMap<>());
    }

    PlanUnitTaskContext buildPlanUnitTaskContext(List<DefRespPlan> plans) {
        Map<Long, List<String>> unitIdsByPlanId = new HashMap<>();
        Map<Long, MonitorFrequencyParams> frequencyByPlanId = new HashMap<>();
        Map<Long, Long> handleIdByPlanId = new HashMap<>();
        if (ObjUtil.isEmpty(plans)) {
            return new PlanUnitTaskContext(unitIdsByPlanId, frequencyByPlanId, handleIdByPlanId);
        }
        List<Long> handleIds = plans.stream()
                                    .filter(plan -> plan != null && DefRespPlanTypeEnum.SINGLE.getCode().equals(plan.getType()))
                                    .map(DefRespPlan::getHandleId)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .toList();
        Map<Long, DzTaskHandle> handleMap = handleIds.isEmpty()
            ? Map.of()
            : CollStreamUtil.toIdentityMap(
            dzTaskHandleMapper.selectList(
                Wrappers.<DzTaskHandle>lambdaQuery().in(DzTaskHandle::getId, handleIds)
            ),
            DzTaskHandle::getId
        );
        Map<String, List<String>> unitIdsByStreetKey = new HashMap<>();
        for (DefRespPlan plan : plans) {
            if (plan == null || plan.getId() == null) {
                continue;
            }
            if (plan.getLevel() != null) {
                frequencyByPlanId.put(plan.getId(), getMonitorFrequencyParamsByLevel(plan.getLevel()));
            }
            if (DefRespPlanTypeEnum.SINGLE.getCode().equals(plan.getType()) && plan.getHandleId() != null) {
                handleIdByPlanId.put(plan.getId(), plan.getHandleId());
            }
            unitIdsByPlanId.put(plan.getId(), resolveSlopeUnitIds(plan, handleMap, unitIdsByStreetKey));
        }
        return new PlanUnitTaskContext(unitIdsByPlanId, frequencyByPlanId, handleIdByPlanId);
    }

    List<String> resolveSlopeUnitIds(DefRespPlan plan, Map<Long, DzTaskHandle> handleMap, Map<String, List<String>> unitIdsByStreetKey) {
        if (plan == null) {
            return List.of();
        }
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(plan.getType())) {
            Long handleId = plan.getHandleId();
            if (handleId == null) {
                return List.of();
            }
            DzTaskHandle handle = handleMap.isEmpty() ? dzTaskHandleMapper.selectById(handleId) : handleMap.get(handleId);
            if (handle == null || StringUtils.isBlank(handle.getSlopeUnitId())) {
                return List.of();
            }
            return List.of(handle.getSlopeUnitId().trim());
        }
        if (DefRespPlanTypeEnum.REGION.getCode().equals(plan.getType())) {
            List<String> streets = splitStreets(plan.getStreets());
            if (streets.isEmpty()) {
                return List.of();
            }
            String streetKey = String.join("|", streets);
            return unitIdsByStreetKey.computeIfAbsent(streetKey, key -> {
                List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(streets);
                if (slopeUnits == null) {
                    return List.of();
                }
                return slopeUnits.stream()
                                 .map(SlopeUnitVo::getId)
                                 .filter(StringUtils::isNotBlank)
                                 .map(String::trim)
                                 .distinct()
                                 .toList();
            });
        }
        return List.of();
    }

    /**
     * 从给定斜坡单元ID列表中筛选出指定日期具有高风险、极高风险等级的斜坡单元。
     * 防御响应方案生成的巡逻任务仅巡查这些斜坡单元。
     *
     * @param date 指定日期
     * @return 指定日期高风险、极高风险等级的风险ID与斜坡单元ID列表
     */
    List<RiskUnitOnDate> listHighAndVeryHighRiskUnitsOnDate(Date date) {
        return listHighAndVeryHighRiskUnitsOnDate(null, date);
    }

    List<RiskUnitOnDate> listUnitIdsWithHighAndVeryHighRiskOnDate(List<String> unitIds, Date date) {
        return listHighAndVeryHighRiskUnitsOnDate(unitIds, date);
    }

    private List<RiskUnitOnDate> listHighAndVeryHighRiskUnitsOnDate(List<String> unitIds, Date date) {
        if (date == null) {
            return List.of();
        }
        if (unitIds != null && unitIds.isEmpty()) {
            return List.of();
        }
        Date dayStart = DateUtil.beginOfDay(date);
        Date dayEnd = DateUtil.endOfDay(date);
        LambdaQueryWrapper<DzRiskAssessment> wrapper = Wrappers.<DzRiskAssessment>lambdaQuery()
                                                               .in(unitIds != null, DzRiskAssessment::getSlopeUnitId, unitIds)
                                                               .in(DzRiskAssessment::getDynamicRiskLevel, HIGH_AND_VERY_HIGH_RISK_LEVELS)
                                                               .between(DzRiskAssessment::getCreateDate, dayStart, dayEnd)
                                                               .select(DzRiskAssessment::getId,
                                                                   DzRiskAssessment::getSlopeUnitId,
                                                                   DzRiskAssessment::getCreateDate,
                                                                   DzRiskAssessment::getDynamicRiskLevel,
                                                                   DzRiskAssessment::getDynamicRiskSuggest);
        List<DzRiskAssessment> assessments = dzRiskAssessmentMapper.selectList(wrapper);
        return buildLatestRiskUnits(assessments);
    }

    List<RiskUnitOnDate> buildLatestRiskUnits(List<DzRiskAssessment> assessments) {
        if (ObjUtil.isEmpty(assessments)) {
            return List.of();
        }
        List<DzRiskAssessment> sortedAssessments = assessments.stream()
                                                              .sorted(
                                                                  Comparator.comparing(DzRiskAssessment::getCreateDate, Comparator.nullsLast(Date::compareTo))
                                                                            .thenComparing(DzRiskAssessment::getId, Comparator.nullsLast(Long::compareTo))
                                                                            .reversed()
                                                              )
                                                              .toList();
        Map<String, RiskUnitOnDate> riskUnitByUnitId = new LinkedHashMap<>();
        for (DzRiskAssessment assessment : sortedAssessments) {
            if (assessment == null || StringUtils.isBlank(assessment.getSlopeUnitId())) {
                continue;
            }
            String unitId = assessment.getSlopeUnitId().trim();
            riskUnitByUnitId.putIfAbsent(unitId, new RiskUnitOnDate(
                assessment.getId(),
                unitId,
                assessment.getDynamicRiskLevel(),
                assessment.getDynamicRiskSuggest()
            ));
        }
        return List.copyOf(riskUnitByUnitId.values());
    }

    static List<String> splitStreets(String streets) {
        if (StringUtils.isBlank(streets)) {
            return List.of();
        }
        Set<String> set = Stream.of(streets.split("[,，]"))
                                .map(String::trim)
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(set);
    }

    private static MonitorFrequencyParams getMonitorFrequencyParamsByLevel(Integer level) {
        if (level == null || level < 1 || level > 4) {
            throw new ServiceException("无效的级别, level: " + level);
        }
        return switch (level) {
            case 4 -> new MonitorFrequencyParams("每4小时1次", "6", 4);
            case 3 -> new MonitorFrequencyParams("每8小时1次", "3", 8);
            case 2 -> new MonitorFrequencyParams("每12小时1次", "2", 12);
            default -> new MonitorFrequencyParams("每24小时1次", "1", 24);
        };
    }

    MonitorFrequencyParams resolveMonitorFrequencyParams(Integer level) {
        if (level == null || level < 1 || level > 4) {
            return null;
        }
        return getMonitorFrequencyParamsByLevel(level);
    }

    DzTaskDistList buildMonitorTask(Long defId, Long handleId, String unitId, Long riskId,
                                            MonitorFrequencyParams freqParams, Date now) {
        DzUserContactVo dzUserContactVo = service.tryResolveSlopeUnitAssignee(unitId, SysRoleEnum.DZ_YHDJCY.getRoleKey());
        if (dzUserContactVo == null) {
            return null;
        }
        DzTaskDistList task = new DzTaskDistList();
        task.setDefId(defId);
        task.setHandleId(handleId);
        task.setUnitId(unitId);
        task.setRiskId(riskId);
        task.setTaskSource(service.buildDefRespTaskSource(defId));
        task.setPlanName(DzTaskDistList.PLAN_NAME_MONITOR);
        // todo 暂时采用默认巡查要求
        task.setSubmitRequire(INSPECTING_REQUIRE_PATROL);
        task.setInspectionSuggestion(buildDefRespMonitorSuggestion(freqParams));
        task.setUserId(dzUserContactVo.getUserId());
        task.setResponsiblePerson(dzUserContactVo.getUserName());
        task.setResponsiblePersonPhone(dzUserContactVo.getPhoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        service.populateStoredTaskType(task);
        service.populateTaskDetailedAddress(task);
        service.normalizeTaskPlanFields(task);
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    DzTaskDistList buildDailyPatrolTask(RiskUnitOnDate riskUnit, Date now) {
        String unitId = riskUnit.unitId();
        DzUserContactVo dzUserContactVo = service.tryResolveSlopeUnitAssigneeForPatrol(unitId, SysRoleEnum.DZ_FXQXCY.getRoleKey());
        if (dzUserContactVo == null) {
            return null;
        }
        String inspectionSuggestion = buildDailyInspectionSuggestion(riskUnit.dynamicRiskSuggest());
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId(unitId);
        task.setRiskId(riskUnit.riskId());
        task.setSubmitRequire(INSPECTING_REQUIRE_PATROL);
        task.setInspectionSuggestion(inspectionSuggestion);
        task.setInspectionSuggestionBackup(inspectionSuggestion);
        task.setTaskSource(service.buildEvalTaskSource(now));
        task.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);
        task.setUserId(dzUserContactVo.getUserId());
        task.setResponsiblePerson(dzUserContactVo.getUserName());
        task.setResponsiblePersonPhone(dzUserContactVo.getPhoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        service.populateStoredTaskType(task);
        service.populateTaskDetailedAddress(task);
        service.normalizeTaskPlanFields(task);
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    DzTaskDistList loadLatestDailyPatrolTaskOnDate(String unitId, Date date) {
        if (StringUtils.isBlank(unitId) || date == null) {
            return null;
        }
        return baseMapper.selectOne(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getUnitId, unitId.trim())
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
                    .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                                                                                         .or()
                                                                                         .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_DAILY_PATROL))
                    .eq(DzTaskDistList::getDelete, 0)
                    .between(DzTaskDistList::getCreateDate, DateUtil.beginOfDay(date), DateUtil.endOfDay(date))
                    .orderByDesc(DzTaskDistList::getCreateDate)
                    .orderByDesc(DzTaskDistList::getId)
                    .last("limit 1")
        );
    }

    boolean isFinishedDailyPatrolStatus(Integer status) {
        return Objects.equals(status, DzTaskDistList.STATUS_CLOSED)
            || Objects.equals(status, DzTaskDistList.STATUS_FEEDBACKED)
            || Objects.equals(status, DzTaskDistList.STATUS_OVERDUE);
    }

    void insertDailyPatrolTask(DzTaskDistList task) {
        if (task == null) {
            return;
        }
        service.normalizeTaskPlanFields(task);
        if (baseMapper.insert(task) <= 0) {
            throw new ServiceException("日常巡逻任务创建失败");
        }
        service.recordDailyPatrolProcessNode(task);
    }

    void refreshExistingDailyPatrolTask(DzTaskDistList target, DzTaskDistList source, Date now) {
        target.setUnitId(source.getUnitId());
        target.setUserId(source.getUserId());
        target.setRiskId(source.getRiskId());
        target.setSubmitRequire(source.getSubmitRequire());
        target.setInspectionSuggestion(source.getInspectionSuggestion());
        target.setInspectionSuggestionBackup(source.getInspectionSuggestionBackup());
        target.setSourceType(source.getSourceType());
        target.setTaskSource(source.getTaskSource());
        target.setPlanName(null);
        target.setPlanType(null);
        target.setResponsiblePerson(source.getResponsiblePerson());
        target.setResponsiblePersonPhone(source.getResponsiblePersonPhone());
        target.setDetailedAddress(source.getDetailedAddress());
        target.setSmsContent(source.getSmsContent());
        target.setDelete(0);
        target.setOverdue(DzTaskDistList.OVERDUE_NO);
        target.setQuotaConsumed(DzTaskDistList.QUOTA_CONSUMED_NO);
        target.setReminderCount(Objects.requireNonNullElse(target.getReminderCount(), 0));
        target.setUpdateDate(now);
    }

    void pushDailyPatrolTask(DzTaskDistList task) {
        Date now = new Date();
        DzTaskDistList update = new DzTaskDistList();
        update.setId(task.getId());
        update.setStatus(DzTaskDistList.STATUS_UNINSPECTED);
        update.setUpdateDate(now);
        DzTaskDistListServiceImpl.markSystemAppPushAudit(update, now);
        DzTaskDistList rollback = new DzTaskDistList();
        rollback.setId(task.getId());
        rollback.setStatus(task.getStatus());
        rollback.setUpdateDate(now);
        rollback.setAppPushType(task.getAppPushType());
        rollback.setAppPushUserId(task.getAppPushUserId());
        rollback.setAppPushTime(task.getAppPushTime());

        InspectionTaskReq req = buildFullInspectionTaskUpdateReq(task, DzTaskDistList.STATUS_UNINSPECTED);
        service.transactionTemplate.executeWithoutResult(status -> {
            if (baseMapper.updateById(update) <= 0) {
                throw new ServiceException("更新任务状态失败");
            }
        });
        try {
            service.pushTasksInParallel(List.of(req));
            task.setStatus(DzTaskDistList.STATUS_UNINSPECTED);
        } catch (Exception e) {
            service.rollbackPushStatus(List.of(rollback), e);
        }
    }

    InspectionTaskReq buildFullInspectionTaskUpdateReq(DzTaskDistList task, Integer status) {
        if (task == null || task.getId() == null) {
            throw new ServiceException("任务不存在，无法推送");
        }
        InspectionTaskReq req = new InspectionTaskReq();
        String unitId = DzTaskDistListServiceImpl.normalizeSlopeUnitId(task.getUnitId());
        req.setSlopeUnitId(StringUtils.isNotBlank(unitId) ? unitId : task.getUnitId());
        req.setTaskId(task.getId());
        req.setHandleId(task.getHandleId());
        req.setDispatchTime(DzTaskDistListServiceImpl.formatAppDispatchTime(task.getCreateDate()));
        req.setInspectorId(task.getUserId());
        SysUserVo user = resolveTaskInspector(task);
        req.setInspectorName(StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : task.getResponsiblePerson());
        req.setInspectorPhone(StringUtils.isNotBlank(user.getPhonenumber()) ? user.getPhonenumber() : task.getResponsiblePersonPhone());
        req.setInspectionSuggestion(task.getInspectionSuggestion());
        req.setReportInfo(ReportInfoJsonUtils.sanitize(task.getReportInfo()));
        req.setRelatedTaskId(task.getRelatedTaskId());
        req.setSourceType(task.getSourceType());
        req.setTaskSource(StringUtils.isNotBlank(task.getTaskSource()) ? task.getTaskSource() : DzTaskDistListServiceImpl.resolveTaskSource(task.getSourceType()));
        DzRiskAssessment riskAssessment = task.getRiskId() == null ? null : dzRiskAssessmentMapper.selectById(task.getRiskId());
        req.setDynamicRiskLevel(riskAssessment != null && riskAssessment.getDynamicRiskLevel() != null
            ? riskAssessment.getDynamicRiskLevel()
            : 0);
        SlopeUnit slopeUnit = service.resolveSlopeUnitByTaskUnitId(task.getUnitId());
        if (slopeUnit != null) {
            req.setLocationCenter(slopeUnit.getCenter());
            req.setLocationDesc(service.buildPushDetailedAddress(slopeUnit, task.getDetailedAddress()));
            req.setSlopeUnitId(slopeUnit.getId());
            req.setSlopeUnitCenter(slopeUnit.getCenter());
            req.setSlopeUnitWkt(slopeUnit.getWkt());
        }
        req.setStatus(status);
        req.setSubmitRequire(StringUtils.isNotBlank(task.getSubmitRequire()) ? task.getSubmitRequire() : DzTaskDistListServiceImpl.INSPECTING_REQUIRE_STANDARD);
        req.setTaskType(service.resolveTaskType(task));
        req.setTaskSubType(service.resolveTaskSubType(task));
        DzTaskDistListServiceImpl.validateInspectionTaskReq(req, task.getId());
        return req;
    }

    SysUserVo resolveTaskInspector(DzTaskDistList task) {
        if (task.getUserId() != null) {
            List<SysUserVo> users = service.sysUserService.selectUserByIds(List.of(task.getUserId()));
            if (ObjUtil.isNotEmpty(users)) {
                return users.getFirst();
            }
        }
        if (StringUtils.isNotBlank(task.getResponsiblePersonPhone())) {
            SysUserVo user = service.sysUserService.selectUserByPhonenumber(task.getResponsiblePersonPhone());
            if (user != null) {
                return user;
            }
        }
        throw new ServiceException("任务[" + task.getId() + "]缺少有效的巡查人员信息");
    }

    DzTaskDistList buildPatrolTask(Long defId, Long handleId, String unitId, Long riskId,
                                           MonitorFrequencyParams freqParams, Date now) {
        DzUserContactVo dzUserContactVo = service.tryResolveSlopeUnitAssigneeForPatrol(unitId, SysRoleEnum.DZ_FXQXCY.getRoleKey());
        if (dzUserContactVo == null) {
            return null;
        }
        DzTaskDistList task = new DzTaskDistList();
        task.setDefId(defId);
        task.setHandleId(handleId);
        task.setUnitId(unitId);
        task.setRiskId(riskId);
        task.setTaskSource(service.buildDefRespTaskSource(defId));
        task.setPlanName(DzTaskDistList.PLAN_NAME_PATROL);
        task.setSubmitRequire(INSPECTING_REQUIRE_PATROL);
        task.setInspectionSuggestion(buildDefRespPatrolSuggestion(freqParams));
        task.setUserId(dzUserContactVo.getUserId());
        task.setResponsiblePerson(dzUserContactVo.getUserName());
        task.setResponsiblePersonPhone(dzUserContactVo.getPhoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        service.populateStoredTaskType(task);
        service.populateTaskDetailedAddress(task);
        service.normalizeTaskPlanFields(task);
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    int createDefRespPatrolTasks(List<DefRespPlan> taskPublishedPlans, PlanUnitTaskContext taskPublishedContext, Date now) {
        if (ObjUtil.isEmpty(taskPublishedPlans) || taskPublishedContext == null) {
            return 0;
        }
        Date todayStart = DateUtil.beginOfDay(now);
        Date todayEnd = DateUtil.endOfDay(now);
        int createdCount = 0;
        for (DefRespPlan plan : taskPublishedPlans) {
            MonitorFrequencyParams freqParams = taskPublishedContext.frequencyByPlanId().get(plan.getId());
            List<String> unitIds = taskPublishedContext.unitIdsByPlanId().getOrDefault(plan.getId(), List.of());
            if (unitIds.isEmpty()) {
                continue;
            }
            Long taskHandleId = taskPublishedContext.handleIdByPlanId().get(plan.getId());
            List<RiskUnitOnDate> patrolRiskUnits = listUnitIdsWithHighAndVeryHighRiskOnDate(unitIds, now);
            Set<String> patrolUnitIds = patrolRiskUnits.stream()
                                                       .map(RiskUnitOnDate::unitId)
                                                       .filter(StringUtils::isNotBlank)
                                                       .map(String::trim)
                                                       .collect(Collectors.toCollection(LinkedHashSet::new));
            if (patrolUnitIds.isEmpty()) {
                continue;
            }
            Set<String> existingOpenDailyPatrolUnitIds = listExistingOpenDailyPatrolTaskUnitIds(patrolUnitIds, now);
            Set<String> existingPatrolUnitIds = service.listExistingDefRespTaskUnitIds(
                plan.getId(),
                patrolUnitIds,
                DzTaskDistList.PLAN_NAME_PATROL,
                todayStart,
                todayEnd
            );
            Set<String> existingMonitorUnitIds = service.listExistingDefRespTaskUnitIds(
                plan.getId(),
                patrolUnitIds,
                DzTaskDistList.PLAN_NAME_MONITOR,
                todayStart,
                todayEnd
            );
            List<DzTaskDistList> tasksToInsert = new ArrayList<>(service.buildDefRespSingleTasksInParallel(
                patrolRiskUnits,
                plan.getId(),
                taskHandleId,
                freqParams,
                now,
                existingMonitorUnitIds,
                existingPatrolUnitIds,
                existingOpenDailyPatrolUnitIds
            ));
            service.normalizeTaskPlanFields(tasksToInsert);
            if (!tasksToInsert.isEmpty() && !baseMapper.insertBatch(tasksToInsert)) {
                throw new ServiceException("防御响应任务创建失败");
            }
            service.recordDefRespBatchProcessNodes(plan.getId(), tasksToInsert, now);
            createdCount += tasksToInsert.size();
        }
        return createdCount;
    }

    Set<String> listExistingOpenDailyPatrolTaskUnitIds(Collection<String> unitIds, Date date) {
        if (ObjUtil.isEmpty(unitIds) || date == null) {
            return Set.of();
        }
        return baseMapper.selectList(
                             Wrappers.<DzTaskDistList>lambdaQuery()
                                     .in(DzTaskDistList::getUnitId, unitIds)
                                     .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                                                            .or()
                                                            .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_DAILY_PATROL))
                                     .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
                                     .eq(DzTaskDistList::getDelete, 0)
                                     .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                                     .between(DzTaskDistList::getCreateDate, DateUtil.beginOfDay(date), DateUtil.endOfDay(date))
                         ).stream()
                         .map(DzTaskDistList::getUnitId)
                         .filter(StringUtils::isNotBlank)
                         .map(String::trim)
                         .collect(Collectors.toSet());
    }

    List<DefRespPlan> listTaskPublishedDefRespPlansForSchedule() {
        return dzDefRespPlanMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getStatus, DefRespPlanStatusEnum.TASK_PUBLISHED.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .eq(DefRespPlan::getExecuteStatus, DefRespExecuteStatusEnum.RUNNING.getCode())
        );
    }

    List<DefRespPlan> listDefenseStartedDefRespPlansForSchedule() {
        return dzDefRespPlanMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getStatus, DefRespPlanStatusEnum.APPROVAL_PASSED.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .eq(DefRespPlan::getExecuteStatus, DefRespExecuteStatusEnum.RUNNING.getCode())
        );
    }

    boolean isTownDefRespPlan(DefRespPlan defRespPlan) {
        return defRespPlan == null
            || !RegionScopeTypeEnum.TOWN.getCode().equals(defRespPlan.getRegionScopeType());
    }

    private String buildDefRespMonitorSuggestion(MonitorFrequencyParams freqParams) {
        if (freqParams == null) {
            return "加强监测并及时上报异常现象，协助转移群众。";
        }
        return "按频次（" + freqParams.monitorFrequency() + "）巡查风险点，异常情况第一时间上报，协助转移群众。";
    }

    private String buildDefRespPatrolSuggestion(MonitorFrequencyParams freqParams) {
        if (freqParams == null) {
            return "加强巡查，重点排查河道、边坡、危房等易受气象灾害影响区域，及时上报隐患并协助避险。";
        }
        return "每日至少开展" + freqParams.patrolTimes() + "次巡查，重点排查河道、边坡、危房等易受气象灾害影响区域，及时上报隐患并协助避险。";
    }

    int applyDefenseStartedDailyPatrolUpdates(List<DefRespPlan> plans, PlanUnitTaskContext context) {
        if (ObjUtil.isEmpty(plans) || context == null) {
            return 0;
        }
        int updatedCount = 0;
        for (DefRespPlan plan : plans) {
            if (plan == null || plan.getId() == null) {
                continue;
            }
            List<String> unitIds = context.unitIdsByPlanId().getOrDefault(plan.getId(), List.of());
            if (unitIds.isEmpty()) {
                continue;
            }
            updatedCount += service.batchGenerateByUnitIdsCore(unitIds, plan.getId());
        }
        return updatedCount;
    }

    Set<String> buildDefRespCoveredUnitIdSet(PlanUnitTaskContext context) {
        if (context == null || context.unitIdsByPlanId().isEmpty()) {
            return Set.of();
        }
        return context.unitIdsByPlanId()
                      .values()
                      .stream()
                      .filter(Objects::nonNull)
                      .flatMap(Collection::stream)
                      .filter(StringUtils::isNotBlank)
                      .map(String::trim)
                      .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    DailyPatrolCreateResult createDailyPatrolTasks(Date now, Set<String> defRespCoveredUnitIds) {
        List<RiskUnitOnDate> dailyCandidates = listHighAndVeryHighRiskUnitsOnDate(now);
        if (dailyCandidates.isEmpty()) {
            log.info("今日无高风险、极高风险等级的斜坡单元, date: {}", now);
            return new DailyPatrolCreateResult(0, 0, 0);
        }
        return createDailyPatrolTasks(dailyCandidates, now, defRespCoveredUnitIds);
    }

    DailyPatrolCreateResult createDailyPatrolTasks(List<RiskUnitOnDate> dailyCandidates, Date now, Set<String> defRespCoveredUnitIds) {
        String lockKey = buildDailyPatrolLockKey(now);
        String lockValue = UUID.randomUUID().toString();
        if (!tryAcquireDailyPatrolLock(lockKey, lockValue)) {
            log.info("日常巡逻任务生成未获取到分布式锁，跳过本次执行, lockKey={}", lockKey);
            return new DailyPatrolCreateResult(0, 0, 0);
        }
        try {
            return doCreateDailyPatrolTasks(dailyCandidates, now, defRespCoveredUnitIds);
        } finally {
            releaseDailyPatrolLock(lockKey, lockValue);
        }
    }

    private DailyPatrolCreateResult doCreateDailyPatrolTasks(List<RiskUnitOnDate> dailyCandidates, Date now, Set<String> defRespCoveredUnitIds) {
        List<RiskUnitOnDate> filteredCandidates = dailyCandidates.stream()
                                                                 .filter(Objects::nonNull)
                                                                 .filter(riskUnit -> !defRespCoveredUnitIds.contains(riskUnit.unitId()))
                                                                 .toList();
        if (filteredCandidates.isEmpty()) {
            return new DailyPatrolCreateResult(0, 0, 0);
        }
        Date todayStart = DateUtil.beginOfDay(now);
        Date todayEnd = DateUtil.endOfDay(now);
        List<String> unitIds = filteredCandidates.stream().map(RiskUnitOnDate::unitId).toList();
        Set<String> existingDailyUnitIds = baseMapper.selectList(
                                                         Wrappers.<DzTaskDistList>lambdaQuery()
                                                                 .in(DzTaskDistList::getUnitId, unitIds)
                                                                 .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
                                                                 .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                                                                                         .or()
                                                                                         .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_DAILY_PATROL))
                                                                 .eq(DzTaskDistList::getDelete, 0)
                                                                 .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                                                                 .between(DzTaskDistList::getCreateDate, todayStart, todayEnd)
                                                     ).stream()
                                                     .map(DzTaskDistList::getUnitId)
                                                     .filter(StringUtils::isNotBlank)
                                                     .map(String::trim)
                                                     .collect(Collectors.toSet());
        Set<String> existingDefRespPatrolUnitIds = baseMapper.selectList(
                                                                 Wrappers.<DzTaskDistList>lambdaQuery()
                                                                         .in(DzTaskDistList::getUnitId, unitIds)
                                                                         .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                                                                          .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_PATROL)
                                                                                                 .or()
                                                                                                 .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_PATROL))
                                                                          .eq(DzTaskDistList::getDelete, 0)
                                                                          .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES)
                                                                          .between(DzTaskDistList::getCreateDate, todayStart, todayEnd)
                                                             ).stream()
                                                             .map(DzTaskDistList::getUnitId)
                                                             .filter(StringUtils::isNotBlank)
                                                             .map(String::trim)
                                                             .collect(Collectors.toSet());
        List<RiskUnitOnDate> candidatesToBuild = filteredCandidates.stream()
                                                                   .filter(riskUnit -> {
                                                                       String unitId = riskUnit.unitId();
                                                                       return !existingDailyUnitIds.contains(unitId)
                                                                           && !existingDefRespPatrolUnitIds.contains(unitId);
                                                                   })
                                                                   .toList();
        List<DailyPatrolBuildResult> buildResults = buildDailyPatrolTasksInParallel(candidatesToBuild, now);
        List<DzTaskDistList> tasksToInsert = buildResults.stream()
                                                         .map(DailyPatrolBuildResult::task)
                                                         .filter(Objects::nonNull)
                                                         .toList();
        service.normalizeTaskPlanFields(tasksToInsert);
        int thirdLevelCount = (int) buildResults.stream()
                                                .filter(result -> result.task() != null)
                                                .filter(result -> Objects.equals(result.dynamicRiskLevel(), 3))
                                                .count();
        int fourthLevelCount = (int) buildResults.stream()
                                                 .filter(result -> result.task() != null)
                                                 .filter(result -> Objects.equals(result.dynamicRiskLevel(), 4))
                                                 .count();
        if (!tasksToInsert.isEmpty() && !baseMapper.insertBatch(tasksToInsert)) {
            throw new ServiceException("日常巡逻任务生成失败");
        }
        tasksToInsert.forEach(service::recordDailyPatrolProcessNode);
        return new DailyPatrolCreateResult(thirdLevelCount, fourthLevelCount, tasksToInsert.size());
    }

    private String buildDailyPatrolLockKey(Date now) {
        return CREATE_DAILY_PATROL_LOCK_PREFIX + DateUtil.format(now, "yyyyMMdd");
    }

    private boolean tryAcquireDailyPatrolLock(String lockKey, String lockValue) {
        long deadline = System.currentTimeMillis() + CREATE_DAILY_PATROL_LOCK_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            Boolean locked = stringRedisTemplate.opsForValue()
                                                .setIfAbsent(lockKey, lockValue, CREATE_DAILY_PATROL_LOCK_MINUTES, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(locked)) {
                return true;
            }
            if (!waitForDailyPatrolLockRetry(deadline)) {
                return false;
            }
        }
        return false;
    }

    private boolean waitForDailyPatrolLockRetry(long deadline) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
            return false;
        }
        long sleepMs = Math.min(CREATE_DAILY_PATROL_LOCK_RETRY_INTERVAL_MS, remaining);
        try {
            Thread.sleep(sleepMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void releaseDailyPatrolLock(String lockKey, String lockValue) {
        if (StringUtils.isBlank(lockKey) || StringUtils.isBlank(lockValue)) {
            return;
        }
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (Objects.equals(lockValue, currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private List<DailyPatrolBuildResult> buildDailyPatrolTasksInParallel(List<RiskUnitOnDate> candidates, Date now) {
        if (ObjUtil.isEmpty(candidates)) {
            return List.of();
        }
        List<CompletableFuture<DailyPatrolBuildResult>> futures = candidates.stream()
                                                                            .map(riskUnit -> CompletableFuture.supplyAsync(
                                                                                () -> new DailyPatrolBuildResult(
                                                                                    buildDailyPatrolTask(riskUnit, now),
                                                                                    riskUnit.dynamicRiskLevel()
                                                                                ),
                                                                                Run.executor
                                                                            ))
                                                                            .toList();
        try {
            return futures.stream()
                          .map(CompletableFuture::join)
                          .toList();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("并行生成日常巡逻任务失败: " + cause.getMessage());
        }
    }

    private String buildDailyInspectionSuggestion(String dynamicRiskSuggest) {
        if (StringUtils.isBlank(dynamicRiskSuggest)) {
            return "重点排查坡体裂缝、挡土墙破损、排水堵塞、植被异常等隐患，发现异常立即上报。";
        }
        StringBuilder suggestionBuilder = new StringBuilder();
        for (String suggest : Arrays.stream(dynamicRiskSuggest.split(","))
                                    .map(String::trim)
                                    .filter(StringUtils::isNotBlank)
                                    .toList()) {
            TaskNeedAttentionEnum taskNeedAttentionEnum = TaskNeedAttentionEnum.codeMap.get(suggest);
            if (taskNeedAttentionEnum == null) {
                log.warn("未知的动态风险建议编码, suggest={}", suggest);
                continue;
            }
            suggestionBuilder.append(taskNeedAttentionEnum.getDaily());
        }
        return suggestionBuilder.isEmpty()
            ? "重点排查坡体裂缝、挡土墙破损、排水堵塞、植被异常等隐患，发现异常立即上报。"
            : suggestionBuilder.toString();
    }

    /**
     * 关闭今天以前 source_type=1/3 的未完成巡查任务并统计达标率。
     * 在生成新巡查任务前调用，替代原每日0点定时任务。
     */
    void closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance() {
        if (!Boolean.TRUE.equals(service.appTaskProps.getAutoExpirePreviousDayPatrolTasksEnabled())) {
            return;
        }
        int closedCount = closePreviousDayPatrolTasks();
        if (closedCount > 0) {
            log.info("关闭今天以前 source_type=1/3 的未完成巡查任务: {}", closedCount);
        }
    }

    public Integer closePreviousDayPatrolTasks() {
        Date todayStart = DateUtil.beginOfDay(new Date());
        LambdaQueryWrapper<DzTaskDistList> wrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                             .in(DzTaskDistList::getSourceType,
                                                                 DzTaskDistList.SOURCE_TYPE_EVAL,
                                                                 DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                                                             .eq(DzTaskDistList::getDelete, 0)
                                                             .lt(DzTaskDistList::getCreateDate, todayStart)
                                                             .in(DzTaskDistList::getStatus,
                                                                 DzTaskDistList.STATUS_UNPUSHED,
                                                                 DzTaskDistList.STATUS_UNINSPECTED,
                                                                 DzTaskDistList.STATUS_INSPECTING);
        List<DzTaskDistList> tasks = baseMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        List<DzTaskDistList> overdueTasks = new ArrayList<>();
        List<Long> appSyncTaskIds = service.collectPushedUnfinishedTaskIds(tasks);
        for (DzTaskDistList t : tasks) {
            t.setStatus(DzTaskDistList.STATUS_OVERDUE);
            t.setOverdue(DzTaskDistList.OVERDUE_YES);
            t.setUpdateDate(now);
            overdueTasks.add(t);
        }
        service.updateTasksBatchAndSyncAppIfNeeded(tasks, appSyncTaskIds, "关闭今天以前未完成巡查任务失败");
        overdueTasks.forEach(t -> service.recordSystemTaskStatusProcessNode(
            t,
            TaskProcessChainNodeTextEnum.TASK_OVERDUE_BY_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_OVERDUE_BY_SYSTEM.getTriggerReason(),
            now,
            TaskProcessNodeCategoryEnum.TASK_OVERDUE.getCode()
        ));
        for (DzTaskDistList t : overdueTasks) {
            try {
                service.sendRemind(t, now, 0);
            } catch (Exception e) {
                log.warn("风险区巡查员任务逾期提醒失败, taskId: {}", t.getId(), e);
            }
        }
        return tasks.size();
    }


}
