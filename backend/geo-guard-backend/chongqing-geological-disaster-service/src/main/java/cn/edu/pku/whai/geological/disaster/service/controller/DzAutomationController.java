/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutomationConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationStatusVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自动化运营配置。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/automation")
public class DzAutomationController extends BaseController {

    private final IDzAutomationService dzAutomationService;

    /**
     * 查询自动化运营配置状态。
     *
     * @return 自动化运营当前配置与运行状态
     */
    @GetMapping("/status")
    public R<AutomationStatusVo> status() {
        return R.ok(dzAutomationService.getStatus());
    }

    /**
     * 更新自动化运营配置。
     *
     * @param bo 自动化运营配置参数
     * @return 更新后的自动化运营状态
     */
    @Log(title = "自动化运营配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping("/config")
    public R<AutomationStatusVo> config(@RequestBody @Validated AutomationConfigBo bo) {
        return R.ok(dzAutomationService.updateConfig(bo));
    }
}
