/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 三方气象风险预警预警接口参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.warning")
public class WarningDataProps {
    /**
     * 请求地址
     */
    private String url;

    /**
     * 监测点预警处置三方接口地址
     */
    private String monitorUrl;

    /**
     * 用户名
     */
    private String userName;
}
