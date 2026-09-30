/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespConsultationConfirmStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consultation.DefRespConsultationConfirmItems;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleDetailContentMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespAlarmLevelUtil;
import cn.edu.pku.whai.geological.disaster.service.utils.EvacuationSchemeParser;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 处置管理详情内容服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DzTaskHandleDetailContentServiceImpl implements IDzTaskHandleDetailContentService {

    private final DzTaskHandleDetailContentMapper baseMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final IDataAlarmService dataAlarmService;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;

    @Override
    public DzTaskHandleDetailContentVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<DzTaskHandleDetailContentVo> queryPageList(DzTaskHandleDetailContentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzTaskHandleDetailContent> lqw = buildQueryWrapper(bo);
        Page<DzTaskHandleDetailContentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzTaskHandleDetailContentVo> queryList(DzTaskHandleDetailContentBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public DzTaskHandleDetailContentVo queryLatest(Integer bizType, Long bizId, Integer contentType, Integer roundNo) {
        if (bizId == null && !canBizIdBeEmpty(bizType, contentType)) {
            return null;
        }
        Integer effectiveRoundNo = resolveQueryRoundNo(bizType, bizId, roundNo);
        DzTaskHandleDetailContentBo bo = new DzTaskHandleDetailContentBo();
        bo.setBizType(bizType);
        bo.setBizId(bizId);
        bo.setContentType(contentType);
        bo.setRoundNo(effectiveRoundNo);
        List<DzTaskHandleDetailContentVo> list = queryList(bo);
        if (list.isEmpty() && effectiveRoundNo != null && effectiveRoundNo > 1) {
            bo.setRoundNo(effectiveRoundNo - 1);
            list = queryList(bo);
        }
        for (DzTaskHandleDetailContentVo vo : list) {
            if (Integer.valueOf(1).equals(vo.getIsLatest())) {
                return vo;
            }
        }
        if (Objects.equals(bizType, DetailBizTypeEnum.DEF_RESP_PLAN.getCode())
            && Objects.equals(contentType, DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode())
            && Objects.equals(effectiveRoundNo, 1)
            && list.isEmpty()) {
            return queryLatest(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), bizId, DetailContentTypeEnum.INITIAL_REPORT.getCode(), 1);
        }
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DzTaskHandleDetailContentBo bo) {
        fillPlanContentFromJsonIfNeeded(bo);
        fillPlanContentJsonIfNeeded(bo);
        DzTaskHandleDetailContent add = MapstructUtils.convert(bo, DzTaskHandleDetailContent.class);
        fillCurrentUser(add);
        Integer resolvedRoundNo = resolveRoundNo(add);
        add.setRoundNo(resolvedRoundNo);
        bo.setRoundNo(resolvedRoundNo);
        validEntityBeforeSave(add);
        Date now = new Date();
        if (add.getCreateDate() == null) {
            add.setCreateDate(now);
        }
        add.setUpdateDate(now);
        if (add.getDeleted() == null) {
            add.setDeleted(0);
        }
        if (add.getIsLatest() == null) {
            add.setIsLatest(1);
        }
        if (add.getStatus() == null) {
            add.setStatus(0);
        }
        if (Integer.valueOf(1).equals(add.getIsLatest())) {
            clearLatestFlag(add.getBizType(), add.getBizId(), add.getContentType(), add.getRoundNo());
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    private void fillPlanContentFromJsonIfNeeded(DzTaskHandleDetailContentBo bo) {
        if (bo == null) {
            return;
        }
        if (!DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bo.getBizType())) {
            return;
        }
        if (!DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode().equals(bo.getContentType())) {
            return;
        }
        if (StringUtils.isNotBlank(bo.getPlanContent())) {
            return;
        }
        if (StringUtils.isBlank(bo.getPlanContentJson())) {
            return;
        }
        bo.setPlanContent(buildConsultationPlanContent(bo.getPlanContentJson(), bo.getBizId()));
    }

    private String buildConsultationPlanContent(String planContentJson, Long defId) {
        String city = null;
        String triggerCondition = null;
        String responsibilityUnit = null;
        Date planCreateDate = null;
        if (defId != null) {
            DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(defId);
            if (defRespPlan != null) {
                city = defRespPlan.getCounty();
                triggerCondition = defRespPlan.getTriggerCondition();
                responsibilityUnit = defRespPlan.getResponsibilityUnit();
                planCreateDate = defRespPlan.getCreateDate();
            }
        }
        return DefRespConsultationConfirmItems.buildReportContentFromPlanContentJson(
            planContentJson, city, triggerCondition, responsibilityUnit, planCreateDate, this::resolveDefenseResponseLevelFromAlarm);
    }

    private Integer resolveDefenseResponseLevelFromAlarm(Long alarmId, Integer warningLevel) {
        if (warningLevel == null) {
            return null;
        }
        if (alarmId == null) {
            return warningLevel;
        }
        DataAlarmVo alarm = dataAlarmService.queryById(alarmId);
        if (alarm == null) {
            return warningLevel;
        }
        return DefRespAlarmLevelUtil.resolveDefenseResponseLevel(alarm, warningLevel);
    }

    private void fillPlanContentJsonIfNeeded(DzTaskHandleDetailContentBo bo) {
        if (bo == null) {
            return;
        }
        if (!DetailBizTypeEnum.TASK_HANDLE.getCode().equals(bo.getBizType())) {
            return;
        }
        if (!DetailContentTypeEnum.EVACUATION_PLAN.getCode().equals(bo.getContentType())) {
            return;
        }
        bo.setPlanContentJson(EvacuationSchemeParser.toJson(bo.getPlanContent()));
    }

    private Integer resolveCurrentDefRespRoundNo(Long defId) {
        if (defId == null) {
            return 1;
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(defId);
        if (defRespPlan == null) {
            return 1;
        }
        return defRespPlan.getCurrentRoundNo() != null ? defRespPlan.getCurrentRoundNo() : 1;
    }

    private Integer resolveQueryRoundNo(Integer bizType, Long bizId, Integer roundNo) {
        if (roundNo != null) {
            return roundNo;
        }
        if (DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bizType) && bizId != null) {
            return resolveCurrentDefRespRoundNo(bizId);
        }
        return null;
    }

    private Integer resolveRoundNo(DzTaskHandleDetailContent entity) {
        if (DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(entity.getBizType())) {
            return resolveCurrentDefRespRoundNo(entity.getBizId());
        }
        return entity.getRoundNo() != null ? entity.getRoundNo() : 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DzTaskHandleDetailContentBo bo) {
        fillPlanContentFromJsonIfNeeded(bo);
        applyCurrentRoundNoOnStatusSubmit(bo);
        return doUpdateByBo(bo);
    }

    /**
     * 提交（status=1）时将防御响应详情记录的批次同步为当前轮次。
     */
    private void applyCurrentRoundNoOnStatusSubmit(DzTaskHandleDetailContentBo bo) {
        if (bo == null || !DefRespConsultationConfirmStatusEnum.PENDING_APPROVAL.getCode().equals(bo.getStatus())) {
            return;
        }
        Integer bizType = bo.getBizType();
        Long bizId = bo.getBizId();
        Integer contentType = bo.getContentType();
        if (bo.getId() != null && (bizType == null || bizId == null || contentType == null)) {
            DzTaskHandleDetailContent existing = baseMapper.selectById(bo.getId());
            if (existing == null) {
                return;
            }
            bizType = ObjUtil.defaultIfNull(bizType, existing.getBizType());
            bizId = ObjUtil.defaultIfNull(bizId, existing.getBizId());
            contentType = ObjUtil.defaultIfNull(contentType, existing.getContentType());
        }
        if (!DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bizType) || bizId == null || contentType == null) {
            return;
        }
        Integer currentRoundNo = resolveCurrentDefRespRoundNo(bizId);
        bo.setRoundNo(currentRoundNo);
        bo.setIsLatest(1);
        clearLatestFlag(bizType, bizId, contentType, currentRoundNo);
    }

    private Boolean doUpdateByBo(DzTaskHandleDetailContentBo bo) {
        DzTaskHandleDetailContent update = MapstructUtils.convert(bo, DzTaskHandleDetailContent.class);
        validEntityBeforeSave(update);
        if (update.getUpdateDate() == null) {
            update.setUpdateDate(new Date());
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConsultationConfirmByBo(DzTaskHandleDetailContentBo bo) {
        if (bo == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (bo.getBizId() == null) {
            throw new ServiceException("bizId不能为空");
        }
        if (StringUtils.isBlank(bo.getPlanContentJson())) {
            throw new ServiceException("planContentJson不能为空");
        }
        fillPlanContentFromJsonIfNeeded(bo);
        Integer roundNo = resolveCurrentDefRespRoundNo(bo.getBizId());
        bo.setRoundNo(roundNo);
        DzTaskHandleDetailContentVo existingDraft = queryLatestDraftConsultation(bo.getBizId(), roundNo);
        if (existingDraft != null && existingDraft.getId() != null) {
            bo.setId(existingDraft.getId());
            bo.setStatus(ObjUtil.defaultIfNull(bo.getStatus(), DefRespConsultationConfirmStatusEnum.PENDING_SUBMIT.getCode()));
            if (!updateByBo(bo)) {
                throw new ServiceException("会商确认内容更新失败");
            }
            return existingDraft.getId();
        }
        bo.setStatus(ObjUtil.defaultIfNull(bo.getStatus(), DefRespConsultationConfirmStatusEnum.PENDING_SUBMIT.getCode()));
        if (!insertByBo(bo)) {
            throw new ServiceException("会商确认内容保存失败");
        }
        return bo.getId();
    }

    @Override
    public DzTaskHandleDetailContentVo queryLatestAlarmSyncedConsultation(Long defId, Integer roundNo) {
        DzTaskHandleDetailContentBo queryBo = new DzTaskHandleDetailContentBo();
        queryBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        queryBo.setBizId(defId);
        queryBo.setContentType(DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode());
        queryBo.setRoundNo(resolveQueryRoundNo(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), defId, roundNo));
        queryBo.setStatus(DefRespConsultationConfirmStatusEnum.ALARM_SYNCED.getCode());
        List<DzTaskHandleDetailContentVo> details = queryList(queryBo);
        return details == null || details.isEmpty() ? null : details.getFirst();
    }

    @Override
    public DzTaskHandleDetailContentVo querySubmittedConsultationDetail(Long defId, Integer roundNo) {
        Integer effectiveRoundNo = resolveQueryRoundNo(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), defId, roundNo);
        DzTaskHandleDetailContentBo queryBo = new DzTaskHandleDetailContentBo();
        queryBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        queryBo.setBizId(defId);
        queryBo.setContentType(DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode());
        queryBo.setRoundNo(effectiveRoundNo);
        queryBo.setStatus(DefRespConsultationConfirmStatusEnum.PENDING_APPROVAL.getCode());
        LambdaQueryWrapper<DzTaskHandleDetailContent> lqw = Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                                                                    .eq(DzTaskHandleDetailContent::getBizType, queryBo.getBizType())
                                                                    .eq(DzTaskHandleDetailContent::getBizId, queryBo.getBizId())
                                                                    .eq(DzTaskHandleDetailContent::getContentType, queryBo.getContentType())
                                                                    .eq(DzTaskHandleDetailContent::getRoundNo, queryBo.getRoundNo())
                                                                    .eq(DzTaskHandleDetailContent::getStatus, queryBo.getStatus())
                                                                    .eq(DzTaskHandleDetailContent::getIsLatest, 1)
                                                                    .eq(DzTaskHandleDetailContent::getDeleted, 0)
                                                                    .orderByDesc(DzTaskHandleDetailContent::getUpdateDate, DzTaskHandleDetailContent::getId)
                                                                    .last("limit 1");
        return baseMapper.selectVoOne(lqw);
    }

    @Override
    public DzTaskHandleDetailContentVo queryLatestPendingFinalReport(Long defId, Integer roundNo) {
        Integer effectiveRoundNo = resolveQueryRoundNo(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), defId, roundNo);
        DzTaskHandleDetailContentBo queryBo = new DzTaskHandleDetailContentBo();
        queryBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        queryBo.setBizId(defId);
        queryBo.setContentType(DetailContentTypeEnum.FINAL_REPORT.getCode());
        queryBo.setRoundNo(effectiveRoundNo);
        queryBo.setStatus(DefRespConsultationConfirmStatusEnum.PENDING_APPROVAL.getCode());
        LambdaQueryWrapper<DzTaskHandleDetailContent> lqw = Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                                                                    .eq(DzTaskHandleDetailContent::getBizType, queryBo.getBizType())
                                                                    .eq(DzTaskHandleDetailContent::getBizId, queryBo.getBizId())
                                                                    .eq(DzTaskHandleDetailContent::getContentType, queryBo.getContentType())
                                                                    .eq(DzTaskHandleDetailContent::getRoundNo, queryBo.getRoundNo())
                                                                    .eq(DzTaskHandleDetailContent::getStatus, queryBo.getStatus())
                                                                    .eq(DzTaskHandleDetailContent::getDeleted, 0)
                                                                    .orderByDesc(DzTaskHandleDetailContent::getUpdateDate, DzTaskHandleDetailContent::getId)
                                                                    .last("limit 1");
        return baseMapper.selectVoOne(lqw);
    }

    @Override
    public DzTaskHandleDetailContentVo requireLatestPendingConsultationDetail(Long defId, Integer roundNo) {
        DzTaskHandleDetailContentVo detail = querySubmittedConsultationDetail(defId, roundNo);
        if (detail == null) {
            detail = querySubmittedConsultationDetail(defId, roundNo - 1);
            if (detail == null) {
                throw new ServiceException("未查询到待审批的会商确认记录");
            }
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAlarmSyncedConsultation(Long defId, Integer roundNo, String planContentJson) {
        Integer currentRoundNo = resolveCurrentDefRespRoundNo(defId);
        DzTaskHandleDetailContentVo existing = queryLatestAlarmSyncedConsultation(defId, currentRoundNo);
        DzTaskHandleDetailContentBo detailBo = new DzTaskHandleDetailContentBo();
        detailBo.setBizId(defId);
        detailBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        detailBo.setContentType(DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode());
        detailBo.setPlanContentJson(planContentJson);
        detailBo.setPlanContent(buildConsultationPlanContent(planContentJson, defId));
        detailBo.setRoundNo(currentRoundNo + 1);
        detailBo.setStatus(DefRespConsultationConfirmStatusEnum.ALARM_SYNCED.getCode());
        if (existing != null && existing.getId() != null) {
            detailBo.setId(existing.getId());
            if (!updateByBo(detailBo)) {
                throw new ServiceException("会商确认内容更新失败");
            }
            return existing.getId();
        }
        if (!insertByBo(detailBo)) {
            throw new ServiceException("会商确认内容保存失败");
        }
        return detailBo.getId();
    }

    /**
     * 以已提交会商确认覆盖终报；仅由 {@code updateApprovalStatusByBo} 触发。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertInitialAndFinalReportFromSubmittedConsultation(DzTaskHandleDetailContentVo submittedConsultation, Integer roundNo) {
        if (submittedConsultation == null) {
            throw new ServiceException("会商确认记录不能为空");
        }
        Long defId = submittedConsultation.getBizId();
        if (defId == null) {
            throw new ServiceException("会商确认记录bizId不能为空");
        }
        String fallbackContent = submittedConsultation.getPlanContent();
        DzTaskHandleDetailContentBo finalBo = new DzTaskHandleDetailContentBo();
        finalBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        finalBo.setBizId(defId);
        finalBo.setContentType(DetailContentTypeEnum.FINAL_REPORT.getCode());
        finalBo.setRoundNo(roundNo);
        finalBo.setPlanContent(StringUtils.defaultIfBlank(submittedConsultation.getPlanContent(), fallbackContent));
        finalBo.setStatus(1);
        finalBo.setPlanContentJson(submittedConsultation.getPlanContentJson());
        if (!insertByBo(finalBo)) {
            throw new ServiceException("终报详情内容保存失败");
        }
    }

    /**
     * 写入初报；若已有会商确认则优先用其内容组装。仅由 {@code saveInitialPlanOnCreate} 触发。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertInitialReportContent(Long defId, String content, Integer roundNo) {
        DzTaskHandleDetailContentVo submittedConsultation = querySubmittedConsultationDetail(defId, roundNo);
        DzTaskHandleDetailContentBo initialBo = new DzTaskHandleDetailContentBo();
        initialBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        initialBo.setBizId(defId);
        initialBo.setContentType(DetailContentTypeEnum.INITIAL_REPORT.getCode());
        initialBo.setRoundNo(roundNo);
        initialBo.setPlanContent(buildInitialReportPlanContent(defId, submittedConsultation, content));
        if (!insertByBo(initialBo)) {
            throw new ServiceException("初报详情内容保存失败");
        }
    }

    /**
     * 写入终报；若已有会商确认则优先用其内容。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertInitialAndFinalReportContent(Long defId, String content, Integer roundNo) {
        DzTaskHandleDetailContentVo submittedConsultation = querySubmittedConsultationDetail(defId, roundNo);
        DzTaskHandleDetailContentBo finalBo = new DzTaskHandleDetailContentBo();
        finalBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        finalBo.setBizId(defId);
        finalBo.setContentType(DetailContentTypeEnum.FINAL_REPORT.getCode());
        finalBo.setRoundNo(roundNo);
        finalBo.setStatus(1);
        if (submittedConsultation != null) {
            finalBo.setPlanContent(StringUtils.defaultIfBlank(submittedConsultation.getPlanContent(), content));
            finalBo.setPlanContentJson(submittedConsultation.getPlanContentJson());
        } else {
            finalBo.setPlanContent(content);
        }
        if (!insertByBo(finalBo)) {
            throw new ServiceException("终报详情内容保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAlarmSyncedConsultationToPendingSubmit(Long defId) {
        DzTaskHandleDetailContentBo queryBo = new DzTaskHandleDetailContentBo();
        queryBo.setBizId(defId);
        queryBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        queryBo.setContentType(DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode());
        queryBo.setRoundNo(resolveCurrentDefRespRoundNo(defId));
        queryBo.setStatus(DefRespConsultationConfirmStatusEnum.ALARM_SYNCED.getCode());
        List<DzTaskHandleDetailContent> contents = baseMapper.selectList(buildQueryWrapper(queryBo));
        for (DzTaskHandleDetailContent content : contents) {
            content.setStatus(DefRespConsultationConfirmStatusEnum.PENDING_SUBMIT.getCode());
            DzTaskHandleDetailContentBo bean = BeanUtil.toBean(content, DzTaskHandleDetailContentBo.class);
            doUpdateByBo(bean);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateConsultationConfirmApproved(DzTaskHandleDetailContentVo detailContent) {
        DzTaskHandleDetailContentBo detailUpdateBo = new DzTaskHandleDetailContentBo();
        detailUpdateBo.setId(detailContent.getId());
        detailUpdateBo.setBizId(detailContent.getBizId());
        detailUpdateBo.setBizType(detailContent.getBizType());
        detailUpdateBo.setContentType(detailContent.getContentType());
        detailUpdateBo.setPlanContent(detailContent.getPlanContent());
        detailUpdateBo.setPlanContentJson(detailContent.getPlanContentJson());
        detailUpdateBo.setSuggest(detailContent.getSuggest());
        detailUpdateBo.setUserId(detailContent.getUserId());
        detailUpdateBo.setUserName(detailContent.getUserName());
        detailUpdateBo.setRoundNo(detailContent.getRoundNo());
        detailUpdateBo.setDeleted(detailContent.getDeleted());
        detailUpdateBo.setIsLatest(detailContent.getIsLatest());
        detailUpdateBo.setCreateDate(detailContent.getCreateDate());
        detailUpdateBo.setStatus(DefRespConsultationConfirmStatusEnum.APPROVED.getCode());
        detailUpdateBo.setUpdateDate(new Date());
        return updateByBo(detailUpdateBo);
    }

    private String buildInitialReportPlanContent(Long defId, DzTaskHandleDetailContentVo submittedConsultation, String fallbackContent) {
        String planContentJson = submittedConsultation.getPlanContentJson();
        if (StringUtils.isBlank(planContentJson)) {
            return fallbackContent;
        }
        DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(defId);
        String city = defRespPlan != null ? defRespPlan.getCounty() : null;
        String triggerCondition = defRespPlan != null ? defRespPlan.getTriggerCondition() : null;
        String responsibilityUnit = defRespPlan != null ? defRespPlan.getResponsibilityUnit() : null;
        Date planCreateDate = defRespPlan != null ? defRespPlan.getCreateDate() : null;
        String planContent = DefRespConsultationConfirmItems.buildReportContentFromPlanContentJson(
            planContentJson, city, triggerCondition, responsibilityUnit, planCreateDate, this::resolveDefenseResponseLevelFromAlarm);
        return StringUtils.defaultIfBlank(planContent, fallbackContent);
    }

    private DzTaskHandleDetailContentVo queryLatestDraftConsultation(Long defId, Integer roundNo) {
        DzTaskHandleDetailContentBo queryBo = new DzTaskHandleDetailContentBo();
        queryBo.setBizType(DetailBizTypeEnum.DEF_RESP_PLAN.getCode());
        queryBo.setBizId(defId);
        queryBo.setContentType(DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode());
        queryBo.setRoundNo(resolveQueryRoundNo(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), defId, roundNo));
        queryBo.setStatus(DefRespConsultationConfirmStatusEnum.PENDING_SUBMIT.getCode());
        List<DzTaskHandleDetailContentVo> details = queryList(queryBo);
        return details == null || details.isEmpty() ? null : details.getFirst();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        List<DzTaskHandleDetailContent> list = baseMapper.selectByIds(ids);
        if (isValid && list.size() != ids.size()) {
            throw new ServiceException("存在处置详情内容不存在，无法删除");
        }
        for (DzTaskHandleDetailContent content : list) {
            DzTaskHandleDetailContent update = new DzTaskHandleDetailContent();
            update.setId(content.getId());
            update.setDeleted(1);
            update.setIsLatest(0);
            update.setUpdateDate(new Date());
            if (baseMapper.updateById(update) < 1) {
                throw new ServiceException("删除处置详情内容失败");
            }
            if (Integer.valueOf(1).equals(content.getIsLatest())) {
                refillLatestFlag(content.getBizType(), content.getBizId(), content.getContentType(), null);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson) {
        fillCurrentUserAndSaveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.EVACUATION_PLAN.getCode(), roundNo, planContent, planContentJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson, Long userId) {
        throw new ServiceException("保存撤离方案时必须同时传入用户ID和用户名");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson, Long userId, String userName) {
        saveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.EVACUATION_PLAN.getCode(), roundNo, planContent, planContentJson, userId, userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDefRespPlanContent(Long defId, Integer roundNo, String planContent) {
        fillCurrentUserAndSaveContent(DetailBizTypeEnum.DEF_RESP_PLAN.getCode(), defId, DetailContentTypeEnum.DEF_RESP_PLAN.getCode(), roundNo, planContent, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAiReportContent(Long handleId, Integer roundNo, String reportContent) {
        fillCurrentUserAndSaveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.AI_REPORT.getCode(), roundNo, reportContent, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAiReportContent(Long handleId, Integer roundNo, String reportContent, Long userId, String userName) {
        saveAiReportContent(handleId, roundNo, reportContent, null, userId, userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAiReportContent(Long handleId, Integer roundNo, String reportContent, String planContentJson, Long userId, String userName) {
        saveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.AI_REPORT.getCode(), roundNo, reportContent, planContentJson, userId, userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReviewReportContent(Long handleId, Integer roundNo, String reportContent) {
        fillCurrentUserAndSaveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.REVIEW_REPORT.getCode(), roundNo, reportContent, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReviewReportContent(Long handleId, Integer roundNo, String reportContent, Long userId, String userName) {
        saveContent(DetailBizTypeEnum.TASK_HANDLE.getCode(), handleId, DetailContentTypeEnum.REVIEW_REPORT.getCode(), roundNo, reportContent, null, userId, userName);
    }

    @Override
    public void updateLatestSuggest(Integer bizType, Long bizId, Integer contentType, String suggest) {
        doUpdateLatestSuggest(bizType, bizId, contentType, suggest, resolveQueryRoundNo(bizType, bizId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markLatestById(Long id) {
        Assert.notNull(id, "id不能为空");
        DzTaskHandleDetailContent target = baseMapper.selectById(id);
        if (target == null || target.getId() == null || Integer.valueOf(1).equals(target.getDeleted())) {
            throw new ServiceException("处置详情内容不存在");
        }
        clearLatestFlag(target.getBizType(), target.getBizId(), target.getContentType(), target.getRoundNo());
        DzTaskHandleDetailContent update = new DzTaskHandleDetailContent();
        update.setId(target.getId());
        update.setIsLatest(1);
        update.setUpdateDate(new Date());
        if (baseMapper.updateById(update) < 1) {
            throw new ServiceException("设置最新详情内容失败");
        }
    }

    private LambdaQueryWrapper<DzTaskHandleDetailContent> buildQueryWrapper(DzTaskHandleDetailContentBo bo) {
        LambdaQueryWrapper<DzTaskHandleDetailContent> lqw = Wrappers.lambdaQuery();
        if (bo == null) {
            lqw.eq(DzTaskHandleDetailContent::getDeleted, 0);
            lqw.orderByDesc(DzTaskHandleDetailContent::getIsLatest, DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId);
            return lqw;
        }
        lqw.eq(bo.getId() != null, DzTaskHandleDetailContent::getId, bo.getId());
        lqw.eq(bo.getBizType() != null, DzTaskHandleDetailContent::getBizType, bo.getBizType());
        lqw.eq(bo.getContentType() != null, DzTaskHandleDetailContent::getContentType, bo.getContentType());
        applyBizIdCondition(lqw, bo.getBizType(), bo.getBizId(), bo.getContentType());
        lqw.eq(bo.getRoundNo() != null, DzTaskHandleDetailContent::getRoundNo, bo.getRoundNo());
        lqw.eq(bo.getUserId() != null, DzTaskHandleDetailContent::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getUserName()), DzTaskHandleDetailContent::getUserName, bo.getUserName());
        lqw.like(StringUtils.isNotBlank(bo.getPlanContent()), DzTaskHandleDetailContent::getPlanContent, bo.getPlanContent());
        lqw.like(StringUtils.isNotBlank(bo.getPlanContentJson()), DzTaskHandleDetailContent::getPlanContentJson, bo.getPlanContentJson());
        lqw.eq(bo.getCreateDate() != null, DzTaskHandleDetailContent::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getDeleted() != null, DzTaskHandleDetailContent::getDeleted, bo.getDeleted());
        lqw.eq(bo.getIsLatest() != null, DzTaskHandleDetailContent::getIsLatest, bo.getIsLatest());
        lqw.eq(bo.getStatus() != null, DzTaskHandleDetailContent::getStatus, bo.getStatus());
        lqw.eq(bo.getUpdateDate() != null, DzTaskHandleDetailContent::getUpdateDate, bo.getUpdateDate());
        lqw.orderByDesc(DzTaskHandleDetailContent::getIsLatest, DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId);
        return lqw;
    }

    private void saveContent(Integer bizType, Long bizId, Integer contentType, Integer roundNo, String planContent, String planContentJson, Long userId) {
        saveContent(bizType, bizId, contentType, roundNo, planContent, planContentJson, userId, null);
    }

    private void saveContent(Integer bizType, Long bizId, Integer contentType, Integer roundNo, String planContent, String planContentJson, Long userId, String userName) {
        DzTaskHandleDetailContent content = new DzTaskHandleDetailContent();
        content.setBizType(bizType);
        content.setBizId(bizId);
        content.setContentType(contentType);
        content.setPlanContent(planContent);
        content.setPlanContentJson(planContentJson);
        content.setRoundNo(roundNo);
        content.setRoundNo(resolveRoundNo(content));
        fillCurrentUser(content, userId, userName);
        Date now = new Date();
        content.setCreateDate(now);
        content.setUpdateDate(now);
        content.setDeleted(0);
        content.setIsLatest(1);
        content.setStatus(0);
        validContent(content);
        clearLatestFlag(bizType, bizId, contentType, content.getRoundNo());
        if (baseMapper.insert(content) <= 0) {
            throw new ServiceException("保存详情内容失败");
        }
        notifyAiHostingOverviewIfNeeded(content);
    }

    private void fillCurrentUserAndSaveContent(Integer bizType, Long bizId, Integer contentType, Integer roundNo, String planContent, String planContentJson) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ServiceException("当前登录用户不存在，无法保存详情内容");
        }
        Long userId = loginUser.getUserId();
        String userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        if (StringUtils.isBlank(userName)) {
            throw new ServiceException("当前登录用户名不能为空，无法保存详情内容");
        }
        saveContent(bizType, bizId, contentType, roundNo, planContent, planContentJson, userId, userName);
    }

    private void notifyAiHostingOverviewIfNeeded(DzTaskHandleDetailContent content) {
        if (content == null || !DetailBizTypeEnum.TASK_HANDLE.getCode().equals(content.getBizType())) {
            return;
        }
        String reason = null;
        if (DetailContentTypeEnum.AI_REPORT.getCode().equals(content.getContentType())) {
            reason = "ai_report_saved";
        } else if (DetailContentTypeEnum.REVIEW_REPORT.getCode().equals(content.getContentType())) {
            reason = "review_report_saved";
        }
        if (reason == null) {
            return;
        }
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "report_content",
            reason,
            "dz_task_handle",
            content.getBizId(),
            null
        );
    }

    private void validContent(DzTaskHandleDetailContent content) {
        Assert.notNull(content.getBizType(), "业务类型不能为空");
        Assert.notNull(content.getContentType(), "内容类型不能为空");
        Assert.isTrue(canBizIdBeEmpty(content.getBizType(), content.getContentType()) || content.getBizId() != null, "业务主键不能为空");
        Assert.notBlank(content.getPlanContent(), "内容不能为空");
    }

    private void doUpdateLatestSuggest(Integer bizType, Long bizId, Integer contentType, String suggest, Integer roundNo) {
        if (bizType == null || contentType == null || StringUtils.isBlank(suggest)) {
            return;
        }
        if (bizId == null && !canBizIdBeEmpty(bizType, contentType)) {
            return;
        }
        LambdaQueryWrapper<DzTaskHandleDetailContent> wrapper = Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                                                                        .eq(DzTaskHandleDetailContent::getBizType, bizType)
                                                                        .eq(DzTaskHandleDetailContent::getContentType, contentType)
                                                                        .eq(DzTaskHandleDetailContent::getDeleted, 0)
                                                                        .eq(DzTaskHandleDetailContent::getIsLatest, 1)
                                                                        .orderByDesc(DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
                                                                        .last("limit 1");
        applyBizIdCondition(wrapper, bizType, bizId, contentType);
        applyRoundNoCondition(wrapper, roundNo);
        DzTaskHandleDetailContent latest = baseMapper.selectOne(
            wrapper
        );
        if (latest == null || latest.getId() == null) {
            log.warn("未找到可更新suggest的详情记录, bizType={}, bizId={}, contentType={}", bizType, bizId, contentType);
            return;
        }
        DzTaskHandleDetailContent update = new DzTaskHandleDetailContent();
        update.setId(latest.getId());
        update.setSuggest(suggest);
        update.setUpdateDate(new Date());
        if (baseMapper.updateById(update) < 1) {
            throw new ServiceException("更新详情建议失败");
        }
    }

    private void validEntityBeforeSave(DzTaskHandleDetailContent entity) {
        Assert.notNull(entity.getBizType(), "业务类型不能为空");
        Assert.notNull(entity.getContentType(), "内容类型不能为空");
        Assert.isTrue(canBizIdBeEmpty(entity.getBizType(), entity.getContentType()) || entity.getBizId() != null, "业务主键不能为空");
        Assert.notBlank(entity.getPlanContent(), "内容不能为空");
    }

    private void clearLatestFlag(Integer bizType, Long bizId, Integer contentType, Integer roundNo) {
        LambdaUpdateWrapper<DzTaskHandleDetailContent> wrapper = Wrappers.<DzTaskHandleDetailContent>lambdaUpdate()
                                                                         .set(DzTaskHandleDetailContent::getIsLatest, 0)
                                                                         .set(DzTaskHandleDetailContent::getUpdateDate, new Date())
                                                                         .eq(DzTaskHandleDetailContent::getBizType, bizType)
                                                                         .eq(DzTaskHandleDetailContent::getContentType, contentType)
                                                                         .eq(DzTaskHandleDetailContent::getDeleted, 0)
                                                                         .eq(DzTaskHandleDetailContent::getIsLatest, 1);
        applyBizIdCondition(wrapper, bizType, bizId, contentType);
        applyRoundNoCondition(wrapper, roundNo);
        baseMapper.update(null, wrapper);
    }

    private void refillLatestFlag(Integer bizType, Long bizId, Integer contentType, Integer roundNo) {
        LambdaQueryWrapper<DzTaskHandleDetailContent> wrapper = Wrappers.<DzTaskHandleDetailContent>lambdaQuery()
                                                                        .eq(DzTaskHandleDetailContent::getBizType, bizType)
                                                                        .eq(DzTaskHandleDetailContent::getContentType, contentType)
                                                                        .eq(DzTaskHandleDetailContent::getDeleted, 0)
                                                                        .orderByDesc(DzTaskHandleDetailContent::getCreateDate, DzTaskHandleDetailContent::getId)
                                                                        .last("limit 1");
        applyBizIdCondition(wrapper, bizType, bizId, contentType);
        applyRoundNoCondition(wrapper, roundNo);
        DzTaskHandleDetailContent latest = baseMapper.selectOne(
            wrapper
        );
        if (latest == null || latest.getId() == null) {
            return;
        }
        DzTaskHandleDetailContent update = new DzTaskHandleDetailContent();
        update.setId(latest.getId());
        update.setIsLatest(1);
        update.setUpdateDate(new Date());
        if (baseMapper.updateById(update) < 1) {
            throw new ServiceException("回填最新详情内容失败");
        }
    }

    private void applyBizIdCondition(LambdaQueryWrapper<DzTaskHandleDetailContent> wrapper, Integer bizType, Long bizId, Integer contentType) {
        if (bizId != null) {
            wrapper.eq(DzTaskHandleDetailContent::getBizId, bizId);
        } else if (canBizIdBeEmpty(bizType, contentType)) {
            wrapper.isNull(DzTaskHandleDetailContent::getBizId);
        }
    }

    private void applyBizIdCondition(LambdaUpdateWrapper<DzTaskHandleDetailContent> wrapper, Integer bizType, Long bizId, Integer contentType) {
        if (bizId != null) {
            wrapper.eq(DzTaskHandleDetailContent::getBizId, bizId);
        } else if (canBizIdBeEmpty(bizType, contentType)) {
            wrapper.isNull(DzTaskHandleDetailContent::getBizId);
        }
    }

    private void applyRoundNoCondition(LambdaQueryWrapper<DzTaskHandleDetailContent> wrapper, Integer roundNo) {
        if (roundNo != null) {
            wrapper.eq(DzTaskHandleDetailContent::getRoundNo, roundNo);
        }
    }

    private void applyRoundNoCondition(LambdaUpdateWrapper<DzTaskHandleDetailContent> wrapper, Integer roundNo) {
        if (roundNo != null) {
            wrapper.eq(DzTaskHandleDetailContent::getRoundNo, roundNo);
        }
    }

    private boolean canBizIdBeEmpty(Integer bizType, Integer contentType) {
        return DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bizType)
            && DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode().equals(contentType);
    }

    private void fillCurrentUser(DzTaskHandleDetailContent content) {
        LoginUser loginUser = requireCurrentLoginUser();
        fillCurrentUser(
            content,
            loginUser.getUserId(),
            StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername())
        );
    }


    private void fillCurrentUser(DzTaskHandleDetailContent content, Long userId, String userName) {
        if (userId == null) {
            throw new ServiceException("保存详情内容时用户ID不能为空");
        }
        if (StringUtils.isBlank(userName)) {
            throw new ServiceException("保存详情内容时用户名不能为空");
        }
        content.setUserId(userId);
        content.setUserName(userName);
    }

    private LoginUser requireCurrentLoginUser() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ServiceException("当前登录用户不存在，无法保存详情内容");
        }
        String userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        if (StringUtils.isBlank(userName)) {
            throw new ServiceException("当前登录用户名不能为空，无法保存详情内容");
        }
        return loginUser;
    }
}
