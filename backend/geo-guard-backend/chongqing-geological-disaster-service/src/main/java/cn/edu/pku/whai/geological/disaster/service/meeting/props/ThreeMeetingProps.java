/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.meeting")
public class ThreeMeetingProps {
    private String prefix;
    private String appId;
    private String appKey;
}
