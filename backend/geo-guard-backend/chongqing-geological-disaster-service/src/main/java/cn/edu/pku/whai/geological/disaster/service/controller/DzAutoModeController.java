/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutoModeConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutoModeStatusVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationStatusVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAiHostingService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自动模式开关。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/autoMode")
public class DzAutoModeController extends BaseController {

    private final IDzAutoModeService dzAutoModeService;
    private final IDzAutomationService dzAutomationService;
    private final IDzAiHostingService dzAiHostingService;

    /**
     * 查询自动模式当前状态。
     *
     * @return 自动模式开关、步骤间隔、执行人以及自动化摘要信息
     */
    @GetMapping("/status")
    public R<AutoModeStatusVo> status() {
        AutoModeStatusVo status = dzAutoModeService.getStatus();
        status.setAutomationSummary(buildAutomationSummary(dzAutomationService.getStatus()));
        return R.ok(status);
    }

    /**
     * 开启自动模式。
     *
     * @param bo 自动模式配置；为空时仅开启开关，保留现有配置
     * @return 开启后的自动模式状态
     */
    @Log(title = "自动模式开关", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/open")
    public R<AutoModeStatusVo> open(@RequestBody(required = false) @Validated AutoModeConfigBo bo) {
        return R.ok(dzAutoModeService.open(bo));
    }

    /**
     * 关闭自动模式。
     *
     * @return 关闭后的自动模式状态
     */
    @Log(title = "自动模式开关", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/close")
    public R<AutoModeStatusVo> close() {
        return R.ok(dzAutoModeService.close());
    }

    /**
     * 更新自动模式配置。
     *
     * @param bo 自动模式配置，包含步骤间隔和执行人信息
     * @return 更新后的自动模式状态
     */
    @Log(title = "自动模式配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping("/config")
    public R<AutoModeStatusVo> config(@RequestBody @Validated AutoModeConfigBo bo) {
        return R.ok(dzAutoModeService.updateConfig(bo));
    }

    /**
     * 查询 AI 托管概览。
     *
     * @return AI 托管状态、统计和展示记录
     */
    @GetMapping("/ai-hosting/overview")
    public R<AiHostingOverviewVo> aiHostingOverview(@RequestParam(required = false) Long recordId) {
        return R.ok(dzAiHostingService.getOverview(recordId));
    }

    /**
     * 分页查询 AI 托管开启记录。
     *
     * @param pageQuery 分页参数
     * @param limit 每条托管记录内返回的历史时间窗口组数，默认 50，最大 200
     * @return AI 托管开启记录分页数据
     */
    @GetMapping("/ai-hosting/records")
    public TableDataInfo<AiHostingRecordSessionVo> aiHostingRecords(
        PageQuery pageQuery,
        @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return dzAiHostingService.listRecords(pageQuery, limit);
    }

    private Map<String, Object> buildAutomationSummary(AutomationStatusVo status) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (status == null) {
            return summary;
        }
        summary.put("dailyPatrolGenerateEnabled", status.getDailyPatrolGenerateEnabled());
        summary.put("dailyPatrolGenerateTime", status.getDailyPatrolGenerateTime());
        summary.put("dailyPatrolAutoPushEnabled", status.getDailyPatrolAutoPushEnabled());
        summary.put("unpushedAutoPushEnabled", status.getUnpushedAutoPushEnabled());
        summary.put("reminderEnabled", status.getReminderEnabled());
        summary.put("warningAutoPushMonitorTaskEnabled", status.getWarningAutoPushMonitorTaskEnabled());
        summary.put("reportAutoHandleEnabled", status.getReportAutoHandleEnabled());
        summary.put("reportAutoPushTaskEnabled", status.getReportAutoPushTaskEnabled());
        summary.put("predictionAutoCreatePushEnabled", status.getPredictionAutoCreatePushEnabled());
        summary.put("predictionAutoPushEnabled", status.getPredictionAutoPushEnabled());
        summary.put("taskSmsEnabled", status.getTaskSmsEnabled());
        summary.put("msgNoticeEnabled", status.getMsgNoticeEnabled());
        return summary;
    }
}
