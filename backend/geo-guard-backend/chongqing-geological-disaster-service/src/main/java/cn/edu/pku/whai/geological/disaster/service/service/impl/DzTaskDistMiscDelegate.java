/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistStatStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentWarningRelation;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.InspectionRuleStatsVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistDayStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistStatSourceTypeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistStatStatusVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.DailyPatrolCreateResult;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.MissingSlopeUnitRoleData;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.PlanUnitTaskContext;
import cn.edu.pku.whai.geological.disaster.service.utils.ReportInfoJsonUtils;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.RiskUnitOnDate;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.SlopeUnitAssigneeContact;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class DzTaskDistMiscDelegate {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DzTaskDistMiscDelegate.class);
    private static final int MANUAL_REMIND_INTERVAL_MINUTES = 15;
    private static final List<Integer> HIGH_AND_VERY_HIGH_RISK_LEVELS = List.of(3, 4);
    private static final DateTimeFormatter STAT_DAY_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private final DzTaskDistListServiceImpl service;
    private final DzTaskDistListMapper baseMapper;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final ISysUserService sysUserService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final ITaskSmsContentService taskSmsContentService;
    private final AppTaskProps appTaskProps;
    private final IAppTaskService appTaskService;

    DzTaskDistMiscDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzRiskAssessmentMapper = service.dzRiskAssessmentMapper;
        this.dzDefRespPlanMapper = service.dzDefRespPlanMapper;
        this.sysUserService = service.sysUserService;
        this.slopeUnitGridMemberRelationService = service.slopeUnitGridMemberRelationService;
        this.taskSmsContentService = service.taskSmsContentService;
        this.appTaskProps = service.appTaskProps;
        this.appTaskService = service.appTaskService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DzTaskDistListBo bo) {
        service.validateDutyOfficerTaskAccess(bo.getUnitId());
        DzTaskDistList add = MapstructUtils.convert(bo, DzTaskDistList.class);
        Date now = new Date();
        add.setRiskId(service.findLatestRiskIdBySlopeUnitId(add.getUnitId()));
        add.setSourceType(Objects.requireNonNullElse(bo.getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL));
        add.setInspectionSuggestion(bo.getInspectionSuggestion());
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(add, now, false);
        if (StringUtils.isNotBlank(bo.getTaskSource())) {
            add.setTaskSource(bo.getTaskSource());
        } else if (Objects.equals(add.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)) {
            add.setTaskSource(service.buildEvalTaskSource(add.getCreateDate()));
        } else if (Objects.equals(add.getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL)) {
            add.setTaskSource(service.buildManualTaskSource());
        } else {
            add.setTaskSource(DzTaskDistListServiceImpl.resolveTaskSource(add.getSourceType()));
        }
        service.populateStoredTaskType(add);
        if (Objects.equals(bo.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)) {
            add.setPlanName(DzTaskDistList.PLAN_NAME_DAILY_PATROL);
            add.setInspectionSuggestionBackup(bo.getInspectionSuggestion());
        }
        DzUserContactVo dzUserContactVo = resolveSlopeUnitAssignee(add.getUnitId(), SysRoleEnum.DZ_FXQXCY.getRoleKey());
        add.setUserId(dzUserContactVo.getUserId());
        add.setResponsiblePerson(dzUserContactVo.getUserName());
        add.setResponsiblePersonPhone(dzUserContactVo.getPhoneNumber());
        service.populateTaskDetailedAddress(add);
        service.normalizeTaskPlanFields(add);
        validateNoDuplicateTask(add, add.getCreateDate());
        taskSmsContentService.populateSmsContent(add);

        int inserted = baseMapper.insert(add);
        if (inserted > 0) {
            bo.setId(add.getId());
            service.recordDailyPatrolProcessNode(add);
            service.recordManualTaskCreateProcessNode(add);
        }
        return inserted > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DzTaskDistListBo bo) {
        Long longId = bo.getId();
        DzTaskDistList task = baseMapper.selectById(longId);
        if (task == null) {
            log.warn("任务不存在，taskId: {}", longId);
            throw new ServiceException("任务不存在");
        }
        service.validateDutyOfficerTaskAccess(task.getUnitId());
        if (StringUtils.isNotBlank(bo.getUnitId()) && !Objects.equals(task.getUnitId(), bo.getUnitId())) {
            service.validateDutyOfficerTaskAccess(bo.getUnitId());
        }

        DzTaskDistList update = MapstructUtils.convert(bo, DzTaskDistList.class);
        update.setId(longId);
        update.setUpdateDate(new Date());
        Integer effectiveStatus = Objects.requireNonNullElse(update.getStatus(), task.getStatus());
        if (Objects.equals(effectiveStatus, DzTaskDistList.STATUS_UNPUSHED)) {
            DzTaskDistList latestTask = buildLatestTaskSnapshot(task, bo, effectiveStatus);
            service.populateTaskDetailedAddress(latestTask);
            service.normalizeTaskPlanFields(latestTask);
            update.setDetailedAddress(latestTask.getDetailedAddress());
            taskSmsContentService.populateSmsContent(latestTask);
            update.setSmsContent(latestTask.getSmsContent());
        }
        Integer effectiveSourceType = Objects.requireNonNullElse(update.getSourceType(), task.getSourceType());
        if (!Objects.equals(effectiveSourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            update.setPlanName(null);
            update.setPlanType(null);
        }
        boolean updated = service.updateTaskInfoByIdAndSyncAppIfNeeded(task, update, "更新任务失败");
        if (updated && !Objects.equals(task.getStatus(), effectiveStatus)) {
            recordTaskStatusChangedByEdit(task, update, effectiveStatus);
        }
        return updated;
    }

    private void recordTaskStatusChangedByEdit(DzTaskDistList task, DzTaskDistList update, Integer effectiveStatus) {
        Date triggerTime = update.getUpdateDate() == null ? new Date() : update.getUpdateDate();
        task.setStatus(effectiveStatus);
        task.setUpdateDate(triggerTime);
        if (Objects.equals(effectiveStatus, DzTaskDistList.STATUS_CLOSED)) {
            if (StringUtils.isNotBlank(update.getCloseReason())) {
                task.setCloseReason(update.getCloseReason());
            }
            if (update.getClosedTime() != null) {
                task.setClosedTime(update.getClosedTime());
            }
            service.recordSystemTaskStatusProcessNode(
                task,
                TaskProcessChainNodeTextEnum.TASK_CLOSE_BY_EDIT.getLinkName(),
                StringUtils.isNotBlank(task.getCloseReason())
                    ? task.getCloseReason()
                    : TaskProcessChainNodeTextEnum.TASK_CLOSE_BY_EDIT.getTriggerReason(),
                firstNonNull(task.getClosedTime(), triggerTime),
                TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode()
            );
            return;
        }
        if (Objects.equals(effectiveStatus, DzTaskDistList.STATUS_OVERDUE)) {
            service.recordSystemTaskStatusProcessNode(
                task,
                TaskProcessChainNodeTextEnum.TASK_OVERDUE_BY_EDIT.getLinkName(),
                TaskProcessChainNodeTextEnum.TASK_OVERDUE_BY_EDIT.getTriggerReason(),
                triggerTime,
                TaskProcessNodeCategoryEnum.TASK_OVERDUE.getCode()
            );
        }
    }

    private Date firstNonNull(Date first, Date second) {
        return first == null ? second : first;
    }

    private DzTaskDistList buildLatestTaskSnapshot(DzTaskDistList currentTask, DzTaskDistListBo bo, Integer effectiveStatus) {
        DzTaskDistList latestTask = new DzTaskDistList();
        latestTask.setId(currentTask.getId());
        latestTask.setUnitId(bo.getUnitId() != null ? bo.getUnitId() : currentTask.getUnitId());
        latestTask.setRiskId(bo.getRiskId() != null ? bo.getRiskId() : currentTask.getRiskId());
        latestTask.setSubmitRequire(bo.getSubmitRequire() != null ? bo.getSubmitRequire() : currentTask.getSubmitRequire());
        latestTask.setInspectionSuggestion(bo.getInspectionSuggestion() != null ? bo.getInspectionSuggestion() : currentTask.getInspectionSuggestion());
        latestTask.setStatus(effectiveStatus);
        latestTask.setSourceType(bo.getSourceType() != null ? bo.getSourceType() : currentTask.getSourceType());
        latestTask.setReportId(bo.getReportId() != null ? bo.getReportId() : currentTask.getReportId());
        latestTask.setHandleId(bo.getHandleId() != null ? bo.getHandleId() : currentTask.getHandleId());
        latestTask.setDefId(bo.getDefId() != null ? bo.getDefId() : currentTask.getDefId());
        latestTask.setTaskSource(bo.getTaskSource() != null ? bo.getTaskSource() : currentTask.getTaskSource());
        latestTask.setRelatedTaskId(bo.getRelatedTaskId() != null ? bo.getRelatedTaskId() : currentTask.getRelatedTaskId());
        latestTask.setReportInfo(ReportInfoJsonUtils.sanitize(bo.getReportInfo() != null ? bo.getReportInfo() : currentTask.getReportInfo()));
        latestTask.setPlanName(bo.getPlanName() != null ? bo.getPlanName() : currentTask.getPlanName());
        latestTask.setResponsiblePerson(bo.getResponsiblePerson() != null ? bo.getResponsiblePerson() : currentTask.getResponsiblePerson());
        latestTask.setResponsiblePersonPhone(bo.getResponsiblePersonPhone() != null ? bo.getResponsiblePersonPhone() : currentTask.getResponsiblePersonPhone());
        latestTask.setDetailedAddress(bo.getDetailedAddress() != null ? bo.getDetailedAddress() : currentTask.getDetailedAddress());
        service.populateStoredTaskType(latestTask);
        return latestTask;
    }

    void validateTaskStatusTransition(Integer currentStatus, Long taskId) {
        if (Objects.equals(currentStatus, DzTaskDistList.STATUS_UNINSPECTED)) {
            return;
        }
        Integer effectiveCurrentStatus = Objects.requireNonNullElse(currentStatus, DzTaskDistList.STATUS_UNPUSHED);
        if (!Objects.equals(effectiveCurrentStatus, DzTaskDistList.STATUS_UNPUSHED)) {
            Integer targetStatus = DzTaskDistList.STATUS_UNINSPECTED;
            log.warn("任务状态异常，taskId: {}, 当前状态: {}, 目标状态: {}", taskId, currentStatus, targetStatus);
            throw new ServiceException("任务状态不匹配");
        }
    }

    private void validateNoDuplicateTask(DzTaskDistList task, Date duplicateCheckDate) {
        if (existsOpenDuplicateTask(task, duplicateCheckDate)) {
            throw new ServiceException("同人同点位同来源存在未闭环任务，请勿重复创建");
        }
    }

    private boolean existsOpenDuplicateTask(DzTaskDistList task, Date duplicateCheckDate) {
        LambdaQueryWrapper<DzTaskDistList> wrapper = buildDuplicateTaskWrapper(
            task.getUnitId(),
            task.getUserId(),
            task.getSourceType(),
            task.getPlanName(),
            task.getPlanType(),
            task.getHandleId(),
            task.getDefId(),
            task.getRiskId(),
            duplicateCheckDate
        );
        return baseMapper.exists(wrapper);
    }

    private LambdaQueryWrapper<DzTaskDistList> buildDuplicateTaskWrapper(String unitId, Long userId, Integer sourceType,
                                                                         String planName, Integer planType, Long handleId,
                                                                         Long defId, Long riskId,
                                                                         Date duplicateCheckDate) {
        LambdaQueryWrapper<DzTaskDistList> wrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                             .eq(StringUtils.isNotBlank(unitId), DzTaskDistList::getUnitId, unitId)
                                                             .eq(userId != null, DzTaskDistList::getUserId, userId)
                                                             .eq(sourceType != null, DzTaskDistList::getSourceType, sourceType)
                                                             .eq(StringUtils.isNotBlank(planName), DzTaskDistList::getPlanName, planName)
                                                             .eq(planType != null, DzTaskDistList::getPlanType, planType)
                                                             .eq(handleId != null, DzTaskDistList::getHandleId, handleId)
                                                             .eq(defId != null, DzTaskDistList::getDefId, defId)
                                                             .eq(riskId != null, DzTaskDistList::getRiskId, riskId)
                                                             .eq(DzTaskDistList::getDelete, 0)
                                                             .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES);
        Date effectiveDate = Objects.requireNonNullElse(duplicateCheckDate, new Date());
        wrapper.between(DzTaskDistList::getCreateDate, DateUtil.beginOfDay(effectiveDate), DateUtil.endOfDay(effectiveDate));
        return wrapper;
    }

    List<DzTaskDistList> filterOpenDuplicateTasks(List<DzTaskDistList> tasks, Date duplicateCheckDate) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        List<DzTaskDistList> filteredTasks = new ArrayList<>();
        Set<String> inMemoryKeys = new LinkedHashSet<>();
        for (DzTaskDistList task : tasks) {
            if (task == null) {
                continue;
            }
            String duplicateKey = buildDuplicateTaskKey(task);
            if (!inMemoryKeys.add(duplicateKey)) {
                log.warn("跳过批量生成中的重复任务, scene: batchGenerateByHandleId, key: {}", duplicateKey);
                continue;
            }
            if (existsOpenDuplicateTask(task, duplicateCheckDate)) {
                log.info("跳过已存在的开放任务, scene: batchGenerateByHandleId, key: {}", duplicateKey);
                continue;
            }
            filteredTasks.add(task);
        }
        return filteredTasks;
    }

    String buildDuplicateTaskKey(DzTaskDistList task) {
        return String.join("|",
            Objects.toString(task.getUnitId(), ""),
            Objects.toString(task.getUserId(), ""),
            Objects.toString(task.getSourceType(), ""),
            Objects.toString(task.getPlanName(), ""),
            Objects.toString(task.getPlanType(), ""),
            Objects.toString(task.getHandleId(), ""),
            Objects.toString(task.getDefId(), ""),
            Objects.toString(task.getRiskId(), "")
        );
    }

    public Map<String, Integer> batchInsertPatrol() {
        service.closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance();
        Date now = new Date();
        List<DefRespPlan> taskPublishedPlans = service.listTaskPublishedDefRespPlansForSchedule();
        PlanUnitTaskContext taskPublishedContext = service.buildPlanUnitTaskContext(taskPublishedPlans);
        DailyPatrolCreateResult result = service.createDailyPatrolTasks(now, service.buildDefRespCoveredUnitIdSet(taskPublishedContext));
        Map<String, Integer> stringIntegerMap = new HashMap<>();
        stringIntegerMap.put("thirdLevelCount", result.thirdLevelCount());
        stringIntegerMap.put("fourthLevelCount", result.fourthLevelCount());
        stringIntegerMap.put("total", result.total());
        return stringIntegerMap;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer batchInsertDefRespPatrol() {
        service.closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance();
        Date now = new Date();
        List<DefRespPlan> taskPublishedPlans = service.listTaskPublishedDefRespPlansForSchedule();
        PlanUnitTaskContext taskPublishedContext = service.buildPlanUnitTaskContext(taskPublishedPlans);
        return service.createDefRespPatrolTasks(taskPublishedPlans, taskPublishedContext, now);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer createDailyPatrolTasksByRiskIds(List<Long> riskIds) {
        if (ObjUtil.isEmpty(riskIds)) {
            return 0;
        }
        List<Long> validRiskIds = riskIds.stream()
                                         .filter(Objects::nonNull)
                                         .distinct()
                                         .toList();
        if (validRiskIds.isEmpty()) {
            return 0;
        }
        service.closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance();
        Date now = new Date();
        Date dayStart = DateUtil.beginOfDay(now);
        Date dayEnd = DateUtil.endOfDay(now);
        List<DzRiskAssessment> assessments = dzRiskAssessmentMapper.selectList(
            Wrappers.<DzRiskAssessment>lambdaQuery()
                    .in(DzRiskAssessment::getId, validRiskIds)
                    .in(DzRiskAssessment::getDynamicRiskLevel, HIGH_AND_VERY_HIGH_RISK_LEVELS)
                    .between(DzRiskAssessment::getCreateDate, dayStart, dayEnd)
                    .select(DzRiskAssessment::getId,
                        DzRiskAssessment::getSlopeUnitId,
                        DzRiskAssessment::getCreateDate,
                        DzRiskAssessment::getDynamicRiskLevel,
                        DzRiskAssessment::getDynamicRiskSuggest)
        );
        List<RiskUnitOnDate> riskUnits = service.buildLatestRiskUnits(assessments).stream()
                                                                          .filter(riskUnit -> HIGH_AND_VERY_HIGH_RISK_LEVELS.contains(
                                                                              riskUnit.dynamicRiskLevel()))
                                                                          .toList();
        if (riskUnits.isEmpty()) {
            log.info("根据风险评估记录创建日常巡逻任务时未命中高/极高风险, riskIds: {}", validRiskIds);
            return 0;
        }
        List<DefRespPlan> taskPublishedPlans = service.listTaskPublishedDefRespPlansForSchedule();
        PlanUnitTaskContext taskPublishedContext = service.buildPlanUnitTaskContext(taskPublishedPlans);
        int createdCount = service.createDailyPatrolTasks(riskUnits, now, service.buildDefRespCoveredUnitIdSet(taskPublishedContext)).total();
        List<DefRespPlan> defenseStartedPlans = service.listDefenseStartedDefRespPlansForSchedule();
        PlanUnitTaskContext defenseStartedContext = service.buildPlanUnitTaskContext(defenseStartedPlans);
        service.applyDefenseStartedDailyPatrolUpdates(defenseStartedPlans, defenseStartedContext);
        log.info("根据风险评估记录创建日常巡逻任务完成, riskIds: {}, 命中风险数: {}, 创建任务数: {}",
            validRiskIds,
            riskUnits.size(),
            createdCount);
        return createdCount;
    }

    public Integer upsertMonitorWarningTaskByRiskId(Long riskId, boolean autoPushTask, boolean sendTaskSms) {
        if (riskId == null) {
            return 0;
        }
        DzRiskAssessment assessment = dzRiskAssessmentMapper.selectById(riskId);
        if (assessment == null || StringUtils.isBlank(assessment.getSlopeUnitId())) {
            log.warn("预警联动监测预警任务跳过，风险评估记录无效, riskId={}", riskId);
            return 0;
        }
        Long warningEventId = resolveMonitorWarningEventId(riskId);

        Date now = new Date();
        String unitId = assessment.getSlopeUnitId().trim();
        DzTaskDistList latestTask = loadLatestMonitorWarningTaskOnDate(unitId, now);
        DzTaskDistList refreshedTask = buildMonitorWarningTask(assessment, unitId, now);
        if (refreshedTask == null) {
            log.warn("预警联动监测预警任务跳过，未解析到监测员, riskId={}, unitId={}", riskId, unitId);
            return 0;
        }
        preserveExistingAssigneeIfNeeded(latestTask, refreshedTask);

        if (latestTask == null || service.isFinishedDailyPatrolStatus(latestTask.getStatus())) {
            insertMonitorWarningTask(refreshedTask);
            recordMonitorWarningChain(refreshedTask, warningEventId);
            if (canAutoPushTask(refreshedTask, autoPushTask)) {
                service.pushTasksAndMaybeSendSms(List.of(refreshedTask), sendTaskSms, "预警联动监测预警任务");
            }
            return 1;
        }

        if (!DzTaskDistListServiceImpl.canUpdateTaskInfo(latestTask, now)) {
            log.info("预警联动监测预警任务跳过信息更新，仅允许修改当天且状态为2/3的任务, taskId={}, status={}, createDate={}",
                latestTask.getId(), latestTask.getStatus(), latestTask.getCreateDate());
            if (Objects.equals(latestTask.getStatus(), DzTaskDistList.STATUS_UNPUSHED) && canAutoPushTask(latestTask, autoPushTask)) {
                service.pushTasksAndMaybeSendSms(List.of(latestTask), sendTaskSms, "预警联动监测预警任务");
            }
            return 0;
        }

        service.refreshExistingDailyPatrolTask(latestTask, refreshedTask, now);
        service.updateMonitorWarningTaskInfoByIdAndSyncAppIfNeeded(latestTask, latestTask, "预警联动监测预警任务更新失败");
        recordMonitorWarningChain(latestTask, warningEventId);

        if (Objects.equals(latestTask.getStatus(), DzTaskDistList.STATUS_UNPUSHED)) {
            if (canAutoPushTask(latestTask, autoPushTask)) {
                service.pushTasksAndMaybeSendSms(List.of(latestTask), sendTaskSms, "预警联动监测预警任务");
            }
        }
        return 1;
    }

    private Long resolveMonitorWarningEventId(Long riskId) {
        DzRiskAssessmentWarningRelation relation = service.dzRiskAssessmentWarningRelationService.getByRiskAssessmentId(riskId);
        Long warningEventId = relation == null ? null : relation.getWarningEventId();
        if (warningEventId == null) {
            log.error("监测设备预警链路写入失败，未找到真实data_tp_warning_event.id, riskId={}", riskId);
            throw new ServiceException("未找到真实监测设备预警记录，无法生成监测预警链路");
        }
        return warningEventId;
    }

    private void recordMonitorWarningChain(DzTaskDistList task, Long warningEventId) {
        if (task == null || task.getId() == null || warningEventId == null) {
            return;
        }
        String chainId = service.taskProcessChainNodeService.resolveSavedChainIdByBiz(
            TaskProcessBizTypeEnum.MONITOR_WARNING.getCode(), warningEventId);
        if (StringUtils.isBlank(chainId)) {
            chainId = service.taskProcessChainNodeService.generateChainId();
        }
        service.recordTaskProcessBizNode(chainId, TaskProcessChainNodeTextEnum.MONITOR_WARNING.getLinkName(),
            TaskProcessChainNodeTextEnum.MONITOR_WARNING.getTriggerReason(),
            TaskProcessBizTypeEnum.MONITOR_WARNING.getCode(), warningEventId, task.getId(), DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID,
            DzTaskDistListServiceImpl.AUTO_AGENT_NAME, TaskProcessSourceTypeEnum.MONITOR_WARNING.getCode());
        service.recordTaskProcessTaskNode(chainId, TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH.getTriggerReason(), task);
    }

    private DzTaskDistList buildMonitorWarningTask(DzRiskAssessment assessment, String unitId, Date now) {
        DzUserContactVo dzUserContactVo = tryResolveMonitorWarningAssignee(unitId);
        if (dzUserContactVo == null) {
            return null;
        }
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId(unitId);
        task.setRiskId(assessment.getId());
        task.setSubmitRequire(DzTaskDistListServiceImpl.INSPECTING_REQUIRE_STANDARD);
        task.setInspectionSuggestion(buildMonitorWarningSuggestion(assessment.getDynamicRiskLevel()));
        task.setInspectionSuggestionBackup(task.getInspectionSuggestion());
        task.setUserId(dzUserContactVo.getUserId());
        task.setResponsiblePerson(dzUserContactVo.getUserName());
        task.setResponsiblePersonPhone(dzUserContactVo.getPhoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING);
        task.setTaskSource(service.buildMonitorWarningTaskSource(unitId, assessment.getDynamicRiskLevel()));
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        service.populateStoredTaskType(task);
        service.populateTaskDetailedAddress(task);
        service.normalizeTaskPlanFields(task);
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    private DzUserContactVo tryResolveMonitorWarningAssignee(String unitId) {
        DzUserContactVo systemUserContact = tryResolveSlopeUnitAssignee(unitId, SysRoleEnum.DZ_YHDJCY.getRoleKey());
        if (systemUserContact != null) {
            return systemUserContact;
        }
        SlopeUnitAssigneeContact contact = tryResolveSlopeUnitAssigneeContact(unitId, SysRoleEnum.DZ_YHDJCY.getRoleKey());
        if (contact == null) {
            return null;
        }
        SysUserVo directUser = selectActiveUserByPhoneDirect(contact.phoneNumber());
        if (directUser != null && directUser.getUserId() != null) {
            return new DzUserContactVo(
                directUser.getUserId(),
                StringUtils.isNotBlank(contact.userName()) ? contact.userName() : directUser.getUserName(),
                contact.phoneNumber()
            );
        }
        log.warn("预警联动监测预警任务使用未关联系统用户的监测员联系方式生成本地任务, unitId={}, monitor={}, phone={}",
            unitId, contact.userName(), contact.phoneNumber());
        return new DzUserContactVo(null, contact.userName(), contact.phoneNumber());
    }

    private SysUserVo selectActiveUserByPhoneDirect(String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return null;
        }
        return service.sysUserMapper.selectVoOne(
            Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getPhonenumber, phoneNumber.trim())
                    .eq(SysUser::getDelFlag, "0")
                    .last("limit 1")
        );
    }

    private void preserveExistingAssigneeIfNeeded(DzTaskDistList latestTask, DzTaskDistList refreshedTask) {
        if (latestTask == null || refreshedTask == null || refreshedTask.getUserId() != null || latestTask.getUserId() == null) {
            return;
        }
        refreshedTask.setUserId(latestTask.getUserId());
        refreshedTask.setResponsiblePerson(latestTask.getResponsiblePerson());
        refreshedTask.setResponsiblePersonPhone(latestTask.getResponsiblePersonPhone());
    }

    private boolean canAutoPushTask(DzTaskDistList task, boolean autoPushTask) {
        if (!autoPushTask) {
            return false;
        }
        if (task != null && task.getUserId() != null) {
            return true;
        }
        log.warn("预警联动监测预警任务已生成但未自动推送，原因：监测员未关联系统用户, taskId={}, unitId={}",
            task == null ? null : task.getId(),
            task == null ? null : task.getUnitId());
        return false;
    }

    private String buildMonitorWarningSuggestion(Integer dynamicRiskLevel) {
        return "监测预警触发，该斜坡单元动态风险等级已变化为"
            + resolveDynamicRiskLevelName(dynamicRiskLevel)
            + "，请根据监测预警信息开展现场监测巡查，重点关注斜坡变形、裂缝、渗流等异常情况，发现险情立即上报。";
    }

    private String resolveDynamicRiskLevelName(Integer dynamicRiskLevel) {
        if (Objects.equals(dynamicRiskLevel, 4)) {
            return "极高风险";
        }
        if (Objects.equals(dynamicRiskLevel, 3)) {
            return "高风险";
        }
        if (Objects.equals(dynamicRiskLevel, 2)) {
            return "中风险";
        }
        if (Objects.equals(dynamicRiskLevel, 1)) {
            return "低风险";
        }
        if (Objects.equals(dynamicRiskLevel, 0)) {
            return "无风险";
        }
        return "未知风险";
    }

    private DzTaskDistList loadLatestMonitorWarningTaskOnDate(String unitId, Date date) {
        if (StringUtils.isBlank(unitId) || date == null) {
            return null;
        }
        return baseMapper.selectOne(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getUnitId, unitId.trim())
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)
                    .eq(DzTaskDistList::getDelete, 0)
                    .between(DzTaskDistList::getCreateDate, DateUtil.beginOfDay(date), DateUtil.endOfDay(date))
                    .orderByDesc(DzTaskDistList::getCreateDate)
                    .orderByDesc(DzTaskDistList::getId)
                    .last("limit 1")
        );
    }

    private void insertMonitorWarningTask(DzTaskDistList task) {
        if (task == null) {
            return;
        }
        if (baseMapper.insert(task) <= 0) {
            throw new ServiceException("预警联动监测预警任务创建失败");
        }
    }

    /**
     * 校验并批量删除任务派发清单信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<DzTaskDistList> tasks = baseMapper.selectByIds(ids);
        for (DzTaskDistList task : tasks) {
            service.validateDutyOfficerTaskAccess(task.getUnitId());
        }
        if (isValid) {
            if (tasks.size() != ids.size()) {
                throw new ServiceException("存在任务不存在或已删除，无法删除");
            }
            for (DzTaskDistList task : tasks) {
                if (!Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED)) {
                    throw new ServiceException("仅允许删除未推送任务");
                }
                if (task.getHandleId() != null || task.getDefId() != null || task.getReportId() != null) {
                    throw new ServiceException("已关联处置、防御响应或上报记录的任务不允许删除");
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    public List<TaskDistStatStatusVo> statStatus(TaskDistStatStatusBo bo) {
        return baseMapper.statStatus(buildTaskStatWrapper(bo, "status"));
    }

    public List<TaskDistStatSourceTypeVo> statSourceType(TaskDistStatStatusBo bo) {
        return baseMapper.statSourceType(buildTaskStatWrapper(bo, "source_type"));
    }

    private QueryWrapper<Object> buildTaskStatWrapper(TaskDistStatStatusBo bo, String groupByColumn) {
        List<DzUserAdRegionVo> userAdRegions = service.getCurrentUserAdRegionsForListQuery();
        DzTaskDistListServiceImpl.ensureListAdRegionPermission(userAdRegions);

        Date now = new Date();
        QueryWrapper<Object> lqw = Wrappers.query();
        if (bo != null && bo.getBeginTime() != null && bo.getEndTime() != null) {
            lqw.between("create_date", bo.getBeginTime(), bo.getEndTime());
        } else if (bo != null && bo.getOffset() != null) {
            DateTime dateStart = bo.getOffset() <= 0 ? DateUtil.beginOfDay(now) : DateUtil.endOfDay(DateUtil.offsetDay(now, -bo.getOffset()));
            lqw.ge("create_date", dateStart);
            lqw.le("create_date", now);
        }
        lqw.eq("delete", 0);
        if (!service.appendSlopeUnitAdRegionPermission(lqw, userAdRegions)) {
            throw DzTaskDistListServiceImpl.regionPermissionDeniedException("统计");
        }
        DzTaskDistListServiceImpl.appendRoleBasedTaskListFilters(lqw, "");
        lqw.groupBy(groupByColumn);

        return lqw;
    }

    public Integer schedulePatrolTaskReminders() {
        Date now = new Date();
        if (!isWithinPatrolReminderWindow(now)) {
            return 0;
        }
        Date todayStart = DateUtil.beginOfDay(now);
        Date todayEnd = DateUtil.endOfDay(now);
        LambdaQueryWrapper<DzTaskDistList> openWrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                                 .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_PATROL)
                                                                                        .or()
                                                                                        .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_PATROL))
                                                                 .eq(DzTaskDistList::getDelete, 0)
                                                                 .between(DzTaskDistList::getCreateDate, todayStart, todayEnd)
                                                                 .in(DzTaskDistList::getStatus, DzTaskDistList.OPEN_STATUSES);
        List<DzTaskDistList> openTasks = baseMapper.selectList(openWrapper);
        if (openTasks.isEmpty()) {
            return 0;
        }
        List<Long> defIds = openTasks.stream().map(DzTaskDistList::getDefId).filter(Objects::nonNull).distinct().toList();
        if (defIds.isEmpty()) {
            return 0;
        }
        List<DefRespPlan> plans = dzDefRespPlanMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery().in(DefRespPlan::getId, defIds)
        );
        Map<Long, DefRespPlan> planMap = CollStreamUtil.toIdentityMap(plans, DefRespPlan::getId);
        Map<String, List<DzTaskDistList>> openTaskMap = openTasks.stream()
                                                                 .filter(task -> task.getDefId() != null && StringUtils.isNotBlank(task.getUnitId()))
                                                                 .collect(Collectors.groupingBy(task -> buildPatrolUnitKey(task.getDefId(), task.getUnitId())));
        int reminded = 0;
        long duplicateWindowMs = getPatrolReminderIntervalMinutes() * 60L * 1000L;
        for (Map.Entry<String, List<DzTaskDistList>> entry : openTaskMap.entrySet()) {
            List<DzTaskDistList> unitTasks = entry.getValue();
            DzTaskDistList task = unitTasks.stream()
                                           .min(Comparator.comparing(DzTaskDistList::getCreateDate, Comparator.nullsLast(Date::compareTo)))
                                           .orElse(null);
            if (task == null) {
                continue;
            }
            DefRespPlan plan = planMap.get(task.getDefId());
            if (plan == null || !DefRespPlanStatusEnum.TASK_PUBLISHED.getCode().equals(plan.getStatus())) {
                continue;
            }
            try {
                service.sendRemind(task, now, duplicateWindowMs);
                reminded++;
            } catch (ServiceException e) {
                log.debug("跳过重复巡查催办, taskId={}", task.getId());
            } catch (Exception e) {
                log.warn("风险区巡查员兜底催办失败, taskId: {}", task.getId(), e);
            }
        }
        return reminded;
    }

    public InspectionRuleStatsVo getInspectionRuleStats(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        ZoneId zoneId = ZoneId.systemDefault();
        Date dayStart = Date.from(targetDate.atStartOfDay(zoneId).toInstant());
        Date dayEnd = Date.from(targetDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant());
        InspectionRuleStatsVo stats = new InspectionRuleStatsVo();
        stats.setDate(targetDate.toString());
        stats.setValidInspectionCount(countValidInspectionTasks(dayStart, dayEnd));
        stats.setSingleOverdueCount(countOverdueTasks(dayStart, dayEnd));
        stats.setContinuousOverdueCount(countContinuousOverdueUnits(targetDate));
        stats.setUnderQuotaCount(countUnderQuotaUnits(targetDate));
        stats.setInvalidSubmitInterceptCount(appTaskService.getInvalidSubmitCount(targetDate));
        stats.setCheckInFenceRadiusMeters(appTaskProps.getCheckInFenceRadiusMeters());
        return stats;
    }

    boolean hasRemindedInCurrentWindow(DzTaskDistList task, long windowStartMs) {
        return task.getLastRemindTime() != null && task.getLastRemindTime().getTime() >= windowStartMs;
    }

    long resolveManualRemindWindowMs(DzTaskDistList task) {
        if (task != null && DzTaskDistList.isPatrolTask(task.getPlanName())) {
            return getPatrolReminderIntervalMinutes() * 60L * 1000L;
        }
        return MANUAL_REMIND_INTERVAL_MINUTES * 60L * 1000L;
    }

    private boolean isWithinPatrolReminderWindow(Date now) {
        LocalTime current = LocalDateTimeUtil.of(now).toLocalTime();
        LocalTime startTime = parsePatrolReminderStartTime();
        return !current.isBefore(startTime);
    }

    private LocalTime parsePatrolReminderStartTime() {
        String config = appTaskProps.getPatrolReminderStartTime();
        try {
            return StringUtils.isNotBlank(config) ? LocalTime.parse(config) : LocalTime.of(17, 0);
        } catch (Exception e) {
            log.warn("风险区巡查员催办起始时间配置非法，回退默认17:00: {}", config);
            return LocalTime.of(17, 0);
        }
    }

    private int getPatrolReminderIntervalMinutes() {
        return appTaskProps.getPatrolReminderIntervalMinutes() != null && appTaskProps.getPatrolReminderIntervalMinutes() > 0
            ? appTaskProps.getPatrolReminderIntervalMinutes()
            : 15;
    }

    private int getContinuousOverdueThreshold() {
        return appTaskProps.getContinuousOverdueThreshold() != null && appTaskProps.getContinuousOverdueThreshold() > 0
            ? appTaskProps.getContinuousOverdueThreshold()
            : 2;
    }

    private String buildPatrolUnitKey(Long defId, String unitId) {
        return defId + "|" + unitId;
    }

    private long countValidInspectionTasks(Date start, Date end) {
        return baseMapper.selectCount(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED)
                    .eq(DzTaskDistList::getDelete, 0)
                    .and(wrapper -> wrapper.in(DzTaskDistList::getPlanName, DzTaskDistList.PATROL_PLAN_NAMES)
                                           .or()
                                           .in(DzTaskDistList::getTaskSource, DzTaskDistList.PATROL_PLAN_NAMES)
                                           .or()
                                           .eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_MONITOR)
                                           .or()
                                           .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_MONITOR))
                    .between(DzTaskDistList::getSubmitTime, start, end)
        );
    }

    private long countOverdueTasks(Date start, Date end) {
        return baseMapper.selectCount(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getOverdue, DzTaskDistList.OVERDUE_YES)
                    .eq(DzTaskDistList::getDelete, 0)
                    .between(DzTaskDistList::getUpdateDate, start, end)
        );
    }

    private long countContinuousOverdueUnits(LocalDate targetDate) {
        Date now = Date.from(targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return listPatrolOverdueTasksForRecentDays(now, getContinuousOverdueThreshold()).entrySet().stream()
                                                                                        .filter(entry -> hasContinuousOverdueDays(entry.getValue(), targetDate, getContinuousOverdueThreshold()))
                                                                                        .count();
    }

    private int countUnderQuotaUnits(LocalDate targetDate) {
        Date date = Date.from(targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayStart = DateUtil.beginOfDay(date);
        Date dayEnd = DateUtil.endOfDay(date);
        LambdaQueryWrapper<DefRespPlan> planWrapper = Wrappers.<DefRespPlan>lambdaQuery()
                                                              .eq(DefRespPlan::getStatus, DefRespPlanStatusEnum.TASK_PUBLISHED.getCode())
                                                              .eq(DefRespPlan::getDeleted, 0);
        List<DefRespPlan> plans = dzDefRespPlanMapper.selectList(planWrapper);
        if (ObjUtil.isEmpty(plans)) {
            return 0;
        }
        PlanUnitTaskContext context = service.buildPlanUnitTaskContext(plans);
        int underQuotaCount = 0;
        for (DefRespPlan plan : plans) {
            List<String> unitIds = context.unitIdsByPlanId().getOrDefault(plan.getId(), List.of());
            List<String> patrolUnitIds = service.listUnitIdsWithHighAndVeryHighRiskOnDate(unitIds, date).stream()
                                                                                                .map(RiskUnitOnDate::unitId)
                                                                                                .toList();
            if (patrolUnitIds.isEmpty()) {
                continue;
            }
            Set<String> completedUnitIds = baseMapper.selectList(
                                                         Wrappers.<DzTaskDistList>lambdaQuery()
                                                                 .eq(DzTaskDistList::getDefId, plan.getId())
                                                                 .in(DzTaskDistList::getUnitId, patrolUnitIds)
                                                                 .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_PATROL)
                                                                                        .or()
                                                                                        .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_PATROL))
                                                                 .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED)
                                                                 .eq(DzTaskDistList::getDelete, 0)
                                                                 .between(DzTaskDistList::getCreateDate, dayStart, dayEnd)
                                                     ).stream()
                                                     .map(DzTaskDistList::getUnitId)
                                                     .filter(StringUtils::isNotBlank)
                                                     .map(String::trim)
                                                     .collect(Collectors.toSet());
            for (String unitId : patrolUnitIds) {
                if (!completedUnitIds.contains(unitId)) {
                    underQuotaCount++;
                }
            }
        }
        return underQuotaCount;
    }

    private Map<String, List<LocalDate>> listPatrolOverdueTasksForRecentDays(Date baseDate, int days) {
        Date start = DateUtil.beginOfDay(DateUtil.offsetDay(baseDate, -(days - 1)));
        Date end = DateUtil.endOfDay(baseDate);
        List<DzTaskDistList> overdueTasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_PATROL)
                                           .or()
                                           .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_PATROL))
                    .eq(DzTaskDistList::getOverdue, DzTaskDistList.OVERDUE_YES)
                    .eq(DzTaskDistList::getDelete, 0)
                    .between(DzTaskDistList::getUpdateDate, start, end)
        );
        Map<String, List<LocalDate>> groupedDates = new HashMap<>();
        for (DzTaskDistList task : overdueTasks) {
            if (task.getDefId() == null || StringUtils.isBlank(task.getUnitId()) || task.getUpdateDate() == null) {
                continue;
            }
            String key = buildPatrolUnitKey(task.getDefId(), task.getUnitId());
            groupedDates.computeIfAbsent(key, ignore -> new ArrayList<>())
                        .add(LocalDateTimeUtil.of(task.getUpdateDate()).toLocalDate());
        }
        return groupedDates.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream().distinct().sorted().toList()
        ));
    }

    private boolean hasContinuousOverdueDays(List<LocalDate> overdueDates, LocalDate targetDate, int threshold) {
        if (ObjUtil.isEmpty(overdueDates) || targetDate == null || threshold <= 0) {
            return false;
        }
        for (int i = 0; i < threshold; i++) {
            if (!overdueDates.contains(targetDate.minusDays(i))) {
                return false;
            }
        }
        return true;
    }

    DzUserContactVo tryResolveHandleTaskAssignee(DzTaskHandle handle, String unitId) {
        DzUserContactVo inspectorAssignee = tryResolveSlopeUnitAssignee(unitId, SysRoleEnum.DZ_FXQXCY.getRoleKey());
        if (inspectorAssignee != null) {
            return inspectorAssignee;
        }
        if (handle != null && StringUtils.isNotBlank(handle.getResponsiblePersonPhone())) {
            SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(handle.getResponsiblePersonPhone());
            if (sysUserVo != null) {
                return new DzUserContactVo(
                    sysUserVo.getUserId(),
                    StringUtils.isNotBlank(handle.getResponsiblePerson()) ? handle.getResponsiblePerson() : sysUserVo.getUserName(),
                    StringUtils.isNotBlank(handle.getResponsiblePersonPhone()) ? handle.getResponsiblePersonPhone() : sysUserVo.getPhonenumber()
                );
            }
        }
        return tryResolveSlopeUnitAssignee(unitId, SysRoleEnum.DZ_XGY.getRoleKey());
    }

    DzUserContactVo tryResolveSlopeUnitAssigneeForPatrol(String unitId, String taskType) {
        SlopeUnitAssigneeContact contact = tryResolveSlopeUnitAssigneeContact(unitId, taskType);
        if (contact == null) {
            return null;
        }
        SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(contact.phoneNumber());
        if (sysUserVo == null || sysUserVo.getUserId() == null) {
            log.warn("斜坡单元[{}],无法通过电话号码找到该用户, phoneNumber: {}", unitId, contact.phoneNumber());
            return null;
        }
        return new DzUserContactVo(
            sysUserVo.getUserId(),
            StringUtils.isNotBlank(contact.userName()) ? contact.userName() : sysUserVo.getUserName(),
            contact.phoneNumber()
        );
    }

    DzUserContactVo tryResolveSlopeUnitAssignee(String unitId, String taskType) {
        SlopeUnitAssigneeContact contact = tryResolveSlopeUnitAssigneeContact(unitId, taskType);
        if (contact == null) {
            return null;
        }
        SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(contact.phoneNumber());
        if (sysUserVo == null || sysUserVo.getUserId() == null) {
            log.warn("斜坡单元[{}],无法通过电话号码找到该用户, phoneNumber: {}", unitId, contact.phoneNumber());
            return null;
        }
        return new DzUserContactVo(
            sysUserVo.getUserId(),
            StringUtils.isNotBlank(contact.userName()) ? contact.userName() : sysUserVo.getUserName(),
            contact.phoneNumber()
        );
    }

    DzUserContactVo resolveSlopeUnitAssignee(String unitId, String taskType) {
        SlopeUnitAssigneeContact contact = resolveSlopeUnitAssigneeContact(unitId, taskType);
        SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(contact.phoneNumber());
        if (sysUserVo == null || sysUserVo.getUserId() == null) {
            throw new ServiceException(unitId + "号斜坡单元的" + resolveSlopeUnitRoleName(taskType)
                + "绑定人员未关联有效系统用户，无法绑定责任人");
        }
        return new DzUserContactVo(
            sysUserVo.getUserId(),
            StringUtils.isNotBlank(contact.userName()) ? contact.userName() : sysUserVo.getUserName(),
            contact.phoneNumber()
        );
    }

    SlopeUnitAssigneeContact resolveSlopeUnitAssigneeContact(String unitId, String taskType) {
        SlopeUnitAssigneeContact contact = tryResolveSlopeUnitAssigneeContact(unitId, taskType);
        if (contact == null) {
            MissingSlopeUnitRoleData missingData = detectMissingSlopeUnitRoleData(unitId, taskType);
            if (missingData != null) {
                throw new ServiceException(missingData.unitId() + "号斜坡单元的" + missingData.roleName() + "数据缺失");
            }
            throw new ServiceException("任务类型异常");
        }
        return contact;
    }

    private String resolveSlopeUnitRoleName(String taskType) {
        SysRoleEnum roleEnum = SysRoleEnum.getByRoleKey(taskType);
        return roleEnum == null ? "相关角色" : roleEnum.getRoleName();
    }

    private SlopeUnitAssigneeContact tryResolveSlopeUnitAssigneeContact(String unitId, String taskType) {
        if (StringUtils.isBlank(unitId)) {
            log.warn("缺少斜坡单元，无法绑定责任人");
            return null;
        }
        SlopeUnitGridMemberRelationVo slopeUnitGridMemberRelationVo = slopeUnitGridMemberRelationService.queryByUnitId(unitId);
        if (ObjectUtil.isNull(slopeUnitGridMemberRelationVo)) {
            logMissingSlopeUnitRoleData(unitId, taskType, "未查询到关联记录");
            return null;
        }
        String userName;
        String phoneNumber;
        if (Objects.equals(taskType, SysRoleEnum.DZ_XGY.getRoleKey())) {
            userName = slopeUnitGridMemberRelationVo.getAssistantManager();
            phoneNumber = slopeUnitGridMemberRelationVo.getAssistantManagerPhone();
        } else if (Objects.equals(taskType, SysRoleEnum.DZ_CZS.getRoleKey())) {
            userName = slopeUnitGridMemberRelationVo.getSpecialManager();
            phoneNumber = slopeUnitGridMemberRelationVo.getSpecialManagerPhone();
        } else if (Objects.equals(taskType, SysRoleEnum.DZ_FXQXCY.getRoleKey())) {
            if (StringUtils.isNotBlank(slopeUnitGridMemberRelationVo.getInspector())) {
                userName = slopeUnitGridMemberRelationVo.getInspector();
                phoneNumber = slopeUnitGridMemberRelationVo.getInspectorPhone();
            } else {
                userName = slopeUnitGridMemberRelationVo.getSpecialManager();
                phoneNumber = slopeUnitGridMemberRelationVo.getSpecialManagerPhone();
            }
        } else if (Objects.equals(taskType, SysRoleEnum.DZ_ZGSSZ.getRoleKey())) {
            userName = slopeUnitGridMemberRelationVo.getAdminUser();
            phoneNumber = slopeUnitGridMemberRelationVo.getAdminUserPhone();
        } else if (Objects.equals(taskType, SysRoleEnum.DZ_YHDJCY.getRoleKey())) {
            userName = slopeUnitGridMemberRelationVo.getMonitor();
            phoneNumber = slopeUnitGridMemberRelationVo.getMonitorPhone();
        } else {
            log.warn("任务类型异常, unitId: {}, taskType: {}", unitId, taskType);
            return null;
        }
        if (StringUtils.isBlank(phoneNumber)) {
            logMissingSlopeUnitRoleData(unitId, taskType, "手机号缺失");
            return null;
        }
        return new SlopeUnitAssigneeContact(userName, phoneNumber);
    }

    private void logMissingSlopeUnitRoleData(String unitId, String taskType, String detail) {
        MissingSlopeUnitRoleData missingData = detectMissingSlopeUnitRoleData(unitId, taskType);
        if (missingData == null) {
            log.warn("斜坡单元[{}]角色数据缺失, taskType: {}, detail: {}", unitId, taskType, detail);
            return;
        }
        log.warn("{}号斜坡单元的{}数据缺失, detail: {}", missingData.unitId(), missingData.roleName(), detail);
    }

    private MissingSlopeUnitRoleData detectMissingSlopeUnitRoleData(String unitId, String taskType) {
        String normalizedUnitId = StringUtils.trim(unitId);
        if (StringUtils.isBlank(normalizedUnitId)) {
            return null;
        }
        if (Objects.equals(taskType, SysRoleEnum.DZ_XGY.getRoleKey())) {
            return new MissingSlopeUnitRoleData(normalizedUnitId, "协管员角色", "assistantManagerPhone");
        }
        if (Objects.equals(taskType, SysRoleEnum.DZ_CZS.getRoleKey())) {
            return new MissingSlopeUnitRoleData(normalizedUnitId, "村支书角色", "specialManagerPhone");
        }
        if (Objects.equals(taskType, SysRoleEnum.DZ_FXQXCY.getRoleKey())) {
            return new MissingSlopeUnitRoleData(normalizedUnitId, "巡查员角色", "inspectorPhone");
        }
        if (Objects.equals(taskType, SysRoleEnum.DZ_ZGSSZ.getRoleKey())) {
            return new MissingSlopeUnitRoleData(normalizedUnitId, "乡自规所所长角色", "adminUserPhone");
        }
        if (Objects.equals(taskType, SysRoleEnum.DZ_YHDJCY.getRoleKey())) {
            return new MissingSlopeUnitRoleData(normalizedUnitId, "监测员角色", "monitorPhone");
        }
        return null;
    }

    /**
     * 统计每日任务数量
     *
     * @param startDateStr 开始日期（格式：YYYY-MM-DD）
     * @param endDateStr   结束日期（格式：YYYY-MM-DD）
     * @return 每日统计列表
     */
    public List<TaskDistDayStatVo> statDay(String startDateStr, String endDateStr) {
        if (startDateStr == null || startDateStr.isBlank() || endDateStr == null || endDateStr.isBlank()) {
            throw new ServiceException("日期参数不能为空");
        }

        log.info("统计每日任务，开始日期：{}, 结束日期：{}", startDateStr, endDateStr);

        LocalDate startDate = parseStatDayDate(startDateStr);
        LocalDate endDate = parseStatDayDate(endDateStr);
        if (startDate.isAfter(endDate)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atStartOfDay();
        return baseMapper.statDay(startDateTime, endDateTime);
    }

    private LocalDate parseStatDayDate(String dateStr) {
        try {
            if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new ServiceException("日期格式无效，应为yyyy-MM-dd");
            }
            return LocalDate.parse(dateStr, STAT_DAY_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("每日任务统计日期无效，日期：{}", dateStr, e);
            throw new ServiceException("日期格式无效，应为yyyy-MM-dd");
        }
    }

    public SmsSendSummaryVo sendDefRespSmsByDefId(Long defId) {
        if (defId == null) {
            throw new ServiceException("defId不能为空");
        }
        DefRespPlan plan = dzDefRespPlanMapper.selectById(defId);
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            throw new ServiceException("防御响应方案不存在");
        }
        SmsSendSummaryVo summary = service.sendDefRespSmsDirect(defId, new Date());
        if (summary.getTotalCount() <= 0) {
            throw new ServiceException("当前防御响应方案无可发送的启动短信接收人");
        }
        return summary;
    }


}
