/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 三方预警风险联动配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.warning.risk-linkage")
public class ThirdPartyWarningRiskLinkageProps {

    /**
     * dynamicRiskLevel 至少上升多少级才触发监测预警任务。
     */
    private Integer dynamicRiskLevelIncreaseThreshold = 1;

    /**
     * 是否自动推送新任务到 APP；已下发任务更新仍会同步 APP。
     */
    private Boolean autoPushTask = true;

}
