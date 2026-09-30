/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzMsgNoticeBo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzMsgNoticeService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.DzMsgNoticeServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DzMsgNoticeMigrationTest {
    /** 列表沿用当前登录用户范围，不能直接采纳调用方传入的其他用户 ID。 */
    @Test
    void listUsesCurrentUserInsteadOfRequestUser() {
        var service = mock(IDzMsgNoticeService.class);
        var controller = new DzMsgNoticeController(service);
        var bo = new DzMsgNoticeBo();
        bo.setUserId(999L);
        var page = new PageQuery(10, 1);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            controller.list(bo, page);
            verify(service).queryPageList(argThat((DzMsgNoticeBo value) -> value.getUserId().equals(42L)), same(page));
        }
    }

    /** 状态计数必须按登录主体委托 Mapper 统计，迁移不能遗漏用户范围。 */
    @Test
    void statisticsUseCurrentUser() {
        var service = mock(IDzMsgNoticeService.class);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            new DzMsgNoticeController(service).countStatus();
            verify(service).countStatus(42L);
        }
    }

    /** 无选中消息时应在任何更新前失败；使用替身确保测试不会修改真实消息。 */
    @Test
    void handleRejectsEmptySelectionWithoutWriting() {
        var mapper = mock(DzMsgNoticeMapper.class);
        var service = new DzMsgNoticeServiceImpl(mapper);
        assertThatThrownBy(() -> service.handle(new DzMsgNoticeBo()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("请选择");
        verifyNoInteractions(mapper);
    }

    /** 批量处理保持原有逐条已处理状态和更新时间，不依赖真实库或生成的转换器。 */
    @Test
    void handleMarksEverySelectedMessageAsProcessed() {
        var service = spy(new DzMsgNoticeServiceImpl(mock(DzMsgNoticeMapper.class)));
        List<Long> updatedIds = new ArrayList<>();
        doAnswer(invocation -> {
            DzMsgNoticeBo item = invocation.getArgument(0);
            assertThat(item.getStatus()).isEqualTo(1);
            assertThat(item.getUpdateDate()).isNotNull();
            updatedIds.add(item.getId());
            return true;
        }).when(service).updateByBo(any());
        var bo = new DzMsgNoticeBo();
        bo.setIds(List.of(12L, 15L));
        service.handle(bo);
        assertThat(updatedIds).containsExactly(12L, 15L);
    }
}
