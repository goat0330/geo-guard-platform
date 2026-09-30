/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DefRespRange;
import cn.edu.pku.whai.geological.disaster.data.domain.po.PersonSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DefRespRangeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataAlarmMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.PersonSlopeMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespApprovalStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessStageTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consultation.DefRespConsultationConfirmItems;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanApprovalStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBatchRelateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanGenerateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.DefRespPlanDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanChildGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanRangeSlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanStreetGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespStartSmsPreviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownScopeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownStatItemVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.IMeetingService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespAlarmLevelUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespPlanCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
@Slf4j
public class DzDefRespPlanServiceImpl implements IDzDefRespPlanService {

    static final int DEF_RESP_MEETING_TYPE = 2;
    static final int DEF_RESP_MEETING_TYPE_A = 4;
    static final String ADMIN_APPROVAL_SMS_TITLE = "行政审批通知";
    static final int DEF_RESP_APPROVAL_PROCESS = HandleProcessEnum.PLAN_IMPLEMENTATION.getCode();
    static final long DEF_RESP_PLACEHOLDER_HANDLE_ID = 10000000L;
    static final int DEF_RESP_PLACEHOLDER_PROCESS = 1;
    private static final int ALARM_SOURCE_TYPE_MANUAL = 1;
    private static final int ALARM_SOURCE_TYPE_REPORT_ANALYSIS = 2;
    /**
     * 预警已被新预警替换（会商确认 planContentJson 切换 source_alarm_id 时标记旧预警）
     */
    static final int ALARM_STATUS_SUPERSEDED = 3;
    static final String DEF_RESP_CODE_MARKER_TOWN = "XZ";
    static final String DEF_RESP_CODE_MARKER_SINGLE = "DD";
    static final String ENSHI_CITY = "恩施市";
    final DzDefRespPlanMapper baseMapper;
    final DataAlarmMapper dataAlarmMapper;
    private final PersonSlopeMapper personSlopeMapper;
    private final IDataAlarmService dataAlarmService;
    private final IAdRegionService adRegionService;
    private final IDzProcessProgressService dzProcessProgressService;
    private final ISlopeUnitService slopeUnitService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final IDzTaskDistListService dzTaskDistListService;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final IDzTaskHandleApprovalService dzTaskHandleApprovalService;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final SseEmitterManager sseEmitterManager;
    private final ISysUserService sysUserService;
    private final DzMsgNoticeMapper dzMsgNoticeMapper;
    private final TransactionTemplate transactionTemplate;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final IDzSingleDefProgressService dzSingleDefProgressService;
    private final IMeetingService meetingService;
    private final SmsSendService smsSendService;
    final IDzTaskProcessChainNodeService taskProcessChainNodeService;
    public static final String KEY_TASKS_NUMBER = "tasksNumber";

    /**
     * 按 id 查询防御响应方案详情
     */
    @Override
    public DefRespPlanVo queryById(Long id) {
        DefRespPlanVo defRespPlanVo = baseMapper.selectVoById(id);
        if (defRespPlanVo == null) {
            return null;
        }
        validateCurrentUserDefRespPlanViewAccess(defRespPlanVo);
        enrichDefRespPlanVos(List.of(defRespPlanVo));
        return defRespPlanVo;
    }

    /**
     * 生成并保存最新方案内容
     */
    private DzDefRespContentDelegate contentDelegate;

    private DzDefRespContentDelegate contentDelegate() {
        if (contentDelegate == null) {
            contentDelegate = new DzDefRespContentDelegate(this, dzTaskHandleDetailContentService, dzProcessProgressService);
        }
        return contentDelegate;
    }

    @Override
    public String generateLatestPlanContent(DefRespPlanGenerateBo bo) {
        return contentDelegate().generateLatestPlanContent(bo);
    }

    DefRespPlan getDefRespPlanById(Long defId) {
        return contentDelegate().getDefRespPlanById(defId);
    }
    private DzDefRespOverviewDelegate overviewDelegate;

    private DzDefRespOverviewDelegate overviewDelegate() {
        if (overviewDelegate == null) {
            overviewDelegate = new DzDefRespOverviewDelegate(this);
        }
        return overviewDelegate;
    }

