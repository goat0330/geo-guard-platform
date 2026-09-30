/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsTestSendVo;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信通用接口
 *
 * @author whai
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/sms")
public class SmsController extends BaseController {

    private static final String DEFAULT_TEST_CONTENT = "地质灾害防治系统短信通道连通性测试";

    private final SmsSendService smsSendService;

    /**
     * 测试短信发送是否正常
     *
     * @param phone   目标手机号
     * @param content 短信正文（可选，默认使用测试文案）
     */
    @Log(title = "短信发送测试", businessType = BusinessType.OTHER, operatorType = OperatorType.PLATFORM)
    @RateLimiter(key = "#phone", time = 60, count = 3)
    @GetMapping("/testSend")
    public R<SmsTestSendVo> testSend(
        @RequestParam @NotBlank(message = "手机号不能为空") String phone,
        @RequestParam(required = false) String content) {
        String msg = StringUtils.isNotBlank(content) ? content : DEFAULT_TEST_CONTENT;
        return R.ok(smsSendService.testSend(msg, phone));
    }

    /**
     * 查询短信平台状态报告
     */
    @Log(title = "短信状态报告查询", businessType = BusinessType.OTHER, operatorType = OperatorType.PLATFORM)
    @GetMapping("/report")
    public R<JsonNode> report() {
        return R.ok(smsSendService.queryReport());
    }
}
