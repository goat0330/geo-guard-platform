/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskAiProcessChainVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainPreviewResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainPushResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessCapabilityVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskDistListControllerTest {

    @Test
    void processChainUsesChainIdPathVariable() {
        IDzTaskDistListService service = mock(IDzTaskDistListService.class);
        DzTaskDistListController controller = new DzTaskDistListController(service);
        when(service.queryProcessChain("20260629-001")).thenReturn(List.of(new TaskProcessChainNodeVo()));

        controller.processChain("20260629-001");

        verify(service).queryProcessChain("20260629-001");
    }

    @Test
    void aiProcessChainUsesChainIdPathVariable() {
        IDzTaskDistListService service = mock(IDzTaskDistListService.class);
        DzTaskDistListController controller = new DzTaskDistListController(service);
        when(service.queryAiProcessChain("20260629-001")).thenReturn(new TaskAiProcessChainVo());

        controller.aiProcessChain("20260629-001");

        verify(service).queryAiProcessChain("20260629-001");
    }

    @Test
    void processChainCapabilitiesUsesChainIdQueryField() {
        IDzTaskDistListService service = mock(IDzTaskDistListService.class);
        DzTaskDistListController controller = new DzTaskDistListController(service);
        when(service.queryProcessChainCapabilities("20260629-001")).thenReturn(new TaskProcessCapabilityVo());

        controller.processChainCapabilities("20260629-001");

        verify(service).queryProcessChainCapabilities("20260629-001");
    }

    @Test
    void pushByChainScopeUsesRequestBody() {
        IDzTaskDistListService service = mock(IDzTaskDistListService.class);
        DzTaskDistListController controller = new DzTaskDistListController(service);
        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        when(service.pushByChainScope(bo)).thenReturn(new TaskDistChainPushResultVo());

        controller.pushByChainScope(bo);

        verify(service).pushByChainScope(bo);
    }

    @Test
    void previewSmsByChainScopeUsesRequestBody() {
        IDzTaskDistListService service = mock(IDzTaskDistListService.class);
        DzTaskDistListController controller = new DzTaskDistListController(service);
        TaskDistChainScopePushBo bo = new TaskDistChainScopePushBo();
        when(service.previewSmsByChainScope(bo)).thenReturn(new TaskDistChainPreviewResultVo());

        controller.previewSmsByChainScope(bo);

        verify(service).previewSmsByChainScope(bo);
    }
}
