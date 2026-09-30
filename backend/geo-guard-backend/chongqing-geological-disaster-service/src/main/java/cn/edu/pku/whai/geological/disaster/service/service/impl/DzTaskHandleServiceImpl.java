package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskZoneService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AiModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.GenerateReviewReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendBatchVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleRiskAreaStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsSendWrapResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.HandleStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.handle.props.HandleProcessProps;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendBatchService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.IEvacuationPlanGenerateService;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.AiReportEventLevelSupport;
import cn.edu.pku.whai.geological.disaster.service.utils.CoordinateConverter;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import io.github.kongweiguang.http.client.Req;
import io.github.kongweiguang.json.Json;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 灾害处置Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskHandleServiceImpl implements IDzTaskHandleService {

    private static final long AUTO_MODE_FALLBACK_USER_ID = 0L;
    private static final int DATA_NOT_DELETED = 0;
    private static final int HANDLE_NOT_SKIPPED = 0;
    private static final int HANDLE_SKIPPED = 1;
    private static final int APPROVAL_STATUS_APPROVED = 1;
    private static final int RISK_OVERVIEW_TYPE_TASK_HANDLE = 1;
    private static final int RISK_OVERVIEW_TYPE_DEFENSE_RESPONSE = 2;
    private static final int ALERT_LEVEL_BLUE = 1;
    private static final int ALERT_LEVEL_YELLOW = 2;
    private static final int ALERT_LEVEL_ORANGE = 3;
    private static final int ALERT_LEVEL_RED = 4;
    private static final String PROCESS_BIZ_TYPE_HANDLE = "HANDLE";
    private static final String ALWAYS_FALSE_SQL = "1 = 0";

    private static final Duration ELEVATION_QUERY_TIMEOUT = Duration.ofSeconds(10);

    private final DzTaskHandleMapper baseMapper;
    private final DifyAgentClient difyAgentClient;
    private final DzTaskHandleSceneRecordMapper dzTaskHandleSceneRecordMapper;
    private final DzTaskHandleApprovalMapper dzTaskHandleApprovalMapper;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final IHazardPointService hazardPointService;
    private final IRiskZoneService riskZoneService;
    private final IDzTaskDistListService dzTaskDistListService;
    private final IEvacuationSmsSendService evacuationSmsSendService;
    private final ISlopeUnitService slopeUnitService;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final IEvacuationPlanGenerateService evacuationPlanGenerateService;
    private final IReceiveService receiveService;
    private final IGeologyInfoService geologyInfoService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final IAdRegionService adRegionService;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final IDzDefRespPlanService dzDefRespPlanService;
    private final IDzSingleDefProgressService dzSingleDefProgressService;
    private final IDzSmsSendBatchService dzSmsSendBatchService;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DataHouseMapper dataHouseMapper;
    private final DataPersonMapper dataPersonMapper;
    private final DisasterDetailedAddressResolver disasterDetailedAddressResolver;
    private final HandleProcessProps handleProcessProps;
    private final DzTaskHandleSceneSupport taskHandleSceneSupport;
    private final DzTaskHandlePermissionSupport taskHandlePermissionSupport;
    private final DzTaskHandleRiskOverviewSupport taskHandleRiskOverviewSupport;
    private final DzTaskHandleAiReportSupport taskHandleAiReportSupport;
    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;

    /**
     * 查询灾害处置
     *
     * @param id 主键
     * @return 灾害处置
     */
    @Override
    public DzTaskHandleVo queryById(Long id) {
        DzTaskHandleVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            return null;
        }
        taskHandleSceneSupport.fillSceneRecordFields(vo);
        taskHandleSceneSupport.fillRelationFields(vo);
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            vo.getProvince(),
            vo.getCity(),
            vo.getCounty(),
            vo.getStreet(),
            vo.getVillage()
        );
        return vo;
    }

    /**
     * 分页查询灾害处置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 灾害处置分页列表
     */
    @Override
    public TableDataInfo<DzTaskHandleVo> queryPageList(DzTaskHandleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzTaskHandle> lqw = buildQueryWrapper(bo);
        taskHandlePermissionSupport.appendCurrentUserTaskHandlePermission(lqw);
        Page<DzTaskHandleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        taskHandleSceneSupport.fillSceneRecordFields(result.getRecords());
        taskHandleSceneSupport.fillRelationFields(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的灾害处置列表
     *
     * @param bo 查询条件
     * @return 灾害处置列表
     */
    @Override
    public List<DzTaskHandleVo> queryList(DzTaskHandleBo bo) {
        LambdaQueryWrapper<DzTaskHandle> lqw = buildQueryWrapper(bo);
        taskHandlePermissionSupport.appendCurrentUserTaskHandlePermission(lqw);
        List<DzTaskHandleVo> list = baseMapper.selectVoList(lqw);
        taskHandleSceneSupport.fillSceneRecordFields(list);
        taskHandleSceneSupport.fillRelationFields(list);
        return list;
    }

    private LambdaQueryWrapper<DzTaskHandle> buildQueryWrapper(DzTaskHandleBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzTaskHandle> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(DzTaskHandle::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), DzTaskHandle::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), DzTaskHandle::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), DzTaskHandle::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), DzTaskHandle::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), DzTaskHandle::getVillage, bo.getVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getCenter()), DzTaskHandle::getCenter, bo.getCenter());
        lqw.like(StringUtils.isNotBlank(bo.getDetailedAddress()), DzTaskHandle::getDetailedAddress, bo.getDetailedAddress());
        lqw.eq(bo.getEventType() != null, DzTaskHandle::getEventType, bo.getEventType());
        lqw.eq(bo.getEventLevel() != null, DzTaskHandle::getEventLevel, bo.getEventLevel());
        lqw.eq(bo.getRespStatus() != null, DzTaskHandle::getRespStatus, bo.getRespStatus());
        lqw.eq(bo.getHandleProcess() != null, DzTaskHandle::getHandleProcess, bo.getHandleProcess());
        lqw.in(bo.getHandleProcessList() != null && !bo.getHandleProcessList().isEmpty(), DzTaskHandle::getHandleProcess, bo.getHandleProcessList());
        lqw.eq(StringUtils.isNotBlank(bo.getInfluenceScope()), DzTaskHandle::getInfluenceScope, bo.getInfluenceScope());
        lqw.eq(StringUtils.isNotBlank(bo.getResponsiblePerson()), DzTaskHandle::getResponsiblePerson, bo.getResponsiblePerson());
        lqw.eq(StringUtils.isNotBlank(bo.getResponsiblePersonPhone()), DzTaskHandle::getResponsiblePersonPhone, bo.getResponsiblePersonPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getResponsiblePersonRole()), DzTaskHandle::getResponsiblePersonRole, bo.getResponsiblePersonRole());
        lqw.eq(StringUtils.isNotBlank(bo.getReporter()), DzTaskHandle::getReporter, bo.getReporter());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeUnitId()), DzTaskHandle::getSlopeUnitId, bo.getSlopeUnitId());
        lqw.eq(bo.getPeopleLeave() != null, DzTaskHandle::getPeopleLeave, bo.getPeopleLeave());
        lqw.eq(bo.getIsSkipped() != null, DzTaskHandle::getIsSkipped, bo.getIsSkipped());
        lqw.eq(bo.getCreateDate() != null, DzTaskHandle::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, DzTaskHandle::getUpdateDate, bo.getUpdateDate());

        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzTaskHandle::getReporterDate,
                DateUtil.parseDateTime((String) params.get("beginTime")),
                DateUtil.parseDateTime((String) params.get("endTime")));
        }

        return lqw;
    }

    /**
     * 新增灾害处置
     *
     * @param bo 灾害处置
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzTaskHandleBo bo) {
        DzTaskHandle add = MapstructUtils.convert(bo, DzTaskHandle.class);
        String detailedAddress = disasterDetailedAddressResolver.resolve(add.getCenter());
        if (StringUtils.isNotBlank(detailedAddress)) {
            add.setDetailedAddress(detailedAddress);
        }
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            add.getProvince(),
            add.getCity(),
            add.getCounty(),
            add.getStreet(),
            add.getVillage()
        );
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改灾害处置
     *
     * @param bo 灾害处置
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzTaskHandleBo bo) {
        if (bo == null) {
            throw new ServiceException("处置任务不能为空");
        }
        if (bo.getId() == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        DzTaskHandle current = taskHandlePermissionSupport.requireTaskHandleById(bo.getId());
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            current.getProvince(),
            current.getCity(),
            current.getCounty(),
            current.getStreet(),
            current.getVillage()
        );
        DzTaskHandle update = MapstructUtils.convert(bo, DzTaskHandle.class);
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            update.getProvince(),
            update.getCity(),
            update.getCounty(),
            update.getStreet(),
            update.getVillage()
        );
        validEntityBeforeSave(update);
        update.setUpdateDate(new Date());
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzTaskHandle entity) {
        if (entity == null) {
            throw new ServiceException("处置任务不能为空");
        }
        if (entity.getHandleProcess() != null && HandleProcessEnum.getByCode(entity.getHandleProcess()) == null) {
            throw new ServiceException("处置流程状态非法");
        }
        if (entity.getPeopleLeave() != null && !List.of(0, 1).contains(entity.getPeopleLeave())) {
            throw new ServiceException("群众撤离标识非法");
        }
        if (entity.getIsSkipped() != null && !List.of(0, 1).contains(entity.getIsSkipped())) {
            throw new ServiceException("跳过标识非法");
        }
        if (StringUtils.isNotBlank(entity.getSlopeUnitId())) {
            LambdaQueryWrapper<DzTaskHandle> lqw = Wrappers.<DzTaskHandle>lambdaQuery()
                                                           .eq(DzTaskHandle::getSlopeUnitId, entity.getSlopeUnitId())
                                                           .lt(DzTaskHandle::getHandleProcess, HandleProcessEnum.CLOSED_ARCHIVED.getCode());
            if (entity.getId() != null) {
                lqw.ne(DzTaskHandle::getId, entity.getId());
            }
            if (baseMapper.selectCount(lqw) > 0) {
                throw new ServiceException("同一斜坡单元存在未闭环处置任务，请勿重复创建");
            }
        }
    }

    /**
     * 校验并批量删除灾害处置信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<DzTaskHandle> handles = baseMapper.selectByIds(ids);
            if (handles.size() != ids.size()) {
                throw new ServiceException("存在处置任务不存在，无法删除");
            }
            for (DzTaskHandle handle : handles) {
                taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
                    handle.getProvince(),
                    handle.getCity(),
                    handle.getCounty(),
                    handle.getStreet(),
                    handle.getVillage()
                );
                if (handle.getHandleProcess() != null && handle.getHandleProcess() > HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode()) {
                    throw new ServiceException("仅允许删除未进入后续流程的处置任务");
                }
                if (dzTaskHandleSceneRecordMapper.selectCount(Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                                                                      .eq(DzTaskHandleSceneRecord::getHandleId, handle.getId())) > 0) {
                    throw new ServiceException("已存在现场记录的处置任务不允许删除");
                }
                if (dzTaskHandleApprovalMapper.selectCount(Wrappers.<DzTaskHandleApproval>lambdaQuery()
                                                                   .eq(DzTaskHandleApproval::getHandleId, handle.getId())) > 0) {
                    throw new ServiceException("已存在审批记录的处置任务不允许删除");
                }
                if (dzTaskDistListMapper.selectCount(Wrappers.<DzTaskDistList>lambdaQuery()
                                                             .eq(DzTaskDistList::getHandleId, handle.getId())
                                                             .eq(DzTaskDistList::getDelete, DATA_NOT_DELETED)) > 0) {
                    throw new ServiceException("已生成处置任务的处置单不允许删除");
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @SneakyThrows
    @Override
    public String aiModifyReport(AiModifyReportBo bo) {
        return taskHandleAiReportSupport.aiModifyReport(bo, this::updateEventLevelFromAiReport);
    }

    @SneakyThrows
    @Override
    public void aiModifyReportStream(SseEmitter sseEmitter, AiModifyReportBo bo) {
        taskHandleAiReportSupport.aiModifyReportStream(sseEmitter, bo, this::updateEventLevelFromAiReport);
    }

    @Override
    public void generateReviewReportStream(SseEmitter sseEmitter, GenerateReviewReportBo bo) {
        taskHandleAiReportSupport.generateReviewReportStream(sseEmitter, bo);
    }

    @Override
    public List<DzTaskHandleVo> getAllHandling() {
        return baseMapper.getAllHandling();
    }

    @Override
    public EvacuationSmsSendWrapResult status(Long handleIdLong) {

        EvacuationSmsSendWrapResult evacuationSmsSendWrapResult = new EvacuationSmsSendWrapResult();
        DzTaskDistListBo dzTaskDistListBo = new DzTaskDistListBo();
        dzTaskDistListBo.setHandleId(handleIdLong);
        dzTaskDistListBo.setDelete(0);
        dzTaskDistListBo.setSourceType(DzTaskDistList.SOURCE_TYPE_EMERGENCY);
        List<DzTaskDistListVo> dzTaskDistListVos = dzTaskDistListService.queryList(dzTaskDistListBo);

        int taskTotalCount = dzTaskDistListVos == null ? 0 : dzTaskDistListVos.size();
        int taskCompleteCount = dzTaskDistListVos == null ? 0 :
            (int) dzTaskDistListVos.stream()
                                   .filter(item -> item.getStatus() != null && (item.getStatus() == 4 || item.getStatus() == 5))
                                   .count();

        List<DzSmsSendBatchVo> dzSmsSendBatchVos = dzSmsSendBatchService.queryByBiz(DzSmsSendBatch.BIZ_TYPE_EVACUATION_SMS, handleIdLong);

        int totalCount = dzSmsSendBatchVos == null ? 0 :
            dzSmsSendBatchVos.stream()
                             .map(item -> Math.max(0,
                                 Objects.requireNonNullElse(item.getTotalCount(), 0)
                                     - Objects.requireNonNullElse(item.getSkipCount(), 0)))
                             .reduce(0, Integer::sum);

        int reachCount = dzSmsSendBatchVos == null ? 0 :
            dzSmsSendBatchVos.stream()
                             .map(DzSmsSendBatchVo::getSuccessCount)
                             .filter(Objects::nonNull)
                             .reduce(0, Integer::sum);

        evacuationSmsSendWrapResult.setTaskTotalCount(taskTotalCount);
        evacuationSmsSendWrapResult.setTaskCompleteCount(taskCompleteCount);
        evacuationSmsSendWrapResult.setTotalCount(totalCount);
        evacuationSmsSendWrapResult.setReachCount(reachCount);
        evacuationSmsSendWrapResult.setCompleteRate(formatCompleteRate(taskCompleteCount, taskTotalCount));
        return evacuationSmsSendWrapResult;
    }

    static BigDecimal formatCompleteRate(int completeCount, int totalCount) {
        if (totalCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(Math.max(0, completeCount))
                         .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                         .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public DzTaskHandleRiskAreaStatVo riskAreaStat(Long handleId) {
        DzTaskHandle taskHandle = taskHandlePermissionSupport.requireTaskHandleById(handleId);
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            taskHandle.getProvince(),
            taskHandle.getCity(),
            taskHandle.getCounty(),
            taskHandle.getStreet(),
            taskHandle.getVillage()
        );

        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        if (sceneRecord == null || StringUtils.isBlank(sceneRecord.getRiskExtent())) {
            throw new ServiceException("当前处置任务缺少风险区域WKT");
        }

        String polygonWkt = sceneRecord.getRiskExtent().trim();
        long buildingTotal = Optional.ofNullable(dataHouseMapper.countDistinctBuildingByPolygonWkt(polygonWkt)).orElse(0L);
        long populationTotal = Optional.ofNullable(dataPersonMapper.countByPolygonWkt(polygonWkt)).orElse(0L);
        long age60AndAboveTotal = Optional.ofNullable(dataPersonMapper.countAgeSixtyAndAboveByPolygonWkt(polygonWkt)).orElse(0L);

        BigDecimal age60AndAboveRatio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (populationTotal > 0) {
            age60AndAboveRatio = BigDecimal.valueOf(age60AndAboveTotal)
                                           .divide(BigDecimal.valueOf(populationTotal), 2, RoundingMode.HALF_UP)
                                           .setScale(2, RoundingMode.HALF_UP);
        }

        return DzTaskHandleRiskAreaStatVo.builder()
                                         .buildingTotal(buildingTotal)
                                         .populationTotal(populationTotal)
                                         .age60AndAboveRatio(age60AndAboveRatio)
                                         .build();
    }

    @Override
    public void modifyReport(ModifyReportBo bo) {
        if (bo == null || bo.getHandleId() == null) {
            throw new ServiceException("处置任务ID不能为空");
        }
        DzTaskHandle taskHandle = taskHandlePermissionSupport.requireTaskHandleById(bo.getHandleId());
        taskHandlePermissionSupport.validateCurrentUserTaskHandleAccess(
            taskHandle.getProvince(),
            taskHandle.getCity(),
            taskHandle.getCounty(),
            taskHandle.getStreet(),
            taskHandle.getVillage()
        );
        dzTaskHandleDetailContentService.saveAiReportContent(bo.getHandleId(), 1, bo.getReport());
        updateEventLevelFromAiReport(bo.getHandleId(), bo.getReport());
    }

    private void updateEventLevelFromAiReport(Long handleId, String aiReport) {
        if (handleId == null || StringUtils.isBlank(aiReport)) {
            return;
        }
        DzTaskHandle taskHandle = baseMapper.selectById(handleId);
        if (taskHandle == null) {
            return;
        }
        DzTaskHandleSceneRecord sceneRecord = dzTaskHandleSceneRecordMapper.selectOne(
            Wrappers.<DzTaskHandleSceneRecord>lambdaQuery().eq(DzTaskHandleSceneRecord::getHandleId, handleId).last("limit 1")
        );
        if (sceneRecord == null) {
            return;
        }
        AiReportEventLevelSupport.AiReportMetrics metrics = AiReportEventLevelSupport.parseMetrics(aiReport);
        if (metrics.hasNoMetric()) {
            return;
        }
        DzTaskHandleSceneRecord updateScene = new DzTaskHandleSceneRecord();
        updateScene.setId(sceneRecord.getId());
        updateScene.setUpdateDate(new Date());
        Integer deadPeople = metrics.deadPeople() != null ? metrics.deadPeople() : sceneRecord.getDeadPeople();
        Integer threatPeople = metrics.threatPeople() != null ? metrics.threatPeople() : sceneRecord.getThreatPeople();
        BigDecimal directLoss = metrics.directLoss() != null ? metrics.directLoss() : sceneRecord.getDirectLoss();
        BigDecimal threatAsset = metrics.threatAsset() != null ? metrics.threatAsset() : sceneRecord.getThreatAsset();
        if (metrics.deadPeople() != null) {
            updateScene.setDeadPeople(metrics.deadPeople());
        }
        if (metrics.threatPeople() != null) {
            updateScene.setThreatPeople(metrics.threatPeople());
        }
        if (metrics.directLoss() != null) {
            updateScene.setDirectLoss(metrics.directLoss());
        }
        if (metrics.threatAsset() != null) {
            updateScene.setThreatAsset(metrics.threatAsset());
        }
        if (updateScene.getDeadPeople() != null
            || updateScene.getThreatPeople() != null
            || updateScene.getDirectLoss() != null
            || updateScene.getThreatAsset() != null) {
            dzTaskHandleSceneRecordMapper.updateById(updateScene);
        }
        Integer targetEventLevel = metrics.eventLevel() != null
            ? metrics.eventLevel()
            : AiReportEventLevelSupport.calculateLevel(deadPeople, threatPeople, directLoss, threatAsset);
        if (!Objects.equals(taskHandle.getEventLevel(), targetEventLevel)) {
            DzTaskHandle updateTask = new DzTaskHandle();
            updateTask.setId(handleId);
            updateTask.setEventLevel(targetEventLevel);
            updateTask.setUpdateDate(new Date());
            baseMapper.updateById(updateTask);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer processNext(Long id) {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("仅允许值班员推进任务流程");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        return processNextInternal(id, false, loginUser.getUserId(), loginUser.getNickname());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer autoProcessNext(Long id, Long actorUserId, String actorName) {
        return processNextInternal(id, true, actorUserId, StringUtils.blankToDefault(actorName, "地象大模型"));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean tryAutoArchiveAfterEmergencyTaskFeedback(Long handleId, Long actorUserId, String actorName) {
        if (handleId == null) {
            return false;
        }
        DzTaskHandle handle = baseMapper.selectById(handleId);
        if (handle == null) {
            log.warn("现场处置任务反馈自动归档未找到处置任务, handleId={}, actorUserId={}, actorName={}",
                handleId, actorUserId, actorName);
            return false;
        }
        Integer currentProcess = handle.getHandleProcess();
        if (currentProcess == null || currentProcess < HandleProcessEnum.RESPONSE_EXECUTION.getCode()) {
            return false;
        }
        if (currentProcess >= HandleProcessEnum.CLOSED_ARCHIVED.getCode()) {
            return false;
        }
        if (hasHandleArchiveProcessNode(handleId) || hasSingleDefRespStartProcessNode(handleId)) {
            return false;
        }
        if (!allHandleEmergencyTasksFeedbacked(handleId)) {
            return false;
        }
        handle.setHandleProcess(HandleProcessEnum.CLOSED_ARCHIVED.getCode());
        baseMapper.updateById(handle);
        Date processTime = new Date();
        dzSingleDefProgressService.recordEnded(handleId, processTime);
        recordHandleArchiveProcessNode(handle, actorUserId, actorName, processTime);
        notifyAiHostingOverview("handle_auto_archived_after_emergency_feedback", handleId);
        log.info("现场处置任务反馈后自动结束归档, handleId={}, actorUserId={}, actorName={}",
            handleId, actorUserId, actorName);
        return true;
    }

    private Integer processNextInternal(Long id, boolean autoMode, Long actorUserId, String actorName) {
        DzTaskHandle dzTaskHandle = baseMapper.selectById(id);
        if (dzTaskHandle == null) {
            throw new ServiceException("任务不存在");
        }
        if (!autoMode) {
            taskHandlePermissionSupport.validateCurrentDutyOfficerTaskHandleAccess(dzTaskHandle);
        } else if (!canAutoModeAdvance(dzTaskHandle.getHandleProcess())) {
            throw new ServiceException("自动模式仅推进到会商研判，后续步骤需人工处理");
        }
        validateBeforeProcessAdvance(dzTaskHandle);
        if (dzTaskHandle.getHandleProcess() >= HandleProcessEnum.CLOSED_ARCHIVED.getCode()) {
            throw new ServiceException("任务已处理完成");
        }
        int nextProcess = dzTaskHandle.getHandleProcess() + 1;
        if (Objects.equals(nextProcess, HandleProcessEnum.CONSULTATION_JUDGMENT.getCode())) {
            taskHandleAiReportSupport.ensureEmergencyInvestigationReportGenerated(
                id,
                actorUserId,
                actorName,
                this::updateEventLevelFromAiReport
            );
        }
        if (Objects.equals(nextProcess, HandleProcessEnum.RESPONSE_EXECUTION.getCode())) {
            if (dzTaskHandle.getIsSkipped() == null || Objects.equals(dzTaskHandle.getIsSkipped(), HANDLE_NOT_SKIPPED)) {
                // 进入响应执行阶段时批量生成处置任务
                if (!executeInternal(id, !autoMode)) {
                    throw new ServiceException("批量生成处置任务失败，不能进入响应执行");
                }
            } else if (Objects.equals(dzTaskHandle.getIsSkipped(), HANDLE_SKIPPED)) {
                log.info("任务已生成过,不再重复生成");
            } else {
                throw new ServiceException("isSkipped类型错误");
            }
        } else if (Objects.equals(nextProcess, HandleProcessEnum.CLOSED_ARCHIVED.getCode())) {
            validateHandleArchiveTasksFeedbacked(id);
        }
        dzTaskHandle.setHandleProcess(nextProcess);
        baseMapper.updateById(dzTaskHandle);
        Date processTime = new Date();
        if (Objects.equals(nextProcess, HandleProcessEnum.RESPONSE_EXECUTION.getCode())) {
            dzSingleDefProgressService.recordTaskPublished(id, processTime);
        } else if (Objects.equals(nextProcess, HandleProcessEnum.CLOSED_ARCHIVED.getCode())) {
            dzSingleDefProgressService.recordEnded(id, processTime);
            recordHandleArchiveProcessNode(dzTaskHandle, actorUserId, actorName, processTime);
        }
        if (autoMode) {
            log.info("自动模式完成处置流程推进, handleId={}, actorUserId={}, actorName={}, process={}",
                id, actorUserId, actorName, dzTaskHandle.getHandleProcess());
        }
        notifyAiHostingOverview("handle_process_advanced", id);
        return dzTaskHandle.getHandleProcess();
    }

    void validateHandleArchiveTasksFeedbacked(Long handleId) {
        List<DzTaskDistList> unfinishedTasks = dzTaskDistListMapper.selectList(buildUnfinishedEmergencyTaskQuery(handleId));
        if (unfinishedTasks == null || unfinishedTasks.isEmpty()) {
            return;
        }
        String taskIds = unfinishedTasks.stream()
                                        .map(DzTaskDistList::getId)
                                        .filter(Objects::nonNull)
                                        .map(String::valueOf)
                                        .limit(10)
                                        .reduce((left, right) -> left + "," + right)
                                        .orElse("未知");
        throw new ServiceException("处置任务未全部反馈，不允许归档。未反馈任务ID：" + taskIds);
    }

    private LambdaQueryWrapper<DzTaskDistList> buildUnfinishedEmergencyTaskQuery(Long handleId) {
        return Wrappers.<DzTaskDistList>lambdaQuery()
            .eq(DzTaskDistList::getHandleId, handleId)
            .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_EMERGENCY)
            .eq(DzTaskDistList::getDelete, DATA_NOT_DELETED)
            .and(wrapper -> wrapper.isNull(DzTaskDistList::getStatus)
                .or()
                .ne(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED));
    }

    private boolean allHandleEmergencyTasksFeedbacked(Long handleId) {
        return dzTaskDistListMapper.selectCount(buildUnfinishedEmergencyTaskQuery(handleId)) == 0;
    }

    private boolean hasHandleArchiveProcessNode(Long handleId) {
        return taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(),
            null
        ) != null;
    }

    private boolean hasSingleDefRespStartProcessNode(Long handleId) {
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.HANDLE.getCode(), handleId);
        if (StringUtils.isBlank(chainId)) {
            return false;
        }
        return containsProcessNode(taskProcessChainNodeService.queryChainByChainId(chainId),
            TaskProcessNodeCategoryEnum.SINGLE_DEF_RESP_START.getCode(),
            TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName());
    }

    private boolean containsProcessNode(List<TaskProcessChainNodeVo> nodes, Integer nodeCategory, String linkName) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (TaskProcessChainNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (Objects.equals(nodeCategory, node.getNodeCategory())
                || TaskProcessChainNodeTextEnum.semanticEquals(linkName, node.getLinkName())) {
                return true;
            }
            if (containsChildProcessNode(node.getChildChains(), nodeCategory, linkName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChildProcessNode(List<List<TaskProcessChainNodeVo>> childChains, Integer nodeCategory, String linkName) {
        if (childChains == null || childChains.isEmpty()) {
            return false;
        }
        for (List<TaskProcessChainNodeVo> childChain : childChains) {
            if (containsProcessNode(childChain, nodeCategory, linkName)) {
                return true;
            }
        }
        return false;
    }

    void recordHandleArchiveProcessNode(DzTaskHandle handle, Long actorUserId, String actorName, Date triggerTime) {
        if (handle == null || handle.getId() == null) {
            return;
        }
        DzTaskProcessChainNode emergencyBatchNode = resolveEmergencyBatchProcessNode(handle.getId());
        if (emergencyBatchNode == null || emergencyBatchNode.getId() == null
            || StringUtils.isBlank(emergencyBatchNode.getChainId())) {
            throw new ServiceException("处置结束归档未找到生成处置任务主链节点, handleId=" + handle.getId());
        }
        taskProcessChainNodeService.recordBizNode(
            emergencyBatchNode.getChainId(),
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getLinkName(),
            TaskProcessChainNodeTextEnum.HANDLE_ARCHIVED.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handle.getId(),
            null,
            actorUserId,
            StringUtils.blankToDefault(actorName, handle.getResponsiblePerson()),
            TaskProcessSourceTypeEnum.HANDLE.getCode(),
            emergencyBatchNode.getId(),
            null,
            TaskProcessNodeCategoryEnum.HANDLE_ARCHIVED.getCode(),
            TaskProcessStageTypeEnum.ARCHIVE.getCode()
        );
    }

    private DzTaskProcessChainNode resolveEmergencyBatchProcessNode(Long handleId) {
        DzTaskProcessChainNode node = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            TaskProcessChainNodeTextEnum.EMERGENCY_BATCH_DISPATCH.getLinkName(),
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        );
        if (node != null) {
            return node;
        }
        return taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.HANDLE.getCode(),
            handleId,
            "批量下发处置任务",
            TaskProcessNodeCategoryEnum.EMERGENCY_BATCH_DISPATCH.getCode()
        );
    }

    @Override
    public HandleStatVo stat() {
        HandleStatVo vo = new HandleStatVo();
        Long hazardPointCount = hazardPointService.countAll();
        vo.setHazardPointCount(hazardPointCount);

        List<RiskZoneStatVo> riskZoneStatVos = riskZoneService.statRiskLevel();
        vo.setRiskZoneLevelList(riskZoneStatVos);

        int riskZoneCount = riskZoneStatVos.stream().mapToInt(RiskZoneStatVo::getCount).sum();
        vo.setRiskZoneCount(riskZoneCount);

        return vo;
    }

    @Override
    public Boolean execute(Long handleId) {
        return executeInternal(handleId, true);
    }

    private Boolean executeInternal(Long handleId, boolean validateDutyOfficer) {
        DzTaskHandle taskHandle = taskHandlePermissionSupport.requireTaskHandleById(handleId);
        if (validateDutyOfficer) {
            taskHandlePermissionSupport.validateCurrentDutyOfficerTaskHandleAccess(taskHandle);
        }
        // 开始批量生成处置任务
        Boolean taskDefRes = dzTaskDistListService.batchGenerateByHandleId(handleId);
        //开始群发撤离短信
        Boolean evacuationSmsSend = evacuationSmsSendService.execute(handleId);
        return taskDefRes && evacuationSmsSend;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long insertByReq(DzTaskHandleReq req) {
        DzTaskHandle add = MapstructUtils.convert(req, DzTaskHandle.class);
        add.setId(req.getHandleId());
        add.setCreateDate(new Date());
        add.setUpdateDate(new Date());
        String center = req.getCenter();
        String detailedAddress = disasterDetailedAddressResolver.resolve(center);
        if (StringUtils.isNotBlank(detailedAddress)) {
            add.setDetailedAddress(detailedAddress);
        }

        SlopeUnit slopeUnit = slopeUnitService.queryPoByCenter(center);
        if (slopeUnit == null || StringUtils.isBlank(slopeUnit.getId())) {
            throw new ServiceException("未匹配到对应斜坡单元，无法创建处置任务");
        }
        add.setSlopeUnitId(slopeUnit.getId());
        HandleResponsibleContact contact = resolveHandleResponsibleContact(slopeUnit.getId());
        add.setResponsiblePerson(contact.name());
        add.setResponsiblePersonPhone(contact.phoneNumber());
        add.setResponsiblePersonRole(contact.role());
        add.setReporter(req.getReporter());
        add.setReporterDate(req.getReporterDate());
        add.setHandleProcess(1);
        add.setIsSkipped(0);
        baseMapper.insert(add);
        notifyAiHostingOverview("handle_created", add.getId());
        return add.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addSceneRecord(DzTaskHandleSceneRecordReq req) {
        if (req == null) {
            throw new ServiceException("现场记录不能为空");
        }
        if (req.getHandleId() == null) {
            throw new ServiceException("handleId不能为空");
        }
        String disasterPoint = CoordinateConverter.validatePointWkt(req.getDisasterCoordinates(), "灾害点坐标");
        String checkInPoint = CoordinateConverter.validatePointWkt(req.getCheckInCoordinates(), "打卡坐标");
        String hazardExtent = CoordinateConverter.validateGeometryWkt(req.getHazardExtent(), "危险范围");
        String riskExtent = CoordinateConverter.validateGeometryWkt(req.getRiskExtent(), "风险范围");
        CoordinateConverter.validatePointWithinGeometry(disasterPoint, hazardExtent, "灾害点必须在危险范围内");
        CoordinateConverter.validateGeometryWithinGeometry(hazardExtent, riskExtent, "危险范围必须在风险范围内");
        disasterPoint = refreshDisasterPointElevation(disasterPoint);
        req.setDisasterCoordinates(disasterPoint);
        req.setCheckInCoordinates(checkInPoint);
        req.setHazardExtent(hazardExtent);
        req.setRiskExtent(riskExtent);
        if (StringUtils.isBlank(req.getDisasterName()) && StringUtils.isNotBlank(req.getLocation())
            && StringUtils.isNotBlank(req.getDisasterType())) {
            req.setDisasterName(req.getLocation() + req.getDisasterType());
        }

        SlopeUnit matchedSlopeUnit = slopeUnitService.queryPoByCenter(disasterPoint);
        if (matchedSlopeUnit == null || StringUtils.isBlank(matchedSlopeUnit.getId())) {
            throw new ServiceException("未匹配到对应斜坡单元，无法新增现场记录");
        }
        SlopeUnit slopeUnit = slopeUnitService.listPoByIds(List.of(matchedSlopeUnit.getId()))
                                              .stream()
                                              .findFirst()
                                              .orElse(null);
        if (slopeUnit == null) {
            throw new ServiceException("斜坡单元不存在，无法新增现场记录");
        }

        taskHandleSceneSupport.fillGeologyInfo(req, disasterPoint);
        DzTaskHandle dzTaskHandle = baseMapper.selectById(req.getHandleId());
        if (dzTaskHandle != null) {
            DzTaskHandleSceneRecordVo byHandleId = dzTaskHandleSceneRecordService.getByHandleId(req.getHandleId());
            if (byHandleId != null) {
                throw new ServiceException("该方案已存在且已存在现场记录");
            }
        }
        Date triggerTime = new Date();
        Long sceneRecordTaskId = parseSceneRecordTaskId(req.getTaskId());
        DzTaskHandleReq handleReq = new DzTaskHandleReq();
        handleReq.setHandleId(req.getHandleId());
        handleReq.setProvince(slopeUnit.getProvince());
        handleReq.setCity(slopeUnit.getCity());
        handleReq.setCounty(slopeUnit.getCounty());
        handleReq.setStreet(slopeUnit.getStreet());
        handleReq.setVillage(slopeUnit.getVillage());
        handleReq.setCenter(disasterPoint);
        handleReq.setReporter(req.getReporter());
        handleReq.setReporterDate(triggerTime);
        insertByReq(handleReq);
        dzTaskHandleSceneRecordService.insertByReq(req);
        DzTaskDistList emergencyInvestigationTask = closeSceneRecordEmergencyInvestigationTask(
            sceneRecordTaskId, req.getHandleId(), triggerTime);
        recordSceneRecordHandleProcessNode(req, emergencyInvestigationTask, triggerTime);
        triggerEvacuationRouteGenerationAfterCommit(req.getHandleId());
    }

    private void triggerEvacuationRouteGenerationAfterCommit(Long handleId) {
        if (handleId == null) {
            return;
        }
        Runnable action = () -> {
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        evacuationPlanGenerateService.generateEvacuationRoute(handleId);
                        log.info("自动生成疏散路线完成, handleId={}", handleId);
                    } catch (Exception e) {
                        log.error("自动生成疏散路线失败, handleId={}", handleId, e);
                    }
                }, Run.executor).exceptionally(ex -> {
                    log.error("自动生成疏散路线异步任务执行异常, handleId={}", handleId, ex);
                    return null;
                });
            } catch (Exception e) {
                log.error("自动生成疏散路线异步任务提交失败, handleId={}", handleId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private Long parseSceneRecordTaskId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        String trimmedTaskId = taskId.trim();
        try {
            Long parsedTaskId = Long.valueOf(trimmedTaskId);
            if (parsedTaskId <= 0) {
                throw new NumberFormatException("taskId must be positive");
            }
            return parsedTaskId;
        } catch (NumberFormatException e) {
            throw new ServiceException("taskId格式错误");
        }
    }

    private DzTaskDistList closeSceneRecordEmergencyInvestigationTask(Long taskId, Long handleId, Date triggerTime) {
        if (taskId == null) {
            return null;
        }
        return dzTaskDistListService.closeLinkedEmergencyInvestigationTask(
            taskId, handleId, triggerTime, TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getTriggerReason());
    }

    void recordSceneRecordHandleProcessNode(DzTaskHandleSceneRecordReq req, DzTaskDistList emergencyInvestigationTask,
                                            Date triggerTime) {
        if (req == null || req.getHandleId() == null) {
            return;
        }
        Long taskId = emergencyInvestigationTask == null ? null : emergencyInvestigationTask.getId();
        String chainId = taskId == null ? null : taskProcessChainNodeService.resolveSavedChainIdByTaskId(taskId);
        if (StringUtils.isBlank(chainId)) {
            chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), req.getHandleId());
        }
        if (StringUtils.isBlank(chainId) && taskId == null) {
            chainId = taskProcessChainNodeService.generateChainId();
        }
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("开启处置管理未找到已有流程链路, handleId=" + req.getHandleId());
        }
        TaskProcessChainNodeTextEnum sceneRecordText = resolveSceneRecordText(emergencyInvestigationTask);
        taskProcessChainNodeService.recordBizNode(chainId, sceneRecordText.getLinkName(), sceneRecordText.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), req.getHandleId(), taskId, null, req.getReporter(),
            TaskProcessSourceTypeEnum.HANDLE.getCode());
        taskProcessChainNodeService.recordBizNode(chainId, TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getLinkName(),
            TaskProcessChainNodeTextEnum.HANDLE_START_FROM_APP_SCENE_RECORD.getTriggerReason(),
            TaskProcessBizTypeEnum.HANDLE.getCode(), req.getHandleId(), taskId, null, req.getReporter(),
            TaskProcessSourceTypeEnum.HANDLE.getCode());
    }

    private TaskProcessChainNodeTextEnum resolveSceneRecordText(DzTaskDistList emergencyInvestigationTask) {
        if (emergencyInvestigationTask == null || emergencyInvestigationTask.getId() == null) {
            return TaskProcessChainNodeTextEnum.SCENE_HANDLE_REPORT_UPLOAD;
        }
        if (Objects.equals(emergencyInvestigationTask.getSourceType(), DzTaskDistList.SOURCE_TYPE_TECH_ASSISTANCE)) {
            return TaskProcessChainNodeTextEnum.TECH_ASSIST_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD;
        }
        return TaskProcessChainNodeTextEnum.TASK_FEEDBACK_SCENE_HANDLE_REPORT_UPLOAD;
    }

    private String refreshDisasterPointElevation(String disasterPoint) {
        double[] coordinates = GeoDistanceUtil.parsePoint(disasterPoint);
        if (coordinates == null || coordinates.length < 2) {
            throw new ServiceException("灾害点坐标解析失败，无法获取高程");
        }
        BigDecimal elevation = queryElevation(coordinates[0], coordinates[1]);
        return "POINT Z(" + formatWktNumber(coordinates[0]) + " " + formatWktNumber(coordinates[1]) + " " + formatWktNumber(elevation) + ")";
    }

    private BigDecimal queryElevation(double lon, double lat) {
        if (StringUtils.isBlank(handleProcessProps.getElevationQueryUrl())) {
            throw new ServiceException("高程查询接口地址未配置");
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("lon", lon);
        requestBody.put("lat", lat);
        try {
            JsonNode root = Req.post(handleProcessProps.getElevationQueryUrl())
                               .json(requestBody)
                               .timeout(ELEVATION_QUERY_TIMEOUT)
                               .ok()
                               .node();
            JsonNode elevationNode = resolveElevationNode(root);
            return parseElevation(elevationNode, lon, lat);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("高程服务调用异常, lon={}, lat={}", lon, lat, e);
            throw new ServiceException("高程服务调用异常: " + e.getMessage());
        }
    }

    private JsonNode resolveElevationNode(JsonNode root) {
        if (root == null || root.isNull() || root.isMissingNode()) {
            return null;
        }
        JsonNode elevationNode = root.findValue("elevation");
        if (elevationNode != null && !elevationNode.isNull()) {
            return elevationNode;
        }
        JsonNode dataNode = root.get("data");
        if (dataNode != null && dataNode.isNumber()) {
            return dataNode;
        }
        return null;
    }

    private BigDecimal parseElevation(JsonNode elevationNode, double lon, double lat) {
        if (elevationNode == null || elevationNode.isNull()) {
            throw new ServiceException("高程服务未返回高程数据(lon=" + formatWktNumber(lon) + ", lat=" + formatWktNumber(lat) + ")，请确认灾害点坐标在高程服务覆盖范围内");
        }
        if (elevationNode.isNumber()) {
            return elevationNode.decimalValue();
        }
        if (elevationNode.isTextual() && StringUtils.isNotBlank(elevationNode.asText())) {
            try {
                return new BigDecimal(elevationNode.asText().trim());
            } catch (NumberFormatException e) {
                throw new ServiceException("高程服务返回高程值格式错误");
            }
        }
        throw new ServiceException("高程服务返回高程值格式错误");
    }

    private String formatWktNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String formatWktNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private HandleResponsibleContact resolveHandleResponsibleContact(String slopeUnitId) {
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(slopeUnitId);
        if (relationVo == null || relationVo.getResponsiblePerson() == null) {
            throw new ServiceException("当前斜坡单元未绑定乡长，无法创建处置任务");
        }
        return new HandleResponsibleContact(relationVo.getResponsiblePerson(), relationVo.getResponsiblePersonPhone(), "乡长");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer skip(Long id) {
        DzTaskHandle dzTaskHandle = taskHandlePermissionSupport.requireTaskHandleById(id);
        taskHandlePermissionSupport.validateCurrentDutyOfficerTaskHandleAccess(dzTaskHandle);
        if (!HandleProcessEnum.CONSULTATION_JUDGMENT.getCode().equals(dzTaskHandle.getHandleProcess())) {
            throw new ServiceException("任务状态错误");
        }
        if (dzTaskHandleSceneRecordMapper.selectCount(Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                                                              .eq(DzTaskHandleSceneRecord::getHandleId, id)) < 1) {
            throw new ServiceException("缺少现场记录，不能跳过会商研判");
        }
        dzTaskHandle.setIsSkipped(HANDLE_SKIPPED);
        dzTaskHandle.setHandleProcess(HandleProcessEnum.RESPONSE_EXECUTION.getCode());
        baseMapper.updateById(dzTaskHandle);
        // 跳过会商直接发布时，仅生成监测巡查（群测群防）任务
        if (!dzTaskDistListService.batchGenerateByHandleId(id, PlanTypeEnum.MONITORING.getCode())) {
            throw new ServiceException("批量生成处置任务失败，不能进入响应执行");
        }
        notifyAiHostingOverview("consultation_skipped", id);
        return dzTaskHandle.getHandleProcess();
    }

    @Override
    public DzRiskOverviewVo riskOverview(Integer type) {
        return taskHandleRiskOverviewSupport.riskOverview(type);
    }

    @Override
    public Long eventCount(AdRegionVo adRegionVo, List<Integer> handleProcess) {
        return taskHandleRiskOverviewSupport.eventCount(adRegionVo, handleProcess);
    }

    static boolean canAutoModeAdvance(Integer currentProcess) {
        return currentProcess != null && currentProcess < HandleProcessEnum.CONSULTATION_JUDGMENT.getCode();
    }

    private List<String> splitStreetNames(String streets) {
        List<String> streetNames = new ArrayList<>();
        for (String street : streets.split("[,，]")) {
            if (StringUtils.isNotBlank(street)) {
                streetNames.add(street.trim());
            }
        }
        return streetNames;
    }

    private void validateBeforeProcessAdvance(DzTaskHandle dzTaskHandle) {
        Integer currentProcess = dzTaskHandle.getHandleProcess();
        if (currentProcess == null) {
            throw new ServiceException("处置流程状态不能为空");
        }
        if (currentProcess >= HandleProcessEnum.CLOSED_ARCHIVED.getCode()) {
            throw new ServiceException("任务已处理完成");
        }
        if (Objects.equals(currentProcess, HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode())
            && dzTaskHandleSceneRecordMapper.selectCount(Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                                                                 .eq(DzTaskHandleSceneRecord::getHandleId, dzTaskHandle.getId())) < 1) {
            throw new ServiceException("缺少现场记录，不能进入会商研判");
        }
        if (Objects.equals(currentProcess, HandleProcessEnum.PLAN_IMPLEMENTATION.getCode())
            && !hasApprovedRecord(
                dzTaskHandle.getId(),
                HandleProcessEnum.PLAN_IMPLEMENTATION.getCode(),
                DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode()
            )) {
            throw new ServiceException("会商研判未审批通过，不能进入方案接入");
        }
        if (Objects.equals(currentProcess, HandleProcessEnum.PLAN_IMPLEMENTATION.getCode())) {
            String schemeJson = getLatestTaskHandlePlanContent(dzTaskHandle.getId(), DetailContentTypeEnum.EVACUATION_PLAN.getCode());
            if (StringUtils.isBlank(schemeJson)) {
                throw new ServiceException("缺少处置方案或报告内容，不能进入响应执行");
            }
        }
    }

    private String getLatestTaskHandlePlanContent(Long handleId, Integer contentType) {
        DzTaskHandleDetailContentVo latest = dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            contentType,
            null
        );
        if (latest == null) {
            return null;
        }
        if (Objects.equals(contentType, DetailContentTypeEnum.EVACUATION_PLAN.getCode())
            && StringUtils.isNotBlank(latest.getPlanContentJson())) {
            return latest.getPlanContentJson();
        }
        return latest.getPlanContent();
    }

    private boolean hasApprovedRecord(Long handleId, Integer process, Integer type) {
        return dzTaskHandleApprovalMapper.selectCount(Wrappers.<DzTaskHandleApproval>lambdaQuery()
                                                              .eq(DzTaskHandleApproval::getHandleId, handleId)
                                                              .eq(DzTaskHandleApproval::getProcess, process)
                                                              .eq(DzTaskHandleApproval::getType, type)
                                                              .eq(DzTaskHandleApproval::getStatus, APPROVAL_STATUS_APPROVED)) > 0;
    }

    private void notifyAiHostingOverview(String reason, Long handleId) {
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "task_handle_process",
            reason,
            "dz_task_handle",
            handleId,
            null
        );
    }

    private record HandleResponsibleContact(String name, String phoneNumber, String role) {
    }
}
