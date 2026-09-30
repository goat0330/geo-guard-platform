/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.RegionDefRespCloseReason;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespApprovalStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consultation.DefRespConsultationConfirmItems;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanApprovalStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBatchRelateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzProcessProgressVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.IMeetingService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespProcessDelegate {

    private static final int DEF_RESP_MEETING_TYPE = DzDefRespPlanServiceImpl.DEF_RESP_MEETING_TYPE;
    private static final int DEF_RESP_APPROVAL_PROCESS = DzDefRespPlanServiceImpl.DEF_RESP_APPROVAL_PROCESS;
    private static final String KEY_TASKS_NUMBER = DzDefRespPlanServiceImpl.KEY_TASKS_NUMBER;

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final IDzTaskHandleApprovalService dzTaskHandleApprovalService;
    private final IDzTaskDistListService dzTaskDistListService;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final IDzProcessProgressService dzProcessProgressService;
    private final IMeetingService meetingService;

    DzDefRespProcessDelegate(DzDefRespPlanServiceImpl service,
                             IDzTaskHandleDetailContentService dzTaskHandleDetailContentService,
                             IDzTaskHandleApprovalService dzTaskHandleApprovalService,
                             IDzTaskDistListService dzTaskDistListService,
                             DzTaskDistListMapper dzTaskDistListMapper,
                             IDzProcessProgressService dzProcessProgressService,
                             IMeetingService meetingService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzTaskHandleDetailContentService = dzTaskHandleDetailContentService;
        this.dzTaskHandleApprovalService = dzTaskHandleApprovalService;
        this.dzTaskDistListService = dzTaskDistListService;
        this.dzTaskDistListMapper = dzTaskDistListMapper;
        this.dzProcessProgressService = dzProcessProgressService;
        this.meetingService = meetingService;
    }

    private DefRespPlan getDefRespPlanById(Long defId) { return service.getDefRespPlanById(defId); }

    private void validateCurrentUserDefRespPlanAccess(Long handleId, String county, String streets) { service.validateCurrentUserDefRespPlanAccess(handleId, county, streets); }

    private int nextRoundNo(Integer currentRoundNo) { return service.nextRoundNo(currentRoundNo); }

    private void syncTownMirrorStatus(Long countyDefId, Integer newStatus, Date date) { service.syncTownMirrorStatus(countyDefId, newStatus, date); }

    private void insertExecutiveByBo(Long handleId, Integer meetingType, Integer handleProcess, Integer roundNo) { service.insertExecutiveByBo(handleId, meetingType, handleProcess, roundNo); }

    private boolean isCountyRegionPlan(DefRespPlan plan) { return service.isCountyRegionPlan(plan); }

    private int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset) { return service.batchUpdateTownRegionLevelsInternal(boList, allowCountyReset); }

    private int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset,
                                                    boolean roundAlreadyAdvanced) {
        return service.batchUpdateTownRegionLevelsInternal(boList, allowCountyReset, roundAlreadyAdvanced);
    }

    private boolean isTownRegionPlan(DefRespPlan plan) { return service.isTownRegionPlan(plan); }

    private void validateCountyRegionStatusTransitionRole() { service.validateCountyRegionStatusTransitionRole(); }

    private List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return service.listTownPlansByCountyId(countyDefId); }

    private boolean isTownRowActive(DefRespPlan town) { return service.isTownRowActive(town); }

    private void markAlarmSuperseded(Long alarmId, Date now) { service.markAlarmSuperseded(alarmId, now); }

    private void archiveTownPlansUnderCounty(Long countyDefId, Date date) { service.archiveTownPlansUnderCounty(countyDefId, date); }

    private String buildProcessProgressDescription(DefRespPlan defRespPlan, Integer status, Integer actionType, Integer roundNo) { return service.buildProcessProgressDescription(defRespPlan, status, actionType, roundNo); }

    private String buildAdminApprovalPendingDescription(DefRespPlan defRespPlan, Integer roundNo) { return service.buildAdminApprovalPendingDescription(defRespPlan, roundNo); }

    private String buildRegionApprovalPendingDescription(Long detailId, String approverName) { return service.buildRegionApprovalPendingDescription(detailId, approverName); }

    private String buildRegionApprovalPassedDescription(Long detailId, String approverName) { return service.buildRegionApprovalPassedDescription(detailId, approverName); }

    private long countTotalDefRespTasksByDefId(Long defId) { return service.countTotalDefRespTasksByDefId(defId); }

    private long countCompletedDefRespTasksByDefId(Long defId) { return service.countCompletedDefRespTasksByDefId(defId); }
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateApprovalStatusByBo(DefRespPlanApprovalStatusBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("防御响应方案id不能为空");
        }
        if (!Objects.equals(bo.getApprovalStatus(), DefRespApprovalStatusEnum.PENDING_APPROVAL.getCode())) {
            throw new ServiceException("当前仅支持提交待审批状态");
        }
        DefRespPlan defRespPlan = getDefRespPlanById(bo.getId());
        validateCurrentUserDefRespPlanAccess(defRespPlan.getHandleId(), defRespPlan.getCounty(), defRespPlan.getStreets());
        validateApprovalSubmitTarget(defRespPlan);
        dzTaskHandleDetailContentService.resetAlarmSyncedConsultationToPendingSubmit(bo.getId());
        Integer currentRoundNo = defRespPlan.getCurrentRoundNo();
        DzTaskHandleDetailContentVo detailContent = dzTaskHandleDetailContentService.requireLatestPendingConsultationDetail(defRespPlan.getId(), currentRoundNo);
        if (Objects.equals(defRespPlan.getApprovalStatus(), DefRespApprovalStatusEnum.PENDING_APPROVAL.getCode())) {
            throw new ServiceException("该区域防御响应变动已提交审批");
        }
        Date now = new Date();
        boolean incrementRound = defRespPlan.getStatus() != null
            && defRespPlan.getStatus() >= DefRespPlanStatusEnum.APPROVAL_PASSED.getCode()
            && hasActualTownScopeOrLevelChange(defRespPlan, detailContent.getPlanContentJson());
        Integer roundNo = incrementRound ? nextRoundNo(currentRoundNo) : currentRoundNo;
        LambdaUpdateWrapper<DefRespPlan> approvalWrapper = Wrappers.<DefRespPlan>lambdaUpdate()
            .eq(DefRespPlan::getId, defRespPlan.getId())
            .eq(DefRespPlan::getStatus, defRespPlan.getStatus())
            .set(DefRespPlan::getApprovalStatus, DefRespApprovalStatusEnum.PENDING_APPROVAL.getCode())
            .set(DefRespPlan::getUpdateDate, now);
        if (defRespPlan.getApprovalStatus() == null) {
            approvalWrapper.isNull(DefRespPlan::getApprovalStatus);
        } else {
            approvalWrapper.eq(DefRespPlan::getApprovalStatus, defRespPlan.getApprovalStatus());
        }
        if (currentRoundNo == null) {
            approvalWrapper.isNull(DefRespPlan::getCurrentRoundNo);
        } else {
            approvalWrapper.eq(DefRespPlan::getCurrentRoundNo, currentRoundNo);
        }
        if (incrementRound) {
            approvalWrapper.set(DefRespPlan::getCurrentRoundNo, roundNo);
        }
        if (baseMapper.update(null, approvalWrapper) <= 0) {
            throw new ServiceException("审批状态更新失败");
        }
        if (incrementRound) {
            DzTaskHandleDetailContentBo detailUpdate = BeanUtil.toBean(detailContent, DzTaskHandleDetailContentBo.class);
            detailUpdate.setRoundNo(roundNo);
            if (!dzTaskHandleDetailContentService.updateByBo(detailUpdate)) {
                throw new ServiceException("会商确认轮次更新失败");
            }
            syncTownMirrorStatus(defRespPlan.getId(), defRespPlan.getStatus(), now);
        }
        dzTaskHandleDetailContentService.insertInitialAndFinalReportFromSubmittedConsultation(detailContent, roundNo);
        insertExecutiveByBo(defRespPlan.getId(), DEF_RESP_MEETING_TYPE, DEF_RESP_APPROVAL_PROCESS, roundNo);
        String approverName = resolveAdminApproverNames(defRespPlan.getId(), roundNo);
        if (defRespPlan.getStatus() >= DefRespPlanStatusEnum.APPROVAL_PASSED.getCode()) {
            insertRegionApprovalProgress(defRespPlan.getId(),
                DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(),
                roundNo,
                buildRegionApprovalPendingDescription(detailContent.getId(), approverName));
        } else {
            refreshAdminApprovalPendingProgress(defRespPlan, roundNo);
        }
        return R.ok("已提交行政审批");
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleRegionApprovalPassed(Long handleId, Long approvalId) {
        if (handleId == null) {
            throw new ServiceException("handleId不能为空");
        }
        DefRespPlan defRespPlan = getDefRespPlanById(handleId);
        if (!isCountyRegionPlan(defRespPlan)) {
            return;
        }
        Integer roundNo = defRespPlan.getCurrentRoundNo() == null ? 1 : defRespPlan.getCurrentRoundNo();
        DzTaskHandleDetailContentVo detailContent = dzTaskHandleDetailContentService.queryLatestPendingFinalReport(handleId, roundNo);
        List<DefRespPlanBatchRelateBo> boList = buildBatchRelateBoList(detailContent.getPlanContentJson());
        batchUpdateTownRegionLevelsInternal(boList, false, true);
        if (!dzTaskHandleDetailContentService.updateConsultationConfirmApproved(detailContent)) {
            throw new ServiceException("会商确认记录状态更新失败");
        }
        DefRespPlan approvalUpdate = new DefRespPlan();
        approvalUpdate.setId(defRespPlan.getId());
        approvalUpdate.setApprovalStatus(DefRespApprovalStatusEnum.NONE.getCode());
        approvalUpdate.setUpdateDate(new Date());
        baseMapper.updateById(approvalUpdate);
        String approverName = resolveApproverNameByApprovalId(approvalId);
        if (defRespPlan.getStatus() >= DefRespPlanStatusEnum.APPROVAL_PASSED.getCode()
            && !hasProcessProgress(defRespPlan.getId(), DefRespPlanStatusEnum.APPROVAL_PASSED.getCode(), roundNo)) {
            insertRegionApprovalProgress(defRespPlan.getId(),
                DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(),
                roundNo,
                buildRegionApprovalPassedDescription(detailContent.getId(), approverName));
        }
    }

    /**
     * 推进防御响应方案到下一状态
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> processNext(Long id) {
        DefRespPlanBo bo = new DefRespPlanBo();
        bo.setId(id);
        DefRespPlan defRespPlan = getDefRespPlanById(id);
        if (defRespPlan.getStatus() == null) {
            throw new ServiceException("状态为空,无法流转");
        }
        boolean approvalModification = defRespPlan.getStatus() >= DefRespPlanStatusEnum.APPROVAL_PASSED.getCode()
            && Objects.equals(defRespPlan.getApprovalStatus(), DefRespApprovalStatusEnum.PENDING_APPROVAL.getCode());
        if (approvalModification) {
            approveRegionDefRespPlan(defRespPlan);
            bo.setStatus(defRespPlan.getStatus());
        } else if (DefRespPlanStatusEnum.STARTED.getCode().equals(defRespPlan.getStatus())) {
            bo.setStatus(DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode());
        } else if (DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode().equals(defRespPlan.getStatus())
            || DefRespApprovalStatusEnum.PENDING_APPROVAL.getCode().equals(defRespPlan.getApprovalStatus())) {
            approveRegionDefRespPlan(defRespPlan);
            bo.setStatus(defRespPlan.getStatus() + 1);
        } else {
            bo.setStatus(defRespPlan.getStatus() + 1);
        }
        if (Objects.equals(bo.getStatus(), DefRespPlanStatusEnum.TASK_PUBLISHED.getCode()) && DefRespApprovalStatusEnum.NONE.getCode().equals(defRespPlan.getApprovalStatus())) {
            bo.setExecuteStatus(1);
        }
        bo.setApprovalStatus(DefRespApprovalStatusEnum.NONE.getCode());
        return processStatusTransition(bo, defRespPlan);
    }

    /**
     * 执行防御响应状态流转
     */
    R<Map<String, Object>> processStatusTransition(DefRespPlanBo bo, DefRespPlan defRespPlan) {
        Map<String, Object> params = new HashMap<>();
        Date date = new Date();
        Integer newStatus = bo.getStatus();
        Integer oldStatus = defRespPlan.getStatus();
        Integer currentRoundNo = defRespPlan.getCurrentRoundNo() == null ? 1 : defRespPlan.getCurrentRoundNo();
        Map<String, Integer> endingTownLevels = DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)
            && isCountyRegionPlan(defRespPlan)
            ? service.captureTownLevelsForSms(defRespPlan)
            : Map.of();
        if (newStatus == null || oldStatus == null) {
            throw new ServiceException("状态为空,无法流转");
        }
        if (oldStatus == 6) {
            throw new ServiceException("该区域防御响应已结束,无法继续推进");
        }
        int diff = newStatus - oldStatus;
        boolean skipExpertConfirm = DefRespPlanStatusEnum.STARTED.getCode().equals(oldStatus)
            && DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode().equals(newStatus);
        boolean approvalStatusOnlyUpdate = diff == 0
            && bo.getApprovalStatus() != null
            && !Objects.equals(bo.getApprovalStatus(), defRespPlan.getApprovalStatus());
        if (!skipExpertConfirm && diff != 1 && !approvalStatusOnlyUpdate) {
            throw new ServiceException("不允许状态回退,期望状态为" + (oldStatus + 1) + ",当前状态为" + oldStatus);
        }
        if (isTownRegionPlan(defRespPlan)) {
            throw new ServiceException("乡镇区域防御响应由县级流程统一驱动，不允许单独推进状态");
        }
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(defRespPlan.getType())) {
            throw new ServiceException("单点防御响应已改为依附处置方案，不允许单独推进防御响应状态");
        }
        if (diff != 0 && DefRespPlanTypeEnum.REGION.getCode().equals(defRespPlan.getType())) {
            if (isCountyRegionPlan(defRespPlan)) {
                validateCountyRegionStatusTransitionRole();
                switch (newStatus) {
                    case 1:
                    case 2:
                    case 3:
                        break;
                    case 4:
                        defRespPlan.setCreateDate(date);
                        log.info("开始执行县级区域防御响应 id={}", defRespPlan.getId());
                        int updatedDailyTaskCount = 0;
                        for (DefRespPlan town : listTownPlansByCountyId(defRespPlan.getId())) {
                            if (!isTownRowActive(town) || StringUtils.isBlank(town.getStreets())) {
                                continue;
                            }
                            updatedDailyTaskCount += dzTaskDistListService.batchGenerateByRange(List.of(town.getStreets().trim()), town.getId());
                        }
                        params.put(KEY_TASKS_NUMBER, updatedDailyTaskCount);
                        break;
                    case 5:
                        break;
                    case 6:
                        if (defRespPlan.getSourceAlarmId() != null) {
                            markAlarmSuperseded(defRespPlan.getSourceAlarmId(), date);
                        }
                        log.info("县级区域防御响应已结束 id={}", defRespPlan.getId());
                        break;
                    default:
                        throw new ServiceException("状态异常,期望状态为" + bo.getStatus());
                }
            } else {
                throw new ServiceException("区域防御响应须为县级方案，无法流转状态");
            }
        }
        if (DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)) {
            long totalTaskCount;
            long completedTaskCount;
            if (isCountyRegionPlan(defRespPlan)) {
                totalTaskCount = 0;
                completedTaskCount = 0;
                for (DefRespPlan town : listTownPlansByCountyId(defRespPlan.getId())) {
                    totalTaskCount += countTotalDefRespTasksByDefId(town.getId());
                    completedTaskCount += countCompletedDefRespTasksByDefId(town.getId());
                }
            } else {
                LambdaQueryWrapper<DzTaskDistList> allTaskWrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                                            .eq(DzTaskDistList::getDefId, defRespPlan.getId())
                                                                            .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                                                                            .eq(DzTaskDistList::getDelete, 0);
                totalTaskCount = dzTaskDistListMapper.selectCount(allTaskWrapper);
                LambdaQueryWrapper<DzTaskDistList> completedTaskWrapper = Wrappers.<DzTaskDistList>lambdaQuery()
                                                                                  .eq(DzTaskDistList::getDefId, defRespPlan.getId())
                                                                                  .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                                                                                  .eq(DzTaskDistList::getDelete, 0)
                                                                                  .and(w -> w.or(w1 -> w1.and(inner -> inner.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_MONITOR)
                                                                                                                            .or()
                                                                                                                            .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_MONITOR))
                                                                                                         .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED))
                                                                                             .or(w2 -> w2.and(inner -> inner.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_PATROL)
                                                                                                                            .or()
                                                                                                                            .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_PATROL))
                                                                                                         .eq(DzTaskDistList::getQuotaConsumed, DzTaskDistList.QUOTA_CONSUMED_YES)));
                completedTaskCount = dzTaskDistListMapper.selectCount(completedTaskWrapper);
            }
            log.info("防御响应方案id={} 关联任务总数={}，已完成任务数={}", defRespPlan.getId(), totalTaskCount, completedTaskCount);
            defRespPlan.setTaskPublishTotal(totalTaskCount);
            defRespPlan.setTaskCompleteTotal(completedTaskCount);
        }
        // 仅更新状态流转字段，避免覆盖 processNext 内嵌审批(handleRegionApprovalPassed)已落库的 streets/level 等聚合字段
        log.info("processStatusTransition - 仅更新状态字段 defId={}, oldStatus={} -> newStatus={}, approvalStatus={}",
            defRespPlan.getId(), oldStatus, newStatus, bo.getApprovalStatus());
        LambdaUpdateWrapper<DefRespPlan> statusWrapper = Wrappers.<DefRespPlan>lambdaUpdate()
                                                                 .eq(DefRespPlan::getId, defRespPlan.getId())
                                                                 .eq(DefRespPlan::getStatus, oldStatus)
                                                                 .set(DefRespPlan::getStatus, newStatus)
                                                                 .set(DefRespPlan::getUpdateDate, date);
        if (bo.getApprovalStatus() != null) {
            statusWrapper.set(DefRespPlan::getApprovalStatus, bo.getApprovalStatus());
        }
        if (DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)) {
            statusWrapper.set(DefRespPlan::getTaskPublishTotal, defRespPlan.getTaskPublishTotal())
                         .set(DefRespPlan::getTaskCompleteTotal, defRespPlan.getTaskCompleteTotal())
                         .set(DefRespPlan::getExecuteStatus, DefRespExecuteStatusEnum.ENDED.getCode())
                         .set(DefRespPlan::getCloseTime, date)
                         .set(DefRespPlan::getCloseReason, RegionDefRespCloseReason.TOWN_ARCHIVE_WITH_COUNTY);
        }
        if (DefRespPlanTypeEnum.REGION.getCode().equals(defRespPlan.getType())
            && isCountyRegionPlan(defRespPlan)
            && DefRespPlanStatusEnum.APPROVAL_PASSED.getCode().equals(newStatus)
            && defRespPlan.getCreateDate() != null) {
            statusWrapper.set(DefRespPlan::getCreateDate, defRespPlan.getCreateDate());
        }
        int i = baseMapper.update(null, statusWrapper);
        defRespPlan.setStatus(newStatus);
        if (bo.getApprovalStatus() != null) {
            defRespPlan.setApprovalStatus(bo.getApprovalStatus());
        }
        if (DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)) {
            defRespPlan.setExecuteStatus(DefRespExecuteStatusEnum.ENDED.getCode());
            defRespPlan.setCloseTime(date);
            defRespPlan.setCloseReason(RegionDefRespCloseReason.TOWN_ARCHIVE_WITH_COUNTY);
        }
        defRespPlan.setUpdateDate(date);
        if (i > 0 && diff != 0 && shouldRecordDefRespStartedProcess(oldStatus, newStatus)) {
            recordDefRespTaskPublishedProcess(defRespPlan, date);
        }
        if (i > 0 && DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)) {
            recordDefRespArchivedProcess(defRespPlan, date);
        }
        if (i > 0 && isCountyRegionPlan(defRespPlan)) {
            if (DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)) {
                archiveTownPlansUnderCounty(defRespPlan.getId(), date);
                Long currentHandleId = defRespPlan.getHandleId() != null ? defRespPlan.getHandleId() : defRespPlan.getId();
                meetingService.closeDefRespMeetingsOnArchive(defRespPlan.getId(), currentHandleId);
            } else {
                syncTownMirrorStatus(defRespPlan.getId(), newStatus, date);
            }
        }
        if (diff != 0) {
            if (skipExpertConfirm) {
                Date progressDate = new Date(date.getTime() + 1000L);
                if (!hasProcessProgress(defRespPlan.getId(), DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(), currentRoundNo)) {
                    DzProcessProgressBo progress2 = new DzProcessProgressBo();
                    progress2.setDefId(defRespPlan.getId());
                    progress2.setStatus(DefRespPlanStatusEnum.MODEL_ANALYZED.getCode());
                    progress2.setCreateDate(date);
                    progress2.setRoundNo(currentRoundNo);
                    progress2.setActionType(1);
                    progress2.setDescription(buildProcessProgressDescription(defRespPlan, DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(), 1, currentRoundNo));
                    dzProcessProgressService.insertByBo(progress2);
                }
                if (!hasProcessProgress(defRespPlan.getId(), newStatus, currentRoundNo)) {
                    DzProcessProgressBo progress3 = new DzProcessProgressBo();
                    progress3.setDefId(defRespPlan.getId());
                    progress3.setStatus(newStatus);
                    progress3.setCreateDate(progressDate);
                    progress3.setRoundNo(currentRoundNo);
                    progress3.setActionType(1);
                    progress3.setDescription(buildProcessProgressDescription(defRespPlan, newStatus, 1, currentRoundNo));
                    dzProcessProgressService.insertByBo(progress3);
                }
            } else if (!hasProcessProgress(defRespPlan.getId(), newStatus, currentRoundNo)) {
                DzProcessProgressBo dzProcessProgressBo = new DzProcessProgressBo();
                dzProcessProgressBo.setDefId(defRespPlan.getId());
                dzProcessProgressBo.setStatus(newStatus);
                dzProcessProgressBo.setCreateDate(date);
                dzProcessProgressBo.setRoundNo(currentRoundNo);
                dzProcessProgressBo.setActionType(1);
                dzProcessProgressBo.setDescription(buildProcessProgressDescription(defRespPlan, newStatus, 1, currentRoundNo));
                dzProcessProgressService.insertByBo(dzProcessProgressBo);
            }
        }
        if (i > 0) {
            if (shouldAutoSendDefRespStartSms(defRespPlan, oldStatus, newStatus)) {
                DefRespSmsEventSnapshot event = service.buildStartSmsEvent(defRespPlan, date);
                service.triggerDefRespLifecycleSmsAfterCommit(event, false);
            } else if (DefRespPlanStatusEnum.ENDED.getCode().equals(newStatus)
                && isCountyRegionPlan(defRespPlan)) {
                DefRespSmsEventSnapshot event = service.buildEndSmsEvent(defRespPlan, date, endingTownLevels);
                service.triggerDefRespLifecycleSmsAfterCommit(event, false);
            }
            return R.ok("状态流转成功", params);
        }
        throw new ServiceException("状态流转失败");
    }

    boolean shouldAutoSendDefRespStartSms(DefRespPlan defRespPlan, Integer oldStatus, Integer newStatus) {
        return DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode().equals(oldStatus)
            && DefRespPlanStatusEnum.APPROVAL_PASSED.getCode().equals(newStatus)
            && isCountyRegionPlan(defRespPlan);
    }

    void triggerDefRespStartSmsAfterCommit(Long defId) {
        if (defId == null) {
            return;
        }
        Runnable action = () -> submitDefRespStartSms(defId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void submitDefRespStartSms(Long defId) {
        try {
            CompletableFuture.runAsync(() -> sendDefRespStartSmsSafely(defId), Run.executor)
                             .exceptionally(ex -> {
                                 log.error("异步发送防御响应启动短信任务执行异常, defId={}", defId, ex);
                                 return null;
                             });
        } catch (Exception e) {
            log.error("提交防御响应启动短信异步任务失败, defId={}", defId, e);
        }
    }

    void sendDefRespStartSmsSafely(Long defId) {
        try {
            SmsSendSummaryVo summary = dzTaskDistListService.sendDefRespSmsByDefId(defId);
            log.info("防御响应启动短信自动发送完成, defId={}, totalCount={}, successCount={}, skipCount={}, failCount={}",
                defId, summary.getTotalCount(), summary.getSuccessCount(), summary.getSkipCount(), summary.getFailCount());
        } catch (Exception e) {
            log.warn("防御响应启动短信自动发送失败，不影响状态流转, defId={}, err={}", defId, e.getMessage(), e);
        }
    }

    /**
     * 保存方案内容历史
     */

    void validateApprovalSubmitTarget(DefRespPlan defRespPlan) {
        if (!isCountyRegionPlan(defRespPlan)) {
            throw new ServiceException("仅支持县级区域防御响应提交行政审批");
        }
    }

    private void recordDefRespArchivedProcess(DefRespPlan defRespPlan, Date triggerTime) {
        if (defRespPlan == null || defRespPlan.getId() == null) {
            return;
        }
        DzTaskProcessChainNode predecessor = ensureDefRespArchivePredecessor(defRespPlan, triggerTime);
        String chainId = resolveDefRespChainId(defRespPlan);
        OperatorSnapshot operator = currentOperator(defRespPlan);
        service.taskProcessChainNodeService.recordBizNode(
            chainId,
            TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getLinkName(),
            StringUtils.isNotBlank(defRespPlan.getCloseReason())
                ? defRespPlan.getCloseReason()
                : TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            null,
            operator.userId(),
            operator.userName(),
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(),
            predecessor == null ? null : predecessor.getId(),
            null,
            null,
            TaskProcessStageTypeEnum.ARCHIVE.getCode()
        );
    }

    OperatorSnapshot currentOperator(DefRespPlan defRespPlan) {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null) {
                String userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
                if (loginUser.getUserId() != null || StringUtils.isNotBlank(userName)) {
                    return new OperatorSnapshot(loginUser.getUserId(), userName);
                }
            }
        } catch (Exception e) {
            log.debug("解析区域防御响应流程操作人失败，将降级使用方案责任人, defId={}",
                defRespPlan == null ? null : defRespPlan.getId(), e);
        }
        return new OperatorSnapshot(null, defRespPlan == null ? null : defRespPlan.getResponsiblePerson());
    }

    private void recordDefRespTaskPublishedProcess(DefRespPlan defRespPlan, Date triggerTime) {
        if (defRespPlan == null || defRespPlan.getId() == null) {
            return;
        }
        TaskProcessChainNodeTextEnum link = TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED;
        OperatorSnapshot operator = currentOperator(defRespPlan);
        DzTaskProcessChainNode predecessor = ensureDefRespPublishedPredecessor(defRespPlan, triggerTime);
        if (predecessor == null || StringUtils.isBlank(predecessor.getChainId())) {
            throw new ServiceException("启动区域防御响应未找到创建阶段前序节点, defId=" + defRespPlan.getId()
                + ", alarmId=" + defRespPlan.getSourceAlarmId());
        }
        service.taskProcessChainNodeService.recordBizNode(
            predecessor.getChainId(),
            link.getLinkName(),
            link.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            null,
            operator.userId(),
            operator.userName(),
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(),
            predecessor == null ? null : predecessor.getId(),
            null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode(),
            TaskProcessStageTypeEnum.DEF_RESP.getCode()
        );
    }

    private boolean shouldRecordDefRespStartedProcess(Integer oldStatus, Integer newStatus) {
        return DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode().equals(oldStatus)
            && DefRespPlanStatusEnum.APPROVAL_PASSED.getCode().equals(newStatus);
    }

    private DzTaskProcessChainNode ensureDefRespPublishedPredecessor(DefRespPlan defRespPlan, Date triggerTime) {
        DzTaskProcessChainNode alarmStartNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (alarmStartNode != null) {
            return alarmStartNode;
        }
        DzTaskProcessChainNode startNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (startNode != null) {
            return startNode;
        }
        return null;
    }

    private DzTaskProcessChainNode ensureDefRespArchivePredecessor(DefRespPlan defRespPlan, Date triggerTime) {
        if (defRespPlan == null || defRespPlan.getId() == null) {
            return null;
        }
        DzTaskProcessChainNode publishedNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (publishedNode != null) {
            return publishedNode;
        }
        DzTaskProcessChainNode startNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (startNode != null) {
            return startNode;
        }
        if (defRespPlan.getSourceAlarmId() != null) {
            recordDefRespTaskPublishedProcess(defRespPlan, triggerTime);
            return service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
                TaskProcessBizTypeEnum.DEF_RESP.getCode(),
                defRespPlan.getId(),
                TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
                TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
            );
        }
        return null;
    }

    private String resolveDefRespPublishedChainId(DefRespPlan defRespPlan) {
        if (defRespPlan.getSourceAlarmId() == null) {
            return resolveDefRespChainId(defRespPlan);
        }
        String alarmChainId = service.taskProcessChainNodeService.resolveSavedChainIdByBizFast(
            TaskProcessBizTypeEnum.ALARM.getCode(), defRespPlan.getSourceAlarmId());
        if (StringUtils.isNotBlank(alarmChainId)) {
            return alarmChainId;
        }
        throw new ServiceException("预警触发防御响应未找到已有流程链路, defId=" + defRespPlan.getId()
            + ", alarmId=" + defRespPlan.getSourceAlarmId());
    }

    private String resolveDefRespChainId(DefRespPlan defRespPlan) {
        if (defRespPlan.getSourceAlarmId() != null) {
            DzTaskProcessChainNode alarmStartNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
                TaskProcessBizTypeEnum.DEF_RESP.getCode(),
                defRespPlan.getId(),
                TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
                TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
            );
            if (alarmStartNode != null && StringUtils.isNotBlank(alarmStartNode.getChainId())) {
                return alarmStartNode.getChainId();
            }
            String alarmChainId = service.taskProcessChainNodeService.resolveSavedChainIdByBizFast(
                TaskProcessBizTypeEnum.ALARM.getCode(), defRespPlan.getSourceAlarmId());
            if (StringUtils.isNotBlank(alarmChainId)) {
                return alarmChainId;
            }
            throw new ServiceException("预警触发防御响应未找到已有流程链路, defId=" + defRespPlan.getId()
                + ", alarmId=" + defRespPlan.getSourceAlarmId());
        }
        DzTaskProcessChainNode startNode = service.taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            defRespPlan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (startNode != null && StringUtils.isNotBlank(startNode.getChainId())) {
            return startNode.getChainId();
        }
        String chainId;
        if (defRespPlan.getHandleId() != null) {
            chainId = service.taskProcessChainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), defRespPlan.getHandleId());
            if (StringUtils.isNotBlank(chainId)) {
                return chainId;
            }
        }
        throw new ServiceException("防御响应未找到已有流程链路, defId=" + defRespPlan.getId());
    }

    List<DefRespPlanBatchRelateBo> buildBatchRelateBoList(String planContentJson) {
        List<DefRespConsultationConfirmItems.Item> items = JacksonUtil.toList(planContentJson, DefRespConsultationConfirmItems.Item.class);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("会商确认数据格式异常");
        }
        List<DefRespPlanBatchRelateBo> boList = new ArrayList<>();
        for (DefRespConsultationConfirmItems.Item item : items) {
            if (item == null || item.getStreets() == null || item.getStreets().isEmpty()) {
                throw new ServiceException("会商确认乡镇不能为空");
            }
            DefRespPlanBatchRelateBo bo = new DefRespPlanBatchRelateBo();
            bo.setStreets(item.getStreets());
            bo.setNewLevel(item.getNewLevel());
            bo.setAlarmId(item.resolveAlarmId());
            boList.add(bo);
        }
        return boList;
    }

    boolean hasActualTownScopeOrLevelChange(DefRespPlan countyPlan, String planContentJson) {
        if (countyPlan == null || countyPlan.getId() == null) {
            return false;
        }
        Map<String, Integer> plannedLevels = new LinkedHashMap<>();
        for (DefRespPlanBatchRelateBo item : buildBatchRelateBoList(planContentJson)) {
            if (item == null || item.getNewLevel() == null || item.getStreets() == null) {
                continue;
            }
            for (String street : item.getStreets()) {
                if (StringUtils.isNotBlank(street)) {
                    plannedLevels.put(street.trim(), item.getNewLevel());
                }
            }
        }
        return !Objects.equals(service.captureTownLevelsForSms(countyPlan), plannedLevels);
    }

    String resolveAdminApproverNames(Long handleId, Integer roundNo) {
        DzTaskHandleApprovalBo queryBo = new DzTaskHandleApprovalBo();
        queryBo.setHandleId(handleId);
        queryBo.setMeetingType(DEF_RESP_MEETING_TYPE);
        queryBo.setType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
        queryBo.setProcess(DEF_RESP_APPROVAL_PROCESS);
        queryBo.setRoundNo(roundNo);
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalService.queryList(queryBo);
        if (approvals == null || approvals.isEmpty()) {
            return null;
        }
        return approvals.stream()
                        .map(DzTaskHandleApprovalVo::getNickname)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .collect(Collectors.joining(","));
    }

    /**
     * processNext 可能先于 approvalStatus 写入行政审批进度，此处补全审批人信息。
     */
    void refreshAdminApprovalPendingProgress(DefRespPlan defRespPlan, Integer roundNo) {
        if (defRespPlan == null || defRespPlan.getId() == null || roundNo == null) {
            return;
        }
        String description = buildAdminApprovalPendingDescription(defRespPlan, roundNo);
        DzProcessProgressBo queryBo = new DzProcessProgressBo();
        queryBo.setDefId(defRespPlan.getId());
        queryBo.setStatus(DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode());
        queryBo.setRoundNo(roundNo);
        List<DzProcessProgressVo> progressList = dzProcessProgressService.queryList(queryBo);
        if (progressList != null && !progressList.isEmpty()) {
            DzProcessProgressVo latest = progressList.stream()
                                                     .max(Comparator.comparing(DzProcessProgressVo::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                                                     .orElse(null);
            if (latest.getId() != null) {
                DzProcessProgressBo update = new DzProcessProgressBo();
                update.setId(latest.getId());
                update.setDescription(description);
                dzProcessProgressService.updateByBo(update);
                return;
            }
        }
        insertRegionApprovalProgress(defRespPlan.getId(),
            DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(),
            roundNo,
            description);
    }

    String resolveApproverNameByApprovalId(Long approvalId) {
        if (approvalId == null) {
            return null;
        }
        DzTaskHandleApprovalVo approvalVo = dzTaskHandleApprovalService.queryById(approvalId);
        return approvalVo == null ? null : approvalVo.getNickname();
    }

    void insertRegionApprovalProgress(Long defId, Integer status, Integer roundNo, String description) {
        if (hasProcessProgress(defId, status, roundNo)) {
            return;
        }
        DzProcessProgressBo progressBo = new DzProcessProgressBo();
        progressBo.setDefId(defId);
        progressBo.setStatus(status);
        progressBo.setRoundNo(roundNo);
        progressBo.setActionType(1);
        progressBo.setCreateDate(new Date());
        progressBo.setDescription(description);
        dzProcessProgressService.insertByBo(progressBo);
    }

    /**
     * processNext 内嵌行政审批：仅将当前用户待审记录置为通过，已通过的记录不重复触发回调。
     */
    void approveRegionDefRespPlan(DefRespPlan defRespPlan) {
        DzTaskHandleApprovalBo dzTaskHandleApprovalBo = new DzTaskHandleApprovalBo();
        dzTaskHandleApprovalBo.setHandleId(defRespPlan.getId());
        dzTaskHandleApprovalBo.setMeetingType(DEF_RESP_MEETING_TYPE);
        dzTaskHandleApprovalBo.setType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
        dzTaskHandleApprovalBo.setProcess(DEF_RESP_APPROVAL_PROCESS);
        dzTaskHandleApprovalBo.setStatus(0);
        dzTaskHandleApprovalBo.setRoundNo(defRespPlan.getCurrentRoundNo());
        List<DzTaskHandleApprovalVo> dzTaskHandleApprovalVos = dzTaskHandleApprovalService.queryList(dzTaskHandleApprovalBo);
        Long userId = LoginHelper.getUserId();
        int total = 0;
        for (DzTaskHandleApprovalVo dzTaskHandleApprovalVo : dzTaskHandleApprovalVos) {
            if (!userId.equals(dzTaskHandleApprovalVo.getUserId())) {
                continue;
            }
            if (!Integer.valueOf(1).equals(dzTaskHandleApprovalVo.getStatus())) {
                dzTaskHandleApprovalVo.setStatus(1);
                dzTaskHandleApprovalService.updateByBo(BeanUtil.toBean(dzTaskHandleApprovalVo, DzTaskHandleApprovalBo.class));
            }
            total++;
        }
        if (total == 0) {
            throw new ServiceException("无该防御响应方案审批权限");
        }
    }

    /**
     * 判断指定轮次是否已有同状态的流程进度，避免无状态流转时重复落库。
     */
    boolean hasProcessProgress(Long defId, Integer status, Integer roundNo) {
        if (defId == null || status == null || roundNo == null) {
            return false;
        }
        DzProcessProgressBo queryBo = new DzProcessProgressBo();
        queryBo.setDefId(defId);
        queryBo.setStatus(status);
        queryBo.setRoundNo(roundNo);
        List<DzProcessProgressVo> progressList = dzProcessProgressService.queryList(queryBo);
        return progressList != null && !progressList.isEmpty();
    }

    record OperatorSnapshot(Long userId, String userName) {
    }

}
