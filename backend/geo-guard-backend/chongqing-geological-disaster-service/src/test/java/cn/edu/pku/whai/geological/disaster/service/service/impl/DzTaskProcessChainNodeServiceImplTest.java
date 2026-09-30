/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainSegmentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskProcessChainNodeServiceImplTest {

    @Test
    void generateChainIdUsesDateAndDailySequence() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectMaxDailyChainSequence("20260629")).thenReturn(0, 12);

        assertThat(service.generateChainId(LocalDate.of(2026, 6, 29))).isEqualTo("20260629-001");
        assertThat(service.generateChainId(LocalDate.of(2026, 6, 29))).isEqualTo("20260629-013");

        verify(chainNodeMapper, times(2)).lockChain("DAILY_CHAIN_ID:20260629");
    }

    @Test
    void legalTaskRootUsesDatabaseTimeAndCreatesNewChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(),
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.MANUAL.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).lockChain("TASK-100");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getChainId()).isEqualTo("TASK-100");
        assertThat(captor.getValue().getParentNodeId()).isNull();
        assertThat(captor.getValue().getTriggerTime()).isNull();
        assertThat(captor.getValue().getCreateDate()).isNull();
    }

    @Test
    void countyRegionDefRespWithoutAlarmCanCreateDefRespRootChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        service.recordBizNode("DEF_RESP-300", TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 300L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).lockChain("DEF_RESP-300");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getChainId()).isEqualTo("DEF_RESP-300");
        assertThat(captor.getValue().getParentNodeId()).isNull();
    }

    @Test
    void alarmReportUploadCanCreateAlarmRootChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        service.recordBizNode("ALARM-88", TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getTriggerReason(),
            TaskProcessBizTypeEnum.ALARM.getCode(), 88L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.ALARM.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).lockChain("ALARM-88");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("上传预警报告");
        assertThat(captor.getValue().getChainId()).isEqualTo("ALARM-88");
        assertThat(captor.getValue().getParentNodeId()).isNull();
    }

    @Test
    void regionDefRespFromAlarmCanFollowAlarmReportUpload() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode alarmReportUpload = node(80L, "ALARM-88", null,
            TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName(), null,
            TaskProcessSourceTypeEnum.ALARM.getCode(), TaskProcessBizTypeEnum.ALARM.getCode(), 88L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(alarmReportUpload)
            .thenReturn(alarmReportUpload);

        service.recordBizNode("ALARM-88",
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.ALARM.getCode(), null, null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(80L);
    }

    @Test
    void sceneHandleReportUploadCanCreateHandleRootChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        service.recordBizNode("HANDLE-66", TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), 66L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.HANDLE.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).lockChain("HANDLE-66");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("上报现场处置报告");
        assertThat(captor.getValue().getChainId()).isEqualTo("HANDLE-66");
        assertThat(captor.getValue().getParentNodeId()).isNull();
    }

    @Test
    void defRespBatchDispatchCanFollowRegionDefRespPublished() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode regionDefRespPublished = node(88L, "ALARM-88", 80L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(), null,
            TaskProcessSourceTypeEnum.ALARM.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(regionDefRespPublished)
            .thenReturn(regionDefRespPublished);

        service.recordBizNode("ALARM-88", TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).lockChain("ALARM-88");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(88L);
    }

    @Test
    void defRespBatchDispatchRejectsAlarmDefRespStart() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode alarmDefRespStart = node(88L, "ALARM-88", 80L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(), null,
            TaskProcessSourceTypeEnum.ALARM.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(alarmDefRespStart)
            .thenReturn(alarmDefRespStart)
            .thenReturn(alarmDefRespStart)
            .thenReturn(alarmDefRespStart);

        assertThatThrownBy(() -> service.recordBizNode("ALARM-88",
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void regionDefRespPublishedCanFollowAlarmDefRespStart() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode alarmDefRespStart = node(88L, "ALARM-88", 80L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(), null,
            TaskProcessSourceTypeEnum.ALARM.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(alarmDefRespStart)
            .thenReturn(alarmDefRespStart);

        service.recordBizNode("ALARM-88",
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(88L);
    }

    @Test
    void regionDefRespPublishedCanFollowRegionDefRespStart() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode regionDefRespStart = node(89L, "DEF_RESP-30", 80L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), null,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(regionDefRespStart)
            .thenReturn(regionDefRespStart);

        service.recordBizNode("DEF_RESP-30",
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(89L);
    }

    @Test
    void defRespArchivedCanFollowRegionDefRespStartDirectly() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode regionDefRespStart = node(89L, "DEF_RESP-30", 80L,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), null,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(89L)).thenReturn(regionDefRespStart);

        service.recordBizNode("DEF_RESP-30",
            TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 30L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), 89L, null, null);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(89L);
    }

    @Test
    void emergencyBatchPushCanFollowEmergencyBatchDispatch() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode batchDispatch = node(90L, "HANDLE-66", 80L,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(), null,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(90L)).thenReturn(batchDispatch);

        service.recordBizNode("HANDLE-66",
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), 66L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), 90L, null,
            TaskProcessNodeCategoryEnum.TASK_PUSH.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(90L);
        assertThat(captor.getValue().getLinkName()).isEqualTo("推送处置任务");
    }

    @Test
    void legacyDefRespTaskDispatchDisplayNormalizesToDispatchText() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode oldNode = node(10L, "TASK-100", null,
            TaskProcessChainNodeTextEnum.LEGACY_DEF_RESP_TASK_DISPATCH_LINK_NAME, 100L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(oldNode);
        when(chainNodeMapper.selectList(any())).thenReturn(List.of(oldNode));

        List<TaskProcessChainNodeVo> result = service.queryChainByChainId("TASK-100");

        assertThat(result).extracting(TaskProcessChainNodeVo::getLinkName)
            .containsExactly(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName());
    }

    @Test
    void regionDefRespPublishedCannotCreateDefRespRootChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.recordBizNode("DEF_RESP-300",
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 300L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void continuationWaitsUntilAllowedPreviousNodeAppears() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode parent = node(10L, "TASK-100", null,
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(null)
            .thenReturn(null)
            .thenReturn(parent)
            .thenReturn(parent);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.MANUAL.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper, times(5)).selectOne(any());
        verify(chainNodeMapper).lockChain("TASK-100");
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(10L);
    }

    @Test
    void continuationUsesSecondLatestNodeWhenLatestDoesNotMatchCurrentStep() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode latestWrong = node(11L, "TASK-100", 10L,
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode secondLatest = node(10L, "TASK-100", null,
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.MANUAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(latestWrong)
            .thenReturn(secondLatest)
            .thenReturn(latestWrong)
            .thenReturn(secondLatest);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.MANUAL.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper, times(5)).selectOne(any());
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(10L);
    }

    @Test
    void emergencyBatchDispatchUsesHandleStartAsPreviousWhenSingleDefenseIsLatestNode() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode latestSingleDefense = node(11L, "HANDLE-66", 10L,
            TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(), null,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.DEF_RESP.getCode(), 200L,
            TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START.getCode());
        DzTaskProcessChainNode handleStart = node(10L, "HANDLE-66", null,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_START.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(latestSingleDefense)
            .thenReturn(handleStart)
            .thenReturn(latestSingleDefense)
            .thenReturn(handleStart);

        service.recordBizNode("HANDLE-66", TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), 66L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), null, null,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper, times(5)).selectOne(any());
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(10L);
    }

    @Test
    void generatedEmergencyInvestigationReportCanFollowHandleStart() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode handleStart = node(10L, "HANDLE-66", null,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_START.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(10L)).thenReturn(handleStart);

        service.recordBizNode("HANDLE-66",
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), 66L, null, 0L, DzTaskDistListServiceImpl.AUTO_AGENT_NAME,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), 10L, null,
            TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(10L);
        assertThat(captor.getValue().getNodeCategory()).isEqualTo(TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode());
        assertThat(captor.getValue().getStageType()).isEqualTo(TaskProcessStageTypeEnum.HANDLE.getCode());
    }

    @Test
    void emergencyBatchDispatchCanFollowGeneratedEmergencyInvestigationReport() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode aiReportNode = node(12L, "HANDLE-66", 10L,
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(12L)).thenReturn(aiReportNode);

        service.recordBizNode("HANDLE-66",
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), 66L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), 12L, null,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(12L);
    }

    @Test
    void singleDefRespStartCanFollowGeneratedEmergencyInvestigationReport() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode aiReportNode = node(12L, "HANDLE-66", 10L,
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_AI_REPORT_GENERATE.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(12L)).thenReturn(aiReportNode);

        service.recordBizNode("HANDLE-66",
            TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(),
            TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), 200L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), 12L, null,
            TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(12L);
    }

    @Test
    void defRespTaskDispatchCanCloseDuringArchive() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode dispatch = node(2070848608761507842L, "TASK-2070848608761507842", null,
            TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(), 2070848608761507842L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(),
            2070848608761507842L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(dispatch)
            .thenReturn(dispatch)
            .thenReturn(dispatch)
            .thenReturn(dispatch);

        service.recordBizNode("TASK-2070848608761507842",
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 2070848608761507842L, 2070848608761507842L,
            1L, "operator", TaskProcessSourceTypeEnum.DEF_RESP.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode(), null, DzTaskDistList.TASK_TYPE_DEF_RESP);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("防御响应任务关闭");
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(dispatch.getId());
    }

    @Test
    void defRespTaskCanCloseAfterPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode push = node(2070848608761507842L, "TASK-2070848608761507842", null,
            "防御响应任务推送", 2070848608761507842L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(),
            2070848608761507842L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(push)
            .thenReturn(push);

        service.recordBizNode("TASK-2070848608761507842",
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 2070848608761507842L, 2070848608761507842L,
            1L, "operator", TaskProcessSourceTypeEnum.DEF_RESP.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("防御响应任务关闭");
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(push.getId());
    }

    @Test
    void taskCloseRejectsFeedbackParent() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode feedback = node(11L, "TASK-100", 10L,
            "日常巡查任务反馈", 100L,
            TaskProcessSourceTypeEnum.EVAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(feedback)
            .thenReturn(feedback)
            .thenReturn(feedback)
            .thenReturn(feedback);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EVAL.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void aiVerifyTaskFeedbackCanFollowAiVerifyPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode aiPush = node(11L, "TASK-100", 10L,
            "AI险情核实任务推送", 100L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(aiPush).thenReturn(aiPush);

        service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.REPORT.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(), null, DzTaskDistList.TASK_TYPE_AI_VERIFY);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("AI险情核实任务反馈");
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(aiPush.getId());
    }

    @Test
    void aiVerifyTaskFeedbackRejectsNonAiVerifyPushParent() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode techPush = node(11L, "TASK-100", 10L,
            "技术协查任务推送", 100L,
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(techPush)
            .thenReturn(techPush)
            .thenReturn(techPush)
            .thenReturn(techPush);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.REPORT.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(), null, DzTaskDistList.TASK_TYPE_AI_VERIFY))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void sceneHandleTaskCanFeedbackDirectlyAfterPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode push = node(11L, "TASK-100", 10L,
            "现场处置任务推送", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(push).thenReturn(push);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), null, null, TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(),
            null, DzTaskDistList.TASK_TYPE_EMERGENCY);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("现场处置任务反馈");
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(push.getId());
    }

    @Test
    void sceneHandleTaskCanFeedbackAfterExecuting() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode executing = node(12L, "TASK-100", 11L,
            "现场处置任务执行中", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L,
            TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(executing).thenReturn(executing);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), null, null, TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(),
            null, DzTaskDistList.TASK_TYPE_EMERGENCY);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("现场处置任务反馈");
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(executing.getId());
    }

    @Test
    void sceneHandleTaskRejectsCloseAfterPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode push = node(11L, "TASK-100", 10L,
            "现场处置任务推送", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(push)
            .thenReturn(push)
            .thenReturn(push)
            .thenReturn(push);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), null, null, TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode(),
            null, DzTaskDistList.TASK_TYPE_EMERGENCY))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("现场处置任务不允许关闭");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void recordTaskNodeRefinesFeedbackByStoredTaskType() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode previous = node(10L, "TASK-100", null,
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(previous).thenReturn(previous);
        DzTaskDistList task = new DzTaskDistList();
        task.setId(100L);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        task.setTaskType(DzTaskDistList.TASK_TYPE_PUBLIC_REPORT);

        service.recordTaskNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(), task, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLinkName()).isEqualTo("群众报灾任务反馈");
        assertThat(captor.getValue().getNodeCategory()).isEqualTo(TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode());
    }

    @Test
    void taskFeedbackRejectsInspectingParent() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode inspecting = node(10L, "TASK-100", 9L,
            "日常巡查任务执行中", 100L,
            TaskProcessSourceTypeEnum.EVAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L,
            TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(inspecting)
            .thenReturn(inspecting)
            .thenReturn(inspecting)
            .thenReturn(inspecting);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EVAL.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(), null, DzTaskDistList.TASK_TYPE_INSPECTION))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void taskFeedbackReportCanFollowDailyPatrolFeedbackNode() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode dailyPatrolFeedback = node(11L, "20260701-005", 10L,
            "日常巡查任务反馈", 2072231789935407105L,
            TaskProcessSourceTypeEnum.EVAL.getCode(), TaskProcessBizTypeEnum.TASK.getCode(),
            2072231789935407105L, TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(11L)).thenReturn(dailyPatrolFeedback);

        service.recordBizNode("20260701-005", TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 101L, 2072231789935407105L,
            7L, "测试巡查员01", TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), 11L, "巡查员",
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(11L);
        assertThat(captor.getValue().getLinkName()).isEqualTo(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName());
    }

    @Test
    void standaloneTaskFeedbackReportCanBeRootOnlyForReportWithoutTask() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());

        service.recordBizNode("REPORT-102", TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 102L, null, 7L, "测试巡查员",
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isNull();
        assertThat(captor.getValue().getTaskId()).isNull();
    }

    @Test
    void taskFeedbackReportWithTaskStillRequiresPreviousNode() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.recordBizNode("TASK-102",
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 102L, 100L, 7L, "测试巡查员",
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");
        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void aiVerifyDispatchCanFollowTaskFeedbackReport() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode feedbackReport = node(12L, "TASK-100", 11L,
            "日常巡查任务反馈报告", 101L,
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessBizTypeEnum.REPORT.getCode(),
            101L, TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(feedbackReport).thenReturn(feedbackReport);
        DzTaskDistList aiTask = new DzTaskDistList();
        aiTask.setId(200L);
        aiTask.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        aiTask.setTaskType(DzTaskDistList.TASK_TYPE_AI_VERIFY);

        service.recordTaskNode("TASK-100", TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getTriggerReason(), aiTask);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(feedbackReport.getId());
        assertThat(captor.getValue().getLinkName()).isEqualTo("下发AI险情核实任务");
    }

    @Test
    void disasterDangerClosedCanFollowGeneratedDisasterReport() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode generatedReport = node(12L, "REPORT-100", 11L,
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName(), null,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.REPORT.getCode(),
            100L, TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(12L)).thenReturn(generatedReport);

        service.recordBizNode("REPORT-100", TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 100L, null, 1L, "operator",
            TaskProcessSourceTypeEnum.REPORT.getCode(), 12L, null,
            TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(12L);
        assertThat(captor.getValue().getLinkName()).isEqualTo(TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName());
    }

    @Test
    void disasterDangerClosedCanFollowTaskFeedbackReport() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode feedbackReport = node(12L, "TASK-100", 11L,
            "日常巡查任务反馈报告", 100L,
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessBizTypeEnum.REPORT.getCode(),
            101L, TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(12L)).thenReturn(feedbackReport);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(), 101L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), 12L, null,
            TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(12L);
        assertThat(captor.getValue().getLinkName()).isEqualTo(TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName());
    }

    @Test
    void aiVerifyTaskPushCanFollowAiVerifyDispatch() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode dispatch = node(12L, "TASK-100", 11L,
            "下发AI险情核实任务", 200L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(dispatch).thenReturn(dispatch);
        DzTaskDistList aiTask = new DzTaskDistList();
        aiTask.setId(200L);
        aiTask.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        aiTask.setTaskType(DzTaskDistList.TASK_TYPE_AI_VERIFY);

        service.recordTaskNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason(), aiTask);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(dispatch.getId());
        assertThat(captor.getValue().getLinkName()).isEqualTo("AI险情核实任务推送");
    }

    @Test
    void techAssistFeedbackSceneReportCanFollowTechAssistPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode push = node(12L, "TASK-100", 11L,
            "技术协查任务推送", 200L,
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(push).thenReturn(push);

        service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 200L, 200L, 1L, "operator",
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(), null, DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION);

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(push.getId());
        assertThat(captor.getValue().getLinkName()).isEqualTo("技术协查任务反馈并上报现场处置报告");
    }

    @Test
    void techAssistFeedbackSceneReportRejectsNonTechAssistPush() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode publicReportPush = node(12L, "TASK-100", 11L,
            "群众报灾任务推送", 200L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);
        when(chainNodeMapper.selectOne(any())).thenReturn(null).thenReturn(publicReportPush);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 200L, 200L, 1L, "operator",
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), null, null,
            TaskProcessNodeCategoryEnum.TASK_FEEDBACK.getCode(), null, DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void taskFeedbackTextUsesExpectedTaskDisplayPrefix() {
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessSourceTypeEnum.EVAL.getCode(), DzTaskDistList.TASK_TYPE_INSPECTION))
            .isEqualTo("日常巡查任务反馈");
        assertThat(TaskProcessChainNodeTextEnum.refineTaskLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(),
            TaskProcessSourceTypeEnum.MONITOR_WARNING.getCode(), DzTaskDistList.TASK_TYPE_MONITOR_WARNING))
            .isEqualTo("监测预警任务反馈");
    }

    @Test
    void continuationFailsAfterInitialQueryAndThreeRetries() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.recordBizNode("TASK-100",
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.MANUAL.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到业务流程上一步节点");

        verify(chainNodeMapper, times(5)).selectOne(any());
        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void emergencyChildRejectsParentWithWrongCategory() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode wrongParent = node(10L, "TASK-MAIN", null,
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_START.getCode());
        when(chainNodeMapper.selectOne(any())).thenReturn(null);
        when(chainNodeMapper.selectById(10L)).thenReturn(wrongParent);

        assertThatThrownBy(() -> service.recordBizNode("TASK-200",
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 200L, 200L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), 10L, "AI智能体", null))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("上一步节点");

        verify(chainNodeMapper, times(4)).selectById(10L);
        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void emergencyChildContinuationKeepsHiddenSegment() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode childStart = node(901L, "TASK-100", 900L,
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        childStart.setChainSegmentType(TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode());
        childStart.setRootChainId("HANDLE-66");
        childStart.setRootBizType(TaskProcessBizTypeEnum.HANDLE.getCode());
        childStart.setRootBizId(66L);
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(childStart)
            .thenReturn(childStart);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(901L);
        assertThat(captor.getValue().getChainSegmentType())
            .isEqualTo(TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode());
        assertThat(captor.getValue().getDisplayBizType()).isNull();
        assertThat(captor.getValue().getDisplayBizId()).isNull();
    }

    @Test
    void blankChainIdNeverFallsBackToGeneratedValue() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());

        assertThatThrownBy(() -> service.recordBizNode(null,
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), "push",
            TaskProcessBizTypeEnum.TASK.getCode(), 100L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.MANUAL.getCode()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("chainId不能为空");

        verify(chainNodeMapper, never()).insert(any(DzTaskProcessChainNode.class));
    }

    @Test
    void defRespChildTaskQueryFlattensOnlyCurrentChildChainAfterParentNode() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskMapper = taskMapper();
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper);

        DzTaskProcessChainNode batch = node(800L, "ALARM-32", null, "批量下发防御响应任务", null,
            null, TaskProcessBizTypeEnum.DEF_RESP.getCode(), 32L,
            TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode());
        DzTaskProcessChainNode childStart = node(801L, "TASK-200", 800L, "下发防御响应任务", 200L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);
        DzTaskProcessChainNode push = node(803L, "TASK-200", 801L, DzTaskDistListServiceImpl.TASK_PUSH_LINK_NAME, 200L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);

        when(chainNodeMapper.selectOne(any()))
            .thenReturn(childStart)
            .thenReturn(childStart)
            .thenReturn(batch);
        when(chainNodeMapper.selectList(any()))
            .thenReturn(List.of(batch))
            .thenReturn(List.of(childStart, push));

        List<TaskProcessChainNodeVo> result = service.queryChainByTaskId(200L);

        assertThat(result).extracting(TaskProcessChainNodeVo::getLinkName)
            .containsExactly("批量下发防御响应任务", "下发防御响应任务", DzTaskDistListServiceImpl.TASK_PUSH_LINK_NAME);
        assertThat(result.get(0).getChildChains()).isNull();
        assertThat(result).extracting(TaskProcessChainNodeVo::getChainId)
            .containsExactly("ALARM-32", "TASK-200", "TASK-200");
    }

    @Test
    void emergencyChildTaskQueryFlattensCurrentChildChainAndExpandsBranchesAfterIt() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskMapper = taskMapper();
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper);

        DzTaskProcessChainNode batch = node(900L, "HANDLE-66", null, "批量下发处置任务", null,
            null, TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());
        DzTaskProcessChainNode currentChildStart = node(901L, "TASK-100", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode otherChildStart = node(902L, "TASK-101", 900L, "下发应急处置任务", 101L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 101L, null);
        DzTaskProcessChainNode laterBatch = node(910L, "TASK-100", 901L, "批量下发处置任务", null,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 77L,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());
        DzTaskProcessChainNode laterChildA = node(911L, "TASK-300", 910L, "下发应急处置任务", 300L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 300L, null);
        DzTaskProcessChainNode laterChildB = node(912L, "TASK-301", 910L, "下发应急处置任务", 301L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 301L, null);
        DzTaskProcessChainNode archive = node(920L, "HANDLE-66", 900L,
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(), null,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED.getCode());

        when(chainNodeMapper.selectOne(any()))
            .thenReturn(currentChildStart)
            .thenReturn(currentChildStart)
            .thenReturn(batch);
        when(chainNodeMapper.selectList(any()))
            .thenReturn(List.of(batch, archive))
            .thenReturn(List.of(currentChildStart, laterBatch))
            .thenReturn(List.of(laterChildA, laterChildB))
            .thenReturn(List.of(laterChildA))
            .thenReturn(List.of(laterChildB));

        List<TaskProcessChainNodeVo> result = service.queryChainByTaskId(100L);

        assertThat(result).hasSize(4);
        TaskProcessChainNodeVo parent = result.get(0);
        assertThat(parent.getLinkName()).isEqualTo("批量下发处置任务");
        assertThat(parent.getChildChains()).isNull();
        assertThat(result).extracting(TaskProcessChainNodeVo::getLinkName)
            .containsExactly("批量下发处置任务", "下发应急处置任务", "批量下发处置任务", "结束归档");
        assertThat(result).extracting(TaskProcessChainNodeVo::getChainId)
            .containsExactly("HANDLE-66", "TASK-100", "TASK-100", "HANDLE-66");

        TaskProcessChainNodeVo downstreamBatch = result.get(2);
        assertThat(downstreamBatch.getChildChains()).hasSize(2);
        assertThat(downstreamBatch.getChildChains())
            .extracting(chain -> chain.get(0).getTaskId())
            .containsExactly(300L, 301L);
        assertThat(result.get(3).getChildChains()).isNull();
    }

    @Test
    void parentChainQueryExpandsEmergencyBatchChildrenAsChildChains() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistListMapper taskMapper = taskMapper();
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper);

        DzTaskProcessChainNode batch = node(900L, "HANDLE-66", null, "批量下发处置任务", null,
            null, TaskProcessBizTypeEnum.HANDLE.getCode(), 66L,
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode());
        DzTaskProcessChainNode childA = node(901L, "TASK-100", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode childB = node(902L, "TASK-101", 900L, "下发应急处置任务", 101L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 101L, null);

        when(chainNodeMapper.selectOne(any())).thenReturn(batch);
        when(chainNodeMapper.selectList(any()))
            .thenReturn(List.of(batch))
            .thenReturn(List.of(childA, childB))
            .thenReturn(List.of(childA))
            .thenReturn(List.of(childB));

        List<TaskProcessChainNodeVo> result = service.queryChainByChainId("HANDLE-66");

        assertThat(result).hasSize(1);
        TaskProcessChainNodeVo parent = result.get(0);
        assertThat(parent.getLinkName()).isEqualTo("批量下发处置任务");
        assertThat(parent.getChildChains()).hasSize(2);
        assertThat(parent.getChildChains())
            .extracting(chain -> chain.get(0).getTaskId())
            .containsExactly(100L, 101L);
    }

    @Test
    void inspectingNodeCanFollowPreviousInspectingNode() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode previousInspecting = node(10L, "TASK-100", 9L,
            TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 1001L,
            TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode());
        when(chainNodeMapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(previousInspecting)
            .thenReturn(previousInspecting);

        service.recordBizNode("TASK-100", TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_INSPECTING.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(), 1002L, 100L, 1L, "operator",
            TaskProcessSourceTypeEnum.EMERGENCY.getCode());

        ArgumentCaptor<DzTaskProcessChainNode> captor = ArgumentCaptor.forClass(DzTaskProcessChainNode.class);
        verify(chainNodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentNodeId()).isEqualTo(10L);
        assertThat(captor.getValue().getBizId()).isEqualTo(1002L);
        assertThat(captor.getValue().getTaskId()).isEqualTo(100L);
        assertThat(captor.getValue().getLinkName()).isEqualTo("现场处置任务执行中");
    }

    @Test
    void chainQueryReturnsMultipleInspectingNodesWithoutTaskRemarksField() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode chainStart = node(10L, "TASK-100", null,
            TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode firstInspecting = node(11L, "TASK-100", 10L,
            TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 1001L,
            TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode());
        DzTaskProcessChainNode secondInspecting = node(12L, "TASK-100", 11L,
            TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 1002L,
            TaskProcessNodeCategoryEnum.TASK_INSPECTING.getCode());

        when(chainNodeMapper.selectOne(any())).thenReturn(chainStart);
        when(chainNodeMapper.selectList(any())).thenReturn(List.of(chainStart, firstInspecting, secondInspecting));

        List<TaskProcessChainNodeVo> result = service.queryChainByChainId("TASK-100");

        assertThat(result).extracting(TaskProcessChainNodeVo::getLinkName)
            .containsExactly(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), "任务核查中", "任务核查中");
        assertThat(result).extracting(TaskProcessChainNodeVo::getBizId)
            .containsExactly(100L, 1001L, 1002L);
        assertThat(Arrays.stream(TaskProcessChainNodeVo.class.getDeclaredFields()))
            .noneMatch(field -> "taskRemarks".equals(field.getName()));
    }

    @Test
    void latestDisplayTaskIdsUsesLatestParentTaskWhenNoEmergencyChildTaskExists() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode latestTask = node(1000L, "REPORT-10", null, "下发险情核实任务", 10L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(latestTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of());

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(10L);
    }

    @Test
    void latestDisplayTaskIdsUsesEmergencyChildTasksInsteadOfParentTask() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode parentLatestTask = node(1000L, "HANDLE-66", null, "任务反馈", 10L,
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode childA = node(1001L, "HANDLE-66", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode childB = node(1002L, "HANDLE-66", 900L, "下发应急处置任务", 101L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 101L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(childA, childB));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(100L, 101L);
    }

    @Test
    void latestDisplayTaskIdsUsesParentChainIdWhenEmergencyChildIsOnSeparateTaskChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode parentLatestTask = node(1000L, "TASK-ROOT", null, "开启处置管理", 10L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode childTask = node(1001L, "TASK-100", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        childTask.setParentChainId("TASK-ROOT");

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(childTask));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(100L);
    }

    @Test
    void latestDisplayTaskIdsRemovesParentTaskFromHistoricalSplitChainWhenEmergencyChildrenExist() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode mainChainParentTask = node(1000L, "TASK-ROOT", null, "开启处置管理", 10L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode historicalSplitParentTask = node(1001L, "TASK-SPLIT", null, "任务推送", 10L,
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode childTask = node(1002L, "TASK-100", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        childTask.setParentChainId("TASK-ROOT");

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(mainChainParentTask, historicalSplitParentTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(childTask));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(100L);
    }

    @Test
    void latestDisplayTaskIdsKeepsOnlyLatestTaskWhenHistoricalSplitTaskResolvesToSameChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode mainChainLatestTask = node(1000L, "TASK-ROOT", null, "开启处置管理", 20L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 20L, null);
        DzTaskProcessChainNode historicalSplitTask = node(1001L, "REPORT-10", null, "任务推送", 10L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode historicalTaskResolvedLatest = node(1002L, "TASK-ROOT", 999L, "申请技术协查", 10L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(mainChainLatestTask, historicalSplitTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of());
        when(chainNodeMapper.selectLatestByTaskIds(List.of(20L, 10L)))
            .thenReturn(List.of(mainChainLatestTask, historicalTaskResolvedLatest));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(20L);
    }

    @Test
    void latestDisplayTaskIdsKeepsParentTaskWhenEmergencyBatchHasNoRealChildTask() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode parentLatestTask = node(1000L, "HANDLE-66", null, "任务反馈", 10L,
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of());

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(10L);
    }

    @Test
    void latestDisplayTaskIdsDeduplicatesEmergencyChildTasksAcrossBatchNodes() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper, taskMapper());
        DzTaskProcessChainNode parentLatestTask = node(1000L, "HANDLE-66", null, "任务反馈", 10L,
            TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode childA = node(1001L, "HANDLE-66", 900L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode duplicateChildA = node(1002L, "HANDLE-66", 901L, "下发应急处置任务", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        DzTaskProcessChainNode childB = node(1003L, "HANDLE-66", 901L, "下发应急处置任务", 101L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 101L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(childA, duplicateChildA, childB));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(100L, 101L);
    }

    @Test
    void latestDisplayTaskIdsUsesEmergencyChildrenForSameCompleteBusinessChain() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistList parentTask = task(10L);
        parentTask.setUnitId("DXG-MG06");
        parentTask.setRiskId(556361L);
        parentTask.setHandleId(20260626225100L);
        DzTaskDistList emergencyChildTask = task(100L);
        emergencyChildTask.setUnitId("DXG-MG06");
        emergencyChildTask.setRiskId(556361L);
        emergencyChildTask.setHandleId(20260626225100L);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper,
            taskMapper(Map.of(10L, parentTask, 100L, emergencyChildTask)));

        DzTaskProcessChainNode parentLatestTask = node(1000L, "TASK-CURRENT", null, "开启处置管理", 10L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode historicalEmergencyChildTask = node(1001L, "TASK-100", 900L, "任务推送", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        historicalEmergencyChildTask.setParentChainId("TASK-HISTORICAL");

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask, historicalEmergencyChildTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(historicalEmergencyChildTask));
        when(chainNodeMapper.selectLatestByTaskIds(List.of(10L, 100L)))
            .thenReturn(List.of(parentLatestTask, historicalEmergencyChildTask));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(100L);
    }

    @Test
    void latestDisplayTaskIdsDoesNotHideUnrelatedReportTaskWithSameRiskAndUnit() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistList reportTask = task(10L);
        reportTask.setSourceType(DzTaskDistList.SOURCE_TYPE_REPORT);
        reportTask.setUnitId("DXG-MG06");
        reportTask.setRiskId(556361L);
        reportTask.setReportId(2070529299137376258L);
        DzTaskDistList emergencyChildTask = task(100L);
        emergencyChildTask.setUnitId("DXG-MG06");
        emergencyChildTask.setRiskId(556361L);
        emergencyChildTask.setHandleId(20260626225100L);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper,
            taskMapper(Map.of(10L, reportTask, 100L, emergencyChildTask)));

        DzTaskProcessChainNode reportLatestTask = node(1000L, "REPORT-2070529299137376258", null, "险情核实任务", 10L,
            TaskProcessSourceTypeEnum.REPORT.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode emergencyChildTaskNode = node(1001L, "TASK-100", 900L, "任务推送", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        emergencyChildTaskNode.setParentChainId("TASK-HANDLE");

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(reportLatestTask, emergencyChildTaskNode));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(emergencyChildTaskNode));
        when(chainNodeMapper.selectLatestByTaskIds(List.of(10L, 100L)))
            .thenReturn(List.of(reportLatestTask, emergencyChildTaskNode));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(10L, 100L);
    }

    @Test
    void latestDisplayTaskIdsDoesNotHideDefRespTaskWhenSameBusinessChainHasEmergencyChildren() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistList parentTask = task(10L);
        parentTask.setUnitId("DXG-MG06");
        parentTask.setRiskId(556361L);
        parentTask.setHandleId(20260626225100L);
        DzTaskDistList emergencyChildTask = task(100L);
        emergencyChildTask.setUnitId("DXG-MG06");
        emergencyChildTask.setRiskId(556361L);
        emergencyChildTask.setHandleId(20260626225100L);
        DzTaskDistList defRespTask = task(200L);
        defRespTask.setSourceType(DzTaskDistList.SOURCE_TYPE_DEF_RESP);
        defRespTask.setUnitId("DXG-MG06");
        defRespTask.setRiskId(556361L);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper,
            taskMapper(Map.of(10L, parentTask, 100L, emergencyChildTask, 200L, defRespTask)));

        DzTaskProcessChainNode parentLatestTask = node(1000L, "TASK-CURRENT", null, "开启处置管理", 10L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);
        DzTaskProcessChainNode defRespLatestTask = node(1001L, "DEF-RESP-200", null, "下发防御响应任务", 200L,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 200L, null);
        DzTaskProcessChainNode emergencyChildTaskNode = node(1002L, "TASK-100", 900L, "任务推送", 100L,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 100L, null);
        emergencyChildTaskNode.setParentChainId("TASK-HISTORICAL");

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(parentLatestTask, defRespLatestTask, emergencyChildTaskNode));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of(emergencyChildTaskNode));
        when(chainNodeMapper.selectLatestByTaskIds(List.of(10L, 200L, 100L)))
            .thenReturn(List.of(parentLatestTask, defRespLatestTask, emergencyChildTaskNode));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(200L, 100L);
    }

    @Test
    void latestDisplayTaskIdsKeepsOnlyLatestParentTaskForSameCompleteBusinessChainWithoutEmergencyChildren() {
        DzTaskProcessChainNodeMapper chainNodeMapper = mock(DzTaskProcessChainNodeMapper.class);
        DzTaskDistList oldTask = task(10L);
        oldTask.setUnitId("DXG-MG06");
        oldTask.setRiskId(556361L);
        oldTask.setReportId(1L);
        DzTaskDistList latestTask = task(20L);
        latestTask.setUnitId("DXG-MG06");
        latestTask.setRiskId(556361L);
        latestTask.setReportId(1L);
        DzTaskProcessChainNodeServiceImpl service = service(chainNodeMapper,
            taskMapper(Map.of(10L, oldTask, 20L, latestTask)));

        DzTaskProcessChainNode latestParentTask = node(1000L, "TASK-LATEST", null, "开启处置管理", 20L,
            TaskProcessSourceTypeEnum.HANDLE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 20L, null);
        DzTaskProcessChainNode oldParentTask = node(1001L, "TASK-OLD", null, "申请技术协查", 10L,
            TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode(), TaskProcessBizTypeEnum.TASK.getCode(), 10L, null);

        when(chainNodeMapper.selectLatestTaskNodesByChain()).thenReturn(List.of(latestParentTask, oldParentTask));
        when(chainNodeMapper.selectEmergencyChildTaskNodesByParentChain()).thenReturn(List.of());
        when(chainNodeMapper.selectLatestByTaskIds(List.of(20L, 10L)))
            .thenReturn(List.of(latestParentTask, oldParentTask));

        assertThat(service.queryLatestDisplayTaskIds()).containsExactly(20L);
    }

    private DzTaskProcessChainNodeServiceImpl service(DzTaskProcessChainNodeMapper chainNodeMapper,
                                                      DzTaskDistListMapper taskMapper) {
        return new DzTaskProcessChainNodeServiceImpl(
            chainNodeMapper,
            taskMapper,
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(DzTaskHandleSceneRecordMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(SysUserRoleMapper.class),
            mock(SysRoleMapper.class),
            mock(IDzTaskProcessChainSummaryService.class)
        ) {
            @Override
            void waitBeforePreviousNodeRetry() {
                // Keep retry assertions fast without changing the production delay.
            }
        };
    }

    private DzTaskDistListMapper taskMapper() {
        return taskMapper(Map.of());
    }

    private DzTaskDistListMapper taskMapper(Map<Long, DzTaskDistList> taskOverrides) {
        DzTaskDistListMapper taskMapper = mock(DzTaskDistListMapper.class);
        when(taskMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            Collection<?> taskIds = invocation.getArgument(0);
            return taskIds.stream()
                .map(id -> {
                    Long taskId = ((Number) id).longValue();
                    return taskOverrides.getOrDefault(taskId, task(taskId));
                })
                .toList();
        });
        return taskMapper;
    }

    private DzTaskDistList task(Long taskId) {
        DzTaskDistList task = new DzTaskDistList();
        task.setId(taskId);
        task.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        task.setStatus(DzTaskDistList.STATUS_UNPUSHED);
        return task;
    }

    private DzTaskProcessChainNode node(Long id, String chainId, Long parentNodeId, String linkName, Long taskId,
                                        Integer sourceType, Integer bizType, Long bizId, Integer nodeCategory) {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        node.setId(id);
        node.setChainId(chainId);
        node.setParentNodeId(parentNodeId);
        node.setLinkName(linkName);
        node.setTaskId(taskId);
        node.setSourceType(sourceType);
        node.setBizType(bizType);
        node.setBizId(bizId);
        node.setNodeCategory(nodeCategory);
        node.setDeleted(0);
        return node;
    }
}
