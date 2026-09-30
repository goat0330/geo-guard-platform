package cn.edu.pku.whai.geological.disaster.service.app.service.impl;

import org.dromara.common.core.domain.dto.OssDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.OssService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitPersonVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionRemindReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionSubmitReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskPushPayloads;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.resp.PushTaskDataResp;
import cn.edu.pku.whai.geological.disaster.service.app.domain.resp.PushTaskResp;
import cn.edu.pku.whai.geological.disaster.service.app.job.AppTaskJob;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AiPictureDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListHistory;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListHistoryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.*;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.DzTaskDistListServiceImpl;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.AiVisionStatusUtils;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.enums.FileTransferMethod;
import io.github.imfangs.dify.client.enums.FileType;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import io.github.imfangs.dify.client.model.file.FileInfo;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * APP任务服务实现。
 *
 * @author kongweiguang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppTaskServiceImpl implements IAppTaskService {
    static final int AI_IMAGE_MAX_SIDE = 1024;
    static final String AI_IMAGE_FALLBACK_FORMAT = "jpg";
    static final int TASK_REMARK_MAX_LENGTH = 255;
    static final String INSPECTING_REMARK_DEFAULT = "任务状态更新为核查中";
    static final int AI_PICTURE_FALLBACK_RISK_LEVEL = 1;
    static final String AI_PICTURE_FALLBACK_RISK_LABEL = "AI识图待确认";
    static final String AI_PICTURE_FALLBACK_REPORT_DETAIL = "AI识图未返回有效结果，请人工核实现场情况。";
    private static final int MIN_SUBMIT_PHOTO_COUNT = 2;
    private static final int TASK_FEEDBACK_REPORT_SOURCE = 1;
    private static final int PUBLIC_REPORT_SOURCE = 2;
    private static final int PATROL_REPORT_PROCESS_TYPE = DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY;
    private static final int DIRECT_TOWN_PROCESS_TYPE = DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN;
    private static final long DEFAULT_PUBLIC_REPORT_IDEMPOTENT_WINDOW_SECONDS = 3600L;
    private static final ConcurrentMap<LocalDate, AtomicLong> INVALID_SUBMIT_COUNTER = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> PUBLIC_REPORT_SUBMIT_LOCKS = new ConcurrentHashMap<>();


    private final AppTaskProps appTaskProps;
    private final ISysOssService sysOssService;
    private final OssService ossService;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final DzTaskDistListHistoryMapper dzTaskDistListHistoryMapper;
    private final DifyAgentClient difyAgentClient;
    private final ISlopeUnitService slopeUnitService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final ISysUserService sysUserService;
    private final ITaskSmsContentService taskSmsContentService;
    private final DisasterDetailedAddressResolver disasterDetailedAddressResolver;
    private final ObjectProvider<IDzTaskHandleService> dzTaskHandleServiceProvider;
    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;
    private final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(InspectionSubmitReq req) {
        try {
            // 如果有任务ID，则更新任务信息
            if (req.getTaskId() != null) {
                SubmitTaskUpdateResult updateResult = updateTaskInfo(req);
                Long submitNodeId = recordSubmitTaskChainNode(updateResult.task(), req, updateResult.historyId());
                tryAutoArchiveHandleAfterEmergencyFeedback(updateResult.task(), req);

                if (updateResult.shouldCreateReport()) {
                    // 任务反馈
                    persistDisasterAndTriggerAi(req, 1, submitNodeId);
                }
                if (updateResult.shouldCreateTechAssistance()) {
                    createTechAssistanceTaskFromSubmit(updateResult.task(), req, submitNodeId);
                }
            } else {
                // 直接插入灾害报告（群众报灾）
                // 群众报灾
                persistPublicReportIdempotently(req);
            }
        } catch (ServiceException e) {
            log.warn("APP任务提交失败, taskId={}, inspectorId={}, status={}, msg={}",
                req.getTaskId(), req.getInspectorId(), req.getStatus(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("APP任务提交异常, taskId={}, inspectorId={}, status={}",
                req.getTaskId(), req.getInspectorId(), req.getStatus(), e);
            throw e;
        }
    }


    /**
     * 更新任务信息
     */
    private SubmitTaskUpdateResult updateTaskInfo(InspectionSubmitReq req) {
        DzTaskDistList dzTaskDistList = dzTaskDistListMapper.selectById(req.getTaskId());
        if (dzTaskDistList == null) {
            throw new ServiceException("任务不存在");
        }
        Integer targetStatus = req.getStatus() != null ? req.getStatus() : DzTaskDistList.STATUS_FEEDBACKED;
        validateTaskSubmit(dzTaskDistList, req, targetStatus);
        String checkInCenter = req.getCheckInCenter();
        String taskCheckCenter = resolveTaskCheckCenter(dzTaskDistList);
        validateFenceDistance(checkInCenter, taskCheckCenter);
        String checkCenterLocation = disasterDetailedAddressResolver.resolveCheckCenterLocation(checkInCenter);
        // 更新任务信息
        Integer originalStatus = dzTaskDistList.getStatus();
        dzTaskDistList.setCheckCenter(req.getCheckInCenter());
        dzTaskDistList.setCheckCenterLocation(checkCenterLocation);
        if (StrUtil.isNotBlank(req.getCheckInTime())) {
            dzTaskDistList.setCheckTime(parseDateTime(req.getCheckInTime(), "打卡时间格式错误"));
        }
        dzTaskDistList.setTextRecord(req.getTextRecord());
        dzTaskDistList.setStatus(targetStatus);
        boolean shouldCreateReport = shouldCreateReport(dzTaskDistList, originalStatus, targetStatus);
        boolean shouldCreateTechAssistance = shouldCreateTechAssistance(originalStatus, targetStatus);
        if (targetStatus.equals(DzTaskDistList.STATUS_FEEDBACKED) && DzTaskDistList.isPatrolTask(dzTaskDistList.getPlanName())) {
            dzTaskDistList.setQuotaConsumed(DzTaskDistList.QUOTA_CONSUMED_YES);
        }

        String photoStr = buildScenePhoto(req.getPhotos());

        dzTaskDistList.setScenePhoto(photoStr);
        if (StrUtil.isNotBlank(req.getSubmitTime())) {
            dzTaskDistList.setSubmitTime(parseDateTime(req.getSubmitTime(), "提交时间格式错误"));
        }

        dzTaskDistListMapper.updateById(dzTaskDistList);
        Long historyId = insertTaskSubmitHistory(req, targetStatus, checkCenterLocation);
        return new SubmitTaskUpdateResult(dzTaskDistList, shouldCreateReport, shouldCreateTechAssistance, historyId);
    }

    private Long insertTaskSubmitHistory(InspectionSubmitReq req, Integer targetStatus, String checkCenterLocation) {
        if (req == null || req.getTaskId() == null) {
            return null;
        }
        Date now = new Date();
        DzTaskDistListHistory history = new DzTaskDistListHistory();
        history.setId(IdUtil.getSnowflakeNextId());
        history.setTaskId(req.getTaskId());
        history.setTextRecord(buildSubmitHistoryText(req.getTextRecord()));
        history.setSubmitStatus(targetStatus);
        history.setScenePhoto(buildScenePhoto(req.getPhotos()));
        history.setCheckCenter(req.getCheckInCenter());
        history.setCheckCenterLocation(checkCenterLocation);
        history.setUserId(req.getInspectorId());
        history.setCreateDate(now);
        history.setUpdateDate(now);
        dzTaskDistListHistoryMapper.insertEntity(history);
        return history.getId();
    }

    private static String buildScenePhoto(List<String> photos) {
        return CollUtil.isNotEmpty(photos)
            ? photos.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining(","))
            : "";
    }

    private void tryAutoArchiveHandleAfterEmergencyFeedback(DzTaskDistList task, InspectionSubmitReq req) {
        if (task == null
            || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)
            || !Objects.equals(task.getStatus(), DzTaskDistList.STATUS_FEEDBACKED)
            || task.getHandleId() == null) {
            return;
        }
        dzTaskHandleServiceProvider.getObject().tryAutoArchiveAfterEmergencyTaskFeedback(
            task.getHandleId(),
            req == null ? null : req.getInspectorId(),
            req == null ? null : StrUtil.blankToDefault(req.getInspectorName(), task.getResponsiblePerson())
        );
    }

    static String buildSubmitHistoryText(String textRecord) {
        String remark = StrUtil.blankToDefault(StrUtil.trim(textRecord), INSPECTING_REMARK_DEFAULT);
        if (remark.length() <= TASK_REMARK_MAX_LENGTH) {
            return remark;
        }
        return remark.substring(0, TASK_REMARK_MAX_LENGTH);
    }

    private boolean shouldCreateReport(DzTaskDistList task, Integer originalStatus, Integer targetStatus) {
        return DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus)
            && !Objects.equals(originalStatus, targetStatus)
            && isReportPersistSource(task);
    }

    private boolean isReportPersistSource(DzTaskDistList task) {
        if (task == null) {
            return false;
        }
        Integer sourceType = task.getSourceType();
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT)) {
            return isPublicReportTask(task);
        }
        return Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MANUAL)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING);
    }

    private boolean shouldCreateTechAssistance(Integer originalStatus, Integer targetStatus) {
        return DzTaskDistList.STATUS_TECH_ASSISTANCE.equals(targetStatus)
            && !Objects.equals(originalStatus, targetStatus);
    }

    private static String normalizeSlopeUnitId(String unitId) {
        if (StrUtil.isBlank(unitId)) {
            return unitId;
        }
        String trimmed = unitId.trim();
        if (!trimmed.matches("\\d+")) {
            return unitId;
        }
        return String.format("%4s", trimmed).replace(' ', '0');
    }

    private void validateTaskSubmit(DzTaskDistList task, InspectionSubmitReq req, Integer targetStatus) {
        if (!isAllowedSubmitStatus(task, targetStatus)) {
            rejectInvalidSubmit("当前任务来源不允许提交为该状态");
        }
        if (Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED)) {
            rejectInvalidSubmit("未推送任务不允许提交");
        }
        if (Objects.equals(task.getStatus(), DzTaskDistList.STATUS_CLOSED)) {
            rejectInvalidSubmit("已关闭任务不允许提交");
        }
        if (Objects.equals(task.getStatus(), DzTaskDistList.STATUS_FEEDBACKED)) {
            rejectInvalidSubmit("任务已反馈，请勿重复提交");
        }
        if (Objects.equals(task.getStatus(), DzTaskDistList.STATUS_TECH_ASSISTANCE)
            && targetStatus.equals(DzTaskDistList.STATUS_TECH_ASSISTANCE)) {
            rejectInvalidSubmit("任务已申请技术协查，请勿重复提交");
        }
        if (targetStatus.equals(DzTaskDistList.STATUS_CLOSED) && shouldValidateSubmitPhotos(task)) {
            validateSubmitPhotos(req);
        }
        if (targetStatus.equals(DzTaskDistList.STATUS_FEEDBACKED)
            || targetStatus.equals(DzTaskDistList.STATUS_TECH_ASSISTANCE)) {
            validateDisasterReportSubmit(task, req, targetStatus);
        }
    }

    private boolean isAllowedSubmitStatus(DzTaskDistList task, Integer targetStatus) {
        return isAllowedSubmitStatus(
            task == null ? null : task.getSourceType(),
            task != null && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_REPORT) && isPublicReportTask(task),
            targetStatus
        );
    }

    static boolean isAllowedSubmitStatus(Integer sourceType, boolean publicReportTask, Integer targetStatus) {
        if (targetStatus == null || !DzTaskDistList.APP_SUBMIT_TARGET_STATUSES.contains(targetStatus)) {
            return false;
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT)) {
            if (publicReportTask) {
                return DzTaskDistList.STATUS_CLOSED.equals(targetStatus)
                    || DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus);
            }
            return DzTaskDistList.STATUS_CLOSED.equals(targetStatus)
                || DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus)
                || DzTaskDistList.STATUS_TECH_ASSISTANCE.equals(targetStatus);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)
            || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MANUAL)) {
            return DzTaskDistList.STATUS_CLOSED.equals(targetStatus)
                || DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return DzTaskDistList.STATUS_INSPECTING.equals(targetStatus)
                || DzTaskDistList.STATUS_FEEDBACKED.equals(targetStatus);
        }
        return false;
    }

    private boolean isPublicReportTask(DzTaskDistList task) {
        if (task == null || task.getReportId() == null) {
            return false;
        }
        DzReportDisaster report = dzReportDisasterMapper.selectById(task.getReportId());
        if (report == null) {
            return false;
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY)) {
            return true;
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)) {
            return false;
        }
        return report.getProcessType() == null
            && Objects.equals(report.getSourceType(), DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT)
            && report.getTaskId() == null;
    }

    private void validateDisasterReportSubmit(DzTaskDistList task, InspectionSubmitReq req, Integer targetStatus) {
        if (StrUtil.isBlank(req.getCheckInTime())) {
            rejectInvalidSubmit("打卡时间不能为空");
        }
        if (StrUtil.isBlank(req.getCheckInCenter())) {
            rejectInvalidSubmit("打卡位置不能为空");
        }
        if (StrUtil.isBlank(req.getSubmitTime())) {
            rejectInvalidSubmit("提交时间不能为空");
        }
        if (Objects.equals(targetStatus, DzTaskDistList.STATUS_TECH_ASSISTANCE)) {
            if (shouldValidateSubmitPhotos(task)) {
                validateSubmitPhotos(req);
            }
            return;
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return;
        }
        if (StrUtil.isBlank(req.getTextRecord())) {
            rejectInvalidSubmit("文字记录不能为空");
        }
        validateSubmitPhotos(req);
    }

    private boolean shouldValidateSubmitPhotos(DzTaskDistList task) {
        return task == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
    }

    private void validateSubmitPhotos(InspectionSubmitReq req) {
        if (!hasEnoughSubmitPhotos(req)) {
            rejectInvalidSubmit("至少上传2张现场照片");
        }
    }

    static boolean hasEnoughSubmitPhotos(InspectionSubmitReq req) {
        return req != null
            && CollUtil.isNotEmpty(req.getPhotos())
            && req.getPhotos().stream().filter(StrUtil::isNotBlank).count() >= MIN_SUBMIT_PHOTO_COUNT;
    }

    private String resolveTaskCheckCenter(DzTaskDistList task) {
        String taskCheckCenter = task.getCheckCenter();
        if (StrUtil.isNotBlank(taskCheckCenter) || StrUtil.isBlank(task.getUnitId())) {
            return taskCheckCenter;
        }
        String unitId = task.getUnitId();
        String normalizedId = normalizeSlopeUnitId(unitId);
        List<String> ids = unitId.equals(normalizedId)
            ? List.of(unitId)
            : List.of(unitId, normalizedId);
        List<SlopeUnit> slopeUnits = slopeUnitService.listPoByIds(ids);
        return slopeUnits.stream()
                         .filter(ObjUtil::isNotNull)
                         .map(SlopeUnit::getCenter)
                         .filter(StrUtil::isNotBlank)
                         .findFirst()
                         .orElse(null);
    }

    private void validateFenceDistance(String checkInCenter, String taskCheckCenter) {
        if (StrUtil.isBlank(checkInCenter) || StrUtil.isBlank(taskCheckCenter)) {
            return;
        }
        double[] checkInCoordinates = GeoDistanceUtil.parsePoint(checkInCenter);
        double[] taskCoordinates = GeoDistanceUtil.parsePoint(taskCheckCenter);
        if (checkInCoordinates == null || taskCoordinates == null) {
            return;
        }
        int radiusMeters = appTaskProps.getCheckInFenceRadiusMeters() != null
            ? appTaskProps.getCheckInFenceRadiusMeters()
            : 500;
        double distance = GeoDistanceUtil.distanceMeters(
            checkInCoordinates[0],
            checkInCoordinates[1],
            taskCoordinates[0],
            taskCoordinates[1]
        );
        if (distance > radiusMeters) {
            rejectInvalidSubmit("打卡位置超出电子围栏范围，需在" + radiusMeters + "米内提交");
        }
    }

    private void rejectInvalidSubmit(String message) {
        INVALID_SUBMIT_COUNTER.computeIfAbsent(LocalDate.now(), key -> new AtomicLong()).incrementAndGet();
        throw new ServiceException(message);
    }

    private Date parseDateTime(String value, String message) {
        try {
            return new Date(DateUtil.parseDateTime(value).getTime());
        } catch (Exception e) {
            throw new ServiceException(message);
        }
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

    private static final String INSPECTING_REQUIRE = "2小时内上传现场图像（≥2张）及文字记录";

    private void persistPublicReportIdempotently(InspectionSubmitReq req) {
        PublicReportIdempotentContext context = buildPublicReportIdempotentContext(req);
        ResolvedPublicReportSubmitter submitter = resolvePublicReportSubmitter(req);
        if (PUBLIC_REPORT_SUBMIT_LOCKS.putIfAbsent(context.key(), Boolean.TRUE) != null) {
            log.info("群众报灾重复提交已忽略, idempotentKey={}", context.key());
            return;
        }

        boolean releaseImmediately = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                releaseImmediately = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        PUBLIC_REPORT_SUBMIT_LOCKS.remove(context.key());
                    }
                });
            }

            DzReportDisaster duplicate = findRecentDuplicatePublicReport(context);
            if (duplicate != null) {
                log.info("群众报灾重复提交已忽略, idempotentKey={}, existedReportId={}",
                    context.key(), duplicate.getId());
                return;
            }

            persistDisasterAndTriggerAi(req, PUBLIC_REPORT_SOURCE, submitter.processType(), null, submitter);
        } finally {
            if (releaseImmediately) {
                PUBLIC_REPORT_SUBMIT_LOCKS.remove(context.key());
            }
        }
    }

    private PublicReportIdempotentContext buildPublicReportIdempotentContext(InspectionSubmitReq req) {
        Date checkTime = parseDateTime(req.getCheckInTime(), "打卡时间格式错误");
        String photos = buildReportPhotoStr(req);
        Date duplicateAfter = new Date(System.currentTimeMillis() - publicReportIdempotentWindowMillis());
        String rawKey = String.join("|",
            keyPart(req.getInspectorId()),
            keyPart(req.getInspectorName()),
            keyPart(req.getInspectorPhone()),
            keyPart(checkTime.getTime()),
            keyPart(req.getCheckInCenter()),
            keyPart(req.getTextRecord()),
            keyPart(photos));
        return new PublicReportIdempotentContext(
            SecureUtil.md5(rawKey),
            req.getInspectorId(),
            req.getInspectorName(),
            req.getInspectorPhone(),
            checkTime,
            req.getCheckInCenter(),
            req.getTextRecord(),
            photos,
            duplicateAfter);
    }

    private long publicReportIdempotentWindowMillis() {
        Long seconds = appTaskProps.getPublicReportIdempotentWindowSeconds();
        if (seconds == null || seconds <= 0) {
            seconds = DEFAULT_PUBLIC_REPORT_IDEMPOTENT_WINDOW_SECONDS;
        }
        return Duration.ofSeconds(seconds).toMillis();
    }

    private DzReportDisaster findRecentDuplicatePublicReport(PublicReportIdempotentContext context) {
        LambdaQueryWrapper<DzReportDisaster> wrapper = Wrappers.<DzReportDisaster>lambdaQuery()
            .and(query -> query
                .eq(DzReportDisaster::getSourceType, PUBLIC_REPORT_SOURCE)
                .isNull(DzReportDisaster::getTaskId)
                .in(DzReportDisaster::getProcessType, PATROL_REPORT_PROCESS_TYPE, DIRECT_TOWN_PROCESS_TYPE)
                .or()
                .eq(DzReportDisaster::getSourceType, TASK_FEEDBACK_REPORT_SOURCE)
                .isNull(DzReportDisaster::getTaskId)
                .eq(DzReportDisaster::getProcessType, DIRECT_TOWN_PROCESS_TYPE))
            .ge(DzReportDisaster::getCreateDate, context.duplicateAfter())
            .orderByDesc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            .last("limit 1");
        eqNullable(wrapper, DzReportDisaster::getUserId, context.userId());
        eqNullable(wrapper, DzReportDisaster::getUserName, context.userName());
        eqNullable(wrapper, DzReportDisaster::getUserPhone, context.userPhone());
        eqNullable(wrapper, DzReportDisaster::getCheckTime, context.checkTime());
        eqNullable(wrapper, DzReportDisaster::getCheckCenter, context.checkCenter());
        eqNullable(wrapper, DzReportDisaster::getSceneTextRecord, context.sceneTextRecord());
        eqNullable(wrapper, DzReportDisaster::getPhotos, context.photos());
        List<DzReportDisaster> reports = dzReportDisasterMapper.selectList(wrapper);
        return CollUtil.isEmpty(reports) ? null : reports.getFirst();
    }

    private static <T> void eqNullable(LambdaQueryWrapper<DzReportDisaster> wrapper,
                                       SFunction<DzReportDisaster, T> column, T value) {
        if (value == null) {
            wrapper.isNull(column);
            return;
        }
        wrapper.eq(column, value);
    }

    private static String keyPart(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record PublicReportIdempotentContext(String key, Long userId, String userName, String userPhone,
                                                 Date checkTime, String checkCenter, String sceneTextRecord,
                                                 String photos, Date duplicateAfter) {
    }

    private record ResolvedPublicReportSubmitter(Integer processType, String displayRole) {
    }

    private DzTaskDistList buildPatrolTask(Long defId, Long handleId, String unitId,
                                           MonitorFrequencyParams freqParams, Date now) {
        TaskAssignee assignee = resolveSlopeUnitAssignee(unitId);
        DzTaskDistList task = new DzTaskDistList();
        task.setDefId(defId);
        task.setHandleId(handleId);
        task.setUnitId(unitId);
        task.setRiskId(findLatestRiskIdBySlopeUnitId(unitId));
        task.setPlanName(DzTaskDistList.PLAN_NAME_PATROL);
        task.setTaskSource(buildDefRespTaskSource(defId));
        task.setSubmitRequire(INSPECTING_REQUIRE);
        task.setInspectionSuggestion("每日至少开展" + freqParams.patrolTimes() + "次巡查，重点排查河道、边坡、危房等易受气象灾害影响区域，及时上报隐患并协助避险。");
        task.setUserId(assignee.userId());
        task.setResponsiblePerson(assignee.userName());
        task.setResponsiblePersonPhone(assignee.phoneNumber());
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        task.setTaskType(DzTaskDistListServiceImpl.resolveStoredTaskType(task.getSourceType()));
        taskSmsContentService.populateSmsContent(task);
        return task;
    }

    private String buildDefRespTaskSource(Long defId) {
        DefRespPlan plan = defId == null ? null : dzDefRespPlanMapper.selectById(defId);
        return plan != null && StringUtils.hasText(plan.getName()) ? plan.getName() : "防御响应";
    }

    private Long findLatestRiskIdBySlopeUnitId(String slopeUnitId) {
        if (!StringUtils.hasText(slopeUnitId)) {
            return null;
        }
        return dzRiskAssessmentMapper.selectList(
                                         Wrappers.<DzRiskAssessment>lambdaQuery()
                                                 .eq(DzRiskAssessment::getSlopeUnitId, slopeUnitId.trim())
                                                 .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId)
                                                 .select(DzRiskAssessment::getId)
                                                 .last("limit 1")
                                     )
                                     .stream()
                                     .map(DzRiskAssessment::getId)
                                     .findFirst()
                                     .orElse(null);
    }

    private record MonitorFrequencyParams(String monitorFrequency, String patrolTimes, int intervalHours) {
    }

    private record TaskAssignee(Long userId, String userName, String phoneNumber) {
    }

    private record SubmitTaskUpdateResult(DzTaskDistList task, boolean shouldCreateReport,
                                          boolean shouldCreateTechAssistance, Long historyId) {
    }

    /**
     * 创建灾害报告对象
     */
    private DzReportDisaster createDzReportDisaster(InspectionSubmitReq req, Integer reportSource) {
        return createDzReportDisaster(req, reportSource, DIRECT_TOWN_PROCESS_TYPE, null);
    }

    private DzReportDisaster createDzReportDisaster(InspectionSubmitReq req, Integer reportSource,
                                                    Integer processType,
                                                    ResolvedPublicReportSubmitter submitter) {
        DzReportDisaster rd = new DzReportDisaster();
        rd.setUserId(req.getInspectorId());
        rd.setUserName(req.getInspectorName());
        rd.setUserPhone(req.getInspectorPhone());
        String role = submitter == null ? req.getRole() : submitter.displayRole();
        if (submitter == null && !StringUtils.hasText(req.getRole())) {
            SysUserExportVo userExportVo = resolveInspectorUser(req);
            // 获取用户角色
            role = sysUserService.selectUserRoleGroup(userExportVo.getUserId());
        }
        rd.setUserRole(role);
        rd.setCheckTime(new Date(DateUtil.parseDateTime(req.getCheckInTime()).getTime()));
        rd.setCheckCenter(req.getCheckInCenter());
        rd.setCheckCenterLocation(resolveCheckCenterLocation(req));
        rd.setDetailedAddress(disasterDetailedAddressResolver.resolve(req.getCheckInCenter()));

        String photoStr = buildReportPhotoStr(req);
        rd.setPhotos(photoStr);

        rd.setSceneTextRecord(req.getTextRecord());
        // 默认为待处理
        rd.setStatus(1);
        // 1:任务反馈, 2:群众报灾
        rd.setSourceType(reportSource);
        rd.setProcessType(processType);
        rd.setTaskId(req.getTaskId());
        rd.setCreateDate(new Date());
        rd.setUpdateDate(new Date());
        return rd;
    }

    private String buildReportPhotoStr(InspectionSubmitReq req) {
        if (req == null || CollUtil.isEmpty(req.getPhotos())) {
            return "";
        }
        List<String> validPhotos = req.getPhotos().stream()
                                      .filter(StrUtil::isNotBlank)
                                      .toList();
        if (CollUtil.isEmpty(validPhotos)) {
            return "";
        }
        return String.join(",", validPhotos);
    }

    private void persistDisasterAndTriggerAi(InspectionSubmitReq req, Integer reportSource, Long feedbackNodeId) {
        persistDisasterAndTriggerAi(req, reportSource, feedbackNodeId, null);
    }

    private void persistDisasterAndTriggerAi(InspectionSubmitReq req, Integer reportSource, Long feedbackNodeId,
                                             ResolvedPublicReportSubmitter submitter) {
        Integer processType = submitter == null ? DIRECT_TOWN_PROCESS_TYPE : submitter.processType();
        persistDisasterAndTriggerAi(req, reportSource, processType, feedbackNodeId, submitter);
    }

    private void persistDisasterAndTriggerAi(InspectionSubmitReq req, Integer reportSource, Integer processType,
                                             Long feedbackNodeId, ResolvedPublicReportSubmitter submitter) {
        DzReportDisaster disaster = createDzReportDisaster(req, reportSource, processType, submitter);
        dzReportDisasterMapper.insert(disaster);
        recordReportSubmitChain(req, reportSource, disaster, feedbackNodeId);
        triggerAiIdentificationAfterCommit(disaster.getId(), disaster.getPhotos());
    }

    private DzTaskDistList createTechAssistanceTaskFromSubmit(DzTaskDistList originTask, InspectionSubmitReq req,
                                                              Long applyNodeId) {
        if (originTask == null) {
            throw new ServiceException("原任务不存在，无法申请技术协查");
        }
        String taskCheckCenter = StrUtil.trim(req.getCheckInCenter());
        SlopeUnit slopeUnit = resolveSlopeUnitByCheckCenter(taskCheckCenter, "无法申请技术协查");
        String slopeUnitId = slopeUnit.getId();
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(slopeUnitId);
        if (relationVo == null || StrUtil.isBlank(relationVo.getAssistantManagerPhone())) {
            throw new ServiceException("当前斜坡单元未绑定协管员，无法生成技术协查任务");
        }
        String assistantManagerPhone = relationVo.getAssistantManagerPhone().trim();
        var sysUserVo = sysUserService.selectUserByPhonenumber(assistantManagerPhone);
        if (sysUserVo == null) {
            throw new ServiceException("协管员未注册系统账号，手机号: " + assistantManagerPhone);
        }

        Date now = new Date();
        DzTaskDistList task = new DzTaskDistList();
        task.setUnitId(slopeUnitId);
        task.setCheckCenter(taskCheckCenter);
        task.setRiskId(findLatestRiskIdBySlopeUnitId(slopeUnitId));
        task.setUserId(sysUserVo.getUserId());
        task.setResponsiblePerson(StrUtil.isNotBlank(relationVo.getAssistantManager()) ? relationVo.getAssistantManager() : sysUserVo.getUserName());
        task.setResponsiblePersonPhone(assistantManagerPhone);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE);
        task.setRelatedTaskId(originTask.getId());
        task.setReportId(null);
        task.setReportInfo(null);
        task.setTaskSource(buildTechAssistanceTaskSource(req, slopeUnitId));
        task.setSubmitRequire(INSPECTING_REQUIRE);
        task.setInspectionSuggestion("请尽快开展技术协查并反馈核实结果");
        task.setInspectionSuggestionBackup("请尽快开展技术协查并反馈核实结果");
        task.setDetailedAddress(resolveTaskDetailedAddress(taskCheckCenter, slopeUnit, originTask.getDetailedAddress()));
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(task, now, true);
        task.setTaskType(DzTaskDistListServiceImpl.resolveStoredTaskType(task.getSourceType()));
        taskSmsContentService.populateSmsContent(task);
        dzTaskDistListMapper.insert(task);

        String chainId = resolveSubmitTaskChainId(originTask);
        Long resolvedApplyNodeId = applyNodeId;
        if (resolvedApplyNodeId == null) {
            resolvedApplyNodeId = recordNode(chainId, TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(),
                TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getTriggerReason(), req.getInspectorId(), req.getInspectorName(),
                TaskProcessBizTypeEnum.TASK.getCode(), originTask.getId(), originTask.getId(),
                TaskProcessSourceTypeEnum.fromTaskSourceType(originTask.getSourceType()).getCode());
        }
        recordNode(chainId, TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getTriggerReason(),
            DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID, DzTaskDistListServiceImpl.AUTO_AGENT_NAME,
            TaskProcessBizTypeEnum.TASK.getCode(), task.getId(), task.getId(),
            TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(), resolvedApplyNodeId, null,
            TaskProcessNodeCategoryEnum.TASK_PUSH.getCode());
        return task;
    }

    private SlopeUnit resolveSlopeUnitByCheckCenter(String checkCenter, String action) {
        if (StrUtil.isBlank(checkCenter)) {
            throw new ServiceException("打卡地点为空，" + action);
        }
        SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(checkCenter);
        if (slopeUnit == null || StrUtil.isBlank(slopeUnit.getId())) {
            throw new ServiceException("本次打卡地点未匹配到斜坡单元，" + action);
        }
        return slopeUnit;
    }

    private String resolveTaskDetailedAddress(String checkCenter, SlopeUnit slopeUnit, String fallback) {
        String detailedAddress = disasterDetailedAddressResolver.resolve(checkCenter);
        if (StrUtil.isNotBlank(detailedAddress)) {
            return detailedAddress;
        }
        if (slopeUnit != null && StrUtil.isNotBlank(slopeUnit.getDetailedAddress())) {
            return slopeUnit.getDetailedAddress();
        }
        return fallback;
    }

    private String buildTechAssistanceTaskSource(InspectionSubmitReq req, String slopeUnitId) {
        String role = req.getRole();
        if (StrUtil.isBlank(role)) {
            try {
                SysUserExportVo userExportVo = resolveInspectorUser(req);
                role = sysUserService.selectUserRoleGroup(userExportVo.getUserId());
            } catch (Exception e) {
                log.warn("获取提交人角色失败，技术协查任务来源描述将不包含角色, inspectorId={}", req.getInspectorId(), e);
            }
        }
        return Stream.of(role, req.getInspectorName())
                     .filter(StrUtil::isNotBlank)
                     .map(String::trim)
                     .collect(Collectors.joining())
            + "在"
            + normalizeSlopeUnitId(slopeUnitId)
            + "号斜坡单元发现地灾迹象，申请技术协查";
    }

    private void recordReportSubmitChain(InspectionSubmitReq req, Integer reportSource, DzReportDisaster disaster,
                                         Long feedbackNodeId) {
        if (disaster == null || disaster.getId() == null) {
            return;
        }
        if (Objects.equals(reportSource, TASK_FEEDBACK_REPORT_SOURCE) && req.getTaskId() != null) {
            DzTaskDistList task = dzTaskDistListMapper.selectById(req.getTaskId());
            if (task != null) {
                if (task.getReportId() == null) {
                    DzTaskDistList update = new DzTaskDistList();
                    update.setId(task.getId());
                    update.setReportId(disaster.getId());
                    update.setUpdateDate(new Date());
                    dzTaskDistListMapper.updateById(update);
                }
                String chainId = resolveSubmitTaskChainId(task);
                Long parentNodeId = feedbackNodeId != null
                    ? feedbackNodeId
                    : recordNode(chainId, TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
                        TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(), task.getUserId(), task.getResponsiblePerson(),
                        TaskProcessBizTypeEnum.TASK.getCode(), task.getId(), task.getId(),
                        TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(), task.getTaskType());
                String feedbackReportLinkName = TaskProcessChainNodeTextEnum.refineTaskFeedbackReportLinkName(
                    TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(), task.getTaskType());
                recordNode(chainId, feedbackReportLinkName,
                    TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getTriggerReason(), disaster.getUserId(), disaster.getUserName(),
                    TaskProcessBizTypeEnum.REPORT.getCode(), disaster.getId(), task.getId(),
                    TaskProcessSourceTypeEnum.fromReportSourceType(disaster.getSourceType()).getCode(), parentNodeId, null,
                    TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
            }
            return;
        }
        if (Objects.equals(reportSource, TASK_FEEDBACK_REPORT_SOURCE)) {
            recordNode(taskProcessChainNodeService.generateChainId(),
                TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(),
                TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getTriggerReason(), disaster.getUserId(),
                disaster.getUserName(), TaskProcessBizTypeEnum.REPORT.getCode(), disaster.getId(), null,
                TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), null,
                TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
            return;
        }
        if (Objects.equals(reportSource, 2)) {
            String rootLinkName = Objects.equals(disaster.getProcessType(), DIRECT_TOWN_PROCESS_TYPE)
                ? TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName()
                : TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName();
            String rootTriggerReason = Objects.equals(disaster.getProcessType(), DIRECT_TOWN_PROCESS_TYPE)
                ? TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getTriggerReason()
                : TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getTriggerReason();
            recordNode(taskProcessChainNodeService.generateChainId(),
                rootLinkName,
                rootTriggerReason, disaster.getUserId(), disaster.getUserName(),
                TaskProcessBizTypeEnum.REPORT.getCode(), disaster.getId(), null,
                TaskProcessSourceTypeEnum.fromReportSourceType(disaster.getSourceType()).getCode(), null,
                TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        }
    }

    private Long recordSubmitTaskChainNode(DzTaskDistList task, InspectionSubmitReq req, Long historyId) {
        if (task == null || task.getId() == null || req == null) {
            return null;
        }
        if (shouldSkipSubmitTaskChainNode(task)) {
            return null;
        }
        TaskProcessChainNodeTextEnum text = resolveSubmitTaskText(task, task.getStatus());
        if (text == null || StrUtil.isBlank(text.getLinkName())) {
            return null;
        }
        Long bizId = Objects.equals(task.getStatus(), DzTaskDistList.STATUS_INSPECTING) && historyId != null
            ? historyId
            : task.getId();
        return recordNode(resolveSubmitTaskChainId(task), text.getLinkName(), text.getTriggerReason(), req.getInspectorId(),
            StrUtil.blankToDefault(req.getInspectorName(), task.getResponsiblePerson()),
            TaskProcessBizTypeEnum.TASK.getCode(), bizId, task.getId(),
            TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(),
            req.getRole(), TaskProcessNodeCategoryEnum.codeFromLabel(text.getLinkName()), task.getTaskType());
    }

    static boolean shouldSkipSubmitTaskChainNode(DzTaskDistList task) {
        return task != null
            && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)
            && Objects.equals(task.getStatus(), DzTaskDistList.STATUS_CLOSED);
    }

    private TaskProcessChainNodeTextEnum resolveSubmitTaskText(DzTaskDistList task, Integer status) {
        if (Objects.equals(status, DzTaskDistList.STATUS_INSPECTING)) {
            return TaskProcessChainNodeTextEnum.TASK_INSPECTING;
        }
        if (Objects.equals(status, DzTaskDistList.STATUS_CLOSED)) {
            return TaskProcessChainNodeTextEnum.TASK_CLOSE_BY_APP;
        }
        if (Objects.equals(status, DzTaskDistList.STATUS_FEEDBACKED)) {
            if (task != null && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
                return TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD;
            }
            return TaskProcessChainNodeTextEnum.TASK_FEEDBACK;
        }
        if (Objects.equals(status, DzTaskDistList.STATUS_TECH_ASSISTANCE)) {
            return TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY;
        }
        return null;
    }

    private String resolveSubmitTaskChainId(DzTaskDistList task) {
        if (task == null || task.getId() == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByTaskId(task.getId());
        if (StrUtil.isNotBlank(chainId)) {
            return chainId;
        }
        if (task.getRelatedTaskId() != null) {
            chainId = taskProcessChainNodeService.resolveSavedChainIdByTaskId(task.getRelatedTaskId());
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_REPORT) && task.getReportId() != null) {
            chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.REPORT.getCode(), task.getReportId());
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            chainId = resolveDefRespTaskChainId(task);
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            chainId = resolveHandleTaskChainId(task.getHandleId());
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        throw new ServiceException("任务提交未找到已有流程链路, taskId=" + task.getId());
    }

    private String resolveDefRespTaskChainId(DzTaskDistList task) {
        DefRespPlan plan = task.getDefId() == null ? null : dzDefRespPlanMapper.selectById(task.getDefId());
        if (plan != null && plan.getSourceAlarmId() != null) {
            String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.ALARM.getCode(), plan.getSourceAlarmId());
            return chainId;
        }
        if (plan != null && plan.getHandleId() != null) {
            String chainId = resolveHandleTaskChainId(plan.getHandleId());
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        if (task.getHandleId() != null) {
            String chainId = resolveHandleTaskChainId(task.getHandleId());
            if (StrUtil.isNotBlank(chainId)) {
                return chainId;
            }
        }
        Long defId = plan == null ? task.getDefId() : plan.getId();
        if (defId == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.DEF_RESP.getCode(), defId);
        return chainId;
    }

    private String resolveHandleTaskChainId(Long handleId) {
        if (handleId == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), handleId);
        return chainId;
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType) {
        return recordNode(chainId, linkName, triggerReason, operatorId, operatorName, bizType, bizId, taskId,
            sourceType, null, null);
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType, String taskType) {
        return recordNode(chainId, linkName, triggerReason, operatorId, operatorName, bizType, bizId, taskId,
            sourceType, null, null, taskType);
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType,
                            String operatorRole, Integer nodeCategory) {
        return recordNode(chainId, linkName, triggerReason, operatorId, operatorName, bizType, bizId, taskId,
            sourceType, operatorRole, nodeCategory, null);
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType,
                            String operatorRole, Integer nodeCategory, String taskType) {
        return recordNode(chainId, linkName, triggerReason, operatorId, operatorName, bizType, bizId, taskId,
            sourceType, null, operatorRole, nodeCategory, taskType);
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType, Long parentNodeId,
                            String operatorRole, Integer nodeCategory) {
        return recordNode(chainId, linkName, triggerReason, operatorId, operatorName, bizType, bizId, taskId,
            sourceType, parentNodeId, operatorRole, nodeCategory, null);
    }

    private Long recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Integer bizType, Long bizId, Long taskId, Integer sourceType, Long parentNodeId,
                            String operatorRole, Integer nodeCategory, String taskType) {
        return taskProcessChainNodeService.recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId,
            operatorId, operatorName, sourceType, parentNodeId, operatorRole, nodeCategory, null, taskType);
    }

    private void triggerAiIdentificationAfterCommit(Long disasterId, String photos) {
        if (disasterId == null || StrUtil.isBlank(photos)) {
            return;
        }
        Runnable task = () -> CompletableFuture.runAsync(() -> aiIdentificationPicture(disasterId, photos), Run.executor)
                                               .exceptionally(ex -> {
                                                   log.error("异步AI图片识别任务执行异常, disasterId={}", disasterId, ex);
                                                   return null;
                                               });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private SysUserExportVo resolveInspectorUser(InspectionSubmitReq req) {
        SysUserBo sysUserBo = new SysUserBo();
        sysUserBo.setUserId(req.getInspectorId());
        sysUserBo.setPhonenumber(req.getInspectorPhone());
        List<SysUserExportVo> sysUserExportVos = sysUserService.selectUserExportList(sysUserBo);
        if (CollUtil.isEmpty(sysUserExportVos)) {
            throw new ServiceException("未查询到巡查人员信息");
        }
        return sysUserExportVos.getFirst();
    }

    private ResolvedPublicReportSubmitter resolvePublicReportSubmitter(InspectionSubmitReq req) {
        String fallbackRole = req == null ? null : req.getRole();
        String requestedPhone = req == null ? null : StrUtil.trim(req.getInspectorPhone());
        if (req == null || req.getInspectorId() == null || StrUtil.isBlank(requestedPhone)) {
            log.warn("群众报灾提交人账号标识不完整，降级群众报灾来源, inspectorId={}, inspectorPhone={}",
                req == null ? null : req.getInspectorId(), req == null ? null : req.getInspectorPhone());
            return new ResolvedPublicReportSubmitter(PATROL_REPORT_PROCESS_TYPE, fallbackRole);
        }
        try {
            SysUserExportVo matchedUser = resolvePublicReportInspectorUser(req, requestedPhone);
            if (matchedUser == null || matchedUser.getUserId() == null) {
                log.warn("群众报灾提交人账号与请求标识无精确匹配，降级群众报灾来源, inspectorId={}, inspectorPhone={}",
                    req.getInspectorId(), req.getInspectorPhone());
                return new ResolvedPublicReportSubmitter(PATROL_REPORT_PROCESS_TYPE, fallbackRole);
            }
            SysUserVo user = sysUserService.selectUserById(matchedUser.getUserId());
            if (user == null || CollUtil.isEmpty(user.getRoles())) {
                log.warn("群众报灾提交人无可用角色，降级群众报灾来源, resolvedUserId={}", matchedUser.getUserId());
                return new ResolvedPublicReportSubmitter(PATROL_REPORT_PROCESS_TYPE, fallbackRole);
            }
            boolean patrolInspector = user.getRoles().stream()
                .filter(Objects::nonNull)
                .anyMatch(role -> Objects.equals(role.getRoleKey(), SysRoleEnum.DZ_FXQXCY.getRoleKey()));
            String displayRole = user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(SysRoleVo::getRoleName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
            return new ResolvedPublicReportSubmitter(
                patrolInspector ? DIRECT_TOWN_PROCESS_TYPE : PATROL_REPORT_PROCESS_TYPE,
                StringUtils.hasText(displayRole) ? displayRole : fallbackRole);
        } catch (Exception e) {
            log.warn("群众报灾提交人角色解析异常，降级群众报灾来源, inspectorId={}, inspectorPhone={}",
                req == null ? null : req.getInspectorId(), req == null ? null : req.getInspectorPhone(), e);
            return new ResolvedPublicReportSubmitter(PATROL_REPORT_PROCESS_TYPE, fallbackRole);
        }
    }

    private SysUserExportVo resolvePublicReportInspectorUser(InspectionSubmitReq req, String requestedPhone) {
        SysUserBo sysUserBo = new SysUserBo();
        sysUserBo.setUserId(req.getInspectorId());
        sysUserBo.setPhonenumber(requestedPhone);
        List<SysUserExportVo> sysUserExportVos = sysUserService.selectUserExportList(sysUserBo);
        if (CollUtil.isEmpty(sysUserExportVos)) {
            return null;
        }
        return sysUserExportVos.stream()
            .filter(Objects::nonNull)
            .filter(user -> Objects.equals(user.getUserId(), req.getInspectorId())
                && Objects.equals(StrUtil.trim(user.getPhonenumber()), requestedPhone))
            .findFirst()
            .orElse(null);
    }

    private String resolveCheckCenterLocation(InspectionSubmitReq req) {
        if (req == null || StrUtil.isBlank(req.getCheckInCenter())) {
            return req != null ? req.getCheckCenterLocation() : null;
        }
        try {
            SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(req.getCheckInCenter());
            String location = buildCheckCenterLocation(slopeUnit);
            return StrUtil.isNotBlank(location) ? location : req.getCheckCenterLocation();
        } catch (Exception e) {
            log.warn("根据打卡坐标匹配斜坡单元位置失败, checkInCenter={}", req.getCheckInCenter(), e);
            return req.getCheckCenterLocation();
        }
    }

    private String buildCheckCenterLocation(SlopeUnit slopeUnit) {
        if (slopeUnit == null || StrUtil.isBlank(slopeUnit.getId())) {
            return null;
        }
        return Stream.of(
                         StrUtil.trim(slopeUnit.getStreet()),
                         StrUtil.blankToDefault(StrUtil.trim(slopeUnit.getVillage()), StrUtil.trim(slopeUnit.getCommunity())),
                         normalizeSlopeUnitId(slopeUnit.getId()) + "号斜坡单元"
                     )
                     .filter(StrUtil::isNotBlank)
                     .collect(Collectors.joining());
    }

    private void aiIdentificationPicture(Long disasterId, String photos) {
        try {
            DifyChatflowClient aiPictureAgent = difyAgentClient.getAiPictureAgent();
            ChatMessage msg = new ChatMessage();
            msg.setQuery("识别图片");
            msg.setResponseMode(ResponseMode.BLOCKING);
            msg.setUser("admin");
            msg.setConversationId("");

            List<String> photoOssIds = extractFirstPhotoOssIds(photos);
            List<FileInfo> fils = buildAiPictureFiles(aiPictureAgent, disasterId, photoOssIds);
            if (CollUtil.isEmpty(fils)) {
                log.warn("AI图片识别无可用上传图片，使用兜底结果, disasterId={}", disasterId);
                updateAiPictureResult(disasterId, buildFallbackAiPictureDto("no_uploaded_image", photoOssIds.size()));
                return;
            }

            msg.setFiles(fils);

            ChatMessageResponse chatRes = aiPictureAgent.sendChatMessage(msg);
            String answer = chatRes == null ? null : chatRes.getAnswer();
            log.info("AI图片识别 响应：{}", answer);
            AiPictureDto aiPictureDto = parseAiPictureAnswer(answer, disasterId);
            updateAiPictureResult(disasterId, normalizeAiPictureDto(aiPictureDto, "empty_or_incomplete_ai_response", photoOssIds.size()));
        } catch (Exception e) {
            log.error("AI图片识别异常, disasterId={}", disasterId, e);
            updateAiPictureFallbackSafely(disasterId, "ai_identification_exception", countValidPhotoIds(photos));
        }
    }

    private List<FileInfo> buildAiPictureFiles(DifyChatflowClient aiPictureAgent, Long disasterId, List<String> photoOssIds) {
        if (CollUtil.isEmpty(photoOssIds)) {
            return List.of();
        }
        List<OssDTO> ossDtos = ossService.selectByIds(String.join(",", photoOssIds));
        if (CollUtil.isEmpty(ossDtos)) {
            return List.of();
        }
        return ossDtos.stream()
                      .map(ossdto -> uploadAiPictureFile(aiPictureAgent, disasterId, ossdto))
                      .filter(ObjUtil::isNotNull)
                      .toList();
    }

    private FileInfo uploadAiPictureFile(DifyChatflowClient aiPictureAgent, Long disasterId, OssDTO ossdto) {
        try {
            FileUploadRequest fur = FileUploadRequest.builder().user("admin").build();
            String fileName = ossdto.getFileName();
            OssClient storage = OssFactory.instance();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            storage.download(fileName, baos, null);

            AiUploadImage aiUploadImage = prepareAiUploadImage(
                baos.toByteArray(),
                ossdto.getOriginalName(),
                disasterId,
                Convert.toStr(ossdto.getOssId())
            );
            FileUploadResponse fres = aiPictureAgent.uploadFile(
                fur,
                new ByteArrayInputStream(aiUploadImage.bytes()),
                aiUploadImage.uploadFileName()
            );
            if (fres == null || StrUtil.isBlank(fres.getId())) {
                throw new ServiceException("AI图片识别 上传文件异常");
            }
            return FileInfo.builder()
                           .type(FileType.IMAGE)
                           .transferMethod(FileTransferMethod.LOCAL_FILE)
                           .uploadFileId(fres.getId())
                           .build();
        } catch (Exception e) {
            log.error("AI图片识别 上传文件异常, disasterId={}, ossId={}", disasterId, ossdto == null ? null : ossdto.getOssId(), e);
            return null;
        }
    }

    private AiPictureDto parseAiPictureAnswer(String answer, Long disasterId) {
        if (StrUtil.isBlank(answer)) {
            log.warn("AI图片识别结果为空, disasterId={}", disasterId);
            return null;
        }
        try {
            return JsonUtils.parseObject(answer, AiPictureDto.class);
        } catch (Exception e) {
            log.warn("AI图片识别结果解析失败, disasterId={}, answer={}", disasterId, answer, e);
            return null;
        }
    }

    private void updateAiPictureFallbackSafely(Long disasterId, String reason, int photoCount) {
        try {
            updateAiPictureResult(disasterId, buildFallbackAiPictureDto(reason, photoCount));
        } catch (Exception updateException) {
            log.error("AI图片识别兜底结果回写失败, disasterId={}", disasterId, updateException);
        }
    }

    private void updateAiPictureResult(Long disasterId, AiPictureDto aiPictureDto) {
        DzReportDisaster update = new DzReportDisaster();
        update.setId(disasterId);
        applyAiPictureResult(update, aiPictureDto);
        update.setUpdateDate(new Date());
        dzReportDisasterMapper.updateById(update);
        refreshAiPictureRiskLevelSummary(disasterId);
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "ai_vision",
            "ai_picture_result_updated",
            "dz_report_disaster",
            disasterId,
            null
        );
    }

    private void refreshAiPictureRiskLevelSummary(Long disasterId) {
        if (disasterId == null) {
            return;
        }
        try {
            taskProcessChainSummaryService.refreshReportRiskLevelIfCurrent(disasterId);
        } catch (Exception e) {
            log.warn("AI识图风险等级回写后刷新流程链路风险等级失败, disasterId={}", disasterId, e);
        }
    }

    private static void applyAiPictureResult(DzReportDisaster disaster, AiPictureDto aiPictureDto) {
        AiPictureDto normalized = normalizeAiPictureDto(aiPictureDto, "empty_or_incomplete_ai_response", 0);
        disaster.setAiRiskLevel(normalized.getAiRiskLevel());
        disaster.setAiRiskLabel(normalized.getAiRiskLabel());
        disaster.setAiReportDetail(normalized.getAiReportDetail());
        disaster.setAiVisionProps(normalized.getAiVisionProps());
    }

    static AiPictureDto normalizeAiPictureDto(AiPictureDto source, String reason, int photoCount) {
        AiPictureDto result = source == null ? new AiPictureDto() : source;
        Map<String, Object> props = result.getAiVisionProps();
        boolean fallbackApplied = !isCompleteAiPictureResult(result) || AiVisionStatusUtils.hasFallbackMarker(props);
        if (fallbackApplied && !isValidAiRiskLevel(result.getAiRiskLevel())) {
            result.setAiRiskLevel(AI_PICTURE_FALLBACK_RISK_LEVEL);
        }
        if (fallbackApplied && StrUtil.isBlank(result.getAiRiskLabel())) {
            result.setAiRiskLabel(AI_PICTURE_FALLBACK_RISK_LABEL);
        }
        if (fallbackApplied && StrUtil.isBlank(result.getAiReportDetail())) {
            result.setAiReportDetail(AI_PICTURE_FALLBACK_REPORT_DETAIL);
        }

        if (fallbackApplied) {
            Map<String, Object> normalizedProps = AiVisionStatusUtils.isPropsBlank(props) ? new LinkedHashMap<>() : new LinkedHashMap<>(props);
            normalizedProps.putIfAbsent("fallbackApplied", true);
            normalizedProps.putIfAbsent("fallbackReason", StrUtil.blankToDefault(reason, "empty_or_incomplete_ai_response"));
            normalizedProps.putIfAbsent("photoCount", Math.max(photoCount, 0));
            normalizedProps.putIfAbsent("fallbackMessage", AI_PICTURE_FALLBACK_REPORT_DETAIL);
            result.setAiVisionProps(normalizedProps);
        }
        return result;
    }

    static AiPictureDto buildFallbackAiPictureDto(String reason, int photoCount) {
        return normalizeAiPictureDto(null, reason, photoCount);
    }

    private static boolean isValidAiRiskLevel(Integer aiRiskLevel) {
        return aiRiskLevel != null && aiRiskLevel >= 1 && aiRiskLevel <= 4;
    }

    private static boolean isCompleteAiPictureResult(AiPictureDto result) {
        return result != null
            && isValidAiRiskLevel(result.getAiRiskLevel())
            && StrUtil.isNotBlank(result.getAiReportDetail())
            && !AiVisionStatusUtils.isPropsBlank(result.getAiVisionProps());
    }

    static AiUploadImage prepareAiUploadImage(byte[] originalBytes, String originalFileName, Long disasterId, String imageIdentifier) {
        if (originalBytes == null || originalBytes.length == 0) {
            return new AiUploadImage(originalBytes, originalFileName);
        }
        String imageFormat = resolveImageFormat(originalFileName);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes)) {
            BufferedImage bufferedImage = ImgUtil.read(inputStream);
            if (bufferedImage == null) {
                log.warn("AI图片识别 图片解析为空, disasterId={}, image={}", disasterId, imageIdentifier);
                return new AiUploadImage(originalBytes, normalizeUploadFileName(originalFileName, imageFormat));
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            int maxSide = Math.max(width, height);
            if (maxSide <= AI_IMAGE_MAX_SIDE) {
                return new AiUploadImage(originalBytes, normalizeUploadFileName(originalFileName, imageFormat));
            }

            double scale = (double) AI_IMAGE_MAX_SIDE / maxSide;
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));
            Image scaledImage = ImgUtil.scale(bufferedImage, targetWidth, targetHeight);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImgUtil.write(ImgUtil.toBufferedImage(scaledImage), imageFormat, outputStream);
            return new AiUploadImage(outputStream.toByteArray(), normalizeUploadFileName(originalFileName, imageFormat));
        } catch (Exception e) {
            log.warn("AI图片识别 图片缩放失败，回退原图上传, disasterId={}, image={}", disasterId, imageIdentifier, e);
            return new AiUploadImage(originalBytes, normalizeUploadFileName(originalFileName, imageFormat));
        }
    }

    static String resolveImageFormat(String originalFileName) {
        String extName = FileUtil.extName(originalFileName);
        return StrUtil.isBlank(extName) ? AI_IMAGE_FALLBACK_FORMAT : extName.toLowerCase();
    }

    static String normalizeUploadFileName(String originalFileName, String imageFormat) {
        String safeFormat = StrUtil.blankToDefault(imageFormat, AI_IMAGE_FALLBACK_FORMAT).toLowerCase();
        if (StrUtil.isBlank(originalFileName)) {
            return "ai-image." + safeFormat;
        }
        String mainName = FileUtil.mainName(originalFileName);
        String safeMainName = StrUtil.blankToDefault(mainName, "ai-image");
        return safeMainName + "." + safeFormat;
    }

    private List<String> extractFirstPhotoOssIds(String photos) {
        return StrUtil.splitTrim(photos, ',')
                      .stream()
                      .filter(StrUtil::isNotBlank)
                      .limit(3)
                      .toList();
    }

    static int countValidPhotoIds(String photos) {
        if (StrUtil.isBlank(photos)) {
            return 0;
        }
        return (int) StrUtil.splitTrim(photos, ',')
                            .stream()
                            .filter(StrUtil::isNotBlank)
                            .count();
    }

    @Override
    public List<String> uploadImages(Long taskId, MultipartFile[] files) {
        return Stream.of(files)
                     .map(sysOssService::upload)
                     .map(SysOssVo::getOssId)
                     .map(Convert::toStr)
                     .toList();
    }

    @Override
    public List<String> getPhotoPreviewUrls(List<String> ossIds) {
        if (CollUtil.isEmpty(ossIds)) {
            return List.of();
        }
        String idsStr = ossIds.stream()
                              .filter(StrUtil::isNotBlank)
                              .collect(Collectors.joining(","));
        if (StrUtil.isBlank(idsStr)) {
            return List.of();
        }
        List<OssDTO> ossDtos = ossService.selectByIds(idsStr);
        OssClient instance = OssFactory.instance();
        String publicBaseUrl = appTaskProps.getOssPhotoPreviewPublicUrl();
        return ossDtos.stream()
                      .map(e -> instance.createPresignedGetUrl(e.getFileName(), Duration.ofMinutes(10)))
                      .map(url -> toPublicUrlIfConfigured(url, publicBaseUrl))
                      .toList();
    }

    @SuppressWarnings("SameParameterValue")
    private String toPublicUrlIfConfigured(String url, String publicBaseUrl) {
        if (StrUtil.isBlank(publicBaseUrl) || StrUtil.isBlank(url)) {
            return url;
        }
        String base = publicBaseUrl.replaceAll("/$", "");
        return url.replaceFirst("^https?://[^/]+", base);
    }

    private boolean isAppApiEnabled(String actionName) {
        boolean enabled = Boolean.TRUE.equals(appTaskProps.getEnabled());
        if (!enabled) {
            log.info("APP接口开关关闭，跳过{}调用", actionName);
        }
        return !enabled;
    }

    private PushTaskResp buildSkippedResp(String actionName) {
        return PushTaskResp.builder()
                           .code(200)
                           .msg("当前环境已关闭APP接口调用，已跳过" + actionName)
                           .data(PushTaskDataResp.builder()
                                                 .receivedTaskIds(List.of())
                                                 .errorTaskIds(List.of())
                                                 .errorMessages(List.of())
                                                 .build())
                           .build();
    }

    @Override
    public PushTaskResp pushTask(List<InspectionTaskReq> req) {
        if (isAppApiEnabled("推送任务")) {
            return buildSkippedResp("推送任务");
        }
        var requestBody = InspectionTaskPushPayloads.toPayload(req);
        String requestJson = InspectionTaskPushPayloads.toJson(req);
        JsonNode node = Req.post(appTaskProps.getPrefixUrl())
                           .path("/v1/tasks/push")
                           .auth(AppTaskJob.APP_TOKEN)
                           .json(requestBody)
                           .ok()
                           .node();
        log.info("推送任务请求: {}", requestJson);
        log.info("推送任务结果: {}", node);
        return parseTaskBatchResp(node, "推送任务");
    }

    @Override
    public PushTaskResp updateTask(List<InspectionTaskReq> req) {
        if (CollUtil.isEmpty(req)) {
            return PushTaskResp.builder()
                               .code(200)
                               .msg("无需更新任务")
                               .data(PushTaskDataResp.builder()
                                                      .receivedTaskIds(List.of())
                                                      .errorTaskIds(List.of())
                                                      .errorMessages(List.of())
                                                      .build())
                               .build();
        }
        normalizeAppUpdateTaskStatus(req);
        if (isAppApiEnabled("更新任务")) {
            return buildSkippedResp("更新任务");
        }
        JsonNode node = Req.post(appTaskProps.getPrefixUrl())
                           .path("/v1/tasks/update")
                           .auth(AppTaskJob.APP_TOKEN)
                           .json(req)
                           .ok()
                           .node();
        return parseTaskBatchResp(node, "更新任务");
    }

    static void normalizeAppUpdateTaskStatus(List<InspectionTaskReq> req) {
        if (CollUtil.isEmpty(req)) {
            return;
        }
        for (InspectionTaskReq task : req) {
            Integer status = task == null ? null : task.getStatus();
            if (Objects.equals(status, DzTaskDistList.STATUS_CLOSED)
                || Objects.equals(status, DzTaskDistList.STATUS_FEEDBACKED)
                || Objects.equals(status, DzTaskDistList.STATUS_OVERDUE)) {
                continue;
            }
            throw new ServiceException("仅能修改app任务状态为已关闭或已过期");
        }
    }

    @Override
    public void remindTask(InspectionRemindReq req) {
        if (isAppApiEnabled("任务催办")) {
            return;
        }
        JsonNode node = Req.post(appTaskProps.getPrefixUrl())
                           .path("/v1/tasks/remind")
                           .auth(AppTaskJob.APP_TOKEN)
                           .json(req)
                           .ok()
                           .node();
        if (node == null || node.isMissingNode()) {
            throw new ServiceException("催办失败，响应为空");
        }
        JsonNode code = node.findPath("code");
        if (code.isMissingNode()) {
            throw new ServiceException("催办失败");
        }

        if (code.asInt() != 200) {
            throw new ServiceException("催办失败");
        }

    }

    @Override
    public Long getInvalidSubmitCount(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        AtomicLong counter = INVALID_SUBMIT_COUNTER.get(targetDate);
        return counter == null ? 0L : counter.get();
    }

    @Override
    public List<DzTaskDistListHistoryVo> feedbacks(Long taskId) {
        return dzTaskDistListHistoryMapper.selectListByTaskId(taskId).stream()
            .map(this::toHistoryVo)
            .toList();
    }

    private DzTaskDistListHistoryVo toHistoryVo(DzTaskDistListHistory history) {
        DzTaskDistListHistoryVo vo = MapstructUtils.convert(history, DzTaskDistListHistoryVo.class);
        if (vo != null) {
            vo.setRemark(vo.getTextRecord());
        }
        return vo;
    }

    private PushTaskResp parseTaskBatchResp(JsonNode node, String actionName) {
        if (node == null || node.isMissingNode()) {
            throw new ServiceException(actionName + "响应数据为空");
        }
        PushTaskResp resp = JsonUtils.parseObject(node.toString(), PushTaskResp.class);
        if (resp == null) {
            throw new ServiceException(actionName + "响应数据解析失败");
        }
        log.info("{}响应: {}", actionName, resp);

        PushTaskDataResp data = resp.getData();
        if (resp.getCode() != 200) {
            throw new ServiceException(StrUtil.blankToDefault(resp.getMsg(), actionName + "失败"));
        }
        if (data == null) {
            throw new ServiceException(actionName + "响应缺少结果明细");
        }
        if (CollUtil.isNotEmpty(data.getErrorTaskIds()) || CollUtil.isNotEmpty(data.getErrorMessages())) {
            throw new ServiceException(StrUtil.blankToDefault(resp.getMsg(), actionName + "失败"));
        }
        return resp;
    }

    private TaskAssignee resolveSlopeUnitAssignee(String unitId) {
        if (StrUtil.isBlank(unitId)) {
            throw new ServiceException("斜坡单元不能为空");
        }
        SlopeUnitContact contact = resolveSlopeUnitInspector(unitId.trim());
        if (contact == null || StrUtil.isBlank(contact.phoneNumber())) {
            throw new ServiceException("当前斜坡单元未绑定巡查人员，无法自动补派任务");
        }
        var sysUserVo = sysUserService.selectUserByPhonenumber(contact.phoneNumber());
        if (sysUserVo == null) {
            throw new ServiceException("斜坡单元绑定人员未注册系统账号，手机号: " + contact.phoneNumber());
        }
        return new TaskAssignee(
            sysUserVo.getUserId(),
            StrUtil.isNotBlank(contact.name()) ? contact.name() : sysUserVo.getUserName(),
            StrUtil.isNotBlank(contact.phoneNumber()) ? contact.phoneNumber() : sysUserVo.getPhonenumber()
        );
    }

    private SlopeUnitContact resolveSlopeUnitInspector(String unitId) {
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(unitId);
        if (relationVo != null) {
            SlopeUnitContact contact = firstAvailableContact(
                resolveInspectorOrSpecialManagerContact(relationVo),
                toContact(relationVo.getMonitor(), relationVo.getMonitorPhone()),
                toContact(relationVo.getResponsiblePerson(), relationVo.getResponsiblePersonPhone()),
                toContact(relationVo.getAdminUser(), relationVo.getAdminUserPhone())
            );
            if (contact != null) {
                return contact;
            }
        }
        SlopeUnitPersonVo person = slopeUnitService.getSlopeUnitPerson(unitId);
        if (person == null) {
            return null;
        }
        return toContact(person.getUserName(), person.getPhoneNumber());
    }

    private SlopeUnitContact resolveInspectorOrSpecialManagerContact(SlopeUnitGridMemberRelationVo relationVo) {
        if (StrUtil.isNotBlank(relationVo.getInspector())) {
            return toContact(relationVo.getInspector(), relationVo.getInspectorPhone());
        }
        return toContact(relationVo.getSpecialManager(), relationVo.getSpecialManagerPhone());
    }

    private SlopeUnitContact firstAvailableContact(SlopeUnitContact... contacts) {
        for (SlopeUnitContact contact : contacts) {
            if (contact != null && (StrUtil.isNotBlank(contact.name()) || StrUtil.isNotBlank(contact.phoneNumber()))) {
                return contact;
            }
        }
        return null;
    }

    private SlopeUnitContact toContact(String name, String phoneNumber) {
        if (StrUtil.isBlank(name) && StrUtil.isBlank(phoneNumber)) {
            return null;
        }
        return new SlopeUnitContact(name, phoneNumber);
    }

    record AiUploadImage(byte[] bytes, String uploadFileName) {
    }

    private record SlopeUnitContact(String name, String phoneNumber) {
    }
}
