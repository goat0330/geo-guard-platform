package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzExpertExt;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzExpertExtMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingParticipantsStatus;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 任务处置审批Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskHandleApprovalServiceImpl implements IDzTaskHandleApprovalService {

    private final DzTaskHandleApprovalMapper baseMapper;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DzExpertExtMapper dzExpertExtMapper;
    private final SseEmitterManager sseEmitterManager;
    private final IDzSingleDefProgressService dzSingleDefProgressService;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;


    /**
     * 查询任务处置审批
     *
     * @param id 主键
     * @return 任务处置审批
     */
    @Override
    public DzTaskHandleApprovalVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询任务处置审批列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务处置审批分页列表
     */
    @Override
    public TableDataInfo<DzTaskHandleApprovalVo> queryPageList(DzTaskHandleApprovalBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzTaskHandleApproval> lqw = buildQueryWrapper(bo);
        Page<DzTaskHandleApprovalVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的任务处置审批列表
     *
     * @param bo 查询条件
     * @return 任务处置审批列表
     */
    @Override
    public List<DzTaskHandleApprovalVo> queryList(DzTaskHandleApprovalBo bo) {
        LambdaQueryWrapper<DzTaskHandleApproval> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzTaskHandleApproval> buildQueryWrapper(DzTaskHandleApprovalBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzTaskHandleApproval> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getHandleId() != null, DzTaskHandleApproval::getHandleId, bo.getHandleId());
        lqw.eq(bo.getMeetingType() != null, DzTaskHandleApproval::getMeetingType, bo.getMeetingType());
        lqw.eq(bo.getUserId() != null, DzTaskHandleApproval::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getNickname()), DzTaskHandleApproval::getNickname, bo.getNickname());
        lqw.eq(bo.getType() != null, DzTaskHandleApproval::getType, bo.getType());
        lqw.eq(bo.getProcess() != null, DzTaskHandleApproval::getProcess, bo.getProcess());
        lqw.eq(bo.getStatus() != null, DzTaskHandleApproval::getStatus, bo.getStatus());
        lqw.eq(bo.getRoundNo() != null, DzTaskHandleApproval::getRoundNo, bo.getRoundNo());
        lqw.between(params.get("beginCreateDate") != null && params.get("endCreateDate") != null,
            DzTaskHandleApproval::getCreateDate, params.get("beginCreateDate"), params.get("endCreateDate"));
        lqw.between(params.get("beginUpdateDate") != null && params.get("endUpdateDate") != null,
            DzTaskHandleApproval::getUpdateDate, params.get("beginUpdateDate"), params.get("endUpdateDate"));
        return lqw;
    }

    /**
     * 新增任务处置审批
     *
     * @param bo 任务处置审批
     * @return 是否新增成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean insertByBo(DzTaskHandleApprovalBo bo) {
        validEntityBeforeSave(bo);
        DzTaskHandleApproval add = MapstructUtils.convert(bo, DzTaskHandleApproval.class);
        add.setCreateDate(new Date());
        add.setUpdateDate(new Date());
        add.setRoundNo(add.getRoundNo() == null ? 1 : add.getRoundNo());

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }

        if (DzTaskHandleApprovalTypeEnum.EXPERT_CONSULTATION.getCode().equals(bo.getType())) {
            ensureExpertExtExists(bo.getUserId());
            baseMapper.updateExpertCount(bo.getUserId());
        }

        return flag;
    }

    /**
     * 修改任务处置审批
     *
     * @param bo 任务处置审批
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DzTaskHandleApprovalBo bo) {
        if (bo == null) {
            throw new ServiceException("审批参数不能为空");
        }
        if (bo.getId() == null) {
            throw new ServiceException("审批记录ID不能为空");
        }
        DzTaskHandleApproval existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("审批记录不存在");
        }
        if (Integer.valueOf(1).equals(existing.getStatus()) && Integer.valueOf(0).equals(bo.getStatus())) {
            throw new ServiceException("已审批记录不允许回退为未审批");
        }
        DzTaskHandleApproval update = MapstructUtils.convert(bo, DzTaskHandleApproval.class);
        update.setUpdateDate(new Date());
        Long currentUserId = LoginHelper.getUserId();
        if (currentUserId == null || !currentUserId.equals(existing.getUserId())) {
            throw new ServiceException("当前用户无修改其他用户审批记录的权限");
        }
        baseMapper.updateById(update);
        if (bo.getStatus() == 1) {
            Integer targetRoundNo = existing.getRoundNo() == null ? 1 : existing.getRoundNo();
            if (isRegionAdminApproval(existing)) {
                SpringUtils.getBean(IDzDefRespPlanService.class).handleRegionApprovalPassed(existing.getHandleId(), existing.getId());
            } else if (DzTaskHandleApprovalTypeEnum.EXPERT_CONSULTATION.getCode().equals(existing.getType())) {
                dzSingleDefProgressService.recordConsultationApproved(existing.getHandleId(), targetRoundNo, update.getUpdateDate());
            } else if (DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode().equals(existing.getType())) {
                dzSingleDefProgressService.recordAdminApproved(existing.getHandleId(), targetRoundNo, update.getUpdateDate());
            }
        }

        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(bo.getMeetingId());
        if (meetingInfo != null) {
            Long initiator = meetingInfo.getInitiator();
            SseResp.SseRespBuilder message = SseResp.builder()
                                                    .type("task-handle-approval")
                                                    .data(bo);
            sseEmitterManager.sendMessage(initiator, message);
            Map<Long, MeetingParticipantsStatus> participantsMap = meetingInfo.getParticipantsMap();
            if (participantsMap != null) {
                for (Long userId : participantsMap.keySet()) {
                    sseEmitterManager.sendMessage(userId, message);
                }
            }
        }
        if (Integer.valueOf(1).equals(bo.getStatus())
            && DzTaskHandleApprovalTypeEnum.EXPERT_CONSULTATION.getCode().equals(existing.getType())
            && HandleProcessEnum.CONSULTATION_JUDGMENT.getCode().equals(existing.getProcess())) {
            aiHostingOverviewNotifyService.notifyChangedAfterCommit(
                "task_handle_approval",
                "consultation_approved",
                "dz_task_handle",
                existing.getHandleId(),
                null
            );
        }

        return true;
    }

    private boolean isRegionAdminApproval(DzTaskHandleApproval approval) {
        if (approval == null
            || !Integer.valueOf(2).equals(approval.getMeetingType())
            || !DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode().equals(approval.getType())
            || approval.getHandleId() == null) {
            return false;
        }
        DefRespPlanVo defRespPlanVo = dzDefRespPlanMapper.selectVoById(approval.getHandleId());
        return defRespPlanVo != null
            && DefRespPlanTypeEnum.REGION.getCode().equals(defRespPlanVo.getType())
            && RegionScopeTypeEnum.COUNTY.getCode().equals(defRespPlanVo.getRegionScopeType());
    }


    /**
     * 校验并批量删除任务处置审批信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<DzTaskHandleApproval> approvals = baseMapper.selectByIds(ids);
            if (approvals.size() != ids.size()) {
                throw new ServiceException("审批记录不存在，无法删除");
            }
            if (approvals.stream().anyMatch(item -> Integer.valueOf(1).equals(item.getStatus()))) {
                throw new ServiceException("已审批记录不允许删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<DzTaskHandleApprovalVo> listStatus(Long handleId, Integer roundNo) {
        if (handleId == null) {
            return List.of();
        }
        Integer targetRoundNo = roundNo;
        if (targetRoundNo == null) {
            DefRespPlanVo defRespPlanVo = dzDefRespPlanMapper.selectVoById(handleId);
            if (defRespPlanVo != null && defRespPlanVo.getCurrentRoundNo() != null) {
                targetRoundNo = defRespPlanVo.getCurrentRoundNo();
            } else {
                LambdaQueryWrapper<DzTaskHandleApproval> lqw = Wrappers.<DzTaskHandleApproval>lambdaQuery()
                                                                       .eq(DzTaskHandleApproval::getHandleId, handleId)
                                                                       .orderByDesc(DzTaskHandleApproval::getRoundNo)
                                                                       .orderByDesc(DzTaskHandleApproval::getCreateDate)
                                                                       .last("limit 1");
                DzTaskHandleApproval latest = baseMapper.selectOne(lqw);
                targetRoundNo = latest != null && latest.getRoundNo() != null ? latest.getRoundNo() : 1;
            }
        }
        List<DzTaskHandleApprovalVo> type1List = baseMapper.selectListStatusType1(handleId, targetRoundNo);
        List<DzTaskHandleApprovalVo> type2List = baseMapper.selectListStatusType2(handleId, targetRoundNo);
        return Stream.concat(type1List.stream(), type2List.stream()).toList();
    }

    private void validEntityBeforeSave(DzTaskHandleApprovalBo bo) {
        if (bo == null) {
            throw new ServiceException("审批参数不能为空");
        }
        if (bo.getHandleId() == null) {
            throw new ServiceException("handleId不能为空");
        }
        if (bo.getUserId() == null) {
            throw new ServiceException("审批人不能为空");
        }
        if (StringUtils.isBlank(bo.getNickname())) {
            throw new ServiceException("审批人名称不能为空");
        }
        if (bo.getType() == null) {
            throw new ServiceException("审批类型不能为空");
        }
        if (bo.getProcess() == null) {
            throw new ServiceException("审批流程节点不能为空");
        }
        if (bo.getStatus() != null && !List.of(0, 1).contains(bo.getStatus())) {
            throw new ServiceException("审批状态非法");
        }
        if (bo.getRoundNo() != null && bo.getRoundNo() < 1) {
            throw new ServiceException("轮次非法");
        }
    }

    private void ensureExpertExtExists(Long userId) {
        if (userId == null) {
            return;
        }
        LambdaQueryWrapper<DzExpertExt> lqw = Wrappers.<DzExpertExt>lambdaQuery()
                                                      .eq(DzExpertExt::getUserId, userId)
                                                      .last("limit 1");
        DzExpertExt expertExt = dzExpertExtMapper.selectOne(lqw);
        if (expertExt != null) {
            return;
        }
        DzExpertExt add = new DzExpertExt();
        add.setUserId(userId);
        add.setMeetingCount(0);
        dzExpertExtMapper.insert(add);
    }
}
