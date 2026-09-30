/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.livekit")
public class LiveKitProps {
    /**
     * 兼容旧配置。
     */
    private String url;

    /**
     * 内网 LiveKit 地址。
     */
    private String innerUrl;

    /**
     * 外网/政务网 LiveKit 地址。
     */
    private String outerUrl;

    /**
     * 判断内网来源的 CIDR 列表。
     */
    private List<String> innerCidrs = new ArrayList<>();

    private String apiKey;
    private String apiSecret;
    private Long tokenTtlSeconds = 21600L;
}
