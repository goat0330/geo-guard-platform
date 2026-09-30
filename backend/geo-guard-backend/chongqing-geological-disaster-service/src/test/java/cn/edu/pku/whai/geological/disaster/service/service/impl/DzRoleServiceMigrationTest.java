/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.mybatis.core.page.PageQuery;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRoleUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RoleUserVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRoleMapper;
import cn.edu.pku.whai.geological.disaster.service.utils.OnlineUtil;
import org.dromara.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("dev")
class DzRoleServiceMigrationTest {
    /** 默认仍查询专家角色，并仅对当前页补在线状态，测试不读取真实用户或 Redis。 */
    @Test
    void defaultExpertRoleAndOnlineStateArePreserved() {
        var mapper=mock(DzRoleMapper.class);
        var service=new DzRoleServiceImpl(mapper,mock(ISysRoleService.class));
        var first=new RoleUserVo(); first.setUserId(11L);
        var second=new RoleUserVo(); second.setUserId(12L);
        Page<RoleUserVo> page=new Page<>(1,10,2); page.setRecords(List.of(first,second));
        when(mapper.selectRoleUserPage(any(),any())).thenAnswer(inv -> {
            QueryWrapper<Object> wrapper=inv.getArgument(1);
            assertThat(wrapper.getSqlSegment()).contains("u.del_flag", "r.role_key", "GROUP BY", "ORDER BY");
            assertThat(wrapper.getParamNameValuePairs().values()).contains("dz_zj");
            return page;
        });
        try(var online=mockStatic(OnlineUtil.class)) {
            online.when(() -> OnlineUtil.getRealtimeOnlineUser(List.of(11L,12L))).thenReturn(Set.of(12L));
            var result=service.queryPageList(null,new PageQuery(10,1));
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(first.getOnline()).isZero();
            assertThat(second.getOnline()).isEqualTo(1);
        }
    }

    /** 自定义角色去空白去重，昵称作为绑定参数查询，不扩大到全部用户。 */
    @Test
    void customRolesAndNicknameRemainQueryParameters() {
        var mapper=mock(DzRoleMapper.class);
        var service=new DzRoleServiceImpl(mapper,mock(ISysRoleService.class));
        var bo=new DzRoleUserBo(); bo.setRoleKeys(List.of(" dz_zj ","dz_zby","dz_zj"," "));bo.setNickName("expert");
        when(mapper.selectRoleUserPage(any(),any())).thenAnswer(inv -> {
            QueryWrapper<Object> wrapper=inv.getArgument(1);
            assertThat(wrapper.getSqlSegment()).contains("u.nick_name");
            assertThat(wrapper.getParamNameValuePairs().values()).contains("dz_zj","dz_zby","%expert%");
            assertThat(wrapper.getParamNameValuePairs().values().stream().filter("dz_zj"::equals).count()).isEqualTo(1);
            return new Page<RoleUserVo>(1,10,0);
        });
        try(var online=mockStatic(OnlineUtil.class)) {
            online.when(() -> OnlineUtil.getRealtimeOnlineUser(List.of())).thenReturn(Set.of());
            assertThat(service.queryPageList(bo,new PageQuery(10,1)).getTotal()).isZero();
        }
    }

    /** 角色详情继续委托系统服务，不重建角色模型或绕过系统查询契约。 */
    @Test
    void roleLookupUsesExistingSystemService() {
        var roles=mock(ISysRoleService.class);
        when(roles.selectRolesByUserId(42L)).thenReturn(List.of());
        assertThat(new DzRoleServiceImpl(mock(DzRoleMapper.class),roles).queryRolesByUserId(42L)).isEmpty();
        verify(roles).selectRolesByUserId(42L);
    }
}
