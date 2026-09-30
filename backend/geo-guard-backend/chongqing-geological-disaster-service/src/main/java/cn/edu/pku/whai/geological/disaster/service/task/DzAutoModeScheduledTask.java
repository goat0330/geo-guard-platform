/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.task;

import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动模式定时扫描任务。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DzAutoModeScheduledTask {

    private final IDzAutoModeService dzAutoModeService;

    @Scheduled(fixedDelayString = "${dizai.auto-mode.scan-delay-ms:10000}")
    public void scan() {
        dzAutoModeService.runOnce();
    }
}
