/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.task;

import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动化运营定时扫描任务。
 */
@RequiredArgsConstructor
@Component
public class DzAutomationScheduledTask {

    private final IDzAutomationService dzAutomationService;

    @Scheduled(fixedDelayString = "${dizai.automation.scan-delay-ms:30000}")
    public void scan() {
        dzAutomationService.runOnce();
    }
}
