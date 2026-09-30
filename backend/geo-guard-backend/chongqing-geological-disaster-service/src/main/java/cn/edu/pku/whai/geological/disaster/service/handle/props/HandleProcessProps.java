/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.handle.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 处置管理handle API配置
 *
 * @author whai
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dizai.handle")
public class HandleProcessProps {

    /**
     * 撤离方案生成接口地址
     */
    private String processUrl = "http://127.0.0.1:8000/api/v1/handle/process";

    /**
     * 撤离方案流式生成接口地址
     */
    private String processStreamUrl = "http://127.0.0.1:8000/api/v1/handle/process/stream";

    /**
     * 撤离方案重新生成接口地址
     */
    private String regenerateUrl = "http://127.0.0.1:8000/api/v1/handle/regenerate";

    /**
     * 撤离方案流式重新生成接口地址
     */
    private String regenerateStreamUrl = "http://127.0.0.1:8000/api/v1/handle/regenerate/stream";

    /**
     * 疏散路线生成接口地址
     */
    private String evacuationRouteUrl = "http://127.0.0.1:8000/api/v1/handle/evacuation-route";

    /**
     * 高程查询接口地址
     */
    private String elevationQueryUrl = "http://127.0.0.1:8000/api/v1/elevation/query";

    /**
     * 应急调查报告生成接口地址
     */
    private String reportUrl = "http://127.0.0.1:8900/v1/report/stream";

    /**
     * 应急调查报告修订接口地址
     */
    private String reportReviseUrl = "http://127.0.0.1:8900/v1/report/revise/stream";

    /**
     * 应急调查报告生成流式接口地址
     */
    private String reportStreamUrl = "http://127.0.0.1:8900/v1/report/stream";

    /**
     * 应急调查报告修订流式接口地址
     */
    private String reportReviseStreamUrl = "http://127.0.0.1:8900/v1/report/revise/stream";
}
