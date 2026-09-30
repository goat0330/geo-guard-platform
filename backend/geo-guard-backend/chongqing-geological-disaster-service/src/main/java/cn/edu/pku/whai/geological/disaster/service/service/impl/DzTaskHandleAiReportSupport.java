/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AiModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.GenerateReviewReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EmergencyInvestigationReportVo;
import cn.edu.pku.whai.geological.disaster.service.handle.client.AiReportApiClient;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.orchestrator.EmergencyInvestigationReportOrchestrator;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import cn.edu.pku.whai.geological.disaster.service.utils.CoordinateConverter;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Component
@RequiredArgsConstructor
class DzTaskHandleAiReportSupport {

    private static final long AUTO_MODE_FALLBACK_USER_ID = 0L;
    private static final int AUTO_REPORT_ROUND_NO = 1;
    private static final String AUTO_REPORT_DEFAULT_PROMPT = "生成报告";
    private static final String DEFAULT_AI_ACTOR_NAME = "地象大模型";
    private static final String LEGACY_PLACEHOLDER_REPORT = "待补充应急调查报告";
    private static final String STREAM_EVENT_MESSAGE = "MESSAGE";
    private static final String STREAM_EVENT_WORKFLOW_FINISHED = "WORKFLOW_FINISHED";
    private static final boolean USE_ALGORITHM_REPORT_API = true;

    private final DifyAgentClient difyAgentClient;
    private final DzTaskHandleSceneRecordMapper dzTaskHandleSceneRecordMapper;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final IReceiveService receiveService;
    private final DzTaskHandlePermissionSupport permissionSupport;
    private final EmergencyInvestigationReportOrchestrator emergencyInvestigationReportOrchestrator;
    private final AiReportApiClient aiReportApiClient;
    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;

