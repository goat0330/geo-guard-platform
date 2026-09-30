/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.handle.client.AiReportApiClient;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.orchestrator.EmergencyInvestigationReportOrchestrator;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class DzTaskHandleAiReportSupportTest {

    @Mock
    private DifyAgentClient difyAgentClient;
    @Mock
    private DzTaskHandleSceneRecordMapper dzTaskHandleSceneRecordMapper;
    @Mock
    private IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    @Mock
    private IReceiveService receiveService;
    @Mock
    private DzTaskHandlePermissionSupport permissionSupport;
    @Mock
    private EmergencyInvestigationReportOrchestrator emergencyInvestigationReportOrchestrator;
    @Mock
    private AiReportApiClient aiReportApiClient;
    @Mock
    private IDzTaskProcessChainNodeService taskProcessChainNodeService;

    private DzTaskHandleAiReportSupport support;

    @BeforeEach
    void setUp() {
        support = new DzTaskHandleAiReportSupport(
            difyAgentClient,
            dzTaskHandleSceneRecordMapper,
            dzTaskHandleDetailContentService,
            receiveService,
            permissionSupport,
            emergencyInvestigationReportOrchestrator,
            aiReportApiClient,
            taskProcessChainNodeService
        );
    }

    @Test
    void buildMessageStreamEventAddsAnswerAndPreservesExistingPayload() {
        DzTaskHandleAiReportSupport.AiReportStreamEvent event = support.buildMessageStreamEvent(
            "实时输出",
            Map.of("conversationId", "conv-1", "event", "message")
        );

        assertThat(event.eventType()).isEqualTo("MESSAGE");
        assertThat(event.event())
            .containsEntry("answer", "实时输出")
            .containsEntry("conversationId", "conv-1")
            .containsEntry("event", "message");
    }

    @Test
    void buildWorkflowFinishedStreamEventIncludesStructuredImagePlaceholders() {
        DzTaskHandleAiReportSupport.AiReportStreamEvent event = support.buildWorkflowFinishedStreamEvent(
            "# 报告",
            "{}",
            Map.of("taskId", "task-1")
        );

        assertThat(event.eventType()).isEqualTo("WORKFLOW_FINISHED");
        assertThat(event.event())
            .containsEntry("markdown", "# 报告")
            .containsEntry("taskId", "task-1");
        assertThat(event.event().get("imagePlaceholders")).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) event.event().get("imagePlaceholders")).isEmpty();
    }

    @Test
    void extractStreamAnswerReadsNestedDeltaField() {
        String answer = support.extractStreamAnswer(Map.of("data", Map.of("delta", "分片内容")));

        assertThat(answer).isEqualTo("分片内容");
    }

    @Test
    void resolveAiReportPlanContentJsonKeepsExistingValueForRevise() {
        String saved = support.resolveAiReportPlanContentJson(true, "{\"img\":1}", "{\"img\":2}");

        assertThat(saved).isEqualTo("{\"img\":1}");
    }

    @Test
    void resolveAiReportPlanContentJsonUsesResponseValueForGenerate() {
        String saved = support.resolveAiReportPlanContentJson(false, "{\"img\":1}", "{\"img\":2}");

        assertThat(saved).isEqualTo("{\"img\":2}");
    }

    @Test
    void sendEmitterUsesSseEventBuilder() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);

        support.sendEmitter(emitter, new DzTaskHandleAiReportSupport.AiReportStreamEvent(
            "MESSAGE",
            Map.of("answer", "hello")
        ));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void saveGeneratedReportFirstTimeRecordsAiReportNode() throws Exception {
        DzTaskProcessChainNode parent = parentHandleStartNode();
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode())
        )).thenReturn(null);
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_START.getCode())
        )).thenReturn(parent);

        invokeSaveGeneratedReport(false, null, null);

        verify(dzTaskHandleDetailContentService).saveAiReportContent(
            eq(66L), eq(1), eq("真实报告"), eq("{}"), eq(0L), eq("地象大模型")
        );
        verify(taskProcessChainNodeService).recordBizNode(
            eq("HANDLE-66"),
            eq(TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName()),
            eq(TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getTriggerReason()),
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()),
            eq(66L),
            isNull(),
            eq(0L),
            eq(DzTaskDistListServiceImpl.AUTO_AGENT_NAME),
            eq(TaskProcessSourceTypeEnum.HANDLE.getCode()),
            eq(10L),
            isNull(),
            eq(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode()),
            eq(TaskProcessStageTypeEnum.HANDLE.getCode())
        );
    }

    @Test
    void saveGeneratedReportDoesNotRecordNodeWhenRealReportAlreadyExists() throws Exception {
        invokeSaveGeneratedReport(true, 100L, "值班员");

        verify(dzTaskHandleDetailContentService).saveAiReportContent(
            eq(66L), eq(1), eq("真实报告"), eq("{}"), eq(100L), eq("值班员")
        );
        verify(taskProcessChainNodeService, never()).recordBizNode(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void saveGeneratedReportReplacingLegacyPlaceholderRecordsNode() throws Exception {
        DzTaskHandleDetailContentVo placeholder = latestAiReport("待补充应急调查报告");
        when(dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(), 66L, DetailContentTypeEnum.AI_REPORT.getCode(), 1
        )).thenReturn(placeholder);
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode())
        )).thenReturn(null);
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_START.getCode())
        )).thenReturn(parentHandleStartNode());

        invokeSaveGeneratedReport(false, 101L, "值班员");

        verify(dzTaskHandleDetailContentService).updateByBo(any());
        verify(taskProcessChainNodeService).recordBizNode(
            eq("HANDLE-66"), any(), any(), eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            isNull(), eq(101L), eq("值班员"), eq(TaskProcessSourceTypeEnum.HANDLE.getCode()),
            eq(10L), isNull(), eq(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode()),
            eq(TaskProcessStageTypeEnum.HANDLE.getCode())
        );
    }

    @Test
    void saveGeneratedReportIgnoresProcessChainWriteFailure() throws Exception {
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode())
        )).thenReturn(null);
        when(taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            eq(TaskProcessBizTypeEnum.HANDLE.getCode()), eq(66L),
            eq(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
            eq(TaskProcessNodeCategoryEnum.HANDLE_START.getCode())
        )).thenReturn(parentHandleStartNode());
        doThrow(new IllegalStateException("chain write failed")).when(taskProcessChainNodeService).recordBizNode(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

        assertThatCode(() -> invokeSaveGeneratedReport(false, null, null)).doesNotThrowAnyException();

        verify(dzTaskHandleDetailContentService).saveAiReportContent(
            eq(66L), eq(1), eq("真实报告"), eq("{}"), eq(0L), eq("地象大模型")
        );
    }

    @SuppressWarnings("unchecked")
    private void invokeSaveGeneratedReport(boolean hadExistingRealReport, Long actorUserId, String actorUserName) throws Exception {
        Method method = DzTaskHandleAiReportSupport.class.getDeclaredMethod(
            "saveGeneratedEmergencyInvestigationReport",
            Long.class,
            String.class,
            String.class,
            Long.class,
            String.class,
            boolean.class,
            BiConsumer.class
        );
        method.setAccessible(true);
        BiConsumer<Long, String> eventLevelUpdater = mock(BiConsumer.class);
        method.invoke(support, 66L, "真实报告", "{}", actorUserId, actorUserName, hadExistingRealReport, eventLevelUpdater);
        verify(eventLevelUpdater).accept(66L, "真实报告");
    }

    private DzTaskProcessChainNode parentHandleStartNode() {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(10L);
        node.setChainId("HANDLE-66");
        node.setLinkName(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName());
        node.setNodeCategory(TaskProcessNodeCategoryEnum.HANDLE_START.getCode());
        return node;
    }

    private DzTaskHandleDetailContentVo latestAiReport(String planContent) {
        DzTaskHandleDetailContentVo latest = new DzTaskHandleDetailContentVo();
        latest.setId(20L);
        latest.setBizId(66L);
        latest.setBizType(DetailBizTypeEnum.TASK_HANDLE.getCode());
        latest.setContentType(DetailContentTypeEnum.AI_REPORT.getCode());
        latest.setPlanContent(planContent);
        latest.setPlanContentJson("{\"old\":true}");
        latest.setCreateDate(new Date());
        latest.setDeleted(0);
        latest.setIsLatest(1);
        latest.setRoundNo(1);
        latest.setStatus(0);
        return latest;
    }
}
