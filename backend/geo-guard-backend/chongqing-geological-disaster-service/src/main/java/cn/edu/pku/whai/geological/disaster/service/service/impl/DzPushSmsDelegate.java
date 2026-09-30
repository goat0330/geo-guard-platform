/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskReq;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.cache.TaskPushSmsPreviewRedisCache;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespStartSmsPreviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistPushVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistSmsPreviewItemVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespStartSmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.TaskSmsGroupKey;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.ReportInfoJsonUtils;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
class DzPushSmsDelegate {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DzPushSmsDelegate.class);
    private static final String INSPECTING_REQUIRE_STANDARD = DzTaskDistListServiceImpl.INSPECTING_REQUIRE_STANDARD;
    private static final int APP_PUSH_BATCH_SIZE = 50;
    /** 日常巡逻任务推送时发送给乡自规所所长的监管聚合短信类型。 */
    private static final String DAILY_PATROL_DIRECTOR_SMS_TYPE = "daily_patrol_director_grouped";
    private final DzTaskDistListServiceImpl service;
    private final DzTaskDistListMapper baseMapper;
    private final ISysUserService sysUserService;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final IAppTaskService appTaskService;
    private final ISlopeUnitService slopeUnitService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final AdRegionMapper adRegionMapper;
    private final ITaskSmsContentService taskSmsContentService;
    private final SmsSendService smsSendService;
    private final IDzDefRespStartSmsConfigService dzDefRespStartSmsConfigService;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final TransactionTemplate transactionTemplate;

    DzPushSmsDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.sysUserService = service.sysUserService;
        this.dzRiskAssessmentMapper = service.dzRiskAssessmentMapper;
        this.dzReportDisasterMapper = service.dzReportDisasterMapper;
        this.appTaskService = service.appTaskService;
        this.slopeUnitService = service.slopeUnitService;
        this.slopeUnitGridMemberRelationService = service.slopeUnitGridMemberRelationService;
        this.dzDefRespPlanMapper = service.dzDefRespPlanMapper;
        this.adRegionMapper = service.adRegionMapper;
        this.taskSmsContentService = service.taskSmsContentService;
        this.smsSendService = service.smsSendService;
        this.dzDefRespStartSmsConfigService = service.dzDefRespStartSmsConfigService;
        this.sysRoleMapper = service.sysRoleMapper;
        this.sysUserMapper = service.sysUserMapper;
        this.sysUserRoleMapper = service.sysUserRoleMapper;
        this.dzUserAdRegionService = service.dzUserAdRegionService;
        this.dzTaskHandleMapper = service.dzTaskHandleMapper;
        this.transactionTemplate = service.transactionTemplate;
    }

    private record PatrolMonitorDirectorSmsGroup(String directorName,
                                                 String directorPhone,
                                                 List<DzTaskDistList> tasks) {
    }
    private static String normalizeSlopeUnitId(String unitId) {
        return DzTaskDistListServiceImpl.normalizeSlopeUnitId(unitId);
    }

    private void validateTaskStatusTransition(Integer currentStatus, Long taskId) {
        service.validateTaskStatusTransition(currentStatus, taskId);
    }

    private static void validateInspectionTaskReq(InspectionTaskReq req, Long taskId) {
        DzTaskDistListServiceImpl.validateInspectionTaskReq(req, taskId);
    }

    private boolean appendCurrentUserAdRegionPermission(LambdaQueryWrapper<DzTaskDistList> lqw) {
        return service.appendCurrentUserAdRegionPermission(lqw);
    }

    private void validateCurrentUserTaskAccess(String unitId, String actionLabel) {
        service.validateCurrentUserTaskAccess(unitId, actionLabel);
    }

    private static LambdaQueryWrapper<DzTaskDistList> buildQueryWrapper(TaskDistPushBo bo) {
        return DzTaskDistListServiceImpl.buildQueryWrapper(bo);
    }

    private static List<String> splitStreets(String streets) {
        return DzTaskDistListServiceImpl.splitStreets(streets);
    }

    private String defaultString(String value, String defaultValue) {
        return service.defaultString(value, defaultValue);
    }

    public TaskDistPushVo push(TaskDistPushBo bo) {
        validateTaskPushAccess();
        TaskDistPushVo result = new TaskDistPushVo();
        LambdaQueryWrapper<DzTaskDistList> pushQueryWrapper = buildPushQueryWrapper(bo);
        appendDirectorDefaultPlanTypeFilter(pushQueryWrapper);
        appendBatchPushPermission(pushQueryWrapper, bo);
        List<DzTaskDistList> dzTaskDistLists = baseMapper.selectList(pushQueryWrapper);
        if (ObjUtil.isEmpty(dzTaskDistLists)) {
            throw new ServiceException("推送任务不存在");
        }
        dzTaskDistLists.forEach(this::validatePushTaskAccess);
        List<Long> userIds = dzTaskDistLists.stream().map(DzTaskDistList::getUserId).filter(Objects::nonNull).distinct().toList();
        List<SysUserVo> sysUserVos = userIds.isEmpty() ? List.of() : sysUserService.selectUserByIds(userIds);
        Map<Long, SysUserVo> userMap = CollStreamUtil.toIdentityMap(sysUserVos, SysUserVo::getUserId);
        Map<Long, String> userRoleNameMap = resolveUserRoleNames(userIds);
        result.setPushName(resolvePushNames(dzTaskDistLists, userIds, userMap));
        result.setRole(resolvePushRoles(dzTaskDistLists, userIds, userMap, userRoleNameMap));
        List<TaskDistSmsPreviewItemVo> previewCache = loadOrBuildTaskPushPreviewCache(
            bo,
            dzTaskDistLists,
            userMap,
            LoginHelper.getUserId(),
            true,
            true
        );

        List<Long> risks = dzTaskDistLists.stream().map(DzTaskDistList::getRiskId).filter(Objects::nonNull).distinct().toList();
        List<DzRiskAssessment> dzRiskAssessments = risks.isEmpty() ? List.of() : dzRiskAssessmentMapper.selectByIds(risks);
        Map<Long, DzRiskAssessment> riskAssessmentMap = CollStreamUtil.toIdentityMap(dzRiskAssessments, DzRiskAssessment::getId);
        List<String> unitIds = dzTaskDistLists.stream()
                                              .map(DzTaskDistList::getUnitId)
                                              .filter(StringUtils::isNotBlank)
                                              .distinct()
                                              .flatMap(id -> {
                                                  List<String> ids = new ArrayList<>();
                                                  ids.add(id);
                                                  String normalized = normalizeSlopeUnitId(id);
                                                  if (!id.equals(normalized)) {
                                                      ids.add(normalized);
                                                  }
                                                  return ids.stream();
                                              })
                                              .distinct()
                                              .toList();
        List<SlopeUnit> dzSlopeUnits = unitIds.isEmpty() ? List.of() : slopeUnitService.listPoByIds(unitIds);
        Map<String, SlopeUnit> unitMap = CollStreamUtil.toIdentityMap(dzSlopeUnits, SlopeUnit::getId);

        Date now = new Date();
        Long appPushUserId = LoginHelper.getUserId();
        List<DzTaskDistList> up = dzTaskDistLists.stream()
                                                 .map(e -> {
                                                     validateTaskStatusTransition(e.getStatus(), e.getId());
                                                     DzTaskDistList task = new DzTaskDistList();
                                                     task.setId(e.getId());
                                                     task.setStatus(DzTaskDistList.STATUS_UNINSPECTED);
                                                     task.setUpdateDate(now);
                                                     DzTaskDistListServiceImpl.markManualAppPushAudit(task, appPushUserId, now);
                                                     return task;
                                                 })
                                                 .toList();

        List<InspectionTaskReq> pu = dzTaskDistLists.stream()
                                                    .map(e -> {
                                                        InspectionTaskReq req = new InspectionTaskReq();
                                                        String unitId = normalizeSlopeUnitId(e.getUnitId());
                                                        req.setSlopeUnitId(StringUtils.isNotBlank(unitId) ? unitId : e.getUnitId());
                                                        req.setTaskId(e.getId());
                                                        req.setDispatchTime(DzTaskDistListServiceImpl.formatAppDispatchTime(new Date()));
                                                        req.setInspectorId(e.getUserId());
                                                        req.setInspectorName(e.getResponsiblePerson());
                                                        req.setInspectorPhone(e.getResponsiblePersonPhone());
                                                        req.setInspectionSuggestion(e.getInspectionSuggestion());
                                                        String reportInfo = e.getReportInfo();
                                                        String sanitize = ReportInfoJsonUtils.sanitize(reportInfo);
                                                        req.setReportInfo(sanitize);
                                                        req.setRelatedTaskId(e.getRelatedTaskId());
                                                        req.setSourceType(e.getSourceType());
                                                        req.setTaskSource(StringUtils.isNotBlank(e.getTaskSource()) ? e.getTaskSource() : DzTaskDistListServiceImpl.resolveTaskSource(e.getSourceType()));
                                                        DzRiskAssessment riskAssessment = riskAssessmentMap.get(e.getRiskId());
                                                        req.setDynamicRiskLevel(riskAssessment != null && riskAssessment.getDynamicRiskLevel() != null
                                                            ? riskAssessment.getDynamicRiskLevel()
                                                            : 0);
                                                        SlopeUnit slopeUnit = unitMap.get(unitId);
                                                        if (slopeUnit == null && StringUtils.isNotBlank(e.getUnitId())) {
                                                            slopeUnit = unitMap.get(e.getUnitId());
                                                        }
                                                        if (slopeUnit != null) {
                                                            req.setLocationCenter(slopeUnit.getCenter());
                                                            req.setLocationDesc(buildPushDetailedAddress(slopeUnit, e.getDetailedAddress()));
                                                            req.setSlopeUnitId(slopeUnit.getId());
                                                            req.setSlopeUnitCenter(slopeUnit.getCenter());
                                                            req.setSlopeUnitWkt(slopeUnit.getWkt());
                                                        }
                                                        req.setStatus(DzTaskDistList.STATUS_UNINSPECTED);
                                                        req.setSubmitRequire(StringUtils.isNotBlank(e.getSubmitRequire()) ? e.getSubmitRequire() : INSPECTING_REQUIRE_STANDARD);
                                                        req.setHandleId(e.getHandleId());
                                                        req.setTaskType(resolveTaskType(e));
                                                        req.setTaskSubType(resolveTaskSubType(e));
                                                        // 校验巡查任务必填字段是否为空
                                                        validateInspectionTaskReq(req, e.getId());
                                                        return req;
                                                    })
                                                    .toList();

        List<DzTaskDistList> rollbackTasks = dzTaskDistLists.stream()
                                                            .map(e -> {
                                                                DzTaskDistList task = new DzTaskDistList();
                                                                task.setId(e.getId());
                                                                task.setStatus(e.getStatus());
                                                                task.setUpdateDate(now);
                                                                task.setAppPushType(e.getAppPushType());
                                                                task.setAppPushUserId(e.getAppPushUserId());
                                                                task.setAppPushTime(e.getAppPushTime());
                                                                return task;
                                                            })
                                                            .toList();
        transactionTemplate.executeWithoutResult(status -> {
            if (!baseMapper.updateBatchById(up)) {
                throw new ServiceException("更新任务状态失败");
            }
        });
        //todo 推送任务
        try {
            pushTasksInParallel(pu);
            sendTaskSmsAfterPush(dzTaskDistLists, previewCache);
            dzTaskDistLists.forEach(task -> DzTaskDistListServiceImpl.markManualAppPushAudit(task, appPushUserId, now));
            service.recordTaskPushProcessNodes(dzTaskDistLists, true, now, null);
        } catch (Exception e) {
            rollbackPushStatus(rollbackTasks, e);
        }
        Map<Integer, List<DzRiskAssessment>> statRiskVoMap = CollStreamUtil.groupByKey(dzRiskAssessments, DzRiskAssessment::getDynamicRiskLevel);
        result.setHighCount(statRiskVoMap.getOrDefault(3, List.of()).size());
        result.setVeryHighCount(statRiskVoMap.getOrDefault(4, List.of()).size());
        return result;
    }

    public List<TaskDistSmsPreviewItemVo> previewSms(TaskDistPushBo bo) {
        validateTaskPushAccess();
        LambdaQueryWrapper<DzTaskDistList> pushQueryWrapper = buildPushQueryWrapper(bo);
        appendDirectorDefaultPlanTypeFilter(pushQueryWrapper);
        appendBatchPushPermission(pushQueryWrapper, bo);
        List<DzTaskDistList> tasks = baseMapper.selectList(pushQueryWrapper);
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        tasks.forEach(this::validatePushTaskAccess);

        List<Long> userIds = tasks.stream()
                                  .map(DzTaskDistList::getUserId)
                                  .filter(Objects::nonNull)
                                  .distinct()
                                  .toList();
        List<SysUserVo> users = userIds.isEmpty() ? List.of() : sysUserService.selectUserByIds(userIds, null);
        Map<Long, SysUserVo> userMap = CollStreamUtil.toIdentityMap(users, SysUserVo::getUserId);
        List<TaskDistSmsPreviewItemVo> result = loadOrBuildTaskPushPreviewCache(
            bo,
            tasks,
            userMap,
            LoginHelper.getUserId(),
            true,
            true
        );
        try {
            List<TaskDistSmsPreviewItemVo> directorPreviewItems = buildPatrolMonitorDirectorPreviewItems(tasks);
            if (ObjUtil.isNotEmpty(directorPreviewItems)) {
                List<TaskDistSmsPreviewItemVo> mergedResult = new ArrayList<>(result);
                mergedResult.addAll(directorPreviewItems);
                result = mergedResult;
            }
        } catch (Exception e) {
            log.warn("巡查、监测任务所长短信预览生成失败，保留原任务对象预览, taskIds={}", extractTaskIds(tasks), e);
        }
        if (ObjUtil.isEmpty(result)) {
            throw new ServiceException("没有可预览的短信内容");
        }
        return result;
    }

     void validateTaskPushAccess() {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZGSSZ.getRoleKey())
            && !CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("角色权限不足：仅乡自规所所长或值班员可推送任务");
        }
    }

    /**
     * 列表查询前规范化查询条件。
     */
     void normalizeRoleBasedListQuery(DzTaskDistListBo bo) {
        if (bo == null) {
            return;
        }
        if (shouldApplyDirectorDefaultPlanTypeFilter()
            && queryOnlyTargetsEmergencyTasks(bo)
            && bo.getPlanType() != null
            && !DzTaskDistList.isMonitoringPlanType(bo.getPlanType())) {
            throw directorEmergencyViewDeniedException();
        }
    }

    private static boolean queryOnlyTargetsEmergencyTasks(DzTaskDistListBo bo) {
        if (bo == null) {
            return false;
        }
        if (Objects.equals(bo.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return true;
        }
        List<Integer> sourceTypeList = bo.getSourceTypeList();
        return sourceTypeList != null
            && !sourceTypeList.isEmpty()
            && sourceTypeList.stream()
                             .filter(Objects::nonNull)
                             .allMatch(type -> Objects.equals(type, DzTaskDistList.SOURCE_TYPE_EMERGENCY));
    }

     void validateTaskViewRoleAccess(Integer sourceType, Integer planType) {
        if (shouldApplyDirectorDefaultPlanTypeFilter()
            && Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)
            && !DzTaskDistList.isMonitoringPlanType(planType)) {
            throw directorEmergencyViewDeniedException();
        }
    }

     void validateRoleBasedTaskAccess(Integer sourceType, Integer planType, boolean forPush) {
        String actionLabel = forPush ? "推送" : "查看";
        if (shouldApplyDirectorDefaultPlanTypeFilter()
            && planType != null
            && !DzTaskDistList.isMonitoringPlanType(planType)) {
            throw directorPlanTypeDeniedException(actionLabel);
        }
    }

    private static ServiceException directorEmergencyViewDeniedException() {
        return new ServiceException("角色权限不足：乡自规所所长可查看全部非应急处置任务；对应急处置任务，仅可查看planType=监测巡查（群测群防/仪器监测）的任务");
    }

    private static ServiceException directorPlanTypeDeniedException(String actionLabel) {
        return new ServiceException("角色权限不足：乡自规所所长仅可" + actionLabel + "未设置方案类型或监测巡查（群测群防/仪器监测）类型的任务");
    }

    /**
     * 按角色追加列表/统计 SQL 过滤。
     */
     static void appendRoleBasedTaskListFilters(QueryWrapper<?> ew, String tableAlias) {
        String prefix = StringUtils.isBlank(tableAlias) ? "" : tableAlias + ".";
        if (shouldApplyDirectorDefaultPlanTypeFilter()) {
            ew.and(wrapper -> wrapper.isNull(prefix + "source_type")
                                     .or()
                                     .ne(prefix + "source_type", DzTaskDistList.SOURCE_TYPE_EMERGENCY)
                                     .or(inner -> inner.eq(prefix + "source_type", DzTaskDistList.SOURCE_TYPE_EMERGENCY)
                                                       .in(prefix + "plan_type",
                                                           DzTaskDistList.PLAN_TYPE_MONITORING,
                                                           DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING)));
        }
    }

     void appendBatchPushPermission(LambdaQueryWrapper<DzTaskDistList> lqw, TaskDistPushBo bo) {
        if (hasSpecificPushTaskSelector(bo)) {
            return;
        }
        boolean hasPermission = appendCurrentUserAdRegionPermission(lqw);
        if (!hasPermission) {
            lqw.apply("1 = 0");
        }
    }

    private static boolean hasSpecificPushTaskSelector(TaskDistPushBo bo) {
        return bo != null
            && (bo.getId() != null || bo.getTaskId() != null || ObjUtil.isNotEmpty(bo.getTaskIds()));
    }

     void validatePushTaskAccess(DzTaskDistList task) {
        if (isDutyOfficer()) {
            validateRoleBasedTaskAccess(task.getSourceType(), task.getPlanType(), true);
        }
        validateCurrentUserTaskAccess(task.getUnitId(), "推送");
    }

    /**
     * 值班员可查看、推送全部来源类型任务，仍受关联行政区划范围约束。
     */
     static boolean isDutyOfficer() {
        return !CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey());
    }

     void appendDirectorDefaultPlanTypeFilter(LambdaQueryWrapper<DzTaskDistList> lqw) {
        if (!shouldApplyDirectorDefaultPlanTypeFilter()) {
            return;
        }
        lqw.and(wrapper -> wrapper.isNull(DzTaskDistList::getPlanType)
                                  .or()
                                  .in(DzTaskDistList::getPlanType,
                                      DzTaskDistList.PLAN_TYPE_MONITORING,
                                      DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING));
    }

    private static boolean shouldApplyDirectorDefaultPlanTypeFilter() {
        return CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZGSSZ.getRoleKey())
            && !CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey());
    }

     int pushTasksInParallel(List<InspectionTaskReq> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return 0;
        }
        List<List<InspectionTaskReq>> batches = partitionInspectionTasks(tasks, APP_PUSH_BATCH_SIZE);
        List<CompletableFuture<Void>> futures = batches.stream()
                                                       .map(batch -> CompletableFuture.runAsync(
                                                           () -> appTaskService.pushTask(batch),
                                                           Run.executor
                                                       ))
                                                       .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("并行推送APP任务失败: " + cause.getMessage());
        }
        return batches.size();
    }

    private List<List<InspectionTaskReq>> partitionInspectionTasks(List<InspectionTaskReq> tasks, int batchSize) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        int size = Math.max(1, batchSize);
        List<List<InspectionTaskReq>> batches = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i += size) {
            batches.add(tasks.subList(i, Math.min(i + size, tasks.size())));
        }
        return batches;
    }

    public static LambdaQueryWrapper<DzTaskDistList> buildPushQueryWrapper(TaskDistPushBo bo) {
        if (bo == null) {
            throw new ServiceException("推送参数不能为空");
        }
        normalizePushQueryBo(bo);
        bo.setStatus(1);
        LambdaQueryWrapper<DzTaskDistList> lqw = buildQueryWrapper(bo);
        if (ObjUtil.isNotEmpty(bo.getTaskIds())) {
            lqw.in(DzTaskDistList::getId, bo.getTaskIds());
        } else if (bo.getTaskId() != null) {
            lqw.eq(DzTaskDistList::getId, bo.getTaskId());
        }
        if (bo.getDelete() == null) {
            lqw.eq(DzTaskDistList::getDelete, 0);
        }
        lqw.eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_UNPUSHED);
        return lqw;
    }

    public static TaskDistPushBo normalizePushQueryBo(TaskDistPushBo bo) {
        if (bo == null) {
            return null;
        }
        if (ObjUtil.isNotEmpty(bo.getTaskIds())) {
            List<Long> normalizedTaskIds = bo.getTaskIds().stream().filter(Objects::nonNull).distinct().toList();
            bo.setTaskIds(normalizedTaskIds);
            if (normalizedTaskIds.isEmpty()) {
                bo.setTaskIds(null);
            }
        }
        if (ObjUtil.isNotEmpty(bo.getTaskIds())) {
            bo.setTaskId(null);
            bo.setId(null);
        } else if (bo.getTaskId() != null) {
            bo.setId(null);
        }
        if (bo.getSourceTypeList() != null) {
            bo.setSourceTypeList(bo.getSourceTypeList().stream().filter(Objects::nonNull).distinct().toList());
        }
        if (bo.getStatus() == null && bo.getStatusList() != null) {
            bo.setStatusList(bo.getStatusList().stream().filter(Objects::nonNull).distinct().toList());
        }
        return bo;
    }

     void rollbackPushStatus(List<DzTaskDistList> rollbackTasks, Exception cause) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                boolean success = true;
                for (DzTaskDistList task : rollbackTasks) {
                    if (task == null || task.getId() == null) {
                        continue;
                    }
                    int updated = baseMapper.update(null, Wrappers.<DzTaskDistList>lambdaUpdate()
                        .eq(DzTaskDistList::getId, task.getId())
                        .set(DzTaskDistList::getStatus, task.getStatus())
                        .set(DzTaskDistList::getUpdateDate, task.getUpdateDate())
                        .set(DzTaskDistList::getAppPushType, task.getAppPushType())
                        .set(DzTaskDistList::getAppPushUserId, task.getAppPushUserId())
                        .set(DzTaskDistList::getAppPushTime, task.getAppPushTime()));
                    if (updated <= 0) {
                        success = false;
                    }
                }
                if (!success) {
                    throw new ServiceException("推送失败后回滚任务状态失败");
                }
            });
        } catch (Exception rollbackEx) {
            log.error("任务推送失败且本地状态回滚失败", rollbackEx);
            throw new ServiceException("任务推送失败，且本地状态回滚失败，请人工核对");
        }
        throw new ServiceException("任务推送失败，已回滚本地任务状态: " + cause.getMessage());
    }

    private void populatePreviewSmsContents(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        for (DzTaskDistList task : tasks) {
            if (task == null || StringUtils.isNotBlank(task.getSmsContent())) {
                continue;
            }
            try {
                taskSmsContentService.populateSmsContent(task);
            } catch (Exception e) {
                log.error("预览补生成任务短信正文失败, taskId: {}", task.getId(), e);
            }
        }
    }

    private String buildGroupedPreviewSmsContent(List<DzTaskDistList> tasks) {
        try {
            return taskSmsContentService.generateGroupedSmsContent(tasks);
        } catch (Exception e) {
            log.error("预览生成聚合短信失败, taskIds: {}", extractTaskIds(tasks), e);
            return null;
        }
    }

    private TaskDistSmsPreviewItemVo buildSmsPreviewItem(List<DzTaskDistList> tasks,
                                                         String content,
                                                         Map<Long, SysUserVo> userMap) {
        DzTaskDistList firstTask = tasks.getFirst();
        TaskDistSmsPreviewItemVo item = new TaskDistSmsPreviewItemVo();
        Long userId = firstTask != null ? firstTask.getUserId() : null;
        item.setUserId(userId);
        SysUserVo user = userId == null ? null : userMap.get(userId);
        String nickName = user != null ? user.getNickName() : null;
        if (StringUtils.isBlank(nickName) && firstTask != null) {
            nickName = firstTask.getResponsiblePerson();
        }
        item.setNickName(nickName);
        item.setTaskIdList(extractTaskIds(tasks));
        item.setSmsContent(content);
        return item;
    }

    private List<TaskDistSmsPreviewItemVo> loadOrBuildTaskPushPreviewCache(TaskDistPushBo bo,
                                                                           List<DzTaskDistList> tasks,
                                                                           Map<Long, SysUserVo> userMap,
                                                                           Long operatorUserId,
                                                                           boolean preferCache,
                                                                           boolean writeCache) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        String previewKey = buildTaskPushPreviewKey(bo, tasks, operatorUserId);
        if (preferCache) {
            List<TaskDistSmsPreviewItemVo> cached = TaskPushSmsPreviewRedisCache.getPreview(previewKey);
            if (ObjUtil.isNotEmpty(cached)) {
                return cached;
            }
        }
        List<TaskDistSmsPreviewItemVo> previewItems = buildTaskPushPreviewItems(tasks, userMap);
        if (writeCache && ObjUtil.isNotEmpty(previewItems)) {
            TaskPushSmsPreviewRedisCache.setPreview(previewKey, previewItems);
        }
        return previewItems;
    }

    private List<TaskDistSmsPreviewItemVo> buildTaskPushPreviewItems(List<DzTaskDistList> tasks,
                                                                     Map<Long, SysUserVo> userMap) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        populatePreviewSmsContents(tasks);
        List<TaskDistSmsPreviewItemVo> result = new ArrayList<>();
        Map<TaskSmsGroupKey, List<DzTaskDistList>> groupedTasks = tasks.stream()
                                                                       .filter(Objects::nonNull)
                                                                       .collect(Collectors.groupingBy(
                                                                           this::buildTaskSmsGroupKey,
                                                                           LinkedHashMap::new,
                                                                           Collectors.toList()
                                                                       ));
        for (Map.Entry<TaskSmsGroupKey, List<DzTaskDistList>> entry : groupedTasks.entrySet()) {
            List<DzTaskDistList> groupedGroupTasks = entry.getValue();
            if (ObjUtil.isEmpty(groupedGroupTasks)) {
                continue;
            }
            String content = buildGroupedPreviewSmsContent(groupedGroupTasks);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            result.add(buildSmsPreviewItem(groupedGroupTasks, content, userMap));
        }
        return result;
    }

    private String buildTaskPushPreviewKey(TaskDistPushBo bo, List<DzTaskDistList> tasks, Long operatorUserId) {
        String queryDigest = buildTaskPushQueryDigest(bo, operatorUserId);
        String snapshotDigest = buildTaskPushSnapshotDigest(tasks);
        return Integer.toHexString((queryDigest + "|" + snapshotDigest).hashCode());
    }

    private String buildTaskPushQueryDigest(TaskDistPushBo bo, Long operatorUserId) {
        Map<String, Object> digestMap = new LinkedHashMap<>();
        digestMap.put("operatorUserId", operatorUserId);
        digestMap.put("id", bo == null ? null : bo.getId());
        digestMap.put("taskId", bo == null ? null : bo.getTaskId());
        digestMap.put("taskIds", bo == null || bo.getTaskIds() == null ? List.of() : bo.getTaskIds().stream().filter(Objects::nonNull).sorted().toList());
        digestMap.put("unitId", bo == null ? null : bo.getUnitId());
        digestMap.put("userId", bo == null ? null : bo.getUserId());
        digestMap.put("riskId", bo == null ? null : bo.getRiskId());
        digestMap.put("dynamicRiskLevel", bo == null ? null : bo.getDynamicRiskLevel());
        digestMap.put("dynamicRiskLevels", bo == null || bo.getDynamicRiskLevels() == null ? List.of() : bo.getDynamicRiskLevels().stream().filter(Objects::nonNull).sorted().toList());
        digestMap.put("pilotArea1", bo == null ? null : bo.getPilotArea1());
        digestMap.put("pilotArea2", bo == null ? null : bo.getPilotArea2());
        digestMap.put("submitRequire", bo == null ? null : bo.getSubmitRequire());
        digestMap.put("inspectionSuggestion", bo == null ? null : bo.getInspectionSuggestion());
        digestMap.put("inspectionSuggestionBackup", bo == null ? null : bo.getInspectionSuggestionBackup());
        digestMap.put("scenePhoto", bo == null ? null : bo.getScenePhoto());
        digestMap.put("textRecord", bo == null ? null : bo.getTextRecord());
        digestMap.put("status", bo == null ? null : bo.getStatus());
        digestMap.put("statusList", bo == null || bo.getStatusList() == null ? List.of() : bo.getStatusList().stream().filter(Objects::nonNull).sorted().toList());
        digestMap.put("createDate", bo == null || bo.getCreateDate() == null ? null : bo.getCreateDate().getTime());
        digestMap.put("updateDate", bo == null || bo.getUpdateDate() == null ? null : bo.getUpdateDate().getTime());
        digestMap.put("checkTime", bo == null || bo.getCheckTime() == null ? null : bo.getCheckTime().getTime());
        digestMap.put("checkCenter", bo == null ? null : bo.getCheckCenter());
        digestMap.put("submitTime", bo == null || bo.getSubmitTime() == null ? null : bo.getSubmitTime().getTime());
        digestMap.put("sourceType", bo == null ? null : bo.getSourceType());
        digestMap.put("sourceTypeList", bo == null || bo.getSourceTypeList() == null ? List.of() : bo.getSourceTypeList().stream().filter(Objects::nonNull).sorted().toList());
        digestMap.put("isEmergency", bo == null ? null : bo.getIsEmergency());
        digestMap.put("reportId", bo == null ? null : bo.getReportId());
        digestMap.put("handleId", bo == null ? null : bo.getHandleId());
        digestMap.put("defId", bo == null ? null : bo.getDefId());
        digestMap.put("planName", bo == null ? null : bo.getPlanName());
        digestMap.put("planType", bo == null ? null : bo.getPlanType());
        digestMap.put("responsiblePerson", bo == null ? null : bo.getResponsiblePerson());
        digestMap.put("responsiblePersonPhone", bo == null ? null : bo.getResponsiblePersonPhone());
        digestMap.put("delete", bo == null ? null : bo.getDelete());
        digestMap.put("overdue", bo == null ? null : bo.getOverdue());
        digestMap.put("quotaConsumed", bo == null ? null : bo.getQuotaConsumed());
        digestMap.put("lastRemindTime", bo == null || bo.getLastRemindTime() == null ? null : bo.getLastRemindTime().getTime());
        digestMap.put("reminderCount", bo == null ? null : bo.getReminderCount());
        digestMap.put("closeReason", bo == null ? null : bo.getCloseReason());
        digestMap.put("closedTime", bo == null || bo.getClosedTime() == null ? null : bo.getClosedTime().getTime());
        digestMap.put("asc", bo == null || bo.getAsc() == null ? List.of() : bo.getAsc().stream().filter(StringUtils::isNotBlank).map(String::trim).toList());
        digestMap.put("desc", bo == null || bo.getDesc() == null ? List.of() : bo.getDesc().stream().filter(StringUtils::isNotBlank).map(String::trim).toList());
        return JacksonUtil.toJson(digestMap);
    }

    private String buildTaskPushSnapshotDigest(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return "empty";
        }
        List<Map<String, Object>> snapshot = tasks.stream()
                                                  .filter(Objects::nonNull)
                                                  .sorted(Comparator.comparing(DzTaskDistList::getId, Comparator.nullsLast(Long::compareTo)))
                                                  .map(task -> {
                                                      Map<String, Object> item = new LinkedHashMap<>();
                                                      item.put("id", task.getId());
                                                      item.put("status", task.getStatus());
                                                      item.put("sourceType", task.getSourceType());
                                                      item.put("planType", task.getPlanType());
                                                      item.put("taskType", task.getTaskType());
                                                      item.put("taskSubType", resolveTaskSubType(task));
                                                      item.put("userId", task.getUserId());
                                                      item.put("responsiblePerson", task.getResponsiblePerson());
                                                      item.put("responsiblePersonPhone", task.getResponsiblePersonPhone());
                                                      item.put("inspectionSuggestion", task.getInspectionSuggestion());
                                                      item.put("submitRequire", task.getSubmitRequire());
                                                      item.put("smsContent", task.getSmsContent());
                                                      item.put("updateDate", task.getUpdateDate() == null ? null : task.getUpdateDate().getTime());
                                                      return item;
                                                  })
                                                  .toList();
        return JacksonUtil.toJson(snapshot);
    }

    private TaskSmsGroupKey buildTaskSmsGroupKey(DzTaskDistList task) {
        return new TaskSmsGroupKey(
            task.getUserId(),
            task.getSourceType(),
            task.getPlanType(),
            resolveTaskType(task),
            resolveTaskSubType(task)
        );
    }

    private Map<String, TaskDistSmsPreviewItemVo> buildTaskPushPreviewItemMap(List<TaskDistSmsPreviewItemVo> previewItems) {
        if (ObjUtil.isEmpty(previewItems)) {
            return Map.of();
        }
        Map<String, TaskDistSmsPreviewItemVo> result = new LinkedHashMap<>();
        for (TaskDistSmsPreviewItemVo item : previewItems) {
            if (item == null || ObjUtil.isEmpty(item.getTaskIdList())) {
                continue;
            }
            result.put(buildTaskIdListKey(item.getTaskIdList()), item);
        }
        return result;
    }

    private String buildTaskIdListKey(List<Long> taskIds) {
        if (ObjUtil.isEmpty(taskIds)) {
            return "";
        }
        return taskIds.stream()
                      .filter(Objects::nonNull)
                      .sorted()
                      .map(String::valueOf)
                      .collect(Collectors.joining(","));
    }

    void sendTaskSmsAfterPush(List<DzTaskDistList> tasks) {
        sendTaskSmsAfterPush(tasks, List.of());
    }

    private void sendTaskSmsAfterPush(List<DzTaskDistList> tasks, List<TaskDistSmsPreviewItemVo> previewItems) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        populateMissingSmsContents(tasks);
        Map<String, TaskDistSmsPreviewItemVo> previewItemMap = buildTaskPushPreviewItemMap(previewItems);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<TaskSmsGroupKey, List<DzTaskDistList>> groupedTasks = tasks.stream()
                                                                       .filter(Objects::nonNull)
                                                                       .collect(Collectors.groupingBy(
                                                                           this::buildTaskSmsGroupKey,
                                                                           LinkedHashMap::new,
                                                                           Collectors.toList()
                                                                       ));
        for (Map.Entry<TaskSmsGroupKey, List<DzTaskDistList>> entry : groupedTasks.entrySet()) {
            TaskDistSmsPreviewItemVo previewItem = previewItemMap.get(buildTaskIdListKey(extractTaskIds(entry.getValue())));
            futures.add(CompletableFuture.runAsync(() -> sendGroupedTaskSms(entry.getValue(), previewItem), Run.executor));
        }
        waitForSmsTasks(futures);
        sendPatrolMonitorDirectorSmsAfterPush(tasks);
    }

    List<TaskDistSmsPreviewItemVo> buildPatrolMonitorDirectorPreviewItems(List<DzTaskDistList> tasks) {
        List<PatrolMonitorDirectorSmsGroup> groups = buildPatrolMonitorDirectorSmsGroups(tasks);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<TaskDistSmsPreviewItemVo> result = new ArrayList<>();
        for (PatrolMonitorDirectorSmsGroup group : groups) {
            String content;
            try {
                content = taskSmsContentService.generatePatrolMonitorDirectorGroupedSmsContent(group.tasks());
            } catch (Exception e) {
                log.error("生成巡查、监测任务所长聚合短信预览失败, phone: {}, taskIds: {}",
                    group.directorPhone(), extractTaskIds(group.tasks()), e);
                continue;
            }
            if (StringUtils.isBlank(content)) {
                log.warn("巡查、监测任务所长聚合短信预览跳过，正文为空, phone: {}, taskIds: {}",
                    group.directorPhone(), extractTaskIds(group.tasks()));
                continue;
            }
            TaskDistSmsPreviewItemVo item = new TaskDistSmsPreviewItemVo();
            item.setUserId(resolveDirectorUserId(group.directorPhone()));
            item.setNickName(group.directorName());
            item.setTaskIdList(extractTaskIds(group.tasks()));
            item.setSmsContent(content);
            result.add(item);
        }
        return result;
    }

    private Long resolveDirectorUserId(String phone) {
        if (StringUtils.isBlank(phone)) {
            return null;
        }
        try {
            SysUserVo user = sysUserService.selectUserByPhonenumber(phone);
            return user == null ? null : user.getUserId();
        } catch (Exception e) {
            log.warn("乡自规所所长手机号未能匹配系统用户，短信预览userId留空, phone: {}", phone, e);
            return null;
        }
    }

    private void sendPatrolMonitorDirectorSmsAfterPush(List<DzTaskDistList> tasks) {
        List<PatrolMonitorDirectorSmsGroup> groups;
        try {
            groups = buildPatrolMonitorDirectorSmsGroups(tasks);
        } catch (Exception e) {
            log.error("巡查、监测任务所长聚合短信接收人解析失败，跳过所长通知, taskIds: {}", extractTaskIds(tasks), e);
            return;
        }
        for (PatrolMonitorDirectorSmsGroup group : groups) {
            try {
                sendPatrolMonitorDirectorSms(group);
            } catch (Exception e) {
                log.error("巡查、监测任务所长聚合短信发送失败，跳过当前所长, phone: {}, taskIds: {}",
                    group.directorPhone(), extractTaskIds(group.tasks()), e);
            }
        }
    }

    private void sendPatrolMonitorDirectorSms(PatrolMonitorDirectorSmsGroup group) {
        if (group == null || ObjUtil.isEmpty(group.tasks()) || StringUtils.isBlank(group.directorPhone())) {
            return;
        }
        String content = taskSmsContentService.generatePatrolMonitorDirectorGroupedSmsContent(group.tasks());
        if (StringUtils.isBlank(content)) {
            log.warn("巡查、监测任务所长聚合短信跳过，正文为空, phone: {}, taskIds: {}",
                group.directorPhone(), extractTaskIds(group.tasks()));
            return;
        }
        DzTaskDistList firstTask = group.tasks().getFirst();
        sendSms(new SmsSendParams(List.of(group.directorPhone()), content,
            DAILY_PATROL_DIRECTOR_SMS_TYPE, firstTask.getSourceType(),
            firstTask.getDefId(), firstTask.getId(), firstTask.getHandleId(),
            group.directorName(), SmsScene.TASK_PUSH));
    }

    private List<PatrolMonitorDirectorSmsGroup> buildPatrolMonitorDirectorSmsGroups(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        List<DzTaskDistList> supervisedTasks = tasks.stream()
            .filter(DzPushSmsDelegate::isDirectorSupervisedTask)
            .toList();
        if (supervisedTasks.isEmpty()) {
            return List.of();
        }
        List<String> unitIds = supervisedTasks.stream()
            .map(DzTaskDistList::getUnitId)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
        List<SlopeUnitGridMemberRelationVo> relations = slopeUnitGridMemberRelationService.queryByUnitIds(unitIds);
        Map<String, SlopeUnitGridMemberRelationVo> relationMap = relations == null
            ? Map.of()
            : relations.stream()
                .filter(Objects::nonNull)
                .filter(relation -> StringUtils.isNotBlank(relation.getUnitId()))
                .collect(Collectors.toMap(
                    relation -> relation.getUnitId().trim(),
                    relation -> relation,
                    (left, right) -> left,
                    LinkedHashMap::new
                ));
        Map<String, List<DzTaskDistList>> tasksByPhone = new LinkedHashMap<>();
        Map<String, String> directorNamesByPhone = new LinkedHashMap<>();
        for (DzTaskDistList task : supervisedTasks) {
            String unitId = StringUtils.isBlank(task.getUnitId()) ? null : task.getUnitId().trim();
            SlopeUnitGridMemberRelationVo relation = unitId == null ? null : relationMap.get(unitId);
            if (relation == null) {
                log.warn("巡查、监测任务所长通知跳过，未找到斜坡单元人员关系, taskId: {}, unitId: {}",
                    task.getId(), task.getUnitId());
                continue;
            }
            String directorPhone = StringUtils.isBlank(relation.getAdminUserPhone())
                ? null
                : relation.getAdminUserPhone().trim();
            if (directorPhone == null) {
                log.warn("巡查、监测任务所长通知跳过，所长手机号为空, taskId: {}, unitId: {}",
                    task.getId(), task.getUnitId());
                continue;
            }
            tasksByPhone.computeIfAbsent(directorPhone, key -> new ArrayList<>()).add(task);
            String directorName = firstNonBlank(relation.getAdminUser());
            if (StringUtils.isNotBlank(directorName)) {
                directorNamesByPhone.putIfAbsent(directorPhone, directorName);
            }
        }
        return tasksByPhone.entrySet().stream()
            .map(entry -> new PatrolMonitorDirectorSmsGroup(
                firstNonBlank(directorNamesByPhone.get(entry.getKey()), SysRoleEnum.DZ_ZGSSZ.getRoleName()),
                entry.getKey(), entry.getValue()))
            .toList();
    }

    static boolean isDailyPatrolTask(DzTaskDistList task) {
        return task != null
            && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)
            && (Objects.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                || Objects.equals(task.getTaskSource(), DzTaskDistList.PLAN_NAME_DAILY_PATROL));
    }

    static boolean isDirectorSupervisedTask(DzTaskDistList task) {
        if (isDailyPatrolTask(task)) {
            return true;
        }
        if (task == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return false;
        }
        return matchesPlanName(task, DzTaskDistList.PLAN_NAME_PATROL)
            || matchesPlanName(task, DzTaskDistList.PLAN_NAME_MONITOR);
    }

    private static boolean matchesPlanName(DzTaskDistList task, String planName) {
        return Objects.equals(task.getPlanName(), planName) || Objects.equals(task.getTaskSource(), planName);
    }

    private void sendGroupedTaskSms(List<DzTaskDistList> tasks, TaskDistSmsPreviewItemVo previewItem) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        List<String> phones = tasks.stream()
                                   .map(DzTaskDistList::getResponsiblePersonPhone)
                                   .filter(StringUtils::isNotBlank)
                                   .map(String::trim)
                                   .distinct()
                                   .toList();
        if (phones.isEmpty()) {
            log.warn("任务推送后聚合短信跳过，手机号为空, taskIds: {}", extractTaskIds(tasks));
            return;
        }
        String content = previewItem == null ? null : previewItem.getSmsContent();
        if (StringUtils.isBlank(content)) {
            try {
                content = taskSmsContentService.generateGroupedSmsContent(tasks);
            } catch (Exception e) {
                log.error("任务推送后生成聚合短信失败, taskIds: {}", extractTaskIds(tasks), e);
                return;
            }
        }
        if (StringUtils.isBlank(content)) {
            log.warn("任务推送后聚合短信跳过，正文为空, taskIds: {}", extractTaskIds(tasks));
            return;
        }
        overwriteTaskSmsSnapshot(tasks, content);
        DzTaskDistList firstTask = tasks.getFirst();
        sendSms(new SmsSendParams(phones, content,
            "task_grouped_" + firstTask.getSourceType(), firstTask.getSourceType(),
            firstTask.getDefId(), firstTask.getId(), firstTask.getHandleId(),
            firstTask.getResponsiblePerson(), SmsScene.TASK_PUSH));
    }

    private void waitForSmsTasks(List<CompletableFuture<Void>> futures) {
        if (ObjUtil.isEmpty(futures)) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("并行发送短信失败: " + cause.getMessage());
        }
    }

     List<Long> extractTaskIds(List<DzTaskDistList> tasks) {
        return tasks.stream()
                    .filter(Objects::nonNull)
                    .map(DzTaskDistList::getId)
                    .filter(Objects::nonNull)
                    .toList();
    }
    private DzDefRespStartSmsDelegate defRespStartSmsDelegate;

    private DzDefRespStartSmsDelegate defRespStartSmsDelegate() {
        if (defRespStartSmsDelegate == null) {
            defRespStartSmsDelegate = new DzDefRespStartSmsDelegate(service, this);
        }
        return defRespStartSmsDelegate;
    }

    public List<DefRespStartSmsPreviewVo> previewDefRespStartSms(Long defId) {
        return defRespStartSmsDelegate().previewDefRespStartSms(defId);
    }

    List<DefRespStartSmsPreviewVo> previewDefRespLifecycleSms(DefRespSmsEventSnapshot event) {
        return defRespStartSmsDelegate().previewDefRespLifecycleSms(event);
    }

    SmsSendSummaryVo sendDefRespLifecycleSms(DefRespSmsEventSnapshot event, boolean manual) {
        return defRespStartSmsDelegate().sendDefRespLifecycleSms(event, manual);
    }

    SmsSendSummaryVo sendDefRespSmsDirect(Long defId, Date referenceDate) {
        return defRespStartSmsDelegate().sendDefRespSmsDirect(defId, referenceDate);
    }


     String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    record SmsSendParams(List<String> phones, String content, String smsType,
                                   Integer sourceType, Long defId, Long taskId,
                                   Long handleId, String receiverName, SmsScene scene) {}

    EvacuationSmsResult sendSms(SmsSendParams p) {
        List<String> validPhones = p.phones() == null ? List.of() : p.phones().stream()
            .filter(StringUtils::isNotBlank).map(String::trim).distinct().toList();
        if (validPhones.isEmpty() || StringUtils.isBlank(p.content())) {
            return EvacuationSmsResult.builder().build();
        }
        SmsSendContext context = SmsSendContext.builder()
            .scene(p.scene())
            .taskSourceType(p.scene() == SmsScene.TASK_PUSH ? p.sourceType() : null)
            .bizType(resolveSmsBizType(p.sourceType(), p.defId(), p.taskId(), p.handleId()))
            .bizId(resolveSmsBizId(p.defId(), p.taskId(), p.handleId()))
            .smsType(p.smsType()).receiverName(p.receiverName()).targetType("PERSON").build();
        try {
            return smsSendService.send(p.content(), validPhones, context);
        } catch (Exception e) {
            log.error("发送短信失败, phones: {}, smsType: {}", validPhones, p.smsType(), e);
            return EvacuationSmsResult.builder().build();
        }
    }

    private Integer resolveSmsBizType(Integer sourceType, Long defId, Long taskId, Long handleId) {
        if (taskId != null) {
            return DzSmsSendBatch.BIZ_TYPE_TASK_PUSH;
        }
        if (defId != null || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return DzSmsSendBatch.BIZ_TYPE_DEF_RESP;
        }
        if (handleId != null) {
            return DzSmsSendBatch.BIZ_TYPE_EVACUATION_SMS;
        }
        return null;
    }

    private Long resolveSmsBizId(Long defId, Long taskId, Long handleId) {
        if (taskId != null) {
            return taskId;
        }
        if (defId != null) {
            return defId;
        }
        return handleId;
    }

     void populateMissingSmsContents(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        Date now = new Date();
        List<DzTaskDistList> updates = new ArrayList<>();
        for (DzTaskDistList task : tasks) {
            if (task == null || StringUtils.isNotBlank(task.getSmsContent())) {
                continue;
            }
            try {
                String content = taskSmsContentService.populateSmsContent(task);
                if (StringUtils.isBlank(content) || task.getId() == null) {
                    continue;
                }
                DzTaskDistList update = new DzTaskDistList();
                update.setId(task.getId());
                update.setSmsContent(content);
                update.setUpdateDate(now);
                updates.add(update);
            } catch (Exception e) {
                log.error("补生成任务短信正文失败, taskId: {}", task.getId(), e);
            }
        }
        if (updates.isEmpty()) {
            return;
        }
        try {
            baseMapper.updateBatchById(updates);
        } catch (Exception e) {
            log.error("批量回写任务短信正文失败, size: {}", updates.size(), e);
        }
    }

     void overwriteTaskSmsSnapshot(List<DzTaskDistList> tasks, String content) {
        if (ObjUtil.isEmpty(tasks) || StringUtils.isBlank(content)) {
            return;
        }
        Date now = new Date();
        List<DzTaskDistList> updates = new ArrayList<>();
        for (DzTaskDistList task : tasks) {
            if (task == null || task.getId() == null) {
                continue;
            }
            task.setSmsContent(content);
            DzTaskDistList update = new DzTaskDistList();
            update.setId(task.getId());
            update.setSmsContent(content);
            update.setUpdateDate(now);
            updates.add(update);
        }
        if (updates.isEmpty()) {
            return;
        }
        try {
            baseMapper.updateBatchById(updates);
        } catch (Exception e) {
            log.error("批量覆盖任务短信快照失败, taskIds: {}", extractTaskIds(tasks), e);
        }
    }

     static String resolveTaskSubType(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)
            && DzTaskDistList.isMonitoringPlanType(task.getPlanType())) {
            if (Objects.equals(task.getPlanType(), DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING)) {
                return "仪器监测";
            }
            if (Objects.equals(task.getPlanType(), DzTaskDistList.PLAN_TYPE_MONITORING)) {
                return "群测群防巡查";
            }
            String monitoringSubType = resolveMonitoringTaskSubType(task);
            if (StringUtils.isNotBlank(monitoringSubType)) {
                return monitoringSubType;
            }
        }
        PlanTypeEnum planTypeEnum = PlanTypeEnum.getByCode(task.getPlanType());
        if (planTypeEnum != null) {
            return planTypeEnum.getName();
        }
        return null;
    }

     static String resolveMonitoringTaskSubType(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        String responsibleName = firstNonBlankStatic(task.getResponsiblePerson(), "");
        String content = firstNonBlankStatic(task.getInspectionSuggestion(), task.getTaskSource(), "");
        if (responsibleName.contains("自规") || responsibleName.contains("所长")) {
            return "仪器监测";
        }
        if (responsibleName.contains("巡查")) {
            return "群测群防巡查";
        }
        boolean hasMassPrevention = content.contains("群测群防");
        boolean hasInstrument = content.contains("仪器监测");
        if (hasMassPrevention && !hasInstrument) {
            return "群测群防巡查";
        }
        if (hasInstrument && !hasMassPrevention) {
            return "仪器监测";
        }
        return null;
    }

     private static String firstNonBlankStatic(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

     String resolveTaskType(DzTaskDistList task) {
        if (task == null) {
            return DzTaskDistList.TASK_TYPE_INSPECTION;
        }
        if (StringUtils.isNotBlank(task.getTaskType())) {
            return task.getTaskType();
        }
        DzReportDisaster report = loadTaskTypeReport(task);
        return DzTaskDistListServiceImpl.resolveStoredTaskType(task.getSourceType(), report);
    }

    static List<String> resolvePushRoles(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        return tasks.stream()
                    .map(DzPushSmsDelegate::resolvePushRole)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toList();
    }

    static List<String> resolvePushRoles(List<DzTaskDistList> tasks, List<SysUserVo> users) {
        return resolvePushRoles(tasks, users, Map.of());
    }

    static List<String> resolvePushRoles(List<DzTaskDistList> tasks, List<SysUserVo> users,
                                         Map<Long, String> userRoleNameMap) {
        if (ObjUtil.isEmpty(tasks) || ObjUtil.isEmpty(users)) {
            return List.of();
        }
        Map<Long, String> roleNameMap = userRoleNameMap == null ? Map.of() : userRoleNameMap;
        Map<Long, List<DzTaskDistList>> taskMap = tasks.stream()
                                                       .filter(Objects::nonNull)
                                                       .filter(task -> task.getUserId() != null)
                                                       .collect(Collectors.groupingBy(
                                                           DzTaskDistList::getUserId,
                                                           LinkedHashMap::new,
                                                           Collectors.toList()
                                                       ));
        return users.stream()
                    .filter(Objects::nonNull)
                    .map(SysUserVo::getUserId)
                    .map(userId -> resolveUserPushRole(taskMap.get(userId), roleNameMap.get(userId)))
                    .toList();
    }

    /**
     * 按任务责任人顺序生成推送展示名称；系统用户不存在时使用任务责任人兜底。
     */
    static List<String> resolvePushNames(List<DzTaskDistList> tasks, List<Long> userIds,
                                         Map<Long, SysUserVo> userMap) {
        if (ObjUtil.isEmpty(userIds)) {
            return List.of();
        }
        Map<Long, List<DzTaskDistList>> taskMap = groupTasksByUserId(tasks);
        Map<Long, SysUserVo> users = userMap == null ? Map.of() : userMap;
        return userIds.stream()
                      .map(userId -> {
                          SysUserVo user = users.get(userId);
                          if (user != null) {
                              return user.getNickName();
                          }
                          return firstResponsiblePerson(taskMap.get(userId));
                      })
                      .toList();
    }

    /**
     * 按任务责任人顺序生成推送角色；系统用户不存在时使用任务类型推导角色兜底。
     */
    static List<String> resolvePushRoles(List<DzTaskDistList> tasks, List<Long> userIds,
                                         Map<Long, SysUserVo> userMap,
                                         Map<Long, String> userRoleNameMap) {
        if (ObjUtil.isEmpty(userIds)) {
            return List.of();
        }
        Map<Long, List<DzTaskDistList>> taskMap = groupTasksByUserId(tasks);
        Map<Long, SysUserVo> users = userMap == null ? Map.of() : userMap;
        Map<Long, String> roleNameMap = userRoleNameMap == null ? Map.of() : userRoleNameMap;
        return userIds.stream()
                      .map(userId -> {
                          List<DzTaskDistList> userTasks = taskMap.get(userId);
                          SysUserVo user = users.get(userId);
                          if (user != null) {
                              return resolveUserPushRole(userTasks, roleNameMap.get(userId));
                          }
                          return resolveUserPushRole(userTasks);
                      })
                      .toList();
    }

    private static Map<Long, List<DzTaskDistList>> groupTasksByUserId(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return Map.of();
        }
        return tasks.stream()
                    .filter(Objects::nonNull)
                    .filter(task -> task.getUserId() != null)
                    .collect(Collectors.groupingBy(
                        DzTaskDistList::getUserId,
                        LinkedHashMap::new,
                        Collectors.toList()
                    ));
    }

    private static String firstResponsiblePerson(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return "";
        }
        return tasks.stream()
                    .map(DzTaskDistList::getResponsiblePerson)
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .findFirst()
                    .orElse("");
    }

    private Map<Long, String> resolveUserRoleNames(List<Long> userIds) {
        if (ObjUtil.isEmpty(userIds)) {
            return Map.of();
        }
        try {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));
            if (ObjUtil.isEmpty(userRoles)) {
                return Map.of();
            }
            List<Long> roleIds = userRoles.stream()
                                          .map(SysUserRole::getRoleId)
                                          .filter(Objects::nonNull)
                                          .distinct()
                                          .toList();
            if (roleIds.isEmpty()) {
                return Map.of();
            }
            List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getRoleId, roleIds));
            if (ObjUtil.isEmpty(roles)) {
                return Map.of();
            }
            Map<Long, String> roleNameMap = roles.stream()
                                                 .filter(Objects::nonNull)
                                                 .filter(role -> role.getRoleId() != null)
                                                 .filter(role -> StringUtils.isNotBlank(role.getRoleName()))
                                                 .collect(Collectors.toMap(
                                                     SysRole::getRoleId,
                                                     role -> role.getRoleName().trim(),
                                                     (existing, replacement) -> existing,
                                                     LinkedHashMap::new
                                                 ));
            Map<Long, List<String>> roleNamesByUserId = new LinkedHashMap<>();
            for (SysUserRole userRole : userRoles) {
                if (userRole == null || userRole.getUserId() == null || userRole.getRoleId() == null) {
                    continue;
                }
                String roleName = roleNameMap.get(userRole.getRoleId());
                if (StringUtils.isBlank(roleName)) {
                    continue;
                }
                List<String> roleNames = roleNamesByUserId.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>());
                if (!roleNames.contains(roleName)) {
                    roleNames.add(roleName);
                }
            }
            Map<Long, String> result = new LinkedHashMap<>();
            userIds.stream()
                   .filter(Objects::nonNull)
                   .distinct()
                   .forEach(userId -> {
                       List<String> roleNames = roleNamesByUserId.get(userId);
                       if (!ObjUtil.isEmpty(roleNames)) {
                           result.put(userId, String.join(",", roleNames));
                       }
                   });
            return result;
        } catch (Exception ex) {
            log.warn("推送返回用户角色查询失败, userIds={}", userIds, ex);
            return Map.of();
        }
    }

    private static String resolveUserPushRole(List<DzTaskDistList> tasks, String userRoleName) {
        if (StringUtils.isNotBlank(userRoleName)) {
            return userRoleName.trim();
        }
        return resolveUserPushRole(tasks);
    }

    private static String resolveUserPushRole(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return "";
        }
        return tasks.stream()
                    .map(DzPushSmsDelegate::resolvePushRole)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("/"));
    }

    static String resolvePushRole(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        String taskType = StringUtils.isNotBlank(task.getTaskType())
            ? task.getTaskType().trim()
            : DzTaskDistListServiceImpl.resolveStoredTaskType(task.getSourceType());
        Integer sourceType = task.getSourceType();
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)
            || Objects.equals(taskType, DzTaskDistList.TASK_TYPE_MONITOR_WARNING)) {
            return SysRoleEnum.DZ_YHDJCY.getRoleName();
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)
            || Objects.equals(taskType, DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION)) {
            return SysRoleEnum.DZ_XGY.getRoleName();
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT)) {
            if (Objects.equals(taskType, DzTaskDistList.TASK_TYPE_AI_VERIFY)) {
                return SysRoleEnum.DZ_ZGSSZ.getRoleName();
            }
            return SysRoleEnum.DZ_FXQXCY.getRoleName();
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return resolveDefRespPushRole(task);
        }
        return SysRoleEnum.DZ_FXQXCY.getRoleName();
    }

    private static String resolveDefRespPushRole(DzTaskDistList task) {
        if (task == null) {
            return SysRoleEnum.DZ_FXQXCY.getRoleName();
        }
        if (Objects.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_MONITOR)
            || Objects.equals(task.getTaskSource(), DzTaskDistList.PLAN_NAME_MONITOR)) {
            return SysRoleEnum.DZ_YHDJCY.getRoleName();
        }
        return SysRoleEnum.DZ_FXQXCY.getRoleName();
    }

    static String resolveTaskTypeBySourceType(Integer sourceType) {
        return resolveTaskTypeBySourceType(sourceType, null);
    }

    static String resolveTaskTypeBySourceType(Integer sourceType, DzReportDisaster report) {
        return DzTaskDistListServiceImpl.resolveStoredTaskType(sourceType, report);
    }

    private DzReportDisaster loadTaskTypeReport(DzTaskDistList task) {
        if (task == null || task.getReportId() == null) {
            return null;
        }
        return dzReportDisasterMapper.selectById(task.getReportId());
    }

     String buildPushDetailedAddress(SlopeUnit slopeUnit, String originalDetailedAddress) {
        String slopeUnitDesc = buildSlopeUnitDisplayName(slopeUnit);
        return slopeUnitDesc + System.lineSeparator() + originalDetailedAddress;
    }

     String buildSlopeUnitDisplayName(SlopeUnit slopeUnit) {
        if (slopeUnit == null) {
            return null;
        }
        String unitNo = StringUtils.isNotBlank(slopeUnit.getId()) ? slopeUnit.getId() + "号斜坡单元" : null;
        return Stream.of(
                         slopeUnit.getStreet(),
                         slopeUnit.getVillage(),
                         slopeUnit.getCommunity(),
                         unitNo
                     )
                     .filter(StringUtils::isNotBlank)
                     .collect(Collectors.joining());
    }

     void populateTaskDetailedAddress(DzTaskDistList task) {
        if (task == null || StringUtils.isNotBlank(task.getDetailedAddress())) {
            return;
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY) && task.getHandleId() != null) {
            DzTaskHandle taskHandle = dzTaskHandleMapper.selectById(task.getHandleId());
            if (taskHandle != null && StringUtils.isNotBlank(taskHandle.getDetailedAddress())) {
                task.setDetailedAddress(taskHandle.getDetailedAddress());
                return;
            }
        }
        if (StringUtils.isBlank(task.getUnitId())) {
            return;
        }
        SlopeUnit slopeUnit = resolveSlopeUnitByTaskUnitId(task.getUnitId());
        if (slopeUnit != null && StringUtils.isNotBlank(slopeUnit.getDetailedAddress())) {
            task.setDetailedAddress(slopeUnit.getDetailedAddress());
        }
    }

     SlopeUnit resolveSlopeUnitByTaskUnitId(String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return null;
        }
        String normalizedUnitId = normalizeSlopeUnitId(unitId);
        List<String> ids = Objects.equals(unitId, normalizedUnitId)
            ? List.of(unitId)
            : List.of(unitId, normalizedUnitId);
        return slopeUnitService.listPoByIds(ids)
                               .stream()
                               .filter(Objects::nonNull)
                               .findFirst()
                               .orElse(null);
    }

}
