package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionRemindReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskReq;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopeActionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistStatStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainFilterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.*;
import cn.edu.pku.whai.geological.disaster.service.domain.req.McpTaskDistListReq;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.mapper.*;
import cn.edu.pku.whai.geological.disaster.service.service.*;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.DailyPatrolCreateResult;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.MonitorFrequencyParams;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.PlanUnitTaskContext;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.RiskUnitOnDate;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/**
 * 任务派发清单Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskDistListServiceImpl implements IDzTaskDistListService {

    public static final String INSPECTING_REQUIRE_STANDARD = "2小时内向指挥部报送巡查结果，发现险情立即上报。注意:现场图像不少于两张";
    public static final Long AUTO_AGENT_USER_ID = 0L;
    public static final String AUTO_AGENT_NAME = "地象智能体";
    public static final String AUTO_AGENT_PHONE = "12345678900";

    private static final String INSPECTING_REQUIRE_PATROL = "上传现场图像（≥2张）及文字记录";
    private static final List<Integer> HIGH_AND_VERY_HIGH_RISK_LEVELS = List.of(3, 4);
    private static final int MANUAL_REMIND_INTERVAL_MINUTES = 15;
    private static final int APP_PUSH_BATCH_SIZE = 50;
    private static final String CREATE_DAILY_PATROL_LOCK_PREFIX = "def_resp_create_daily_patrol_lock_";
    private static final long CREATE_DAILY_PATROL_LOCK_MINUTES = 30L;
    private static final long CREATE_DAILY_PATROL_LOCK_WAIT_MS = 3 * 60 * 1000L;
    private static final long CREATE_DAILY_PATROL_LOCK_RETRY_INTERVAL_MS = 500L;
    private static final String PROCESS_BIZ_TYPE_TASK = "TASK";
    private static final String PROCESS_BIZ_TYPE_REPORT = "REPORT";
    private static final String PROCESS_BIZ_TYPE_HANDLE = "HANDLE";
    private static final String PROCESS_BIZ_TYPE_DEF_RESP = "DEF_RESP";
    private static final String AI_PROCESS_CHAIN_QUERY =
        "请基于当前业务信息（可能是任务、报灾报告或处置管理）、斜坡单元信息、风险评估信息（如果存在）和流程链路，仅返回JSON对象，字段包含taskUnderstanding、currentJudgement。"
            + "流程链路 process_chain 可能包含 childChains 嵌套子链，分析时需要同时参考父链和所有子链节点。";
    static final int AI_PROCESS_CHAIN_INPUT_MAX_LENGTH = 20480;
    private static final int AI_PROCESS_CHAIN_INPUT_SAFE_LENGTH = 20000;
    private static final String AI_PROCESS_CONTEXT_TYPE_REPORT = "REPORT";
    private static final String AI_PROCESS_CONTEXT_TYPE_HANDLE = "HANDLE";
    static final String TASK_PUSH_LINK_NAME = TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName();
    static final String TASK_PUSH_REASON_MANUAL = TaskProcessChainNodeTextEnum.TASK_PUSH_MANUAL.getTriggerReason();
    static final String TASK_PUSH_REASON_SYSTEM = TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getTriggerReason();
    static final String DEFAULT_TASK_LIST_ORDER_COLUMN = "t.update_date";
    static final Map<String, String> TASK_LIST_SORT_COLUMN_MAP = Map.ofEntries(
        Map.entry("id", "t.id"),
        Map.entry("unitId", "t.unit_id"),
        Map.entry("unit_id", "t.unit_id"),
        Map.entry("userId", "t.user_id"),
        Map.entry("user_id", "t.user_id"),
        Map.entry("riskId", "t.risk_id"),
        Map.entry("risk_id", "t.risk_id"),
        Map.entry("status", "t.status"),
        Map.entry("createDate", "t.create_date"),
        Map.entry("create_date", "t.create_date"),
        Map.entry("updateDate", "t.update_date"),
        Map.entry("update_date", "t.update_date"),
        Map.entry("checkTime", "t.check_time"),
        Map.entry("check_time", "t.check_time"),
        Map.entry("submitTime", "t.submit_time"),
        Map.entry("submit_time", "t.submit_time"),
        Map.entry("sourceType", "t.source_type"),
        Map.entry("source_type", "t.source_type"),
        Map.entry("planName", "t.plan_name"),
        Map.entry("plan_name", "t.plan_name"),
        Map.entry("planType", "t.plan_type"),
        Map.entry("plan_type", "t.plan_type"),
        Map.entry("responsiblePerson", "t.responsible_person"),
        Map.entry("responsible_person", "t.responsible_person"),
        Map.entry("responsiblePersonPhone", "t.responsible_person_phone"),
        Map.entry("responsible_person_phone", "t.responsible_person_phone"),
        Map.entry("overdue", "t.overdue"),
        Map.entry("quotaConsumed", "t.quota_consumed"),
        Map.entry("quota_consumed", "t.quota_consumed"),
        Map.entry("lastRemindTime", "t.last_remind_time"),
        Map.entry("last_remind_time", "t.last_remind_time"),
        Map.entry("reminderCount", "t.reminder_count"),
        Map.entry("reminder_count", "t.reminder_count"),
        Map.entry("closedTime", "t.closed_time"),
        Map.entry("closed_time", "t.closed_time"),
        Map.entry("dynamicRiskLevel", "ra.dynamic_risk_level"),
        Map.entry("dynamic_risk_level", "ra.dynamic_risk_level")
    );
    static final Set<String> TASK_LIST_CHINESE_STRING_SORT_COLUMNS = Set.of(
        "t.plan_name",
        "t.responsible_person"
    );
    final DzTaskDistListMapper baseMapper;
    final DzTaskDistListAddMapper dzTaskDistListAddMapper;
    final DzTaskDistListRemarkMapper dzTaskDistListRemarkMapper;
    final DzTaskDistListHistoryMapper dzTaskDistListHistoryMapper;
    final DzTaskHandleMapper dzTaskHandleMapper;
    final ISysUserService sysUserService;
    final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    final DzReportDisasterMapper dzReportDisasterMapper;
    final IAppTaskService appTaskService;
    final AppTaskProps appTaskProps;
    final ISlopeUnitService slopeUnitService;
    final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    final IDzUserAdRegionService dzUserAdRegionService;
    final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    final DzDefRespPlanMapper dzDefRespPlanMapper;
    final AdRegionMapper adRegionMapper;
    final TransactionTemplate transactionTemplate;
    final ITaskSmsContentService taskSmsContentService;
    final SmsSendService smsSendService;
    final IDzDefRespStartSmsConfigService dzDefRespStartSmsConfigService;
    final SysRoleMapper sysRoleMapper;
    final SysUserMapper sysUserMapper;
    final SysUserRoleMapper sysUserRoleMapper;
    final StringRedisTemplate stringRedisTemplate;
    final IDzTaskProcessChainNodeService taskProcessChainNodeService;
    final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;
    final IDzRiskAssessmentWarningRelationService dzRiskAssessmentWarningRelationService;
    final DifyAgentClient difyAgentClient;

    // Internal records/classes extracted to helper package

    /**
     * 查询任务派发清单
     *
     * @param id 主键
     * @return 任务派发清单
     */
    @Override
    public DzTaskDistListVo queryById(Long id) {
        DzTaskDistListVo task = baseMapper.selectVoById(id);
        if (task == null) {
            return null;
        }
        validateTaskViewAccess(task.getUnitId(), task.getSourceType(), task.getPlanType());
        return task;
    }

    @Override
    public DzTaskDistListHistoryVo queryHistoryById(Long id) {
        DzTaskDistListHistory history = dzTaskDistListHistoryMapper.selectById(id);
        if (history == null) {
            return null;
        }
        DzTaskDistListVo task = queryById(history.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        return toHistoryVo(history);
    }

    @Override
    public List<TaskProcessChainNodeVo> queryProcessChain(Long taskId) {
        List<TaskProcessChainNodeVo> processChain = taskProcessChainNodeService.queryChainByTaskId(taskId);
        fillProcessChainTaskDisplayFields(processChain);
        return processChain;
    }

    @Override
    public List<TaskProcessChainNodeVo> queryProcessChain(String chainId) {
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("链路id不能为空");
        }
        List<TaskProcessChainNodeVo> processChain = taskProcessChainNodeService.queryChainByChainId(chainId.trim());
        fillProcessChainTaskDisplayFields(processChain);
        return processChain;
    }

    @Override
    public TaskProcessCapabilityVo queryProcessChainCapabilities(Long taskId) {
        return querySavedProcessChainCapabilities(taskProcessChainNodeService, taskId);
    }

    @Override
    public TaskProcessCapabilityVo queryProcessChainCapabilities(String chainId) {
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("链路id不能为空");
        }
        String trimmedChainId = chainId.trim();
        return resolveProcessChainCapabilities(taskProcessChainNodeService.queryChainByChainId(trimmedChainId), trimmedChainId);
    }

    static TaskProcessCapabilityVo querySavedProcessChainCapabilities(IDzTaskProcessChainNodeService chainNodeService, Long taskId) {
        if (taskId == null) {
            return emptyProcessCapability();
        }
        String chainId = findSavedTaskChainIdWithoutRetry(chainNodeService, taskId);
        if (StringUtils.isBlank(chainId)) {
            return emptyProcessCapability();
        }
        return resolveProcessChainCapabilities(chainNodeService.queryChainByChainId(chainId), chainId);
    }

    static TaskProcessCapabilityVo resolveProcessChainCapabilities(List<TaskProcessChainNodeVo> processChain) {
        return resolveProcessChainCapabilities(processChain, resolveProcessChainId(processChain));
    }

    static TaskProcessCapabilityVo resolveProcessChainCapabilities(List<TaskProcessChainNodeVo> processChain, String chainId) {
        List<TaskProcessChainNodeVo> nodes = new ArrayList<>();
        collectProcessChainNodes(processChain, nodes);
        LinkedHashSet<String> currentCapabilities = new LinkedHashSet<>();
        LinkedHashMap<Integer, StageCapabilityAccumulator> stageCapabilityMap = new LinkedHashMap<>();
        TaskProcessChainNodeVo lastProgressNode = null;
        for (TaskProcessChainNodeVo node : nodes) {
            List<String> nodeCapabilities = resolveNodeCapabilities(node);
            addCapabilities(currentCapabilities, nodeCapabilities);
            StageCapabilityAccumulator stageAccumulator = resolveStageCapabilityAccumulator(stageCapabilityMap, node);
            if (stageAccumulator != null) {
                addCapabilities(stageAccumulator.currentCapabilities, nodeCapabilities);
            }
            if (!nodeCapabilities.isEmpty() || isTerminalProcessNode(node) || !resolveNextNodeCapabilities(node).isEmpty()) {
                lastProgressNode = node;
                if (stageAccumulator != null) {
                    stageAccumulator.lastProgressNode = node;
                }
            }
        }

        TaskProcessCapabilityVo vo = new TaskProcessCapabilityVo();
        vo.setChainId(chainId);
        vo.setCurrentCapabilities(new ArrayList<>(currentCapabilities));
        vo.setNextCapabilities(lastProgressNode == null ? List.of() : resolveNextNodeCapabilities(lastProgressNode));
        vo.setStageCapabilities(resolveStageCapabilities(stageCapabilityMap));
        return vo;
    }

    private static String resolveProcessChainId(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (StringUtils.isNotBlank(node.getChainId())) {
                return node.getChainId();
            }
            String childChainId = resolveProcessChainIdFromChildren(node.getChildChains());
            if (StringUtils.isNotBlank(childChainId)) {
                return childChainId;
            }
        }
        return null;
    }

    private static String resolveProcessChainIdFromChildren(List<List<TaskProcessChainNodeVo>> childChains) {
        if (childChains == null || childChains.isEmpty()) {
            return null;
        }
        for (List<TaskProcessChainNodeVo> childChain : childChains) {
            String chainId = resolveProcessChainId(childChain);
            if (StringUtils.isNotBlank(chainId)) {
                return chainId;
            }
        }
        return null;
    }

    private static void collectProcessChainNodes(List<TaskProcessChainNodeVo> nodes, List<TaskProcessChainNodeVo> result) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            result.add(node);
            if (node.getChildChains() != null) {
                node.getChildChains().forEach(childChain -> collectProcessChainNodes(childChain, result));
            }
        }
    }

    private static TaskProcessCapabilityVo emptyProcessCapability() {
        return new TaskProcessCapabilityVo();
    }

    private static TaskProcessCapabilityVo emptyProcessCapability(String chainId) {
        TaskProcessCapabilityVo vo = emptyProcessCapability();
        vo.setChainId(chainId);
        return vo;
    }

    private static void addCapabilities(Set<String> target, List<String> values) {
        target.addAll(values);
    }

    private static StageCapabilityAccumulator resolveStageCapabilityAccumulator(
        Map<Integer, StageCapabilityAccumulator> stageCapabilityMap, TaskProcessChainNodeVo node) {
        Integer stageType = resolveNodeStageType(node);
        if (stageType == null) {
            return null;
        }
        return stageCapabilityMap.computeIfAbsent(stageType, key -> new StageCapabilityAccumulator(stageType));
    }

    private static Integer resolveNodeStageType(TaskProcessChainNodeVo node) {
        if (node == null) {
            return null;
        }
        TaskProcessStageTypeEnum stage = TaskProcessStageTypeEnum.of(node.getStageType());
        if (stage != null) {
            return stage.getCode();
        }
        TaskProcessStageTypeEnum resolved = TaskProcessStageTypeEnum.fromNode(
            node.getLinkName(),
            node.getBizType() == null ? node.getType() : node.getBizType(),
            node.getSourceTypeCode() == null ? node.getSourceType() : node.getSourceTypeCode()
        );
        return resolved == null ? null : resolved.getCode();
    }

    private static List<TaskProcessStageCapabilityVo> resolveStageCapabilities(
        Map<Integer, StageCapabilityAccumulator> stageCapabilityMap) {
        if (stageCapabilityMap.isEmpty()) {
            return List.of();
        }
        return stageCapabilityMap.values().stream()
            .map(StageCapabilityAccumulator::toVo)
            .toList();
    }

    private static List<String> resolveNodeCapabilities(TaskProcessChainNodeVo node) {
        if (node == null) {
            return List.of();
        }
        TaskProcessNodeCategoryEnum category = TaskProcessNodeCategoryEnum.of(node.getNodeCategory());
        if (category != null) {
            List<String> capabilities = resolveNodeCapabilitiesByCategory(category);
            if (!capabilities.isEmpty()) {
                return capabilities;
            }
        }

        String normalizedLinkName = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        List<String> capabilities = resolveNodeCapabilitiesByLinkName(normalizedLinkName);
        if (!capabilities.isEmpty()) {
            return capabilities;
        }
        return resolveNodeCapabilitiesBySourceType(node);
    }

    private static List<String> resolveNodeCapabilitiesByCategory(TaskProcessNodeCategoryEnum category) {
        return switch (category) {
            case TASK_INSPECTING -> List.of("APP端", "地图定位");
            case TASK_FEEDBACK -> List.of("APP端", "地图定位");
            case TECH_ASSIST_APPLY -> List.of("APP端", "AI会商系统");
            case HANDLE_START -> List.of("大语言模型", "风险评价算法", "空间分析");
            case SINGLE_DEF_RESP_START -> List.of("路线规划算法", "APP端", "短信系统");
            case EMERGENCY_BATCH_DISPATCH, TASK_GENERATE -> List.of("大语言模型", "风险评价算法", "空间分析");
            case REGION_DEF_RESP_START -> List.of("报告解读模型", "大语言模型", "AI会商系统", "短信系统");
            case DEF_RESP_BATCH_DISPATCH -> List.of("APP端", "短信系统");
            case PUBLIC_REPORT_REPORT -> List.of("APP端", "多模态大模型");
            case TASK_FEEDBACK_REPORT -> List.of("多模态大模型");
            case TASK_PUSH -> List.of("APP端", "短信系统");
            case TASK_CLOSE, TASK_OVERDUE, HANDLE_ARCHIVED -> List.of();
            default -> List.of();
        };
    }

    private static List<String> resolveNodeCapabilitiesByLinkName(String normalizedLinkName) {
        if (StringUtils.isBlank(normalizedLinkName)) {
            return List.of();
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalizedLinkName)
            || isTaskDispatchLink(normalizedLinkName)) {
            return List.of("APP端", "短信系统");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.PUBLIC_REPORT.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName(), normalizedLinkName)) {
            return List.of("APP端", "多模态大模型");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), normalizedLinkName)) {
            return List.of("APP端", "地图定位");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), normalizedLinkName)) {
            return List.of("多模态大模型");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalizedLinkName)) {
            return List.of("APP端", "AI会商系统");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), normalizedLinkName)) {
            return List.of("大语言模型", "风险评价算法", "空间分析");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(), normalizedLinkName)) {
            return List.of("大语言模型", "风险评价算法", "空间分析");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), normalizedLinkName)) {
            return List.of("路线规划算法", "APP端", "短信系统");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(), normalizedLinkName)) {
            return List.of("报告解读模型", "大语言模型", "AI会商系统", "短信系统");
        }
        return List.of();
    }

    private static List<String> resolveNodeCapabilitiesBySourceType(TaskProcessChainNodeVo node) {
        Integer sourceType = node.getSourceTypeCode() == null ? node.getSourceType() : node.getSourceTypeCode();
        if (TaskProcessSourceTypeEnum.REPORT.getCode().equals(sourceType)) {
            return List.of("APP端", "多模态大模型");
        }
        if (TaskProcessSourceTypeEnum.DEF_RESP.getCode().equals(sourceType)
            && TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getBizType())) {
            return List.of("报告解读模型", "大语言模型", "AI会商系统", "短信系统");
        }
        if (TaskProcessSourceTypeEnum.HANDLE.getCode().equals(sourceType)
            && TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getBizType())) {
            return List.of("大语言模型", "风险评价算法", "空间分析");
        }
        return List.of();
    }

    private static List<String> resolveNextNodeCapabilities(TaskProcessChainNodeVo node) {
        if (node == null || isTerminalProcessNode(node)) {
            return List.of();
        }
        TaskProcessNodeCategoryEnum category = TaskProcessNodeCategoryEnum.of(node.getNodeCategory());
        if (category != null) {
            List<String> capabilities = resolveNextNodeCapabilitiesByCategory(category);
            if (!capabilities.isEmpty()) {
                return capabilities;
            }
        }

        String normalizedLinkName = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        if (StringUtils.isBlank(normalizedLinkName)) {
            return List.of();
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalizedLinkName)
            || isTaskDispatchLink(normalizedLinkName)) {
            return List.of("APP端", "地图定位");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), normalizedLinkName)) {
            return List.of("APP端", "地图定位", "多模态大模型");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), normalizedLinkName)) {
            return List.of("大语言模型", "风险评价算法", "空间分析");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalizedLinkName)) {
            return List.of("APP端", "短信系统");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getLinkName(), normalizedLinkName)) {
            return List.of("路线规划算法", "APP端", "短信系统");
        }
        return List.of();
    }

    private static List<String> resolveNextNodeCapabilitiesByCategory(TaskProcessNodeCategoryEnum category) {
        return switch (category) {
            case TASK_INSPECTING -> List.of("APP端", "地图定位", "多模态大模型");
            case TASK_FEEDBACK, TASK_FEEDBACK_REPORT -> List.of("大语言模型", "风险评价算法", "空间分析");
            case TECH_ASSIST_APPLY -> List.of("APP端", "短信系统");
            case HANDLE_START -> List.of("路线规划算法", "APP端", "短信系统");
            case EMERGENCY_BATCH_DISPATCH, TASK_GENERATE -> List.of("APP端", "短信系统");
            case TASK_PUSH -> List.of("APP端", "地图定位");
            case TASK_CLOSE, TASK_OVERDUE, HANDLE_ARCHIVED -> List.of();
            default -> List.of();
        };
    }

    private static class StageCapabilityAccumulator {

        private final Integer stageType;

        private final LinkedHashSet<String> currentCapabilities = new LinkedHashSet<>();

        private TaskProcessChainNodeVo lastProgressNode;

        private StageCapabilityAccumulator(Integer stageType) {
            this.stageType = stageType;
        }

        private TaskProcessStageCapabilityVo toVo() {
            TaskProcessStageCapabilityVo vo = new TaskProcessStageCapabilityVo();
            vo.setStageType(stageType);
            vo.setStageTypeLabel(TaskProcessStageTypeEnum.label(stageType));
            vo.setCurrentCapabilities(new ArrayList<>(currentCapabilities));
            vo.setNextCapabilities(lastProgressNode == null ? List.of() : resolveNextNodeCapabilities(lastProgressNode));
            return vo;
        }
    }

    private static boolean isTaskDispatchLink(String normalizedLinkName) {
        return normalizedLinkName != null
            && (normalizedLinkName.startsWith("下发") && normalizedLinkName.endsWith("任务")
            || normalizedLinkName.startsWith("生成") && normalizedLinkName.endsWith("任务"));
    }

    private static boolean isTerminalProcessNode(TaskProcessChainNodeVo node) {
        TaskProcessNodeCategoryEnum category = TaskProcessNodeCategoryEnum.of(node.getNodeCategory());
        if (category == TaskProcessNodeCategoryEnum.TASK_CLOSE
            || category == TaskProcessNodeCategoryEnum.TASK_OVERDUE
            || category == TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED) {
            return true;
        }
        String normalizedLinkName = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_OVERDUE_DEFAULT.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(), normalizedLinkName)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getLinkName(), normalizedLinkName);
    }

    @Override
    public List<DzTaskDistListHistoryVo> queryProcessChainHistories(String chainId, Long taskId) {
        List<TaskProcessChainNodeVo> processChain;
        if (StringUtils.isNotBlank(chainId)) {
            processChain = taskProcessChainNodeService.queryChainByChainId(chainId.trim());
        } else if (taskId != null) {
            processChain = queryProcessChain(taskId);
        } else {
            throw new ServiceException("chainId或taskId不能为空");
        }
        LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
        collectProcessChainTaskIds(processChain, taskIds);
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return dzTaskDistListHistoryMapper.selectListByTaskIds(new ArrayList<>(taskIds)).stream()
            .map(this::toHistoryVo)
            .sorted(Comparator
                .comparing(DzTaskDistListHistoryVo::getCreateDate, Comparator.nullsLast(Date::compareTo))
                .thenComparing(DzTaskDistListHistoryVo::getId, Comparator.nullsLast(Long::compareTo))
                .reversed())
            .toList();
    }

    private void collectProcessChainTaskIds(List<TaskProcessChainNodeVo> nodes, Set<Long> taskIds) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getTaskId() != null) {
                taskIds.add(node.getTaskId());
            }
            if (node.getChildChains() != null) {
                node.getChildChains().forEach(childChain -> collectProcessChainTaskIds(childChain, taskIds));
            }
        }
    }

    private Long resolveCurrentProcessChainTaskId(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Long taskId = null;
        for (TaskProcessChainNodeVo node : nodes) {
            Long currentTaskId = resolveCurrentProcessChainTaskId(node);
            if (currentTaskId != null) {
                taskId = currentTaskId;
            }
        }
        return taskId;
    }

    private Long resolveCurrentProcessChainTaskId(TaskProcessChainNodeVo node) {
        if (node == null) {
            return null;
        }
        Long taskId = node.getTaskId();
        if (taskId == null && TaskProcessBizTypeEnum.TASK.getCode().equals(node.getBizType())) {
            taskId = node.getBizId();
        }
        if (node.getChildChains() != null) {
            for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                Long childTaskId = resolveCurrentProcessChainTaskId(childChain);
                if (childTaskId != null) {
                    taskId = childTaskId;
                }
            }
        }
        return taskId;
    }

    Long resolveProcessChainRelatedTaskId(List<TaskProcessChainNodeVo> nodes) {
        ProcessChainBusinessIds businessIds = new ProcessChainBusinessIds(
            new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>()
        );
        collectProcessChainBusinessIds(nodes, businessIds);
        if (businessIds.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<DzTaskDistList> wrapper = Wrappers.<DzTaskDistList>lambdaQuery()
            .and(query -> appendRelatedTaskConditions(query, businessIds))
            .orderByDesc(DzTaskDistList::getUpdateDate, DzTaskDistList::getCreateDate, DzTaskDistList::getId)
            .last("limit 1");
        DzTaskDistList task = baseMapper.selectOne(wrapper);
        return task == null ? null : task.getId();
    }

    Long resolveCurrentProcessChainReportId(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Long reportId = null;
        for (TaskProcessChainNodeVo node : nodes) {
            Long currentReportId = resolveCurrentProcessChainReportId(node);
            if (currentReportId != null) {
                reportId = currentReportId;
            }
        }
        return reportId;
    }

    private Long resolveCurrentProcessChainReportId(TaskProcessChainNodeVo node) {
        if (node == null) {
            return null;
        }
        Long reportId = null;
        Integer bizType = node.getBizType() == null ? node.getType() : node.getBizType();
        if (TaskProcessBizTypeEnum.REPORT.getCode().equals(bizType)) {
            reportId = node.getBizId();
        }
        if (node.getChildChains() != null) {
            for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                Long childReportId = resolveCurrentProcessChainReportId(childChain);
                if (childReportId != null) {
                    reportId = childReportId;
                }
            }
        }
        return reportId;
    }

    private DzReportDisasterVo queryAiProcessChainReport(List<TaskProcessChainNodeVo> processChain) {
        Long reportId = resolveCurrentProcessChainReportId(processChain);
        return reportId == null ? null : dzReportDisasterMapper.selectVoById(reportId);
    }

    private Long resolveCurrentProcessChainHandleId(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Long handleId = null;
        for (TaskProcessChainNodeVo node : nodes) {
            Long currentHandleId = resolveCurrentProcessChainHandleId(node);
            if (currentHandleId != null) {
                handleId = currentHandleId;
            }
        }
        return handleId;
    }

    private Long resolveCurrentProcessChainHandleId(TaskProcessChainNodeVo node) {
        if (node == null) {
            return null;
        }
        Long handleId = null;
        Integer bizType = node.getBizType() == null ? node.getType() : node.getBizType();
        if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(bizType)) {
            handleId = node.getBizId();
        }
        if (node.getChildChains() != null) {
            for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                Long childHandleId = resolveCurrentProcessChainHandleId(childChain);
                if (childHandleId != null) {
                    handleId = childHandleId;
                }
            }
        }
        return handleId;
    }

    private Map<String, Object> queryAiProcessChainHandleContext(List<TaskProcessChainNodeVo> processChain) {
        Long handleId = resolveCurrentProcessChainHandleId(processChain);
        if (handleId == null) {
            return null;
        }
        DzTaskHandle handle = dzTaskHandleMapper.selectById(handleId);
        if (handle == null) {
            return null;
        }
        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        DzTaskHandleDetailContentVo latestAiReport = dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            DetailContentTypeEnum.AI_REPORT.getCode(),
            null
        );
        return buildAiProcessChainHandleContext(handle, sceneRecord, latestAiReport);
    }

    private static Map<String, Object> buildAiProcessChainHandleContext(DzTaskHandle handle,
                                                                         DzTaskHandleSceneRecordVo sceneRecord,
                                                                         DzTaskHandleDetailContentVo latestAiReport) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("contextType", AI_PROCESS_CONTEXT_TYPE_HANDLE);
        putIfNotNull(context, "id", handle.getId());
        context.put("handle", handle);
        putIfNotNull(context, "sceneRecord", sceneRecord);
        putIfNotNull(context, "latestAiReport", latestAiReport);
        return context;
    }

    private void collectProcessChainBusinessIds(List<TaskProcessChainNodeVo> nodes, ProcessChainBusinessIds businessIds) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            collectProcessChainBusinessId(node, businessIds);
        }
    }

    private void collectProcessChainBusinessId(TaskProcessChainNodeVo node, ProcessChainBusinessIds businessIds) {
        if (node == null) {
            return;
        }
        Integer bizType = node.getBizType() == null ? node.getType() : node.getBizType();
        Long bizId = node.getBizId();
        if (bizId != null) {
            if (TaskProcessBizTypeEnum.REPORT.getCode().equals(bizType)) {
                businessIds.reportIds().add(bizId);
            } else if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(bizType)) {
                businessIds.handleIds().add(bizId);
            } else if (TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(bizType)) {
                businessIds.defIds().add(bizId);
            }
        }
        if (node.getChildChains() != null) {
            node.getChildChains().forEach(childChain -> collectProcessChainBusinessIds(childChain, businessIds));
        }
    }

    private void appendRelatedTaskConditions(LambdaQueryWrapper<DzTaskDistList> query,
                                             ProcessChainBusinessIds businessIds) {
        boolean hasCondition = false;
        if (!businessIds.reportIds().isEmpty()) {
            query.in(DzTaskDistList::getReportId, businessIds.reportIds());
            hasCondition = true;
        }
        if (!businessIds.handleIds().isEmpty()) {
            if (hasCondition) {
                query.or();
            }
            query.in(DzTaskDistList::getHandleId, businessIds.handleIds());
            hasCondition = true;
        }
        if (!businessIds.defIds().isEmpty()) {
            if (hasCondition) {
                query.or();
            }
            query.in(DzTaskDistList::getDefId, businessIds.defIds());
        }
    }

    private record ProcessChainBusinessIds(LinkedHashSet<Long> reportIds,
                                           LinkedHashSet<Long> handleIds,
                                           LinkedHashSet<Long> defIds) {
        private boolean isEmpty() {
            return reportIds.isEmpty() && handleIds.isEmpty() && defIds.isEmpty();
        }
    }

    private DzTaskDistListHistoryVo toHistoryVo(DzTaskDistListHistory history) {
        DzTaskDistListHistoryVo vo = new DzTaskDistListHistoryVo();
        vo.setId(history.getId());
        vo.setTaskId(history.getTaskId());
        vo.setTextRecord(history.getTextRecord());
        vo.setRemark(history.getTextRecord());
        vo.setSubmitStatus(history.getSubmitStatus());
        vo.setScenePhoto(history.getScenePhoto());
        vo.setCheckCenter(history.getCheckCenter());
        vo.setCheckCenterLocation(history.getCheckCenterLocation());
        vo.setCreateDate(history.getCreateDate());
        vo.setUpdateDate(history.getUpdateDate());
        vo.setUserId(history.getUserId());
        vo.setUserName(history.getUserName());
        return vo;
    }

    @Override
    public TaskAiProcessChainVo queryAiProcessChain(Long taskId) {
        DzTaskDistListVo task = queryAiProcessChainTask(taskId);
        List<TaskProcessChainNodeVo> processChain = queryProcessChain(taskId);
        return buildAiProcessChain(task, null, null, processChain);
    }

    @Override
    public TaskAiProcessChainVo queryAiProcessChain(String chainId) {
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("链路id不能为空");
        }
        String trimmedChainId = chainId.trim();
        List<TaskProcessChainNodeVo> processChain = taskProcessChainNodeService.queryChainByChainId(trimmedChainId);
        Long taskId = resolveCurrentProcessChainTaskId(processChain);
        if (taskId == null) {
            taskId = resolveProcessChainRelatedTaskId(processChain);
        }
        if (taskId == null) {
            DzReportDisasterVo report = queryAiProcessChainReport(processChain);
            if (report != null) {
                return buildAiProcessChain(null, report, null, processChain);
            }
            Map<String, Object> handleContext = queryAiProcessChainHandleContext(processChain);
            if (handleContext == null) {
                throw new ServiceException("流程链路未找到可用于AI分析的任务、报灾报告或处置管理, chainId=" + trimmedChainId);
            }
            return buildAiProcessChain(null, null, handleContext, processChain);
        }
        DzTaskDistListVo task = queryAiProcessChainTask(taskId);
        return buildAiProcessChain(task, null, null, processChain);
    }

    private DzTaskDistListVo queryAiProcessChainTask(Long taskId) {
        DzTaskDistListVo task = queryById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        fillTaskBaseInfo(List.of(task));
        return task;
    }

    private TaskAiProcessChainVo buildAiProcessChain(DzTaskDistListVo task, DzReportDisasterVo report,
                                                     Map<String, Object> handleContext,
                                                     List<TaskProcessChainNodeVo> processChain) {
        fillProcessChainTaskDisplayFields(processChain);
        TaskAiProcessChainVo result = callAiProcessChain(task, report, handleContext, processChain);
        result.setProcessChain(processChain);
        fillAiProcessChainBusinessIds(result, task, report, processChain);
        return result;
    }

    private void fillProcessChainTaskDisplayFields(List<TaskProcessChainNodeVo> processChain) {
        LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
        collectProcessChainTaskIds(processChain, taskIds);
        if (taskIds.isEmpty()) {
            return;
        }
        Map<Long, DzTaskDistList> taskMap = baseMapper.selectBatchIds(taskIds).stream()
            .filter(Objects::nonNull)
            .filter(task -> task.getId() != null)
            .collect(java.util.stream.Collectors.toMap(DzTaskDistList::getId, task -> task, (left, right) -> left));
        fillProcessChainTaskDisplayFields(processChain, taskMap);
    }

    private void fillProcessChainTaskDisplayFields(List<TaskProcessChainNodeVo> nodes, Map<Long, DzTaskDistList> taskMap) {
        if (nodes == null || nodes.isEmpty() || taskMap == null || taskMap.isEmpty()) {
            return;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            DzTaskDistList task = taskMap.get(node.getTaskId());
            if (task == null && TaskProcessBizTypeEnum.TASK.getCode().equals(node.getBizType())) {
                task = taskMap.get(node.getBizId());
            }
            if (task != null) {
                node.setPlanType(resolveProcessChainPlanType(task));
                node.setTaskType(resolveProcessChainTaskType(task));
            }
            fillChildProcessChainTaskDisplayFields(node.getChildChains(), taskMap);
        }
    }

    private void fillChildProcessChainTaskDisplayFields(List<List<TaskProcessChainNodeVo>> childChains,
                                                        Map<Long, DzTaskDistList> taskMap) {
        if (childChains == null || childChains.isEmpty()) {
            return;
        }
        childChains.forEach(childChain -> fillProcessChainTaskDisplayFields(childChain, taskMap));
    }

    static String resolveProcessChainTaskType(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        if (DzTaskDistList.isMonitoringPlanType(task.getPlanType())) {
            if (StringUtils.isNotBlank(task.getTaskType())) {
                return task.getTaskType().trim();
            }
            return resolveStoredTaskType(task.getSourceType());
        }
        PlanTypeEnum planType = PlanTypeEnum.getByCode(task.getPlanType());
        if (planType != null) {
            return planType.getName();
        }
        if (StringUtils.isNotBlank(task.getTaskType())) {
            return task.getTaskType().trim();
        }
        return resolveStoredTaskType(task.getSourceType());
    }

    static Integer resolveProcessChainPlanType(DzTaskDistList task) {
        if (task == null || task.getPlanType() == null) {
            return null;
        }
        return task.getPlanType();
    }

    private void fillAiProcessChainBusinessIds(TaskAiProcessChainVo result, DzTaskDistListVo task,
                                               DzReportDisasterVo report,
                                               List<TaskProcessChainNodeVo> processChain) {
        if (result == null) {
            return;
        }
        LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
        LinkedHashSet<Long> reportIds = new LinkedHashSet<>();
        LinkedHashSet<Long> reportCandidateIds = new LinkedHashSet<>();
        applyAiProcessChainTaskBusinessIds(result, task, taskIds);
        applyAiProcessChainReportBusinessIds(report, taskIds, reportIds);
        collectAiProcessChainNodeBusinessIds(result, processChain, taskIds, reportIds, reportCandidateIds);
        fillAiProcessChainTaskBusinessIds(result, taskIds, reportIds);
        normalizeAiProcessChainDefId(result);
        fillAiProcessChainDefRespHandleId(result);
        addFeedbackReportIdsByIds(reportIds, reportCandidateIds);
        addFeedbackReportIdsByTaskIds(reportIds, taskIds);
        if (!reportIds.isEmpty()) {
            result.setReportIds(new ArrayList<>(reportIds));
        }
    }

    private void applyAiProcessChainReportBusinessIds(DzReportDisasterVo report, Set<Long> taskIds,
                                                      Set<Long> reportIds) {
        if (report == null) {
            return;
        }
        addIfNotNull(reportIds, report.getId());
        addIfNotNull(taskIds, report.getTaskId());
    }

    private void applyAiProcessChainTaskBusinessIds(TaskAiProcessChainVo result, DzTaskDistListVo task,
                                                    Set<Long> taskIds) {
        if (task == null) {
            return;
        }
        addIfNotNull(taskIds, task.getId());
        addIfNotNull(taskIds, task.getRelatedTaskId());
        if (result.getHandleId() == null) {
            result.setHandleId(task.getHandleId());
        }
        if (result.getDefId() == null) {
            result.setDefId(task.getDefId());
        }
    }

    private void collectAiProcessChainNodeBusinessIds(TaskAiProcessChainVo result, List<TaskProcessChainNodeVo> nodes,
                                                      Set<Long> taskIds, Set<Long> reportIds,
                                                      Set<Long> reportCandidateIds) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            collectAiProcessChainNodeBusinessId(result, node, taskIds, reportIds, reportCandidateIds);
        }
    }

    private void collectAiProcessChainNodeBusinessId(TaskAiProcessChainVo result, TaskProcessChainNodeVo node,
                                                     Set<Long> taskIds, Set<Long> reportIds,
                                                     Set<Long> reportCandidateIds) {
        if (node == null) {
            return;
        }
        addIfNotNull(taskIds, node.getTaskId());
        if (TaskProcessBizTypeEnum.TASK.getCode().equals(node.getBizType()) || TaskProcessBizTypeEnum.TASK.getCode().equals(node.getType())) {
            addIfNotNull(taskIds, node.getBizId());
        } else if ((TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getBizType())
            || TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getType())) && result.getHandleId() == null) {
            result.setHandleId(node.getBizId());
        } else if ((TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getBizType())
            || TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getType())) && result.getDefId() == null) {
            result.setDefId(node.getBizId());
        } else if ((TaskProcessBizTypeEnum.REPORT.getCode().equals(node.getBizType())
            || TaskProcessBizTypeEnum.REPORT.getCode().equals(node.getType())) && node.getBizId() != null) {
            if (isFeedbackReportSourceType(node.getSourceType()) || isPublicReportSourceType(node.getSourceType())) {
                reportIds.add(node.getBizId());
            } else {
                reportCandidateIds.add(node.getBizId());
            }
        }
        if (node.getChildChains() != null) {
            node.getChildChains().forEach(childChain ->
                collectAiProcessChainNodeBusinessIds(result, childChain, taskIds, reportIds, reportCandidateIds));
        }
    }

    private void fillAiProcessChainTaskBusinessIds(TaskAiProcessChainVo result, Set<Long> taskIds, Set<Long> reportIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        List<DzTaskDistList> tasks = baseMapper.selectBatchIds(taskIds);
        LinkedHashSet<Long> taskReportIds = new LinkedHashSet<>();
        for (DzTaskDistList task : tasks) {
            if (task == null) {
                continue;
            }
            if (result.getHandleId() == null) {
                result.setHandleId(task.getHandleId());
            }
            if (result.getDefId() == null) {
                result.setDefId(task.getDefId());
            }
            addIfNotNull(taskReportIds, task.getReportId());
        }
        addFeedbackReportIdsByIds(reportIds, taskReportIds);
    }

    private void normalizeAiProcessChainDefId(TaskAiProcessChainVo result) {
        if (result == null || result.getDefId() == null) {
            return;
        }
        Long currentDefId = result.getDefId();
        DefRespPlan currentPlan = dzDefRespPlanMapper.selectById(currentDefId);
        if (!isTownRegionDefRespPlan(currentPlan)) {
            return;
        }
        if (currentPlan.getParentDefId() == null) {
            log.warn("AI流程链防御响应defId归一化跳过：乡镇级防御响应缺少parentDefId, defId={}", currentDefId);
            return;
        }
        DefRespPlan parentPlan = dzDefRespPlanMapper.selectById(currentPlan.getParentDefId());
        Long normalizedDefId = resolveAiProcessChainResponseDefId(currentDefId, currentPlan, parentPlan);
        if (Objects.equals(currentDefId, normalizedDefId)) {
            log.warn("AI流程链防御响应defId归一化跳过：父级防御响应不是县级区域响应, defId={}, parentDefId={}",
                currentDefId, currentPlan.getParentDefId());
            return;
        }
        result.setDefId(normalizedDefId);
    }

    static Long resolveAiProcessChainResponseDefId(Long currentDefId, DefRespPlan currentPlan, DefRespPlan parentPlan) {
        if (currentDefId == null || !isTownRegionDefRespPlan(currentPlan) || !isCountyRegionDefRespPlan(parentPlan)) {
            return currentDefId;
        }
        return parentPlan.getId() != null ? parentPlan.getId() : currentPlan.getParentDefId();
    }

    private static boolean isTownRegionDefRespPlan(DefRespPlan plan) {
        return isRegionDefRespPlan(plan)
            && RegionScopeTypeEnum.TOWN.getCode().equals(plan.getRegionScopeType());
    }

    private static boolean isCountyRegionDefRespPlan(DefRespPlan plan) {
        return isRegionDefRespPlan(plan)
            && RegionScopeTypeEnum.COUNTY.getCode().equals(plan.getRegionScopeType());
    }

    private static boolean isRegionDefRespPlan(DefRespPlan plan) {
        return plan != null && DefRespPlanTypeEnum.REGION.getCode().equals(plan.getType());
    }

    private void fillAiProcessChainDefRespHandleId(TaskAiProcessChainVo result) {
        if (result == null || result.getHandleId() != null || result.getDefId() == null) {
            return;
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(result.getDefId());
        if (defRespPlan != null) {
            result.setHandleId(defRespPlan.getHandleId());
        }
    }

    private void addFeedbackReportIdsByTaskIds(Set<Long> reportIds, Set<Long> taskIds) {
        if (reportIds == null || taskIds == null || taskIds.isEmpty()) {
            return;
        }
        dzReportDisasterMapper.selectList(
                Wrappers.<DzReportDisaster>lambdaQuery()
                    .eq(DzReportDisaster::getSourceType, 1)
                    .in(DzReportDisaster::getTaskId, taskIds)
                    .orderByAsc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            )
            .stream()
            .map(DzReportDisaster::getId)
            .forEach(reportIds::add);
    }

    private void addFeedbackReportIdsByIds(Set<Long> reportIds, Collection<Long> candidateIds) {
        if (reportIds == null || candidateIds == null || candidateIds.isEmpty()) {
            return;
        }
        List<Long> ids = candidateIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            return;
        }
        dzReportDisasterMapper.selectBatchIds(ids).stream()
            .filter(report -> report != null && Objects.equals(report.getSourceType(), 1))
            .map(DzReportDisaster::getId)
            .forEach(reportIds::add);
    }

    private boolean isFeedbackReportSourceType(Integer sourceType) {
        return TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode().equals(sourceType);
    }

    private boolean isPublicReportSourceType(Integer sourceType) {
        return TaskProcessSourceTypeEnum.REPORT.getCode().equals(sourceType);
    }

    private void addIfNotNull(Collection<Long> target, Long value) {
        if (target != null && value != null) {
            target.add(value);
        }
    }

    private TaskAiProcessChainVo callAiProcessChain(DzTaskDistListVo task, DzReportDisasterVo report,
                                                    Map<String, Object> handleContext,
                                                    List<TaskProcessChainNodeVo> processChain) {
        DzRiskAssessmentVo riskInfo = task == null ? null : resolveTaskRiskInfo(task);
        Map<String, Object> inputs = buildAiProcessChainInputs(task, report, handleContext, riskInfo, processChain);
        String taskInfo = Objects.toString(inputs.get("task_info"), null);
        String reportInfo = Objects.toString(inputs.get("report_info"), null);
        String slopeUnitInfo = Objects.toString(inputs.get("slope_unit_info"), null);
        String riskInfoInput = Objects.toString(inputs.get("risk_info"), null);
        String processChainInput = Objects.toString(inputs.get("process_chain"), null);
        ChatMessage message = ChatMessage.builder()
            .query(AI_PROCESS_CHAIN_QUERY)
            .inputs(inputs)
            .responseMode(ResponseMode.BLOCKING)
            .user("admin")
            .conversationId("")
            .build();
        try {
            ChatMessageResponse response = difyAgentClient.getAiTaskProcessChainAgent().sendChatMessage(message);
            String answer = response == null ? null : response.getAnswer();
            if (StringUtils.isBlank(answer)) {
                throw new ServiceException("AI未返回任务流程链路分析结果");
            }
            return parseAiProcessChainAnswer(answer);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Dify任务流程链路AI分析失败，taskId={}, reportId={}, handleId={}, taskInfoLength={}, reportInfoLength={}, slopeUnitInfoLength={}, riskInfoLength={}, processChainLength={}",
                task == null ? null : task.getId(), report == null ? null : report.getId(),
                handleContext == null ? null : handleContext.get("id"),
                stringLength(taskInfo), stringLength(reportInfo), stringLength(slopeUnitInfo), stringLength(riskInfoInput),
                stringLength(processChainInput), e);
            throw new ServiceException("调用Dify任务流程链路AI分析失败");
        }
    }

    static Map<String, Object> buildAiProcessChainInputs(DzTaskDistListVo task, DzReportDisasterVo report,
                                                         Map<String, Object> handleContext,
                                                         DzRiskAssessmentVo riskInfo,
                                                         List<TaskProcessChainNodeVo> processChain) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        if (task != null) {
            inputs.put("task_info", JSONUtil.toJsonStr(task));
            inputs.put("slope_unit_info", JSONUtil.toJsonStr(task.getSlopeUnit()));
        } else if (report != null) {
            String reportInfo = JSONUtil.toJsonStr(report);
            inputs.put("task_info", JSONUtil.toJsonStr(buildAiProcessChainReportContext(report)));
            inputs.put("report_info", reportInfo);
        } else if (handleContext != null) {
            inputs.put("task_info", JSONUtil.toJsonStr(handleContext));
        }
        if (riskInfo != null) {
            inputs.put("risk_info", JSONUtil.toJsonStr(riskInfo));
        }
        inputs.put("process_chain", buildAiProcessChainInput(processChain));
        return inputs;
    }

    private static Map<String, Object> buildAiProcessChainReportContext(DzReportDisasterVo report) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("contextType", AI_PROCESS_CONTEXT_TYPE_REPORT);
        putIfNotNull(context, "id", report.getId());
        putIfNotNull(context, "userId", report.getUserId());
        putIfNotBlank(context, "userName", report.getUserName());
        putIfNotBlank(context, "userPhone", report.getUserPhone());
        putIfNotBlank(context, "userRole", report.getUserRole());
        putIfNotNull(context, "checkTime", report.getCheckTime());
        putIfNotBlank(context, "checkCenter", report.getCheckCenter());
        putIfNotBlank(context, "checkCenterLocation", report.getCheckCenterLocation());
        putIfNotBlank(context, "detailedAddress", report.getDetailedAddress());
        putIfNotBlank(context, "photos", report.getPhotos());
        putIfNotBlank(context, "sceneTextRecord", report.getSceneTextRecord());
        putIfNotNull(context, "aiRiskLevel", report.getAiRiskLevel());
        putIfNotBlank(context, "aiRiskLabel", report.getAiRiskLabel());
        putIfNotBlank(context, "aiReportDetail", report.getAiReportDetail());
        putIfNotNull(context, "aiVisionProps", report.getAiVisionProps());
        putIfNotNull(context, "manualRiskLevel", report.getManualRiskLevel());
        putIfNotBlank(context, "manualRiskRemark", report.getManualRiskRemark());
        putIfNotNull(context, "status", report.getStatus());
        putIfNotNull(context, "sourceType", report.getSourceType());
        context.put("sourceTypeLabel", TaskProcessSourceTypeEnum.fromReportSourceType(report.getSourceType()).getLabel());
        putIfNotNull(context, "taskId", report.getTaskId());
        putIfNotNull(context, "createDate", report.getCreateDate());
        putIfNotNull(context, "updateDate", report.getUpdateDate());
        putIfNotBlank(context, "county", report.getCounty());
        putIfNotBlank(context, "street", report.getStreet());
        putIfNotBlank(context, "village", report.getVillage());
        return context;
    }

    static String buildAiProcessChainInput(List<TaskProcessChainNodeVo> processChain) {
        String compactJson = JSONUtil.toJsonStr(toAiProcessChainNodes(processChain, true));
        if (stringLength(compactJson) <= AI_PROCESS_CHAIN_INPUT_MAX_LENGTH) {
            return compactJson;
        }
        String summaryJson = JSONUtil.toJsonStr(buildAiProcessChainSummary(processChain));
        if (stringLength(summaryJson) <= AI_PROCESS_CHAIN_INPUT_MAX_LENGTH) {
            return summaryJson;
        }
        return summaryJson.substring(0, AI_PROCESS_CHAIN_INPUT_SAFE_LENGTH)
            + "\n...内容过长，已截断；请基于以上父链和子链摘要分析。";
    }

    static List<Map<String, Object>> toAiProcessChainNodes(List<TaskProcessChainNodeVo> nodes, boolean includeChildChains) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            Map<String, Object> item = toAiProcessChainNode(node);
            if (includeChildChains && node.getChildChains() != null && !node.getChildChains().isEmpty()) {
                List<List<Map<String, Object>>> childChains = new ArrayList<>();
                for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                    List<Map<String, Object>> childNodes = toAiProcessChainNodes(childChain, true);
                    if (!childNodes.isEmpty()) {
                        childChains.add(childNodes);
                    }
                }
                putIfNotEmpty(item, "childChains", childChains);
            }
            result.add(item);
        }
        return result;
    }

    private static Map<String, Object> buildAiProcessChainSummary(List<TaskProcessChainNodeVo> processChain) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summaryMode", true);
        summary.put("note", "流程链路内容超过Dify单变量长度限制，已保留父链完整精简节点，并将子链压缩为数量和首尾节点摘要。");
        summary.put("mainChain", toAiProcessChainNodes(processChain, false));
        summary.put("childChainSummaries", summarizeAiProcessChildChains(processChain));
        return summary;
    }

    private static List<Map<String, Object>> summarizeAiProcessChildChains(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getChildChains() != null && !node.getChildChains().isEmpty()) {
                Map<String, Object> parentSummary = new LinkedHashMap<>();
                putIfNotNull(parentSummary, "parentNodeId", node.getNodeId());
                putIfNotBlank(parentSummary, "parentLinkName", node.getLinkName());
                parentSummary.put("childChainCount", node.getChildChains().size());
                List<Map<String, Object>> childSummaries = new ArrayList<>();
                for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                    if (childChain == null || childChain.isEmpty()) {
                        continue;
                    }
                    Map<String, Object> childSummary = new LinkedHashMap<>();
                    TaskProcessChainNodeVo firstNode = childChain.get(0);
                    TaskProcessChainNodeVo lastNode = childChain.get(childChain.size() - 1);
                    putIfNotBlank(childSummary, "chainId", firstNode.getChainId());
                    childSummary.put("nodeCount", childChain.size());
                    childSummary.put("firstNode", toAiProcessChainNode(firstNode));
                    childSummary.put("lastNode", toAiProcessChainNode(lastNode));
                    childSummaries.add(childSummary);
                    summaries.addAll(summarizeAiProcessChildChains(childChain));
                }
                putIfNotEmpty(parentSummary, "childChains", childSummaries);
                summaries.add(parentSummary);
            }
        }
        return summaries;
    }

    private static Map<String, Object> toAiProcessChainNode(TaskProcessChainNodeVo node) {
        Map<String, Object> item = new LinkedHashMap<>();
        putIfNotNull(item, "nodeId", node.getNodeId());
        putIfNotBlank(item, "chainId", node.getChainId());
        putIfNotNull(item, "parentNodeId", node.getParentNodeId());
        putIfNotBlank(item, "linkName", node.getLinkName());
        putIfNotBlank(item, "triggerReason", node.getTriggerReason());
        putIfNotBlank(item, "operatorName", node.getOperatorName());
        putIfNotBlank(item, "operatorRole", node.getOperatorRole());
        putIfNotBlank(item, "triggerTime", formatAiProcessChainTime(node.getTriggerTime()));
        putIfNotNull(item, "bizType", node.getBizType());
        putIfNotBlank(item, "bizTypeLabel", taskProcessBizTypeLabel(node.getBizType()));
        putIfNotNull(item, "bizId", node.getBizId());
        putIfNotNull(item, "taskId", node.getTaskId());
        putIfNotBlank(item, "taskType", node.getTaskType());
        putIfNotNull(item, "planType", node.getPlanType());
        putIfNotNull(item, "sourceType", node.getSourceType());
        putIfNotBlank(item, "sourceTypeLabel", taskProcessSourceTypeLabel(node.getSourceType()));
        putIfNotNull(item, "nodeCategory", node.getNodeCategory());
        putIfNotBlank(item, "nodeCategoryLabel", TaskProcessNodeCategoryEnum.label(node.getNodeCategory()));
        putIfNotNull(item, "stageType", node.getStageType());
        putIfNotBlank(item, "stageTypeLabel", TaskProcessStageTypeEnum.label(node.getStageType()));
        return item;
    }

    private static String taskProcessBizTypeLabel(Integer code) {
        return code == null ? null : TaskProcessBizTypeEnum.label(code);
    }

    private static String taskProcessSourceTypeLabel(Integer code) {
        return code == null ? null : TaskProcessSourceTypeEnum.label(code);
    }

    private static String formatAiProcessChainTime(Date time) {
        return time == null ? null : DateUtil.formatDateTime(time);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private static void putIfNotEmpty(Map<String, Object> target, String key, Collection<?> value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    private static int stringLength(String value) {
        return value == null ? 0 : value.length();
    }

    private DzRiskAssessmentVo resolveTaskRiskInfo(DzTaskDistListVo task) {
        if (task == null || task.getRiskId() == null) {
            return null;
        }
        return dzRiskAssessmentMapper.selectVoById(task.getRiskId());
    }

    private TaskAiProcessChainVo parseAiProcessChainAnswer(String answer) {
        TaskAiProcessChainVo result = new TaskAiProcessChainVo();
        String json = extractJsonObject(answer);
        if (StringUtils.isBlank(json)) {
            result.setTaskUnderstanding(answer);
            return result;
        }
        try {
            JSONObject root = JSONUtil.parseObj(json);
            JSONObject data = resolveAiProcessChainData(root);
            result.setTaskUnderstanding(firstText(data, "taskUnderstanding", "task_understanding", "任务理解"));
            result.setCurrentJudgement(firstText(data, "currentJudgement", "current_judgement", "currentJudgment", "current_judgment", "当前判断"));
            if (StringUtils.isBlank(result.getTaskUnderstanding())
                && StringUtils.isBlank(result.getCurrentJudgement())) {
                result.setTaskUnderstanding(answer);
            }
            return result;
        } catch (Exception e) {
            log.warn("解析Dify任务流程链路AI分析结果失败，按文本返回，answer={}", answer, e);
            result.setTaskUnderstanding(answer);
            return result;
        }
    }

    private String extractJsonObject(String answer) {
        if (StringUtils.isBlank(answer)) {
            return null;
        }
        int start = answer.indexOf('{');
        int end = answer.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return answer.substring(start, end + 1);
    }

    private JSONObject resolveAiProcessChainData(JSONObject root) {
        for (String key : List.of("analysis", "data", "result", "outputs")) {
            Object value = root.get(key);
            if (value instanceof JSONObject object) {
                return object;
            }
            if (value instanceof Map<?, ?> || JSONUtil.isTypeJSON(String.valueOf(value))) {
                try {
                    return JSONUtil.parseObj(value);
                } catch (Exception ignored) {
                }
            }
        }
        return root;
    }

    private String firstText(JSONObject data, String... keys) {
        Object value = firstValue(data, keys);
        return value == null ? null : String.valueOf(value);
    }

    private Object firstValue(JSONObject data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    void recordTaskProcessTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task) {
        recordTaskProcessTaskNode(chainId, linkName, triggerReason, task, null, null);
    }

    void recordTaskProcessTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task,
                                   Long parentNodeId, Integer nodeCategory) {
        taskProcessChainNodeService.recordTaskNode(chainId, linkName, triggerReason, task, parentNodeId, nodeCategory);
    }

    Long recordTaskProcessBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                                  Long taskId, Long operatorId, String operatorName, Integer sourceType) {
        return recordTaskProcessBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId, operatorName,
            sourceType, null, null, null);
    }

    Long recordTaskProcessBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                                  Long taskId, Long operatorId, String operatorName, Integer sourceType,
                                  Long parentNodeId, String operatorRole, Integer nodeCategory) {
        return recordTaskProcessBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId,
            operatorName, sourceType, parentNodeId, operatorRole, nodeCategory, null);
    }

    Long recordTaskProcessBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                                  Long taskId, Long operatorId, String operatorName, Integer sourceType,
                                  Long parentNodeId, String operatorRole, Integer nodeCategory, String taskType) {
        return taskProcessChainNodeService.recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId,
            operatorName, sourceType, parentNodeId, operatorRole, nodeCategory, null, taskType);
    }

    String buildTaskProcessChainId(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        if (task.getId() != null) {
            String chainId = taskProcessChainNodeService.resolveSavedChainIdByTaskId(task.getId());
            if (StringUtils.isNotBlank(chainId)) {
                return chainId;
            }
        }
        if (task.getRelatedTaskId() != null) {
            String chainId = taskProcessChainNodeService.resolveSavedChainIdByTaskId(task.getRelatedTaskId());
            if (StringUtils.isNotBlank(chainId)) {
                return chainId;
            }
        }
        return null;
    }

    String resolveHandleProcessChainId(Long handleId) {
        if (handleId == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), handleId);
        return chainId;
    }

    String buildDefRespProcessChainId(DefRespPlan plan) {
        if (plan == null) {
            return null;
        }
        if (plan.getSourceAlarmId() != null) {
            DzTaskProcessChainNode startNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                TaskProcessBizTypeEnum.DEF_RESP.getCode(),
                plan.getId(),
                TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
                TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
            return startNode == null ? null : startNode.getChainId();
        }
        if (plan.getHandleId() != null) {
            return resolveHandleProcessChainId(plan.getHandleId());
        }
        return plan.getId() == null ? null : taskProcessChainNodeService.resolveSavedChainIdByBiz(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), plan.getId());
    }

    private DefRespBatchMainPlan resolveDefRespBatchMainPlan(Long defId) {
        DefRespPlan plan = defId == null ? null : dzDefRespPlanMapper.selectById(defId);
        if (!isTownRegionDefRespPlan(plan)) {
            return new DefRespBatchMainPlan(defId, plan, plan == null ? null : plan.getParentDefId());
        }
        Long parentDefId = plan.getParentDefId();
        DefRespPlan parentPlan = parentDefId == null ? null : dzDefRespPlanMapper.selectById(parentDefId);
        if (isCountyRegionDefRespPlan(parentPlan)) {
            Long countyDefId = parentPlan.getId() == null ? parentDefId : parentPlan.getId();
            return new DefRespBatchMainPlan(countyDefId, parentPlan, parentDefId);
        }
        return new DefRespBatchMainPlan(defId, plan, parentDefId);
    }

    private record DefRespBatchMainPlan(Long defId, DefRespPlan plan, Long parentDefId) {
    }

    void recordTaskPushProcessNodes(Collection<DzTaskDistList> tasks, boolean manualPush, Date triggerTime, String sceneLabel) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        tasks.forEach(task -> recordTaskPushProcessNode(task, manualPush, triggerTime, sceneLabel));
    }

    void recordTaskPushProcessNode(DzTaskDistList task, boolean manualPush, Date triggerTime, String sceneLabel) {
        if (task == null || task.getId() == null) {
            return;
        }
        LoginUser loginUser = manualPush ? getLoginUserSafely() : null;
        Long operatorId = manualPush ? resolveManualPushOperatorId(task, loginUser) : AUTO_AGENT_USER_ID;
        String operatorName = manualPush ? resolveManualPushOperatorName(loginUser, operatorId) : AUTO_AGENT_NAME;
        String chainId = buildTaskPushProcessChainId(task);
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("任务推送未找到已有流程链路, taskId=" + task.getId());
        }
        recordTaskProcessBizNode(
                chainId,
                TASK_PUSH_LINK_NAME,
                buildTaskPushTriggerReason(manualPush, sceneLabel),
                TaskProcessBizTypeEnum.TASK.getCode(),
                task.getId(),
                task.getId(),
                operatorId,
                operatorName,
                TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(),
                null,
                null,
                null,
                task.getTaskType()
            );
        maybeRecordEmergencyBatchPushProcessNode(task, triggerTime);
    }

    private void maybeRecordEmergencyBatchPushProcessNode(DzTaskDistList task, Date triggerTime) {
        if (task == null || task.getHandleId() == null
            || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return;
        }
        List<DzTaskDistList> batchTasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .eq(DzTaskDistList::getHandleId, task.getHandleId())
                .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)
                .eq(DzTaskDistList::getDelete, 0)
        );
        if (ObjUtil.isEmpty(batchTasks) || !allEmergencyBatchTasksHavePushNode(batchTasks)) {
            return;
        }
        DzTaskProcessChainNode batchNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            task.getHandleId(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        );
        if (batchNode == null || StringUtils.isBlank(batchNode.getChainId())) {
            return;
        }
        recordTaskProcessBizNode(
            batchNode.getChainId(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            task.getHandleId(),
            null,
            AUTO_AGENT_USER_ID,
            AUTO_AGENT_NAME,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(),
            batchNode.getId(),
            "AI智能体",
            TaskProcessNodeCategoryEnum.TASK_PUSH.getCode()
        );
    }

    private boolean allEmergencyBatchTasksHavePushNode(Collection<DzTaskDistList> tasks) {
        for (DzTaskDistList task : tasks) {
            if (task == null || task.getId() == null || !hasTaskPushProcessNode(task.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTaskPushProcessNode(Long taskId) {
        try {
            List<TaskProcessChainNodeVo> chain = taskProcessChainNodeService.queryChainByTaskId(taskId);
            return containsTaskPushProcessNode(chain);
        } catch (ServiceException e) {
            return false;
        }
    }

    private boolean containsTaskPushProcessNode(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (TaskProcessChainNodeTextEnum.semanticEquals(
                TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), node.getLinkName())) {
                return true;
            }
            if (node.getChildChains() == null) {
                continue;
            }
            for (List<TaskProcessChainNodeVo> childChain : node.getChildChains()) {
                if (containsTaskPushProcessNode(childChain)) {
                    return true;
                }
            }
        }
        return false;
    }

    String buildTaskPushProcessChainId(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        String savedTaskChainId = task.getId() == null ? null
            : taskProcessChainNodeService.resolveSavedChainIdByTaskId(task.getId());
        if (StringUtils.isNotBlank(savedTaskChainId)) {
            return savedTaskChainId;
        }
        Integer sourceType = task.getSourceType();
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT) && task.getReportId() != null) {
            return buildReportDisasterTriggerReason(task);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return buildDefRespTaskPushChainId(task);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return buildEmergencyTaskPushChainId(task);
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)) {
            Long warningEventId = resolveMonitorWarningEventIdForTaskPush(task);
            return warningEventId == null ? buildTaskProcessChainId(task)
                : taskProcessChainNodeService.resolveSavedChainIdByBiz(
                    TaskProcessBizTypeEnum.MONITOR_WARNING.getCode(), warningEventId);
        }
        return buildTaskProcessChainId(task);
    }

    String buildReportDisasterTriggerReason(DzTaskDistList task) {
        if (task == null || task.getId() == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(2,task.getReportId());
        if (StringUtils.isNotBlank(chainId)) {
            return chainId;
        }
        return null;
    }

    private String buildDefRespTaskPushChainId(DzTaskDistList task) {
        if (task == null || task.getId() == null) {
            return null;
        }
        String chainId = findSavedTaskChainIdWithoutRetry(task);
        if (StringUtils.isNotBlank(chainId)) {
            return chainId;
        }
        chainId = resolveDefRespTaskMainChainId(task);
        if (StringUtils.isNotBlank(chainId)) {
            return chainId;
        }
        return null;
    }

    private String resolveDefRespTaskMainChainId(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        DefRespPlan plan = task.getDefId() == null ? null : dzDefRespPlanMapper.selectById(task.getDefId());
        if (plan != null && plan.getSourceAlarmId() != null) {
            String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(
                TaskProcessBizTypeEnum.ALARM.getCode(), plan.getSourceAlarmId());
            return chainId;
        }
        Long handleId = plan != null && plan.getHandleId() != null ? plan.getHandleId() : task.getHandleId();
        if (handleId != null) {
            return resolveHandleProcessChainId(handleId);
        }
        Long defId = plan == null ? task.getDefId() : plan.getId();
        if (defId == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.DEF_RESP.getCode(), defId);
        return chainId;
    }

    private String buildEmergencyTaskPushChainId(DzTaskDistList task) {
        if (task == null || task.getId() == null) {
            return null;
        }
        String chainId = findSavedTaskChainIdWithoutRetry(task);
        if (StringUtils.isNotBlank(chainId)) {
            return chainId;
        }
        if (task.getHandleId() != null) {
            return resolveHandleProcessChainId(task.getHandleId());
        }
        return null;
    }

    private Long resolveMonitorWarningEventIdForTaskPush(DzTaskDistList task) {
        if (task == null || task.getRiskId() == null || dzRiskAssessmentWarningRelationService == null) {
            return null;
        }
        try {
            DzRiskAssessmentWarningRelation relation = dzRiskAssessmentWarningRelationService.getByRiskAssessmentId(task.getRiskId());
            return relation == null ? null : relation.getWarningEventId();
        } catch (Exception e) {
            log.warn("任务推送链路解析监测预警事件失败，降级使用任务链路, taskId={}, riskId={}", task.getId(), task.getRiskId(), e);
            return null;
        }
    }

    private String buildTaskPushTriggerReason(boolean manualPush, String sceneLabel) {
        if (manualPush) {
            return TASK_PUSH_REASON_MANUAL;
        }
        if (StringUtils.isNotBlank(sceneLabel)) {
            return sceneLabel + "自动推送任务到 APP";
        }
        return TASK_PUSH_REASON_SYSTEM;
    }

    private LoginUser getLoginUserSafely() {
        try {
            return LoginHelper.getLoginUser();
        } catch (Exception e) {
            log.debug("获取当前登录用户失败，任务推送链路操作人将降级为空", e);
            return null;
        }
    }

    private Long resolveManualPushOperatorId(DzTaskDistList task, LoginUser loginUser) {
        if (loginUser != null && loginUser.getUserId() != null) {
            return loginUser.getUserId();
        }
        return task == null ? null : task.getAppPushUserId();
    }

    private String resolveManualPushOperatorName(LoginUser loginUser, Long operatorId) {
        if (loginUser != null) {
            String name = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        if (Objects.equals(operatorId, AUTO_AGENT_USER_ID)) {
            return AUTO_AGENT_NAME;
        }
        return null;
    }

    void recordDailyPatrolProcessNode(DzTaskDistList task) {
        if (task == null || task.getId() == null || !DzTaskDistList.isPatrolTask(task.getPlanName())
            || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)) {
            return;
        }
        recordTaskProcessTaskNode(resolveTaskChainIdOrGenerate(task),
            TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getLinkName(),
            TaskProcessChainNodeTextEnum.DAILY_PATROL_TASK.getTriggerReason(), task);
    }

    void recordManualTaskCreateProcessNode(DzTaskDistList task) {
        if (task == null || task.getId() == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL)) {
            return;
        }
        recordTaskProcessTaskNode(resolveTaskChainIdOrGenerate(task),
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getLinkName(),
            TaskProcessChainNodeTextEnum.MANUAL_TASK_CREATE.getTriggerReason(), task);
    }

    void recordDefRespTaskProcessNode(DzTaskDistList task) {
        recordDefRespTaskProcessNode(task, null);
    }

    void recordDefRespTaskProcessNode(DzTaskDistList task, Long parentNodeId) {
        if (task == null || task.getId() == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return;
        }
        String chainId = parentNodeId == null
            ? buildDefRespTaskPushChainId(task)
            : resolveTaskChainIdOrGenerate(task);
        recordTaskProcessTaskNode(chainId, TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getTriggerReason(),
            task, parentNodeId, null);
    }

    void recordEmergencyTaskProcessNode(DzTaskDistList task) {
        recordEmergencyTaskProcessNode(task, null);
    }

    void recordEmergencyTaskProcessNode(DzTaskDistList task, Long parentNodeId) {
        if (task == null || task.getId() == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return;
        }
        String chainId = parentNodeId == null
            ? buildEmergencyTaskPushChainId(task)
            : resolveTaskChainIdOrGenerate(task);
        recordTaskProcessTaskNode(chainId, TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getTriggerReason(),
            task, parentNodeId, null);
    }

    private String resolveTaskChainIdOrGenerate(DzTaskDistList task) {
        String chainId = findSavedTaskChainIdWithoutRetry(task);
        return StringUtils.isNotBlank(chainId) ? chainId : taskProcessChainNodeService.generateChainId();
    }

    private String findSavedTaskChainIdWithoutRetry(DzTaskDistList task) {
        Long taskId = task == null ? null : task.getId();
        return findSavedTaskChainIdWithoutRetry(taskProcessChainNodeService, taskId);
    }

    private static String findSavedTaskChainIdWithoutRetry(IDzTaskProcessChainNodeService chainNodeService, Long taskId) {
        if (taskId == null) {
            return null;
        }
        TaskProcessChainNodeVo latestNode = chainNodeService.queryLatestNodeByTaskIds(List.of(taskId)).get(taskId);
        return latestNode == null ? null : latestNode.getChainId();
    }

    void recordEmergencyBatchProcessNodes(Long handleId, Collection<DzTaskDistList> tasks, Date triggerTime) {
        if (handleId == null || ObjUtil.isEmpty(tasks)) {
            return;
        }
        String chainId = resolveHandleProcessChainId(handleId);
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("生成处置任务未找到已有主链, handleId=" + handleId);
        }
        Long parentNodeId = recordTaskProcessBizNode(
            chainId,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            null,
            AUTO_AGENT_USER_ID,
            AUTO_AGENT_NAME,
            TaskProcessSourceTypeEnum.EMERGENCY.getCode(),
            null,
            "AI智能体",
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        );
        tasks.forEach(task -> recordEmergencyTaskProcessNode(task, parentNodeId));
    }

    void recordDefRespBatchProcessNodes(Long defId, Collection<DzTaskDistList> tasks, Date triggerTime) {
        if (defId == null || ObjUtil.isEmpty(tasks)) {
            return;
        }
        DefRespBatchMainPlan mainPlan = resolveDefRespBatchMainPlan(defId);
        String chainId = buildDefRespProcessChainId(mainPlan.plan());
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("生成防御响应任务未找到已有主链, defId=" + defId
                + ", mainDefId=" + mainPlan.defId()
                + ", parentDefId=" + mainPlan.parentDefId());
        }
        Long parentNodeId = recordTaskProcessBizNode(
            chainId,
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            mainPlan.defId(),
            null,
            AUTO_AGENT_USER_ID,
            AUTO_AGENT_NAME,
            TaskProcessSourceTypeEnum.DEF_RESP.getCode(),
            null,
            "AI智能体",
            TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode()
        );
        tasks.forEach(task -> recordDefRespTaskProcessNode(task, parentNodeId));
    }

    void recordSystemTaskStatusProcessNode(DzTaskDistList task, String linkName, String triggerReason, Date triggerTime,
                                           Integer nodeCategory) {
        if (task == null || task.getId() == null || StringUtils.isBlank(linkName)) {
            return;
        }
        String chainId = buildTaskPushProcessChainId(task);
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("任务状态节点未找到已有流程链路, taskId=" + task.getId());
        }
        recordTaskProcessBizNode(
            chainId,
            linkName,
            triggerReason,
            TaskProcessBizTypeEnum.TASK.getCode(),
            task.getId(),
            task.getId(),
            AUTO_AGENT_USER_ID,
            AUTO_AGENT_NAME,
            TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode(),
            null,
            "AI智能体",
            nodeCategory,
            task.getTaskType()
        );
    }

    private DzTaskDistMiscDelegate miscDelegate;

    private DzTaskDistMiscDelegate miscDelegate() {
        if (miscDelegate == null) {
            miscDelegate = new DzTaskDistMiscDelegate(this);
        }
        return miscDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DzTaskDistListBo bo) {
        return miscDelegate().insertByBo(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DzTaskDistListBo bo) {
        return miscDelegate().updateByBo(bo);
    }

    void validateTaskStatusTransition(Integer currentStatus, Long taskId) {
        miscDelegate().validateTaskStatusTransition(currentStatus, taskId);
    }

    List<DzTaskDistList> filterOpenDuplicateTasks(List<DzTaskDistList> tasks, Date duplicateCheckDate) {
        return miscDelegate().filterOpenDuplicateTasks(tasks, duplicateCheckDate);
    }

    /**
     * 分页查询任务派发清单列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务派发清单分页列表
     */
    private DzQueryPermissionDelegate queryPermissionDelegate;

    private DzQueryPermissionDelegate queryPermissionDelegate() {
        if (queryPermissionDelegate == null) {
            queryPermissionDelegate = new DzQueryPermissionDelegate(this);
        }
        return queryPermissionDelegate;
    }

    @Override
    public TableDataInfo<DzTaskDistListVo> queryPageList(DzTaskDistListBo bo, PageQuery pageQuery) {
        return queryPermissionDelegate().queryPageList(bo, pageQuery);
    }

    @Override
    public TableDataInfo<DzTaskDistListVo> queryPageListWithLatestProcessNode(DzTaskDistListBo bo, PageQuery pageQuery) {
        Set<Long> latestDisplayTaskIds = taskProcessChainNodeService.queryLatestDisplayTaskIds();
        TableDataInfo<DzTaskDistListVo> tableData = queryPermissionDelegate()
            .queryPageListInTaskIds(bo, pageQuery, latestDisplayTaskIds);
        fillLatestProcessNode(tableData.getData());
        return tableData;
    }

    private void fillLatestProcessNode(List<DzTaskDistListVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> taskIds = records.stream()
            .map(DzTaskDistListVo::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (taskIds.isEmpty()) {
            return;
        }
        Map<Long, TaskProcessChainNodeVo> latestNodeMap = taskProcessChainNodeService.queryLatestNodeByTaskIds(taskIds);
        records.forEach(record -> record.setLatestProcessNode(latestNodeMap.get(record.getId())));
    }

    List<DzUserAdRegionVo> getCurrentUserAdRegionsForListQuery() {
        return queryPermissionDelegate().getCurrentUserAdRegionsForListQuery();
    }

    private void fillTaskBaseInfo(List<DzTaskDistListVo> records) {
        queryPermissionDelegate().fillTaskBaseInfo(records);
    }

    private static void appendTaskListOrderByCondition(QueryWrapper<DzTaskDistList> ew, DzTaskDistListBo bo) {
        DzQueryPermissionDelegate.appendTaskListOrderByCondition(ew, bo);
    }

    static LambdaQueryWrapper<DzTaskDistList> buildQueryWrapper(DzTaskDistListBo bo) {
        return DzQueryPermissionDelegate.buildQueryWrapper(bo);
    }

    boolean appendCurrentUserAdRegionPermission(LambdaQueryWrapper<DzTaskDistList> lqw) {
        return queryPermissionDelegate().appendCurrentUserAdRegionPermission(lqw);
    }

    boolean appendSlopeUnitAdRegionPermission(AbstractWrapper<?, ?, ?> lqw, List<DzUserAdRegionVo> userAdRegions) {
        return queryPermissionDelegate().appendSlopeUnitAdRegionPermission(lqw, userAdRegions);
    }

    private List<DzUserAdRegionVo> getCurrentUserAdRegions() {
        return queryPermissionDelegate().getCurrentUserAdRegions();
    }

    static void ensureListAdRegionPermission(List<DzUserAdRegionVo> userAdRegions) {
        DzQueryPermissionDelegate.ensureListAdRegionPermission(userAdRegions);
    }

    static ServiceException regionPermissionDeniedException(String actionLabel) {
        return DzQueryPermissionDelegate.regionPermissionDeniedException(actionLabel);
    }

    private void validateListQueryAccess(DzTaskDistListBo bo) {
        queryPermissionDelegate().validateListQueryAccess(bo);
    }

    private void validateTaskViewAccess(String unitId, Integer sourceType, Integer planType) {
        queryPermissionDelegate().validateTaskViewAccess(unitId, sourceType, planType);
    }

    void validateCurrentUserTaskAccess(String unitId, String actionLabel) {
        queryPermissionDelegate().validateCurrentUserTaskAccess(unitId, actionLabel);
    }

    void validateDutyOfficerTaskAccess(String unitId) {
        queryPermissionDelegate().validateDutyOfficerTaskAccess(unitId);
    }

    public static String resolveSlopeUnitAdRegionCodeValue(DzUserAdRegionVo userAdRegion, Integer adRegionLevel) {
        return DzQueryPermissionDelegate.resolveSlopeUnitAdRegionCodeValue(userAdRegion, adRegionLevel);
    }

    private String buildSlopeUnitRegionPermissionSql(List<DzUserAdRegionVo> userAdRegions, String alias) {
        return queryPermissionDelegate().buildSlopeUnitRegionPermissionSql(userAdRegions, alias);
    }

    private String buildSlopeUnitRegionCondition(DzUserAdRegionVo userAdRegion, String alias) {
        return queryPermissionDelegate().buildSlopeUnitRegionCondition(userAdRegion, alias);
    }

    @Override
    public List<DzTaskDistListVo> queryList(DzTaskDistListBo bo) {
        return queryPermissionDelegate().queryList(bo);
    }

    public Map<String, Integer> batchInsertPatrol() {
        return miscDelegate().batchInsertPatrol();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchInsertDefRespPatrol() {
        return miscDelegate().batchInsertDefRespPatrol();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createDailyPatrolTasksByRiskIds(List<Long> riskIds) {
        return miscDelegate().createDailyPatrolTasksByRiskIds(riskIds);
    }

    @Override
    public Integer upsertMonitorWarningTaskByRiskId(Long riskId, boolean autoPushTask, boolean sendTaskSms) {
        return miscDelegate().upsertMonitorWarningTaskByRiskId(riskId, autoPushTask, sendTaskSms);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<DzTaskDistList> tasks = loadTasksForDelete(ids, Boolean.TRUE.equals(isValid));
        softDeleteTasks(tasks, buildManualDeleteCloseReason(), new Date());
        return true;
    }

    private List<DzTaskDistList> loadTasksForDelete(Collection<Long> ids, boolean validateAllExists) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("任务ID不能为空");
        }
        LinkedHashSet<Long> taskIds = ids.stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (taskIds.isEmpty()) {
            throw new ServiceException("任务ID不能为空");
        }
        List<DzTaskDistList> tasks = baseMapper.selectBatchIds(taskIds).stream()
            .filter(Objects::nonNull)
            .filter(task -> Objects.equals(task.getDelete(), 0))
            .toList();
        if (validateAllExists && tasks.size() != taskIds.size()) {
            throw new ServiceException("存在任务不存在或已删除，无法删除");
        }
        for (DzTaskDistList task : tasks) {
            validateDutyOfficerTaskAccess(task.getUnitId());
        }
        return tasks;
    }

    private void softDeleteTasks(List<DzTaskDistList> tasks, String closeReason, Date now) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        syncDeleteClosedStatusToApp(tasks);
        List<DzTaskDistList> updates = tasks.stream()
            .filter(Objects::nonNull)
            .map(task -> {
                DzTaskDistList update = new DzTaskDistList();
                update.setId(task.getId());
                update.setStatus(DzTaskDistList.STATUS_CLOSED);
                update.setDelete(1);
                update.setClosedTime(now);
                update.setCloseReason(closeReason);
                update.setUpdateDate(now);
                return update;
            })
            .toList();
        if (!baseMapper.updateBatchById(updates)) {
            throw new ServiceException("删除任务失败");
        }
    }

    private void syncDeleteClosedStatusToApp(List<DzTaskDistList> tasks) {
        List<InspectionTaskReq> updateReqs = tasks.stream()
            .filter(this::shouldSyncAppClosedStatusOnDelete)
            .map(task -> InspectionTaskReq.builder()
                .taskId(task.getId())
                .status(DzTaskDistList.STATUS_CLOSED)
                .sourceType(task.getSourceType())
                .taskType(task.getTaskType())
                .handleId(task.getHandleId())
                .build())
            .toList();
        if (updateReqs.isEmpty()) {
            return;
        }
        appTaskService.updateTask(updateReqs);
    }

    private boolean shouldSyncAppClosedStatusOnDelete(DzTaskDistList task) {
        return task != null
            && task.getId() != null
            && !Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED)
            && DzTaskDistList.isOpenStatus(task.getStatus());
    }

    private String buildManualDeleteCloseReason() {
        String userName = "当前用户";
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null) {
                userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
            }
        } catch (Exception ignored) {
        }
        String roleName = null;
        try {
            roleName = CurrentRoleUtil.requireCurrentRoles().stream()
                .map(SysRole::getRoleName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
        } catch (Exception ignored) {
        }
        String operator = StringUtils.defaultIfBlank(roleName, "") + StringUtils.defaultIfBlank(userName, "当前用户");
        return operator + "删除任务，自动关闭";
    }

    @Override
    public List<TaskDistStatStatusVo> statStatus(TaskDistStatStatusBo bo) {
        return miscDelegate().statStatus(bo);
    }

    @Override
    public List<TaskDistStatSourceTypeVo> statSourceType(TaskDistStatStatusBo bo) {
        return miscDelegate().statSourceType(bo);
    }

    private DzPushSmsDelegate pushSmsDelegate;

    private DzPushSmsDelegate pushSmsDelegate() {
        if (pushSmsDelegate == null) {
            pushSmsDelegate = new DzPushSmsDelegate(this);
        }
        return pushSmsDelegate;
    }

    @Override
    public TaskDistPushVo push(TaskDistPushBo bo) {
        return pushSmsDelegate().push(bo);
    }

    static void markManualAppPushAudit(DzTaskDistList task, Long userId, Date pushTime) {
        markAppPushAudit(task, DzTaskDistList.APP_PUSH_TYPE_MANUAL, userId, pushTime);
    }

    static void markSystemAppPushAudit(DzTaskDistList task, Date pushTime) {
        markAppPushAudit(task, DzTaskDistList.APP_PUSH_TYPE_SYSTEM, AUTO_AGENT_USER_ID, pushTime);
    }

    static void markManualTaskCreateAudit(DzTaskDistList task, Long userId) {
        markTaskCreateAudit(task, DzTaskDistList.TASK_CREATE_TYPE_MANUAL, userId);
    }

    static void markSystemTaskCreateAudit(DzTaskDistList task) {
        markTaskCreateAudit(task, DzTaskDistList.TASK_CREATE_TYPE_SYSTEM, AUTO_AGENT_USER_ID);
    }

    public static void prepareNewTaskDefaults(DzTaskDistList task, Date now, boolean systemCreate) {
        if (task == null) {
            return;
        }
        Date effectiveNow = now == null ? new Date() : now;
        if (task.getStatus() == null) {
            task.setStatus(DzTaskDistList.STATUS_UNPUSHED);
        }
        if (task.getDelete() == null) {
            task.setDelete(0);
        }
        if (task.getOverdue() == null) {
            task.setOverdue(DzTaskDistList.OVERDUE_NO);
        }
        if (task.getQuotaConsumed() == null) {
            task.setQuotaConsumed(DzTaskDistList.QUOTA_CONSUMED_NO);
        }
        if (task.getReminderCount() == null) {
            task.setReminderCount(0);
        }
        if (task.getCreateDate() == null) {
            task.setCreateDate(effectiveNow);
        }
        task.setUpdateDate(effectiveNow);
        if (systemCreate) {
            markSystemTaskCreateAudit(task);
        } else {
            markManualTaskCreateAudit(task, LoginHelper.getUserId());
        }
    }

    static String formatAppDispatchTime(Date createDate) {
        return DateUtil.formatDateTime(Objects.requireNonNullElseGet(createDate, Date::new));
    }

    public static String resolveStoredTaskType(Integer sourceType) {
        return resolveStoredTaskType(sourceType, null);
    }

    public static String resolveStoredTaskType(Integer sourceType, DzReportDisaster report) {
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT)) {
            if (report != null) {
                if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY)) {
                    return DzTaskDistList.TASK_TYPE_PUBLIC_REPORT;
                }
                if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)) {
                    return DzTaskDistList.TASK_TYPE_AI_VERIFY;
                }
                if (report.getTaskId() != null
                    && (Objects.equals(report.getSourceType(), DzReportDisaster.SOURCE_TYPE_TASK_FEEDBACK)
                    || Objects.equals(report.getSourceType(), DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT))) {
                    return DzTaskDistList.TASK_TYPE_AI_VERIFY;
                }
                if (Objects.equals(report.getSourceType(), DzReportDisaster.SOURCE_TYPE_PUBLIC_REPORT)
                    && report.getTaskId() == null) {
                    return DzTaskDistList.TASK_TYPE_PUBLIC_REPORT;
                }
            }
            return DzTaskDistList.TASK_TYPE_AI_VERIFY;
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return DzTaskDistList.TASK_TYPE_DEF_RESP;
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return DzTaskDistList.TASK_TYPE_EMERGENCY;
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)) {
            return DzTaskDistList.TASK_TYPE_MONITOR_WARNING;
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION;
        }
        return DzTaskDistList.TASK_TYPE_INSPECTION;
    }

    String resolveStoredTaskType(DzTaskDistList task) {
        if (task == null) {
            return DzTaskDistList.TASK_TYPE_INSPECTION;
        }
        DzReportDisaster report = loadTaskTypeReport(task);
        return resolveStoredTaskType(task.getSourceType(), report);
    }

    void populateStoredTaskType(DzTaskDistList task) {
        if (task == null) {
            return;
        }
        task.setTaskType(resolveStoredTaskType(task));
    }

    void populateStoredTaskType(DzTaskDistList task, DzReportDisaster report) {
        if (task == null) {
            return;
        }
        task.setTaskType(resolveStoredTaskType(task.getSourceType(), report));
    }

    private DzReportDisaster loadTaskTypeReport(DzTaskDistList task) {
        if (task == null || task.getReportId() == null) {
            return null;
        }
        return dzReportDisasterMapper.selectById(task.getReportId());
    }

    private static void markAppPushAudit(DzTaskDistList task, Integer pushType, Long userId, Date pushTime) {
        if (task == null) {
            return;
        }
        task.setAppPushType(pushType);
        task.setAppPushUserId(userId);
        task.setAppPushTime(pushTime);
    }

    private static void markTaskCreateAudit(DzTaskDistList task, Integer createType, Long userId) {
        if (task == null) {
            return;
        }
        task.setTaskCreateType(createType);
        task.setTaskCreateUserId(userId);
    }

    @Override
    public List<TaskDistSmsPreviewItemVo> previewSms(TaskDistPushBo bo) {
        return pushSmsDelegate().previewSms(bo);
    }

    @Override
    public Object pushCompatible(TaskDistChainScopePushBo bo) {
        return taskChainPushScopeDelegate().pushCompatible(bo);
    }

    @Override
    public List<TaskDistSmsPreviewItemVo> previewSmsCompatible(TaskDistChainScopePushBo bo) {
        return taskChainPushScopeDelegate().previewSmsCompatible(bo);
    }

    private DzTaskChainPushScopeDelegate taskChainPushScopeDelegate;

    private DzTaskChainPushScopeDelegate taskChainPushScopeDelegate() {
        if (taskChainPushScopeDelegate == null) {
            taskChainPushScopeDelegate = new DzTaskChainPushScopeDelegate(this);
        }
        return taskChainPushScopeDelegate;
    }

    @Override
    public TaskDistChainPushResultVo pushByChainScope(TaskDistChainScopePushBo bo) {
        return taskChainPushScopeDelegate().pushByChainScope(bo);
    }

    @Override
    public TaskDistChainPreviewResultVo previewSmsByChainScope(TaskDistChainScopePushBo bo) {
        return taskChainPushScopeDelegate().previewSmsByChainScope(bo);
    }

    void validateTaskPushAccess() {
        pushSmsDelegate().validateTaskPushAccess();
    }

    void normalizeRoleBasedListQuery(DzTaskDistListBo bo) {
        pushSmsDelegate().normalizeRoleBasedListQuery(bo);
    }

    void validateTaskViewRoleAccess(Integer sourceType, Integer planType) {
        pushSmsDelegate().validateTaskViewRoleAccess(sourceType, planType);
    }

    private void validateRoleBasedTaskAccess(Integer sourceType, Integer planType, boolean forPush) {
        pushSmsDelegate().validateRoleBasedTaskAccess(sourceType, planType, forPush);
    }

    static void appendRoleBasedTaskListFilters(QueryWrapper<?> ew, String tableAlias) {
        DzPushSmsDelegate.appendRoleBasedTaskListFilters(ew, tableAlias);
    }

    private void appendBatchPushPermission(LambdaQueryWrapper<DzTaskDistList> lqw, TaskDistPushBo bo) {
        pushSmsDelegate().appendBatchPushPermission(lqw, bo);
    }

    void validatePushTaskAccess(DzTaskDistList task) {
        pushSmsDelegate().validatePushTaskAccess(task);
    }

    private static boolean isDutyOfficer() {
        return DzPushSmsDelegate.isDutyOfficer();
    }

    private void appendDirectorDefaultPlanTypeFilter(LambdaQueryWrapper<DzTaskDistList> lqw) {
        pushSmsDelegate().appendDirectorDefaultPlanTypeFilter(lqw);
    }

    int pushTasksInParallel(List<InspectionTaskReq> tasks) {
        return pushSmsDelegate().pushTasksInParallel(tasks);
    }

    public static LambdaQueryWrapper<DzTaskDistList> buildPushQueryWrapper(TaskDistPushBo bo) {
        return DzPushSmsDelegate.buildPushQueryWrapper(bo);
    }

    public static TaskDistPushBo normalizePushQueryBo(TaskDistPushBo bo) {
        return DzPushSmsDelegate.normalizePushQueryBo(bo);
    }

    void rollbackPushStatus(List<DzTaskDistList> rollbackTasks, Exception cause) {
        pushSmsDelegate().rollbackPushStatus(rollbackTasks, cause);
    }

    SmsSendSummaryVo sendDefRespSmsDirect(Long defId, Date referenceDate) {
        return pushSmsDelegate().sendDefRespSmsDirect(defId, referenceDate);
    }

    @Override
    public List<DefRespStartSmsPreviewVo> previewDefRespLifecycleSms(DefRespSmsEventSnapshot event) {
        return pushSmsDelegate().previewDefRespLifecycleSms(event);
    }

    @Override
    public SmsSendSummaryVo sendDefRespLifecycleSms(DefRespSmsEventSnapshot event, boolean manual) {
        return pushSmsDelegate().sendDefRespLifecycleSms(event, manual);
    }

    private List<Long> extractTaskIds(List<DzTaskDistList> tasks) {
        return pushSmsDelegate().extractTaskIds(tasks);
    }

    @Override
    public List<DefRespStartSmsPreviewVo> previewDefRespStartSms(Long defId) {
        return pushSmsDelegate().previewDefRespStartSms(defId);
    }

    private String firstNonBlank(String... values) {
        return pushSmsDelegate().firstNonBlank(values);
    }

    private void populateMissingSmsContents(List<DzTaskDistList> tasks) {
        pushSmsDelegate().populateMissingSmsContents(tasks);
    }

    private void overwriteTaskSmsSnapshot(List<DzTaskDistList> tasks, String content) {
        pushSmsDelegate().overwriteTaskSmsSnapshot(tasks, content);
    }

    String resolveTaskSubType(DzTaskDistList task) {
        return pushSmsDelegate().resolveTaskSubType(task);
    }

    String resolveTaskType(DzTaskDistList task) {
        return pushSmsDelegate().resolveTaskType(task);
    }

    String buildPushDetailedAddress(SlopeUnit slopeUnit, String originalDetailedAddress) {
        return pushSmsDelegate().buildPushDetailedAddress(slopeUnit, originalDetailedAddress);
    }

    private String buildSlopeUnitDisplayName(SlopeUnit slopeUnit) {
        return pushSmsDelegate().buildSlopeUnitDisplayName(slopeUnit);
    }

    void populateTaskDetailedAddress(DzTaskDistList task) {
        pushSmsDelegate().populateTaskDetailedAddress(task);
    }

    SlopeUnit resolveSlopeUnitByTaskUnitId(String unitId) {
        return pushSmsDelegate().resolveSlopeUnitByTaskUnitId(unitId);
    }


    Long findLatestRiskIdBySlopeUnitId(String slopeUnitId) {
        DzRiskAssessment riskAssessment = findLatestRiskAssessmentBySlopeUnitId(slopeUnitId);
        return riskAssessment != null ? riskAssessment.getId() : null;
    }

    DzRiskAssessment findLatestRiskAssessmentForTaskSource(String slopeUnitId) {
        return findLatestRiskAssessmentBySlopeUnitId(slopeUnitId);
    }

    private DzRiskAssessment findLatestRiskAssessmentBySlopeUnitId(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return null;
        }
        String normalizedSlopeUnitId = normalizeSlopeUnitId(slopeUnitId);
        List<String> slopeUnitIds = Objects.equals(slopeUnitId, normalizedSlopeUnitId)
            ? List.of(slopeUnitId)
            : List.of(slopeUnitId, normalizedSlopeUnitId);
        return dzRiskAssessmentMapper.selectList(
                Wrappers.<DzRiskAssessment>lambdaQuery()
                    .in(DzRiskAssessment::getSlopeUnitId, slopeUnitIds)
                    .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId)
                    .select(DzRiskAssessment::getId,
                        DzRiskAssessment::getSlopeUnitId,
                        DzRiskAssessment::getCreateDate,
                        DzRiskAssessment::getDynamicRiskLevel)
                    .last("limit 1")
            )
            .stream()
            .findFirst()
            .orElse(null);
    }

    /**
     * 校验InspectionTaskReq必填字段是否为空，若存在空字段则抛出ServiceException。
     *
     * @param req    待校验的巡查任务请求对象
     * @param taskId 任务ID，用于异常信息
     */
    static void validateInspectionTaskReq(InspectionTaskReq req, Long taskId) {
        List<String> emptyFields = new ArrayList<>();
        if (StringUtils.isBlank(req.getDispatchTime())) {
            emptyFields.add("dispatchTime");
        }
        if (req.getDynamicRiskLevel() == null) {
            emptyFields.add("dynamicRiskLevel");
        }
        if (StringUtils.isBlank(req.getInspectionSuggestion())) {
            emptyFields.add("inspectionSuggestion");
        }
        if (req.getInspectorId() == null) {
            emptyFields.add("inspectorId");
        }
        if (StringUtils.isBlank(req.getInspectorName())) {
            emptyFields.add("inspectorName");
        }
        if (StringUtils.isBlank(req.getInspectorPhone())) {
            emptyFields.add("inspectorPhone");
        }
        if (StringUtils.isBlank(req.getLocationCenter())) {
            emptyFields.add("locationCenter");
        }
        if (StringUtils.isBlank(req.getLocationDesc())) {
            emptyFields.add("locationDesc");
        }
        if (StringUtils.isBlank(req.getSlopeUnitId())) {
            emptyFields.add("slopeUnitId");
        }
        if (req.getStatus() == null) {
            emptyFields.add("status");
        }
        if (StringUtils.isBlank(req.getSubmitRequire())) {
            emptyFields.add("submitRequire");
        }
        if (req.getTaskId() == null) {
            emptyFields.add("taskId");
        }
        if (!emptyFields.isEmpty()) {
            throw new ServiceException("任务ID[" + taskId + "]推送数据校验失败，以下字段为空: " + String.join(", ", emptyFields));
        }
    }

    /**
     * 将斜坡单元ID规范化为4位补零格式，以兼容data_slope_unit表中id的存储格式。
     * 当unitId在流转中被转为"15"时，可正确匹配表中的"0015"记录。
     *
     * @param unitId 斜坡单元ID，可能为"15"或"0015"等格式
     * @return 规范化后的ID，纯数字则补零至4位，非数字或空则原样返回
     */
    static String normalizeSlopeUnitId(String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return unitId;
        }
        String trimmed = unitId.trim();
        if (!trimmed.matches("\\d+")) {
            return unitId;
        }
        return String.format("%4s", trimmed).replace(' ', '0');
    }

    @Override
    public void remind(Long id) {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZGSSZ.getRoleKey())) {
            throw new ServiceException("角色权限不足：仅乡自规所所长可催办任务");
        }
        DzTaskDistList task = requireTaskById(id);
        validateRoleBasedTaskAccess(task.getSourceType(), task.getPlanType(), false);
        validateCurrentUserTaskAccess(task.getUnitId(), "催办");
        sendRemind(task, new Date(), resolveManualRemindWindowMs(task));
    }

    @Override
    public TaskDistChainScopeActionResultVo remindByChainScope(TaskDistChainScopeActionBo bo) {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZGSSZ.getRoleKey())) {
            throw new ServiceException("角色权限不足：仅乡自规所所长可催办任务");
        }
        TaskScopeActionContext context = resolveTaskScopeActionContext(bo);
        TaskDistChainScopeActionResultVo result = context.result();
        if (context.taskIds().isEmpty()) {
            finishActionResult(result, "NO_TASK_MATCH", "未命中任何任务");
            return result;
        }
        Map<Long, DzTaskDistList> taskMap = loadTaskMap(context.taskIds());
        Date now = new Date();
        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (Long taskId : context.taskIds()) {
            DzTaskDistList task = taskMap.get(taskId);
            if (task == null) {
                skipped++;
                addActionFailure(result, taskId, "任务不存在或已删除");
                continue;
            }
            if (!DzTaskDistList.isOpenStatus(task.getStatus())) {
                skipped++;
                addActionFailure(result, taskId, "仅未闭环任务允许催办");
                continue;
            }
            try {
                validateRoleBasedTaskAccess(task.getSourceType(), task.getPlanType(), false);
                validateCurrentUserTaskAccess(task.getUnitId(), "催办");
                sendRemind(task, now, resolveManualRemindWindowMs(task));
                success++;
            } catch (Exception e) {
                failed++;
                addActionFailure(result, taskId, e.getMessage());
            }
        }
        fillActionCounts(result, success, skipped, failed, "催办");
        return result;
    }

    @Override
    public TaskDistChainScopeActionResultVo deleteByChainScope(TaskDistChainScopeActionBo bo) {
        TaskScopeActionContext context = resolveTaskScopeActionContext(bo);
        TaskDistChainScopeActionResultVo result = context.result();
        if (context.taskIds().isEmpty()) {
            finishActionResult(result, "NO_TASK_MATCH", "未命中任何任务");
            return result;
        }
        Map<Long, DzTaskDistList> taskMap = loadTaskMap(context.taskIds());
        String closeReason = buildManualDeleteCloseReason();
        Date now = new Date();
        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (Long taskId : context.taskIds()) {
            DzTaskDistList task = taskMap.get(taskId);
            if (task == null) {
                skipped++;
                addActionFailure(result, taskId, "任务不存在或已删除");
                continue;
            }
            try {
                validateDutyOfficerTaskAccess(task.getUnitId());
                softDeleteTasks(List.of(task), closeReason, now);
                success++;
            } catch (Exception e) {
                failed++;
                addActionFailure(result, taskId, e.getMessage());
            }
        }
        fillActionCounts(result, success, skipped, failed, "删除");
        return result;
    }

    private Map<Long, DzTaskDistList> loadTaskMap(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return baseMapper.selectBatchIds(taskIds).stream()
            .filter(Objects::nonNull)
            .filter(task -> task.getId() != null)
            .filter(task -> Objects.equals(task.getDelete(), 0))
            .collect(java.util.stream.Collectors.toMap(
                DzTaskDistList::getId,
                task -> task,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
    }

    private TaskScopeActionContext resolveTaskScopeActionContext(TaskDistChainScopeActionBo bo) {
        if (bo == null) {
            throw new ServiceException("链路范围操作参数不能为空");
        }
        TaskDistChainScopeActionResultVo result = new TaskDistChainScopeActionResultVo();
        result.setInputChainIdCount(0);
        result.setFilteredChainIdCount(0);
        result.setFinalChainIdCount(0);
        result.setExpandedTaskCount(0);
        result.setDeduplicatedTaskCount(0);
        result.setSuccessCount(0);
        result.setSkippedCount(0);
        result.setFailedCount(0);

        if (hasExplicitTaskSelectors(bo)) {
            LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
            if (bo.getTaskId() != null) {
                taskIds.add(bo.getTaskId());
            }
            if (bo.getTaskIds() != null) {
                bo.getTaskIds().stream().filter(Objects::nonNull).forEach(taskIds::add);
            }
            result.setExpandedTaskCount(taskIds.size());
            result.setDeduplicatedTaskCount(taskIds.size());
            return new TaskScopeActionContext(result, new ArrayList<>(taskIds));
        }

        Set<String> explicitChainIds = normalizedActionChainIds(bo.getChainIds());
        Set<String> filteredChainIds = resolveActionFilteredChainIds(bo.getLatestProcessNodeFilter());
        if (explicitChainIds.isEmpty() && filteredChainIds.isEmpty()) {
            throw new ServiceException("taskId、taskIds、chainIds和latestProcessNodeFilter不能同时为空");
        }
        Set<String> finalChainIds = resolveActionFinalChainIds(explicitChainIds, filteredChainIds);
        result.setInputChainIdCount(explicitChainIds.size());
        result.setFilteredChainIdCount(filteredChainIds.size());
        result.setFinalChainIdCount(finalChainIds.size());
        if (finalChainIds.isEmpty()) {
            return new TaskScopeActionContext(result, List.of());
        }

        List<Long> expandedTaskIds = taskProcessChainNodeService.queryRelatedTaskIdsByChainIds(finalChainIds);
        result.setExpandedTaskCount(expandedTaskIds.size());
        LinkedHashSet<Long> deduplicatedTaskIds = expandedTaskIds.stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        result.setDeduplicatedTaskCount(deduplicatedTaskIds.size());
        return new TaskScopeActionContext(result, new ArrayList<>(deduplicatedTaskIds));
    }

    private boolean hasExplicitTaskSelectors(TaskDistChainScopeActionBo bo) {
        return bo != null && (bo.getTaskId() != null || (bo.getTaskIds() != null && !bo.getTaskIds().isEmpty()));
    }

    private Set<String> normalizedActionChainIds(Collection<String> chainIds) {
        if (chainIds == null || chainIds.isEmpty()) {
            return Set.of();
        }
        return chainIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> resolveActionFilteredChainIds(TaskProcessChainFilterBo filterBo) {
        if (!hasActionFilterCondition(filterBo)) {
            return Set.of();
        }
        return taskProcessChainSummaryService.queryChainIdsByFilter(filterBo);
    }

    private Set<String> resolveActionFinalChainIds(Set<String> explicitChainIds, Set<String> filteredChainIds) {
        if (!explicitChainIds.isEmpty() && !filteredChainIds.isEmpty()) {
            LinkedHashSet<String> intersection = new LinkedHashSet<>(explicitChainIds);
            intersection.retainAll(filteredChainIds);
            return intersection;
        }
        return !explicitChainIds.isEmpty() ? explicitChainIds : filteredChainIds;
    }

    private boolean hasActionFilterCondition(TaskProcessChainFilterBo filterBo) {
        if (filterBo == null) {
            return false;
        }
        for (Field field : TaskProcessChainFilterBo.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(filterBo);
                if (value instanceof Collection<?> collection) {
                    if (!collection.isEmpty()) {
                        return true;
                    }
                } else if (value != null) {
                    return true;
                }
            } catch (IllegalAccessException e) {
                throw new ServiceException("链路筛选参数解析失败");
            }
        }
        return false;
    }

    private void fillActionCounts(TaskDistChainScopeActionResultVo result, int success, int skipped, int failed,
                                  String actionLabel) {
        result.setSuccessCount(success);
        result.setSkippedCount(skipped);
        result.setFailedCount(failed);
        if (success == 0 && failed == 0) {
            finishActionResult(result, "NO_TASK_MATCH", "未命中可" + actionLabel + "任务");
        } else if (failed > 0 || skipped > 0) {
            finishActionResult(result, "PARTIAL_SUCCESS", actionLabel + "完成，部分任务失败或跳过");
        } else {
            finishActionResult(result, "SUCCESS", actionLabel + "成功");
        }
    }

    private void finishActionResult(TaskDistChainScopeActionResultVo result, String code, String message) {
        result.setResultCode(code);
        result.setResultMessage(message);
    }

    private void addActionFailure(TaskDistChainScopeActionResultVo result, Long taskId, String reason) {
        TaskDistChainScopeActionResultVo.TaskActionFailure failure =
            new TaskDistChainScopeActionResultVo.TaskActionFailure();
        failure.setTaskId(taskId);
        failure.setReason(StringUtils.defaultIfBlank(reason, "处理失败"));
        result.getFailures().add(failure);
    }

    private DzTaskDistList requireTaskById(Long id) {
        if (id == null) {
            throw new ServiceException("任务ID不能为空");
        }
        DzTaskDistList task = baseMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        if (!DzTaskDistList.isOpenStatus(task.getStatus())) {
            throw new ServiceException("仅未闭环任务允许催办");
        }
        return task;
    }

    private record TaskScopeActionContext(TaskDistChainScopeActionResultVo result, List<Long> taskIds) {
    }

    void sendRemind(DzTaskDistList task, Date now, long duplicateWindowMs) {
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        if (duplicateWindowMs > 0 && hasRemindedInCurrentWindow(task, now.getTime() - duplicateWindowMs)) {
            throw new ServiceException("当前时间窗内已催办，请勿重复催办");
        }
        InspectionRemindReq req = InspectionRemindReq.builder()
            .taskId(task.getId())
            .build();
        appTaskService.remindTask(req);
        DzTaskDistList update = new DzTaskDistList();
        update.setId(task.getId());
        update.setLastRemindTime(now);
        update.setReminderCount((task.getReminderCount() == null ? 0 : task.getReminderCount()) + 1);
        update.setUpdateDate(now);
        if (baseMapper.updateById(update) <= 0) {
            throw new ServiceException("更新任务催办信息失败");
        }
        task.setLastRemindTime(now);
        task.setReminderCount(update.getReminderCount());
        task.setUpdateDate(now);
    }

    @Override
    public List<DzTaskDistListVo> selectTaskDistList(McpTaskDistListReq req) {
        QueryWrapper<DzTaskDistList> ew = Wrappers.query();
        ew.like(StringUtils.isNotBlank(req.getArea()), "concat(province, city, county, street, village, community)", req.getArea());
        if (req.getStartTime() != null && req.getEndTime() != null) {
            ew.ge("t1.create_date", req.getStartTime());
            ew.le("t1.create_date", req.getEndTime());
        }
        return baseMapper.selectTaskDistList(ew);
    }

    private DzBatchGenerateDelegate batchGenerateDelegate;

    private DzBatchGenerateDelegate batchGenerateDelegate() {
        if (batchGenerateDelegate == null) {
            batchGenerateDelegate = new DzBatchGenerateDelegate(this);
        }
        return batchGenerateDelegate;
    }

    @Override
    public Boolean batchGenerateByHandleId(Long handleId) {
        return batchGenerateDelegate().batchGenerateByHandleId(handleId);
    }

    @Override
    public Boolean batchGenerateByHandleId(Long handleId, Integer planType) {
        return batchGenerateDelegate().batchGenerateByHandleId(handleId, planType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DzTaskDistList closeLinkedEmergencyInvestigationTask(Long taskId, Long handleId, Date closedTime, String closeReason) {
        if (taskId == null) {
            throw new ServiceException("taskId不能为空");
        }
        DzTaskDistList task = baseMapper.selectById(taskId);
        if (task == null || Objects.equals(task.getDelete(), 1)) {
            throw new ServiceException("应急调查任务不存在");
        }
        if (!isEmergencyInvestigationTask(task)) {
            throw new ServiceException("taskId不是应急调查任务");
        }
        if (!isLinkedEmergencyInvestigationTask(task, handleId)) {
            throw new ServiceException("taskId不是当前处置关联的应急调查任务");
        }

        Date effectiveClosedTime = Objects.requireNonNullElseGet(closedTime, Date::new);
        String effectiveCloseReason = StringUtils.defaultIfBlank(closeReason, "应急调查现场记录已提交，自动更新任务状态为已反馈");
        boolean shouldBackfillHandleId = task.getHandleId() == null && handleId != null;
        boolean shouldUpdateStatus = !Objects.equals(task.getStatus(), DzTaskDistList.STATUS_FEEDBACKED);
        if (shouldUpdateStatus || shouldBackfillHandleId) {
            DzTaskDistList update = new DzTaskDistList();
            update.setId(task.getId());
            update.setUpdateDate(effectiveClosedTime);
            if (shouldUpdateStatus) {
                update.setStatus(DzTaskDistList.STATUS_FEEDBACKED);
                update.setClosedTime(effectiveClosedTime);
                update.setCloseReason(effectiveCloseReason);
            }
            if (shouldBackfillHandleId) {
                update.setHandleId(handleId);
            }
            if (baseMapper.updateById(update) <= 0) {
                throw new ServiceException("自动更新应急调查任务失败");
            }
            task.setStatus(DzTaskDistList.STATUS_FEEDBACKED);
            task.setClosedTime(effectiveClosedTime);
            task.setCloseReason(effectiveCloseReason);
            task.setUpdateDate(effectiveClosedTime);
            if (shouldBackfillHandleId) {
                task.setHandleId(handleId);
            }
        }
        syncClosedEmergencyInvestigationTaskToApp(task);
        return task;
    }

    static boolean isEmergencyInvestigationTask(DzTaskDistList task) {
        if (task == null || !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return false;
        }
        return StringUtils.isBlank(task.getTaskType())
               || Objects.equals(task.getTaskType(), DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION);
    }

    static boolean isLinkedEmergencyInvestigationTask(DzTaskDistList task, Long handleId) {
        if (!isEmergencyInvestigationTask(task)) {
            return false;
        }
        return handleId == null || task.getHandleId() == null || Objects.equals(task.getHandleId(), handleId);
    }

    private void syncClosedEmergencyInvestigationTaskToApp(DzTaskDistList task) {
        InspectionTaskReq updateReq = new InspectionTaskReq();
        updateReq.setTaskId(task.getId());
        updateReq.setStatus(DzTaskDistList.STATUS_FEEDBACKED);
        updateReq.setSourceType(task.getSourceType());
        updateReq.setTaskType(DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION);
        updateReq.setHandleId(task.getHandleId());
        appTaskService.updateTask(List.of(updateReq));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchGenerateByRange(List<String> streets, Long defId) {
        return batchGenerateDelegate().batchGenerateByRange(streets, defId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchGenerateByUnitIds(List<String> unitIds, Long defId) {
        return batchGenerateDelegate().batchGenerateByUnitIds(unitIds, defId);
    }

    Integer batchGenerateByUnitIdsCore(List<String> unitIds, Long defId) {
        return batchGenerateDelegate().batchGenerateByUnitIdsCore(unitIds, defId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeOpenDefRespTasksByDefId(Long defId, String reason) {
        return batchGenerateDelegate().closeOpenDefRespTasksByDefId(defId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshDefenseTaskSuggestionByDefIds(Collection<Long> defIds) {
        return batchGenerateDelegate().refreshDefenseTaskSuggestionByDefIds(defIds);
    }

    boolean updateTaskByIdAndSyncAppIfNeeded(DzTaskDistList beforeUpdate, DzTaskDistList update, String errorMessage) {
        if (update == null || update.getId() == null) {
            throw new ServiceException("任务更新参数缺失");
        }
        normalizeTaskPlanFields(update);
        update.setTaskType(resolveMergedStoredTaskType(beforeUpdate, update));
        boolean needSyncApp = isPushedUnfinishedTask(beforeUpdate);
        if (baseMapper.updateById(update) <= 0) {
            throw new ServiceException(errorMessage);
        }
        clearPlanFieldsIfNotEmergency(update.getId(), Objects.requireNonNullElse(update.getSourceType(), beforeUpdate == null ? null : beforeUpdate.getSourceType()));
        if (needSyncApp) {
            syncUpdatedTaskFullFieldsToApp(update.getId());
        }
        return true;
    }

    boolean updateMonitorWarningTaskByIdAndSyncAppIfNeeded(DzTaskDistList beforeUpdate, DzTaskDistList update, String errorMessage) {
        if (update == null || update.getId() == null) {
            throw new ServiceException("任务更新参数缺失");
        }
        update.setTaskType(resolveMergedStoredTaskType(beforeUpdate, update));
        boolean needSyncApp = isPushedUnfinishedTask(beforeUpdate);
        int updated = baseMapper.update(
            null,
            Wrappers.<DzTaskDistList>lambdaUpdate()
                .eq(DzTaskDistList::getId, update.getId())
                .set(DzTaskDistList::getUnitId, update.getUnitId())
                .set(DzTaskDistList::getUserId, update.getUserId())
                .set(DzTaskDistList::getRiskId, update.getRiskId())
                .set(DzTaskDistList::getSubmitRequire, update.getSubmitRequire())
                .set(DzTaskDistList::getInspectionSuggestion, update.getInspectionSuggestion())
                .set(DzTaskDistList::getInspectionSuggestionBackup, update.getInspectionSuggestionBackup())
                .set(DzTaskDistList::getSourceType, update.getSourceType())
                .set(DzTaskDistList::getTaskSource, update.getTaskSource())
                .set(DzTaskDistList::getTaskType, update.getTaskType())
                .set(DzTaskDistList::getPlanName, null)
                .set(DzTaskDistList::getPlanType, null)
                .set(DzTaskDistList::getResponsiblePerson, update.getResponsiblePerson())
                .set(DzTaskDistList::getResponsiblePersonPhone, update.getResponsiblePersonPhone())
                .set(DzTaskDistList::getDetailedAddress, update.getDetailedAddress())
                .set(DzTaskDistList::getSmsContent, update.getSmsContent())
                .set(DzTaskDistList::getDelete, update.getDelete())
                .set(DzTaskDistList::getOverdue, update.getOverdue())
                .set(DzTaskDistList::getQuotaConsumed, update.getQuotaConsumed())
                .set(DzTaskDistList::getReminderCount, update.getReminderCount())
                .set(DzTaskDistList::getUpdateDate, update.getUpdateDate())
        );
        if (updated <= 0) {
            throw new ServiceException(errorMessage);
        }
        if (needSyncApp) {
            syncUpdatedTaskFullFieldsToApp(update.getId());
        }
        return true;
    }

    boolean updateTaskInfoByIdAndSyncAppIfNeeded(DzTaskDistList beforeUpdate, DzTaskDistList update, String errorMessage) {
        validateTaskInfoUpdateAllowed(beforeUpdate);
        return updateTaskByIdAndSyncAppIfNeeded(beforeUpdate, update, errorMessage);
    }

    boolean updateMonitorWarningTaskInfoByIdAndSyncAppIfNeeded(DzTaskDistList beforeUpdate, DzTaskDistList update, String errorMessage) {
        validateTaskInfoUpdateAllowed(beforeUpdate);
        return updateMonitorWarningTaskByIdAndSyncAppIfNeeded(beforeUpdate, update, errorMessage);
    }

    void updateTaskInfosBatchAndSyncAppIfNeeded(List<DzTaskDistList> tasks, String errorMessage) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        Date now = new Date();
        List<DzTaskDistList> eligibleTasks = tasks.stream()
            .filter(Objects::nonNull)
            .filter(task -> canUpdateTaskInfo(task, now))
            .toList();
        if (eligibleTasks.isEmpty()) {
            log.info("批量任务信息更新跳过，原因：仅允许修改当天且状态为2/3的任务, taskIds={}",
                tasks.stream().filter(Objects::nonNull).map(DzTaskDistList::getId).filter(Objects::nonNull).toList());
            return;
        }
        if (eligibleTasks.size() != tasks.size()) {
            List<Long> skippedTaskIds = tasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> !canUpdateTaskInfo(task, now))
                .map(DzTaskDistList::getId)
                .filter(Objects::nonNull)
                .toList();
            log.info("批量任务信息更新部分跳过，原因：仅允许修改当天且状态为2/3的任务, skippedTaskIds={}", skippedTaskIds);
        }
        updateTasksBatchAndSyncAppIfNeeded(eligibleTasks, collectPushedUnfinishedTaskIds(eligibleTasks), errorMessage);
    }

    void updateTasksBatchAndSyncAppIfNeeded(List<DzTaskDistList> tasks, String errorMessage) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        updateTasksBatchAndSyncAppIfNeeded(tasks, collectPushedUnfinishedTaskIds(tasks), errorMessage);
    }

    void updateTasksBatchAndSyncAppIfNeeded(List<DzTaskDistList> tasks, List<Long> appSyncTaskIds, String errorMessage) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        normalizeTaskPlanFields(tasks);
        tasks.stream().filter(Objects::nonNull).forEach(this::populateStoredTaskType);
        if (!baseMapper.updateBatchById(tasks)) {
            throw new ServiceException(errorMessage);
        }
        clearPlanFieldsIfNotEmergency(tasks);
        syncUpdatedTasksFullFieldsToApp(appSyncTaskIds);
    }

    void normalizeTaskPlanFields(DzTaskDistList task) {
        if (task == null) {
            return;
        }
        if (Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            normalizeEmergencyTaskPlanFields(task);
            return;
        }
        task.setPlanType(null);
    }

    private void normalizeEmergencyTaskPlanFields(DzTaskDistList task) {
        PlanTypeEnum planType = PlanTypeEnum.getByCode(task.getPlanType());
        if (planType == null && StringUtils.isNotBlank(task.getPlanName())) {
            planType = PlanTypeEnum.getByName(task.getPlanName().trim());
        }
        if (planType == null) {
            task.setPlanType(null);
            return;
        }
        task.setPlanType(planType.getCode());
        task.setPlanName(planType.getName());
    }

    void normalizeTaskPlanFields(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        tasks.forEach(this::normalizeTaskPlanFields);
    }

    private String resolveMergedStoredTaskType(DzTaskDistList beforeUpdate, DzTaskDistList update) {
        Integer sourceType = update.getSourceType() != null
            ? update.getSourceType()
            : beforeUpdate == null ? null : beforeUpdate.getSourceType();
        Long reportId = update.getReportId() != null
            ? update.getReportId()
            : beforeUpdate == null ? null : beforeUpdate.getReportId();
        DzReportDisaster report = reportId == null ? null : dzReportDisasterMapper.selectById(reportId);
        return resolveStoredTaskType(sourceType, report);
    }

    private void clearPlanFieldsIfNotEmergency(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return;
        }
        tasks.stream()
            .filter(Objects::nonNull)
            .forEach(task -> clearPlanFieldsIfNotEmergency(task.getId(), task.getSourceType()));
    }

    private void clearPlanFieldsIfNotEmergency(Long taskId, Integer sourceType) {
        if (taskId == null || Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return;
        }
        baseMapper.update(
            null,
            Wrappers.<DzTaskDistList>lambdaUpdate()
                .eq(DzTaskDistList::getId, taskId)
                .set(DzTaskDistList::getPlanName, null)
                .set(DzTaskDistList::getPlanType, null)
        );
    }

    List<Long> collectPushedUnfinishedTaskIds(List<DzTaskDistList> tasks) {
        if (ObjUtil.isEmpty(tasks)) {
            return List.of();
        }
        return tasks.stream()
            .filter(this::isPushedUnfinishedTask)
            .map(DzTaskDistList::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private boolean isPushedUnfinishedTask(DzTaskDistList task) {
        return task != null
               && (Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNINSPECTED)
                   || Objects.equals(task.getStatus(), DzTaskDistList.STATUS_INSPECTING));
    }

    static boolean canUpdateTaskInfo(DzTaskDistList task, Date referenceDate) {
        if (task == null || task.getCreateDate() == null || referenceDate == null) {
            return false;
        }
        return isPushedUnfinishedTaskStatus(task.getStatus()) && DateUtil.isSameDay(task.getCreateDate(), referenceDate);
    }

    static boolean isPushedUnfinishedTaskStatus(Integer status) {
        return Objects.equals(status, DzTaskDistList.STATUS_UNINSPECTED)
               || Objects.equals(status, DzTaskDistList.STATUS_INSPECTING);
    }

    int pushTasksAndMaybeSendSms(List<DzTaskDistList> tasks, boolean sendTaskSms, String sceneLabel) {
        if (ObjUtil.isEmpty(tasks)) {
            return 0;
        }
        int pushed = 0;
        List<DzTaskDistList> pushedTasks = new ArrayList<>();
        for (DzTaskDistList task : tasks) {
            try {
                pushDailyPatrolTask(task);
                recordTaskPushProcessNode(task, false, new Date(), sceneLabel);
                pushed++;
                pushedTasks.add(task);
            } catch (Exception e) {
                log.warn("{}推送失败, taskId={}", sceneLabel, task == null ? null : task.getId(), e);
            }
        }
        // 所有任务推送统一进入短信场景策略；是否真实发送由 sys_config 动态配置决定。
        if (!pushedTasks.isEmpty()) {
            try {
                pushSmsDelegate().sendTaskSmsAfterPush(pushedTasks);
            } catch (Exception e) {
                log.warn("{}短信发送失败, taskIds={}", sceneLabel, pushedTasks.stream().map(DzTaskDistList::getId).toList(), e);
            }
        }
        return pushed;
    }

    private void validateTaskInfoUpdateAllowed(DzTaskDistList beforeUpdate) {
        if (canUpdateTaskInfo(beforeUpdate, new Date())) {
            return;
        }
        throw new ServiceException("仅允许修改当天且状态为2、3的任务信息");
    }

    private void syncUpdatedTaskFullFieldsToApp(Long taskId) {
        if (taskId == null) {
            return;
        }
        syncUpdatedTasksFullFieldsToApp(List.of(taskId));
    }

    private void syncUpdatedTasksFullFieldsToApp(List<Long> taskIds) {
        if (ObjUtil.isEmpty(taskIds)) {
            return;
        }
        List<DzTaskDistList> latestTasks = baseMapper.selectBatchIds(taskIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        List<InspectionTaskReq> updateReqs = latestTasks.stream()
            .filter(Objects::nonNull)
            .filter(task -> task.getId() != null)
            .map(task -> buildFullInspectionTaskUpdateReq(task, task.getStatus()))
            .toList();
        if (updateReqs.isEmpty()) {
            return;
        }
        appTaskService.updateTask(updateReqs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateAlertTasksByDefId(Long defId) {
        return batchGenerateDelegate().batchCreateAlertTasksByDefId(defId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateAlertTasksByUnitIds(Long defId, List<String> unitIds) {
        return batchGenerateDelegate().batchCreateAlertTasksByUnitIds(defId, unitIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer scheduleCreateMonitorTasks() {
        return batchGenerateDelegate().scheduleCreateMonitorTasks();
    }

    Set<String> listExistingDefRespTaskUnitIds(Long defId, Collection<String> unitIds, String planName, Date start, Date end) {
        return batchGenerateDelegate().listExistingDefRespTaskUnitIds(defId, unitIds, planName, start, end);
    }

    List<DzTaskDistList> buildDefRespSingleTasksInParallel(List<RiskUnitOnDate> highRiskUnits, Long defId, Long handleId,
                                                           MonitorFrequencyParams freqParams, Date now,
                                                           Set<String> existingMonitorUnitIds, Set<String> existingPatrolUnitIds,
                                                           Set<String> existingOpenDailyPatrolUnitIds) {
        return batchGenerateDelegate().buildDefRespSingleTasksInParallel(
            highRiskUnits, defId, handleId, freqParams, now, existingMonitorUnitIds, existingPatrolUnitIds, existingOpenDailyPatrolUnitIds
        );
    }

    private DzDailyPatrolDelegate dailyPatrolDelegate;

    private DzDailyPatrolDelegate dailyPatrolDelegate() {
        if (dailyPatrolDelegate == null) {
            dailyPatrolDelegate = new DzDailyPatrolDelegate(this);
        }
        return dailyPatrolDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scheduleCreatePatrolQuotaTasks() {
        dailyPatrolDelegate().scheduleCreatePatrolQuotaTasks();
    }

    @Override
    public Integer schedulePatrolTaskReminders() {
        return miscDelegate().schedulePatrolTaskReminders();
    }

    @Override
    public Integer autoPushUnpushedTasks(List<Integer> sourceTypes, int limit, boolean sendTaskSms) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return 0;
        }
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_UNPUSHED)
                .eq(DzTaskDistList::getDelete, 0)
                .in(DzTaskDistList::getSourceType, sourceTypes)
                .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
                .last("limit " + Math.max(1, limit))
        );
        return autoPushTaskList(tasks, sendTaskSms);
    }

    @Override
    public Integer autoPushDailyPatrolTasks(int limit, boolean sendTaskSms) {
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_UNPUSHED)
                .eq(DzTaskDistList::getDelete, 0)
                .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EVAL)
                .and(wrapper -> wrapper.eq(DzTaskDistList::getPlanName, DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                    .or()
                    .eq(DzTaskDistList::getTaskSource, DzTaskDistList.PLAN_NAME_DAILY_PATROL))
                .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
                .last("limit " + Math.max(1, limit))
        );
        return autoPushTaskList(tasks, sendTaskSms);
    }

    @Override
    public Integer autoRemindOpenTasks(List<Integer> sourceTypes, int limit) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return 0;
        }
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .in(DzTaskDistList::getStatus, DzTaskDistList.STATUS_UNINSPECTED, DzTaskDistList.STATUS_INSPECTING)
                .eq(DzTaskDistList::getDelete, 0)
                .in(DzTaskDistList::getSourceType, sourceTypes)
                .orderByAsc(DzTaskDistList::getLastRemindTime, DzTaskDistList::getCreateDate, DzTaskDistList::getId)
                .last("limit " + Math.max(1, limit))
        );
        int reminded = 0;
        Date now = new Date();
        for (DzTaskDistList task : tasks) {
            try {
                sendRemind(task, now, resolveManualRemindWindowMs(task));
                reminded++;
            } catch (Exception e) {
                log.warn("自动化运营催办任务失败, taskId={}", task == null ? null : task.getId(), e);
            }
        }
        return reminded;
    }

    @Override
    public Integer autoPushTasksByReportId(Long reportId, boolean sendTaskSms) {
        if (reportId == null) {
            return 0;
        }
        List<DzTaskDistList> tasks = baseMapper.selectList(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .eq(DzTaskDistList::getReportId, reportId)
                .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_UNPUSHED)
                .eq(DzTaskDistList::getDelete, 0)
                .orderByAsc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
        );
        return autoPushTaskList(tasks, sendTaskSms);
    }

    private int autoPushTaskList(List<DzTaskDistList> tasks, boolean sendTaskSms) {
        return pushTasksAndMaybeSendSms(tasks, sendTaskSms, "自动化运营任务");
    }

    List<String> resolveSlopeUnitIds(DefRespPlan plan) {
        return dailyPatrolDelegate().resolveSlopeUnitIds(plan);
    }

    PlanUnitTaskContext buildPlanUnitTaskContext(List<DefRespPlan> plans) {
        return dailyPatrolDelegate().buildPlanUnitTaskContext(plans);
    }

    List<String> resolveSlopeUnitIds(DefRespPlan plan, Map<Long, DzTaskHandle> handleMap, Map<String, List<String>> unitIdsByStreetKey) {
        return dailyPatrolDelegate().resolveSlopeUnitIds(plan, handleMap, unitIdsByStreetKey);
    }

    List<RiskUnitOnDate> listUnitIdsWithHighAndVeryHighRiskOnDate(List<String> unitIds, Date date) {
        return dailyPatrolDelegate().listUnitIdsWithHighAndVeryHighRiskOnDate(unitIds, date);
    }

    static List<String> splitStreets(String streets) {
        return DzDailyPatrolDelegate.splitStreets(streets);
    }

    MonitorFrequencyParams resolveMonitorFrequencyParams(Integer level) {
        return dailyPatrolDelegate().resolveMonitorFrequencyParams(level);
    }

    DzTaskDistList buildMonitorTask(Long defId, Long handleId, String unitId, Long riskId, MonitorFrequencyParams freqParams, Date now) {
        return dailyPatrolDelegate().buildMonitorTask(defId, handleId, unitId, riskId, freqParams, now);
    }

    DzTaskDistList buildPatrolTask(Long defId, Long handleId, String unitId, Long riskId, MonitorFrequencyParams freqParams, Date now) {
        return dailyPatrolDelegate().buildPatrolTask(defId, handleId, unitId, riskId, freqParams, now);
    }

    Set<String> listExistingOpenDailyPatrolTaskUnitIds(Collection<String> unitIds, Date date) {
        return dailyPatrolDelegate().listExistingOpenDailyPatrolTaskUnitIds(unitIds, date);
    }

    Set<String> buildDefRespCoveredUnitIdSet(PlanUnitTaskContext context) {
        return dailyPatrolDelegate().buildDefRespCoveredUnitIdSet(context);
    }

    DailyPatrolCreateResult createDailyPatrolTasks(Date now, Set<String> defRespCoveredUnitIds) {
        return dailyPatrolDelegate().createDailyPatrolTasks(now, defRespCoveredUnitIds);
    }

    void closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance() {
        dailyPatrolDelegate().closeExpiredPatrolTasksBeforeTodayAndCalculateCompliance();
    }

    @Override
    public Integer closePreviousDayPatrolTasks() {
        return dailyPatrolDelegate().closePreviousDayPatrolTasks();
    }

    List<DefRespPlan> listTaskPublishedDefRespPlansForSchedule() {
        return dailyPatrolDelegate().listTaskPublishedDefRespPlansForSchedule();
    }

    List<DefRespPlan> listDefenseStartedDefRespPlansForSchedule() {
        return dailyPatrolDelegate().listDefenseStartedDefRespPlansForSchedule();
    }

    boolean isTownDefRespPlan(DefRespPlan defRespPlan) {
        return dailyPatrolDelegate().isTownDefRespPlan(defRespPlan);
    }

    List<RiskUnitOnDate> buildLatestRiskUnits(List<DzRiskAssessment> assessments) {
        return dailyPatrolDelegate().buildLatestRiskUnits(assessments);
    }

    DzTaskDistList buildDailyPatrolTask(RiskUnitOnDate riskUnit, Date now) {
        return dailyPatrolDelegate().buildDailyPatrolTask(riskUnit, now);
    }

    DzTaskDistList loadLatestDailyPatrolTaskOnDate(String unitId, Date date) {
        return dailyPatrolDelegate().loadLatestDailyPatrolTaskOnDate(unitId, date);
    }

    boolean isFinishedDailyPatrolStatus(Integer status) {
        return dailyPatrolDelegate().isFinishedDailyPatrolStatus(status);
    }

    void insertDailyPatrolTask(DzTaskDistList task) {
        dailyPatrolDelegate().insertDailyPatrolTask(task);
    }

    void refreshExistingDailyPatrolTask(DzTaskDistList target, DzTaskDistList source, Date now) {
        dailyPatrolDelegate().refreshExistingDailyPatrolTask(target, source, now);
    }

    void pushDailyPatrolTask(DzTaskDistList task) {
        dailyPatrolDelegate().pushDailyPatrolTask(task);
    }

    InspectionTaskReq buildFullInspectionTaskUpdateReq(DzTaskDistList task, Integer status) {
        return dailyPatrolDelegate().buildFullInspectionTaskUpdateReq(task, status);
    }

    int createDefRespPatrolTasks(List<DefRespPlan> taskPublishedPlans, PlanUnitTaskContext taskPublishedContext, Date now) {
        return dailyPatrolDelegate().createDefRespPatrolTasks(taskPublishedPlans, taskPublishedContext, now);
    }

    DailyPatrolCreateResult createDailyPatrolTasks(List<RiskUnitOnDate> dailyCandidates, Date now, Set<String> defRespCoveredUnitIds) {
        return dailyPatrolDelegate().createDailyPatrolTasks(dailyCandidates, now, defRespCoveredUnitIds);
    }

    int applyDefenseStartedDailyPatrolUpdates(List<DefRespPlan> plans, PlanUnitTaskContext context) {
        return dailyPatrolDelegate().applyDefenseStartedDailyPatrolUpdates(plans, context);
    }

    @Override
    public InspectionRuleStatsVo getInspectionRuleStats(LocalDate date) {
        return miscDelegate().getInspectionRuleStats(date);
    }

    boolean hasRemindedInCurrentWindow(DzTaskDistList task, long windowStartMs) {
        return miscDelegate().hasRemindedInCurrentWindow(task, windowStartMs);
    }

    long resolveManualRemindWindowMs(DzTaskDistList task) {
        return miscDelegate().resolveManualRemindWindowMs(task);
    }

    DzUserContactVo tryResolveHandleTaskAssignee(DzTaskHandle handle, String unitId) {
        return miscDelegate().tryResolveHandleTaskAssignee(handle, unitId);
    }

    DzUserContactVo tryResolveSlopeUnitAssigneeForPatrol(String unitId, String taskType) {
        return miscDelegate().tryResolveSlopeUnitAssigneeForPatrol(unitId, taskType);
    }

    DzUserContactVo tryResolveSlopeUnitAssignee(String unitId, String taskType) {
        return miscDelegate().tryResolveSlopeUnitAssignee(unitId, taskType);
    }

    DzUserContactVo resolveSlopeUnitAssignee(String unitId, String taskType) {
        return miscDelegate().resolveSlopeUnitAssignee(unitId, taskType);
    }

    @Override
    public List<TaskDistDayStatVo> statDay(String startDateStr, String endDateStr) {
        return miscDelegate().statDay(startDateStr, endDateStr);
    }

    @Override
    public SmsSendSummaryVo sendDefRespSmsByDefId(Long defId) {
        return miscDelegate().sendDefRespSmsByDefId(defId);
    }

    String defaultString(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
    }

    String buildEvalTaskSource(Date createDate) {
        Date date = createDate != null ? createDate : new Date();
        return DateUtil.format(date, "MM月dd日") + "风险研判";
    }

    String buildReportTaskSource(DzReportDisaster report) {
        return joinUserRoleAndName(report == null ? null : report.getUserRole(),
            report == null ? null : report.getUserName()) + "上报地灾迹象";
    }

    String buildDefRespTaskSource(Long defId) {
        DefRespPlan plan = defId == null ? null : dzDefRespPlanMapper.selectById(defId);
        return plan != null && StringUtils.isNotBlank(plan.getName())
            ? plan.getName()
            : resolveTaskSource(DzTaskDistList.SOURCE_TYPE_DEF_RESP);
    }

    String buildEmergencyTaskSource(String slopeUnitId) {
        return normalizeSlopeUnitIdForText(slopeUnitId) + "号斜坡单元触发应急处置事件";
    }

    String buildMonitorWarningTaskSource(String slopeUnitId, Integer dynamicRiskLevel) {
        return normalizeSlopeUnitIdForText(slopeUnitId)
               + "号斜坡单元触发设备预警导致斜坡单元风险等级上升为"
               + resolveRiskColor(dynamicRiskLevel)
               + "风险等级";
    }

    String buildTechAssistanceTaskSource(DzReportDisaster report, String slopeUnitId) {
        return joinUserRoleAndName(report == null ? null : report.getUserRole(),
            report == null ? null : report.getUserName())
               + "在"
               + normalizeSlopeUnitIdForText(slopeUnitId)
               + "号斜坡单元发现地灾迹象，申请技术协查";
    }

    String buildManualTaskSource() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        String userName = loginUser == null ? null : StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        String role = null;
        if (userId != null) {
            try {
                role = sysUserService.selectUserRoleGroup(userId);
            } catch (Exception e) {
                log.warn("获取当前用户角色失败，手动任务来源描述将不包含角色, userId={}", userId, e);
            }
        }
        return joinUserRoleAndName(role, userName) + "手动添加";
    }

    private String joinUserRoleAndName(String userRole, String userName) {
        String joined = Stream.of(userRole, userName)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(java.util.stream.Collectors.joining());
        return StringUtils.isNotBlank(joined) ? joined : "";
    }

    private String normalizeSlopeUnitIdForText(String slopeUnitId) {
        String unitId = normalizeSlopeUnitId(slopeUnitId);
        return StringUtils.isNotBlank(unitId) ? unitId : "未知";
    }

    private String resolveRiskColor(Integer dynamicRiskLevel) {
        if (Objects.equals(dynamicRiskLevel, 4)) {
            return "红色";
        }
        if (Objects.equals(dynamicRiskLevel, 3)) {
            return "橙色";
        }
        if (Objects.equals(dynamicRiskLevel, 2)) {
            return "黄色";
        }
        if (Objects.equals(dynamicRiskLevel, 1)) {
            return "蓝色";
        }
        return "未知";
    }

    static String resolveTaskSource(Integer sourceType) {
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EVAL)) {
            return "系统评估";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_REPORT)) {
            return "群众上报";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)) {
            return "防御响应";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)) {
            return "应急处置";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MONITOR_WARNING)) {
            return "监测预警";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return "技术协查";
        }
        if (Objects.equals(sourceType, DzTaskDistList.SOURCE_TYPE_MANUAL)) {
            return "手动添加";
        }
        return null;
    }

    static boolean isDailyPatrolTask(DzTaskDistList task) {
        return task != null
               && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_EVAL)
               && (Objects.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_DAILY_PATROL)
                   || Objects.equals(task.getTaskSource(), DzTaskDistList.PLAN_NAME_DAILY_PATROL));
    }

    static boolean isDefRespMonitorTask(DzTaskDistList task) {
        return task != null
               && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)
               && (Objects.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_MONITOR)
                   || Objects.equals(task.getTaskSource(), DzTaskDistList.PLAN_NAME_MONITOR));
    }

    static boolean isDefRespPatrolTask(DzTaskDistList task) {
        return task != null
               && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP)
               && (Objects.equals(task.getPlanName(), DzTaskDistList.PLAN_NAME_PATROL)
                   || Objects.equals(task.getTaskSource(), DzTaskDistList.PLAN_NAME_PATROL));
    }
}
