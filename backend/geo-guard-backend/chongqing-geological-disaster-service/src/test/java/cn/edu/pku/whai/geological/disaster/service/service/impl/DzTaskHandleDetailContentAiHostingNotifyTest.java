/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleDetailContentMapper;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("dev")
class DzTaskHandleDetailContentAiHostingNotifyTest {

    @Test
    void aiReportContentBroadcastsOverviewRefreshSignal() throws Exception {
        Fixture fixture = fixture();
        DzTaskHandleDetailContent content = content(66L, DetailContentTypeEnum.AI_REPORT.getCode());

        notify(fixture.service(), content);

        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "report_content",
            "ai_report_saved",
            "dz_task_handle",
            66L,
            null
        );
    }

    @Test
    void reviewReportContentBroadcastsOverviewRefreshSignal() throws Exception {
        Fixture fixture = fixture();
        DzTaskHandleDetailContent content = content(77L, DetailContentTypeEnum.REVIEW_REPORT.getCode());

        notify(fixture.service(), content);

        verify(fixture.notifyService()).notifyChangedAfterCommit(
            "report_content",
            "review_report_saved",
            "dz_task_handle",
            77L,
            null
        );
    }

    private Fixture fixture() {
        DzTaskHandleDetailContentMapper mapper = mock(DzTaskHandleDetailContentMapper.class);
        AiHostingOverviewNotifyService notifyService = mock(AiHostingOverviewNotifyService.class);
        DzTaskHandleDetailContentServiceImpl service = new DzTaskHandleDetailContentServiceImpl(
            mapper,
            mock(DzDefRespPlanMapper.class),
            mock(IDataAlarmService.class),
            notifyService
        );
        return new Fixture(service, mapper, notifyService);
    }

    private DzTaskHandleDetailContent content(Long bizId, Integer contentType) {
        DzTaskHandleDetailContent content = new DzTaskHandleDetailContent();
        content.setBizType(DetailBizTypeEnum.TASK_HANDLE.getCode());
        content.setBizId(bizId);
        content.setContentType(contentType);
        return content;
    }

    private void notify(DzTaskHandleDetailContentServiceImpl service, DzTaskHandleDetailContent content) throws Exception {
        Method method = DzTaskHandleDetailContentServiceImpl.class.getDeclaredMethod(
            "notifyAiHostingOverviewIfNeeded",
            DzTaskHandleDetailContent.class
        );
        method.setAccessible(true);
        method.invoke(service, content);
    }

    private record Fixture(DzTaskHandleDetailContentServiceImpl service,
                           DzTaskHandleDetailContentMapper mapper,
                           AiHostingOverviewNotifyService notifyService) {
    }
}
