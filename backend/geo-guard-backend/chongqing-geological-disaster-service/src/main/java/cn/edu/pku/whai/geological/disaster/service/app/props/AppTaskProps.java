/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.app")
public class AppTaskProps {
    /**
     * 是否启用 APP 出站接口调用，仅生产环境开启
     */
    private Boolean enabled = false;
    private String prefixUrl;
    private String userName;
    private String password;
    private Integer type;
    private String sysToken;
    /**
     * 是否启用 APP 接口 token 校验
     */
    private Boolean authEnabled = true;
    private String ossPhotoPreviewPublicUrl;

    /**
     * 是否自动将今天以前未完成的巡查任务置为逾期；默认关闭，保留人工过期能力
     */
    private Boolean autoExpirePreviousDayPatrolTasksEnabled = false;

    /**
     * 打卡电子围栏半径，单位米
     */
    private Integer checkInFenceRadiusMeters;

    /**
     * 群众报灾入参幂等校验窗口，单位秒
     */
    private Long publicReportIdempotentWindowSeconds = 3600L;

    /**
     * 风险区巡查员兜底催办起始时间，格式 HH:mm
     */
    private String patrolReminderStartTime;

    /**
     * 风险区巡查员催办间隔，单位分钟
     */
    private Integer patrolReminderIntervalMinutes;

    /**
     * 连续逾期上报阈值
     */
    private Integer continuousOverdueThreshold;
}
