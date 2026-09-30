/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.cache.DzBusinessCacheRedisCache;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzBusinessCacheBo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * 通用业务缓存 Controller 的范围归一化测试，不写入真实 Redis。
 */
@Tag("dev")
class DzCacheControllerTest {

    /**
     * 全局范围应使用固定主体 ID，避免全局标记随当前登录用户漂移。
     */
    @Test
    void shouldUseGlobalFixedUserIdWhenRecordScopeIsGlobal() {
        DzCacheController controller = new DzCacheController();
        DzBusinessCacheBo bo = new DzBusinessCacheBo();
        bo.setBusinessType("task");
        bo.setBusinessContent("done");
        bo.setScope(1);

        try (MockedStatic<LoginHelper> loginHelperMock = mockStatic(LoginHelper.class);
             MockedStatic<DzBusinessCacheRedisCache> cacheMock = mockStatic(DzBusinessCacheRedisCache.class)) {
            loginHelperMock.when(LoginHelper::getUserId).thenReturn(123L);

            R<Void> result = controller.record(bo);

            cacheMock.verify(() -> DzBusinessCacheRedisCache.record(eq("task"), eq(0L), eq("done")));
            assertThat(result.getCode()).isEqualTo(200);
        }
    }

    /**
     * 全局范围查询必须与记录使用相同固定主体 ID，保证读写命中同一标记。
     */
    @Test
    void shouldQueryGlobalCacheByFixedUserIdWhenScopeIsGlobal() {
        DzCacheController controller = new DzCacheController();
        DzBusinessCacheBo bo = new DzBusinessCacheBo();
        bo.setBusinessType("task");
        bo.setScope(1);

        try (MockedStatic<LoginHelper> loginHelperMock = mockStatic(LoginHelper.class);
             MockedStatic<DzBusinessCacheRedisCache> cacheMock = mockStatic(DzBusinessCacheRedisCache.class)) {
            loginHelperMock.when(LoginHelper::getUserId).thenReturn(456L);
            cacheMock.when(() -> DzBusinessCacheRedisCache.hasRecord("task", 0L)).thenReturn(true);

            R<Boolean> result = controller.query(bo);

            cacheMock.verify(() -> DzBusinessCacheRedisCache.hasRecord(eq("task"), eq(0L)));
            assertThat(result.getData()).isTrue();
        }
    }
}
