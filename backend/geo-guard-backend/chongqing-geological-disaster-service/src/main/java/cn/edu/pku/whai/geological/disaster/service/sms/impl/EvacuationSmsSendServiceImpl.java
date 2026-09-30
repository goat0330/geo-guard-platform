/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.service.IDataPersonService;
import cn.edu.pku.whai.geological.disaster.data.service.IEvacuationPlanService;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsBatchResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsResult;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 按 handleId 编排多方案撤离短信群发：查处置与方案、按方案生成正文与范围查号、调用发送预留接口
 *
 * @author whai
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EvacuationSmsSendServiceImpl implements IEvacuationSmsSendService {

    private final IEvacuationPlanService evacuationPlanService;
    private final IEvacuationSmsContentService evacuationSmsContentService;
    private final IDataPersonService dataPersonService;
    private final SmsSendService smsSendService;
    private final DzTaskHandleMapper dzTaskHandleMapper;

    /**
     * 按 handleId 编排多方案撤离短信群发
     *
     * @param handleId 处置记录 ID
     * @return 是否发送成功
     */
    @Override
    public EvacuationSmsBatchResult sendMassesByHandleId(Long handleId) {
        DzTaskHandle handle = dzTaskHandleMapper.selectById(handleId);
        if (handle == null) {
            throw new ServiceException("灾害处置记录不存在");
        }
        List<EvacuationPlan> plans = evacuationPlanService.listByPlanId(handleId);
        if (plans == null || plans.isEmpty()) {
            throw new ServiceException("暂无撤离方案");
        }
        int totalSuccess = 0;
        int totalFail = 0;
        int skippedPlanCount = 0;
        int totalSkipCount = 0;
        int totalCount = 0;
        List<EvacuationSmsResult> taskDetails = new ArrayList<>();

        for (EvacuationPlan plan : plans) {
            String content = evacuationSmsContentService.generate(handle, plan);

            String evacuationArea = plan.getEvacuationArea();
            if (StringUtils.isBlank(evacuationArea)) {
                skippedPlanCount++;
                log.warn("撤离方案无有效撤离区域名称，跳过，handleId={}, planId={}", handleId, plan.getId());
                continue;
            }

            List<String> phones = dataPersonService.listPhoneByEvacuationArea(evacuationArea);
            List<String> filteredPhones = phones == null ? new ArrayList<>() : phones.stream()
                                                                                     .filter(p -> p != null && !p.isBlank())
                                                                                     .distinct()
                                                                                     .collect(Collectors.toList());

            EvacuationSmsResult taskResult = smsSendService.send(content, filteredPhones, SmsSendContext.builder()
                                                                                                        .scene(SmsScene.EVACUATION)
                                                                                                        .bizType(DzSmsSendBatch.BIZ_TYPE_EVACUATION_SMS)
                                                                                                        .bizId(handleId)
                                                                                                        .smsType(DzSmsSendBatch.SMS_TYPE_MASSES)
                                                                                                        .targetType(DzSmsSendBatch.TARGET_TYPE_MASSES)
                                                                                                        .build());
            taskResult.setEvacuationArea(plan.getEvacuationArea());
            taskDetails.add(taskResult);
            totalSuccess += taskResult.getSuccessCount();
            totalFail += taskResult.getFailCount();
            totalSkipCount += taskResult.getSkipCount();
            totalCount += taskResult.getTotalCount();
        }
        return EvacuationSmsBatchResult.builder()
                                       .totalSuccessCount(totalSuccess)
                                       .totalFailCount(totalFail)
                                       .totalCount(totalCount)
                                       .skipCount(skippedPlanCount)
                                       .totalSkipCount(totalSkipCount)
                                       .taskDetails(taskDetails)
                                       .build();
    }

    //todo: 按 handleId 编排负责人撤离短信群发：查处置与方案、按方案生成正文与范围查号、调用发送预留接口
    @Override
    public EvacuationSmsBatchResult sendPrincipalByHandleId(Long handleId) {
        return new EvacuationSmsBatchResult();
    }

    //todo: 启动上面两个任务
    @Override
    public Boolean execute(Long handleId) {
        // 按 handleId 编排群众多方案撤离短信群发
        sendMassesByHandleId(handleId);
        // 按 handleId 编排负责人撤离短信群发
        sendPrincipalByHandleId(handleId);
        //todo: 生成具体的短信任务,用于确认当前状态
        return true;
    }

    @Override
    public Map<String, String> generateSmsByHandleId(Long handleId) {
        DzTaskHandle handle = dzTaskHandleMapper.selectById(handleId);
        List<EvacuationPlan> plans = evacuationPlanService.listByPlanId(handleId);
        if (plans == null || plans.isEmpty()) {
            throw new ServiceException("暂无撤离方案");
        }
        // 按方案 ID 排序，用 LinkedHashMap 保证迭代顺序与 ID 一致
        return plans.stream()
                    .sorted(Comparator.comparingLong(a -> a.getId() == null ? 0L : a.getId()))
                    .collect(Collectors.toMap(
                        EvacuationPlan::getEvacuationArea,
                        plan -> evacuationSmsContentService.generate(handle, plan),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                    ));
    }
}
