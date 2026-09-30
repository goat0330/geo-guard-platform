/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthCenterProps {
    private List<String> defaultPerms;
    private String defaultUrl;
}
