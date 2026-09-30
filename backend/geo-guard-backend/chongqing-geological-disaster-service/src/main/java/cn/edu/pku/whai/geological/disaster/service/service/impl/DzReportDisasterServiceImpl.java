package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterFeedbackBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReportDisasterStatVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzReportDisasterService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import cn.edu.pku.whai.geological.disaster.service.utils.ReportInfoJsonUtils;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 报灾管理Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzReportDisasterServiceImpl implements IDzReportDisasterService {

    private final DzReportDisasterMapper baseMapper;
    private final DzTaskDistListMapper taskDistListMapper;
    private final ISlopeUnitService slopeUnitService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final ISysUserService sysUserService;
    private final SmsSendService smsSendService;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final ITaskSmsContentService taskSmsContentService;
    private final DisasterDetailedAddressResolver disasterDetailedAddressResolver;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;
    private final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;
    private final Environment environment;


    /**
     * 查询报灾管理
     *
     * @param id 主键
     * @return 报灾管理
     */
    @Override
    public DzReportDisasterVo queryById(Long id) {
        DzReportDisasterVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            enrichReportDisasterVos(List.of(vo));
        }
        return vo;
    }

    /**
     * 分页查询报灾管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 报灾管理分页列表
     */
    @Override
    public TableDataInfo<DzReportDisasterVo> queryPageList(DzReportDisasterBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzReportDisaster> lqw = buildQueryWrapper(bo);
        AdRegionPermissionContext permissionContext = appendCurrentUserAdRegionPermission(lqw);
        if (!permissionContext.allowed()) {
            return TableDataInfo.build(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize(), 0));
        }
        Page<DzReportDisasterVo> result = baseMapper.selectListPage(pageQuery.build(), lqw);
        List<DzReportDisasterVo> records = result.getRecords();
        enrichReportDisasterVos(records);
        return new TableDataInfo<>(records, result.getTotal());
    }

    /**
     * 查询符合条件的报灾管理列表
     *
     * @param bo 查询条件
     * @return 报灾管理列表
     */
    @Override
    public List<DzReportDisasterVo> queryList(DzReportDisasterBo bo) {
        LambdaQueryWrapper<DzReportDisaster> lqw = buildQueryWrapper(bo);
        List<DzReportDisasterVo> records = baseMapper.selectVoList(lqw);
        enrichReportDisasterVos(records);
        return records;
    }

    private LambdaQueryWrapper<DzReportDisaster> buildQueryWrapper(DzReportDisasterBo bo) {
        Map<String, Object> params = bo.getParams() == null ? Map.of() : bo.getParams();
        LambdaQueryWrapper<DzReportDisaster> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, DzReportDisaster::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getUserName()), DzReportDisaster::getUserName, bo.getUserName());
        lqw.eq(StringUtils.isNotBlank(bo.getUserPhone()), DzReportDisaster::getUserPhone, bo.getUserPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getUserRole()), DzReportDisaster::getUserRole, bo.getUserRole());

        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzReportDisaster::getCheckTime, params.get("beginTime"), params.get("endTime"));
        }

        lqw.eq(StringUtils.isNotBlank(bo.getCheckCenter()), DzReportDisaster::getCheckCenter, bo.getCheckCenter());
        lqw.like(StringUtils.isNotBlank(bo.getCheckCenterLocation()), DzReportDisaster::getCheckCenterLocation, bo.getCheckCenterLocation());
        lqw.like(StringUtils.isNotBlank(bo.getDetailedAddress()), DzReportDisaster::getDetailedAddress, bo.getDetailedAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getPhotos()), DzReportDisaster::getPhotos, bo.getPhotos());
        lqw.eq(StringUtils.isNotBlank(bo.getSceneTextRecord()), DzReportDisaster::getSceneTextRecord, bo.getSceneTextRecord());
        lqw.eq(bo.getAiRiskLevel() != null, DzReportDisaster::getAiRiskLevel, bo.getAiRiskLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getAiRiskLabel()), DzReportDisaster::getAiRiskLabel, bo.getAiRiskLabel());
        lqw.eq(bo.getAiReportDetail() != null, DzReportDisaster::getAiReportDetail, bo.getAiReportDetail());
        lqw.eq(bo.getManualRiskLevel() != null, DzReportDisaster::getManualRiskLevel, bo.getManualRiskLevel());
        lqw.like(StringUtils.isNotBlank(bo.getManualRiskRemark()), DzReportDisaster::getManualRiskRemark, bo.getManualRiskRemark());
        lqw.eq(bo.getStatus() != null, DzReportDisaster::getStatus, bo.getStatus());
        lqw.eq(bo.getSourceType() != null, DzReportDisaster::getSourceType, bo.getSourceType());
        lqw.eq(bo.getTaskId() != null, DzReportDisaster::getTaskId, bo.getTaskId());
        lqw.eq(bo.getCreateDate() != null, DzReportDisaster::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, DzReportDisaster::getUpdateDate, bo.getUpdateDate());
        return lqw;
    }

    private AdRegionPermissionContext appendCurrentUserAdRegionPermission(LambdaQueryWrapper<DzReportDisaster> lqw) {
        if (isLocalProfile()) {
            return new AdRegionPermissionContext(true, null, 0, false);
        }
        boolean superAdmin = LoginHelper.isSuperAdmin();
        if (superAdmin) {
            return new AdRegionPermissionContext(true, LoginHelper.getUserId(), 0, true);
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return new AdRegionPermissionContext(false, null, 0, false);
        }
        List<DzUserAdRegionVo> userAdRegions = dzUserAdRegionService.queryEffectiveListByUserId(userId)
                                                                    .stream()
                                                                    .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
                                                                    .toList();
        if (userAdRegions.isEmpty()) {
            return new AdRegionPermissionContext(false, userId, 0, false);
        }
        lqw.apply("""
            exists (
                select 1
                from data_ad_region ar
                where ar.id in (%s)
                  and ar.geom is not null
                  and check_center is not null
                  and ST_Contains(ar.geom, ST_GeomFromText(check_center, 4326))
            )
            """.formatted(buildQuotedInValues(userAdRegions.stream().map(DzUserAdRegionVo::getAdRegionId).toList())));
        return new AdRegionPermissionContext(true, userId, userAdRegions.size(), false);
    }

    private boolean isLocalProfile() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    private String buildQuotedInValues(List<String> values) {
        return values.stream()
                     .filter(StringUtils::isNotBlank)
                     .distinct()
                     .map(value -> "'" + value.replace("'", "''") + "'")
                     .collect(java.util.stream.Collectors.joining(","));
    }

    /**
     * 新增报灾管理
     *
     * @param bo 报灾管理
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzReportDisasterBo bo) {
        DzReportDisaster add = MapstructUtils.convert(bo, DzReportDisaster.class);
        if (add.getProcessType() == null) {
            Integer processType = resolveProcessTypeBySourceType(add.getSourceType());
            if (processType == null) {
                throw new ServiceException("报灾来源类型为空或不支持，无法确定处置路线");
            }
            add.setProcessType(processType);
        }
        String detailedAddress = disasterDetailedAddressResolver.resolve(add.getCheckCenter());
        if (StringUtils.isNotBlank(detailedAddress)) {
            add.setDetailedAddress(detailedAddress);
        }
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改报灾管理
     *
     * @param bo 报灾管理
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzReportDisasterBo bo) {
        DzReportDisaster update = MapstructUtils.convert(bo, DzReportDisaster.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzReportDisaster entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除报灾管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private Integer resolveProcessTypeBySourceType(Integer sourceType) {
        if (Objects.equals(sourceType, REPORT_SOURCE_TASK_FEEDBACK)) {
            return DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN;
        }
        if (Objects.equals(sourceType, REPORT_SOURCE_PUBLIC_REPORT)) {
            return DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY;
        }
        return null;
    }

    private static final String INSPECTING_REQUIRE = "2小时内上传现场图像（≥2张）及文字记录";
    private static final Integer REPORT_SOURCE_TASK_FEEDBACK = 1;
    private static final Integer REPORT_SOURCE_PUBLIC_REPORT = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DzReportDisasterHandleVo handle(Long id) {
        DzReportDisaster dzReportDisaster = baseMapper.selectByIdForUpdate(id);
        if (dzReportDisaster == null) {
            throw new ServiceException("数据不存在");
        }

        Integer status = dzReportDisaster.getStatus();
        if (status > 1) {
            throw new ServiceException("数据已处理");
        }

        DzTaskDistList dzTask;
        if (Objects.equals(dzReportDisaster.getProcessType(), DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY)) {
            dzTask = getOrCreateTaskFromReportDisaster(dzReportDisaster, DzTaskDistList.SOURCE_TYPE_REPORT,
                SlopeUnitDispatchRole.INSPECTOR, ReportTaskScene.PUBLIC_REPORT);
            recordPublicReportTaskDispatchNode(dzReportDisaster, dzTask);
            dzReportDisaster.setStatus(3);
        } else if (Objects.equals(dzReportDisaster.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)) {
            dzReportDisaster.setStatus(2);
            dzTask = getOrCreateTaskFromReportDisaster(dzReportDisaster, DzTaskDistList.SOURCE_TYPE_REPORT,
                SlopeUnitDispatchRole.ADMIN_USER, ReportTaskScene.AI_VERIFY);
            Long reportToTownNodeId = recordReportToTownTaskNodeIfRequired(dzReportDisaster, dzTask);
            recordAiVerifyTaskDispatchNode(dzReportDisaster, dzTask, reportToTownNodeId);
            dzReportDisaster.setStatus(3);
        } else {
            throw new ServiceException("不支持的报灾处置路线");
        }
        baseMapper.updateById(dzReportDisaster);
        notifyAiHostingOverview("report_status_changed", dzReportDisaster.getId(), dzReportDisaster.getTaskId());
        DzReportDisasterHandleVo vo = new DzReportDisasterHandleVo();
        vo.setDispatchTargetName(dzTask.getResponsiblePerson());
        appendSlopeUnitAdRegion(vo, dzTask.getUnitId());

        return vo;
    }

    private DzTaskDistList getOrCreateTaskFromReportDisaster(DzReportDisaster dzReportDisaster, Integer taskSourceType,
                                                             SlopeUnitDispatchRole dispatchRole,
                                                             ReportTaskScene taskScene) {
        DzTaskDistList existedTask = selectLatestReportTask(dzReportDisaster == null ? null : dzReportDisaster.getId());
        if (existedTask != null) {
            return existedTask;
        }
        return createTaskFromReportDisaster(dzReportDisaster, taskSourceType, dispatchRole, taskScene);
    }

    private DzTaskDistList selectLatestReportTask(Long reportId) {
        if (reportId == null) {
            return null;
        }
        return taskDistListMapper.selectOne(
            Wrappers.<DzTaskDistList>lambdaQuery()
                .eq(DzTaskDistList::getReportId, reportId)
                .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_REPORT)
                .eq(DzTaskDistList::getDelete, 0)
                .orderByDesc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
                .last("limit 1")
        );
    }

    private DzTaskDistList createTaskFromReportDisaster(DzReportDisaster dzReportDisaster, Integer taskSourceType,
                                                        SlopeUnitDispatchRole dispatchRole,
                                                        ReportTaskScene taskScene) {
        String slopeUnitId = resolveReportSlopeUnitId(dzReportDisaster);
        DzTaskDistList dzTask = new DzTaskDistList();
        dzTask.setUnitId(slopeUnitId);
        dzTask.setCheckCenter(StringUtils.trim(dzReportDisaster.getCheckCenter()));
        SlopeUnitGridMemberRelationVo slopeUnitGridMemberRelationVo = slopeUnitGridMemberRelationService.queryByUnitId(slopeUnitId);
        ResolvedTaskAssignee assignee = resolveSlopeUnitDispatchAssignee(slopeUnitGridMemberRelationVo, dispatchRole);
        dzTask.setUserId(assignee.userId());
        dzTask.setResponsiblePerson(assignee.userName());
        dzTask.setResponsiblePersonPhone(assignee.phoneNumber());
        dzTask.setRiskId(findLatestRiskIdBySlopeUnitId(slopeUnitId));
        String detailedAddress = dzReportDisaster.getDetailedAddress();
        if (StringUtils.isBlank(detailedAddress)) {
            detailedAddress = slopeUnitService.queryById(slopeUnitId).getDetailedAddress();
        }
        dzTask.setDetailedAddress(detailedAddress);
        dzTask.setSourceType(taskSourceType);
        dzTask.setInspectionSuggestion("请尽快前往目标地点巡逻查看");
        dzTask.setInspectionSuggestionBackup("请尽快前往目标地点巡逻查看");
        dzTask.setSubmitRequire(INSPECTING_REQUIRE);
        dzTask.setReportId(dzReportDisaster.getId());
        dzTask.setReportInfo(ReportInfoJsonUtils.toLimitedJson(dzReportDisaster));
        dzTask.setRelatedTaskId(dzReportDisaster.getTaskId());
        dzTask.setTaskSource(buildReportTaskSource(dzReportDisaster, taskScene));
        DzTaskDistListServiceImpl.prepareNewTaskDefaults(dzTask, new Date(), true);
        dzTask.setTaskType(resolveReportTaskType(dzReportDisaster));
        taskSmsContentService.populateSmsContent(dzTask);
        taskDistListMapper.insert(dzTask);
        return dzTask;
    }

    private String resolveReportTaskType(DzReportDisaster report) {
        if (report == null) {
            throw new ServiceException("报灾记录为空，无法确定任务类型");
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY)) {
            return DzTaskDistList.TASK_TYPE_PUBLIC_REPORT;
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)) {
            return DzTaskDistList.TASK_TYPE_AI_VERIFY;
        }
        throw new ServiceException("未知报灾处置路线，禁止派单");
    }

    private ResolvedTaskAssignee resolveSlopeUnitDispatchAssignee(SlopeUnitGridMemberRelationVo relationVo,
                                                                  SlopeUnitDispatchRole dispatchRole) {
        if (relationVo == null) {
            throw new ServiceException("当前斜坡单元未配置网格员关系，无法下发任务");
        }
        return switch (dispatchRole) {
            case INSPECTOR -> resolveInspectorAssignee(relationVo);
            case ADMIN_USER -> resolveRelationAssignee(relationVo.getAdminUser(), relationVo.getAdminUserPhone(), "乡自规所所长");
            case ASSISTANT_MANAGER -> resolveRelationAssignee(relationVo.getAssistantManager(), relationVo.getAssistantManagerPhone(), "协管员");
        };
    }

    private ResolvedTaskAssignee resolveInspectorAssignee(SlopeUnitGridMemberRelationVo relationVo) {
        if (StringUtils.isNotBlank(relationVo.getInspector())) {
            return resolveRelationAssignee(relationVo.getInspector(), relationVo.getInspectorPhone(), "巡查员");
        }
        return resolveRelationAssignee(relationVo.getSpecialManager(), relationVo.getSpecialManagerPhone(), "村支书");
    }

    private ResolvedTaskAssignee resolveRelationAssignee(String name, String phoneNumber, String roleName) {
        if (StringUtils.isBlank(phoneNumber)) {
            throw new ServiceException("当前斜坡单元未绑定" + roleName + "，无法下发任务");
        }
        String normalizedPhone = phoneNumber.trim();
        SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(normalizedPhone);
        if (sysUserVo == null || sysUserVo.getUserId() == null) {
            throw new ServiceException(roleName + "未注册系统账号，手机号: " + normalizedPhone);
        }
        return new ResolvedTaskAssignee(
            sysUserVo.getUserId(),
            StringUtils.isNotBlank(name) ? name : sysUserVo.getUserName(),
            normalizedPhone
        );
    }

    private Long recordReportToTownTaskNodeIfRequired(DzReportDisaster report, DzTaskDistList task) {
        if (report == null || task == null
            || !Objects.equals(report.getSourceType(), REPORT_SOURCE_PUBLIC_REPORT)
            || report.getTaskId() != null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(
            TaskProcessBizTypeEnum.REPORT.getCode(), report.getId());
        DzTaskProcessChainNode rootNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
            TaskProcessBizTypeEnum.REPORT.getCode(), report.getId(),
            TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName(),
            TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        if (StringUtils.isBlank(chainId) || rootNode == null || rootNode.getId() == null) {
            throw new ServiceException("报送乡镇未找到巡查员任务报灾根节点, reportId=" + report.getId());
        }
        return taskProcessChainNodeService.recordBizNode(
            chainId,
            TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName(),
            TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getTriggerReason(),
            TaskProcessBizTypeEnum.TASK.getCode(),
            task.getId(),
            task.getId(),
            task.getUserId(),
            task.getResponsiblePerson(),
            TaskProcessSourceTypeEnum.fromReportSourceType(report.getSourceType()).getCode(),
            rootNode.getId(),
            null,
            null);
    }

    private void recordAiVerifyTaskDispatchNode(DzReportDisaster report, DzTaskDistList task, Long parentNodeId) {
        if (task == null || report == null || report.getId() == null) {
            return;
        }
        String chainId;
        Long resolvedParentNodeId = parentNodeId;
        if (Objects.equals(report.getSourceType(), REPORT_SOURCE_TASK_FEEDBACK)) {
            DzTaskProcessChainNode feedbackReportNode = taskProcessChainNodeService.resolveSavedNodeByBizAndCategory(
                TaskProcessBizTypeEnum.REPORT.getCode(), report.getId(),
                TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
            if (feedbackReportNode == null || feedbackReportNode.getId() == null
                || StringUtils.isBlank(feedbackReportNode.getChainId())) {
                throw new ServiceException("下发AI险情核实任务未找到任务反馈报告前驱节点, reportId=" + report.getId());
            }
            chainId = feedbackReportNode.getChainId();
            resolvedParentNodeId = feedbackReportNode.getId();
        } else if (Objects.equals(report.getSourceType(), REPORT_SOURCE_PUBLIC_REPORT)) {
            chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(
                TaskProcessBizTypeEnum.REPORT.getCode(), report.getId());
            if (StringUtils.isBlank(chainId) || resolvedParentNodeId == null) {
                throw new ServiceException("下发AI险情核实任务未找到报送乡镇前驱节点, reportId=" + report.getId());
            }
        } else {
            throw new ServiceException("不支持的报灾来源类型");
        }
        taskProcessChainNodeService.recordTaskNode(
            chainId,
            TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getTriggerReason(),
            task,
            resolvedParentNodeId,
            TaskProcessNodeCategoryEnum.TASK_PUSH.getCode(),
            null);
    }

    private void recordPublicReportTaskDispatchNode(DzReportDisaster report, DzTaskDistList task) {
        if (task == null) {
            return;
        }
        String chainId = resolveOrCreatePublicReportRootChainId(report);
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("下发群众报灾任务未找到群众报灾根链, reportId=" + report.getId());
        }
        taskProcessChainNodeService.recordBizNode(chainId,
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(),
            report.getId(),
            null,
            report.getUserId(),
            report.getUserName(),
            TaskProcessSourceTypeEnum.fromReportSourceType(report.getSourceType()).getCode(),
            null,
            null,
            TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        taskProcessChainNodeService.recordTaskNode(chainId, TaskProcessChainNodeTextEnum.PUBLIC_REPORT_TASK_DISPATCH.getLinkName(),
            TaskProcessChainNodeTextEnum.PUBLIC_REPORT_TASK_DISPATCH.getTriggerReason(), task);
    }

    private String resolveOrCreatePublicReportRootChainId(DzReportDisaster report) {
        if (report == null || report.getId() == null) {
            return null;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(
            TaskProcessBizTypeEnum.REPORT.getCode(), report.getId());
        if (StringUtils.isNotBlank(chainId)) {
            return chainId;
        }
        chainId = taskProcessChainNodeService.generateChainId();
        taskProcessChainNodeService.recordBizNode(chainId,
            TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName(),
            TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(),
            report.getId(),
            null,
            report.getUserId(),
            report.getUserName(),
            TaskProcessSourceTypeEnum.fromReportSourceType(report.getSourceType()).getCode(),
            null,
            null,
            TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        return chainId;
    }

    private void recordNode(String chainId, String linkName, String triggerReason, Long operatorId, String operatorName,
                            Date triggerTime, Integer bizType, Long bizId, Long taskId, Integer sourceType) {
        taskProcessChainNodeService.recordBizNode(chainId, linkName, triggerReason, bizType, bizId, taskId,
            operatorId, operatorName, sourceType);
    }

    private String resolveReportSlopeUnitId(DzReportDisaster dzReportDisaster) {
        String checkCenter = dzReportDisaster == null ? null : StringUtils.trim(dzReportDisaster.getCheckCenter());
        if (StringUtils.isBlank(checkCenter)) {
            throw new ServiceException("打卡地点为空，无法下发任务");
        }
        SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(checkCenter);
        if (slopeUnit == null || StringUtils.isBlank(slopeUnit.getId())) {
            throw new ServiceException("本次打卡地点未匹配到斜坡单元，无法下发任务");
        }
        return slopeUnit.getId();
    }

    private String buildReportTaskSource(DzReportDisaster report, ReportTaskScene taskScene) {
        String reporter = joinUserRoleAndName(report.getUserRole(), report.getUserName());
        String prefix = StringUtils.isBlank(reporter) ? "" : reporter;
        if (taskScene == ReportTaskScene.AI_VERIFY) {
            return prefix + "任务反馈报告后下发AI险情核实任务";
        }
        return prefix + "群众报灾后下发群众报灾任务";
    }

    private void appendSlopeUnitAdRegion(DzReportDisasterHandleVo vo, String slopeUnitId) {
        if (vo == null || StringUtils.isBlank(slopeUnitId)) {
            return;
        }
        SlopeUnitVo slopeUnitVo = slopeUnitService.queryById(slopeUnitId.trim());
        if (slopeUnitVo == null) {
            return;
        }
        vo.setCounty(slopeUnitVo.getCounty());
        vo.setStreet(slopeUnitVo.getStreet());
        vo.setVillage(slopeUnitVo.getVillage());
    }

    private void enrichReportDisasterVos(List<DzReportDisasterVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, DzTaskDistList> latestTaskByReportId = buildLatestTaskByReportId(records);
        Map<Long, DzTaskDistList> taskByTaskId = buildTaskByTaskId(records);
        Map<Long, Integer> processTypeByReportId = buildProcessTypeByReportId(records);
        Map<Long, String> slopeUnitIdByReportId = new HashMap<>();
        Map<String, String> slopeUnitIdByCheckCenter = new HashMap<>();
        Set<String> unresolvedCheckCenters = new LinkedHashSet<>();
        Set<String> slopeUnitIds = new LinkedHashSet<>();

        for (DzReportDisasterVo record : records) {
            if (record == null) {
                continue;
            }
            DzTaskDistList dispatchTask = latestTaskByReportId.get(record.getId());
            if (dispatchTask == null && record.getTaskId() != null) {
                dispatchTask = taskByTaskId.get(record.getTaskId());
            }
            if (dispatchTask != null) {
                String unitId = normalizeSlopeUnitId(dispatchTask.getUnitId());
                if (StringUtils.isNotBlank(unitId)) {
                    slopeUnitIdByReportId.put(record.getId(), unitId);
                    slopeUnitIds.add(unitId);
                    continue;
                }
            }
            if (StringUtils.isNotBlank(record.getCheckCenter())) {
                unresolvedCheckCenters.add(record.getCheckCenter().trim());
            }
        }
        for (String checkCenter : unresolvedCheckCenters) {
            SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(checkCenter);
            String slopeUnitId = slopeUnit == null ? null : normalizeSlopeUnitId(slopeUnit.getId());
            if (StringUtils.isNotBlank(slopeUnitId)) {
                slopeUnitIdByCheckCenter.put(checkCenter, slopeUnitId);
                slopeUnitIds.add(slopeUnitId);
            }
        }

        for (DzReportDisasterVo record : records) {
            if (record == null || slopeUnitIdByReportId.containsKey(record.getId()) || StringUtils.isBlank(record.getCheckCenter())) {
                continue;
            }
            String slopeUnitId = slopeUnitIdByCheckCenter.get(record.getCheckCenter().trim());
            if (StringUtils.isNotBlank(slopeUnitId)) {
                slopeUnitIdByReportId.put(record.getId(), slopeUnitId);
            }
        }
        Map<String, SlopeUnitVo> slopeUnitVoMap = slopeUnitIds.isEmpty()
            ? Map.of()
            : slopeUnitService.queryNoWktByIds(List.copyOf(slopeUnitIds)).stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotBlank(item.getId()))
                .collect(java.util.stream.Collectors.toMap(item -> item.getId().trim(), item -> item, (left, right) -> left, HashMap::new));
        Map<String, SlopeUnitGridMemberRelationVo> relationVoMap = buildSlopeUnitRelationMap(slopeUnitIds);
        for (DzReportDisasterVo record : records) {
            if (record == null) {
                continue;
            }
            String slopeUnitId = slopeUnitIdByReportId.get(record.getId());
            if (StringUtils.isBlank(slopeUnitId)) {
                continue;
            }
            record.setDispatchTargetName(resolveDispatchTargetName(processTypeByReportId.get(record.getId()), relationVoMap.get(slopeUnitId)));
            appendSlopeUnitAdRegion(record, slopeUnitVoMap.get(slopeUnitId));
        }
    }

    private Map<Long, Integer> buildProcessTypeByReportId(List<DzReportDisasterVo> records) {
        List<Long> reportIds = records.stream()
            .filter(Objects::nonNull)
            .map(DzReportDisasterVo::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        List<DzReportDisaster> reports = baseMapper.selectList(
                Wrappers.<DzReportDisaster>lambdaQuery()
                    .select(DzReportDisaster::getId, DzReportDisaster::getProcessType, DzReportDisaster::getSourceType)
                    .in(DzReportDisaster::getId, reportIds)
            );
        if (reports == null || reports.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> processTypeByReportId = new HashMap<>();
        for (DzReportDisaster report : reports) {
            if (report == null || report.getId() == null) {
                continue;
            }
            Integer processType = report.getProcessType();
            if (processType == null) {
                processType = resolveProcessTypeBySourceType(report.getSourceType());
            }
            if (processType != null) {
                processTypeByReportId.putIfAbsent(report.getId(), processType);
            }
        }
        return processTypeByReportId;
    }

    private Map<String, SlopeUnitGridMemberRelationVo> buildSlopeUnitRelationMap(Set<String> slopeUnitIds) {
        if (slopeUnitIds == null || slopeUnitIds.isEmpty()) {
            return Map.of();
        }
        Map<String, SlopeUnitGridMemberRelationVo> relationVoMap = new HashMap<>();
        for (String slopeUnitId : slopeUnitIds) {
            String normalizedSlopeUnitId = normalizeSlopeUnitId(slopeUnitId);
            if (StringUtils.isBlank(normalizedSlopeUnitId) || relationVoMap.containsKey(normalizedSlopeUnitId)) {
                continue;
            }
            SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(normalizedSlopeUnitId);
            if (relationVo != null) {
                relationVoMap.put(normalizedSlopeUnitId, relationVo);
            }
        }
        return relationVoMap;
    }

    private String resolveDispatchTargetName(Integer processType, SlopeUnitGridMemberRelationVo relationVo) {
        if (relationVo == null) {
            return null;
        }
        if (DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY.equals(processType)) {
            return StringUtils.isNotBlank(relationVo.getInspector())
                ? relationVo.getInspector()
                : relationVo.getSpecialManager();
        }
        if (DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN.equals(processType)) {
            return relationVo.getAdminUser();
        }
        return null;
    }

    private Map<Long, DzTaskDistList> buildLatestTaskByReportId(List<DzReportDisasterVo> records) {
        List<Long> reportIds = records.stream()
            .map(DzReportDisasterVo::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return taskDistListMapper.selectList(
                Wrappers.<DzTaskDistList>lambdaQuery()
                    .in(DzTaskDistList::getReportId, reportIds)
                    .orderByDesc(DzTaskDistList::getCreateDate, DzTaskDistList::getId)
            ).stream()
            .filter(task -> task.getReportId() != null)
            .collect(java.util.stream.Collectors.toMap(
                DzTaskDistList::getReportId,
                task -> task,
                (left, right) -> Comparator
                    .comparing(DzTaskDistList::getCreateDate, Comparator.nullsLast(Date::compareTo))
                    .thenComparing(DzTaskDistList::getId, Comparator.nullsLast(Long::compareTo))
                    .compare(left, right) >= 0 ? left : right,
                HashMap::new
            ));
    }

    private Map<Long, DzTaskDistList> buildTaskByTaskId(List<DzReportDisasterVo> records) {
        List<Long> taskIds = records.stream()
            .map(DzReportDisasterVo::getTaskId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return taskDistListMapper.selectList(
                Wrappers.<DzTaskDistList>lambdaQuery()
                    .in(DzTaskDistList::getId, taskIds)
            ).stream()
            .filter(task -> task.getId() != null)
            .collect(java.util.stream.Collectors.toMap(DzTaskDistList::getId, task -> task, (left, right) -> left));
    }

    private String normalizeSlopeUnitId(String slopeUnitId) {
        return StringUtils.isBlank(slopeUnitId) ? null : slopeUnitId.trim();
    }

    private void appendSlopeUnitAdRegion(DzReportDisasterVo vo, SlopeUnitVo slopeUnitVo) {
        if (vo == null || slopeUnitVo == null) {
            return;
        }
        vo.setCounty(slopeUnitVo.getCounty());
        vo.setStreet(slopeUnitVo.getStreet());
        vo.setVillage(slopeUnitVo.getVillage());
    }

    private record AdRegionPermissionContext(boolean allowed, Long userId, int adRegionCount, boolean superAdmin) {
    }

    private String buildTechAssistanceTaskSource(DzReportDisaster report, String slopeUnitId) {
        return joinUserRoleAndName(report.getUserRole(), report.getUserName())
            + "在"
            + DzTaskDistListServiceImpl.normalizeSlopeUnitId(slopeUnitId)
            + "号斜坡单元发现地灾迹象，申请技术协查";
    }

    private String joinUserRoleAndName(String userRole, String userName) {
        return java.util.stream.Stream.of(userRole, userName)
                                      .filter(StringUtils::isNotBlank)
                                      .map(String::trim)
                                      .collect(java.util.stream.Collectors.joining());
    }

    private enum SlopeUnitDispatchRole {
        INSPECTOR,
        ADMIN_USER,
        ASSISTANT_MANAGER
    }

    private enum ReportTaskScene {
        PUBLIC_REPORT,
        AI_VERIFY
    }

    private record ResolvedTaskAssignee(Long userId, String userName, String phoneNumber) {
    }

    private Long findLatestRiskIdBySlopeUnitId(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
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

    @Override
    public ReportDisasterStatVo stat() {
        return baseMapper.stat();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeReport(Long id) {
        DzReportDisaster existed = baseMapper.selectById(id);
        if (existed == null) {
            throw new ServiceException("报灾记录不存在");
        }
        DzReportDisaster dzReportDisaster = new DzReportDisaster();
        dzReportDisaster.setId(id);
        dzReportDisaster.setStatus(3);
        baseMapper.updateById(dzReportDisaster);
        recordDisasterDangerClosedNode(existed);
        notifyAiHostingOverview("report_closed", id, null);
    }

    private void recordDisasterDangerClosedNode(DzReportDisaster report) {
        DzTaskProcessChainNode parent = resolveDisasterDangerClosedParentNode(report);
        if (parent == null) {
            log.warn("关闭报灾未找到可挂载的流程链路父节点, reportId={}, processType={}, sourceType={}",
                report.getId(), report.getProcessType(), report.getSourceType());
            return;
        }
        ReportCloseOperator operator = resolveReportCloseOperator(report);
        taskProcessChainNodeService.recordBizNode(
            parent.getChainId(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getLinkName(),
            TaskProcessChainNodeTextEnum.DISASTER_DANGER_CLOSED.getTriggerReason(),
            TaskProcessBizTypeEnum.REPORT.getCode(),
            report.getId(),
            report.getTaskId(),
            operator.userId(),
            operator.userName(),
            TaskProcessSourceTypeEnum.fromReportSourceType(report.getSourceType()).getCode(),
            parent.getId(),
            operator.userRole(),
            TaskProcessNodeCategoryEnum.DISASTER_DANGER_CLOSED.getCode());
    }

    private ReportCloseOperator resolveReportCloseOperator(DzReportDisaster report) {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null) {
                String userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
                if (loginUser.getUserId() != null || StringUtils.isNotBlank(userName)) {
                    return new ReportCloseOperator(loginUser.getUserId(), userName, null);
                }
            }
        } catch (Exception e) {
            log.debug("解析关闭报灾当前操作人失败，将降级使用报灾上报人, reportId={}",
                report == null ? null : report.getId(), e);
        }
        return new ReportCloseOperator(
            report == null ? null : report.getUserId(),
            report == null ? null : report.getUserName(),
            report == null ? null : report.getUserRole()
        );
    }

    private record ReportCloseOperator(Long userId, String userName, String userRole) {
    }

    private DzTaskProcessChainNode resolveDisasterDangerClosedParentNode(DzReportDisaster report) {
        if (report == null || report.getId() == null) {
            return null;
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_INSPECTOR_VERIFY)) {
            DzTaskProcessChainNode generatedReportNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                TaskProcessBizTypeEnum.REPORT.getCode(),
                report.getId(),
                TaskProcessChainNodeTextEnum.DISASTER_REPORT_GENERATE.getLinkName(),
                null);
            if (generatedReportNode != null) {
                return generatedReportNode;
            }
            return taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                TaskProcessBizTypeEnum.REPORT.getCode(),
                report.getId(),
                TaskProcessChainNodeTextEnum.PUBLIC_REPORT_REPORT.getLinkName(),
                TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)
            && Objects.equals(report.getSourceType(), REPORT_SOURCE_TASK_FEEDBACK)) {
            return taskProcessChainNodeService.resolveSavedNodeByBizAndCategory(
                TaskProcessBizTypeEnum.REPORT.getCode(),
                report.getId(),
                TaskProcessNodeCategoryEnum.TASK_FEEDBACK_REPORT.getCode());
        }
        if (Objects.equals(report.getProcessType(), DzReportDisaster.PROCESS_TYPE_DIRECT_TOWN)
            && Objects.equals(report.getSourceType(), REPORT_SOURCE_PUBLIC_REPORT)) {
            DzTaskDistList task = selectLatestReportTask(report.getId());
            if (task != null && task.getId() != null) {
                DzTaskProcessChainNode aiVerifyNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                    TaskProcessBizTypeEnum.TASK.getCode(),
                    task.getId(),
                    TaskProcessChainNodeTextEnum.AI_VERIFY_TASK_DISPATCH.getLinkName(),
                    TaskProcessNodeCategoryEnum.TASK_PUSH.getCode());
                if (aiVerifyNode != null) {
                    return aiVerifyNode;
                }
                DzTaskProcessChainNode reportToTownNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                    TaskProcessBizTypeEnum.TASK.getCode(),
                    task.getId(),
                    TaskProcessChainNodeTextEnum.REPORT_TO_TOWN_TASK.getLinkName(),
                    null);
                if (reportToTownNode != null) {
                    return reportToTownNode;
                }
            }
            return taskProcessChainNodeService.resolveSavedNodeByBizAndLink(
                TaskProcessBizTypeEnum.REPORT.getCode(),
                report.getId(),
                TaskProcessChainNodeTextEnum.PATROL_REPORT_REPORT.getLinkName(),
                TaskProcessNodeCategoryEnum.PUBLIC_REPORT_REPORT.getCode());
        }
        return null;
    }

    @Override
    public List<DzReportDisasterVo> getAllHandling() {
        LambdaQueryWrapper<DzReportDisaster> lqw = Wrappers.lambdaQuery();
        lqw.in(DzReportDisaster::getStatus, 1, 2);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public void addFeedback(DzReportDisasterFeedbackBo bo) {
        DzReportDisaster existed = baseMapper.selectById(bo.getId());
        if (existed == null) {
            throw new ServiceException("报灾记录不存在");
        }
        DzReportDisaster update = new DzReportDisaster();
        update.setId(bo.getId());
        update.setManualRiskLevel(bo.getManualRiskLevel());
        update.setManualRiskRemark(bo.getManualRiskRemark());
        update.setUpdateDate(new Date());
        baseMapper.updateById(update);
        refreshReportRiskLevelIfCurrent(bo.getId());
    }

    private void refreshReportRiskLevelIfCurrent(Long reportId) {
        if (reportId == null) {
            return;
        }
        try {
            taskProcessChainSummaryService.refreshReportRiskLevelIfCurrent(reportId);
        } catch (Exception e) {
            log.warn("报灾人工反馈风险等级回写后刷新流程链路风险等级失败, reportId={}", reportId, e);
        }
    }

    private void notifyAiHostingOverview(String reason, Long reportId, Long taskId) {
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "report_status",
            reason,
            "dz_report_disaster",
            reportId,
            taskId
        );
    }
}
