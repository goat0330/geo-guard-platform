/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.sms.IEvacuationSmsSendService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 撤离短信群发接口（按 handleId 多方案群发，发送能力预留）
 *
 * @author whai
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/sms/evacuation")
public class EvacuationSmsController extends BaseController {
    private final IEvacuationSmsSendService evacuationSmsSendService;

    /**
     * 按 handleId 生成短信,用于展示
     *
     * @param handleId 处置与撤离方案主键
     */
    @Log(title = "撤离短信生成", businessType = BusinessType.OTHER, operatorType = OperatorType.PLATFORM)
    @GetMapping("/generate")
    public R<Map<String, String>> generate(@RequestParam("handleId") @NotNull(message = "handleId不能为空") String handleId) {
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return R.ok(evacuationSmsSendService.generateSmsByHandleId(handleIdLong));
    }
}
