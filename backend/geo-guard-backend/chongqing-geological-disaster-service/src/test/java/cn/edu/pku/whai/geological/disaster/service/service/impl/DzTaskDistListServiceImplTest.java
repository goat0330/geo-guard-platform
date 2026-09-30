/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.*;
import cn.edu.pku.whai.geological.disaster.service.service.*;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysUserService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskDistListServiceImplTest {

    @Test
    void aiProcessChainInputKeepsOnlyAnalysisFields() {
        TaskProcessChainNodeVo node = node(1L, "20260702-002", "巡查任务");
        node.setDisplayBizId(100L);
        node.setCreateDate(new Date());
        node.setUpdateDate(new Date());

        String input = DzTaskDistListServiceImpl.buildAiProcessChainInput(List.of(node));
        JSONArray array = JSONUtil.parseArray(input);
        JSONObject item = array.getJSONObject(0);

        assertThat(item.getLong("nodeId")).isEqualTo(1L);
        assertThat(item.getStr("linkName")).isEqualTo("巡查任务");
        assertThat(item.getStr("bizTypeLabel")).isEqualTo("任务");
        assertThat(item.getStr("sourceTypeLabel")).isEqualTo("系统评估");
        assertThat(item.getStr("nodeCategoryLabel")).isEqualTo("任务生成");
        assertThat(item.getStr("stageTypeLabel")).isEqualTo("起始任务");
        assertThat(item.containsKey("displayBizId")).isFalse();
        assertThat(item.containsKey("createDate")).isFalse();
        assertThat(item.containsKey("updateDate")).isFalse();
        assertThat(input.length()).isLessThanOrEqualTo(DzTaskDistListServiceImpl.AI_PROCESS_CHAIN_INPUT_MAX_LENGTH);
    }

    @Test
    void aiProcessChainInputKeepsChildChainHierarchy() {
        TaskProcessChainNodeVo parent = node(1L, "20260702-002", "生成处置任务");
        TaskProcessChainNodeVo childStart = node(2L, "20260702-004", "应急处置任务");
        childStart.setPlanType(DzTaskDistList.PLAN_TYPE_MONITORING);
        TaskProcessChainNodeVo childFeedback = node(3L, "20260702-004", "任务反馈");
        parent.setChildChains(List.of(List.of(childStart, childFeedback)));

        String input = DzTaskDistListServiceImpl.buildAiProcessChainInput(List.of(parent));
        JSONObject item = JSONUtil.parseArray(input).getJSONObject(0);
        JSONArray childChains = item.getJSONArray("childChains");
        JSONArray childChain = childChains.getJSONArray(0);

        assertThat(childChains).hasSize(1);
        assertThat(childChain).hasSize(2);
        assertThat(childChain.getJSONObject(0).getStr("chainId")).isEqualTo("20260702-004");
        assertThat(childChain.getJSONObject(0).getInt("planType")).isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
        assertThat(childChain.getJSONObject(1).getStr("linkName")).isEqualTo("任务反馈");
    }

    @Test
    void processChainPlanTypeDistinguishesMonitoringAndPatrol() {
        DzTaskDistList monitor = new DzTaskDistList();
        monitor.setPlanType(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);

        DzTaskDistList patrol = new DzTaskDistList();
        patrol.setPlanType(DzTaskDistList.PLAN_TYPE_MONITORING);

        assertThat(DzTaskDistListServiceImpl.resolveProcessChainPlanType(monitor))
            .isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        assertThat(DzTaskDistListServiceImpl.resolveProcessChainPlanType(patrol))
            .isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
        assertThat(DzTaskDistListServiceImpl.resolveProcessChainTaskType(monitor)).isEqualTo(DzTaskDistList.TASK_TYPE_INSPECTION);
        assertThat(DzTaskDistListServiceImpl.resolveProcessChainTaskType(patrol)).isEqualTo(DzTaskDistList.TASK_TYPE_INSPECTION);
    }

    @Test
    void queryProcessChainFillsMonitoringPlanTypeOnChildChainNodes() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskDistListServiceImpl service = service(baseMapper, chainNodeService);

        TaskProcessChainNodeVo parent = node(1L, "HANDLE-1", "生成处置任务");
        TaskProcessChainNodeVo monitorChild = node(2L, "TASK-100", "下发监测巡查任务");
        monitorChild.setTaskId(100L);
        monitorChild.setBizId(100L);
        TaskProcessChainNodeVo patrolChild = node(3L, "TASK-101", "下发监测巡查任务");
        patrolChild.setTaskId(101L);
        patrolChild.setBizId(101L);
        parent.setChildChains(List.of(List.of(monitorChild), List.of(patrolChild)));

        DzTaskDistList monitorTask = task(100L, DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        DzTaskDistList patrolTask = task(101L, DzTaskDistList.PLAN_TYPE_MONITORING);
        when(chainNodeService.queryChainByChainId("HANDLE-1")).thenReturn(List.of(parent));
        when(baseMapper.selectBatchIds(any())).thenReturn(List.of(monitorTask, patrolTask));

        List<TaskProcessChainNodeVo> result = service.queryProcessChain(" HANDLE-1 ");
        TaskProcessChainNodeVo resultParent = result.get(0);

        assertThat(resultParent.getChildChains()).hasSize(2);
        assertThat(resultParent.getChildChains().get(0).get(0).getPlanType())
            .isEqualTo(DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING);
        assertThat(resultParent.getChildChains().get(0).get(0).getTaskType())
            .isEqualTo(DzTaskDistList.TASK_TYPE_INSPECTION);
        assertThat(resultParent.getChildChains().get(1).get(0).getPlanType())
            .isEqualTo(DzTaskDistList.PLAN_TYPE_MONITORING);
        assertThat(resultParent.getChildChains().get(1).get(0).getTaskType())
            .isEqualTo(DzTaskDistList.TASK_TYPE_INSPECTION);
    }

    @Test
    void aiProcessChainTaskIdFallsBackToRelatedBusinessTask() {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        DzTaskDistListServiceImpl service = service(baseMapper, mock(IDzTaskProcessChainNodeService.class));

        TaskProcessChainNodeVo handleNode = node(1L, "20260706-018", "开启处置管理");
        handleNode.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setBizId(66L);
        handleNode.setTaskId(null);
        TaskProcessChainNodeVo defNode = node(2L, "20260706-018", "开启单点防御响应");
        defNode.setBizType(TaskProcessBizTypeEnum.DEF_RESP.getCode());
        defNode.setType(TaskProcessBizTypeEnum.DEF_RESP.getCode());
        defNode.setBizId(88L);
        defNode.setTaskId(null);

        when(baseMapper.selectOne(any())).thenReturn(task(100L, DzTaskDistList.PLAN_TYPE_MONITORING));

        Long taskId = service.resolveProcessChainRelatedTaskId(List.of(handleNode, defNode));

        assertThat(taskId).isEqualTo(100L);
    }

    @Test
    void queryAiProcessChainUsesReportContextWhenTaskNotCreated() throws Exception {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DifyAgentClient difyAgentClient = mock(DifyAgentClient.class);
        DifyChatflowClient agent = mock(DifyChatflowClient.class);
        DzTaskDistListServiceImpl service = service(baseMapper, reportMapper, chainNodeService, difyAgentClient);

        TaskProcessChainNodeVo reportNode = node(1L, "20260706-018", "群众报灾报告");
        reportNode.setBizType(TaskProcessBizTypeEnum.REPORT.getCode());
        reportNode.setType(TaskProcessBizTypeEnum.REPORT.getCode());
        reportNode.setBizId(200L);
        reportNode.setTaskId(null);
        reportNode.setSourceType(TaskProcessSourceTypeEnum.REPORT.getCode());
        when(chainNodeService.queryChainByChainId("20260706-018")).thenReturn(List.of(reportNode));
        when(baseMapper.selectOne(any())).thenReturn(null);
        when(reportMapper.selectVoById(200L)).thenReturn(report(200L));
        when(difyAgentClient.getAiTaskProcessChainAgent()).thenReturn(agent);
        when(agent.sendChatMessage(any(ChatMessage.class))).thenReturn(ChatMessageResponse.builder()
            .answer("{\"taskUnderstanding\":\"报告链理解\",\"currentJudgement\":\"等待下发任务\"}")
            .build());

        var result = service.queryAiProcessChain("20260706-018");

        assertThat(result.getTaskUnderstanding()).isEqualTo("报告链理解");
        assertThat(result.getCurrentJudgement()).isEqualTo("等待下发任务");
        assertThat(result.getReportIds()).containsExactly(200L);
        assertThat(result.getProcessChain()).containsExactly(reportNode);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(agent).sendChatMessage(messageCaptor.capture());
        Map<String, Object> inputs = messageCaptor.getValue().getInputs();
        JSONObject taskInfo = JSONUtil.parseObj(inputs.get("task_info"));
        JSONObject reportInfo = JSONUtil.parseObj(inputs.get("report_info"));
        assertThat(taskInfo.getStr("contextType")).isEqualTo("REPORT");
        assertThat(taskInfo.getLong("id")).isEqualTo(200L);
        assertThat(reportInfo.getLong("id")).isEqualTo(200L);
        assertThat(inputs).containsKey("process_chain");
        assertThat(inputs).doesNotContainKeys("slope_unit_info", "risk_info");
        assertThat(messageCaptor.getValue().getQuery()).contains("当前业务信息");
    }

    @Test
    void queryAiProcessChainUsesHandleContextWhenTaskAndReportNotCreated() throws Exception {
        DzTaskDistListMapper baseMapper = mock(DzTaskDistListMapper.class);
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        DzTaskHandleMapper handleMapper = mock(DzTaskHandleMapper.class);
        IDzTaskHandleSceneRecordService sceneRecordService = mock(IDzTaskHandleSceneRecordService.class);
        IDzTaskHandleDetailContentService detailContentService = mock(IDzTaskHandleDetailContentService.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DifyAgentClient difyAgentClient = mock(DifyAgentClient.class);
        DifyChatflowClient agent = mock(DifyChatflowClient.class);
        DzTaskDistListServiceImpl service = service(baseMapper, reportMapper, handleMapper, sceneRecordService,
            detailContentService, chainNodeService, difyAgentClient);

        TaskProcessChainNodeVo handleNode = node(1L, "20260713-013", "上报现场处置报告");
        handleNode.setBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setType(TaskProcessBizTypeEnum.HANDLE.getCode());
        handleNode.setBizId(66L);
        handleNode.setTaskId(null);
        handleNode.setSourceType(TaskProcessSourceTypeEnum.HANDLE.getCode());
        handleNode.setNodeCategory(null);

        DzTaskHandle handle = new DzTaskHandle();
        handle.setId(66L);
        handle.setReporter("AI测试账号");
        handle.setHandleProcess(1);
        DzTaskHandleSceneRecordVo sceneRecord = new DzTaskHandleSceneRecordVo();
        sceneRecord.setHandleId(66L);
        sceneRecord.setLocation("测试现场");
        DzTaskHandleDetailContentVo latestAiReport = new DzTaskHandleDetailContentVo();
        latestAiReport.setBizId(66L);
        latestAiReport.setPlanContent("应急调查报告内容");

        when(chainNodeService.queryChainByChainId("20260713-013")).thenReturn(List.of(handleNode));
        when(baseMapper.selectOne(any())).thenReturn(null);
        when(reportMapper.selectVoById(any())).thenReturn(null);
        when(handleMapper.selectById(66L)).thenReturn(handle);
        when(sceneRecordService.getByHandleId(66L)).thenReturn(sceneRecord);
        when(detailContentService.queryLatest(DetailBizTypeEnum.TASK_HANDLE.getCode(), 66L,
            DetailContentTypeEnum.AI_REPORT.getCode(), null)).thenReturn(latestAiReport);
        when(difyAgentClient.getAiTaskProcessChainAgent()).thenReturn(agent);
        when(agent.sendChatMessage(any(ChatMessage.class))).thenReturn(ChatMessageResponse.builder()
            .answer("{\"taskUnderstanding\":\"处置链理解\",\"currentJudgement\":\"已提交现场处置报告\"}")
            .build());

        var result = service.queryAiProcessChain("20260713-013");

        assertThat(result.getTaskUnderstanding()).isEqualTo("处置链理解");
        assertThat(result.getCurrentJudgement()).isEqualTo("已提交现场处置报告");
        assertThat(result.getHandleId()).isEqualTo(66L);
        assertThat(result.getProcessChain()).containsExactly(handleNode);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(agent).sendChatMessage(messageCaptor.capture());
        Map<String, Object> inputs = messageCaptor.getValue().getInputs();
        JSONObject taskInfo = JSONUtil.parseObj(inputs.get("task_info"));
        assertThat(taskInfo.getStr("contextType")).isEqualTo("HANDLE");
        assertThat(taskInfo.getLong("id")).isEqualTo(66L);
        assertThat(taskInfo.getJSONObject("handle").getStr("reporter")).isEqualTo("AI测试账号");
        assertThat(taskInfo.getJSONObject("sceneRecord").getStr("location")).isEqualTo("测试现场");
        assertThat(taskInfo.getJSONObject("latestAiReport").getStr("planContent")).isEqualTo("应急调查报告内容");
        assertThat(inputs).containsKey("process_chain");
        assertThat(inputs).doesNotContainKeys("report_info", "slope_unit_info", "risk_info");
        assertThat(messageCaptor.getValue().getQuery()).contains("处置管理");
    }

    @Test
    void queryAiProcessChainReportsMissingAnalysisContextClearly() {
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        DzTaskDistListServiceImpl service = service(mock(DzTaskDistListMapper.class), chainNodeService);
        TaskProcessChainNodeVo orphanNode = node(1L, "20260706-019", "未知节点");
        orphanNode.setBizType(TaskProcessBizTypeEnum.UNKNOWN.getCode());
        orphanNode.setType(TaskProcessBizTypeEnum.UNKNOWN.getCode());
        orphanNode.setBizId(null);
        orphanNode.setTaskId(null);
        when(chainNodeService.queryChainByChainId("20260706-019")).thenReturn(List.of(orphanNode));

        assertThatThrownBy(() -> service.queryAiProcessChain("20260706-019"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("流程链路未找到可用于AI分析的任务、报灾报告或处置管理");
    }

    @Test
    void pushRolesPreferActualUserRolesAndFollowUserOrder() {
        DzTaskDistList inspectorTask = new DzTaskDistList();
        inspectorTask.setUserId(1L);
        inspectorTask.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);
        DzTaskDistList directorTask = new DzTaskDistList();
        directorTask.setUserId(2L);
        directorTask.setSourceType(DzTaskDistList.SOURCE_TYPE_EVAL);

        List<String> roles = DzPushSmsDelegate.resolvePushRoles(
            List.of(inspectorTask, directorTask),
            List.of(user(2L, "测试自规所长"), user(1L, "测试巡查员")),
            Map.of(1L, "风险区巡查员", 2L, "乡自规所所长")
        );

        assertThat(roles).containsExactly("乡自规所所长", "风险区巡查员");
    }

    @Test
    void aiProcessChainInputFallsBackToSummaryWhenCompactJsonIsTooLong() {
        TaskProcessChainNodeVo parent = node(1L, "20260702-002", "生成处置任务");
        List<List<TaskProcessChainNodeVo>> childChains = new ArrayList<>();
        for (int chainIndex = 0; chainIndex < 3; chainIndex++) {
            List<TaskProcessChainNodeVo> childChain = new ArrayList<>();
            for (int nodeIndex = 0; nodeIndex < 120; nodeIndex++) {
                TaskProcessChainNodeVo child = node((long) (chainIndex * 1000 + nodeIndex + 2),
                    "20260702-00" + (chainIndex + 4), "应急处置任务节点" + nodeIndex);
                child.setTriggerReason("这是一段用于构造较长流程链路的触发原因，节点序号=" + nodeIndex);
                childChain.add(child);
            }
            childChains.add(childChain);
        }
        parent.setChildChains(childChains);

        String input = DzTaskDistListServiceImpl.buildAiProcessChainInput(List.of(parent));
        JSONObject summary = JSONUtil.parseObj(input);

        assertThat(input.length()).isLessThanOrEqualTo(DzTaskDistListServiceImpl.AI_PROCESS_CHAIN_INPUT_MAX_LENGTH);
        assertThat(summary.getBool("summaryMode")).isTrue();
        assertThat(summary.getJSONArray("mainChain")).hasSize(1);
        assertThat(summary.getJSONArray("childChainSummaries")).hasSize(1);
        JSONObject parentSummary = summary.getJSONArray("childChainSummaries").getJSONObject(0);
        assertThat(parentSummary.getInt("childChainCount")).isEqualTo(3);
        assertThat(parentSummary.getJSONArray("childChains")).hasSize(3);
        assertThat(parentSummary.getJSONArray("childChains").getJSONObject(0).getInt("nodeCount")).isEqualTo(120);
    }

    private static TaskProcessChainNodeVo node(Long nodeId, String chainId, String linkName) {
        TaskProcessChainNodeVo node = new TaskProcessChainNodeVo();
        node.setNodeId(nodeId);
        node.setChainId(chainId);
        node.setLinkName(linkName);
        node.setTriggerReason("触发原因");
        node.setOperatorName("地象智能体");
        node.setTriggerTime(new Date(1782960000000L));
        node.setBizType(TaskProcessBizTypeEnum.TASK.getCode());
        node.setBizId(2072493920945647618L);
        node.setTaskId(2072493920945647618L);
        node.setSourceType(TaskProcessSourceTypeEnum.EVAL.getCode());
        node.setNodeCategory(TaskProcessNodeCategoryEnum.TASK_GENERATE.getCode());
        node.setStageType(TaskProcessStageTypeEnum.START_TASK.getCode());
        return node;
    }

    private static DzTaskDistList task(Long id, Integer planType) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(id);
        task.setPlanType(planType);
        return task;
    }

    private static DzReportDisasterVo report(Long id) {
        DzReportDisasterVo report = new DzReportDisasterVo();
        report.setId(id);
        report.setSourceType(2);
        report.setUserName("群众用户");
        report.setUserPhone("13800000000");
        report.setDetailedAddress("测试地址");
        report.setSceneTextRecord("发现房屋后方边坡有裂缝");
        report.setAiRiskLevel(3);
        report.setAiRiskLabel("高风险");
        report.setAiReportDetail("AI识别为高风险，请尽快核查。");
        report.setCreateDate(new Date(1782960000000L));
        return report;
    }

    private static SysUserVo user(Long userId, String nickName) {
        SysUserVo user = new SysUserVo();
        user.setUserId(userId);
        user.setNickName(nickName);
        return user;
    }

    private static DzTaskDistListServiceImpl service(DzTaskDistListMapper baseMapper,
                                                     IDzTaskProcessChainNodeService chainNodeService) {
        return service(baseMapper, mock(DzReportDisasterMapper.class), chainNodeService, mock(DifyAgentClient.class));
    }

    private static DzTaskDistListServiceImpl service(DzTaskDistListMapper baseMapper,
                                                     DzReportDisasterMapper reportMapper,
                                                     IDzTaskProcessChainNodeService chainNodeService,
                                                     DifyAgentClient difyAgentClient) {
        return service(baseMapper, reportMapper, mock(DzTaskHandleMapper.class),
            mock(IDzTaskHandleSceneRecordService.class), mock(IDzTaskHandleDetailContentService.class),
            chainNodeService, difyAgentClient);
    }

    private static DzTaskDistListServiceImpl service(DzTaskDistListMapper baseMapper,
                                                     DzReportDisasterMapper reportMapper,
                                                     DzTaskHandleMapper handleMapper,
                                                     IDzTaskHandleSceneRecordService sceneRecordService,
                                                     IDzTaskHandleDetailContentService detailContentService,
                                                     IDzTaskProcessChainNodeService chainNodeService,
                                                     DifyAgentClient difyAgentClient) {
        return new DzTaskDistListServiceImpl(
            baseMapper,
            mock(DzTaskDistListAddMapper.class),
            mock(DzTaskDistListRemarkMapper.class),
            mock(DzTaskDistListHistoryMapper.class),
            handleMapper,
            mock(ISysUserService.class),
            mock(DzRiskAssessmentMapper.class),
            reportMapper,
            mock(IAppTaskService.class),
            mock(AppTaskProps.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(IDzUserAdRegionService.class),
            sceneRecordService,
            detailContentService,
            mock(DzDefRespPlanMapper.class),
            mock(AdRegionMapper.class),
            mock(TransactionTemplate.class),
            mock(ITaskSmsContentService.class),
            mock(SmsSendService.class),
            mock(IDzDefRespStartSmsConfigService.class),
            mock(SysRoleMapper.class),
            mock(SysUserMapper.class),
            mock(SysUserRoleMapper.class),
            mock(StringRedisTemplate.class),
            chainNodeService,
            mock(IDzTaskProcessChainSummaryService.class),
            mock(IDzRiskAssessmentWarningRelationService.class),
            difyAgentClient
        );
    }
}
