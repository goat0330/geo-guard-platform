/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.dify.props.DifyAgentProps;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@Tag("dev")
class DifyAgentClientTest {

    /**
     * Missing workflow keys must fail before the third-party factory is called, so a misconfigured
     * environment cannot create a client that only fails later during an external request.
     */
    @Test
    void taskProcessChainAgentRejectsMissingKeyBeforeClientCreation() {
        DifyAgentClient client = new DifyAgentClient(new DifyAgentProps());

        ServiceException exception = assertThrows(ServiceException.class, client::getAiTaskProcessChainAgent);

        assertEquals("Dify任务流程链路AI分析key未配置", exception.getMessage());
    }

    /**
     * The review workflow intentionally falls back to the report key to preserve deployments that
     * share one Dify workflow; static mocking keeps this contract test fully offline.
     */
    @Test
    void reviewAgentFallsBackToReportKeyWithoutCallingDify() {
        DifyAgentProps props = new DifyAgentProps();
        props.setUrl("http://127.0.0.1:9/v1");
        props.setAiReportKey("report-key");
        props.setAiReviewReportKey(" ");
        DifyChatflowClient expected = mock(DifyChatflowClient.class);
        DifyAgentClient client = new DifyAgentClient(props);

        try (MockedStatic<DifyClientFactory> factory = org.mockito.Mockito.mockStatic(DifyClientFactory.class)) {
            factory.when(() -> DifyClientFactory.createChatWorkflowClient(props.getUrl(), props.getAiReportKey()))
                .thenReturn(expected);

            DifyChatflowClient actual = client.getAiReviewReportAgent();

            assertSame(expected, actual);
            factory.verify(() -> DifyClientFactory.createChatWorkflowClient(props.getUrl(), props.getAiReportKey()));
        }
    }
}