    @SneakyThrows
    String aiModifyReport(AiModifyReportBo bo, BiConsumer<Long, String> eventLevelUpdater) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        Long currentUserId = loginUser == null ? null : loginUser.getUserId();
        String currentUserName = loginUser == null ? null : StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        return generateEmergencyInvestigationReport(
            bo.getId(),
            bo.getPrompt(),
            currentUserId,
            currentUserName,
            true,
            USE_ALGORITHM_REPORT_API,
            eventLevelUpdater
        );
    }

    @SneakyThrows
    void aiModifyReportStream(SseEmitter sseEmitter, AiModifyReportBo bo, BiConsumer<Long, String> eventLevelUpdater) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        Long currentUserId = loginUser == null ? null : loginUser.getUserId();
        String currentUserName = loginUser == null ? null : StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        ReportGenerationContext context = buildEmergencyInvestigationReportContext(
            bo.getId(),
            bo.getPrompt(),
            true,
            USE_ALGORITHM_REPORT_API
        );
        if (USE_ALGORITHM_REPORT_API) {
            CompletableFuture.runAsync(() -> {
                try {
                    AiReportApiClient.AiReportResponse response = aiReportApiClient.reportStream(
                        toAiReportRequest(context),
                        chunk -> sendEmitter(sseEmitter, buildMessageStreamEvent(chunk, null))
                    );
                    String savedPlanContentJson = resolveAiReportPlanContentJson(
                        context.hasExistingReport(),
                        context.latestPlanContentJson(),
                        response.imagePlaceholdersJson()
                    );
                    saveGeneratedEmergencyInvestigationReport(
                        bo.getId(),
                        response.markdown(),
                        savedPlanContentJson,
                        currentUserId,
                        currentUserName,
                        context.hasExistingReport(),
                        eventLevelUpdater
                    );
                    sendEmitter(sseEmitter, buildWorkflowFinishedStreamEvent(
                        response.markdown(),
                        savedPlanContentJson,
                        null
                    ));
                    sseEmitter.complete();
                } catch (Exception e) {
                    sseEmitter.completeWithError(e);
                }
            }, Run.executor);
            return;
        }
        DifyChatflowClient agent = difyAgentClient.getAiReportAgent();
        ChatMessage message = ChatMessage.builder()
                                         .query(context.prompt())
                                         .inputs(context.inputs())
                                         .responseMode(ResponseMode.STREAMING)
                                         .user("admin")
                                         .conversationId("")
                                         .build();

        agent.sendChatMessageStream(message, new ChatflowStreamCallback() {
            @Override
            public void onMessage(MessageEvent event) {
                sendEmitter(sseEmitter, buildMessageStreamEvent(extractStreamAnswer(event), event));
            }

            @Override
            public void onWorkflowFinished(WorkflowFinishedEvent event) {
                try {
                    Object answer = event == null || event.getData() == null || event.getData().getOutputs() == null
                        ? null
                        : event.getData().getOutputs().get("answer");
                    if (answer == null) {
                        throw new ServiceException("AI未返回调查报告内容");
                    }
                    String savedPlanContentJson = resolveAiReportPlanContentJson(
                        context.hasExistingReport(),
                        context.latestPlanContentJson(),
                        null
                    );
                    saveGeneratedEmergencyInvestigationReport(
                        bo.getId(),
                        answer.toString(),
                        savedPlanContentJson,
                        currentUserId,
                        currentUserName,
                        context.hasExistingReport(),
                        eventLevelUpdater
                    );
                    sendEmitter(sseEmitter, buildWorkflowFinishedStreamEvent(answer.toString(), savedPlanContentJson, event));
                } catch (ServiceException e) {
                    sseEmitter.completeWithError(e);
                    return;
                } finally {
                    sseEmitter.complete();
                }
            }
        });
    }

    void generateReviewReportStream(SseEmitter sseEmitter, GenerateReviewReportBo bo) {
        if (bo == null || bo.getHandleId() == null) {
            throw new ServiceException("handleId不能为空");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        Long currentUserId = loginUser == null ? null : loginUser.getUserId();
        String currentUserName = loginUser == null ? null : StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        DzTaskHandle taskHandle = permissionSupport.requireTaskHandleById(bo.getHandleId());
        if (!CurrentRoleUtil.hasAnyRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("只有值班员才可以生成复盘报告");
        }
        permissionSupport.validateCurrentUserTaskHandleAccess(
            taskHandle.getProvince(),
            taskHandle.getCity(),
            taskHandle.getCounty(),
            taskHandle.getStreet(),
            taskHandle.getVillage()
        );
        if (hasGeneratedReviewReport(bo.getHandleId())) {
            throw new ServiceException("当前处置任务已生成复盘报告，请勿重复生成");
        }

        DifyChatflowClient agent = difyAgentClient.getAiReviewReportAgent();
        ChatMessage message = ChatMessage.builder()
                                         .query(StringUtils.blankToDefault(bo.getPrompt(), "生成复盘报告"))
                                         .inputs(buildReviewReportInputs(taskHandle))
                                         .responseMode(ResponseMode.STREAMING)
                                         .user("admin")
                                         .conversationId("")
                                         .build();
        try {
            agent.sendChatMessageStream(message, new ChatflowStreamCallback() {
                @Override
                public void onMessage(MessageEvent event) {
                    sendEmitter(sseEmitter, event);
                }

                @Override
                public void onWorkflowFinished(WorkflowFinishedEvent event) {
                    try {
                        Object answer = event == null || event.getData() == null || event.getData().getOutputs() == null
                            ? null
                            : event.getData().getOutputs().get("answer");
                        if (answer == null) {
                            throw new ServiceException("AI未返回复盘报告内容");
                        }
                        dzTaskHandleDetailContentService.saveReviewReportContent(
                            bo.getHandleId(),
                            AUTO_REPORT_ROUND_NO,
                            answer.toString(),
                            currentUserId,
                            currentUserName
                        );
                    } catch (Exception e) {
                        sseEmitter.completeWithError(e);
                        return;
                    }
                    sendEmitter(sseEmitter, event);
                    sseEmitter.complete();
                }
            });
        } catch (Exception e) {
            throw new ServiceException("调用Dify流式接口失败");
        }
    }

    void ensureEmergencyInvestigationReportGenerated(
        Long handleId,
        Long actorUserId,
        String actorName,
        BiConsumer<Long, String> eventLevelUpdater
    ) {
        if (hasGeneratedEmergencyInvestigationReport(handleId)) {
            return;
        }
        generateEmergencyInvestigationReport(
            handleId,
            AUTO_REPORT_DEFAULT_PROMPT,
            actorUserId == null ? AUTO_MODE_FALLBACK_USER_ID : actorUserId,
            StringUtils.blankToDefault(actorName, DEFAULT_AI_ACTOR_NAME),
            false,
            USE_ALGORITHM_REPORT_API,
            eventLevelUpdater
        );
    }

    private Map<String, Object> buildReviewReportInputs(DzTaskHandle taskHandle) {
        Long handleId = taskHandle.getId();
        return MapUtil.<String, Object>builder()
                      .put("data", JsonUtils.toJsonString(receiveService.queryHandleProcess(handleId)))
                      .build();
    }

    private boolean hasGeneratedReviewReport(Long handleId) {
        return StringUtils.isNotBlank(getLatestTaskHandlePlanContent(handleId, DetailContentTypeEnum.REVIEW_REPORT.getCode()));
    }

    @SneakyThrows
    private String generateEmergencyInvestigationReport(
        Long handleId,
        String prompt,
        Long actorUserId,
        String actorUserName,
        boolean validateManualRole,
        boolean useAlgorithmReportApi,
        BiConsumer<Long, String> eventLevelUpdater
    ) {
        ReportGenerationContext context = buildEmergencyInvestigationReportContext(
            handleId,
            prompt,
            validateManualRole,
            useAlgorithmReportApi
        );
        String answer;
        String responsePlanContentJson = null;
        if (useAlgorithmReportApi) {
            AiReportApiClient.AiReportResponse response = aiReportApiClient.report(toAiReportRequest(context));
            answer = response.markdown();
            responsePlanContentJson = response.imagePlaceholdersJson();
        } else {
            ChatMessage message = ChatMessage.builder()
                                             .query(context.prompt())
                                             .inputs(context.inputs())
                                             .responseMode(ResponseMode.BLOCKING)
                                             .user("admin")
                                             .conversationId("")
                                             .build();
            ChatMessageResponse chatResp = difyAgentClient.getAiReportAgent().sendChatMessage(message);
            answer = chatResp == null ? null : chatResp.getAnswer();
        }
        if (StringUtils.isBlank(answer)) {
            throw new ServiceException("AI未返回调查报告内容");
        }
        String savedPlanContentJson = resolveAiReportPlanContentJson(
            context.hasExistingReport(),
            context.latestPlanContentJson(),
            responsePlanContentJson
        );
        saveGeneratedEmergencyInvestigationReport(
            handleId,
            answer,
            savedPlanContentJson,
            actorUserId,
            actorUserName,
            context.hasExistingReport(),
            eventLevelUpdater
        );
        return answer;
    }

    private ReportGenerationContext buildEmergencyInvestigationReportContext(
        Long handleId,
        String prompt,
        boolean validateManualRole,
        boolean useAlgorithmReportApi
    ) {
        if (handleId == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        dzTaskHandleDetailContentService.updateLatestSuggest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.AI_REPORT.getCode(),
            prompt
        );
        DzTaskHandle dzTaskHandle = permissionSupport.requireTaskHandleById(handleId);
        if (validateManualRole && !CurrentRoleUtil.hasAnyRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("只有值班员才可以生成应急调查报告");
        }
        if (validateManualRole) {
            permissionSupport.validateCurrentUserTaskHandleAccess(
                dzTaskHandle.getProvince(),
                dzTaskHandle.getCity(),
                dzTaskHandle.getCounty(),
                dzTaskHandle.getStreet(),
                dzTaskHandle.getVillage()
            );
        }
        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordMapper.selectVoOne(
            Wrappers.<DzTaskHandleSceneRecord>lambdaQuery().eq(DzTaskHandleSceneRecord::getHandleId, handleId)
        );
        if (sceneRecord == null) {
            throw new ServiceException("缺少现场记录，无法生成应急调查报告");
        }
        fillSceneRecordCoordinates(sceneRecord);
        DzTaskHandleDetailContentVo latestAiReport = getLatestTaskHandleDetailContent(handleId, DetailContentTypeEnum.AI_REPORT.getCode(), null);
        String latestReport = latestAiReport == null ? null : latestAiReport.getPlanContent();
        boolean hasExistingReport = isRealEmergencyInvestigationReport(latestReport);
        String latestPlanContentJson = latestAiReport == null ? null : latestAiReport.getPlanContentJson();
        Map<String, Object> inputs;
        if (useAlgorithmReportApi) {
            EmergencyInvestigationReportVo mcpResponse = emergencyInvestigationReportOrchestrator.queryByHandleId(handleId);
            inputs = MapUtil.<String, Object>builder()
                            .put("app_input", sceneRecord)
                            .put("mcp_response", mcpResponse)
                            .build();
        } else {
            inputs = MapUtil.<String, Object>builder()
                            .put("incident_context", JsonUtils.toJsonString(Map.of("report_info", sceneRecord)))
                            .put("report", hasExistingReport ? latestReport : "")
                            .build();
        }
        return new ReportGenerationContext(
            handleId,
            StringUtils.blankToDefault(prompt, AUTO_REPORT_DEFAULT_PROMPT),
            hasExistingReport,
            latestReport,
            latestPlanContentJson,
            inputs
        );
    }

    private AiReportApiClient.AiReportRequest toAiReportRequest(ReportGenerationContext context) {
        return new AiReportApiClient.AiReportRequest(
            context.handleId(),
            context.hasExistingReport(),
            context.inputs().get("app_input"),
            context.inputs().get("mcp_response"),
            context.prompt(),
            context.latestReport()
        );
    }

    private void saveGeneratedEmergencyInvestigationReport(
        Long handleId,
        String aiReport,
        String imagePlaceholdersJson,
        Long actorUserId,
        String actorUserName,
        boolean hadExistingRealReport,
        BiConsumer<Long, String> eventLevelUpdater
    ) {
        Long effectiveUserId = actorUserId == null ? AUTO_MODE_FALLBACK_USER_ID : actorUserId;
        String effectiveUserName = StringUtils.blankToDefault(actorUserName, DEFAULT_AI_ACTOR_NAME);
        DzTaskHandleDetailContentVo latest = dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.AI_REPORT.getCode(),
            AUTO_REPORT_ROUND_NO
        );
        if (latest != null && latest.getId() != null && isLegacyPlaceholderReport(latest.getPlanContent())) {
            DzTaskHandleDetailContentBo updateBo = new DzTaskHandleDetailContentBo();
            updateBo.setId(latest.getId());
            updateBo.setBizId(latest.getBizId());
            updateBo.setBizType(latest.getBizType());
            updateBo.setContentType(latest.getContentType());
            updateBo.setPlanContent(aiReport);
            updateBo.setPlanContentJson(imagePlaceholdersJson == null ? latest.getPlanContentJson() : imagePlaceholdersJson);
            updateBo.setSuggest(latest.getSuggest());
            updateBo.setUserId(effectiveUserId);
            updateBo.setUserName(effectiveUserName);
            updateBo.setCreateDate(latest.getCreateDate());
            updateBo.setDeleted(latest.getDeleted());
            updateBo.setIsLatest(latest.getIsLatest());
            updateBo.setRoundNo(latest.getRoundNo());
            updateBo.setStatus(latest.getStatus());
            updateBo.setUpdateDate(new Date());
            dzTaskHandleDetailContentService.updateByBo(updateBo);
            eventLevelUpdater.accept(handleId, aiReport);
            recordGeneratedEmergencyInvestigationReportNodeIfFirst(handleId, hadExistingRealReport, effectiveUserId, effectiveUserName);
            return;
        }
        dzTaskHandleDetailContentService.saveAiReportContent(
            handleId,
            AUTO_REPORT_ROUND_NO,
            aiReport,
            imagePlaceholdersJson,
            effectiveUserId,
            effectiveUserName
        );
        eventLevelUpdater.accept(handleId, aiReport);
        recordGeneratedEmergencyInvestigationReportNodeIfFirst(handleId, hadExistingRealReport, effectiveUserId, effectiveUserName);
    }

    private void recordGeneratedEmergencyInvestigationReportNodeIfFirst(
        Long handleId,
        boolean hadExistingRealReport,
        Long operatorId,
        String operatorName
    ) {
        if (handleId == null || hadExistingRealReport) {
            return;
        }
        try {
            DzTaskProcessChainNode existed = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
                TaskProcessBizTypeEnum.HANDLE.getCode(),
                handleId,
                TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(),
                TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode()
            );
            if (existed != null) {
                return;
            }
            DzTaskProcessChainNode parent = resolveHandleStartNode(handleId);
            if (parent == null || parent.getId() == null || StringUtils.isBlank(parent.getChainId())) {
                log.warn("首次生成应急调查报告后未找到开启处置管理节点，跳过流程链路写入，handleId={}", handleId);
                return;
            }
            Long effectiveOperatorId = operatorId == null ? AUTO_MODE_FALLBACK_USER_ID : operatorId;
            String effectiveOperatorName = Objects.equals(effectiveOperatorId, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID)
                ? DzTaskDistListServiceImpl.AUTO_AGENT_NAME
                : StringUtils.blankToDefault(operatorName, DEFAULT_AI_ACTOR_NAME);
            taskProcessChainNodeService.recordBizNode(
                parent.getChainId(),
                TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(),
                TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getTriggerReason(),
                TaskProcessBizTypeEnum.HANDLE.getCode(),
                handleId,
                null,
                effectiveOperatorId,
                effectiveOperatorName,
                TaskProcessSourceTypeEnum.HANDLE.getCode(),
                parent.getId(),
                null,
                TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode(),
                TaskProcessStageTypeEnum.HANDLE.getCode()
            );
        } catch (Exception e) {
            log.warn("首次生成应急调查报告后写入流程链路节点失败，handleId={}", handleId, e);
        }
    }

    private DzTaskProcessChainNode resolveHandleStartNode(Long handleId) {
        DzTaskProcessChainNode node = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(),
            TaskProcessNodeCategoryEnum.HANDLE_START.getCode()
        );
        if (node != null) {
            return node;
        }
        return taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(),
            null
        );
    }

    private boolean hasGeneratedEmergencyInvestigationReport(Long handleId) {
        return isRealEmergencyInvestigationReport(getLatestTaskHandlePlanContent(handleId, DetailContentTypeEnum.AI_REPORT.getCode()));
    }

    private boolean isRealEmergencyInvestigationReport(String reportContent) {
        return StringUtils.isNotBlank(reportContent) && !isLegacyPlaceholderReport(reportContent);
    }

    private boolean isLegacyPlaceholderReport(String reportContent) {
        return Objects.equals(StringUtils.trimToEmpty(reportContent), LEGACY_PLACEHOLDER_REPORT);
    }

    private String getLatestTaskHandlePlanContent(Long handleId, Integer contentType) {
        DzTaskHandleDetailContentVo latest = getLatestTaskHandleDetailContent(handleId, contentType, null);
        if (latest == null) {
            return null;
        }
        if (Objects.equals(contentType, DetailContentTypeEnum.EVACUATION_PLAN.getCode())
            && StringUtils.isNotBlank(latest.getPlanContentJson())) {
            return latest.getPlanContentJson();
        }
        return latest.getPlanContent();
    }

    private DzTaskHandleDetailContentVo getLatestTaskHandleDetailContent(Long handleId, Integer contentType, Integer roundNo) {
        return dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            contentType,
            roundNo
        );
    }

    private void fillSceneRecordCoordinates(DzTaskHandleSceneRecordVo sceneRecord) {
        String disasterCoordinates = sceneRecord.getDisasterCoordinates();
        double[] coordinates = GeoDistanceUtil.parsePoint(disasterCoordinates);
        if (coordinates != null && coordinates.length >= 2) {
            sceneRecord.setDisasterLongitude(BigDecimal.valueOf(coordinates[0]));
            sceneRecord.setDisasterLatitude(BigDecimal.valueOf(coordinates[1]));
        }
        sceneRecord.setCheckInCoordinates(CoordinateConverter.convertWktToDisplay(sceneRecord.getCheckInCoordinates()));
        sceneRecord.setDisasterCoordinates(CoordinateConverter.convertWktToDisplay(sceneRecord.getDisasterCoordinates()));
    }

    AiReportStreamEvent buildMessageStreamEvent(String answer, Object rawEvent) {
        Map<String, Object> payload = toStreamPayload(rawEvent);
        String resolvedAnswer = StringUtils.defaultIfBlank(answer, extractStreamAnswer(rawEvent));
        if (StringUtils.isNotBlank(resolvedAnswer)) {
            payload.put("answer", resolvedAnswer);
        }
        return new AiReportStreamEvent(STREAM_EVENT_MESSAGE, payload);
    }

    AiReportStreamEvent buildWorkflowFinishedStreamEvent(String markdown, String imagePlaceholdersJson, Object rawEvent) {
        Map<String, Object> payload = toStreamPayload(rawEvent);
        payload.put("markdown", StringUtils.defaultString(markdown));
        payload.put("imagePlaceholders", parseImagePlaceholders(imagePlaceholdersJson));
        return new AiReportStreamEvent(STREAM_EVENT_WORKFLOW_FINISHED, payload);
    }

    String extractStreamAnswer(Object rawEvent) {
        JsonNode node = JacksonUtil.toJsonNode(rawEvent);
        return findStreamText(node);
    }

    Map<String, Object> toStreamPayload(Object rawEvent) {
        if (rawEvent == null) {
            return new LinkedHashMap<>();
        }
        String json = JacksonUtil.toJson(rawEvent);
        Map<String, Object> payload = JacksonUtil.toMap(json);
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    Object parseImagePlaceholders(String imagePlaceholdersJson) {
        if (StringUtils.isBlank(imagePlaceholdersJson)) {
            return null;
        }
        JsonNode node = JacksonUtil.toJsonNode(imagePlaceholdersJson);
        if (node == null || node.isNull()) {
            return imagePlaceholdersJson;
        }
        if (node.isObject()) {
            Map<String, Object> parsed = JacksonUtil.toMap(node.toString());
            return parsed == null ? new LinkedHashMap<>() : parsed;
        }
        return node;
    }

    String resolveAiReportPlanContentJson(boolean hasExistingReport, String latestPlanContentJson, String responsePlanContentJson) {
        return hasExistingReport ? latestPlanContentJson : responsePlanContentJson;
    }

    private String findStreamText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = findStreamText(item);
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
            return null;
        }
        String[] textFields = {"answer", "content", "text", "message", "chunk", "delta"};
        for (String field : textFields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = findStreamText(value);
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
        }
        JsonNode data = node.get("data");
        if (data != null && !data.isNull()) {
            String text = findStreamText(data);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        }
        return null;
    }

    void sendEmitter(SseEmitter emitter, Object vo) {
        try {
            emitter.send(SseEmitter.event().comment("message").data(JacksonUtil.toJson(vo)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private record ReportGenerationContext(
        Long handleId,
        String prompt,
        boolean hasExistingReport,
        String latestReport,
        String latestPlanContentJson,
        Map<String, Object> inputs
    ) {
    }

    record AiReportStreamEvent(
        String eventType,
        Map<String, Object> event
    ) {
    }
}
