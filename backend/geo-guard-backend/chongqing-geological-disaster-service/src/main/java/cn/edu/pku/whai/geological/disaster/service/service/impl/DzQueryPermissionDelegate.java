package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskNeedAttentionEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListRemark;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskAssessmentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListAddVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListAddMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListRemarkMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 任务派发清单Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
class DzQueryPermissionDelegate {

    private final DzTaskDistListServiceImpl service;
    private final DzTaskDistListMapper baseMapper;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final ISysUserService sysUserService;
    private final ISlopeUnitService slopeUnitService;
    private static final String DEFAULT_TASK_LIST_ORDER_COLUMN = DzTaskDistListServiceImpl.DEFAULT_TASK_LIST_ORDER_COLUMN;
    private static final Map<String, String> TASK_LIST_SORT_COLUMN_MAP = DzTaskDistListServiceImpl.TASK_LIST_SORT_COLUMN_MAP;
    private static final Set<String> TASK_LIST_CHINESE_STRING_SORT_COLUMNS = DzTaskDistListServiceImpl.TASK_LIST_CHINESE_STRING_SORT_COLUMNS;

    private final IDzUserAdRegionService dzUserAdRegionService;
    private final DzTaskDistListRemarkMapper dzTaskDistListRemarkMapper;
    private final DzTaskDistListAddMapper dzTaskDistListAddMapper;

    DzQueryPermissionDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzRiskAssessmentMapper = service.dzRiskAssessmentMapper;
        this.sysUserService = service.sysUserService;
        this.slopeUnitService = service.slopeUnitService;
        this.dzUserAdRegionService = service.dzUserAdRegionService;
        this.dzTaskDistListRemarkMapper = service.dzTaskDistListRemarkMapper;
        this.dzTaskDistListAddMapper = service.dzTaskDistListAddMapper;
    }

    private static String normalizeSlopeUnitId(String unitId) {
        return DzTaskDistListServiceImpl.normalizeSlopeUnitId(unitId);
    }

    private SlopeUnit resolveSlopeUnitByTaskUnitId(String unitId) {
        return service.resolveSlopeUnitByTaskUnitId(unitId);
    }

    public TableDataInfo<DzTaskDistListVo> queryPageList(DzTaskDistListBo bo, PageQuery pageQuery) {
        validateListQueryAccess(bo);
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegionsForListQuery();
        ensureListAdRegionPermission(userAdRegions);

        QueryWrapper<DzTaskDistList> ew = buildListQueryWrapper(bo, resolveDynamicRiskLevels(bo), userAdRegions);
        Page<DzTaskDistListVo> result = baseMapper.selectTaskDistPage(
            pageQuery.build(),
            ew
        );

        List<DzTaskDistListVo> records = result.getRecords();
        fillTaskBaseInfo(records);
        return TableDataInfo.build(result);
    }

    public TableDataInfo<DzTaskDistListVo> queryPageListInTaskIds(DzTaskDistListBo bo, PageQuery pageQuery,
                                                                  Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return TableDataInfo.build(List.of());
        }
        List<Long> distinctTaskIds = taskIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctTaskIds.isEmpty()) {
            return TableDataInfo.build(List.of());
        }
        validateListQueryAccess(bo);
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegionsForListQuery();
        ensureListAdRegionPermission(userAdRegions);

        QueryWrapper<DzTaskDistList> ew = buildListQueryWrapper(bo, resolveDynamicRiskLevels(bo), userAdRegions);
        ew.in("t.id", distinctTaskIds);
        Page<DzTaskDistListVo> result = baseMapper.selectTaskDistPage(pageQuery.build(), ew);

        List<DzTaskDistListVo> records = result.getRecords();
        fillTaskBaseInfo(records);
        return TableDataInfo.build(result);
    }

     List<DzUserAdRegionVo> getCurrentUserAdRegionsForListQuery() {
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegions();
        if (userAdRegions == null) {
            return null;
        }
        return userAdRegions.stream()
                            .filter(item -> StringUtils.isNotBlank(resolveSlopeUnitAdRegionCodeField(item.getAdRegionLevel()))
                                && StringUtils.isNotBlank(resolveSlopeUnitAdRegionCodeValue(item, item.getAdRegionLevel())))
                            .toList();
    }

     void fillTaskBaseInfo(List<DzTaskDistListVo> records) {

        Map<Long, SysUserVo> umap = getLongSysUserVoMap(records);
        Map<String, SlopeUnitVo> slopeUnitMap = querySlopeUnitMap(records);
        Map<Long, DzRiskAssessmentVo> rmap = getLongDzRiskAssessmentVoMap(records);

        records.forEach(e -> {
            e.setSysUser(umap.get(e.getUserId()));
            e.setSlopeUnit(resolveSlopeUnitVo(slopeUnitMap, e.getUnitId()));

            fillTaskRiskInfo(e, rmap.get(e.getRiskId()));
        });
    }

    static QueryWrapper<DzTaskDistList> buildListQueryWrapper(DzTaskDistListBo bo,
                                                              List<Integer> dynamicRiskLevels,
                                                              List<DzUserAdRegionVo> userAdRegions) {
        QueryWrapper<DzTaskDistList> ew = Wrappers.query();
        appendSlopeUnitPilotAreaFilters(ew, bo.getPilotArea1(), bo.getPilotArea2());
        ew.eq("t.\"delete\"", 0);
        ew.eq(bo.getId() != null, "t.id", bo.getId());
        ew.eq(StringUtils.isNotBlank(bo.getUnitId()), "t.unit_id", bo.getUnitId());
        ew.eq(bo.getUserId() != null, "t.user_id", bo.getUserId());
        ew.eq(bo.getRiskId() != null, "t.risk_id", bo.getRiskId());
        ew.eq(StringUtils.isNotBlank(bo.getSubmitRequire()), "t.submit_require", bo.getSubmitRequire());
        ew.eq(StringUtils.isNotBlank(bo.getInspectionSuggestion()), "t.inspection_suggestion", bo.getInspectionSuggestion());
        ew.eq(StringUtils.isNotBlank(bo.getInspectionSuggestionBackup()), "t.inspection_suggestion_backup", bo.getInspectionSuggestionBackup());
        ew.eq(StringUtils.isNotBlank(bo.getScenePhoto()), "t.scene_photo", bo.getScenePhoto());
        ew.eq(StringUtils.isNotBlank(bo.getTextRecord()), "t.text_record", bo.getTextRecord());
        ew.in(dynamicRiskLevels != null && !dynamicRiskLevels.isEmpty(), "ra.dynamic_risk_level", dynamicRiskLevels);
        ew.eq(bo.getStatus() != null, "t.status", bo.getStatus());
        if (bo.getCreateDate() != null) {
            ew.ge("t.create_date", DateUtil.beginOfDay(bo.getCreateDate()));
            ew.le("t.create_date", DateUtil.endOfDay(bo.getCreateDate()));
        }
        ew.eq(bo.getUpdateDate() != null, "t.update_date", bo.getUpdateDate());
        ew.eq(bo.getCheckTime() != null, "t.check_time", bo.getCheckTime());
        ew.eq(StringUtils.isNotBlank(bo.getCheckCenter()), "t.check_center", bo.getCheckCenter());
        ew.eq(bo.getSubmitTime() != null, "t.submit_time", bo.getSubmitTime());
        ew.eq(bo.getSourceType() != null, "t.source_type", bo.getSourceType());
        ew.eq(bo.getReportId() != null, "t.report_id", bo.getReportId());
        ew.eq(bo.getHandleId() != null, "t.handle_id", bo.getHandleId());
        ew.eq(bo.getDefId() != null, "t.def_id", bo.getDefId());
        ew.like(StringUtils.isNotBlank(bo.getPlanName()), "t.plan_name", bo.getPlanName());
        ew.eq(bo.getPlanType() != null, "t.plan_type", bo.getPlanType());
        ew.eq(StringUtils.isNotBlank(bo.getResponsiblePerson()), "t.responsible_person", bo.getResponsiblePerson());
        ew.eq(StringUtils.isNotBlank(bo.getResponsiblePersonPhone()), "t.responsible_person_phone", bo.getResponsiblePersonPhone());
        ew.eq(bo.getOverdue() != null, "t.overdue", bo.getOverdue());
        ew.eq(bo.getQuotaConsumed() != null, "t.quota_consumed", bo.getQuotaConsumed());
        ew.eq(bo.getLastRemindTime() != null, "t.last_remind_time", bo.getLastRemindTime());
        ew.eq(bo.getReminderCount() != null, "t.reminder_count", bo.getReminderCount());
        ew.eq(StringUtils.isNotBlank(bo.getCloseReason()), "t.close_reason", bo.getCloseReason());
        ew.eq(bo.getClosedTime() != null, "t.closed_time", bo.getClosedTime());
        ew.eq(StringUtils.isNotBlank(bo.getRemark()), "t.remark", bo.getRemark());
        appendSourceTypeCondition(ew, bo);
        ew.in(bo.getStatusList() != null && !bo.getStatusList().isEmpty(), "t.status", bo.getStatusList());
        appendAdRegionPermissionCondition(ew, userAdRegions);
        DzPushSmsDelegate.appendRoleBasedTaskListFilters(ew, "t");
        appendTaskListOrderByCondition(ew, bo);
        return ew;
    }

     static void appendTaskListOrderByCondition(QueryWrapper<DzTaskDistList> ew, DzTaskDistListBo bo) {
        LinkedHashSet<String> customOrderColumns = new LinkedHashSet<>();
        appendTaskListOrderBy(ew, bo == null ? null : bo.getAsc(), true, customOrderColumns);
        appendTaskListOrderBy(ew, bo == null ? null : bo.getDesc(), false, customOrderColumns);
        if (!customOrderColumns.contains(DEFAULT_TASK_LIST_ORDER_COLUMN)) {
            ew.orderByDesc(DEFAULT_TASK_LIST_ORDER_COLUMN);
        }
    }

    private static void appendTaskListOrderBy(QueryWrapper<DzTaskDistList> ew,
                                              List<String> fields,
                                              boolean asc,
                                              Set<String> orderedColumns) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (String field : fields) {
            String sortColumn = resolveTaskListSortColumn(field);
            if (sortColumn == null || !orderedColumns.add(sortColumn)) {
                continue;
            }
            if (TASK_LIST_CHINESE_STRING_SORT_COLUMNS.contains(sortColumn)) {
                String expr = "convert_to(coalesce(" + sortColumn + ", ''), 'GB18030')";
                if (asc) {
                    ew.orderByAsc(expr);
                } else {
                    ew.orderByDesc(expr);
                }
                continue;
            }
            if (asc) {
                ew.orderByAsc(sortColumn);
            } else {
                ew.orderByDesc(sortColumn);
            }
        }
    }

    private static String resolveTaskListSortColumn(String field) {
        if (StringUtils.isBlank(field)) {
            return null;
        }
        return TASK_LIST_SORT_COLUMN_MAP.get(field.trim());
    }

    private static void appendSourceTypeCondition(QueryWrapper<DzTaskDistList> ew, DzTaskDistListBo bo) {
        List<Integer> sourceTypeList = bo.getSourceTypeList();
        if (sourceTypeList == null || sourceTypeList.isEmpty()) {
            return;
        }
        boolean includeEmergencyMonitoring = includeEmergencyMonitoring(bo);
        if (includeEmergencyMonitoring) {
            ew.and(wrapper -> wrapper.in("t.source_type", sourceTypeList)
                                     .or(inner -> inner.eq("t.source_type", DzTaskDistList.SOURCE_TYPE_EMERGENCY)
                                                       .in("t.plan_type",
                                                           DzTaskDistList.PLAN_TYPE_MONITORING,
                                                           DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING)));
            return;
        }
        ew.in("t.source_type", sourceTypeList);
    }

    private static void appendAdRegionPermissionCondition(QueryWrapper<DzTaskDistList> ew,
                                                          List<DzUserAdRegionVo> userAdRegions) {
        if (userAdRegions == null || userAdRegions.isEmpty()) {
            return;
        }
        List<DzUserAdRegionVo> effectiveRegions = userAdRegions.stream()
                                                                .filter(DzQueryPermissionDelegate::hasSlopeUnitAdRegionCondition)
                                                                .toList();
        if (effectiveRegions.isEmpty()) {
            ew.apply("1 = 0");
            return;
        }
        ew.and(wrapper -> {
            boolean first = true;
            for (DzUserAdRegionVo userAdRegion : effectiveRegions) {
                String column = resolveSlopeUnitAdRegionCodeColumn(userAdRegion.getAdRegionLevel());
                String value = resolveSlopeUnitAdRegionCodeValue(userAdRegion, userAdRegion.getAdRegionLevel());
                if (!first) {
                    wrapper.or();
                }
                wrapper.eq(column, value);
                first = false;
            }
        });
    }

    private static String resolveSlopeUnitAdRegionCodeColumn(Integer adRegionLevel) {
        String field = resolveSlopeUnitAdRegionCodeField(adRegionLevel);
        return StringUtils.isBlank(field) ? null : "su." + field;
    }

    private static boolean hasSlopeUnitAdRegionCondition(DzUserAdRegionVo userAdRegion) {
        return userAdRegion != null
            && StringUtils.isNotBlank(resolveSlopeUnitAdRegionCodeColumn(userAdRegion.getAdRegionLevel()))
            && StringUtils.isNotBlank(resolveSlopeUnitAdRegionCodeValue(userAdRegion, userAdRegion.getAdRegionLevel()));
    }

    static List<Integer> resolveDynamicRiskLevels(DzTaskDistListBo bo) {
        LinkedHashSet<Integer> dynamicRiskLevels = new LinkedHashSet<>();
        if (bo.getDynamicRiskLevel() != null) {
            dynamicRiskLevels.add(bo.getDynamicRiskLevel());
        }
        if (bo.getDynamicRiskLevels() != null) {
            bo.getDynamicRiskLevels().stream()
              .filter(Objects::nonNull)
              .forEach(dynamicRiskLevels::add);
        }
        return new ArrayList<>(dynamicRiskLevels);
    }

    private static boolean includeEmergencyMonitoring(DzTaskDistListBo bo) {
        List<Integer> sourceTypeList = bo.getSourceTypeList();
        return sourceTypeList != null
            && !sourceTypeList.isEmpty()
            && Objects.equals(bo.getIsEmergency(), 1)
            && !sourceTypeList.contains(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
    }

    private Map<Long, DzRiskAssessmentVo> getLongDzRiskAssessmentVoMap(List<DzTaskDistListVo> records) {
        List<Long> riskIds = records.stream().map(DzTaskDistListVo::getRiskId).filter(Objects::nonNull).distinct().toList();
        List<DzRiskAssessmentVo> dzRiskAssessments = riskIds.isEmpty() ? List.of() : dzRiskAssessmentMapper.selectVoByIds(riskIds);
        return CollStreamUtil.toIdentityMap(dzRiskAssessments, DzRiskAssessmentVo::getId);
    }

    private Map<Long, SysUserVo> getLongSysUserVoMap(List<DzTaskDistListVo> records) {
        List<Long> userIds = records.stream().map(DzTaskDistListVo::getUserId).filter(Objects::nonNull).distinct().toList();
        List<SysUserVo> sysUserVos = userIds.isEmpty() ? List.of() : sysUserService.selectUserByIds(userIds, null);
        return CollStreamUtil.toIdentityMap(sysUserVos, SysUserVo::getUserId);
    }

    private Map<String, SlopeUnitVo> querySlopeUnitMap(List<DzTaskDistListVo> records) {
        List<String> candidateIds = records.stream()
                                           .map(DzTaskDistListVo::getUnitId)
                                           .flatMap(unitId -> Stream.of(unitId, normalizeSlopeUnitId(unitId)))
                                           .filter(StringUtils::isNotBlank)
                                           .map(String::trim)
                                           .distinct()
                                           .toList();
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        List<SlopeUnitVo> slopeUnits = slopeUnitService.queryNoWktByIds(candidateIds);
        return slopeUnits.stream().collect(Collectors.toMap(SlopeUnitVo::getId, item -> item, (left, right) -> left));
    }

    private SlopeUnitVo resolveSlopeUnitVo(Map<String, SlopeUnitVo> slopeUnitMap, String unitId) {
        if (StringUtils.isBlank(unitId) || slopeUnitMap.isEmpty()) {
            return null;
        }
        SlopeUnitVo slopeUnitVo = slopeUnitMap.get(unitId.trim());
        if (slopeUnitVo != null) {
            return slopeUnitVo;
        }
        return slopeUnitMap.get(normalizeSlopeUnitId(unitId));
    }

    private void fillTaskRiskInfo(DzTaskDistListVo task, DzRiskAssessmentVo risk) {
        if (risk == null) {
            return;
        }
        String dynamicRiskSuggest = risk.getDynamicRiskSuggest();
        if (StringUtils.isNotBlank(dynamicRiskSuggest)) {
            List<String> suggests = StringUtils.splitList(dynamicRiskSuggest);
            StringBuilder dailyNeedAttention = new StringBuilder();
            StringBuilder defenseNeedAttention = new StringBuilder();
            for (String suggest : suggests) {
                TaskNeedAttentionEnum taskNeedAttentionEnum = TaskNeedAttentionEnum.codeMap.get(suggest);
                if (taskNeedAttentionEnum != null) {
                    String daily = taskNeedAttentionEnum.getDaily();
                    String defense = taskNeedAttentionEnum.getDefense();
                    dailyNeedAttention.append(daily).append("\n");
                    defenseNeedAttention.append(defense).append("\n");
                }
            }
            task.setDailyNeedAttention(dailyNeedAttention.toString());
            task.setDefenseNeedAttention(defenseNeedAttention.toString());
        }
        task.setDynamicRiskLevel(risk.getDynamicRiskLevel());
    }

    /**
     * 查询符合条件的任务派发清单列表
     *
     * @param bo 查询条件
     * @return 任务派发清单列表
     */
    public List<DzTaskDistListVo> queryList(DzTaskDistListBo bo) {
        validateListQueryAccess(bo);
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegionsForListQuery();
        ensureListAdRegionPermission(userAdRegions);
        QueryWrapper<DzTaskDistList> ew = buildListQueryWrapper(bo, resolveDynamicRiskLevels(bo), userAdRegions);
        List<DzTaskDistListVo> records = baseMapper.selectTaskDistListByQuery(ew);
        if (records.isEmpty()) {
            return records;
        }

        Map<String, SlopeUnitVo> slopeUnitMap = querySlopeUnitMap(records);

        List<Long> taskIds = records.stream()
                                    .map(DzTaskDistListVo::getId)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .toList();
        if (taskIds.isEmpty()) {
            return records;
        }

        List<DzTaskDistListRemark> taskRemarks = dzTaskDistListRemarkMapper.selectLatestByTaskIds(taskIds);
        Map<Long, DzTaskDistListRemark> latestTaskRemarkMap = taskRemarks.stream()
                                                                         .collect(Collectors.toMap(DzTaskDistListRemark::getTaskId, item -> item, (existing, replacement) -> existing));

        List<DzTaskDistListAddVo> listAdds = dzTaskDistListAddMapper.selectLatestByTaskIds(taskIds);
        Map<Long, DzTaskDistListAddVo> latestAddRemarkMap = listAdds.stream()
                                                                    .collect(Collectors.toMap(DzTaskDistListAddVo::getTaskId, item -> item, (existing, replacement) -> existing));

        List<Long> userIds = Stream.concat(
                                       taskRemarks.stream().map(DzTaskDistListRemark::getUserId),
                                       listAdds.stream().map(DzTaskDistListAddVo::getReportUserId)
                                   )
                                   .filter(Objects::nonNull)
                                   .distinct()
                                   .toList();
        List<SysUserVo> users = userIds.isEmpty() ? List.of() : sysUserService.selectUserByIds(userIds, null);
        Map<Long, String> nickNameMap = users.stream()
                                             .collect(Collectors.toMap(SysUserVo::getUserId, SysUserVo::getNickName, (a, b) -> a));

        records.forEach(record -> {
            record.setSlopeUnit(resolveSlopeUnitVo(slopeUnitMap, record.getUnitId()));

            DzTaskDistListRemark taskRemark = latestTaskRemarkMap.get(record.getId());
            if (taskRemark != null) {
                record.setTaskRemark(taskRemark.getRemark());
                record.setTaskRemarkCreateDate(taskRemark.getCreateDate());
                record.setTaskRemarkUserId(taskRemark.getUserId());
                record.setTaskRemarkUserName(nickNameMap.get(taskRemark.getUserId()));
            }

            DzTaskDistListAddVo addRemark = latestAddRemarkMap.get(record.getId());
            if (addRemark == null) {
                return;
            }
            record.setAddRemark(addRemark.getRemark());
            record.setAddRemarkCreateDate(addRemark.getCreateDate());
            record.setAddReportUserId(addRemark.getReportUserId());
            record.setAddReportUserName(nickNameMap.get(addRemark.getReportUserId()));
        });
        return records;
    }

    boolean appendCurrentUserAdRegionPermission(LambdaQueryWrapper<DzTaskDistList> lqw) {
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegions();
        if (userAdRegions == null) {
            return true;
        }
        return appendSlopeUnitAdRegionPermission(lqw, userAdRegions);
    }

     boolean appendSlopeUnitAdRegionPermission(AbstractWrapper<?, ?, ?> lqw, List<DzUserAdRegionVo> userAdRegions) {
        if (userAdRegions == null) {
            return true;
        }
        if (userAdRegions.isEmpty()) {
            return false;
        }
        String condition = buildSlopeUnitRegionPermissionSql(userAdRegions, "su");
        if (StringUtils.isBlank(condition)) {
            return false;
        }
        lqw.apply(
            "exists (select 1 from data_slope_unit su where su.id = unit_id and (" + condition + "))"
        );
        return true;
    }

     List<DzUserAdRegionVo> getCurrentUserAdRegions() {
        if (LoginHelper.isSuperAdmin()) {
            return null;
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }
        List<DzUserAdRegionVo> userAdRegions = dzUserAdRegionService.queryEffectiveListByUserId(userId)
                                                                    .stream()
                                                                    .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
                                                                    .toList();
        if (userAdRegions.isEmpty()) {
            throw new ServiceException("行政区划权限不足：当前用户未配置关联行政区划，无法访问任务");
        }
        return userAdRegions;
    }

     static void ensureListAdRegionPermission(List<DzUserAdRegionVo> userAdRegions) {
        if (userAdRegions == null) {
            return;
        }
        if (userAdRegions.isEmpty()) {
            throw new ServiceException("行政区划权限不足：当前用户关联的行政区划配置无效，无法查询任务");
        }
    }

     static ServiceException regionPermissionDeniedException(String actionLabel) {
        return new ServiceException("行政区划权限不足：任务所在斜坡单元不在您关联的行政区划范围内，无权" + actionLabel);
    }

     void validateListQueryAccess(DzTaskDistListBo bo) {
        service.normalizeRoleBasedListQuery(bo);
        if (bo == null) {
            return;
        }
        if (StringUtils.isNotBlank(bo.getUnitId())) {
            validateCurrentUserTaskAccess(bo.getUnitId(), "查看");
        }
        if (bo.getId() == null) {
            return;
        }
        DzTaskDistList task = baseMapper.selectById(bo.getId());
        if (task == null || Objects.equals(task.getDelete(), 1)) {
            return;
        }
        validateTaskViewAccess(task.getUnitId(), task.getSourceType(), task.getPlanType());
    }

     void validateTaskViewAccess(String unitId, Integer sourceType, Integer planType) {
        if (DzPushSmsDelegate.isDutyOfficer()) {
            service.validateTaskViewRoleAccess(sourceType, planType);
        }
        validateCurrentUserTaskAccess(unitId, "查看");
    }

    void validateCurrentUserTaskAccess(String unitId, String actionLabel) {
        List<DzUserAdRegionVo> userAdRegions = getCurrentUserAdRegions();
        if (userAdRegions == null) {
            return;
        }
        if (StringUtils.isBlank(unitId)) {
            throw new ServiceException("行政区划权限不足：任务未关联斜坡单元，无权" + actionLabel);
        }
        SlopeUnit slopeUnit = resolveSlopeUnitByTaskUnitId(unitId);
        if (slopeUnit == null) {
            throw new ServiceException("行政区划权限不足：任务关联的斜坡单元不存在，无权" + actionLabel);
        }
        if (!currentUserHasUnitPermission(unitId, userAdRegions)) {
            throw regionPermissionDeniedException(actionLabel);
        }
    }

     void validateDutyOfficerTaskAccess(String unitId) {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("角色权限不足：仅值班员允许执行该操作");
        }
        validateCurrentUserTaskAccess(unitId, "操作");
    }

    private boolean currentUserHasUnitPermission(String unitId, List<DzUserAdRegionVo> userAdRegions) {
        if (StringUtils.isBlank(unitId) || userAdRegions == null || userAdRegions.isEmpty()) {
            return false;
        }
        SlopeUnit slopeUnit = resolveSlopeUnitByTaskUnitId(unitId);
        if (slopeUnit == null) {
            return false;
        }
        for (DzUserAdRegionVo userAdRegion : userAdRegions) {
            String expectedCode = resolveSlopeUnitAdRegionCodeValue(userAdRegion, userAdRegion.getAdRegionLevel());
            if (StringUtils.isBlank(expectedCode)) {
                continue;
            }
            String actualCode = switch (userAdRegion.getAdRegionLevel()) {
                case 1 -> slopeUnit.getProvinceCode();
                case 2 -> slopeUnit.getCityCode();
                case 3 -> slopeUnit.getCountyCode();
                case 4 -> slopeUnit.getStreetCode();
                case 5 -> slopeUnit.getVillageCode();
                default -> null;
            };
            if (StringUtils.isNotBlank(actualCode) && expectedCode.equals(actualCode)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveSlopeUnitAdRegionCodeField(Integer adRegionLevel) {
        if (adRegionLevel == null) {
            return null;
        }
        return switch (adRegionLevel) {
            case 1 -> "province_code";
            case 2 -> "city_code";
            case 3 -> "county_code";
            case 4 -> "street_code";
            case 5 -> "village_code";
            default -> null;
        };
    }

    public static String resolveSlopeUnitAdRegionCodeValue(DzUserAdRegionVo userAdRegion, Integer adRegionLevel) {
        if (userAdRegion == null || adRegionLevel == null) {
            return null;
        }
        return userAdRegion.getAdRegionId();
    }

     String buildSlopeUnitRegionPermissionSql(List<DzUserAdRegionVo> userAdRegions, String alias) {
        return userAdRegions.stream()
                            .map(item -> buildSlopeUnitRegionCondition(item, alias))
                            .filter(StringUtils::isNotBlank)
                            .distinct()
                            .collect(Collectors.joining(" or "));
    }

     String buildSlopeUnitRegionCondition(DzUserAdRegionVo userAdRegion, String alias) {
        String field = resolveSlopeUnitAdRegionCodeField(userAdRegion.getAdRegionLevel());
        String value = resolveSlopeUnitAdRegionCodeValue(userAdRegion, userAdRegion.getAdRegionLevel());
        if (StringUtils.isBlank(field) || StringUtils.isBlank(value)) {
            return null;
        }
        return alias + "." + field + " = '" + value.replace("'", "''") + "'";
    }

    static LambdaQueryWrapper<DzTaskDistList> buildQueryWrapper(DzTaskDistListBo bo) {
        LambdaQueryWrapper<DzTaskDistList> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, DzTaskDistList::getId, bo.getId());
        lqw.eq(StringUtils.isNotBlank(bo.getUnitId()), DzTaskDistList::getUnitId, bo.getUnitId());
        lqw.eq(bo.getUserId() != null, DzTaskDistList::getUserId, bo.getUserId());
        lqw.eq(bo.getRiskId() != null, DzTaskDistList::getRiskId, bo.getRiskId());
        appendDynamicRiskLevelFilters(lqw, bo);
        lqw.eq(StringUtils.isNotBlank(bo.getSubmitRequire()), DzTaskDistList::getSubmitRequire, bo.getSubmitRequire());
        lqw.eq(StringUtils.isNotBlank(bo.getInspectionSuggestion()), DzTaskDistList::getInspectionSuggestion, bo.getInspectionSuggestion());
        lqw.eq(StringUtils.isNotBlank(bo.getInspectionSuggestionBackup()), DzTaskDistList::getInspectionSuggestionBackup, bo.getInspectionSuggestionBackup());
        lqw.eq(StringUtils.isNotBlank(bo.getScenePhoto()), DzTaskDistList::getScenePhoto, bo.getScenePhoto());
        lqw.eq(StringUtils.isNotBlank(bo.getTextRecord()), DzTaskDistList::getTextRecord, bo.getTextRecord());
        lqw.eq(bo.getStatus() != null, DzTaskDistList::getStatus, bo.getStatus());
        if (bo.getCreateDate() != null) {
            lqw.ge(DzTaskDistList::getCreateDate, DateUtil.beginOfDay(bo.getCreateDate()));
            lqw.le(DzTaskDistList::getCreateDate, DateUtil.endOfDay(bo.getCreateDate()));
        }
        lqw.eq(bo.getUpdateDate() != null, DzTaskDistList::getUpdateDate, bo.getUpdateDate());
        lqw.eq(bo.getCheckTime() != null, DzTaskDistList::getCheckTime, bo.getCheckTime());
        lqw.eq(StringUtils.isNotBlank(bo.getCheckCenter()), DzTaskDistList::getCheckCenter, bo.getCheckCenter());
        lqw.eq(bo.getSubmitTime() != null, DzTaskDistList::getSubmitTime, bo.getSubmitTime());
        lqw.eq(bo.getSourceType() != null, DzTaskDistList::getSourceType, bo.getSourceType());
        lqw.eq(bo.getReportId() != null, DzTaskDistList::getReportId, bo.getReportId());
        lqw.eq(bo.getHandleId() != null, DzTaskDistList::getHandleId, bo.getHandleId());
        lqw.eq(bo.getDefId() != null, DzTaskDistList::getDefId, bo.getDefId());
        lqw.like(StringUtils.isNotBlank(bo.getPlanName()), DzTaskDistList::getPlanName, bo.getPlanName());
        lqw.eq(bo.getPlanType() != null, DzTaskDistList::getPlanType, bo.getPlanType());
        lqw.eq(StringUtils.isNotBlank(bo.getResponsiblePerson()), DzTaskDistList::getResponsiblePerson, bo.getResponsiblePerson());
        lqw.eq(StringUtils.isNotBlank(bo.getResponsiblePersonPhone()), DzTaskDistList::getResponsiblePersonPhone, bo.getResponsiblePersonPhone());
        lqw.eq(bo.getDelete() != null, DzTaskDistList::getDelete, bo.getDelete());
        lqw.eq(bo.getOverdue() != null, DzTaskDistList::getOverdue, bo.getOverdue());
        lqw.eq(bo.getQuotaConsumed() != null, DzTaskDistList::getQuotaConsumed, bo.getQuotaConsumed());
        lqw.eq(bo.getLastRemindTime() != null, DzTaskDistList::getLastRemindTime, bo.getLastRemindTime());
        lqw.eq(bo.getReminderCount() != null, DzTaskDistList::getReminderCount, bo.getReminderCount());
        lqw.eq(StringUtils.isNotBlank(bo.getCloseReason()), DzTaskDistList::getCloseReason, bo.getCloseReason());
        lqw.eq(bo.getClosedTime() != null, DzTaskDistList::getClosedTime, bo.getClosedTime());
        appendSlopeUnitPilotAreaFilters(lqw, bo.getPilotArea1(), bo.getPilotArea2());
        List<Integer> sourceTypeList = bo.getSourceTypeList();
        if (sourceTypeList != null && !sourceTypeList.isEmpty()) {
            boolean includeEmergencyMonitoring = Objects.equals(bo.getIsEmergency(), 1)
                && !sourceTypeList.contains(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
            if (includeEmergencyMonitoring) {
                lqw.and(wrapper -> wrapper.in(DzTaskDistList::getSourceType, sourceTypeList)
                                          .or(emergencyWrapper -> emergencyWrapper.eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)
                                                                                  .in(DzTaskDistList::getPlanType,
                                                                                      DzTaskDistList.PLAN_TYPE_MONITORING,
                                                                                      DzTaskDistList.PLAN_TYPE_INSTRUMENT_MONITORING)));
            } else {
                lqw.in(DzTaskDistList::getSourceType, sourceTypeList);
            }
        }
        lqw.in(bo.getStatusList() != null && !bo.getStatusList().isEmpty(), DzTaskDistList::getStatus, bo.getStatusList());
        return lqw;
    }

    private static void appendDynamicRiskLevelFilters(LambdaQueryWrapper<DzTaskDistList> lqw, DzTaskDistListBo bo) {
        List<Integer> dynamicRiskLevels = Stream.concat(
                                                    Stream.of(bo.getDynamicRiskLevel()),
                                                    bo.getDynamicRiskLevels() == null ? Stream.empty() : bo.getDynamicRiskLevels().stream()
                                                )
                                                .filter(Objects::nonNull)
                                                .distinct()
                                                .toList();
        if (dynamicRiskLevels.isEmpty()) {
            return;
        }
        String placeholders = IntStream.range(0, dynamicRiskLevels.size())
                                       .mapToObj(i -> "{" + i + "}")
                                       .collect(Collectors.joining(", "));
        lqw.apply(
            "exists (select 1 from dz_risk_assessment ra where ra.id = risk_id and ra.dynamic_risk_level in (" + placeholders + "))",
            dynamicRiskLevels.toArray()
        );
    }

    private static void appendSlopeUnitPilotAreaFilters(QueryWrapper<DzTaskDistList> lqw,
                                                        Integer pilotArea1,
                                                        Integer pilotArea2) {
        lqw.apply(pilotArea1 != null,
            "exists (select 1 from data_slope_unit su where su.id = unit_id and su.pilot_area_1 = {0})",
            pilotArea1);
        lqw.apply(pilotArea2 != null,
            "exists (select 1 from data_slope_unit su where su.id = unit_id and su.pilot_area_2 = {0})",
            pilotArea2);
    }

    private static void appendSlopeUnitPilotAreaFilters(LambdaQueryWrapper<DzTaskDistList> lqw,
                                                        Integer pilotArea1,
                                                        Integer pilotArea2) {
        lqw.apply(pilotArea1 != null,
            "exists (select 1 from data_slope_unit su where su.id = unit_id and su.pilot_area_1 = {0})",
            pilotArea1);
        lqw.apply(pilotArea2 != null,
            "exists (select 1 from data_slope_unit su where su.id = unit_id and su.pilot_area_2 = {0})",
            pilotArea2);
    }

}
