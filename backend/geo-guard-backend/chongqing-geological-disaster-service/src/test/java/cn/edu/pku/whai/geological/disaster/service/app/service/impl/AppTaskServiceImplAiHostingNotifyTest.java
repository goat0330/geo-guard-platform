/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.service.impl;

import org.dromara.common.core.service.OssService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AiPictureDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.DisasterDetailedAddressResolver;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("dev")
class AppTaskServiceImplAiHostingNotifyTest {

    @Test
    void updateAiPictureResultBroadcastsOverviewRefreshSignal() throws Exception {
        Fixture fixture = fixture();
        AiPictureDto dto = new AiPictureDto();
        dto.setAiRiskLevel(3);
        dto.setAiRiskLabel("高风险");
        dto.setAiReportDetail("识别完成");
        dto.setAiVisionProps(Map.of("source", "unit-test"));

        Method method = AppTaskServiceImpl.class.getDeclaredMethod("updateAiPictureResult", Long.class, AiPictureDto.class);
        method.setAccessible(true);
        method.invoke(fixture.service(), 88L, dto);

        ArgumentCaptor<DzReportDisaster> captor = ArgumentCaptor.forClass(DzReportDisaster.class);
        verify(fixture.reportMapper()).updateById(captor.capture());
        DzReportDisaster update = captor.getValue();
        assertThat(update.getId()).isEqualTo(88L);
        assertThat(update.getAiRiskLevel()).isEqualTo(3);
        assertThat(update.getAiReportDetail()).isEqualTo("识别完成");
        verify(fixture.chainSummaryService()).refreshReportRiskLevelIfCurrent(88L);
        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "ai_vision",
            "ai_picture_result_updated",
            "dz_report_disaster",
            88L,
            null
        );
    }

    private Fixture fixture() {
        DzReportDisasterMapper reportMapper = mock(DzReportDisasterMapper.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        IDzTaskProcessChainNodeService chainNodeService = mock(IDzTaskProcessChainNodeService.class);
        IDzTaskProcessChainSummaryService chainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IDzTaskHandleService> taskHandleServiceProvider = mock(ObjectProvider.class);
        AppTaskServiceImpl service = new AppTaskServiceImpl(
            new AppTaskProps(),
            mock(ISysOssService.class),
            mock(OssService.class),
            mock(DzTaskDistListMapper.class),
            reportMapper,
            mock(DzDefRespPlanMapper.class),
            mock(DzRiskAssessmentMapper.class),
            mock(DzTaskDistListHistoryMapper.class),
            mock(DifyAgentClient.class),
            mock(ISlopeUnitService.class),
            mock(ISlopeUnitGridMemberRelationService.class),
            mock(ISysUserService.class),
            mock(ITaskSmsContentService.class),
            mock(DisasterDetailedAddressResolver.class),
            taskHandleServiceProvider,
            chainNodeService,
            chainSummaryService,
            notifyService
        );
        return new Fixture(service, reportMapper, chainSummaryService, notifyService);
    }

    private record Fixture(AppTaskServiceImpl service,
                           DzReportDisasterMapper reportMapper,
                           IDzTaskProcessChainSummaryService chainSummaryService,
                           AiHostingOverviewNotifyService notifyService) {
    }
}
