/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzProcessProgress;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzProcessProgressMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleApprovalMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 单点防御响应进度写入服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DzSingleDefProgressServiceImpl implements IDzSingleDefProgressService {

    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DzProcessProgressMapper dzProcessProgressMapper;
    private final DzTaskHandleApprovalMapper dzTaskHandleApprovalMapper;
    private final IDzProcessProgressService dzProcessProgressService;
    private final ISysUserService sysUserService;

    @Override
    public void recordStarted(DefRespPlan singlePlan, DzTaskHandle taskHandle, Long operatorUserId, Date progressTime) {
        if (taskHandle == null) {
            return;
        }
        if (!isSinglePlan(singlePlan)) {
            return;
        }
        upsertProgress(singlePlan, DefRespPlanStatusEnum.STARTED.getCode(), resolveRoundNo(singlePlan), progressTime,
            buildStartedDescription(taskHandle, operatorUserId));
    }

    @Override
    public void recordPlanGenerated(Long handleId, Date progressTime) {
        DefRespPlan singlePlan = findSinglePlanByHandleId(handleId);
        if (singlePlan == null) {
            return;
        }
        upsertProgress(singlePlan, DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(), resolveRoundNo(singlePlan), progressTime,
            "大模型已根据应急调查报告,生成处置方案");
    }

    @Override
    public void recordConsultationApproved(Long handleId, Integer roundNo, Date progressTime) {
        DefRespPlan singlePlan = findSinglePlanByHandleId(handleId);
        if (singlePlan == null) {
            return;
        }
        int targetRoundNo = normalizeRoundNo(roundNo, singlePlan);
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalMapper.selectListStatusType1(handleId, targetRoundNo)
                                                                           .stream()
                                                                           .filter(item -> Objects.equals(item.getProcess(), HandleProcessEnum.CONSULTATION_JUDGMENT.getCode()))
                                                                           .toList();
        String nicknames = approvals.stream()
                                    .map(DzTaskHandleApprovalVo::getNickname)
                                    .filter(StringUtils::isNotBlank)
                                    .distinct()
                                    .collect(Collectors.joining(","));
        String description = StringUtils.isBlank(nicknames)
            ? "专家组已会商确认方案,同意发布"
            : "专家组" + nicknames + "已会商确认方案,同意发布";
        upsertProgress(singlePlan, DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(), targetRoundNo, progressTime, description);
    }

    @Override
    public void recordAdminApproved(Long handleId, Integer roundNo, Date progressTime) {
        DefRespPlan singlePlan = findSinglePlanByHandleId(handleId);
        if (singlePlan == null) {
            return;
        }
        int targetRoundNo = normalizeRoundNo(roundNo, singlePlan);
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalMapper.selectListStatusType2(handleId, targetRoundNo)
                                                                           .stream()
                                                                           .filter(item -> Objects.equals(item.getProcess(), HandleProcessEnum.PLAN_IMPLEMENTATION.getCode()))
                                                                           .filter(item -> Objects.equals(item.getStatus(), 1))
                                                                           .toList();
        DzTaskHandleApprovalVo latestApproval = approvals.isEmpty() ? null : approvals.getLast();
        String approver = buildApproverName(latestApproval);
        String description = StringUtils.isBlank(approver) ? "已审批通过,同意发布" : approver + "已审批通过,同意发布";
        upsertProgress(singlePlan, DefRespPlanStatusEnum.APPROVAL_PASSED.getCode(), targetRoundNo, progressTime, description);
    }

    @Override
    public void recordTaskPublished(Long handleId, Date progressTime) {
        DefRespPlan singlePlan = findSinglePlanByHandleId(handleId);
        if (singlePlan == null) {
            return;
        }
        upsertProgress(singlePlan, DefRespPlanStatusEnum.TASK_PUBLISHED.getCode(), resolveRoundNo(singlePlan), progressTime,
            "针对此次处置事件,已对范围内群众发送告警短信,并向相关巡查员派发任务");
    }

    @Override
    public void recordEnded(Long handleId, Date progressTime) {
        DefRespPlan singlePlan = findSinglePlanByHandleId(handleId);
        if (singlePlan == null) {
            return;
        }
        upsertProgress(singlePlan, DefRespPlanStatusEnum.ENDED.getCode(), resolveRoundNo(singlePlan), progressTime,
            "接收到处置事件归档信息,此次防御响应已结束");
        if (!Objects.equals(singlePlan.getStatus(), DefRespPlanStatusEnum.ENDED.getCode())) {
            DefRespPlan update = new DefRespPlan();
            update.setId(singlePlan.getId());
            update.setStatus(DefRespPlanStatusEnum.ENDED.getCode());
            update.setUpdateDate(progressTime == null ? new Date() : progressTime);
            dzDefRespPlanMapper.updateById(update);
        }
    }

    private boolean isSinglePlan(DefRespPlan singlePlan) {
        return singlePlan != null && Objects.equals(singlePlan.getType(), DefRespPlanTypeEnum.SINGLE.getCode()) && singlePlan.getId() != null;
    }

    private DefRespPlan findSinglePlanByHandleId(Long handleId) {
        if (handleId == null) {
            return null;
        }
        return dzDefRespPlanMapper.selectOne(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getHandleId, handleId)
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.SINGLE.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .orderByDesc(DefRespPlan::getCreateDate, DefRespPlan::getId)
                    .last("limit 1")
        );
    }

    private void upsertProgress(DefRespPlan singlePlan, Integer status, Integer roundNo, Date progressTime, String description) {
        if (status == null || StringUtils.isBlank(description)) {
            return;
        }
        if (!isSinglePlan(singlePlan)) {
            return;
        }
        Integer targetRoundNo = normalizeRoundNo(roundNo, singlePlan);
        DzProcessProgress existing = dzProcessProgressMapper.selectOne(
            Wrappers.<DzProcessProgress>lambdaQuery()
                    .eq(DzProcessProgress::getDefId, singlePlan.getId())
                    .eq(DzProcessProgress::getStatus, status)
                    .eq(DzProcessProgress::getRoundNo, targetRoundNo)
                    .eq(DzProcessProgress::getActionType, 1)
                    .orderByAsc(DzProcessProgress::getId)
                    .last("limit 1")
        );
        if (existing == null) {
            DzProcessProgressBo bo = new DzProcessProgressBo();
            bo.setDefId(singlePlan.getId());
            bo.setStatus(status);
            bo.setRoundNo(targetRoundNo);
            bo.setActionType(1);
            bo.setCreateDate(progressTime == null ? new Date() : progressTime);
            bo.setDescription(description);
            dzProcessProgressService.insertByBo(bo);
            return;
        }
        if (!StringUtils.equals(existing.getDescription(), description)) {
            DzProcessProgress update = new DzProcessProgress();
            update.setId(existing.getId());
            update.setDescription(description);
            dzProcessProgressMapper.updateById(update);
        }
    }

    private int resolveRoundNo(DefRespPlan singlePlan) {
        return singlePlan == null || singlePlan.getCurrentRoundNo() == null ? 1 : singlePlan.getCurrentRoundNo();
    }

    private int normalizeRoundNo(Integer roundNo, DefRespPlan singlePlan) {
        return roundNo == null || roundNo < 1 ? resolveRoundNo(singlePlan) : roundNo;
    }

    private String buildStartedDescription(DzTaskHandle taskHandle, Long operatorUserId) {
        String operatorName = resolveUserDisplayName(operatorUserId);
        String location = StringUtils.defaultString(taskHandle.getCity())
            + StringUtils.defaultString(taskHandle.getCounty())
            + StringUtils.defaultString(taskHandle.getStreet());
        if (StringUtils.isBlank(operatorName)) {
            throw new ServiceException("操作用户不存在");
        }
        return "用户" + operatorName + "手动启动" + location + "单点防御响应";
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserVo user = sysUserService.selectUserById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.defaultIfBlank(user.getNickName(), user.getUserName());
    }

    private String buildApproverName(DzTaskHandleApprovalVo approvalVo) {
        if (approvalVo == null) {
            return null;
        }
        if (StringUtils.isNotBlank(approvalVo.getRoleName()) && StringUtils.isNotBlank(approvalVo.getNickname())) {
            return approvalVo.getRoleName() + approvalVo.getNickname();
        }
        return StringUtils.defaultIfBlank(approvalVo.getRoleName(), approvalVo.getNickname());
    }
}
