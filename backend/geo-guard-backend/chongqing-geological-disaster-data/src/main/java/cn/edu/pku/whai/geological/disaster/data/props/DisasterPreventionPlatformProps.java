/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.prevention")
public class DisasterPreventionPlatformProps {
    private String url;
    private String token;
}
