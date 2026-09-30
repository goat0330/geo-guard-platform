/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzDefRespStartSmsConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzDefRespStartSmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespStartSmsConfigService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 启动短信配置接口
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/dizai/defRespStartSmsConfig")
public class DzDefRespStartSmsConfigController extends BaseController {

    private final IDzDefRespStartSmsConfigService configService;

    /**
     * 查询启动短信配置列表
     */
    @GetMapping("/list")
    public R<List<DzDefRespStartSmsConfigVo>> list() {
        return R.ok(configService.queryList());
    }

    /**
     * 修改启动短信配置
     */
    @Log(title = "启动短信配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit
    @PutMapping("/edit")
    public R<Void> edit(@Validated({EditGroup.class, Default.class}) @RequestBody DzDefRespStartSmsConfigBo bo) {
        return toAjax(configService.updateByBo(bo));
    }
}
