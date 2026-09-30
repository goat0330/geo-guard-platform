/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 模拟/联调接口全局配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.simulation")
public class DizaiSimulationProps {

    /**
     * 是否开启模拟/联调接口。
     */
    private Boolean enabled = false;
}
