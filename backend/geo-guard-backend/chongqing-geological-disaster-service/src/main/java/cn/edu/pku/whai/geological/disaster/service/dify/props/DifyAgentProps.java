/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.dify")
public class DifyAgentProps {
    private String url;
    private String knowledgeBaseApiKey;
    private String demoKey;
    private String homeKey;
    private String aiPictureKey;
    private int aiPictureConnectTimeoutMs = 30000;
    private int aiPictureReadTimeoutMs = 180000;
    private int aiPictureWriteTimeoutMs = 60000;
    private String aiReportKey;
    private String aiReviewReportKey;
    private String aiAnalyzeAlarmKey;
    private String aiEvacuationPlanKey;
    private String aiTaskProcessChainKey;
}
