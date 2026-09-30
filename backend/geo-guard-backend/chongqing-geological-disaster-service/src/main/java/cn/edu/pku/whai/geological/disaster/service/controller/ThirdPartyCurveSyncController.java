/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyCurveRefreshBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyCurveRefreshVo;
import cn.edu.pku.whai.geological.disaster.service.annotation.SimulationEnabledRequired;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ThirdPartyWarningRiskIncreaseSimulateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.WeatherWarningSyncBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ThirdPartyWarningRiskIncreaseSimulateVo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.ThirdPartyCurveSyncService;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ThirdPartyWarningRiskReplayVo;
import cn.edu.pku.whai.geological.disaster.service.task.DefRespScheduledTask;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.ThirdPartyWarningRiskLinkageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三方设备监测曲线同步接口
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/dizai/warningData")
public class ThirdPartyCurveSyncController {

    private final ThirdPartyCurveSyncService thirdPartyCurveSyncService;
    private final ThirdPartyWarningRiskLinkageService thirdPartyWarningRiskLinkageService;
    private final DefRespScheduledTask defRespScheduledTask;

    /**
     * 按时间段全量重刷设备监测曲线数据
     */
    @Log(title = "三方设备监测曲线同步", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/refreshDeviceCurveByTimeRange")
    public R<ThirdPartyCurveRefreshVo> refreshDeviceCurveByTimeRange(@Validated @RequestBody ThirdPartyCurveRefreshBo bo) {
        return R.ok(thirdPartyCurveSyncService.refreshDeviceCurveData(bo));
    }

    /**
     * 根据今日已同步预警数据重放风险联动
     */
    @Log(title = "三方预警风险联动补跑", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @SimulationEnabledRequired(message = "今日预警风险联动补跑接口已关闭")
    @PostMapping("/replayTodayWarningRiskLinkage")
    public R<ThirdPartyWarningRiskReplayVo> replayTodayWarningRiskLinkage() {
        return R.ok(thirdPartyWarningRiskLinkageService.replayTodayWarnings());
    }

    /**
     * 模拟预警同步导致斜坡单元动态风险等级升高。
     */
    @Log(title = "三方预警风险联动模拟", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @SimulationEnabledRequired(message = "风险等级升高模拟接口已关闭")
    @PostMapping("/simulateWarningRiskIncrease")
    public R<ThirdPartyWarningRiskIncreaseSimulateVo> simulateWarningRiskIncrease(
        @Validated @RequestBody ThirdPartyWarningRiskIncreaseSimulateBo bo) {
        return R.ok(thirdPartyWarningRiskLinkageService.simulateRiskIncrease(bo));
    }

    /**
     * 手动触发第三方气象预警同步，可指定起止日期。
     */
    @Log(title = "第三方气象预警同步", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/syncWeatherWarning")
    public R<String> syncWeatherWarning(@Validated @RequestBody(required = false) WeatherWarningSyncBo bo) {
        if (bo == null) {
            return buildTaskTriggerResponse(defRespScheduledTask.triggerSyncWeatherWarning(), "气象预警同步");
        }
        return buildTaskTriggerResponse(defRespScheduledTask.triggerSyncWeatherWarning(bo.getStartTime(), bo.getEndTime()), "气象预警同步");
    }

    /**
     * 手动触发第三方预警 AI 解析入库。
     */
    @Log(title = "第三方预警AI解析", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/parseAlarmWithAi")
    public R<String> parseAlarmWithAi() {
        return buildTaskTriggerResponse(defRespScheduledTask.triggerParseAlarmWithAi(), "预警AI解析");
    }

    private R<String> buildTaskTriggerResponse(DefRespScheduledTask.TaskTriggerResult result, String taskName) {
        return switch (result) {
            case SUCCESS -> R.ok("触发成功");
            case ALREADY_SUCCESS -> R.ok("当前小时已执行成功，无需重复触发");
            case RUNNING -> R.fail("当前已有" + taskName + "任务在执行，请稍后重试");
            case FAILED -> R.fail(taskName + "执行失败，请查看日志");
        };
    }
}
