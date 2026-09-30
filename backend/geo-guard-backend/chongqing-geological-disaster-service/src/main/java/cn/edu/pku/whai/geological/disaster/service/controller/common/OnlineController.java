/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.anno.ReqLogIgnore;
import cn.edu.pku.whai.geological.disaster.service.utils.OnlineUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户
 *
 * @author kongweiguang
 */
@Slf4j
@RestController
@RequestMapping("/dizai/online")
@RequiredArgsConstructor
public class OnlineController {

    /**
     * 记录在线心跳并保持接口幂等；Redis 异常不阻断前端心跳响应。
     */
    @ReqLogIgnore
    @PostMapping("/heartbeat")
    public R<Void> heartbeat() {
        try {
            OnlineUtil.addRealtimeOnlineUser(LoginHelper.getUserId());
        } catch (Exception e) {
            log.error("heartbeat error. ", e);
        }
        return R.ok();
    }
}
