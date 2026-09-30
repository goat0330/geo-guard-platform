/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service.impl;

import cn.edu.pku.whai.geological.disaster.service.meeting.props.LiveKitProps;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 手工自检入口：验证 LiveKit 连接地址按环境和来源 IP 返回。
 */
@Tag("dev")
public class LiveKitServiceImplTest {

    private final LiveKitServiceImpl liveKitService = new LiveKitServiceImpl(buildProps());

    public static void main(String[] args) {
        LiveKitServiceImplTest test = new LiveKitServiceImplTest();
        test.shouldReturnInnerUrlWhenNotProd();
        test.shouldReturnInnerUrlWhenProdAndClientIpMatchesInnerCidrs();
        test.shouldReturnOuterUrlWhenProdAndClientIpDoesNotMatchInnerCidrs();
        System.out.println("LiveKitServiceImplTest verify passed.");
    }

    @Test
    void shouldReturnInnerUrlWhenNotProd() {
        assertUrl(
            "非 prod 默认返回 innerUrl",
            "ws://127.0.0.1:7890",
            liveKitService.resolveLiveKitUrl("59.208.44.100", false)
        );
    }

    @Test
    void shouldReturnInnerUrlWhenProdAndClientIpMatchesInnerCidrs() {
        assertUrl(
            "prod 且来源 IP 命中内网网段",
            "ws://127.0.0.1:7890",
            liveKitService.resolveLiveKitUrl("127.0.0.1", true)
        );
    }

    @Test
    void shouldReturnOuterUrlWhenProdAndClientIpDoesNotMatchInnerCidrs() {
        assertUrl(
            "prod 且来源 IP 未命中内网网段",
            "ws://59.208.44.93:7880",
            liveKitService.resolveLiveKitUrl("59.208.44.100", true)
        );
    }

    private static void assertUrl(String scenario, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(scenario + " 校验失败，expected=" + expected + ", actual=" + actual);
        }
        System.out.println(scenario + " verify passed: " + actual);
    }

    private static LiveKitProps buildProps() {
        LiveKitProps props = new LiveKitProps();
        props.setUrl("http://127.0.0.1:7880");
        props.setInnerUrl("ws://127.0.0.1:7890");
        props.setOuterUrl("ws://59.208.44.93:7880");
        props.setInnerCidrs(List.of("127.0.0.1/16", "127.0.0.1/8", "127.0.0.1/16"));
        return props;
    }
}
