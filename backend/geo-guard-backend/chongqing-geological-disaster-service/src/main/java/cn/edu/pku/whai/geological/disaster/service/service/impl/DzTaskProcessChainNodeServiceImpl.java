/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainSegmentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务流程链路节点 Service 实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskProcessChainNodeServiceImpl implements IDzTaskProcessChainNodeService {

    static final int PREVIOUS_NODE_RETRY_COUNT = 3;
    static final long PREVIOUS_NODE_RETRY_INTERVAL_MILLIS = 10_000L;
    static final DateTimeFormatter DAILY_CHAIN_ID_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final String DAILY_CHAIN_ID_LOCK_PREFIX = "DAILY_CHAIN_ID:";
    private static final Integer BIZ_TYPE_TASK = TaskProcessBizTypeEnum.TASK.getCode();
    private static final Integer BIZ_TYPE_REPORT = TaskProcessBizTypeEnum.REPORT.getCode();
    private static final Integer BIZ_TYPE_HANDLE = TaskProcessBizTypeEnum.HANDLE.getCode();
    private static final Integer BIZ_TYPE_DEF_RESP = TaskProcessBizTypeEnum.DEF_RESP.getCode();
    private static final Integer BIZ_TYPE_ALARM = TaskProcessBizTypeEnum.ALARM.getCode();
    private static final Integer BIZ_TYPE_MONITOR_WARNING = TaskProcessBizTypeEnum.MONITOR_WARNING.getCode();

    private final DzTaskProcessChainNodeMapper chainNodeMapper;
    private final DzTaskDistListMapper taskDistListMapper;
    private final DzReportDisasterMapper reportDisasterMapper;
    private final DzTaskHandleMapper taskHandleMapper;
    private final DzTaskHandleSceneRecordMapper taskHandleSceneRecordMapper;
    private final DzDefRespPlanMapper defRespPlanMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordNode(DzTaskProcessChainNode node) {
        return insertNode(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordNodeIfAbsent(DzTaskProcessChainNode node) {
        if (node == null) {
            return null;
        }
        prepareBasicNode(node);
        DzTaskProcessChainNode existed = chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getChainId, node.getChainId())
                .eq(DzTaskProcessChainNode::getLinkName, node.getLinkName())
                .eq(DzTaskProcessChainNode::getBizType, node.getBizType())
                .eq(DzTaskProcessChainNode::getBizId, node.getBizId())
                .eq(node.getTaskId() != null, DzTaskProcessChainNode::getTaskId, node.getTaskId())
                .isNull(node.getTaskId() == null, DzTaskProcessChainNode::getTaskId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .last("limit 1")
        );
        if (existed != null) {
            prepareExistingDisplayFields(existed);
            fillMissingFields(existed, node);
            taskProcessChainSummaryService.refreshByNode(chainNodeMapper.selectById(existed.getId()));
            return existed.getId();
        }
        validateNodeBeforeParentLookup(node);
        prepareParentNodeStrictly(node);
        chainNodeMapper.insert(node);
        taskProcessChainSummaryService.refreshByNode(node);
        return node.getId();
    }

    @Override
    public Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task) {
        return recordTaskNode(chainId, linkName, triggerReason, task, null, null);
    }

    @Override
    public Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task,
                               Long parentNodeId, Integer nodeCategory) {
        return recordTaskNode(chainId, linkName, triggerReason, task, parentNodeId, nodeCategory, null);
    }

    @Override
    public Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task,
                               Long parentNodeId, Integer nodeCategory, Integer stageType) {
        if (task == null || task.getId() == null) {
            return null;
        }
        Long operatorId = resolveTaskNodeOperatorId(task);
        Integer sourceType = TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode();
        String refinedLinkName = TaskProcessChainNodeTextEnum.refineTaskLinkName(linkName, sourceType, task.getTaskType());
        return recordBizNode(
            chainId,
            refinedLinkName,
            triggerReason,
            BIZ_TYPE_TASK,
            task.getId(),
            task.getId(),
            operatorId,
            resolveTaskNodeOperatorName(task, operatorId),
            sourceType,
            parentNodeId,
            resolveOperatorRole(operatorId, null),
            nodeCategory,
            resolveStageType(stageType, refinedLinkName, BIZ_TYPE_TASK, sourceType, task),
            task.getTaskType()
        );
    }

    static Long resolveAutoAgentOperatorId() {
        return DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID;
    }

    static String resolveAutoAgentOperatorName() {
        return DzTaskDistListServiceImpl.AUTO_AGENT_NAME;
    }

    private Long resolveTaskNodeOperatorId(DzTaskDistList task) {
        if (task == null) {
            return null;
        }
        if (task.getTaskCreateUserId() != null) {
            return task.getTaskCreateUserId();
        }
        if (Objects.equals(task.getTaskCreateType(), DzTaskDistList.TASK_CREATE_TYPE_SYSTEM)
            || isImplicitSystemGeneratedTask(task)) {
            return resolveAutoAgentOperatorId();
        }
        return task.getUserId();
    }

    private String resolveTaskNodeOperatorName(DzTaskDistList task, Long operatorId) {
        if (Objects.equals(operatorId, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID)) {
            return DzTaskDistListServiceImpl.AUTO_AGENT_NAME;
        }
        return task == null ? null : task.getResponsiblePerson();
    }

    private boolean isImplicitSystemGeneratedTask(DzTaskDistList task) {
        return task != null
            && task.getSourceType() != null
            && !Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_MANUAL);
    }

    @Override
    public Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                              Long taskId, Long operatorId, String operatorName, Integer sourceType) {
        return recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId, operatorName,
            sourceType, null, null, null);
    }

    @Override
    public Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                              Long taskId, Long operatorId, String operatorName, Integer sourceType,
                              Long parentNodeId, String operatorRole, Integer nodeCategory) {
        return recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId, operatorName,
            sourceType, parentNodeId, operatorRole, nodeCategory, null);
    }

    @Override
    public Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                              Long taskId, Long operatorId, String operatorName, Integer sourceType,
                              Long parentNodeId, String operatorRole, Integer nodeCategory, Integer stageType) {
        Integer resolvedSourceType = resolveChainSourceType(sourceType, bizType);
        return recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId, operatorId, operatorName,
            resolvedSourceType, parentNodeId, operatorRole, nodeCategory, stageType, null);
    }

    @Override
    public Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                              Long taskId, Long operatorId, String operatorName, Integer sourceType,
                              Long parentNodeId, String operatorRole, Integer nodeCategory, Integer stageType,
                              String taskType) {
        DzTaskProcessChainNode node = new DzTaskProcessChainNode();
        Integer resolvedSourceType = resolveChainSourceType(sourceType, bizType);
        String refinedLinkName = BIZ_TYPE_TASK.equals(bizType)
            ? TaskProcessChainNodeTextEnum.refineTaskLinkName(linkName, resolvedSourceType, taskType)
            : TaskProcessChainNodeTextEnum.normalizeDisplayLinkName(linkName);
        node.setChainId(chainId);
        node.setParentNodeId(parentNodeId);
        node.setLinkName(refinedLinkName);
        node.setTriggerReason(triggerReason);
        node.setOperatorId(operatorId);
        node.setOperatorName(operatorName);
        node.setOperatorRole(resolveOperatorRole(operatorId, operatorRole));
        node.setBizType(bizType);
        node.setBizId(bizId);
        node.setTaskId(taskId);
        node.setSourceType(resolvedSourceType);
        node.setNodeCategory(resolveNodeCategory(nodeCategory, refinedLinkName));
        node.setStageType(resolveStageType(stageType, refinedLinkName, bizType, resolvedSourceType, null));
        return recordNodeIfAbsent(node);
    }

    @Override
    public String generateChainId() {
        return generateChainId(LocalDate.now());
    }

    String generateChainId(LocalDate date) {
        LocalDate bizDate = date == null ? LocalDate.now() : date;
        String datePrefix = DAILY_CHAIN_ID_DATE_FORMATTER.format(bizDate);
        chainNodeMapper.lockChain(DAILY_CHAIN_ID_LOCK_PREFIX + datePrefix);
        Integer maxSequence = chainNodeMapper.selectMaxDailyChainSequence(datePrefix);
        int nextSequence = maxSequence == null ? 1 : maxSequence + 1;
        return datePrefix + "-" + String.format("%03d", nextSequence);
    }

    static Integer resolveChainSourceType(Integer sourceType, Integer bizType) {
        if (sourceType != null) {
            return TaskProcessSourceTypeEnum.of(sourceType).getCode();
        }
        return TaskProcessSourceTypeEnum.fromBizType(bizType).getCode();
    }

    static Integer resolveLegacyChainSourceType(String sourceType, Integer bizType) {
        return TaskProcessSourceTypeEnum.fromLegacy(sourceType, bizType).getCode();
    }

    private Long insertNode(DzTaskProcessChainNode node) {
        if (node == null) {
            return null;
        }
        prepareNode(node);
        chainNodeMapper.insert(node);
        taskProcessChainSummaryService.refreshByNode(node);
        return node.getId();
    }

    private void prepareNode(DzTaskProcessChainNode node) {
        prepareBasicNode(node);
        prepareParentNodeStrictly(node);
    }

    private void prepareBasicNode(DzTaskProcessChainNode node) {
        if (StrUtil.isBlank(node.getChainId())) {
            throw new ServiceException("流程链路chainId不能为空");
        }
        node.setChainId(node.getChainId().trim());
        node.setTriggerTime(null);
        node.setCreateDate(null);
        node.setUpdateDate(null);
        node.setOperatorRole(resolveOperatorRole(node.getOperatorId(), node.getOperatorRole()));
        node.setNodeCategory(resolveNodeCategory(node.getNodeCategory(), node.getLinkName()));
        node.setStageType(resolveStageType(node.getStageType(), node.getLinkName(), node.getBizType(), node.getSourceType(), null));
        if (node.getDeleted() == null) {
            node.setDeleted(0);
        }
    }

    private void prepareParentNodeStrictly(DzTaskProcessChainNode node) {
        if (node == null) {
            throw new ServiceException("流程链路节点不能为空");
        }
        if (isRootNode(node)) {
            chainNodeMapper.lockChain(node.getChainId());
            DzTaskProcessChainNode existed = selectLatestNodeByChainId(node.getChainId());
            if (existed != null) {
                throw new ServiceException("流程根节点chainId已存在: " + node.getChainId());
            }
            fillChainDisplayFields(node, null);
            return;
        }

        DzTaskProcessChainNode parent = queryPreviousNodeWithRetry(node);
        chainNodeMapper.lockChain(node.getChainId());
        parent = resolvePreviousNodeCandidate(node);
        validatePreviousNode(node, parent);
        node.setParentNodeId(parent.getId());
        fillChainDisplayFields(node, parent);
    }

    private void prepareExistingDisplayFields(DzTaskProcessChainNode existed) {
        if (existed == null || existed.getId() == null || hasChainDisplayFields(existed)) {
            return;
        }
        DzTaskProcessChainNode parent = existed.getParentNodeId() == null ? null : chainNodeMapper.selectById(existed.getParentNodeId());
        fillChainDisplayFields(existed, parent);
        fillMissingFields(chainNodeMapper.selectById(existed.getId()), existed);
    }

    private boolean hasChainDisplayFields(DzTaskProcessChainNode node) {
        return node != null
            && StrUtil.isNotBlank(node.getRootChainId())
            && node.getChainSegmentType() != null
            && (!TaskProcessChainSegmentTypeEnum.displayable(node.getChainSegmentType())
                || (node.getDisplayBizType() != null && node.getDisplayBizId() != null));
    }

    private void fillChainDisplayFields(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (node == null) {
            return;
        }
        node.setParentChainId(parent == null ? null : parent.getChainId());
        node.setRootChainId(resolveRootChainId(node, parent));
        node.setRootBizType(resolveRootBizType(node, parent));
        node.setRootBizId(resolveRootBizId(node, parent));
        node.setRootSourceType(resolveRootSourceType(node, parent));

        DisplayDecision decision = resolveDisplayDecision(node, parent);
        node.setChainSegmentType(decision.segmentType());
        node.setDisplayBizType(decision.displayBizType());
        node.setDisplayBizId(decision.displayBizId());
    }

    private String resolveRootChainId(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (parent == null) {
            return node.getChainId();
        }
        return StrUtil.isNotBlank(parent.getRootChainId()) ? parent.getRootChainId() : parent.getChainId();
    }

    private Integer resolveRootBizType(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (parent == null) {
            return node.getBizType();
        }
        return parent.getRootBizType() != null ? parent.getRootBizType() : parent.getBizType();
    }

    private Long resolveRootBizId(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (parent == null) {
            return node.getBizId();
        }
        return parent.getRootBizId() != null ? parent.getRootBizId() : parent.getBizId();
    }

    private Integer resolveRootSourceType(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (parent == null) {
            return node.getSourceType();
        }
        return parent.getRootSourceType() != null ? parent.getRootSourceType() : parent.getSourceType();
    }

    private DisplayDecision resolveDisplayDecision(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (isEmergencyTaskChildNode(node, parent)) {
            return DisplayDecision.hidden(TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode());
        }
        if (isDefRespTaskMainNode(node, parent)) {
            return DisplayDecision.of(TaskProcessChainSegmentTypeEnum.DEF_RESP_TASK_MAIN_CHAIN.getCode(),
                BIZ_TYPE_TASK, resolveTaskDisplayBizId(node));
        }
        if (isHandleMainNode(node)) {
            return DisplayDecision.of(TaskProcessChainSegmentTypeEnum.HANDLE_MAIN_CHAIN.getCode(),
                BIZ_TYPE_HANDLE, node.getBizId());
        }
        if (isTaskMainNode(node)) {
            return DisplayDecision.of(TaskProcessChainSegmentTypeEnum.MAIN_CHAIN.getCode(),
                BIZ_TYPE_TASK, resolveTaskDisplayBizId(node));
        }
        if (canInheritDisplay(parent)) {
            return DisplayDecision.of(parent.getChainSegmentType(), parent.getDisplayBizType(), parent.getDisplayBizId());
        }
        if (shouldInheritEmergencyChildSegment(node, parent)) {
            return DisplayDecision.hidden(parent.getChainSegmentType());
        }
        if (isSourcePreChainNode(node)) {
            return DisplayDecision.hidden(TaskProcessChainSegmentTypeEnum.SOURCE_PRE_CHAIN.getCode());
        }
        if (isOtherCrossChainChild(node, parent)) {
            return DisplayDecision.hidden(TaskProcessChainSegmentTypeEnum.OTHER_DERIVED_CHILD_CHAIN.getCode());
        }
        if (node.getBizType() != null && node.getBizId() != null) {
            return DisplayDecision.of(TaskProcessChainSegmentTypeEnum.MAIN_CHAIN.getCode(), node.getBizType(), node.getBizId());
        }
        return DisplayDecision.hidden(TaskProcessChainSegmentTypeEnum.UNKNOWN.getCode());
    }

    private boolean isEmergencyTaskChildNode(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (node == null) {
            return false;
        }
        boolean emergencyTask = TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(node.getSourceType())
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), node.getLinkName())
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK.getLinkName(), node.getLinkName());
        return emergencyTask && (parent == null
            || TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())
            || !Objects.equals(node.getChainId(), parent.getChainId()));
    }

    private boolean isDefRespTaskMainNode(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (node == null) {
            return false;
        }
        boolean defRespTask = TaskProcessSourceTypeEnum.DEF_RESP.getCode().equals(node.getSourceType())
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(), node.getLinkName())
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_TASK.getLinkName(), node.getLinkName());
        return defRespTask && BIZ_TYPE_TASK.equals(node.getBizType()) && resolveTaskDisplayBizId(node) != null;
    }

    private boolean isHandleMainNode(DzTaskProcessChainNode node) {
        if (node == null) {
            return false;
        }
        if (BIZ_TYPE_HANDLE.equals(node.getBizType()) && node.getBizId() != null) {
            return true;
        }
        return List.of(
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getLinkName(),
            TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName()
        ).contains(node.getLinkName());
    }

    private boolean isTaskMainNode(DzTaskProcessChainNode node) {
        if (node == null || !BIZ_TYPE_TASK.equals(node.getBizType()) || resolveTaskDisplayBizId(node) == null) {
            return false;
        }
        return !TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(node.getSourceType())
            && !TaskProcessSourceTypeEnum.DEF_RESP.getCode().equals(node.getSourceType());
    }

    private boolean canInheritDisplay(DzTaskProcessChainNode parent) {
        return parent != null
            && TaskProcessChainSegmentTypeEnum.displayable(parent.getChainSegmentType())
            && parent.getDisplayBizType() != null
            && parent.getDisplayBizId() != null;
    }

    private boolean shouldInheritEmergencyChildSegment(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        return node != null
            && parent != null
            && Objects.equals(node.getChainId(), parent.getChainId())
            && TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode().equals(parent.getChainSegmentType());
    }

    private boolean isSourcePreChainNode(DzTaskProcessChainNode node) {
        if (node == null) {
            return false;
        }
        return Set.of(
            TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getLinkName()
        ).contains(node.getLinkName());
    }

    private boolean isOtherCrossChainChild(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        return node != null && parent != null && !Objects.equals(node.getChainId(), parent.getChainId());
    }

    private Long resolveTaskDisplayBizId(DzTaskProcessChainNode node) {
        if (node == null) {
            return null;
        }
        if (node.getTaskId() != null) {
            return node.getTaskId();
        }
        if (BIZ_TYPE_TASK.equals(node.getBizType())) {
            return node.getBizId();
        }
        return null;
    }

    private DzTaskProcessChainNode queryPreviousNodeWithRetry(DzTaskProcessChainNode node) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= PREVIOUS_NODE_RETRY_COUNT; attempt++) {
            try {
                DzTaskProcessChainNode parent = resolvePreviousNodeCandidate(node);
                validatePreviousNode(node, parent);
                return parent;
            } catch (Exception e) {
                lastException = e;
                if (attempt == PREVIOUS_NODE_RETRY_COUNT) {
                    break;
                }
                log.warn("流程前序节点查询失败，等待重试, chainId={}, linkName={}, parentNodeId={}, retry={}/{}",
                    node.getChainId(), node.getLinkName(), node.getParentNodeId(), attempt + 1,
                    PREVIOUS_NODE_RETRY_COUNT, e);
                waitBeforePreviousNodeRetry();
            }
        }
        log.error("流程前序节点连续查询失败, chainId={}, linkName={}, parentNodeId={}, retries={}",
            node.getChainId(), node.getLinkName(), node.getParentNodeId(), PREVIOUS_NODE_RETRY_COUNT, lastException);
        throw new ServiceException("未找到业务流程上一步节点: " + node.getLinkName());
    }

    void waitBeforePreviousNodeRetry() {
        try {
            Thread.sleep(PREVIOUS_NODE_RETRY_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("等待流程前序节点时线程被中断");
        }
    }

    private DzTaskProcessChainNode selectLatestNodeByChainId(String chainId) {
        return chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getChainId, chainId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1")
        );
    }

    private DzTaskProcessChainNode selectSecondLatestNodeByChainId(String chainId) {
        return chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getChainId, chainId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1 offset 1")
        );
    }

    private DzTaskProcessChainNode resolvePreviousNodeCandidate(DzTaskProcessChainNode node) {
        if (node.getParentNodeId() != null) {
            return chainNodeMapper.selectById(node.getParentNodeId());
        }
        DzTaskProcessChainNode latest = selectLatestNodeByChainId(node.getChainId());
        if (latest == null) {
            return null;
        }
        try {
            validatePreviousNode(node, latest);
            return latest;
        } catch (ServiceException e) {
            DzTaskProcessChainNode secondLatest = selectSecondLatestNodeByChainId(node.getChainId());
            if (secondLatest == null) {
                throw e;
            }
            return secondLatest;
        }
    }

    private void validatePreviousNode(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        if (parent == null || parent.getId() == null || Objects.equals(parent.getDeleted(), 1)) {
            throw new ServiceException("未找到流程前序节点");
        }
        if (node.getParentNodeId() == null && !Objects.equals(parent.getChainId(), node.getChainId())) {
            throw new ServiceException("流程前序节点chainId不一致");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), node.getLinkName())
            && node.getParentNodeId() != null
            && !TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())) {
            throw new ServiceException("处置任务子链父节点必须是生成处置任务节点");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_TASK_DISPATCH.getLinkName(), node.getLinkName())
            && node.getParentNodeId() != null
            && !TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())) {
            throw new ServiceException("防御响应任务子链父节点必须是生成防御响应任务节点");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(), node.getLinkName())
            && node.getParentNodeId() != null
            && !TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())) {
            throw new ServiceException("推送处置任务父节点必须是生成处置任务节点");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(), node.getLinkName())
            && !TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())
            && !TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(), parent.getLinkName())) {
            throw new ServiceException("结束归档父节点必须是生成处置任务节点或推送处置任务节点");
        }
        Set<String> allowedPreviousLinks = allowedPreviousLinks(node.getLinkName());
        String parentLinkName = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(parent.getLinkName());
        if (!allowedPreviousLinks.isEmpty() && !allowedPreviousLinks.contains(parentLinkName)
            && !isDefRespTaskCloseFromDispatch(node, parent)
            && !isSceneHandleFeedbackFromInspecting(node, parent)) {
            throw new ServiceException("流程前序节点不符合顺序要求: " + parent.getLinkName() + " -> " + node.getLinkName());
        }
        validateTaskSpecificPreviousNode(node, parent);
    }

    private void validateTaskSpecificPreviousNode(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        validateNodeBeforeParentLookup(node);
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), node.getLinkName())
            && !Objects.equals(parent.getLinkName(), "AI险情核实任务推送")) {
            throw new ServiceException("申请技术协查前序节点必须是AI险情核实任务推送");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), node.getLinkName())
            && Objects.equals(node.getLinkName(), "AI险情核实任务反馈")
            && !Objects.equals(parent.getLinkName(), "AI险情核实任务推送")) {
            throw new ServiceException("AI险情核实任务反馈前序节点必须是AI险情核实任务推送");
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), node.getLinkName())
            && !Objects.equals(parent.getLinkName(), "技术协查任务推送")) {
            throw new ServiceException("技术协查任务反馈前序节点必须是技术协查任务推送");
        }
    }

    private void validateNodeBeforeParentLookup(DzTaskProcessChainNode node) {
        if (node != null
            && TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), node.getLinkName())
            && TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(node.getSourceType())) {
            throw new ServiceException("现场处置任务不允许关闭，请提交为已反馈");
        }
    }

    private boolean isSceneHandleFeedbackFromInspecting(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        return node != null
            && parent != null
            && TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(node.getSourceType())
            && TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), node.getLinkName())
            && TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), parent.getLinkName());
    }

    private boolean isDefRespTaskCloseFromDispatch(DzTaskProcessChainNode node, DzTaskProcessChainNode parent) {
        return node != null
            && parent != null
            && TaskProcessSourceTypeEnum.DEF_RESP.getCode().equals(node.getSourceType())
            && TaskProcessNodeCategoryEnum.TASK_CLOSE.getCode().equals(node.getNodeCategory())
            && TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), node.getLinkName())
            && TaskProcessChainNodeTextEnum.REFINED_DEF_RESP_DISPATCH.equals(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(parent.getLinkName()));
    }

    private boolean isRootNode(DzTaskProcessChainNode node) {
        if (node.getParentNodeId() != null) {
            return false;
        }
        if (isRegionDefRespRootNode(node)) {
            return true;
        }
        if (isSceneHandleReportRootNode(node)) {
            return true;
        }
        if (isStandaloneTaskFeedbackReportRootNode(node)) {
            return true;
        }
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        return Set.of(
            TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName()),
            TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName()),
            TaskProcessChainNodeTextEnum.REFINED_PATROL_TASK,
            TaskProcessChainNodeTextEnum.REFINED_MANUAL_TASK,
            TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName()),
            TaskProcessChainNodeTextEnum.REFINED_MONITOR_WARNING_TASK
        ).contains(normalized);
    }

    private boolean isRegionDefRespRootNode(DzTaskProcessChainNode node) {
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), node.getLinkName())
            && Objects.equals(node.getBizType(), TaskProcessBizTypeEnum.DEF_RESP.getCode())
            && Objects.equals(node.getSourceType(), TaskProcessSourceTypeEnum.DEF_RESP.getCode())
            && node.getBizId() != null;
    }

    private boolean isSceneHandleReportRootNode(DzTaskProcessChainNode node) {
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), node.getLinkName())
            && Objects.equals(node.getBizType(), TaskProcessBizTypeEnum.HANDLE.getCode())
            && node.getBizId() != null;
    }

    private boolean isStandaloneTaskFeedbackReportRootNode(DzTaskProcessChainNode node) {
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), node.getLinkName())
            && Objects.equals(node.getBizType(), TaskProcessBizTypeEnum.REPORT.getCode())
            && Objects.equals(node.getSourceType(), TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode())
            && node.getTaskId() == null
            && Objects.equals(node.getNodeCategory(), TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode())
            && node.getBizId() != null;
    }

    private Set<String> allowedPreviousLinks(String linkName) {
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(linkName);
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.REFINED_PUBLIC_REPORT_DISPATCH.equals(normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.REFINED_AI_VERIFY_DISPATCH.equals(normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DANGER_VERIFY_TASK.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.MONITOR_WARNING_TASK_DISPATCH.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.REFINED_MONITOR_WARNING_TASK);
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.REFINED_PATROL_TASK,
                TaskProcessChainNodeTextEnum.REFINED_MANUAL_TASK,
                TaskProcessChainNodeTextEnum.REFINED_PUBLIC_REPORT_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_AI_VERIFY_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_EMERGENCY_SURVEY_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_DEF_RESP_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_SCENE_HANDLE_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_MONITOR_WARNING_DISPATCH
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_OVERDUE_DEFAULT.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName()),
                TaskProcessChainNodeTextEnum.REFINED_PATROL_TASK,
                TaskProcessChainNodeTextEnum.REFINED_MANUAL_TASK,
                TaskProcessChainNodeTextEnum.REFINED_PUBLIC_REPORT_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_AI_VERIFY_DISPATCH,
                TaskProcessChainNodeTextEnum.REFINED_DEF_RESP_DISPATCH
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(
            TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(
            TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(
                TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(
                    TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.HANDLE_START_FROM_REPORT.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(
                    TaskProcessChainNodeTextEnum.GENERATE_EMERGENCY_INVESTIGATION_REPORT.getLinkName())
            );
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_PUSH.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_BATCH_DISPATCH.getLinkName(), normalized)) {
            return Set.of(TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName()));
        }
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.DEF_RESP_ARCHIVED.getLinkName(), normalized)) {
            return Set.of(
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName()),
                TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(TaskProcessChainNodeTextEnum.REGION_DEF_RESP_PUBLISHED.getLinkName())
            );
        }
        return Set.of();
    }

    private void fillMissingFields(DzTaskProcessChainNode existed, DzTaskProcessChainNode incoming) {
        if (existed == null || incoming == null || existed.getId() == null) {
            return;
        }
        DzTaskProcessChainNode update = new DzTaskProcessChainNode();
        update.setId(existed.getId());
        boolean changed = false;
        if (existed.getParentNodeId() == null && incoming.getParentNodeId() != null
            && !Objects.equals(existed.getId(), incoming.getParentNodeId())) {
            update.setParentNodeId(incoming.getParentNodeId());
            changed = true;
        }
        if (StrUtil.isBlank(existed.getOperatorRole()) && StrUtil.isNotBlank(incoming.getOperatorRole())) {
            update.setOperatorRole(incoming.getOperatorRole());
            changed = true;
        }
        if (existed.getNodeCategory() == null && incoming.getNodeCategory() != null) {
            update.setNodeCategory(incoming.getNodeCategory());
            changed = true;
        }
        if (existed.getStageType() == null && incoming.getStageType() != null) {
            update.setStageType(incoming.getStageType());
            changed = true;
        }
        if (StrUtil.isBlank(existed.getParentChainId()) && StrUtil.isNotBlank(incoming.getParentChainId())) {
            update.setParentChainId(incoming.getParentChainId());
            changed = true;
        }
        if (StrUtil.isBlank(existed.getRootChainId()) && StrUtil.isNotBlank(incoming.getRootChainId())) {
            update.setRootChainId(incoming.getRootChainId());
            changed = true;
        }
        if (existed.getDisplayBizType() == null && incoming.getDisplayBizType() != null) {
            update.setDisplayBizType(incoming.getDisplayBizType());
            changed = true;
        }
        if (existed.getDisplayBizId() == null && incoming.getDisplayBizId() != null) {
            update.setDisplayBizId(incoming.getDisplayBizId());
            changed = true;
        }
        if (existed.getChainSegmentType() == null && incoming.getChainSegmentType() != null) {
            update.setChainSegmentType(incoming.getChainSegmentType());
            changed = true;
        }
        if (existed.getRootBizType() == null && incoming.getRootBizType() != null) {
            update.setRootBizType(incoming.getRootBizType());
            changed = true;
        }
        if (existed.getRootBizId() == null && incoming.getRootBizId() != null) {
            update.setRootBizId(incoming.getRootBizId());
            changed = true;
        }
        if (existed.getRootSourceType() == null && incoming.getRootSourceType() != null) {
            update.setRootSourceType(incoming.getRootSourceType());
            changed = true;
        }
        if (changed) {
            update.setUpdateDate(new Date());
            chainNodeMapper.updateById(update);
        }
    }

    private String resolveOperatorRole(Long operatorId, String operatorRole) {
        if (Objects.equals(operatorId, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID)) {
            return "AI智能体";
        }
        if (StrUtil.isNotBlank(operatorRole)) {
            return operatorRole.trim();
        }
        return resolveOperatorRoleByUserId(operatorId);
    }

    private String resolveOperatorRoleByUserId(Long operatorId) {
        if (operatorId == null) {
            return null;
        }
        try {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, operatorId)
            );
            if (userRoles == null || userRoles.isEmpty()) {
                return null;
            }
            List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            if (roleIds.isEmpty()) {
                return null;
            }
            List<SysRole> roles = sysRoleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery()
                    .in(SysRole::getRoleId, roleIds)
            );
            if (roles == null || roles.isEmpty()) {
                return null;
            }
            return roles.stream()
                .map(SysRole::getRoleName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        } catch (Exception e) {
            log.warn("流程链路操作人角色自动填充失败, operatorId={}", operatorId, e);
            return null;
        }
    }

    private Integer resolveNodeCategory(Integer nodeCategory, String linkName) {
        if (nodeCategory != null) {
            TaskProcessNodeCategoryEnum resolved = TaskProcessNodeCategoryEnum.of(nodeCategory);
            return resolved == null ? null : resolved.getCode();
        }
        return TaskProcessNodeCategoryEnum.codeFromLabel(linkName);
    }

    private Integer resolveStageType(Integer stageType, String linkName, Integer bizType, Integer sourceType, DzTaskDistList task) {
        if (stageType != null) {
            TaskProcessStageTypeEnum resolved = TaskProcessStageTypeEnum.of(stageType);
            return resolved == null ? null : resolved.getCode();
        }
        TaskProcessStageTypeEnum resolved = task == null
            ? TaskProcessStageTypeEnum.fromNode(linkName, bizType, sourceType)
            : TaskProcessStageTypeEnum.fromTask(linkName, task);
        return resolved == null ? null : resolved.getCode();
    }

    @Override
    public String resolveSavedChainIdByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        DzTaskProcessChainNode current = chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getTaskId, taskId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1")
        );
        return current == null ? null : current.getChainId();
    }

    @Override
    public String resolveSavedChainIdByBiz(Integer bizType, Long bizId) {
        return resolveSavedChainIdByBizFast(bizType, bizId);
    }

    @Override
    public String resolveSavedChainIdByBizFast(Integer bizType, Long bizId) {
        DzTaskProcessChainNode current = selectSavedNodeByBiz(bizType, bizId);
        return current == null ? null : current.getChainId();
    }

    private DzTaskProcessChainNode selectSavedNodeByBiz(Integer bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return null;
        }
        return chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getBizType, bizType)
            .eq(DzTaskProcessChainNode::getBizId, bizId)
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            .last("limit 1"));
    }

    @Override
    public DzTaskProcessChainNode resolveSavedNodeByBizAndLink(Integer bizType, Long bizId, String linkName,
                                                               Integer nodeCategory) {
        if (bizType == null || bizId == null || StrUtil.isBlank(linkName)) {
            return null;
        }
        return resolveSavedNodeByBizAndLinkFast(bizType, bizId, linkName, nodeCategory);
    }

    @Override
    public DzTaskProcessChainNode resolveSavedNodeByBizAndLinkFast(Integer bizType, Long bizId, String linkName,
                                                                   Integer nodeCategory) {
        return selectSavedNodeByBizAndLink(bizType, bizId, linkName, nodeCategory);
    }

    private DzTaskProcessChainNode selectSavedNodeByBizAndLink(Integer bizType, Long bizId, String linkName,
                                                               Integer nodeCategory) {
        if (bizType == null || bizId == null || StrUtil.isBlank(linkName)) {
            return null;
        }
        return chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getBizType, bizType)
            .eq(DzTaskProcessChainNode::getBizId, bizId)
            .eq(DzTaskProcessChainNode::getLinkName, linkName.trim())
            .eq(nodeCategory != null, DzTaskProcessChainNode::getNodeCategory, nodeCategory)
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            .last("limit 1"));
    }

    @Override
    public DzTaskProcessChainNode resolveSavedNodeByBizAndCategory(Integer bizType, Long bizId, Integer nodeCategory) {
        if (bizType == null || bizId == null || nodeCategory == null) {
            return null;
        }
        return chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getBizType, bizType)
            .eq(DzTaskProcessChainNode::getBizId, bizId)
            .eq(DzTaskProcessChainNode::getNodeCategory, nodeCategory)
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            .last("limit 1"));
    }

    @Override
    public List<TaskProcessChainNodeVo> queryChainByTaskId(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        SavedChainResult savedChain = querySavedChain(taskId);
        if (!savedChain.nodes().isEmpty()) {
            return enrichProcessChain(savedChain.nodes(), savedChain.includeChildChains(),
                savedChain.limitedParentNodeId(), savedChain.onlyChildChainId(), savedChain.suppressedParentNodeId());
        }
        throw new ServiceException("任务未找到真实流程链路, taskId=" + taskId);
    }

    @Override
    public List<TaskProcessChainNodeVo> queryChainByChainId(String chainId) {
        SavedChainResult savedChain = querySavedChainByChainId(chainId);
        if (savedChain.nodes().isEmpty()) {
            throw new ServiceException("未找到真实流程链路, chainId=" + chainId);
        }
        return enrichProcessChain(savedChain.nodes(), savedChain.includeChildChains(),
            savedChain.limitedParentNodeId(), savedChain.onlyChildChainId(), savedChain.suppressedParentNodeId());
    }

    @Override
    public Map<Long, TaskProcessChainNodeVo> queryLatestNodeByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinctTaskIds = taskIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctTaskIds.isEmpty()) {
            return Map.of();
        }
        return chainNodeMapper.selectLatestByTaskIds(distinctTaskIds).stream()
            .map(this::toVo)
            .filter(node -> node.getTaskId() != null)
            .collect(Collectors.toMap(
                TaskProcessChainNodeVo::getTaskId,
                node -> node,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public Set<Long> queryLatestDisplayTaskIds() {
        Map<String, Long> latestParentTaskIds = new LinkedHashMap<>();
        List<DzTaskProcessChainNode> latestTaskNodes = chainNodeMapper.selectLatestTaskNodesByChain();
        if (latestTaskNodes != null) {
            for (DzTaskProcessChainNode node : latestTaskNodes) {
                if (node == null || StrUtil.isBlank(node.getChainId()) || node.getTaskId() == null) {
                    continue;
                }
                latestParentTaskIds.putIfAbsent(node.getChainId(), node.getTaskId());
            }
        }

        Map<String, LinkedHashSet<Long>> childTaskIdsByParentChain = new LinkedHashMap<>();
        List<DzTaskProcessChainNode> childTaskNodes = chainNodeMapper.selectEmergencyChildTaskNodesByParentChain();
        if (childTaskNodes != null) {
            for (DzTaskProcessChainNode node : childTaskNodes) {
                String parentChainId = node == null ? null : node.getParentChainId();
                if (StrUtil.isBlank(parentChainId) && node != null) {
                    parentChainId = node.getChainId();
                }
                if (node == null || StrUtil.isBlank(parentChainId) || node.getTaskId() == null
                    || !isEmergencyTaskNode(node)) {
                    continue;
                }
                childTaskIdsByParentChain
                    .computeIfAbsent(parentChainId, key -> new LinkedHashSet<>())
                    .add(node.getTaskId());
            }
        }

        if (latestParentTaskIds.isEmpty() && childTaskIdsByParentChain.isEmpty()) {
            return Set.of();
        }

        Set<Long> taskIds = new LinkedHashSet<>();
        Set<Long> emergencyChildTaskIds = new LinkedHashSet<>();
        Set<Long> hiddenParentTaskIds = new LinkedHashSet<>();
        LinkedHashSet<String> chainIds = new LinkedHashSet<>();
        chainIds.addAll(latestParentTaskIds.keySet());
        chainIds.addAll(childTaskIdsByParentChain.keySet());
        for (String chainId : chainIds) {
            LinkedHashSet<Long> childTaskIds = childTaskIdsByParentChain.get(chainId);
            if (childTaskIds != null && !childTaskIds.isEmpty()) {
                Long latestParentTaskId = latestParentTaskIds.get(chainId);
                if (latestParentTaskId != null) {
                    hiddenParentTaskIds.add(latestParentTaskId);
                }
                emergencyChildTaskIds.addAll(childTaskIds);
                taskIds.addAll(childTaskIds);
                continue;
            }
            Long latestParentTaskId = latestParentTaskIds.get(chainId);
            if (latestParentTaskId != null) {
                taskIds.add(latestParentTaskId);
            }
        }
        taskIds.removeAll(hiddenParentTaskIds);
        Set<Long> filteredTaskIds = filterDisplayTaskIdsByResolvedLatestChain(taskIds, latestParentTaskIds);
        return filterDisplayTaskIdsByCompleteBusinessChain(filteredTaskIds, emergencyChildTaskIds);
    }

    @Override
    public List<Long> queryRelatedTaskIdsByChainIds(Collection<String> chainIds) {
        if (chainIds == null || chainIds.isEmpty()) {
            return List.of();
        }
        List<String> normalizedChainIds = chainIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList();
        if (normalizedChainIds.isEmpty()) {
            return List.of();
        }
        return chainNodeMapper.selectRelatedTaskIdsByChainIds(normalizedChainIds).stream()
            .filter(Objects::nonNull)
            .toList();
    }

    private boolean isEmergencyTaskNode(DzTaskProcessChainNode node) {
        return node != null
            && (TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(node.getSourceType())
                || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName(), node.getLinkName()));
    }

    private Set<Long> filterDisplayTaskIdsByResolvedLatestChain(Set<Long> taskIds, Map<String, Long> latestParentTaskIds) {
        if (taskIds == null || taskIds.size() <= 1 || latestParentTaskIds == null || latestParentTaskIds.isEmpty()) {
            return taskIds == null ? Set.of() : taskIds;
        }
        List<DzTaskProcessChainNode> latestNodes = chainNodeMapper.selectLatestByTaskIds(new ArrayList<>(taskIds));
        if (latestNodes == null || latestNodes.isEmpty()) {
            return taskIds;
        }
        Map<Long, DzTaskProcessChainNode> latestNodeByTaskId = latestNodes.stream()
            .filter(node -> node != null && node.getTaskId() != null)
            .collect(Collectors.toMap(
                DzTaskProcessChainNode::getTaskId,
                node -> node,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
        Set<Long> filteredTaskIds = new LinkedHashSet<>();
        for (Long taskId : taskIds) {
            DzTaskProcessChainNode latestNode = latestNodeByTaskId.get(taskId);
            if (latestNode == null || StrUtil.isBlank(latestNode.getChainId())) {
                filteredTaskIds.add(taskId);
                continue;
            }
            Long latestTaskIdForResolvedChain = latestParentTaskIds.get(latestNode.getChainId());
            if (latestTaskIdForResolvedChain == null || Objects.equals(latestTaskIdForResolvedChain, taskId)) {
                filteredTaskIds.add(taskId);
            }
        }
        return filteredTaskIds;
    }

    private Set<Long> filterDisplayTaskIdsByCompleteBusinessChain(Set<Long> taskIds, Set<Long> emergencyChildTaskIds) {
        if (taskIds == null || taskIds.size() <= 1) {
            return taskIds == null ? Set.of() : taskIds;
        }
        Set<Long> allTaskIds = new LinkedHashSet<>(taskIds);
        if (emergencyChildTaskIds != null) {
            allTaskIds.addAll(emergencyChildTaskIds);
        }
        Map<Long, DzTaskDistList> taskMap = loadTaskMap(allTaskIds);
        if (taskMap.isEmpty()) {
            return taskIds;
        }

        Set<Long> filteredTaskIds = new LinkedHashSet<>();
        Map<String, CompleteChainDisplayGroup> groups = new LinkedHashMap<>();
        for (Long taskId : taskIds) {
            DzTaskDistList task = taskMap.get(taskId);
            if (isDefRespTask(task)) {
                filteredTaskIds.add(taskId);
                continue;
            }
            String groupKey = resolveCompleteTaskChainKey(taskId, task, taskMap);
            groups.computeIfAbsent(groupKey, key -> new CompleteChainDisplayGroup()).candidateTaskIds.add(taskId);
        }
        if (emergencyChildTaskIds != null) {
            for (Long taskId : emergencyChildTaskIds) {
                DzTaskDistList task = taskMap.get(taskId);
                if (task == null) {
                    continue;
                }
                String groupKey = resolveCompleteTaskChainKey(taskId, task, taskMap);
                groups.computeIfAbsent(groupKey, key -> new CompleteChainDisplayGroup()).emergencyChildTaskIds.add(taskId);
            }
        }

        for (CompleteChainDisplayGroup group : groups.values()) {
            if (!group.emergencyChildTaskIds.isEmpty()) {
                filteredTaskIds.addAll(group.emergencyChildTaskIds);
                continue;
            }
            if (!group.candidateTaskIds.isEmpty()) {
                filteredTaskIds.add(group.candidateTaskIds.iterator().next());
            }
        }
        return filteredTaskIds;
    }

    private boolean isDefRespTask(DzTaskDistList task) {
        return task != null && Objects.equals(task.getSourceType(), DzTaskDistList.SOURCE_TYPE_DEF_RESP);
    }

    private Map<Long, DzTaskDistList> loadTaskMap(Set<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        List<DzTaskDistList> tasks = taskDistListMapper.selectBatchIds(taskIds);
        if (tasks == null || tasks.isEmpty()) {
            return Map.of();
        }
        return tasks.stream()
            .filter(task -> task != null && task.getId() != null)
            .collect(Collectors.toMap(
                DzTaskDistList::getId,
                task -> task,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
    }

    private String resolveCompleteTaskChainKey(Long taskId, DzTaskDistList task, Map<Long, DzTaskDistList> taskMap) {
        if (task == null) {
            return "TASK-" + taskId;
        }
        if (task.getHandleId() != null) {
            return "HANDLE-" + task.getHandleId();
        }
        if (task.getReportId() != null) {
            return "REPORT-" + task.getReportId();
        }
        Long rootTaskId = resolveRelatedRootTaskId(task, taskMap);
        if (rootTaskId != null) {
            return "ROOT-TASK-" + rootTaskId;
        }
        return "TASK-" + task.getId();
    }

    private Long resolveRelatedRootTaskId(DzTaskDistList task, Map<Long, DzTaskDistList> taskMap) {
        if (task == null || task.getId() == null || taskMap == null || taskMap.isEmpty()) {
            return task == null ? null : task.getId();
        }
        Long rootTaskId = task.getId();
        Long relatedTaskId = task.getRelatedTaskId();
        Set<Long> visited = new LinkedHashSet<>();
        visited.add(rootTaskId);
        while (relatedTaskId != null && visited.add(relatedTaskId)) {
            rootTaskId = relatedTaskId;
            DzTaskDistList parentTask = taskMap.get(relatedTaskId);
            relatedTaskId = parentTask == null ? null : parentTask.getRelatedTaskId();
        }
        return rootTaskId;
    }

    private static class CompleteChainDisplayGroup {
        private final LinkedHashSet<Long> candidateTaskIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> emergencyChildTaskIds = new LinkedHashSet<>();
    }

    private SavedChainResult querySavedChain(Long taskId) {
        DzTaskProcessChainNode current = chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getTaskId, taskId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1")
        );
        if (current == null || StrUtil.isBlank(current.getChainId())) {
            return SavedChainResult.empty();
        }
        return querySavedChainByChainId(current.getChainId());
    }

    private SavedChainResult querySavedChainByChainId(String chainId) {
        if (StrUtil.isBlank(chainId)) {
            return SavedChainResult.empty();
        }
        chainId = chainId.trim();
        DzTaskProcessChainNode chainStart = chainNodeMapper.selectOne(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getChainId, chainId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByAsc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1")
        );
        if (chainStart == null) {
            return SavedChainResult.empty();
        }
        if (chainStart != null && chainStart.getParentNodeId() != null) {
            DzTaskProcessChainNode parent = chainNodeMapper.selectOne(
                Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                    .eq(DzTaskProcessChainNode::getId, chainStart.getParentNodeId())
                    .eq(DzTaskProcessChainNode::getDeleted, 0)
                    .last("limit 1")
            );
            if (parent != null && StrUtil.isNotBlank(parent.getChainId())
                && !Objects.equals(parent.getChainId(), chainId)) {
                if (TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())) {
                    return new SavedChainResult(toVoList(flattenChildChainAfterParent(parent.getChainId(), chainId, parent.getId())),
                        true, null, null, parent.getId());
                }
                if (TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode().equals(parent.getNodeCategory())) {
                    return new SavedChainResult(toVoList(flattenChildChainAfterParent(parent.getChainId(), chainId, parent.getId())),
                        true, null, null, parent.getId());
                }
                return new SavedChainResult(toVoList(queryChainNodes(parent.getChainId())), true,
                    parent.getId(), chainId, null);
            }
        }
        return new SavedChainResult(toVoList(queryChainNodes(chainId)), true, null, null, null);
    }

    private List<TaskProcessChainNodeVo> toVoList(List<DzTaskProcessChainNode> nodes) {
        return nodes.stream().map(this::toVo).toList();
    }

    private record SavedChainResult(List<TaskProcessChainNodeVo> nodes, boolean includeChildChains,
                                    Long limitedParentNodeId, String onlyChildChainId,
                                    Long suppressedParentNodeId) {
        private static SavedChainResult empty() {
            return new SavedChainResult(List.of(), true, null, null, null);
        }
    }

    private List<DzTaskProcessChainNode> flattenChildChainAfterParent(String parentChainId, String childChainId,
                                                                      Long parentNodeId) {
        List<DzTaskProcessChainNode> parentNodes = queryChainNodes(parentChainId);
        List<DzTaskProcessChainNode> childNodes = queryChainNodes(childChainId);
        if (parentNodes.isEmpty()) {
            return childNodes;
        }
        if (childNodes.isEmpty()) {
            return parentNodes;
        }
        List<DzTaskProcessChainNode> merged = new ArrayList<>(parentNodes.size() + childNodes.size());
        boolean appended = false;
        for (DzTaskProcessChainNode node : parentNodes) {
            merged.add(node);
            if (!appended && Objects.equals(node.getId(), parentNodeId)) {
                merged.addAll(childNodes);
                appended = true;
            }
        }
        if (!appended) {
            merged.addAll(childNodes);
        }
        return merged;
    }

    private List<DzTaskProcessChainNode> queryChainNodes(String chainId) {
        if (StrUtil.isBlank(chainId)) {
            return List.of();
        }
        return chainNodeMapper.selectList(
            Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getChainId, chainId)
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByAsc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
        );
    }

    private List<TaskProcessChainNodeVo> enrichProcessChain(List<TaskProcessChainNodeVo> nodes, boolean includeChildChains) {
        return enrichProcessChain(nodes, includeChildChains, null, null, null);
    }

    private List<TaskProcessChainNodeVo> enrichProcessChain(List<TaskProcessChainNodeVo> nodes, boolean includeChildChains,
                                                            Long limitedParentNodeId, String onlyChildChainId,
                                                            Long suppressedParentNodeId) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        enrichTaskFields(nodes);
        if (includeChildChains) {
            enrichChildChains(nodes, limitedParentNodeId, onlyChildChainId, suppressedParentNodeId);
        }
        return nodes;
    }

    private void enrichTaskFields(List<TaskProcessChainNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Set<Long> taskIds = nodes.stream()
            .map(this::resolveVoTaskId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return;
        }
        Map<Long, DzTaskDistList> taskMap = taskDistListMapper.selectBatchIds(taskIds).stream()
            .filter(task -> task != null && task.getId() != null)
            .collect(Collectors.toMap(DzTaskDistList::getId, task -> task, (left, right) -> left, LinkedHashMap::new));

        for (TaskProcessChainNodeVo node : nodes) {
            Long taskId = resolveVoTaskId(node);
            if (taskId == null) {
                continue;
            }
            DzTaskDistList task = taskMap.get(taskId);
            if (task == null) {
                continue;
            }
            node.setTaskId(taskId);
            node.setTaskStatus(task.getStatus());
            if (node.getSourceTypeCode() == null && task.getSourceType() != null) {
                applySourceType(node, TaskProcessSourceTypeEnum.fromTaskSourceType(task.getSourceType()).getCode());
            }
            if (node.getStageType() == null) {
                applyStageType(node, TaskProcessStageTypeEnum.fromTask(node.getLinkName(), task));
            }
        }
    }

    private Long resolveVoTaskId(TaskProcessChainNodeVo node) {
        if (node == null) {
            return null;
        }
        if (node.getTaskId() != null) {
            return node.getTaskId();
        }
        if (BIZ_TYPE_TASK.equals(node.getBizType()) || BIZ_TYPE_TASK.equals(node.getType())) {
            return node.getBizId();
        }
        return null;
    }

    private void enrichChildChains(List<TaskProcessChainNodeVo> nodes, Long limitedParentNodeId,
                                   String onlyChildChainId, Long suppressedParentNodeId) {
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null || node.getNodeId() == null || !isBatchDispatchNode(node.getNodeCategory())) {
                continue;
            }
            if (Objects.equals(node.getNodeId(), suppressedParentNodeId)) {
                continue;
            }
            List<DzTaskProcessChainNode> childStartNodes = chainNodeMapper.selectList(
                Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                    .eq(DzTaskProcessChainNode::getParentNodeId, node.getNodeId())
                    .eq(DzTaskProcessChainNode::getDeleted, 0)
                    .orderByAsc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            );
            if (childStartNodes.isEmpty()) {
                continue;
            }
            List<List<TaskProcessChainNodeVo>> childChains = new ArrayList<>();
            String currentOnlyChildChainId = Objects.equals(node.getNodeId(), limitedParentNodeId) ? onlyChildChainId : null;
            Map<String, DzTaskProcessChainNode> childStartByChainId = childStartNodes.stream()
                .filter(childStart -> StrUtil.isNotBlank(childStart.getChainId()))
                .filter(childStart -> !Objects.equals(childStart.getChainId(), node.getChainId()))
                .filter(childStart -> StrUtil.isBlank(currentOnlyChildChainId)
                    || Objects.equals(childStart.getChainId(), currentOnlyChildChainId))
                .collect(Collectors.toMap(
                    DzTaskProcessChainNode::getChainId,
                    childStart -> childStart,
                    (left, right) -> left,
                    LinkedHashMap::new
                ));
            for (String childChainId : childStartByChainId.keySet()) {
                List<TaskProcessChainNodeVo> childChain = queryChainNodes(childChainId)
                    .stream()
                    .map(this::toVo)
                    .toList();
                if (!childChain.isEmpty()) {
                    childChains.add(enrichProcessChain(childChain, true));
                }
            }
            node.setChildChains(childChains);
        }
    }

    private boolean isBatchDispatchNode(Integer nodeCategory) {
        return TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(nodeCategory)
            || TaskProcessNodeCategoryEnum.DEF_RESP_BATCH_DISPATCH.getCode().equals(nodeCategory);
    }

    private TaskProcessChainNodeVo toVo(DzTaskProcessChainNode node) {
        TaskProcessChainNodeVo vo = new TaskProcessChainNodeVo();
        vo.setNodeId(node.getId());
        vo.setChainId(node.getChainId());
        vo.setParentChainId(node.getParentChainId());
        vo.setParentNodeId(node.getParentNodeId());
        vo.setLinkName(normalizeLinkName(node.getLinkName()));
        vo.setTriggerReason(node.getTriggerReason());
        vo.setOperatorId(node.getOperatorId());
        vo.setOperatorName(node.getOperatorName());
        vo.setOperatorRole(node.getOperatorRole());
        vo.setTriggerTime(node.getTriggerTime());
        applyBizType(vo, node.getBizType());
        vo.setBizId(node.getBizId());
        vo.setTaskId(node.getTaskId());
        applySourceType(vo, node.getSourceType());
        vo.setNodeCategory(node.getNodeCategory());
        vo.setNodeCategoryLabel(TaskProcessNodeCategoryEnum.label(node.getNodeCategory()));
        applyStageType(vo, node.getStageType());
        vo.setRootChainId(node.getRootChainId());
        vo.setRootChainClosed(node.getRootChainClosed());
        vo.setDisplayBizType(node.getDisplayBizType());
        vo.setDisplayBizId(node.getDisplayBizId());
        vo.setChainSegmentType(node.getChainSegmentType());
        vo.setChainSegmentTypeLabel(TaskProcessChainSegmentTypeEnum.label(node.getChainSegmentType()));
        vo.setRootBizType(node.getRootBizType());
        vo.setRootBizId(node.getRootBizId());
        vo.setRootSourceType(node.getRootSourceType());
        vo.setCreateDate(node.getCreateDate());
        vo.setUpdateDate(node.getUpdateDate());
        return vo;
    }

    private void applyBizType(TaskProcessChainNodeVo vo, Integer bizType) {
        vo.setBizType(bizType);
        vo.setBizTypeLabel(TaskProcessBizTypeEnum.label(bizType));
        vo.setType(bizType);
    }

    private String normalizeLinkName(String linkName) {
        return TaskProcessChainNodeTextEnum.normalizeDisplayLinkName(linkName);
    }

    private void applySourceType(TaskProcessChainNodeVo vo, Integer sourceType) {
        vo.setSourceTypeCode(sourceType);
        vo.setSourceTypeLabel(TaskProcessSourceTypeEnum.label(sourceType));
        vo.setSourceType(sourceType);
    }

    private void applyStageType(TaskProcessChainNodeVo vo, Integer stageType) {
        vo.setStageType(stageType);
        vo.setStageTypeLabel(TaskProcessStageTypeEnum.label(stageType));
    }

    private void applyStageType(TaskProcessChainNodeVo vo, TaskProcessStageTypeEnum stageType) {
        if (stageType == null) {
            return;
        }
        applyStageType(vo, stageType.getCode());
    }

    private record DisplayDecision(Integer segmentType, Integer displayBizType, Long displayBizId) {

        private static DisplayDecision of(Integer segmentType, Integer displayBizType, Long displayBizId) {
            return new DisplayDecision(segmentType, displayBizType, displayBizId);
        }

        private static DisplayDecision hidden(Integer segmentType) {
            return new DisplayDecision(segmentType, null, null);
        }
    }

}