    @Override
    public DefRespRangeResp getDefenseRespRange() {
        return overviewDelegate().getDefenseRespRange();
    }
    @Override
    public List<DefRespPlanRangeSlopeUnitVo> getRangeSlopeUnits(Long defId) {
        if (defId == null) {
            throw new ServiceException("defId不能为空");
        }
        DefRespPlan defRespPlan = getDefRespPlanById(defId);
        if (DefRespPlanTypeEnum.REGION.getCode().equals(defRespPlan.getType())
            && splitStreets(defRespPlan.getStreets()).isEmpty()) {
            return List.of();
        }
        validateCurrentUserDefRespPlanAccess(defRespPlan.getHandleId(), defRespPlan.getCounty(), defRespPlan.getStreets());
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(defRespPlan.getType())) {
            return buildSingleRangeSlopeUnits(defRespPlan);
        }
        return buildRegionRangeSlopeUnits(defRespPlan);
    }

    /**
     * 按指定气象预警解析乡镇等级并保存会商确认内容。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long syncConsultationFromAlarm(Long defId, Long alarmId) {
        if (defId == null) {
            throw new ServiceException("defId不能为空");
        }
        if (alarmId == null) {
            throw new ServiceException("alarmId不能为空");
        }
        DataAlarmVo alarm = dataAlarmService.queryById(alarmId);
        if (alarm == null) {
            throw new ServiceException("气象预警不存在");
        }
        DefRespPlanVo defRespPlan = queryById(defId);
        String planContentJson = buildConsultationPlanContentJsonFromAlarm(alarm);
        if (StringUtils.isBlank(planContentJson)) {
            return null;
        }
        Integer roundNo = defRespPlan == null || defRespPlan.getCurrentRoundNo() == null
            ? 1
            : defRespPlan.getCurrentRoundNo();
        DzTaskHandleDetailContentVo existing = dzTaskHandleDetailContentService.queryLatestAlarmSyncedConsultation(defId, roundNo);
        if (existing != null && Objects.equals(existing.getPlanContentJson(), planContentJson)
            && StringUtils.isNotBlank(existing.getPlanContent())) {
            return null;
        }
        return dzTaskHandleDetailContentService.saveAlarmSyncedConsultation(defId, roundNo, planContentJson);
    }

    private String buildConsultationPlanContentJsonFromAlarm(DataAlarmVo alarm) {
        if (alarm == null || StringUtils.isBlank(alarm.getLevelStreetsJson())) {
            return null;
        }
        Map<String, Integer> streetWarningLevels = DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(alarm.getLevelStreetsJson());
        if (streetWarningLevels.isEmpty()) {
            return null;
        }
        Map<Integer, List<String>> streetsByDefRespLevel = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : streetWarningLevels.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            Integer defRespLevel = DefRespAlarmLevelUtil.resolveDefenseResponseLevel(alarm, entry.getValue());
            if (defRespLevel == null) {
                continue;
            }
            streetsByDefRespLevel.computeIfAbsent(defRespLevel, ignored -> new ArrayList<>())
                                 .add(entry.getKey().trim());
        }
        if (streetsByDefRespLevel.isEmpty()) {
            return null;
        }
        List<DefRespConsultationConfirmItems.Item> items = new ArrayList<>();
        streetsByDefRespLevel.forEach((defRespLevel, streets) -> {
            DefRespConsultationConfirmItems.Item item = new DefRespConsultationConfirmItems.Item();
            item.setStreets(streets);
            item.setNewLevel(defRespLevel);
            item.setAlarmId(String.valueOf(alarm.getId()));
            items.add(item);
        });
        return JacksonUtil.toJson(items);
    }

    private DzDefRespGeoAdviceDelegate geoAdviceDelegate;

    private DzDefRespGeoAdviceDelegate geoAdviceDelegate() {
        if (geoAdviceDelegate == null) {
            geoAdviceDelegate = new DzDefRespGeoAdviceDelegate(this);
        }
        return geoAdviceDelegate;
    }

    @Override
    public List<DefRespPlanChildGeoAdviceVo> listChildGeoAdvice(Long alarmId) {
        return geoAdviceDelegate().listChildGeoAdvice(alarmId);
    }

    @Override
    public List<DefRespPlanStreetGeoAdviceVo> queryStreetGeoAdvice(List<String> streets) {
        return geoAdviceDelegate().queryStreetGeoAdvice(streets);
    }
    DefRespPlan chooseLatestTownPlan(DefRespPlan left, DefRespPlan right) { return geoAdviceDelegate().chooseLatestTownPlan(left, right); }

    private DzDefRespRegionCoreDelegate regionCoreDelegate;

    private DzDefRespRegionCoreDelegate regionCoreDelegate() {
        if (regionCoreDelegate == null) {
            regionCoreDelegate = new DzDefRespRegionCoreDelegate(this);
        }
        return regionCoreDelegate;
    }

    boolean isCountyRegionPlan(DefRespPlan plan) { return regionCoreDelegate().isCountyRegionPlan(plan); }

    boolean isCountyRegionPlan(Integer type, Integer regionScopeType) { return regionCoreDelegate().isCountyRegionPlan(type, regionScopeType); }

    void validateCountyRegionStatusTransitionRole() { regionCoreDelegate().validateCountyRegionStatusTransitionRole(); }

    boolean isTownRegionPlan(DefRespPlan plan) { return regionCoreDelegate().isTownRegionPlan(plan); }

    List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return regionCoreDelegate().listTownPlansByCountyId(countyDefId); }

    boolean isTownRowActive(DefRespPlan town) { return regionCoreDelegate().isTownRowActive(town); }

    boolean isActiveExecuteStatus(Integer executeStatus) { return regionCoreDelegate().isActiveExecuteStatus(executeStatus); }

    boolean isEffectiveRegionPlan(DefRespPlan plan) { return regionCoreDelegate().isEffectiveRegionPlan(plan); }

    DefRespPlan getActiveCountyRegionPlanById(Long countyDefId) { return regionCoreDelegate().getActiveCountyRegionPlanById(countyDefId); }

    void updateActiveCountyRegionPlanOrThrow(DefRespPlan county, String errorMessage) { regionCoreDelegate().updateActiveCountyRegionPlanOrThrow(county, errorMessage); }

    void updateActiveTownRegionPlanOrThrow(DefRespPlan town, String errorMessage) { regionCoreDelegate().updateActiveTownRegionPlanOrThrow(town, errorMessage); }

    void updateActiveTownRegionPlansOrThrow(List<DefRespPlan> towns, String errorMessage) { regionCoreDelegate().updateActiveTownRegionPlansOrThrow(towns, errorMessage); }

    List<DefRespPlan> listEffectiveRegionPlans(String county, Integer regionScopeType, Long excludeId) { return regionCoreDelegate().listEffectiveRegionPlans(county, regionScopeType, excludeId); }

    void validateCountyRegionConstraints(String county, String streets, Long currentCountyId) { regionCoreDelegate().validateCountyRegionConstraints(county, streets, currentCountyId); }

    void validateCountyRegionStreetOccupancy(String county, String streets, Long currentCountyId) { regionCoreDelegate().validateCountyRegionStreetOccupancy(county, streets, currentCountyId); }

    void validateTownRegionConstraints(String county, String street, Long currentTownId) { regionCoreDelegate().validateTownRegionConstraints(county, street, currentTownId); }

    void validateRegionPlanConstraints(DefRespPlan plan) { regionCoreDelegate().validateRegionPlanConstraints(plan); }

    DefRespPlan buildRegionConstraintCandidate(DefRespPlan current, DefRespPlanBo bo) { return regionCoreDelegate().buildRegionConstraintCandidate(current, bo); }

    Integer resolveTownExecuteStatusForCounty(DefRespPlan county) { return regionCoreDelegate().resolveTownExecuteStatusForCounty(county); }

    void syncTownMirrorStatus(Long countyDefId, Integer newStatus, Date date) { regionCoreDelegate().syncTownMirrorStatus(countyDefId, newStatus, date); }
    private DzDefRespRegionAlarmDelegate regionAlarmDelegate;

    private DzDefRespRegionAlarmDelegate regionAlarmDelegate() {
        if (regionAlarmDelegate == null) {
            regionAlarmDelegate = new DzDefRespRegionAlarmDelegate(this, dzTaskDistListService);
        }
        return regionAlarmDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DefRespPlan updateCountyRegionFromAlarm(DefRespPlanVo countyVo, DataAlarmVo dataAlarmVo) {
        return regionAlarmDelegate().updateCountyRegionFromAlarm(countyVo, dataAlarmVo);
    }

    static final String DEFAULT_REGION_RESP_UNIT = "湖北省自然资源厅,湖北省气象局";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DefRespPlan generateOrUpdateRegionPlansFromAlarm(DataAlarmVo dataAlarmVo) {
        return regionAlarmDelegate().generateOrUpdateRegionPlansFromAlarm(dataAlarmVo);
    }

    private void ensureRegionDefRespAlarmProcess(DefRespPlan plan) {
        Long alarmId = plan == null ? null : plan.getSourceAlarmId();
        if (plan == null || plan.getId() == null || alarmId == null) {
            return;
        }
        DzTaskProcessChainNode existed = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            plan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode());
        if (existed != null) {
            return;
        }
        DzTaskProcessChainNode uploadNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.ALARM.getCode(),
            alarmId,
            TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName(),
            null);
        if (uploadNode == null || uploadNode.getId() == null || StringUtils.isBlank(uploadNode.getChainId())) {
            throw new ServiceException("预警触发防御响应未找到data_alarm创建阶段上传预警报告节点, defId=" + plan.getId()
                + ", alarmId=" + alarmId);
        }
        taskProcessChainNodeService.recordBizNode(uploadNode.getChainId(), TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), plan.getId(), null, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID,
            DzTaskDistListServiceImpl.AUTO_AGENT_NAME, TaskProcessSourceTypeEnum.ALARM.getCode(),
            uploadNode.getId(), "AI智能体",
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode(),
            TaskProcessStageTypeEnum.DEF_RESP.getCode());
    }

    void recordRegionDefRespStartProcess(DefRespPlan plan, Date triggerTime) {
        if (!isCountyRegionPlan(plan)) {
            return;
        }
        if (plan.getSourceAlarmId() != null) {
            return;
        }
        String chainId;
        Integer sourceType;
        chainId = taskProcessChainNodeService.resolveSavedChainIdByBizFast(TaskProcessBizTypeEnum.DEF_RESP.getCode(), plan.getId());
        if (StringUtils.isBlank(chainId)) {
            chainId = taskProcessChainNodeService.generateChainId();
        }
        sourceType = TaskProcessSourceTypeEnum.DEF_RESP.getCode();
        taskProcessChainNodeService.recordBizNode(chainId,
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), plan.getId(), null,
            DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID, DzTaskDistListServiceImpl.AUTO_AGENT_NAME,
            sourceType, null, "AI智能体",
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode(),
            TaskProcessStageTypeEnum.DEF_RESP.getCode());
    }

    void recordSingleDefRespStartProcess(DzTaskHandle handle, DefRespPlan plan, Date triggerTime) {
        if (handle == null || handle.getId() == null || plan == null || plan.getId() == null) {
            return;
        }
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(TaskProcessBizTypeEnum.HANDLE.getCode(), handle.getId());
        if (StringUtils.isBlank(chainId)) {
            throw new ServiceException("开启单点防御响应未找到处置管理主链, handleId=" + handle.getId());
        }
        taskProcessChainNodeService.recordBizNode(chainId, TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getLinkName(),
            TaskProcessChainNodeTextEnum.SINGLE_DEF_RESP_START.getTriggerReason(),
            TaskProcessBizTypeEnum.DEF_RESP.getCode(), plan.getId(), null, DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID,
            DzTaskDistListServiceImpl.AUTO_AGENT_NAME, TaskProcessSourceTypeEnum.DEF_RESP.getCode());
    }

    private String resolveSavedBizChainIdOrGenerate(Integer bizType, Long bizId) {
        String chainId = taskProcessChainNodeService.resolveSavedChainIdByBiz(bizType, bizId);
        return StringUtils.isNotBlank(chainId) ? chainId : taskProcessChainNodeService.generateChainId();
    }

    Integer resolveDefenseResponseLevel(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().resolveDefenseResponseLevel(dataAlarmVo); }

    Integer resolveDefenseResponseLevel(DataAlarmVo dataAlarmVo, Integer warningLevel) { return regionAlarmDelegate().resolveDefenseResponseLevel(dataAlarmVo, warningLevel); }

    List<String> resolveAlarmStreets(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().resolveAlarmStreets(dataAlarmVo); }

    Map<String, Integer> resolveAlarmStreetWarningLevelMap(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().resolveAlarmStreetWarningLevelMap(dataAlarmVo); }

    Integer resolveAlarmHighestWarningLevel(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().resolveAlarmHighestWarningLevel(dataAlarmVo); }

    Integer resolveAlarmHighestDefenseLevel(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().resolveAlarmHighestDefenseLevel(dataAlarmVo); }

    DefRespPlan findExistingCountyRegionPlanForAlarm(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().findExistingCountyRegionPlanForAlarm(dataAlarmVo); }

    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged) { regionAlarmDelegate().reconcileCountyRegionFromChildTowns(countyDefId, now, relationChanged); }

    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged, boolean allowCountyReset) { regionAlarmDelegate().reconcileCountyRegionFromChildTowns(countyDefId, now, relationChanged, allowCountyReset); }

    void reconcileCountyRegionFromChildTowns(Long countyDefId, Date now, boolean relationChanged,
                                             boolean allowCountyReset, boolean roundAlreadyAdvanced) {
        regionAlarmDelegate().reconcileCountyRegionFromChildTowns(
            countyDefId, now, relationChanged, allowCountyReset, roundAlreadyAdvanced);
    }

    void persistCountyAggregatedFieldsOrThrow(DefRespPlan county, Date now) { regionAlarmDelegate().persistCountyAggregatedFieldsOrThrow(county, now); }

    void persistCountyAggregatedFieldsOrThrow(DefRespPlan county, Date now, String branch) { regionAlarmDelegate().persistCountyAggregatedFieldsOrThrow(county, now, branch); }

    Long resolveCountyDefIdForTownReconcile(DefRespPlan townPlan) { return regionAlarmDelegate().resolveCountyDefIdForTownReconcile(townPlan); }

    List<DefRespPlan> listActiveTownPlansForCountyAggregation(Long countyDefId) { return regionAlarmDelegate().listActiveTownPlansForCountyAggregation(countyDefId); }

    Set<String> collectTownStreetsByCountyId(Long countyDefId) { return regionAlarmDelegate().collectTownStreetsByCountyId(countyDefId); }

    Integer resolveCountyLevelFromActiveTowns(Long countyDefId) { return regionAlarmDelegate().resolveCountyLevelFromActiveTowns(countyDefId); }

    void incrementCountyRoundNo(DefRespPlan county) { regionAlarmDelegate().incrementCountyRoundNo(county); }

    int nextRoundNo(Integer currentRoundNo) { return regionAlarmDelegate().nextRoundNo(currentRoundNo); }

    void updateCountyPlanByAlarm(DefRespPlan countyPlan, DataAlarmVo dataAlarmVo, Date now) { regionAlarmDelegate().updateCountyPlanByAlarm(countyPlan, dataAlarmVo, now); }

    String buildCountyRegionPlanName(String county, Integer level, Date referenceDate) { return regionAlarmDelegate().buildCountyRegionPlanName(county, level, referenceDate); }

    void applyCountyRegionPlanName(DefRespPlan county, Integer level, Date now) { regionAlarmDelegate().applyCountyRegionPlanName(county, level, now); }

    String buildRegionTriggerCondition(DataAlarmVo dataAlarmVo) { return regionAlarmDelegate().buildRegionTriggerCondition(dataAlarmVo); }

    String buildRegionTriggerConditionByLevel(Integer level) { return regionAlarmDelegate().buildRegionTriggerConditionByLevel(level); }

    String buildRegionTriggerCondition(String source, Integer level) { return regionAlarmDelegate().buildRegionTriggerCondition(source, level); }

    void syncActiveTownNonLevelInheritedFieldsFromCounty(Long countyDefId, DefRespPlan county, Date now) { regionAlarmDelegate().syncActiveTownNonLevelInheritedFieldsFromCounty(countyDefId, county, now); }

    DefRespPlan findEffectiveTownRegionPlanByCountyAndStreet(String county, String street) { return regionAlarmDelegate().findEffectiveTownRegionPlanByCountyAndStreet(county, street); }

    boolean syncTownPlanWithCounty(DefRespPlan townPlan, DefRespPlan countyPlan, Date now) { return regionAlarmDelegate().syncTownPlanWithCounty(townPlan, countyPlan, now); }

    void syncActiveTownInheritedFields(Long countyDefId, Integer level, String triggerCondition, Date now) { regionAlarmDelegate().syncActiveTownInheritedFields(countyDefId, level, triggerCondition, now); }

    void validateCountyRegionLevelImmutable(DefRespPlan countyPlan, Integer targetLevel) { regionAlarmDelegate().validateCountyRegionLevelImmutable(countyPlan, targetLevel); }

    private DzDefRespSmsEventFactory defRespSmsEventFactory;

    private DzDefRespSmsEventFactory defRespSmsEventFactory() {
        if (defRespSmsEventFactory == null) {
            defRespSmsEventFactory = new DzDefRespSmsEventFactory(
                this, dataAlarmMapper, adRegionService, taskProcessChainNodeService);
        }
        return defRespSmsEventFactory;
    }

    Map<Long, DzDefRespSmsEventFactory.CountySmsState> captureStartedCountySmsStates() {
        return defRespSmsEventFactory().captureStartedCountyStates();
    }

    Map<String, Integer> captureTownLevelsForSms(DefRespPlan countyPlan) {
        return defRespSmsEventFactory().captureTownLevels(countyPlan);
    }

    DefRespSmsEventSnapshot buildStartSmsEvent(DefRespPlan countyPlan, Date actionTime) {
        return defRespSmsEventFactory().buildStartEvent(countyPlan, actionTime);
    }

    DefRespSmsEventSnapshot buildEndSmsEvent(DefRespPlan countyPlan, Date actionTime,
                                             Map<String, Integer> beforeTownLevels) {
        return defRespSmsEventFactory().buildEndEvent(countyPlan, actionTime, beforeTownLevels);
    }

    void triggerAdjustedSmsForCapturedStates(Map<Long, DzDefRespSmsEventFactory.CountySmsState> beforeStates,
                                             Date actionTime) {
        if (beforeStates == null || beforeStates.isEmpty()) {
            return;
        }
        for (DzDefRespSmsEventFactory.CountySmsState beforeState : beforeStates.values()) {
            if (beforeState == null || beforeState.countyPlan() == null || beforeState.countyPlan().getId() == null) {
                continue;
            }
            DefRespPlan current = baseMapper.selectById(beforeState.countyPlan().getId());
            if (current == null || !DefRespPlanStatusEnum.isDefenseStarted(current.getStatus())) {
                continue;
            }
            Map<String, Integer> afterLevels = captureTownLevelsForSms(current);
            if (Objects.equals(beforeState.townLevels(), afterLevels)) {
                continue;
            }
            DefRespSmsEventSnapshot event = defRespSmsEventFactory().buildAdjustEvent(
                current,
                actionTime,
                beforeState.townLevels(),
                afterLevels,
                beforeState.sourceAlarmId(),
                current.getSourceAlarmId()
            );
            triggerDefRespLifecycleSmsAfterCommit(event, false);
        }
    }

    void triggerDefRespLifecycleSmsAfterCommit(DefRespSmsEventSnapshot event, boolean manual) {
        if (event == null) {
            return;
        }
        Runnable action = () -> submitDefRespLifecycleSms(event, manual);
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

    private void submitDefRespLifecycleSms(DefRespSmsEventSnapshot event, boolean manual) {
        try {
            CompletableFuture.runAsync(() -> sendDefRespLifecycleSmsSafely(event, manual), Run.executor)
                             .exceptionally(ex -> {
                                 log.error("区域防御响应生命周期短信异步任务异常, defId={}, action={}, roundNo={}",
                                     event.defId(), event.action(), event.roundNo(), ex);
                                 return null;
                             });
        } catch (Exception e) {
            log.error("提交区域防御响应生命周期短信任务失败, defId={}, action={}, roundNo={}",
                event.defId(), event.action(), event.roundNo(), e);
        }
    }

    void sendDefRespLifecycleSmsSafely(DefRespSmsEventSnapshot event, boolean manual) {
        try {
            SmsSendSummaryVo summary = dzTaskDistListService.sendDefRespLifecycleSms(event, manual);
            log.info("区域防御响应生命周期短信发送完成, defId={}, action={}, source={}, roundNo={}, total={}, success={}, skip={}, fail={}",
                event.defId(), event.action(), event.source(), event.roundNo(), summary.getTotalCount(),
                summary.getSuccessCount(), summary.getSkipCount(), summary.getFailCount());
        } catch (Exception e) {
            log.warn("区域防御响应生命周期短信发送失败，不影响主流程, defId={}, action={}, roundNo={}, err={}",
                event.defId(), event.action(), event.roundNo(), e.getMessage(), e);
        }
    }

    @Override
    public List<DefRespStartSmsPreviewVo> previewStartSms(Long defId) {
        DefRespPlan plan = getDefRespPlanById(defId);
        DefRespSmsEventSnapshot event = buildStartSmsEvent(
            plan, plan.getCreateDate() == null ? new Date() : plan.getCreateDate());
        List<DefRespStartSmsPreviewVo> previews = dzTaskDistListService.previewDefRespLifecycleSms(event);
        if (previews == null || previews.isEmpty()) {
            throw new ServiceException("当前区域防御响应无可预览的正式启动短信");
        }
        return previews;
    }

    @Override
    public SmsSendSummaryVo sendStartSms(Long defId) {
        DefRespPlan plan = getDefRespPlanById(defId);
        DefRespSmsEventSnapshot event = buildStartSmsEvent(
            plan, plan.getCreateDate() == null ? new Date() : plan.getCreateDate());
        SmsSendSummaryVo summary = dzTaskDistListService.sendDefRespLifecycleSms(event, true);
        if (summary == null || summary.getTotalCount() <= 0) {
            throw new ServiceException("当前区域防御响应无可发送的正式启动短信接收人");
        }
        return summary;
    }
    @Override
    public Long eventCount(AdRegionVo adRegionVo, List<Integer> status) {
        return overviewDelegate().eventCount(adRegionVo, status);
    }

    @Override
    public List<DefRespPlanVo> getRelationInfo(Long id) {
        return overviewDelegate().getRelationInfo(id);
    }
    private DzDefRespTownLevelDelegate townLevelDelegate;

    private DzDefRespTownLevelDelegate townLevelDelegate() {
        if (townLevelDelegate == null) {
            townLevelDelegate = new DzDefRespTownLevelDelegate(this, dataAlarmMapper, dzTaskDistListService, dzTaskHandleDetailContentService);
        }
        return townLevelDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateTownRegionLevels(List<DefRespPlanBatchRelateBo> boList) {
        return townLevelDelegate().batchUpdateTownRegionLevels(boList);
    }

    @Override
    public int matchCirculatingTownLevels(List<DefRespPlanBatchRelateBo> boList) {
        return townLevelDelegate().matchCirculatingTownLevels(boList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConsultationConfirm(DzTaskHandleDetailContentBo bo) {
        return townLevelDelegate().saveConsultationConfirm(bo);
    }

    int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset) { return townLevelDelegate().batchUpdateTownRegionLevelsInternal(boList, allowCountyReset); }

    int batchUpdateTownRegionLevelsInternal(List<DefRespPlanBatchRelateBo> boList, boolean allowCountyReset,
                                            boolean roundAlreadyAdvanced) {
        return townLevelDelegate().batchUpdateTownRegionLevelsInternal(
            boList, allowCountyReset, roundAlreadyAdvanced);
    }

    void markAlarmSuperseded(Long alarmId, Date now) { townLevelDelegate().markAlarmSuperseded(alarmId, now); }
    private DzDefRespRegionBuildDelegate regionBuildDelegate;

    private DzDefRespRegionBuildDelegate regionBuildDelegate() {
        if (regionBuildDelegate == null) {
            regionBuildDelegate = new DzDefRespRegionBuildDelegate(this, adRegionService, dzTaskDistListService, dzTaskDistListMapper, dzProcessProgressService);
        }
        return regionBuildDelegate;
    }

    DefRespPlan requireUniqueEffectiveCountyRegionPlan(String county) { return regionBuildDelegate().requireUniqueEffectiveCountyRegionPlan(county); }

    DefRespPlan createCountyRegionPlanForTownLevelUpdate(String county, Integer level, List<String> targetStreets, Date now, Long alarmId) { return regionBuildDelegate().createCountyRegionPlanForTownLevelUpdate(county, level, targetStreets, now, alarmId); }

    Long resolveCountyRegionHandleIdFromMeeting() { return regionBuildDelegate().resolveCountyRegionHandleIdFromMeeting(); }

    String resolveCountyRegionPlanCodeFromMeeting() { return regionBuildDelegate().resolveCountyRegionPlanCodeFromMeeting(); }

    MeetingInfoVo requireCountyDefRespMeetingInfo() { return regionBuildDelegate().requireCountyDefRespMeetingInfo(); }

    Map<String, List<DefRespPlan>> buildEffectiveTownPlansByStreet(String county) { return regionBuildDelegate().buildEffectiveTownPlansByStreet(county); }

    boolean syncTownPlanInheritedFields(DefRespPlan townPlan, DefRespPlan targetCounty, Date now) { return regionBuildDelegate().syncTownPlanInheritedFields(townPlan, targetCounty, now); }

    List<AdRegionVo> queryTownAdRegions(List<String> streetNames) { return regionBuildDelegate().queryTownAdRegions(streetNames); }

    List<AdRegionVo> listTownAdRegionsByCounty(String county) { return regionBuildDelegate().listTownAdRegionsByCounty(county); }

    DefRespPlan findTownByCountyAndStreet(Long countyId, String street) { return regionBuildDelegate().findTownByCountyAndStreet(countyId, street); }

    void closeTownPlan(DefRespPlan town, String reason, Date now) { regionBuildDelegate().closeTownPlan(town, reason, now); }

    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, String oneStreet, Date now) { return regionBuildDelegate().insertTownPlanUnderCounty(county, oneStreet, now); }

    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, DataAlarmVo alarm, String oneStreet, Date now) { return regionBuildDelegate().insertTownPlanUnderCounty(county, alarm, oneStreet, now); }

    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, DataAlarmVo alarm, String oneStreet, Integer defRespLevel, Date now) { return regionBuildDelegate().insertTownPlanUnderCounty(county, alarm, oneStreet, defRespLevel, now); }

    void archiveTownPlansUnderCounty(Long countyDefId, Date date) { regionBuildDelegate().archiveTownPlansUnderCounty(countyDefId, date); }
    String buildProcessProgressDescription(DefRespPlan defRespPlan, Integer status, Integer actionType, Integer roundNo) {
        if (defRespPlan == null) {
            return null;
        }
        DefRespPlanStatusEnum statusEnum = DefRespPlanStatusEnum.getByCode(status);
        if (statusEnum == null) {
            return null;
        }
        int safeRoundNo = roundNo == null ? 1 : roundNo;
        return switch (statusEnum) {
            case STARTED -> "已生成《" + StringUtils.defaultString(defRespPlan.getCounty()) + "防御响应方案V1》";
            case MODEL_ANALYZED -> buildConsultationConfirmedDescription(defRespPlan, safeRoundNo);
            case CONSULTATION_CONFIRMED -> buildAdminApprovalPendingDescription(defRespPlan, safeRoundNo);
            case APPROVAL_PASSED -> "接收到" + StringUtils.defaultString(defRespPlan.getTriggerCondition()) + ",系统开始启动" + defRespPlan.getName();
            case TASK_PUBLISHED ->
                "针对此次" + requireTypeDescription(defRespPlan.getType()) + "防御响应，已向四级六位责任体系发送防御响应告警，专项巡排查任务需在调度模式任务派发模块重新推送。";
            case ENDED -> "接收到此次防御响应终止通知，此次防御响应已结束";
            default -> statusEnum.getName();
        };
    }

    /**
     * 构建会商确认进度描述
     */
    private String buildConsultationConfirmedDescription(DefRespPlan defRespPlan, Integer roundNo) {
        DzTaskHandleApprovalBo dzTaskHandleApprovalBo = new DzTaskHandleApprovalBo();
        dzTaskHandleApprovalBo.setMeetingType(DEF_RESP_MEETING_TYPE);
        dzTaskHandleApprovalBo.setType(DzTaskHandleApprovalTypeEnum.EXPERT_CONSULTATION.getCode());
        dzTaskHandleApprovalBo.setHandleId(defRespPlan.getId());
        dzTaskHandleApprovalBo.setRoundNo(roundNo);
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalService.queryList(dzTaskHandleApprovalBo);
        if (approvals == null || approvals.isEmpty()) {
            return "专家组已会商确认方案《" + StringUtils.defaultString(defRespPlan.getCounty()) + "防御响应最终版》,同意发布";
        }
        String nicknames = approvals.stream()
                                    .map(DzTaskHandleApprovalVo::getNickname)
                                    .filter(StringUtils::isNotBlank)
                                    .distinct()
                                    .collect(Collectors.joining(","));
        if (StringUtils.isBlank(nicknames)) {
            return "专家组已会商确认方案《" + StringUtils.defaultString(defRespPlan.getCounty()) + "防御响应最终版》,同意发布";
        }
        return "专家组" + nicknames + "已会商确认方案《" + StringUtils.defaultString(defRespPlan.getCounty()) + "防御响应最终版》,同意发布";
    }

    /**
     * 构建行政审批待审进度描述
     */
    String buildAdminApprovalPendingDescription(DefRespPlan defRespPlan, Integer roundNo) {
        String approverName = resolveAdminApproverNames(defRespPlan.getId(), roundNo);
        return "《" + StringUtils.defaultString(defRespPlan.getName()) + "》已流转至行政审批环节,审批人:"
            + StringUtils.defaultIfBlank(approverName, "");
    }

    String buildRegionApprovalPendingDescription(Long detailId, String approverName) {
        return "专家组已会商确认《区域防御响应方案变动（编号：" + detailId + "）》,同意发布,已流转至行政审批环节,由"
            + StringUtils.defaultIfBlank(approverName, "审批人") + "审批";
    }

    String buildRegionApprovalPassedDescription(Long detailId, String approverName) {
        return StringUtils.defaultIfBlank(approverName, "审批人")
            + "审批《区域防御响应方案变动（编号：" + detailId + "）》通过,已更新完毕";
    }

    /**
     * 获取方案类型描述
     */
    private String requireTypeDescription(Integer type) {
        DefRespPlanTypeEnum typeEnum = DefRespPlanTypeEnum.getByCode(type);
        return typeEnum == null ? "此次" : typeEnum.getName();
    }

    /**
     * 统计方案下防御任务总数
     */
    long countTotalDefRespTasksByDefId(Long defId) {
        if (defId == null) {
            return 0;
        }
        return dzTaskDistListMapper.selectCount(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getDefId, defId)
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                    .eq(DzTaskDistList::getDelete, 0)
        );
    }

    /**
     * 统计方案下已完成防御任务数
     */
    long countCompletedDefRespTasksByDefId(Long defId) {
        if (defId == null) {
            return 0;
        }
        return dzTaskDistListMapper.selectCount(
            Wrappers.<DzTaskDistList>lambdaQuery()
                    .eq(DzTaskDistList::getDefId, defId)
                    .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                    .eq(DzTaskDistList::getDelete, 0)
                    .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED)
                    .in(DzTaskDistList::getTaskSource,
                        DzTaskDistList.PLAN_NAME_MONITOR,
                        DzTaskDistList.PLAN_NAME_PATROL)
        );
    }

    /**
     * 启动单点防御响应方案
     */
    private DzDefRespSingleDelegate singleDelegate;

    private DzDefRespSingleDelegate singleDelegate() {
        if (singleDelegate == null) {
            singleDelegate = new DzDefRespSingleDelegate(this, dzTaskHandleMapper, dzTaskHandleSceneRecordService, dzSingleDefProgressService);
        }
        return singleDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startSingle(Long handleId) {
        return singleDelegate().startSingle(handleId);
    }
    String buildDefRespPlanCode(String marker) {
        return DefRespPlanCodeUtil.buildCode(marker);
    }

    /**
     * 拆分逗号分隔的街道字符串
     */
    static List<String> splitStreets(String streets) {
        if (StringUtils.isBlank(streets)) {
            return List.of();
        }
        Set<String> set = Stream.of(streets.split("[,，]"))
                                .map(String::trim)
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(set);
    }

    static List<String> normalizeStreetNames(Collection<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return List.of();
        }
        return streets.stream()
                      .filter(StringUtils::isNotBlank)
                      .map(String::trim)
                      .distinct()
                      .toList();
    }

    /**
     * 追加街道包含查询条件
     */
    private DzDefRespQueryDelegate queryDelegate;

    private DzDefRespQueryDelegate queryDelegate() {
        if (queryDelegate == null) {
            queryDelegate = new DzDefRespQueryDelegate(this, dzUserAdRegionService, adRegionService, dzTaskHandleMapper, slopeUnitService);
        }
        return queryDelegate;
    }

    void appendStreetContainsCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) { queryDelegate().appendStreetContainsCondition(lqw, street); }

    @Override
    public TableDataInfo<DefRespPlanVo> listByCondition(DefRespPlanDto bo, PageQuery pageQuery) {
        return queryDelegate().listByCondition(bo, pageQuery);
    }

    @Override
    public DefRespTownStatVo getCirculatingTownStats(Long id) {
        Integer countyLevel = resolveTownStatsCountyLevel(id);
        Long parentDefId = id;
        boolean filterByParent = id != null;
        validateActiveTownRegionUniqueness(parentDefId, filterByParent);
        List<DefRespTownStatItemVo> stats = baseMapper.selectCirculatingTownStats(parentDefId, filterByParent);
        List<DefRespTownScopeVo> scopes = baseMapper.selectCirculatingTownScopes(parentDefId, filterByParent);
        DefRespTownStatVo vo = new DefRespTownStatVo();
        if (stats == null || stats.isEmpty()) {
            DefRespTownStatItemVo total = emptyTownStatItem(countyLevel);
            fillTownRuntimeFields(total, scopes, true);
            vo.setTotal(total);
            return vo;
        }
        fillTownPopulation(stats, scopes);
        List<DefRespTownStatItemVo> levelStats = new ArrayList<>();
        for (DefRespTownStatItemVo item : stats) {
            boolean totalItem = item.getLevel() == null;
            if (totalItem) {
                item.setLevel(countyLevel);
            }
            normalizeTownStatItem(item);
            fillTownRuntimeFields(item, scopes, totalItem);
            if (totalItem) {
                vo.setTotal(item);
            } else {
                levelStats.add(item);
            }
        }
        if (vo.getTotal() == null) {
            DefRespTownStatItemVo total = emptyTownStatItem(countyLevel);
            fillTownRuntimeFields(total, scopes, true);
            vo.setTotal(total);
        }
        vo.setLevelStats(levelStats);
        return vo;
    }

    private Integer resolveTownStatsCountyLevel(Long id) {
        if (id == null) {
            return baseMapper.selectCirculatingCountyLevel();
        }
        DefRespPlan defRespPlan = baseMapper.selectById(id);
        if (defRespPlan == null
            || !Objects.equals(defRespPlan.getType(), DefRespPlanTypeEnum.REGION.getCode())
            || !Objects.equals(defRespPlan.getRegionScopeType(), RegionScopeTypeEnum.COUNTY.getCode())
            || !Objects.equals(defRespPlan.getDeleted(), 0)) {
            throw new ServiceException("县级防御响应不存在或已删除");
        }
        return defRespPlan.getLevel();
    }

    private void validateActiveTownRegionUniqueness(Long parentDefId, boolean filterByParent) {
        List<DefRespPlan> activeTowns = baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                .eq(DefRespPlan::getDeleted, 0)
                .in(DefRespPlan::getExecuteStatus,
                    DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                    DefRespExecuteStatusEnum.RUNNING.getCode())
                .eq(filterByParent, DefRespPlan::getParentDefId, parentDefId)
        );
        if (activeTowns == null || activeTowns.isEmpty()) {
            return;
        }
        Map<String, Long> countByTown = activeTowns.stream()
            .filter(Objects::nonNull)
            .filter(plan -> StringUtils.isNotBlank(plan.getStreets()))
            .collect(Collectors.groupingBy(this::buildTownUniquenessKey, Collectors.counting()));
        countByTown.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue() > 1)
            .findFirst()
            .ifPresent(entry -> {
                String[] parts = entry.getKey().split("\\|", -1);
                String street = parts.length >= 3 ? parts[2] : entry.getKey();
                throw new ServiceException("乡镇" + street + "同时存在多条未关闭或未归档的乡镇级区域防御响应");
            });
    }

    private String buildTownUniquenessKey(DefRespPlan plan) {
        return StringUtils.defaultString(plan.getCounty())
            + "|" + StringUtils.defaultString(String.valueOf(plan.getParentDefId()))
            + "|" + StringUtils.trim(plan.getStreets());
    }

    private String buildAllCirculatingStreets(List<DefRespTownScopeVo> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return scopes.stream()
            .filter(Objects::nonNull)
            .map(DefRespTownScopeVo::getStreet)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .collect(Collectors.joining(","));
    }

    private void fillTownRuntimeFields(DefRespTownStatItemVo item, List<DefRespTownScopeVo> scopes, boolean includeAllLevels) {
        if (item == null) {
            return;
        }
        Integer level = item.getLevel();
        List<DefRespTownScopeVo> matchedScopes = includeAllLevels ? scopes : filterTownScopesByLevel(scopes, level);
        item.setAllStreets(buildAllCirculatingStreets(matchedScopes));
        DefRespTownScopeVo latestScope = findLatestTownScope(matchedScopes);
        if (latestScope != null) {
            item.setStatus(latestScope.getStatus());
            item.setExecuteStatus(latestScope.getExecuteStatus());
            item.setStartTime(latestScope.getCreateDate());
        }
    }

    private List<DefRespTownScopeVo> filterTownScopesByLevel(List<DefRespTownScopeVo> scopes, Integer level) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return scopes.stream()
            .filter(Objects::nonNull)
            .filter(scope -> level == null || Objects.equals(scope.getLevel(), level))
            .toList();
    }

    private DefRespTownScopeVo findLatestTownScope(List<DefRespTownScopeVo> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        DefRespTownScopeVo latest = null;
        for (DefRespTownScopeVo scope : scopes) {
            if (scope == null) {
                continue;
            }
            if (latest == null || compareTownScopeUpdateTime(scope, latest) > 0) {
                latest = scope;
            }
        }
        return latest;
    }

    private int compareTownScopeUpdateTime(DefRespTownScopeVo left, DefRespTownScopeVo right) {
        Date leftTime = left.getUpdateDate() == null ? left.getCreateDate() : left.getUpdateDate();
        Date rightTime = right.getUpdateDate() == null ? right.getCreateDate() : right.getUpdateDate();
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return -1;
        }
        if (rightTime == null) {
            return 1;
        }
        return leftTime.compareTo(rightTime);
    }

    private void fillTownPopulation(List<DefRespTownStatItemVo> stats, List<DefRespTownScopeVo> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return;
        }
        List<CompletableFuture<TownPopulationStat>> futures = scopes.stream()
            .filter(Objects::nonNull)
            .map(scope -> CompletableFuture.supplyAsync(() -> new TownPopulationStat(
                scope.getLevel(),
                countTownPopulation(scope.getCounty(), scope.getStreet())
            ), Run.executor))
            .toList();
        Map<Integer, Long> populationByLevel = new HashMap<>();
        long totalPopulation = 0L;
        for (CompletableFuture<TownPopulationStat> future : futures) {
            TownPopulationStat stat = future.join();
            totalPopulation += stat.population();
            if (stat.level() != null) {
                populationByLevel.merge(stat.level(), stat.population(), Long::sum);
            }
        }
        for (DefRespTownStatItemVo item : stats) {
            if (item == null) {
                continue;
            }
            long population = item.getLevel() == null
                ? totalPopulation
                : populationByLevel.getOrDefault(item.getLevel(), 0L);
            item.setPopulationTenThousand(toTenThousand(population));
        }
    }

    private Long countTownPopulation(String county, String street) {
        if (StringUtils.isBlank(street)) {
            return 0L;
        }
        List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(List.of(street.trim()));
        if (slopeUnits == null || slopeUnits.isEmpty()) {
            return 0L;
        }
        List<String> slopeUnitIds = slopeUnits.stream()
            .filter(Objects::nonNull)
            .filter(item -> StringUtils.isBlank(county) || StringUtils.equals(county, item.getCounty()))
            .map(SlopeUnitVo::getId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (slopeUnitIds.isEmpty()) {
            return 0L;
        }
        Long count = personSlopeMapper.countBySlopeUnitId(
            Wrappers.<PersonSlope>lambdaQuery().in(PersonSlope::getSlopeUnitId, slopeUnitIds)
        );
        return count == null ? 0L : count;
    }

    private BigDecimal toTenThousand(long population) {
        return BigDecimal.valueOf(population).divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
    }

    private record TownPopulationStat(Integer level, long population) {
    }

    private DefRespTownStatItemVo emptyTownStatItem(Integer level) {
        DefRespTownStatItemVo item = new DefRespTownStatItemVo();
        item.setLevel(level);
        normalizeTownStatItem(item);
        return item;
    }

    private void normalizeTownStatItem(DefRespTownStatItemVo item) {
        if (item == null) {
            return;
        }
        item.setLevelName(LevelCodeUtil.resolveDefenseResponseDisplayName(item.getLevel()));
        item.setTownCount(item.getTownCount() == null ? 0L : item.getTownCount());
        item.setAreaSquareKilometer(normalizeDecimal(item.getAreaSquareKilometer(), 2));
        item.setPopulationTenThousand(normalizeDecimal(item.getPopulationTenThousand(), 4));
        item.setDisasterPointCount(item.getDisasterPointCount() == null ? 0L : item.getDisasterPointCount());
    }

    private BigDecimal normalizeDecimal(BigDecimal value, int scale) {
        return (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
    }

    AdRegionVo requireCurrentUserDefRespPlanRegion() { return queryDelegate().requireCurrentUserDefRespPlanRegion(); }

    List<AdRegionVo> listCurrentUserDefRespPlanRegions() { return queryDelegate().listCurrentUserDefRespPlanRegions(); }

    void appendCurrentUserDefRespPlanPermission(LambdaQueryWrapper<DefRespPlan> lqw, boolean includeAssociatedCountyRegion) { queryDelegate().appendCurrentUserDefRespPlanPermission(lqw, includeAssociatedCountyRegion); }

    void validateCurrentUserDefRespPlanViewAccess(DefRespPlanVo defRespPlanVo) { queryDelegate().validateCurrentUserDefRespPlanViewAccess(defRespPlanVo); }

    void validateCurrentUserDefRespPlanAccess(Long handleId, String county, String streets) { queryDelegate().validateCurrentUserDefRespPlanAccess(handleId, county, streets); }

    boolean hasDefRespPlanViewAccess(AdRegionVo adRegionVo, DefRespPlanVo defRespPlanVo) { return queryDelegate().hasDefRespPlanViewAccess(adRegionVo, defRespPlanVo); }

    void appendSingleDefRespPlanPermission(LambdaQueryWrapper<DefRespPlan> lqw, AdRegionVo adRegionVo, boolean includeAssociatedCountyRegion) { queryDelegate().appendSingleDefRespPlanPermission(lqw, adRegionVo, includeAssociatedCountyRegion); }

    void appendDefRespPlanViewStreetCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) { queryDelegate().appendDefRespPlanViewStreetCondition(lqw, street); }

    void appendCountyRegionViewStreetCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) { queryDelegate().appendCountyRegionViewStreetCondition(lqw, street); }

    boolean matchesAssociatedCountyRegionPlan(AdRegionVo adRegionVo, String county, String streets) { return queryDelegate().matchesAssociatedCountyRegionPlan(adRegionVo, county, streets); }

    boolean matchesRegion(AdRegionVo adRegionVo, String province, String city, String county, String street, String village) { return queryDelegate().matchesRegion(adRegionVo, province, city, county, street, village); }

    void enrichDefRespPlanVos(List<DefRespPlanVo> vos) { queryDelegate().enrichDefRespPlanVos(vos); }

    Map<Long, String> buildSlopeUnitIdByHandleId(List<Long> handleIds) { return queryDelegate().buildSlopeUnitIdByHandleId(handleIds); }

    List<DefRespPlanRangeSlopeUnitVo> buildSingleRangeSlopeUnits(DefRespPlan defRespPlan) { return queryDelegate().buildSingleRangeSlopeUnits(defRespPlan); }

    List<DefRespPlanRangeSlopeUnitVo> buildRegionRangeSlopeUnits(DefRespPlan defRespPlan) { return queryDelegate().buildRegionRangeSlopeUnits(defRespPlan); }

    DefRespPlanRangeSlopeUnitVo buildRangeSlopeUnitVo(SlopeUnitVo slopeUnitVo) { return queryDelegate().buildRangeSlopeUnitVo(slopeUnitVo); }

    Map<String, Integer> buildSlopeUnitCountByStreetKey(List<String> streetsList) { return queryDelegate().buildSlopeUnitCountByStreetKey(streetsList); }

    Map<String, List<DefRespRange>> buildRegionRangesByStreetKey(List<DefRespPlan> regionPlans) { return queryDelegate().buildRegionRangesByStreetKey(regionPlans); }

    String joinStreetKey(String streets) { return queryDelegate().joinStreetKey(streets); }

    LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo) { return queryDelegate().buildQueryWrapper(bo); }

    LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo, boolean withOrderBy) { return queryDelegate().buildQueryWrapper(bo, withOrderBy); }
    @Override
    public Boolean insertByBo(DefRespPlanBo bo) {
        return insertByBo(bo, false);
    }

    /**
     * 新增防御响应方案（可选跳过区域约束）
     */
    Boolean insertByBo(DefRespPlanBo bo, boolean skipRegionConstraint) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            LambdaQueryWrapper<DefRespPlan> defRespPlanLambdaQueryWrapper = buildQueryWrapper(bo, false);
            long count = baseMapper.selectCount(defRespPlanLambdaQueryWrapper);
            if (count > 0) {
                log.error("已存在相同编码的防御响应方案，请勿重复添加");
                return false;
            }
            // 单点方案关联区域 regId：仅绑定乡镇级区域防御响应行（不指向县级）
            if (DefRespPlanTypeEnum.SINGLE.getCode().equals(bo.getType())) {
                DefRespPlanBo defRespPlanBo = new DefRespPlanBo();
                defRespPlanBo.setType(DefRespPlanTypeEnum.REGION.getCode());
                LambdaQueryWrapper<DefRespPlan> buildQueryWrapper = buildQueryWrapper(defRespPlanBo);
                buildQueryWrapper.in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes());
                buildQueryWrapper.like(DefRespPlan::getStreets, bo.getStreets());
                buildQueryWrapper.eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode());
                buildQueryWrapper.orderByDesc(DefRespPlan::getCreateDate);
                List<DefRespPlan> defRespPlans = baseMapper.selectList(buildQueryWrapper);
                if (defRespPlans != null && !defRespPlans.isEmpty()) {
                    bo.setRegId(defRespPlans.getFirst().getId());
                }
            }
            if (bo.getStatus() == null) {
                bo.setStatus(DefRespPlanStatusEnum.STARTED.getCode());
            }
            fillResponsibleContactFromCurrentUserIfNeeded(bo);
            if (bo.getCurrentRoundNo() == null) {
                bo.setCurrentRoundNo(1);
            }
            Date date = new Date();
            if (bo.getCreateDate() == null) {
                bo.setCreateDate(date);
            }
            bo.setUpdateDate(date);
            bo.setDeleted(0);
            DefRespPlan add = MapstructUtils.convert(bo, DefRespPlan.class);
            validEntityBeforeSave(add);
            if (!skipRegionConstraint) {
                validateRegionPlanConstraints(add);
            }
            boolean flag = baseMapper.insert(add) > 0;
            if (flag) {
                bo.setId(add.getId());
                saveInitialPlanOnCreate(add);
                if (isCountyRegionPlan(add)) {
                    if (add.getSourceAlarmId() == null) {
                        recordRegionDefRespStartProcess(add, add.getCreateDate());
                    } else {
                        ensureRegionDefRespAlarmProcess(add);
                    }
                }
            }
            return flag;
        }));
    }

    private void fillResponsibleContactFromCurrentUserIfNeeded(DefRespPlanBo bo) {
        if (bo == null || !DefRespPlanTypeEnum.REGION.getCode().equals(bo.getType())) {
            return;
        }
        if (StringUtils.isNotBlank(bo.getResponsiblePerson()) && StringUtils.isNotBlank(bo.getResponsiblePersonPhone())) {
            return;
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            return;
        }
        if (StringUtils.isBlank(bo.getResponsiblePerson())) {
            bo.setResponsiblePerson(StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername()));
        }
        if (StringUtils.isBlank(bo.getResponsiblePersonPhone())) {
            bo.setResponsiblePersonPhone(loginUser.getPhonenumber());
        }
    }

    /**
     * 县级方案创建时初始化初报与终报；若已有会商确认（content_type=7）则优先用其内容。
     */
    void saveInitialPlanOnCreate(DefRespPlan defRespPlan) { contentDelegate().saveInitialPlanOnCreate(defRespPlan); }
    @Override
    public R<Map<String, Object>> updateByBo(DefRespPlanBo bo) {
        DefRespPlan update = MapstructUtils.convert(bo, DefRespPlan.class);
        DefRespPlan defRespPlan = getDefRespPlanById(bo.getId());
        validateCurrentUserDefRespPlanAccess(defRespPlan.getHandleId(), defRespPlan.getCounty(), defRespPlan.getStreets());
        validateCountyRegionLevelImmutable(defRespPlan, bo.getLevel());
        DefRespPlan candidate = buildRegionConstraintCandidate(defRespPlan, bo);
        Map<String, Object> params = new HashMap<>();
        Date date = new Date();
        Integer newStatus = bo.getStatus();
        Integer oldStatus = defRespPlan.getStatus();
        if (newStatus == null || oldStatus == null || Objects.equals(newStatus, oldStatus)) {
            validateRegionPlanConstraints(candidate);
            update.setUpdateDate(date);
            int i = baseMapper.updateById(update);
            if (i > 0) {
                return R.ok("方案内容已更新", params);
            }
            throw new ServiceException("方案内容更新失败");
        }
        throw new ServiceException("状态流转已迁移至独立接口 /dizai/defRespPlan/processNext/{id}");
    }

    /**
     * 提交行政审批；以已提交会商确认（content_type=7, status=1）覆盖终报。
     */
    private DzDefRespProcessDelegate processDelegate;

    private DzDefRespProcessDelegate processDelegate() {
        if (processDelegate == null) {
            processDelegate = new DzDefRespProcessDelegate(this, dzTaskHandleDetailContentService, dzTaskHandleApprovalService,
                dzTaskDistListService, dzTaskDistListMapper, dzProcessProgressService, meetingService);
        }
        return processDelegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateApprovalStatusByBo(DefRespPlanApprovalStatusBo bo) {
        return processDelegate().updateApprovalStatusByBo(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRegionApprovalPassed(Long handleId, Long approvalId) {
        processDelegate().handleRegionApprovalPassed(handleId, approvalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> processNext(Long id) {
        return processDelegate().processNext(id);
    }

    R<Map<String, Object>> processStatusTransition(DefRespPlanBo bo, DefRespPlan defRespPlan) { return processDelegate().processStatusTransition(bo, defRespPlan); }
    void saveDefRespPlanHistory(DefRespPlan defRespPlan, String content, Integer roundNo) { contentDelegate().saveDefRespPlanHistory(defRespPlan, content, roundNo); }

    String generatePlanContent(DefRespPlan defRespPlan, DefRespPlanGenerateBo bo) { return contentDelegate().generatePlanContent(defRespPlan, bo); }

    DefRespPlanGenerateBo buildGenerateBoWithDefaultValues(DefRespPlan defRespPlan, DefRespPlanGenerateBo bo, Date currentDate) { return contentDelegate().buildGenerateBoWithDefaultValues(defRespPlan, bo, currentDate); }

    Map<String, String> buildPlanParamsByType(Integer type, DefRespPlanGenerateBo bo, Date currentDate, Date planCreateDate) { return contentDelegate().buildPlanParamsByType(type, bo, currentDate, planCreateDate); }

    void putMonitorFrequencyParams(Map<String, String> params, DefRespPlanGenerateBo bo) { contentDelegate().putMonitorFrequencyParams(params, bo); }

    String fillPlanContentByType(Integer type, Map<String, String> planParams, Integer level) { return contentDelegate().fillPlanContentByType(type, planParams, level); }

    String defaultIfBlank(String value, String defaultValue) { return contentDelegate().defaultIfBlank(value, defaultValue); }
    void validateApprovalSubmitTarget(DefRespPlan defRespPlan) { processDelegate().validateApprovalSubmitTarget(defRespPlan); }

    List<DefRespPlanBatchRelateBo> buildBatchRelateBoList(String planContentJson) { return processDelegate().buildBatchRelateBoList(planContentJson); }

    String resolveAdminApproverNames(Long handleId, Integer roundNo) { return processDelegate().resolveAdminApproverNames(handleId, roundNo); }

    void refreshAdminApprovalPendingProgress(DefRespPlan defRespPlan, Integer roundNo) { processDelegate().refreshAdminApprovalPendingProgress(defRespPlan, roundNo); }

    String resolveApproverNameByApprovalId(Long approvalId) { return processDelegate().resolveApproverNameByApprovalId(approvalId); }

    void insertRegionApprovalProgress(Long defId, Integer status, Integer roundNo, String description) { processDelegate().insertRegionApprovalProgress(defId, status, roundNo, description); }

    void approveRegionDefRespPlan(DefRespPlan defRespPlan) { processDelegate().approveRegionDefRespPlan(defRespPlan); }

    boolean hasProcessProgress(Long defId, Integer status, Integer roundNo) { return processDelegate().hasProcessProgress(defId, status, roundNo); }
    private void validEntityBeforeSave(DefRespPlan entity) {
        Assert.notNull(entity.getCode(), "编码不能为空");
        Assert.notNull(entity.getName(), "名称不能为空");
        Assert.notNull(entity.getType(), "类型不能为空");
        Assert.notNull(entity.getStatus(), "状态不能为空");
        Assert.notNull(entity.getCounty(), "所属区/县/县级市不能为空");
        Assert.notNull(entity.getStreets(), "街道不能为空");
        Assert.notNull(entity.getResponsibilityUnit(), "责任单位不能为空");
        boolean allowEmptyResponsibleContact = DefRespPlanTypeEnum.REGION.getCode().equals(entity.getType())
            && DefRespPlanStatusEnum.UNSTARTED.getCode().equals(entity.getStatus());
        if (!allowEmptyResponsibleContact) {
            Assert.notNull(entity.getResponsiblePerson(), "责任人不能为空");
            Assert.notNull(entity.getResponsiblePersonPhone(), "责任人手机号不能为空");
        }
        Assert.notNull(entity.getLevel(), "响应级别不能为空");
        Assert.notNull(entity.getCreateDate(), "创建时间不能为空");
        Assert.notNull(entity.getUpdateDate(), "更新时间不能为空");
        Assert.notNull(entity.getDeleted(), "删除状态不能为空");
        if (entity.getApprovalStatus() == null) {
            entity.setApprovalStatus(DefRespApprovalStatusEnum.NONE.getCode());
        }
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(entity.getType())) {
            Assert.notNull(entity.getHandleId(), "HandleId不能为空");
            Assert.notNull(entity.getCenter(), "中心点不能为空");
        }
        if (DefRespPlanTypeEnum.REGION.getCode().equals(entity.getType())) {
            Assert.notNull(entity.getTriggerCondition(), "触发条件不能为空");
            Assert.notNull(entity.getRegionScopeType(), "区域方案须填写区域层级（县级/乡镇）");
            if (RegionScopeTypeEnum.TOWN.getCode().equals(entity.getRegionScopeType())) {
                Assert.notNull(entity.getParentDefId(), "乡镇级区域方案须填写父级县级方案 id");
            }
        }
    }

    /**
     * 批量删除防御响应方案
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 获取防御响应方案树形统计
     */
    private DzDefRespTreeDelegate treeDelegate;

    private DzDefRespTreeDelegate treeDelegate() {
        if (treeDelegate == null) {
            treeDelegate = new DzDefRespTreeDelegate(this, adRegionService);
        }
        return treeDelegate;
    }

    @Override
    public List<Tree<String>> getTree() {
        return treeDelegate().getTree();
    }

    void appendCurrentUserDefRespPlanTreePermission(LambdaQueryWrapper<DefRespPlan> lqw, AdRegionVo adRegionVo) { treeDelegate().appendCurrentUserDefRespPlanTreePermission(lqw, adRegionVo); }

    List<DefRespPlan> filterCurrentUserVisibleTreePlans(List<DefRespPlan> plans, AdRegionVo adRegionVo) { return treeDelegate().filterCurrentUserVisibleTreePlans(plans, adRegionVo); }

    boolean canCurrentUserViewTreePlan(DefRespPlan plan, AdRegionVo adRegionVo, Set<String> visibleCounties) { return treeDelegate().canCurrentUserViewTreePlan(plan, adRegionVo, visibleCounties); }

    boolean countyRegionContainsStreet(String streets, String currentStreet) { return treeDelegate().countyRegionContainsStreet(streets, currentStreet); }

    Set<String> resolveTreeVisibleCounties(AdRegionVo adRegionVo) { return treeDelegate().resolveTreeVisibleCounties(adRegionVo); }

    String resolveAdRegionCountyName(AdRegionVo adRegionVo) { return treeDelegate().resolveAdRegionCountyName(adRegionVo); }

    String resolveAdRegionStreetName(AdRegionVo adRegionVo) { return treeDelegate().resolveAdRegionStreetName(adRegionVo); }

    boolean isStreetLevelTreeRegion(AdRegionVo adRegionVo) { return treeDelegate().isStreetLevelTreeRegion(adRegionVo); }

    long countTreeVisiblePlans(List<DefRespPlan> typeList) { return treeDelegate().countTreeVisiblePlans(typeList); }

    Tree<String> buildStatusGroupNode(Integer type, List<DefRespPlan> typeList, String groupKey, Predicate<DefRespPlan> statusPredicate) { return treeDelegate().buildStatusGroupNode(type, typeList, groupKey, statusPredicate); }

    Tree<String> buildReqIdGroupNode(String parentId, boolean reqIdEmpty, List<DefRespPlan> groupList) { return treeDelegate().buildReqIdGroupNode(parentId, reqIdEmpty, groupList); }
    private DzDefRespAdminApprovalDelegate adminApprovalDelegate;

    private DzDefRespAdminApprovalDelegate adminApprovalDelegate() {
        if (adminApprovalDelegate == null) {
            adminApprovalDelegate = new DzDefRespAdminApprovalDelegate(this, dzTaskHandleApprovalService, transactionTemplate,
                sysUserService, dzMsgNoticeMapper, sseEmitterManager, smsSendService, dzTaskHandleMapper, dzUserAdRegionService);
        }
        return adminApprovalDelegate;
    }

    @Override
    public R<String> insertExecutiveByBo(Long handleId, Integer meetingType, Integer handleProcess, Integer roundNo) {
        return adminApprovalDelegate().insertExecutiveByBo(handleId, meetingType, handleProcess, roundNo);
    }
}
