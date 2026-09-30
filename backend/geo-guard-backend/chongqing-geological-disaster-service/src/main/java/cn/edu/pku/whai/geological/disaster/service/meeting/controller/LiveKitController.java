/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.anno.ReqLogIgnore;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.LiveKitTokenReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.LiveKitTokenResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.ILiveKitService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ReqLogIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/meeting/livekit")
public class LiveKitController {

    private final ILiveKitService liveKitService;

    /**
     * 生成会议 LiveKit token
     */
    @Log(title = "LiveKit会议Token", businessType = BusinessType.OTHER, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/token")
    public R<LiveKitTokenResp> generateToken(@Validated @RequestBody LiveKitTokenReq req) {
        return R.ok(liveKitService.generateToken(req));
    }

    /**
     * 接收 LiveKit webhook
     */
    @Log(title = "LiveKit会议回调", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/webhook")
    public R<Void> webhook(@RequestBody String body, @RequestHeader(value = "Authorization", required = false) String authorization) {
        liveKitService.handleWebhook(body, authorization);
        return R.ok();
    }
}
