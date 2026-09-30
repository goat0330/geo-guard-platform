/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataAlarmReportAnalysisReq;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AlgorithmReceiveBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanGenerateByPendingAlarmBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReceiveHandleProcessVo;
import cn.edu.pku.whai.geological.disaster.service.service.IReceiveService;
import cn.edu.pku.whai.geological.disaster.service.task.DefRespScheduledTask;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 算法结果接受
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/dizai/algorithm-receive")
@RequiredArgsConstructor
@SaIgnore
@Validated
@Slf4j
public class ReceiveController {
    private final IReceiveService receiveService;
    private final IDataAlarmService dataAlarmService;
    private final DefRespScheduledTask defRespScheduledTask;

    @Value("${dizai.algorithm.token}")
    private String algorithmToken;

    /**
     * 接受风险评估相关结果
     */
    @Log(title = "接受风险评估相关结果", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("risk")
    public R<Void> risk(@RequestBody AlgorithmReceiveBo bo, @RequestHeader("token") String token) {
        if (!algorithmToken.equals(token)) {
            return R.fail("Token验证失败");
        }
        receiveService.risk(bo);
        return R.ok();
    }

    /**
     * 接收算法侧 report_analysis 预警结果并写入 data_alarm。
     */
    @Log(title = "预警数据接收", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("reportAnalysis")
    public R<List<Long>> receiveReportAnalysis(@RequestBody DataAlarmReportAnalysisReq req) {
        String messageId = req == null ? null : req.getMessageId();
        if (messageId != null
            && dataAlarmService.getExistingCodes(Collections.singleton(messageId)).contains(messageId)) {
            return R.ok("当前记录已保存", Collections.emptyList());
        }
        List<Long> insertedIds = dataAlarmService.importReportAnalysis(req);
        triggerDefRespPlanForInsertedAlarms(insertedIds);
        return R.ok("操作成功", insertedIds);
    }

    /**
     * 将新写入的预警交给原防御响应调度入口，保持算法回调后的联动行为。
     */
    private void triggerDefRespPlanForInsertedAlarms(List<Long> insertedIds) {
        if (insertedIds == null || insertedIds.isEmpty()) {
            return;
        }
        DefRespPlanGenerateByPendingAlarmBo bo = new DefRespPlanGenerateByPendingAlarmBo();
        bo.setIds(insertedIds.stream().map(String::valueOf).toList());
        defRespScheduledTask.triggerGenerateDefRespPlan(bo);
    }

    /**
     * 根据 handleId 查询处置流程回溯结果。
     *
     * @param handleId 处置主键
     * @return 处置流程回溯结果
     */
    @GetMapping("/handleProcess/{handleId}")
    public R<ReceiveHandleProcessVo> queryHandleProcess(@NotNull(message = "handleId不能为空")
                                                        @PathVariable Long handleId) {
        return R.ok(receiveService.queryHandleProcess(handleId));
    }
}
