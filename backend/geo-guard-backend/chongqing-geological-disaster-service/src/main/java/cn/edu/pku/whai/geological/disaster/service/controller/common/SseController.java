/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.bo.SseNotifyBo;
import cn.edu.pku.whai.geological.disaster.service.sse.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/dizai/sse")
@RequiredArgsConstructor
@Validated
public class SseController {
    private final SseEmitterManager sseEmitterManager;
    private final SseService sseService;

    /**
     * 建立 SSE 连接
     */
    @GetMapping(value = "connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        return sseEmitterManager.connect(userId, tokenValue);
    }

    /**
     * 通知指定用户信息
     *
     */
    @Log(title = "SSE消息通知", businessType = BusinessType.OTHER, operatorType = OperatorType.PLATFORM)
    @PostMapping("/notifyByUserIds")
    public R<Void> notifyByUserIds(@RequestBody SseNotifyBo bo) {
        sseService.notifyByUserIds(bo);
        return R.ok();
    }
}
