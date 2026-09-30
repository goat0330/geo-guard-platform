/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.SmsConfigUpdateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.service.ISmsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信动态配置管理。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/sms/config")
public class SmsConfigController extends BaseController {

    private final ISmsConfigService smsConfigService;

    /**
     * 查询当前短信动态配置。
     *
     * @return 脱敏后的有效配置
     */
    @GetMapping
    public R<SmsConfigVo> getConfig() {
        return R.ok(smsConfigService.getConfig());
    }

    /**
     * 部分更新短信动态配置。
     *
     * @param bo 待更新字段
     * @return 更新后的脱敏配置
     */
    @Log(title = "短信动态配置", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PutMapping
    public R<SmsConfigVo> updateConfig(@RequestBody SmsConfigUpdateBo bo) {
        return R.ok(smsConfigService.updateConfig(bo));
    }
}
