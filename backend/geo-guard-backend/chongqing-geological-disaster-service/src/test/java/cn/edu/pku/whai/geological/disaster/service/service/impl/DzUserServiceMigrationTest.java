/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("dev")
class DzUserServiceMigrationTest {
    /** 分页仍由系统服务负责，辖区只按当前页批量回填，避免跨页或跨用户混淆。 */
    @Test
    void pagePreservesTotalAndAssociatesRegionsByUser() {
        var users=mock(ISysUserService.class);
        var regions=mock(IDzUserAdRegionService.class);
        var first=new SysUserVo();first.setUserId(1L);
        var second=new SysUserVo();second.setUserId(2L);
        when(users.selectPageUserList(any(),any())).thenReturn(new TableDataInfo<>(List.of(first,second),8L));
        var region=new DzUserAdRegionVo();region.setUserId(1L);region.setAdRegionId("500101");
        when(regions.queryList(any())).thenReturn(List.of(region));
        var service=new DzUserServiceImpl(users,regions,mock(AdRegionMapper.class));
        var result=service.selectPageUserList(new SysUserBo(),new PageQuery(10,1));
        assertThat(result.getTotal()).isEqualTo(8);
        assertThat(result.getData().get(0).getUserAdRegions()).containsExactly(region);
        assertThat(result.getData().get(1).getUserAdRegions()).isEmpty();
        verify(regions).queryList(argThat(bo -> bo.getUserIds().equals(List.of(1L,2L))));
    }

    /** 未提交辖区变更时保留已有绑定，用户更新不应顺带清空权限范围。 */
    @Test
    void updateWithoutRegionPayloadDoesNotReplaceBindings() {
        var users=mock(ISysUserService.class);
        var regions=mock(IDzUserAdRegionService.class);
        when(users.updateUser(any())).thenReturn(1);
        var bo=new DzUserBo();bo.setUserId(1L);
        var service=new DzUserServiceImpl(users,regions,mock(AdRegionMapper.class));
        assertThat(service.updateUser(bo)).isEqualTo(1);
        verifyNoInteractions(regions);
    }
}
