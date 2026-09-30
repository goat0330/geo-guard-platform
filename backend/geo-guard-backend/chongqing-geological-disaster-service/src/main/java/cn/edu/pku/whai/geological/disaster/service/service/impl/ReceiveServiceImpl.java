/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallLogSlopeUnitStatisticService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.*;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AlgorithmReceiveBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.*;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReceiveHandleProcessVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.*;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReceiveServiceImpl implements IReceiveService {
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final ISlopeUnitService slopeUnitService;
    private final IRainfallLogSlopeUnitStatisticService rainfallLogSlopeUnitStatisticService;
    private final ISysUserService sysUserService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final DzTaskHandleSceneRecordMapper dzTaskHandleSceneRecordMapper;
    private final DzTaskHandleDetailContentMapper dzTaskHandleDetailContentMapper;
    private final DzTaskHandleApprovalMapper dzTaskHandleApprovalMapper;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final IDzTaskDistListService dzTaskDistListService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer risk(AlgorithmReceiveBo bo) {
        if (bo == null || bo.getRiskIds() == null || bo.getRiskIds().isEmpty()) {
            return 0;
        }
        return dzTaskDistListService.createDailyPatrolTasksByRiskIds(bo.getRiskIds());
    }

    @Override
    public ReceiveHandleProcessVo queryHandleProcess(Long handleId) {
        DzTaskHandle taskHandle = dzTaskHandleMapper.selectById(handleId);
        if (taskHandle == null) {
            throw new ServiceException("未找到handleId对应的处置任务");
        }

        DzTaskHandleSceneRecord sceneRecord = dzTaskHandleSceneRecordMapper.selectOne(
            Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                    .eq(DzTaskHandleSceneRecord::getHandleId, handleId)
                    .last("limit 1")
        );
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectOne(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getHandleId, handleId)
                    .eq(DefRespPlan::getDeleted, 0)
                    .orderByDesc(DefRespPlan::getCreateDate, DefRespPlan::getId)
                    .last("limit 1")
        );

        Date referenceTime = firstNonNull(
            sceneRecord == null ? null : sceneRecord.getOccurrenceTime(),
            taskHandle.getReporterDate(),
            taskHandle.getCreateDate()
        );
        LocalDate endDate = toLocalDate(referenceTime);
        LocalDate startDate = endDate.minusDays(2);

        ReceiveHandleProcessVo vo = new ReceiveHandleProcessVo();
        vo.setHandleId(handleId);
        vo.setWorkflow(buildWorkflowInfo(handleId, taskHandle, sceneRecord));
        vo.setTriggerCondition(buildTriggerConditionInfo(taskHandle, startDate, endDate));
        vo.setInvestigation(buildInvestigationInfo(taskHandle, sceneRecord));
        vo.setExpertConsultation(buildExpertConsultationInfo(handleId));
        vo.setDefenseResponse(buildDefenseResponseInfo(taskHandle, handleId, defRespPlan));
        vo.setDefenseReview(buildDefenseReviewInfo(handleId, defRespPlan));
        vo.setTaskDispatch(buildTaskDispatchInfo(handleId));
        vo.setResponseExecution(buildResponseExecutionInfo(handleId));
        vo.setArchive(buildArchiveInfo(taskHandle));
        return vo;
    }

    private ReceiveHandleProcessVo.WorkflowInfo buildWorkflowInfo(Long handleId, DzTaskHandle taskHandle, DzTaskHandleSceneRecord sceneRecord) {
        ReceiveHandleProcessVo.WorkflowInfo workflow = new ReceiveHandleProcessVo.WorkflowInfo();
        workflow.setOccurrenceTime(firstNonNull(
            sceneRecord == null ? null : sceneRecord.getOccurrenceTime(),
            taskHandle.getReporterDate(),
            taskHandle.getCreateDate()
        ));
        workflow.setName(resolveEventName(taskHandle, sceneRecord));
        workflow.setCode(String.valueOf(handleId));
        return workflow;
    }

    private ReceiveHandleProcessVo.TriggerConditionInfo buildTriggerConditionInfo(DzTaskHandle taskHandle, LocalDate startDate, LocalDate endDate) {
        ReceiveHandleProcessVo.TriggerConditionInfo info = new ReceiveHandleProcessVo.TriggerConditionInfo();
        info.setSlopeUnitId(taskHandle.getSlopeUnitId());
        info.setRecentRainfalls(queryRecentRainfalls(taskHandle.getSlopeUnitId(), startDate, endDate));
        info.setRecentRiskLevels(queryRecentRiskLevels(taskHandle.getSlopeUnitId(), startDate, endDate));
        info.setRecentDisasterReports(queryRecentDisasterReports(taskHandle.getSlopeUnitId(), startDate, endDate));
        return info;
    }

    private ReceiveHandleProcessVo.InvestigationInfo buildInvestigationInfo(DzTaskHandle taskHandle, DzTaskHandleSceneRecord sceneRecord) {
        ReceiveHandleProcessVo.InvestigationInfo info = new ReceiveHandleProcessVo.InvestigationInfo();
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(taskHandle.getSlopeUnitId());
        if (relationVo != null) {
            info.setAssistantManager(relationVo.getAssistantManager());
        }
        info.setInvestigationFinishTime(firstNonNull(
            sceneRecord == null ? null : sceneRecord.getUpdateDate(),
            sceneRecord == null ? null : sceneRecord.getCreateDate()
        ));

        List<DzTaskHandleDetailContent> reportContents = listTaskHandleContents(taskHandle.getId(), DetailContentTypeEnum.AI_REPORT.getCode());
        DzTaskHandleDetailContent earliest = reportContents.isEmpty() ? null : reportContents.getLast();
        info.setInitialReportGenerateTime(earliest == null ? null : earliest.getCreateDate());
        info.setInitialReportContent(earliest == null ? null : earliest.getPlanContent());
        return info;
    }

    private ReceiveHandleProcessVo.ExpertConsultationInfo buildExpertConsultationInfo(Long handleId) {
        ReceiveHandleProcessVo.ExpertConsultationInfo info = new ReceiveHandleProcessVo.ExpertConsultationInfo();
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalMapper.selectListStatusType1(handleId, null)
                                                                           .stream()
                                                                           .filter(item -> Objects.equals(item.getProcess(), HandleProcessEnum.CONSULTATION_JUDGMENT.getCode()))
                                                                           .toList();
        info.setExpertNames(approvals.stream()
                                     .map(DzTaskHandleApprovalVo::getNickname)
                                     .filter(StringUtils::isNotBlank)
                                     .distinct()
                                     .toList());
        info.setConsultationTime(approvals.stream()
                                          .map(DzTaskHandleApprovalVo::getCreateDate)
                                          .filter(Objects::nonNull)
                                          .max(Date::compareTo)
                                          .orElse(null));

        DzTaskHandleDetailContent latest = queryLatestDetailContent(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.AI_REPORT.getCode()
        );
        info.setDutyOfficer(resolveUserDisplayName(latest == null ? null : latest.getUserId()));
        info.setExpertOpinion(latest == null ? null : latest.getSuggest());
        info.setFinalReportContent(latest == null ? null : latest.getPlanContent());
        return info;
    }

    private ReceiveHandleProcessVo.DefenseResponseInfo buildDefenseResponseInfo(DzTaskHandle taskHandle, Long handleId, DefRespPlan defRespPlan) {
        ReceiveHandleProcessVo.DefenseResponseInfo info = new ReceiveHandleProcessVo.DefenseResponseInfo();
        info.setEventLevel(resolveEventLevelName(taskHandle.getEventLevel()));
        info.setResponseStartTime(firstNonNull(
            defRespPlan == null ? null : defRespPlan.getCreateDate(),
            queryEarliestTaskDispatchTime(handleId)
        ));
        info.setResponseLevel(firstNonNull(
            resolveResponseLevelName(defRespPlan == null ? null : defRespPlan.getLevel()),
            resolveResponseLevelName(taskHandle.getRespStatus())
        ));

        DzTaskHandleDetailContent initialPlan = queryInitialPlanContent(defRespPlan, handleId);
        info.setInitialPlanContent(initialPlan == null ? null : initialPlan.getPlanContent());
        return info;
    }

    private ReceiveHandleProcessVo.DefenseReviewInfo buildDefenseReviewInfo(Long handleId, DefRespPlan defRespPlan) {
        ReceiveHandleProcessVo.DefenseReviewInfo info = new ReceiveHandleProcessVo.DefenseReviewInfo();
        Integer roundNo = defRespPlan == null ? null : defRespPlan.getCurrentRoundNo();
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalMapper.selectListStatusType1(handleId, roundNo)
                                                                           .stream()
                                                                           .filter(item -> Objects.equals(item.getProcess(), HandleProcessEnum.PLAN_IMPLEMENTATION.getCode()))
                                                                           .toList();
        info.setExpertNames(approvals.stream()
                                     .map(DzTaskHandleApprovalVo::getNickname)
                                     .filter(StringUtils::isNotBlank)
                                     .distinct()
                                     .toList());
        info.setConsultationTime(approvals.stream()
                                          .map(DzTaskHandleApprovalVo::getCreateDate)
                                          .filter(Objects::nonNull)
                                          .max(Date::compareTo)
                                          .orElse(null));

        DzTaskHandleDetailContent finalPlan = queryFinalPlanContent(defRespPlan, handleId);
        DzTaskHandleDetailContent initialPlan = queryInitialPlanContent(defRespPlan, handleId);
        DzTaskHandleDetailContent dutySource = finalPlan != null ? finalPlan : initialPlan;
        info.setDutyOfficer(resolveUserDisplayName(dutySource == null ? null : dutySource.getUserId()));
        info.setExpertOpinion(firstNonNull(
            finalPlan == null ? null : finalPlan.getSuggest(),
            initialPlan == null ? null : initialPlan.getSuggest()
        ));
        info.setFinalPlanContent(finalPlan == null ? null : finalPlan.getPlanContent());
        return info;
    }

    private ReceiveHandleProcessVo.TaskDispatchInfo buildTaskDispatchInfo(Long handleId) {
        ReceiveHandleProcessVo.TaskDispatchInfo info = new ReceiveHandleProcessVo.TaskDispatchInfo();
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalMapper.selectListStatusType2(handleId, null)
                                                                           .stream()
                                                                           .filter(item -> Objects.equals(item.getProcess(), HandleProcessEnum.PLAN_IMPLEMENTATION.getCode()))
                                                                           .toList();
        info.setApprovers(approvals.stream()
                                   .map(item -> joinRoleAndName(item.getRoleName(), item.getNickname()))
                                   .filter(StringUtils::isNotBlank)
                                   .distinct()
                                   .toList());

        List<DzTaskDistListVo> tasks = dzTaskDistListMapper.selectVoList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getHandleId, handleId)
                    .eq(DzTaskDistList::getDelete, 0)
                    .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
        );
        info.setDispatchTime(tasks.stream()
                                  .map(DzTaskDistListVo::getCreateDate)
                                  .filter(Objects::nonNull)
                                  .min(Date::compareTo)
                                  .orElse(null));
        info.setTaskExecutors(tasks.stream()
                                   .map(item -> {
                                       ReceiveHandleProcessVo.TaskExecutorItem executorItem = new ReceiveHandleProcessVo.TaskExecutorItem();
                                       executorItem.setTaskName(item.getPlanName());
                                       executorItem.setExecutor(item.getResponsiblePerson());
                                       return executorItem;
                                   })
                                   .toList());
        return info;
    }

    private ReceiveHandleProcessVo.ResponseExecutionInfo buildResponseExecutionInfo(Long handleId) {
        ReceiveHandleProcessVo.ResponseExecutionInfo info = new ReceiveHandleProcessVo.ResponseExecutionInfo();
        List<DzTaskDistListVo> tasks = dzTaskDistListMapper.selectVoList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getHandleId, handleId)
                    .eq(DzTaskDistList::getDelete, 0)
                    .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
        );
        Map<String, Date> closeTimeMap = new LinkedHashMap<>();
        for (DzTaskDistListVo task : tasks) {
            if (StringUtils.isBlank(task.getPlanName()) || task.getClosedTime() == null) {
                continue;
            }
            Date current = closeTimeMap.get(task.getPlanName());
            if (current == null || task.getClosedTime().after(current)) {
                closeTimeMap.put(task.getPlanName(), task.getClosedTime());
            }
        }
        List<ReceiveHandleProcessVo.TaskCloseItem> taskCloseItems = new ArrayList<>();
        for (Map.Entry<String, Date> entry : closeTimeMap.entrySet()) {
            ReceiveHandleProcessVo.TaskCloseItem item = new ReceiveHandleProcessVo.TaskCloseItem();
            item.setTaskName(entry.getKey());
            item.setClosedTime(entry.getValue());
            taskCloseItems.add(item);
        }
        info.setTaskCloseTimes(taskCloseItems);
        boolean allClosed = !tasks.isEmpty() && tasks.stream().allMatch(task -> task.getClosedTime() != null);
        info.setAllTasksClosedTime(allClosed
            ? tasks.stream().map(DzTaskDistListVo::getClosedTime).max(Date::compareTo).orElse(null)
            : null);
        return info;
    }

    private ReceiveHandleProcessVo.ArchiveInfo buildArchiveInfo(DzTaskHandle taskHandle) {
        ReceiveHandleProcessVo.ArchiveInfo info = new ReceiveHandleProcessVo.ArchiveInfo();
        if (taskHandle.getHandleProcess() != null
            && taskHandle.getHandleProcess() >= HandleProcessEnum.CLOSED_ARCHIVED.getCode()) {
            info.setFinishResponseTime(taskHandle.getUpdateDate());
        }
        return info;
    }

    private List<ReceiveHandleProcessVo.RainfallItem> queryRecentRainfalls(String slopeUnitId, LocalDate startDate, LocalDate endDate) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        RainfallLogSlopeUnitStatisticQueryBo bo = new RainfallLogSlopeUnitStatisticQueryBo();
        bo.setSlopeUnitId(slopeUnitId);
        bo.setStartDate(startDate);
        bo.setEndDate(endDate);
        List<RainfallLogSlopeUnitStatisticVo> list = rainfallLogSlopeUnitStatisticService
            .queryPageListBySlopeUnit(bo)
            .getActualStatistics();
        if (list == null) {
            return List.of();
        }
        return list.stream()
                   .map(item -> {
                       ReceiveHandleProcessVo.RainfallItem rainfallItem = new ReceiveHandleProcessVo.RainfallItem();
                       rainfallItem.setStatDate(item.getStatDate() == null ? null : item.getStatDate().toString());
                       rainfallItem.setHourlyRainfall(item.getHourlyRainfall());
                       rainfallItem.setDailyCumulativeRainfall(item.getDailyCumulativeRainfall());
                       return rainfallItem;
                   })
                   .toList();
    }

    private List<ReceiveHandleProcessVo.RiskLevelItem> queryRecentRiskLevels(String slopeUnitId, LocalDate startDate, LocalDate endDate) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        Date startTime = toDate(startDate);
        Date endTime = toDate(endDate.plusDays(1));
        List<DzRiskAssessment> riskAssessments = dzRiskAssessmentMapper.selectList(
            Wrappers.<DzRiskAssessment>lambdaQuery()
                    .eq(DzRiskAssessment::getSlopeUnitId, slopeUnitId)
                    .ge(DzRiskAssessment::getCreateDate, startTime)
                    .lt(DzRiskAssessment::getCreateDate, endTime)
                    .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId)
        );
        Map<LocalDate, DzRiskAssessment> latestPerDay = new LinkedHashMap<>();
        for (DzRiskAssessment item : riskAssessments) {
            if (item.getCreateDate() == null) {
                continue;
            }
            LocalDate statDate = toLocalDate(item.getCreateDate());
            latestPerDay.putIfAbsent(statDate, item);
        }
        return latestPerDay.entrySet()
                           .stream()
                           .sorted(Map.Entry.comparingByKey())
                           .map(entry -> {
                               DzRiskAssessment assessment = entry.getValue();
                               ReceiveHandleProcessVo.RiskLevelItem item = new ReceiveHandleProcessVo.RiskLevelItem();
                               item.setTime(assessment.getCreateDate());
                               item.setRiskLevel(assessment.getDynamicRiskLevel());
                               item.setRiskLevelName(resolveDynamicRiskLevelName(assessment.getDynamicRiskLevel()));
                               return item;
                           })
                           .toList();
    }

    private List<ReceiveHandleProcessVo.DisasterReportItem> queryRecentDisasterReports(String slopeUnitId, LocalDate startDate, LocalDate endDate) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        Date startTime = toDate(startDate);
        Date endTime = toDate(endDate.plusDays(1));
        List<DzReportDisaster> reports = dzReportDisasterMapper.selectList(
            Wrappers.<DzReportDisaster>lambdaQuery()
                    .ge(DzReportDisaster::getCheckTime, startTime)
                    .lt(DzReportDisaster::getCheckTime, endTime)
                    .orderByDesc(DzReportDisaster::getCheckTime, DzReportDisaster::getId)
        );
        return reports.stream()
                      .filter(report -> matchSlopeUnit(report.getCheckCenter(), slopeUnitId))
                      .map(report -> {
                          ReceiveHandleProcessVo.DisasterReportItem item = new ReceiveHandleProcessVo.DisasterReportItem();
                          item.setReportId(report.getId());
                          item.setLocationName(report.getCheckCenterLocation());
                          item.setTime(report.getCheckTime());
                          item.setReporter(report.getUserName());
                          item.setAiTag(buildAiTag(report));
                          return item;
                      })
                      .toList();
    }

    private boolean matchSlopeUnit(String point, String slopeUnitId) {
        if (StringUtils.isBlank(point) || StringUtils.isBlank(slopeUnitId)) {
            return false;
        }
        SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(point);
        return slopeUnit != null && Objects.equals(slopeUnitId, slopeUnit.getId());
    }

    private String buildAiTag(DzReportDisaster report) {
        List<String> parts = new ArrayList<>(2);
        if (StringUtils.isNotBlank(report.getAiRiskLabel())) {
            parts.add(report.getAiRiskLabel());
        }
        if (report.getAiRiskLevel() != null) {
            parts.add(resolveDynamicRiskLevelName(report.getAiRiskLevel()));
        }
        return String.join(" + ", parts);
    }

    private List<DzTaskHandleDetailContent> listTaskHandleContents(Long handleId, Integer contentType) {
        return dzTaskHandleDetailContentMapper.selectList(
            Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                    .eq(DzTaskHandleDetailContent::getBizType, DetailBizTypeEnum.TASK_HANDLE.getCode())
                    .eq(DzTaskHandleDetailContent::getBizId, handleId)
                    .eq(DzTaskHandleDetailContent::getContentType, contentType)
                    .eq(DzTaskHandleDetailContent::getDeleted, 0)
                    .orderByDesc(DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
        );
    }

    private DzTaskHandleDetailContent queryInitialPlanContent(DefRespPlan defRespPlan, Long handleId) {
        if (defRespPlan != null) {
            DzTaskHandleDetailContent initial = queryLatestDetailContent(
                DetailBizTypeEnum.DEF_RESP_PLAN.getCode(),
                defRespPlan.getId(),
                DetailContentTypeEnum.INITIAL_REPORT.getCode()
            );
            if (initial != null) {
                return initial;
            }
        }
        return queryLatestDetailContent(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.EVACUATION_PLAN.getCode()
        );
    }

    private DzTaskHandleDetailContent queryFinalPlanContent(DefRespPlan defRespPlan, Long handleId) {
        if (defRespPlan != null) {
            DzTaskHandleDetailContent latest = queryLatestDetailContent(
                DetailBizTypeEnum.DEF_RESP_PLAN.getCode(),
                defRespPlan.getId(),
                DetailContentTypeEnum.FINAL_REPORT.getCode()
            );
            if (latest != null) {
                return latest;
            }
            latest = queryLatestDetailContent(
                DetailBizTypeEnum.DEF_RESP_PLAN.getCode(),
                defRespPlan.getId(),
                DetailContentTypeEnum.DEF_RESP_PLAN.getCode()
            );
            if (latest != null) {
                return latest;
            }
        }
        return queryLatestDetailContent(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.EVACUATION_PLAN.getCode()
        );
    }

    private DzTaskHandleDetailContent queryLatestDetailContent(Integer bizType, Long bizId, Integer contentType) {
        return dzTaskHandleDetailContentMapper.selectOne(
            Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                    .eq(DzTaskHandleDetailContent::getBizType, bizType)
                    .eq(DzTaskHandleDetailContent::getBizId, bizId)
                    .eq(DzTaskHandleDetailContent::getContentType, contentType)
                    .eq(DzTaskHandleDetailContent::getDeleted, 0)
                    .eq(DzTaskHandleDetailContent::getIsLatest, 1)
                    .orderByDesc(DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
                    .last("limit 1")
        );
    }

    private Date queryEarliestTaskDispatchTime(Long handleId) {
        return dzTaskDistListMapper.selectVoList(
                                       Wrappers.<DzTaskDistList>lambdaQuery()
                                               .eq(DzTaskDistList::getHandleId, handleId)
                                               .eq(DzTaskDistList::getDelete, 0)
                                               .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
                                   ).stream()
                                   .map(DzTaskDistListVo::getCreateDate)
                                   .filter(Objects::nonNull)
                                   .min(Date::compareTo)
                                   .orElse(null);
    }

    private String resolveEventName(DzTaskHandle taskHandle, DzTaskHandleSceneRecord sceneRecord) {
        if (sceneRecord != null && StringUtils.isNotBlank(sceneRecord.getDisasterName())) {
            return sceneRecord.getDisasterName();
        }
        if (sceneRecord != null && StringUtils.isNotBlank(sceneRecord.getLocation())) {
            String eventTypeName = sceneRecord.getDisasterType();
            if (StringUtils.isBlank(eventTypeName)) {
                eventTypeName = resolveEventTypeName(taskHandle.getEventType());
            }
            if (StringUtils.isNotBlank(eventTypeName)) {
                return sceneRecord.getLocation() + eventTypeName;
            }
            return sceneRecord.getLocation();
        }
        return resolveEventTypeName(taskHandle.getEventType());
    }

    private String resolveEventTypeName(Integer eventType) {
        if (eventType == null) {
            return null;
        }
        return EventTypeEnum.getByCode(eventType) == null
            ? null
            : Objects.requireNonNull(EventTypeEnum.getByCode(eventType)).getName();
    }

    private String resolveEventLevelName(Integer eventLevel) {
        return LevelCodeUtil.resolveEventLevelName(eventLevel);
    }

    private String resolveResponseLevelName(Integer responseLevel) {
        return ResponseStatusEnum.getDisplayName(responseLevel);
    }

    private String resolveDynamicRiskLevelName(Integer riskLevel) {
        return LevelCodeUtil.resolveDynamicRiskLevelName(riskLevel);
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserVo user = sysUserService.selectUserById(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.isNotBlank(user.getNickName())) {
            return user.getNickName();
        }
        return user.getUserName();
    }

    private String joinRoleAndName(String roleName, String name) {
        if (StringUtils.isBlank(roleName)) {
            return name;
        }
        if (StringUtils.isBlank(name)) {
            return roleName;
        }
        return roleName + name;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return LocalDate.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
