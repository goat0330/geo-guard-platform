/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutomationConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationRunResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutomationStatusVo;

public interface IDzAutomationService {

    AutomationStatusVo getStatus();

    AutomationStatusVo updateConfig(AutomationConfigBo bo);

    AutomationRunResultVo runOnce();

    boolean isWarningAutoPushMonitorTaskEnabled(boolean fallback);

    boolean isTaskSmsEnabledForAutomation();

    boolean isDailyPatrolGenerateEnabled();

    boolean runDailyPatrolGenerationIfDue();
}
