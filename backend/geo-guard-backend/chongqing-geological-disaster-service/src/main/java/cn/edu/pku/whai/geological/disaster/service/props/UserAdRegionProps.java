/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 用户行政区划兼容配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.user-ad-region")
public class UserAdRegionProps {

    /**
     * 用户绑定多个不同层级行政区划时，是否仅使用最高层级作为数据权限范围。
     */
    private Boolean preferHighestLevel = false;
}
