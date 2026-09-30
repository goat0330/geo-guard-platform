/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainSegmentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessDisplayStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessRiskLevelSourceEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainFilterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainSummaryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainSummary;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainNodeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskProcessChainSummaryMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import org.dromara.system.domain.vo.SysUserVo;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务流程链路最新节点汇总 Service 实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskProcessChainSummaryServiceImpl implements IDzTaskProcessChainSummaryService {

    private static final List<Integer> DEFAULT_DISPLAY_SEGMENTS = List.of(
        TaskProcessChainSegmentTypeEnum.MAIN_CHAIN.getCode(),
        TaskProcessChainSegmentTypeEnum.DEF_RESP_TASK_MAIN_CHAIN.getCode(),
        TaskProcessChainSegmentTypeEnum.HANDLE_MAIN_CHAIN.getCode()
    );

    private static final Map<String, String> SORT_COLUMN_MAP = Map.ofEntries(
        Map.entry("id", "display_biz_id"),
        Map.entry("summaryId", "id"),
        Map.entry("summary_id", "id"),
        Map.entry("nodeId", "node_id"),
        Map.entry("node_id", "node_id"),
        Map.entry("displayBizType", "display_biz_type"),
        Map.entry("display_biz_type", "display_biz_type"),
        Map.entry("displayBizId", "display_biz_id"),
        Map.entry("display_biz_id", "display_biz_id"),
        Map.entry("rootChainClosed", "root_chain_closed"),
        Map.entry("root_chain_closed", "root_chain_closed"),
        Map.entry("rootSourceType", "root_source_type"),
        Map.entry("root_source_type", "root_source_type"),
        Map.entry("chainSegmentType", "chain_segment_type"),
        Map.entry("chain_segment_type", "chain_segment_type"),
        Map.entry("linkName", "link_name"),
        Map.entry("link_name", "link_name"),
        Map.entry("currentStatus", "current_status"),
        Map.entry("current_status", "current_status"),
        Map.entry("pendingPushTaskCount", "pending_push_task_count"),
        Map.entry("pending_push_task_count", "pending_push_task_count"),
        Map.entry("riskLevel", "risk_level"),
        Map.entry("risk_level", "risk_level"),
        Map.entry("dynamicRiskLevel", "dynamic_risk_level"),
        Map.entry("dynamic_risk_level", "dynamic_risk_level"),
        Map.entry("unitId", "unit_id"),
        Map.entry("unit_id", "unit_id"),
        Map.entry("county", "county"),
        Map.entry("street", "street"),
        Map.entry("village", "village"),
        Map.entry("pilotArea1", "pilot_area_1"),
        Map.entry("pilot_area_1", "pilot_area_1"),
        Map.entry("pilotArea2", "pilot_area_2"),
        Map.entry("pilot_area_2", "pilot_area_2"),
        Map.entry("responsiblePerson", "responsible_person"),
        Map.entry("responsible_person", "responsible_person"),
        Map.entry("responsiblePersonPhone", "responsible_person_phone"),
        Map.entry("responsible_person_phone", "responsible_person_phone"),
        Map.entry("createDate", "create_date"),
        Map.entry("create_date", "create_date"),
        Map.entry("updateDate", "update_date"),
        Map.entry("update_date", "update_date"),
        Map.entry("triggerTime", "trigger_time"),
        Map.entry("trigger_time", "trigger_time")
    );

    private final DzTaskProcessChainSummaryMapper summaryMapper;
    private final DzTaskProcessChainNodeMapper chainNodeMapper;
    private final DzTaskDistListMapper taskDistListMapper;
    private final DzReportDisasterMapper reportDisasterMapper;
    private final DzTaskHandleMapper taskHandleMapper;
    private final DzDefRespPlanMapper defRespPlanMapper;
    private final DzRiskAssessmentMapper riskAssessmentMapper;
    private final ISlopeUnitService slopeUnitService;
    private final IDzUserAdRegionService userAdRegionService;

    @Override
    public TableDataInfo<TaskProcessChainSummaryVo> queryPageList(TaskProcessChainSummaryBo bo, PageQuery pageQuery) {
        TaskProcessChainSummaryBo query = bo == null ? new TaskProcessChainSummaryBo() : bo;

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(query);
        appendCurrentUserAdRegionPermission(wrapper);
        appendOrderBy(wrapper, query);

        Page<DzTaskProcessChainSummary> page = summaryMapper.selectPage(pageQuery.build(), wrapper);
        List<TaskProcessChainSummaryVo> records = page.getRecords().stream()
            .map(this::toVo)
            .toList();
        return new TableDataInfo<>(records, page.getTotal());
    }

    @Override
    public Set<String> queryChainIdsByFilter(TaskProcessChainFilterBo bo) {
        TaskProcessChainSummaryBo query = toSummaryBo(bo);

        QueryWrapper<DzTaskProcessChainSummary> wrapper = buildQueryWrapper(query);
        appendCurrentUserAdRegionPermission(wrapper);
        wrapper.select("DISTINCT chain_id");

        return summaryMapper.selectObjs(wrapper).stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshByNode(DzTaskProcessChainNode node) {
        DzTaskProcessChainNode saved = resolveSavedNode(node);
        if (saved == null) {
            return;
        }
        for (DisplayKey displayKey : resolveAffectedDisplayKeys(saved)) {
            refreshByDisplayKey(displayKey);
        }
        refreshRootChainClosed(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshReportRiskLevelIfCurrent(Long reportId) {
        if (reportId == null) {
            return;
        }
        DzReportDisaster report = reportDisasterMapper.selectById(reportId);
        if (report == null || !hasReportRiskLevel(report)) {
            return;
        }
        for (DisplayKey displayKey : loadAffectedDisplayKeysByReport(report)) {
            refreshByDisplayKeyIfCurrentReport(displayKey, reportId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDynamicRiskLevelByRiskAssessment(Long riskId) {
        if (riskId == null) {
            return;
        }
        DzRiskAssessment riskAssessment = riskAssessmentMapper.selectById(riskId);
        refreshDynamicRiskLevelByRiskAndUnit(riskId, riskAssessment == null ? null : riskAssessment.getSlopeUnitId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDynamicRiskLevelByUnitId(String unitId) {
        refreshDynamicRiskLevelByRiskAndUnit(null, unitId);
    }

    private QueryWrapper<DzTaskProcessChainSummary> buildQueryWrapper(TaskProcessChainSummaryBo bo) {
        QueryWrapper<DzTaskProcessChainSummary> wrapper = Wrappers.query();
        wrapper.eq("deleted", 0);
        List<Integer> segments = bo.getChainSegmentTypes();
        if (segments == null || segments.isEmpty()) {
            if (bo.getChainSegmentType() == null) {
                wrapper.in("chain_segment_type", DEFAULT_DISPLAY_SEGMENTS);
            } else {
                wrapper.eq("chain_segment_type", bo.getChainSegmentType());
            }
        } else {
            wrapper.in("chain_segment_type", segments.stream().filter(Objects::nonNull).distinct().toList());
        }
        wrapper.eq(bo.getId() != null, "display_biz_id", bo.getId());
        wrapper.eq(bo.getNodeId() != null, "node_id", bo.getNodeId());
        appendMainChainFilter(wrapper, bo);
        wrapper.like(StringUtils.isNotBlank(bo.getChainId()), "chain_id", bo.getChainId());
        wrapper.eq(StringUtils.isNotBlank(bo.getRootChainId()), "root_chain_id", bo.getRootChainId());
        wrapper.eq(bo.getRootChainClosed() != null, "root_chain_closed", bo.getRootChainClosed());
        wrapper.eq(bo.getDisplayBizType() != null, "display_biz_type", bo.getDisplayBizType());
        wrapper.eq(bo.getDisplayBizId() != null, "display_biz_id", bo.getDisplayBizId());
        wrapper.eq(bo.getRootBizType() != null, "root_biz_type", bo.getRootBizType());
        wrapper.eq(bo.getRootBizId() != null, "root_biz_id", bo.getRootBizId());
        wrapper.eq(bo.getRootSourceType() != null, "root_source_type", bo.getRootSourceType());
        wrapper.eq(StringUtils.isNotBlank(bo.getLinkName()), "link_name", bo.getLinkName());
        appendCurrentStatusFilter(wrapper, bo);
        appendRiskFilter(wrapper, bo);
        wrapper.eq(bo.getRiskLevelSource() != null, "risk_level_source", bo.getRiskLevelSource());
        wrapper.like(StringUtils.isNotBlank(bo.getUnitId()), "unit_id", bo.getUnitId());
        wrapper.eq(StringUtils.isNotBlank(bo.getCounty()), "county", bo.getCounty());
        wrapper.eq(StringUtils.isNotBlank(bo.getStreet()), "street", bo.getStreet());
        wrapper.eq(StringUtils.isNotBlank(bo.getVillage()), "village", bo.getVillage());
        wrapper.eq(bo.getPilotArea1() != null, "pilot_area_1", bo.getPilotArea1());
        wrapper.eq(bo.getPilotArea2() != null, "pilot_area_2", bo.getPilotArea2());
        wrapper.like(StringUtils.isNotBlank(bo.getResponsiblePerson()), "responsible_person", bo.getResponsiblePerson());
        wrapper.like(StringUtils.isNotBlank(bo.getResponsiblePersonPhone()), "responsible_person_phone", bo.getResponsiblePersonPhone());
        appendSourceTypeFilter(wrapper, bo);
        wrapper.eq(bo.getPendingPushTaskCount() != null, "pending_push_task_count", bo.getPendingPushTaskCount());
        wrapper.eq(bo.getDefId() != null, "def_id", bo.getDefId());
        wrapper.eq(bo.getRiskId() != null, "risk_id", bo.getRiskId());
        appendDateFilter(wrapper, "create_date", bo.getCreateDate());
        appendDateFilter(wrapper, "update_date", bo.getUpdateDate());
        appendDateFilter(wrapper, "trigger_time", bo.getTriggerTime());
        return wrapper;
    }

    private void appendMainChainFilter(QueryWrapper<DzTaskProcessChainSummary> wrapper, TaskProcessChainSummaryBo bo) {
        LinkedHashSet<String> chainIds = null;
        if (bo.getTaskId() != null) {
            chainIds = normalizeChainIds(chainNodeMapper.selectMainChainIdsByTaskId(bo.getTaskId()));
        }
        if (bo.getHandleId() != null) {
            LinkedHashSet<String> handleChainIds = normalizeChainIds(chainNodeMapper.selectMainChainIdsByHandleId(
                bo.getHandleId(),
                TaskProcessBizTypeEnum.HANDLE.getCode()
            ));
            if (chainIds == null) {
                chainIds = handleChainIds;
            } else {
                chainIds.retainAll(handleChainIds);
            }
        }
        if (chainIds == null) {
            return;
        }
        if (chainIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in("chain_id", chainIds);
    }

    private LinkedHashSet<String> normalizeChainIds(List<String> chainIds) {
        if (chainIds == null || chainIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return chainIds.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TaskProcessChainSummaryBo toSummaryBo(TaskProcessChainFilterBo bo) {
        TaskProcessChainSummaryBo query = new TaskProcessChainSummaryBo();
        if (bo == null) {
            return query;
        }
        BeanUtils.copyProperties(bo, query);
        return query;
    }

    private void appendRiskFilter(QueryWrapper<DzTaskProcessChainSummary> wrapper, TaskProcessChainSummaryBo bo) {
        List<Integer> riskLevels = Stream.concat(
                Stream.of(bo.getRiskLevel()),
                bo.getRiskLevels() == null ? Stream.empty() : bo.getRiskLevels().stream()
            )
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!riskLevels.isEmpty()) {
            wrapper.in("risk_level", riskLevels);
        }
        List<Integer> dynamicRiskLevels = Stream.concat(
                Stream.of(bo.getDynamicRiskLevel()),
                bo.getDynamicRiskLevels() == null ? Stream.empty() : bo.getDynamicRiskLevels().stream()
            )
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!dynamicRiskLevels.isEmpty()) {
            wrapper.in("dynamic_risk_level", dynamicRiskLevels);
        }
    }

    private void appendCurrentStatusFilter(QueryWrapper<DzTaskProcessChainSummary> wrapper, TaskProcessChainSummaryBo bo) {
        List<Integer> currentStatuses = Stream.of(
                Stream.of(bo.getCurrentStatus()),
                bo.getCurrentStatusList() == null ? Stream.<Integer>empty() : bo.getCurrentStatusList().stream()
            )
            .flatMap(item -> item)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!currentStatuses.isEmpty()) {
            wrapper.in("current_status", currentStatuses);
        }
    }

    private void appendSourceTypeFilter(QueryWrapper<DzTaskProcessChainSummary> wrapper, TaskProcessChainSummaryBo bo) {
        LinkedHashSet<Integer> sourceTypes = new LinkedHashSet<>();
        if (bo.getSourceType() != null) {
            sourceTypes.add(bo.getSourceType());
        }
        if (bo.getSourceTypeList() != null) {
            bo.getSourceTypeList().stream().filter(Objects::nonNull).forEach(sourceTypes::add);
        }
        if (sourceTypes.isEmpty()) {
            return;
        }
        wrapper.in("source_type", sourceTypes);
    }

    private void appendDateFilter(QueryWrapper<DzTaskProcessChainSummary> wrapper, String column, Date value) {
        if (value == null) {
            return;
        }
        wrapper.ge(column, DateUtil.beginOfDay(value));
        wrapper.le(column, DateUtil.endOfDay(value));
    }

    private void appendOrderBy(QueryWrapper<DzTaskProcessChainSummary> wrapper, TaskProcessChainSummaryBo bo) {
        Set<String> ordered = new LinkedHashSet<>();
        appendOrderBy(wrapper, bo.getAsc(), true, ordered);
        appendOrderBy(wrapper, bo.getDesc(), false, ordered);
        if (!ordered.contains("update_date")) {
            wrapper.orderByDesc("update_date");
        }
        if (!ordered.contains("node_id")) {
            wrapper.orderByDesc("node_id");
        }
    }

    private void appendOrderBy(QueryWrapper<DzTaskProcessChainSummary> wrapper, List<String> fields, boolean asc,
                               Set<String> ordered) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (String field : fields) {
            if (StringUtils.isBlank(field)) {
                continue;
            }
            String column = SORT_COLUMN_MAP.get(field.trim());
            if (column == null || !ordered.add(column)) {
                continue;
            }
            if (asc) {
                wrapper.orderByAsc(column);
            } else {
                wrapper.orderByDesc(column);
            }
        }
    }

    private void appendCurrentUserAdRegionPermission(QueryWrapper<DzTaskProcessChainSummary> wrapper) {
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegions();
        if (userAdRegions == null) {
            return;
        }
        List<DzUserAdRegionVo> effectiveRegions = userAdRegions.stream()
            .filter(item -> StringUtils.isNotBlank(resolveRegionColumn(item.getAdRegionLevel()))
                && StringUtils.isNotBlank(item.getAdRegionId()))
            .toList();
        if (effectiveRegions.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(w -> {
            boolean first = true;
            for (DzUserAdRegionVo item : effectiveRegions) {
                String column = resolveRegionColumn(item.getAdRegionLevel());
                if (StringUtils.isBlank(column)) {
                    continue;
                }
                if (!first) {
                    w.or();
                }
                w.apply("EXISTS (SELECT 1 FROM data_slope_unit slope WHERE slope.id = unit_id AND slope." + column + " = {0})",
                    item.getAdRegionId());
                first = false;
            }
        });
    }

    private List<DzUserAdRegionVo> getCurrentUserAdRegions() {
        if (LoginHelper.isSuperAdmin()) {
            return null;
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }
        List<DzUserAdRegionVo> userAdRegions = userAdRegionService.queryEffectiveListByUserId(userId).stream()
            .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
            .toList();
        if (userAdRegions.isEmpty()) {
            throw new ServiceException("行政区划权限不足：当前用户未配置关联行政区划，无法访问任务");
        }
        return userAdRegions;
    }

    private String resolveRegionColumn(Integer level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 1 -> "province_code";
            case 2 -> "city_code";
            case 3 -> "county_code";
            case 4 -> "street_code";
            case 5 -> "village_code";
            default -> null;
        };
    }

    private DzTaskProcessChainNode resolveSavedNode(DzTaskProcessChainNode node) {
        if (node == null) {
            return null;
        }
        if (node.getId() != null) {
            DzTaskProcessChainNode saved = chainNodeMapper.selectById(node.getId());
            if (saved != null) {
                return saved;
            }
        }
        if (node.getDisplayBizType() != null && node.getDisplayBizId() != null) {
            return chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
                .eq(DzTaskProcessChainNode::getDisplayBizType, node.getDisplayBizType())
                .eq(DzTaskProcessChainNode::getDisplayBizId, node.getDisplayBizId())
                .eq(DzTaskProcessChainNode::getDeleted, 0)
                .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
                .last("limit 1"));
        }
        return node;
    }

    private Set<DisplayKey> resolveAffectedDisplayKeys(DzTaskProcessChainNode node) {
        LinkedHashSet<DisplayKey> keys = new LinkedHashSet<>();
        if (TaskProcessChainSegmentTypeEnum.displayable(node.getChainSegmentType())
            && node.getDisplayBizType() != null
            && node.getDisplayBizId() != null) {
            keys.add(new DisplayKey(node.getDisplayBizType(), node.getDisplayBizId()));
        }
        if (TaskProcessChainSegmentTypeEnum.EMERGENCY_TASK_CHILD_CHAIN.getCode().equals(node.getChainSegmentType())) {
            DzTaskProcessChainNode batchNode = resolveEmergencyBatchNode(node);
            if (batchNode != null && batchNode.getDisplayBizType() != null && batchNode.getDisplayBizId() != null) {
                keys.add(new DisplayKey(batchNode.getDisplayBizType(), batchNode.getDisplayBizId()));
            }
        }
        return keys;
    }

    private DzTaskProcessChainNode resolveEmergencyBatchNode(DzTaskProcessChainNode node) {
        DzTaskProcessChainNode candidate = node.getParentNodeId() == null ? null : chainNodeMapper.selectById(node.getParentNodeId());
        if (isEmergencyBatchNode(candidate)) {
            return candidate;
        }
        DzTaskProcessChainNode chainStart = chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getChainId, node.getChainId())
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByAsc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            .last("limit 1"));
        if (chainStart == null || chainStart.getParentNodeId() == null) {
            return null;
        }
        candidate = chainNodeMapper.selectById(chainStart.getParentNodeId());
        return isEmergencyBatchNode(candidate) ? candidate : null;
    }

    private boolean isEmergencyBatchNode(DzTaskProcessChainNode node) {
        return node != null
            && TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(node.getNodeCategory());
    }

    private Set<DisplayKey> loadAffectedDisplayKeysByReport(DzReportDisaster report) {
        LinkedHashSet<DisplayKey> keys = new LinkedHashSet<>();
        if (report == null || report.getId() == null) {
            return keys;
        }
        List<Long> taskIds = loadReportRelatedTaskIds(report);
        addDisplayKeys(keys, summaryMapper.selectList(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
            .eq(DzTaskProcessChainSummary::getDeleted, 0)
            .and(w -> w
                .eq(DzTaskProcessChainSummary::getDisplayBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                .eq(DzTaskProcessChainSummary::getDisplayBizId, report.getId())
                .or()
                .eq(DzTaskProcessChainSummary::getBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                .eq(DzTaskProcessChainSummary::getBizId, report.getId())
                .or()
                .eq(DzTaskProcessChainSummary::getRootBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                .eq(DzTaskProcessChainSummary::getRootBizId, report.getId()))));
        if (!taskIds.isEmpty()) {
            addDisplayKeys(keys, summaryMapper.selectList(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
                .eq(DzTaskProcessChainSummary::getDeleted, 0)
                .and(w -> w.in(DzTaskProcessChainSummary::getTaskId, taskIds)
                    .or(inner -> inner.eq(DzTaskProcessChainSummary::getDisplayBizType, TaskProcessBizTypeEnum.TASK.getCode())
                        .in(DzTaskProcessChainSummary::getDisplayBizId, taskIds)))));
        }

        List<DzTaskProcessChainNode> reportNodes = loadReportRelatedNodes(report, taskIds);
        for (DzTaskProcessChainNode node : reportNodes) {
            keys.addAll(resolveAffectedDisplayKeys(node));
        }
        addDisplayKeysByReportChains(keys, reportNodes);
        return keys;
    }

    private List<Long> loadReportRelatedTaskIds(DzReportDisaster report) {
        LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
        if (report == null || report.getId() == null) {
            return List.of();
        }
        if (report.getTaskId() != null) {
            taskIds.add(report.getTaskId());
        }
        List<DzTaskDistList> linkedTasks = taskDistListMapper.selectList(Wrappers.<DzTaskDistList>lambdaQuery()
            .eq(DzTaskDistList::getDelete, 0)
            .eq(DzTaskDistList::getReportId, report.getId()));
        if (linkedTasks != null) {
            linkedTasks.stream()
                .map(DzTaskDistList::getId)
                .filter(Objects::nonNull)
                .forEach(taskIds::add);
        }
        return taskIds.stream().toList();
    }

    private List<DzTaskProcessChainNode> loadReportRelatedNodes(DzReportDisaster report, List<Long> taskIds) {
        if (report == null || report.getId() == null) {
            return List.of();
        }
        return chainNodeMapper.selectList(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .and(w -> {
                w.eq(DzTaskProcessChainNode::getBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                    .eq(DzTaskProcessChainNode::getBizId, report.getId())
                    .or()
                    .eq(DzTaskProcessChainNode::getDisplayBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                    .eq(DzTaskProcessChainNode::getDisplayBizId, report.getId())
                    .or()
                    .eq(DzTaskProcessChainNode::getRootBizType, TaskProcessBizTypeEnum.REPORT.getCode())
                    .eq(DzTaskProcessChainNode::getRootBizId, report.getId());
                if (taskIds != null && !taskIds.isEmpty()) {
                    w.or()
                        .in(DzTaskProcessChainNode::getTaskId, taskIds)
                        .or(inner -> inner.eq(DzTaskProcessChainNode::getDisplayBizType, TaskProcessBizTypeEnum.TASK.getCode())
                            .in(DzTaskProcessChainNode::getDisplayBizId, taskIds));
                }
            }));
    }

    private void addDisplayKeysByReportChains(Set<DisplayKey> keys, List<DzTaskProcessChainNode> reportNodes) {
        if (keys == null || reportNodes == null || reportNodes.isEmpty()) {
            return;
        }
        LinkedHashSet<String> chainIds = reportNodes.stream()
            .flatMap(node -> Stream.of(node.getChainId(), node.getRootChainId()))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (chainIds.isEmpty()) {
            return;
        }
        addDisplayKeys(keys, summaryMapper.selectList(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
            .eq(DzTaskProcessChainSummary::getDeleted, 0)
            .and(w -> w.in(DzTaskProcessChainSummary::getChainId, chainIds)
                .or()
                .in(DzTaskProcessChainSummary::getRootChainId, chainIds))));
    }

    private boolean currentRiskLevelUsesReport(DzTaskProcessChainNode node, Long reportId) {
        if (node == null || reportId == null) {
            return false;
        }
        BusinessContext context = resolveBusinessContext(node);
        DzReportDisaster report = resolveRiskReportForNode(node, context);
        return report != null && reportId.equals(report.getId()) && hasReportRiskLevel(report);
    }

    private DzReportDisaster resolveRiskReportForNode(DzTaskProcessChainNode node, BusinessContext context) {
        if (node == null) {
            return null;
        }
        if (isReportRiskPreferredNode(node, context)) {
            return resolveRiskReport(context);
        }
        if (TaskProcessBizTypeEnum.REPORT.getCode().equals(node.getBizType())) {
            DzReportDisaster report = context == null ? null : context.report();
            return hasReportRiskLevel(report) ? report : null;
        }
        return null;
    }

    private boolean hasReportRiskLevel(DzReportDisaster report) {
        return report != null && (report.getManualRiskLevel() != null || report.getAiRiskLevel() != null);
    }

    private DzTaskProcessChainNode selectLatestDisplayNode(DisplayKey displayKey) {
        if (displayKey == null || displayKey.bizType() == null || displayKey.bizId() == null) {
            return null;
        }
        return chainNodeMapper.selectOne(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getDisplayBizType, displayKey.bizType())
            .eq(DzTaskProcessChainNode::getDisplayBizId, displayKey.bizId())
            .in(DzTaskProcessChainNode::getChainSegmentType, DEFAULT_DISPLAY_SEGMENTS)
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByDesc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId)
            .last("limit 1"));
    }

    private void refreshByDisplayKey(DisplayKey displayKey) {
        DzTaskProcessChainNode latestNode = selectLatestDisplayNode(displayKey);
        if (latestNode == null) {
            return;
        }
        refreshByDisplayKey(displayKey, latestNode);
    }

    private void refreshByDisplayKeyIfCurrentReport(DisplayKey displayKey, Long reportId) {
        DzTaskProcessChainNode latestNode = selectLatestDisplayNode(displayKey);
        if (latestNode == null || !currentRiskLevelUsesReport(latestNode, reportId)) {
            return;
        }
        refreshByDisplayKey(displayKey, latestNode);
    }

    private void refreshByDisplayKey(DisplayKey displayKey, DzTaskProcessChainNode latestNode) {
        if (displayKey == null || latestNode == null) {
            return;
        }
        DzTaskProcessChainSummary summary = buildSummary(latestNode);
        clearStaleSummaries(summary, displayKey);
        DzTaskProcessChainSummary existing = summaryMapper.selectOne(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
            .eq(DzTaskProcessChainSummary::getDisplayBizType, displayKey.bizType())
            .eq(DzTaskProcessChainSummary::getDisplayBizId, displayKey.bizId())
            .eq(DzTaskProcessChainSummary::getDeleted, 0)
            .last("limit 1"));
        Date now = new Date();
        summary.setUpdateDate(now);
        summary.setDeleted(0);
        if (existing == null) {
            summary.setCreateDate(now);
            summaryMapper.insert(summary);
            return;
        }
        summary.setId(existing.getId());
        summaryMapper.updateById(summary);
    }

    private void refreshRootChainClosed(DzTaskProcessChainNode node) {
        String chainId = resolveClosedScopeChainId(node);
        if (StringUtils.isBlank(chainId)) {
            return;
        }
        Integer rootChainClosed = resolveRootChainClosed(chainId);
        updateRootChainClosedForNodes(chainId, rootChainClosed);
        updateRootChainClosedForSummaries(chainId, rootChainClosed);
    }

    private String resolveClosedScopeChainId(DzTaskProcessChainNode node) {
        if (node == null) {
            return null;
        }
        return node.getChainId();
    }

    private Integer resolveRootChainClosed(String chainId) {
        if (StringUtils.isBlank(chainId)) {
            return 0;
        }
        List<DzTaskProcessChainNode> latestNodes = chainNodeMapper.selectLatestNodesByChainTree(chainId);
        if (latestNodes == null || latestNodes.isEmpty()) {
            return 0;
        }
        return latestNodes.stream().allMatch(this::isClosedLatestNode) ? 1 : 0;
    }

    private boolean isClosedLatestNode(DzTaskProcessChainNode node) {
        return node != null
            && TaskProcessChainNodeTextEnum.isRootClosedLinkName(node.getLinkName());
    }

    private void updateRootChainClosedForNodes(String chainId, Integer rootChainClosed) {
        chainNodeMapper.updateRootChainClosedByChainTree(chainId, rootChainClosed);
    }

    private void updateRootChainClosedForSummaries(String chainId, Integer rootChainClosed) {
        summaryMapper.updateRootChainClosedByChainTree(chainId, rootChainClosed);
    }

    private void clearStaleSummaries(DzTaskProcessChainSummary summary, DisplayKey displayKey) {
        if (summary == null || StringUtils.isBlank(summary.getChainId())) {
            return;
        }
        Date now = new Date();
        summaryMapper.update(null, Wrappers.<DzTaskProcessChainSummary>lambdaUpdate()
            .set(DzTaskProcessChainSummary::getDeleted, 1)
            .set(DzTaskProcessChainSummary::getUpdateDate, now)
            .eq(DzTaskProcessChainSummary::getChainId, summary.getChainId())
            .eq(DzTaskProcessChainSummary::getDeleted, 0)
            .and(w -> w.ne(DzTaskProcessChainSummary::getDisplayBizType, displayKey.bizType())
                .or()
                .ne(DzTaskProcessChainSummary::getDisplayBizId, displayKey.bizId())));
    }

    private DzTaskProcessChainSummary buildSummary(DzTaskProcessChainNode node) {
        DzTaskProcessChainSummary summary = new DzTaskProcessChainSummary();
        copyNodeFields(summary, node);
        TaskProcessDisplayStatusEnum displayStatus = TaskProcessDisplayStatusEnum.fromLinkName(node.getLinkName());
        summary.setCurrentStatus(displayStatus.getCode());

        BusinessContext context = resolveBusinessContext(node);
        applyBusinessContext(summary, context);
        applyChainBusinessIds(summary, node, context);
        summary.setPendingPushTaskCount(resolvePendingPushTaskCount(summary, context));
        RiskLevel riskLevel = resolveRiskLevel(node, context, summary.getRiskId());
        summary.setRiskLevel(riskLevel.level());
        summary.setRiskLevelSource(riskLevel.source());
        summary.setDynamicRiskLevel(resolveCurrentSlopeDynamicRiskLevel(summary.getRiskId(), context.unitId()));
        applySlopeUnit(summary, context.unitId());

        ResponsiblePersons responsiblePersons = resolveResponsiblePersons(node, context);
        summary.setResponsiblePerson(responsiblePersons.names());
        summary.setResponsiblePersonPhone(responsiblePersons.phones());
        return summary;
    }

    private void refreshDynamicRiskLevelByRiskAndUnit(Long riskId, String unitId) {
        for (DisplayKey displayKey : loadAffectedDisplayKeysByRiskAndUnit(riskId, unitId)) {
            refreshByDisplayKey(displayKey);
        }
    }

    private Set<DisplayKey> loadAffectedDisplayKeysByRiskAndUnit(Long riskId, String unitId) {
        LinkedHashSet<DisplayKey> keys = new LinkedHashSet<>();
        if (riskId != null) {
            addDisplayKeys(keys, summaryMapper.selectList(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
                .eq(DzTaskProcessChainSummary::getDeleted, 0)
                .eq(DzTaskProcessChainSummary::getRiskId, riskId)));
        }
        List<String> unitIds = expandSlopeUnitIds(unitId);
        if (!unitIds.isEmpty()) {
            addDisplayKeys(keys, summaryMapper.selectList(Wrappers.<DzTaskProcessChainSummary>lambdaQuery()
                .eq(DzTaskProcessChainSummary::getDeleted, 0)
                .in(DzTaskProcessChainSummary::getUnitId, unitIds)));
        }
        return keys;
    }

    private void addDisplayKeys(Set<DisplayKey> keys, List<DzTaskProcessChainSummary> summaries) {
        if (keys == null || summaries == null || summaries.isEmpty()) {
            return;
        }
        summaries.stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getDisplayBizType() != null && item.getDisplayBizId() != null)
            .map(item -> new DisplayKey(item.getDisplayBizType(), item.getDisplayBizId()))
            .forEach(keys::add);
    }

    private Integer resolveCurrentSlopeDynamicRiskLevel(Long riskId, String unitId) {
        if (riskId != null) {
            DzRiskAssessment riskAssessment = riskAssessmentMapper.selectById(riskId);
            return riskAssessment == null ? null : riskAssessment.getDynamicRiskLevel();
        }
        List<String> unitIds = expandSlopeUnitIds(unitId);
        if (unitIds.isEmpty()) {
            return null;
        }
        DzRiskAssessment latestAssessment = riskAssessmentMapper.selectOne(Wrappers.<DzRiskAssessment>lambdaQuery()
            .in(DzRiskAssessment::getSlopeUnitId, unitIds)
            .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId)
            .last("limit 1"));
        return latestAssessment == null ? null : latestAssessment.getDynamicRiskLevel();
    }

    private Long resolveLatestRiskAssessmentId(String unitId) {
        List<String> unitIds = expandSlopeUnitIds(unitId);
        if (unitIds.isEmpty()) {
            return null;
        }
        DzRiskAssessment latestAssessment = riskAssessmentMapper.selectOne(Wrappers.<DzRiskAssessment>lambdaQuery()
            .in(DzRiskAssessment::getSlopeUnitId, unitIds)
            .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId)
            .last("limit 1"));
        return latestAssessment == null ? null : latestAssessment.getId();
    }

    private List<String> expandSlopeUnitIds(String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return List.of();
        }
        String trimmed = unitId.trim();
        String normalized = DzTaskDistListServiceImpl.normalizeSlopeUnitId(trimmed);
        return Stream.of(trimmed, normalized)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
    }

    private void copyNodeFields(DzTaskProcessChainSummary summary, DzTaskProcessChainNode node) {
        summary.setNodeId(node.getId());
        summary.setChainId(node.getChainId());
        summary.setParentChainId(node.getParentChainId());
        summary.setParentNodeId(node.getParentNodeId());
        summary.setLinkName(node.getLinkName());
        summary.setTriggerReason(node.getTriggerReason());
        summary.setOperatorId(node.getOperatorId());
        summary.setOperatorName(node.getOperatorName());
        summary.setOperatorRole(node.getOperatorRole());
        summary.setTriggerTime(node.getTriggerTime());
        summary.setBizType(node.getBizType());
        summary.setBizId(node.getBizId());
        summary.setTaskId(node.getTaskId());
        summary.setSourceType(node.getSourceType());
        summary.setNodeCategory(node.getNodeCategory());
        summary.setStageType(node.getStageType());
        summary.setNodeCreateDate(node.getCreateDate());
        summary.setNodeUpdateDate(node.getUpdateDate());
        summary.setDisplayBizType(node.getDisplayBizType());
        summary.setDisplayBizId(node.getDisplayBizId());
        summary.setRootChainId(node.getRootChainId());
        summary.setRootChainClosed(node.getRootChainClosed());
        summary.setRootBizType(node.getRootBizType());
        summary.setRootBizId(node.getRootBizId());
        summary.setRootSourceType(node.getRootSourceType());
        summary.setChainSegmentType(node.getChainSegmentType());
    }

    private BusinessContext resolveBusinessContext(DzTaskProcessChainNode node) {
        DzTaskDistList task = resolveDisplayTask(node);
        DzReportDisaster report = null;
        DzTaskHandle handle = null;
        DefRespPlan defRespPlan = null;
        if (TaskProcessBizTypeEnum.REPORT.getCode().equals(node.getBizType()) && node.getBizId() != null) {
            report = reportDisasterMapper.selectById(node.getBizId());
            if (task == null && report != null && report.getTaskId() != null) {
                task = taskDistListMapper.selectById(report.getTaskId());
            }
        }
        if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getDisplayBizType()) && node.getDisplayBizId() != null) {
            handle = taskHandleMapper.selectById(node.getDisplayBizId());
        } else if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getBizType()) && node.getBizId() != null) {
            handle = taskHandleMapper.selectById(node.getBizId());
        }
        if (TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getDisplayBizType()) && node.getDisplayBizId() != null) {
            defRespPlan = defRespPlanMapper.selectById(node.getDisplayBizId());
        } else if (TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getBizType()) && node.getBizId() != null) {
            defRespPlan = defRespPlanMapper.selectById(node.getBizId());
        }
        String unitId = task == null ? null : task.getUnitId();
        if (StringUtils.isBlank(unitId) && handle != null) {
            unitId = handle.getSlopeUnitId();
        }
        if (StringUtils.isBlank(unitId)) {
            unitId = resolveReportSlopeUnitId(report);
        }
        return new BusinessContext(task, report, handle, defRespPlan, unitId);
    }

    private String resolveReportSlopeUnitId(DzReportDisaster report) {
        if (report == null || StringUtils.isBlank(report.getCheckCenter())) {
            return null;
        }
        try {
            SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(report.getCheckCenter());
            return slopeUnit == null ? null : slopeUnit.getId();
        } catch (Exception e) {
            log.warn("报灾坐标匹配斜坡单元失败, reportId={}, checkCenter={}", report.getId(), report.getCheckCenter(), e);
            return null;
        }
    }

    private DzTaskDistList resolveDisplayTask(DzTaskProcessChainNode node) {
        Long taskId = null;
        if (TaskProcessBizTypeEnum.TASK.getCode().equals(node.getDisplayBizType())) {
            taskId = node.getDisplayBizId();
        }
        if (taskId == null && node.getTaskId() != null) {
            taskId = node.getTaskId();
        }
        if (taskId == null && TaskProcessBizTypeEnum.TASK.getCode().equals(node.getBizType())) {
            taskId = node.getBizId();
        }
        return taskId == null ? null : taskDistListMapper.selectById(taskId);
    }

    private void applyBusinessContext(DzTaskProcessChainSummary summary, BusinessContext context) {
        DzTaskDistList task = context.task();
        if (task != null) {
            summary.setTaskId(task.getId());
            summary.setUnitId(task.getUnitId());
            summary.setSubmitRequire(task.getSubmitRequire());
        }
    }

    private void applyChainBusinessIds(DzTaskProcessChainSummary summary, DzTaskProcessChainNode node, BusinessContext context) {
        Long defId = resolveChainBizId(summary, node, TaskProcessBizTypeEnum.DEF_RESP.getCode());
        Long handleId = resolveChainBizId(summary, node, TaskProcessBizTypeEnum.HANDLE.getCode());
        DefRespPlan defRespPlan = defId == null ? context.defRespPlan() : defRespPlanMapper.selectById(defId);
        if (handleId == null && defRespPlan != null) {
            handleId = defRespPlan.getHandleId();
        }
        String unitId = context.unitId();
        if (StringUtils.isBlank(unitId) && context.handle() != null) {
            unitId = context.handle().getSlopeUnitId();
        }
        summary.setDefId(defId);
        summary.setHandleId(handleId);
        summary.setRiskId(resolveSummaryRiskAssessmentId(context, unitId));
    }

    private Long resolveSummaryRiskAssessmentId(BusinessContext context, String unitId) {
        DzTaskDistList task = context == null ? null : context.task();
        if (task != null && task.getRiskId() != null) {
            return task.getRiskId();
        }
        return resolveLatestRiskAssessmentId(unitId);
    }

    private Long resolveChainBizId(DzTaskProcessChainSummary summary, DzTaskProcessChainNode node, Integer bizType) {
        Long localBizId = resolveLocalBizId(node, bizType);
        if (localBizId != null) {
            return localBizId;
        }
        if (summary == null || StringUtils.isBlank(summary.getChainId())) {
            return null;
        }
        try {
            return summaryMapper.selectLatestBizIdByChainIdAndType(summary.getChainId(), bizType);
        } catch (Exception e) {
            log.warn("链路关联业务ID解析失败, chainId={}, bizType={}", summary.getChainId(), bizType, e);
            return null;
        }
    }

    private Long resolveLocalBizId(DzTaskProcessChainNode node, Integer bizType) {
        if (node == null || bizType == null) {
            return null;
        }
        if (bizType.equals(node.getDisplayBizType()) && node.getDisplayBizId() != null) {
            return node.getDisplayBizId();
        }
        if (bizType.equals(node.getBizType()) && node.getBizId() != null) {
            return node.getBizId();
        }
        if (bizType.equals(node.getRootBizType()) && node.getRootBizId() != null) {
            return node.getRootBizId();
        }
        return null;
    }

    private Integer resolvePendingPushTaskCount(DzTaskProcessChainSummary summary, BusinessContext context) {
        if (summary == null) {
            return 0;
        }
        if (StringUtils.isBlank(summary.getChainId())) {
            DzTaskDistList task = context == null ? null : context.task();
            return task != null && Objects.equals(task.getDelete(), 0)
                && Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED) ? 1 : 0;
        }
        try {
            Integer count = summaryMapper.selectPendingPushTaskCountByChainId(summary.getChainId());
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("待推送任务数量解析失败, chainId={}", summary.getChainId(), e);
            DzTaskDistList task = context == null ? null : context.task();
            return task != null && Objects.equals(task.getDelete(), 0)
                && Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED) ? 1 : 0;
        }
    }

    private void applySlopeUnit(DzTaskProcessChainSummary summary, String unitId) {
        if (StringUtils.isBlank(unitId)) {
            return;
        }
        summary.setUnitId(unitId);
        List<SlopeUnitVo> slopeUnits = slopeUnitService.queryNoWktByIds(List.of(unitId));
        if (slopeUnits == null || slopeUnits.isEmpty()) {
            return;
        }
        SlopeUnitVo slopeUnit = slopeUnits.get(0);
        summary.setProvince(slopeUnit.getProvince());
        summary.setCity(slopeUnit.getCity());
        summary.setCounty(slopeUnit.getCounty());
        summary.setStreet(slopeUnit.getStreet());
        summary.setVillage(slopeUnit.getVillage());
        summary.setPilotArea1(slopeUnit.getPilotArea1());
        summary.setPilotArea2(slopeUnit.getPilotArea2());
    }

    private RiskLevel resolveRiskLevel(DzTaskProcessChainNode node, BusinessContext context, Long riskId) {
        return resolveRiskLevel(node, context, riskId, new LinkedHashSet<>());
    }

    private RiskLevel resolveRiskLevel(DzTaskProcessChainNode node, BusinessContext context, Long riskId,
                                       Set<Long> visitedNodeIds) {
        RiskLevel inheritedRiskLevel = resolveInheritedTechAssistRiskLevel(node, visitedNodeIds);
        if (inheritedRiskLevel.level() != null) {
            return inheritedRiskLevel;
        }
        RiskLevel reportRiskLevel = resolvePreferredReportRiskLevel(node, context);
        if (reportRiskLevel.level() != null) {
            return reportRiskLevel;
        }
        if (TaskProcessBizTypeEnum.REPORT.getCode().equals(node.getBizType()) && context.report() != null) {
            DzReportDisaster report = context.report();
            if (report.getManualRiskLevel() != null) {
                return new RiskLevel(report.getManualRiskLevel(), TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
            }
            if (report.getAiRiskLevel() != null) {
                return new RiskLevel(report.getAiRiskLevel(), TaskProcessRiskLevelSourceEnum.REPORT_AI.getCode());
            }
        }
        if (TaskProcessBizTypeEnum.HANDLE.getCode().equals(node.getBizType()) && context.handle() != null
            && context.handle().getEventLevel() != null) {
            return new RiskLevel(context.handle().getEventLevel(), TaskProcessRiskLevelSourceEnum.HANDLE_EVENT_LEVEL.getCode());
        }
        if (TaskProcessBizTypeEnum.DEF_RESP.getCode().equals(node.getBizType()) && context.defRespPlan() != null
            && context.defRespPlan().getLevel() != null) {
            return new RiskLevel(context.defRespPlan().getLevel(), TaskProcessRiskLevelSourceEnum.DEF_RESP_LEVEL.getCode());
        }
        if (TaskProcessBizTypeEnum.MONITOR_WARNING.getCode().equals(node.getBizType())) {
            Integer level = resolveCurrentSlopeDynamicRiskLevel(
                riskId,
                context.unitId()
            );
            if (level != null) {
                return new RiskLevel(level, TaskProcessRiskLevelSourceEnum.RISK_ASSESSMENT.getCode());
            }
        }
        if (TaskProcessBizTypeEnum.ALARM.getCode().equals(node.getBizType()) && node.getBizId() != null) {
            try {
                Integer level = summaryMapper.selectAlarmMaxLevel(node.getBizId());
                if (level != null) {
                    return new RiskLevel(level, TaskProcessRiskLevelSourceEnum.ALARM_LEVEL.getCode());
                }
            } catch (Exception e) {
                log.warn("预警报告风险等级解析失败, alarmId={}", node.getBizId(), e);
            }
        }
        if (riskId != null) {
            DzRiskAssessment riskAssessment = riskAssessmentMapper.selectById(riskId);
            if (riskAssessment != null && riskAssessment.getDynamicRiskLevel() != null) {
                return new RiskLevel(riskAssessment.getDynamicRiskLevel(), TaskProcessRiskLevelSourceEnum.RISK_ASSESSMENT.getCode());
            }
        }
        return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
    }

    private RiskLevel resolveInheritedTechAssistRiskLevel(DzTaskProcessChainNode node, Set<Long> visitedNodeIds) {
        if (!isTechAssistRiskInheritedNode(node) || node.getParentNodeId() == null) {
            return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
        }
        if (node.getId() != null && !visitedNodeIds.add(node.getId())) {
            return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
        }
        DzTaskProcessChainNode parent = chainNodeMapper.selectById(node.getParentNodeId());
        if (parent == null || Objects.equals(parent.getDeleted(), 1)) {
            return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
        }
        BusinessContext parentContext = resolveBusinessContext(parent);
        Long parentRiskId = resolveLatestRiskAssessmentId(parentContext.unitId());
        return resolveRiskLevel(parent, parentContext, parentRiskId, visitedNodeIds);
    }

    private boolean isTechAssistRiskInheritedNode(DzTaskProcessChainNode node) {
        if (node == null || StringUtils.isBlank(node.getLinkName())) {
            return false;
        }
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(), normalized)
            || isTechAssistTaskPushNode(node, normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(
                TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), normalized);
    }

    private boolean isTechAssistTaskPushNode(DzTaskProcessChainNode node, String normalizedLinkName) {
        return TaskProcessSourceTypeEnum.TECH_ASSISTANCE.getCode().equals(node.getSourceType())
            && TaskProcessChainNodeTextEnum.semanticEquals(
                TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalizedLinkName);
    }

    private RiskLevel resolvePreferredReportRiskLevel(DzTaskProcessChainNode node, BusinessContext context) {
        if (!isReportRiskPreferredNode(node, context)) {
            return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
        }
        return resolveReportRiskLevel(resolveRiskReport(context));
    }

    private RiskLevel resolveReportRiskLevel(DzReportDisaster report) {
        if (report == null) {
            return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
        }
        if (report.getManualRiskLevel() != null) {
            return new RiskLevel(report.getManualRiskLevel(), TaskProcessRiskLevelSourceEnum.REPORT_MANUAL.getCode());
        }
        if (report.getAiRiskLevel() != null) {
            return new RiskLevel(report.getAiRiskLevel(), TaskProcessRiskLevelSourceEnum.REPORT_AI.getCode());
        }
        return new RiskLevel(null, TaskProcessRiskLevelSourceEnum.UNKNOWN.getCode());
    }

    private boolean isReportRiskPreferredNode(DzTaskProcessChainNode node, BusinessContext context) {
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        if (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(
                TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD.getLinkName(), normalized)) {
            return true;
        }
        DzTaskDistList task = context == null ? null : context.task();
        if (task == null) {
            return false;
        }
        boolean aiVerifyTask = DzTaskDistList.TASK_TYPE_AI_VERIFY.equals(task.getTaskType())
            || DzTaskDistList.SOURCE_TYPE_REPORT.equals(task.getSourceType());
        boolean techAssistTask = DzTaskDistList.TASK_TYPE_EMERGENCY_INVESTIGATION.equals(task.getTaskType())
            || DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE.equals(task.getSourceType());
        return aiVerifyTask && (TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalized))
            || techAssistTask && TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalized);
    }

    private DzReportDisaster resolveRiskReport(BusinessContext context) {
        if (context == null) {
            return null;
        }
        DzReportDisaster report = context.report();
        if (report != null && (report.getManualRiskLevel() != null || report.getAiRiskLevel() != null)) {
            return report;
        }
        DzTaskDistList task = context.task();
        if (task == null) {
            return null;
        }
        if (task.getReportId() != null) {
            DzReportDisaster linkedReport = reportDisasterMapper.selectById(task.getReportId());
            if (linkedReport != null && (linkedReport.getManualRiskLevel() != null || linkedReport.getAiRiskLevel() != null)) {
                return linkedReport;
            }
        }
        return reportDisasterMapper.selectOne(Wrappers.<DzReportDisaster>lambdaQuery()
            .eq(DzReportDisaster::getSourceType, TaskProcessSourceTypeEnum.TASK_FEEDBACK.getCode())
            .eq(DzReportDisaster::getTaskId, task.getId())
            .and(w -> w.isNotNull(DzReportDisaster::getManualRiskLevel)
                .or()
                .isNotNull(DzReportDisaster::getAiRiskLevel))
            .orderByDesc(DzReportDisaster::getCreateDate, DzReportDisaster::getId)
            .last("limit 1"));
    }

    private ResponsiblePersons resolveResponsiblePersons(DzTaskProcessChainNode node, BusinessContext context) {
        if (TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName().equals(node.getLinkName())) {
            return ResponsiblePersons.single(node.getOperatorId(), node.getOperatorName(), null);
        }
        if (isEmergencyBatchDisplayNode(node)) {
            ResponsiblePersons aggregated = aggregateEmergencyChildResponsiblePersons(node);
            if (!aggregated.isEmpty()) {
                return aggregated;
            }
        }
        DzTaskDistList task = context.task();
        if (task != null && isTaskResponsibleNode(node)) {
            return ResponsiblePersons.single(task.getUserId(), task.getResponsiblePerson(), task.getResponsiblePersonPhone());
        }
        return ResponsiblePersons.single(node.getOperatorId(), node.getOperatorName(), null);
    }

    private boolean isTaskResponsibleNode(DzTaskProcessChainNode node) {
        if (TaskProcessBizTypeEnum.TASK.getCode().equals(node.getBizType())) {
            return true;
        }
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(node.getLinkName());
        return TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_PUSH_SYSTEM.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_INSPECTING.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_FEEDBACK_REPORT.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TASK_CLOSE_DEFAULT.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_APPLY.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(), normalized)
            || TaskProcessChainNodeTextEnum.semanticEquals(TaskProcessChainNodeTextEnum.TECH_ASSIST_TASK_DISPATCH.getLinkName(), normalized);
    }

    private boolean isEmergencyBatchDisplayNode(DzTaskProcessChainNode node) {
        return TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode().equals(node.getNodeCategory())
            || TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName().equals(node.getLinkName())
            || TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName().equals(node.getLinkName());
    }

    private ResponsiblePersons aggregateEmergencyChildResponsiblePersons(DzTaskProcessChainNode node) {
        if (node.getId() == null) {
            return ResponsiblePersons.empty();
        }
        List<DzTaskProcessChainNode> childNodes = chainNodeMapper.selectList(Wrappers.<DzTaskProcessChainNode>lambdaQuery()
            .eq(DzTaskProcessChainNode::getParentNodeId, node.getId())
            .eq(DzTaskProcessChainNode::getDeleted, 0)
            .orderByAsc(DzTaskProcessChainNode::getCreateDate, DzTaskProcessChainNode::getId));
        List<Long> taskIds = childNodes.stream()
            .filter(child -> child != null && child.getTaskId() != null)
            .filter(child -> TaskProcessSourceTypeEnum.EMERGENCY.getCode().equals(child.getSourceType())
                || TaskProcessChainNodeTextEnum.EMERGENCY_TASK_DISPATCH.getLinkName().equals(child.getLinkName()))
            .map(DzTaskProcessChainNode::getTaskId)
            .distinct()
            .toList();
        if (taskIds.isEmpty()) {
            return ResponsiblePersons.empty();
        }
        List<DzTaskDistList> tasks = taskDistListMapper.selectBatchIds(taskIds);
        ResponsiblePersons.Builder builder = ResponsiblePersons.builder();
        for (Long taskId : taskIds) {
            DzTaskDistList task = tasks.stream()
                .filter(item -> item != null && Objects.equals(item.getId(), taskId))
                .findFirst()
                .orElse(null);
            if (task != null) {
                builder.add(task.getUserId(), task.getResponsiblePerson(), task.getResponsiblePersonPhone());
            }
        }
        return builder.build();
    }

    private TaskProcessChainSummaryVo toVo(DzTaskProcessChainSummary summary) {
        TaskProcessChainSummaryVo vo = new TaskProcessChainSummaryVo();
        vo.setId(summary.getDisplayBizId());
        vo.setSummaryId(summary.getId());
        vo.setNodeId(summary.getNodeId());
        vo.setChainId(summary.getChainId());
        vo.setParentChainId(summary.getParentChainId());
        vo.setParentNodeId(summary.getParentNodeId());
        vo.setLinkName(normalizeLinkName(summary.getLinkName()));
        vo.setTriggerReason(summary.getTriggerReason());
        vo.setOperatorId(summary.getOperatorId());
        vo.setOperatorName(summary.getOperatorName());
        vo.setOperatorRole(summary.getOperatorRole());
        vo.setTriggerTime(summary.getTriggerTime());
        vo.setBizType(summary.getBizType());
        vo.setBizId(summary.getBizId());
        vo.setTaskId(summary.getTaskId());
        vo.setSourceType(summary.getSourceType());
        vo.setNodeCategory(summary.getNodeCategory());
        vo.setStageType(summary.getStageType());
        vo.setNodeCreateDate(summary.getNodeCreateDate());
        vo.setNodeUpdateDate(summary.getNodeUpdateDate());
        vo.setDisplayBizType(summary.getDisplayBizType());
        vo.setDisplayBizId(summary.getDisplayBizId());
        vo.setRootChainId(summary.getRootChainId());
        vo.setRootChainClosed(summary.getRootChainClosed());
        vo.setRootBizType(summary.getRootBizType());
        vo.setRootBizId(summary.getRootBizId());
        vo.setRootSourceType(summary.getRootSourceType());
        vo.setChainSegmentType(summary.getChainSegmentType());
        vo.setChainSegmentTypeLabel(TaskProcessChainSegmentTypeEnum.label(summary.getChainSegmentType()));
        vo.setCurrentStatus(summary.getCurrentStatus());
        vo.setRiskLevel(summary.getRiskLevel());
        vo.setRiskLevelSource(summary.getRiskLevelSource());
        vo.setRiskLevelSourceLabel(TaskProcessRiskLevelSourceEnum.label(summary.getRiskLevelSource()));
        vo.setUnitId(summary.getUnitId());
        vo.setProvince(summary.getProvince());
        vo.setCity(summary.getCity());
        vo.setCounty(summary.getCounty());
        vo.setStreet(summary.getStreet());
        vo.setVillage(summary.getVillage());
        vo.setPilotArea1(summary.getPilotArea1());
        vo.setPilotArea2(summary.getPilotArea2());
        vo.setResponsiblePerson(summary.getResponsiblePerson());
        vo.setResponsiblePersonPhone(summary.getResponsiblePersonPhone());
        vo.setPendingPushTaskCount(summary.getPendingPushTaskCount());
        vo.setDynamicRiskLevel(summary.getDynamicRiskLevel());
        vo.setInspectingRequire(summary.getInspectingRequire());
        vo.setSubmitRequire(StringUtils.isNotBlank(summary.getSubmitRequire())
            ? summary.getSubmitRequire()
            : summary.getInspectingRequire());
        vo.setDefId(summary.getDefId());
        vo.setHandleId(summary.getHandleId());
        vo.setRiskId(summary.getRiskId());
        vo.setCreateDate(summary.getCreateDate());
        vo.setUpdateDate(summary.getUpdateDate());
        vo.setSlopeUnit(toSlopeUnitVo(summary));
        vo.setSysUser(toSysUserVo(summary));
        TaskProcessChainNodeVo currentNode = toNodeVo(summary);
        vo.setCurrentNode(currentNode);
        vo.setLatestProcessNode(currentNode);
        return vo;
    }

    private SlopeUnitVo toSlopeUnitVo(DzTaskProcessChainSummary summary) {
        if (StringUtils.isBlank(summary.getUnitId())) {
            return null;
        }
        SlopeUnitVo slopeUnit = new SlopeUnitVo();
        slopeUnit.setId(summary.getUnitId());
        slopeUnit.setProvince(summary.getProvince());
        slopeUnit.setCity(summary.getCity());
        slopeUnit.setCounty(summary.getCounty());
        slopeUnit.setStreet(summary.getStreet());
        slopeUnit.setVillage(summary.getVillage());
        slopeUnit.setPilotArea1(summary.getPilotArea1());
        slopeUnit.setPilotArea2(summary.getPilotArea2());
        return slopeUnit;
    }

    private SysUserVo toSysUserVo(DzTaskProcessChainSummary summary) {
        if (StringUtils.isBlank(summary.getResponsiblePerson())
            && StringUtils.isBlank(summary.getResponsiblePersonPhone())) {
            return null;
        }
        SysUserVo sysUser = new SysUserVo();
        sysUser.setNickName(summary.getResponsiblePerson());
        sysUser.setPhonenumber(summary.getResponsiblePersonPhone());
        return sysUser;
    }

    private TaskProcessChainNodeVo toNodeVo(DzTaskProcessChainSummary summary) {
        TaskProcessChainNodeVo node = new TaskProcessChainNodeVo();
        node.setNodeId(summary.getNodeId());
        node.setChainId(summary.getChainId());
        node.setParentChainId(summary.getParentChainId());
        node.setParentNodeId(summary.getParentNodeId());
        node.setLinkName(normalizeLinkName(summary.getLinkName()));
        node.setTriggerReason(summary.getTriggerReason());
        node.setOperatorId(summary.getOperatorId());
        node.setOperatorName(summary.getOperatorName());
        node.setOperatorRole(summary.getOperatorRole());
        node.setTriggerTime(summary.getTriggerTime());
        node.setBizType(summary.getBizType());
        node.setType(summary.getBizType());
        node.setBizTypeLabel(TaskProcessBizTypeEnum.label(summary.getBizType()));
        node.setBizId(summary.getBizId());
        node.setTaskId(summary.getTaskId());
        node.setSourceType(summary.getSourceType());
        node.setSourceTypeCode(summary.getSourceType());
        node.setSourceTypeLabel(TaskProcessSourceTypeEnum.label(summary.getSourceType()));
        node.setNodeCategory(summary.getNodeCategory());
        node.setNodeCategoryLabel(TaskProcessNodeCategoryEnum.label(summary.getNodeCategory()));
        node.setStageType(summary.getStageType());
        node.setStageTypeLabel(TaskProcessStageTypeEnum.label(summary.getStageType()));
        node.setRootChainId(summary.getRootChainId());
        node.setRootChainClosed(summary.getRootChainClosed());
        node.setDisplayBizType(summary.getDisplayBizType());
        node.setDisplayBizId(summary.getDisplayBizId());
        node.setChainSegmentType(summary.getChainSegmentType());
        node.setChainSegmentTypeLabel(TaskProcessChainSegmentTypeEnum.label(summary.getChainSegmentType()));
        node.setRootBizType(summary.getRootBizType());
        node.setRootBizId(summary.getRootBizId());
        node.setRootSourceType(summary.getRootSourceType());
        node.setCreateDate(summary.getNodeCreateDate());
        node.setUpdateDate(summary.getNodeUpdateDate());
        return node;
    }

    private String normalizeLinkName(String linkName) {
        return TaskProcessChainNodeTextEnum.normalizeDisplayLinkName(linkName);
    }

    private record DisplayKey(Integer bizType, Long bizId) {
    }

    private record BusinessContext(DzTaskDistList task, DzReportDisaster report, DzTaskHandle handle,
                                   DefRespPlan defRespPlan, String unitId) {
    }

    private record RiskLevel(Integer level, Integer source) {
    }

    private record ResponsiblePersons(String names, String phones) {

        private static ResponsiblePersons empty() {
            return new ResponsiblePersons(null, null);
        }

        private static ResponsiblePersons single(Long userId, String name, String phone) {
            return builder().add(userId, name, phone).build();
        }

        private static Builder builder() {
            return new Builder();
        }

        private boolean isEmpty() {
            return StringUtils.isBlank(names) && StringUtils.isBlank(phones);
        }

        private static class Builder {
            private final Map<String, ResponsibleItem> items = new LinkedHashMap<>();

            private Builder add(Long userId, String name, String phone) {
                if (StringUtils.isBlank(name) && StringUtils.isBlank(phone)) {
                    return this;
                }
                String normalizedName = trimToNull(name);
                String normalizedPhone = trimToNull(phone);
                String key;
                if (userId != null) {
                    key = "u:" + userId;
                } else if (StringUtils.isNotBlank(normalizedName) || StringUtils.isNotBlank(normalizedPhone)) {
                    key = "np:" + StringUtils.defaultString(normalizedName) + ":" + StringUtils.defaultString(normalizedPhone);
                } else {
                    key = "p:" + normalizedPhone;
                }
                items.putIfAbsent(key, new ResponsibleItem(normalizedName, normalizedPhone));
                return this;
            }

            private ResponsiblePersons build() {
                String names = items.values().stream()
                    .map(ResponsibleItem::name)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
                String phones = items.values().stream()
                    .map(ResponsibleItem::phone)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
                return new ResponsiblePersons(StringUtils.isBlank(names) ? null : names,
                    StringUtils.isBlank(phones) ? null : phones);
            }

            private static String trimToNull(String value) {
                if (StringUtils.isBlank(value)) {
                    return null;
                }
                return value.trim();
            }
        }
    }

    private record ResponsibleItem(String name, String phone) {
    }
}
