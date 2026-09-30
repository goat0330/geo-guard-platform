/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.dify.props.DifyAgentProps;
import cn.hutool.core.util.StrUtil;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.model.DifyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DifyAgentClient {

    private final DifyAgentProps difyAgentProps;

    public DifyChatflowClient getAgent(String key) {
        return DifyClientFactory.createChatWorkflowClient(difyAgentProps.getUrl(), key);
    }

    public DifyChatflowClient getAgent(String key, int connectTimeoutMs, int readTimeoutMs, int writeTimeoutMs) {
        DifyConfig config = DifyConfig.builder()
            .baseUrl(difyAgentProps.getUrl())
            .apiKey(key)
            .connectTimeout(connectTimeoutMs)
            .readTimeout(readTimeoutMs)
            .writeTimeout(writeTimeoutMs)
            .build();
        return DifyClientFactory.createChatWorkflowClient(config);
    }

    /**
     * demo agent client
     */
    public DifyChatflowClient getDemoAgent() {
        return getAgent(difyAgentProps.getDemoKey());
    }

    /**
     * 地灾聊天agent client
     */
    public DifyChatflowClient getChatAgent() {
        return getAgent(difyAgentProps.getHomeKey());
    }

    /**
     * 报灾ai识图 client
     */
    public DifyChatflowClient getAiPictureAgent() {
        return getAgent(
            difyAgentProps.getAiPictureKey(),
            difyAgentProps.getAiPictureConnectTimeoutMs(),
            difyAgentProps.getAiPictureReadTimeoutMs(),
            difyAgentProps.getAiPictureWriteTimeoutMs()
        );
    }

    /**
     * 处置管理ai生成报告agent client
     */
    public DifyChatflowClient getAiReportAgent() {
        return getAgent(difyAgentProps.getAiReportKey());
    }

    /**
     * 处置管理ai生成复盘报告agent client
     */
    public DifyChatflowClient getAiReviewReportAgent() {
        String key = difyAgentProps.getAiReviewReportKey();
        return getAgent(StrUtil.blankToDefault(key, difyAgentProps.getAiReportKey()));
    }

    /**
     * 解析气象局报警数据
     */
    public DifyChatflowClient getAiAnalyzeAlarmAgent() {
        return getAgent(difyAgentProps.getAiAnalyzeAlarmKey());
    }

    /**
     * 撤离方案生成/修改 agent
     */
    public DifyChatflowClient getAiEvacuationPlanAgent() {
        return getAgent(difyAgentProps.getAiEvacuationPlanKey());
    }

    /**
     * 任务流程链路 AI 分析 agent。
     */
    public DifyChatflowClient getAiTaskProcessChainAgent() {
        String key = difyAgentProps.getAiTaskProcessChainKey();
        if (StrUtil.isBlank(key)) {
            throw new ServiceException("Dify任务流程链路AI分析key未配置");
        }
        return getAgent(key);
    }

}
