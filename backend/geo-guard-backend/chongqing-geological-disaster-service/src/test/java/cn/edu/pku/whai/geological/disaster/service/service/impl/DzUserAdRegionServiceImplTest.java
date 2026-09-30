/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzUserAdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.props.UserAdRegionProps;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
public class DzUserAdRegionServiceImplTest {

    @Test
    public void queryEffectiveListByUserIdKeepsAllBindingsWhenSwitchOff() {
        UserAdRegionProps props = new UserAdRegionProps();
        props.setPreferHighestLevel(false);
        DzUserAdRegionMapper mapper = mock(DzUserAdRegionMapper.class);
        when(mapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            binding("420000", 1),
            binding("422800", 2),
            binding("422801", 3)
        ));

        List<DzUserAdRegionVo> result = service(mapper, mock(AdRegionMapper.class), props).queryEffectiveListByUserId(1L);

        assertThat(result).extracting(DzUserAdRegionVo::getAdRegionId)
                          .containsExactly("420000", "422800", "422801");
    }

    @Test
    public void queryEffectiveListByUserIdKeepsHighestLevelBindingsWhenSwitchOn() {
        UserAdRegionProps props = new UserAdRegionProps();
        props.setPreferHighestLevel(true);
        DzUserAdRegionMapper mapper = mock(DzUserAdRegionMapper.class);
        when(mapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            binding("420000", 1),
            binding("429000", 1),
            binding("422800", 2),
            binding("422801", 3)
        ));

        List<DzUserAdRegionVo> result = service(mapper, mock(AdRegionMapper.class), props).queryEffectiveListByUserId(1L);

        assertThat(result).extracting(DzUserAdRegionVo::getAdRegionId)
                          .containsExactly("420000", "429000");
    }

    @Test
    public void resolveCompatibleQueryRegionIdsIncludesCurrentAndAncestors() throws Exception {
        AdRegionMapper adRegionMapper = mock(AdRegionMapper.class);
        when(adRegionMapper.selectById("422801101000")).thenReturn(region("422801101000", "422801000000"));
        when(adRegionMapper.selectById("422801000000")).thenReturn(region("422801000000", "422800000000"));
        when(adRegionMapper.selectById("422800000000")).thenReturn(region("422800000000", "420000000000"));
        when(adRegionMapper.selectById("420000000000")).thenReturn(region("420000000000", null));
        DzUserAdRegionServiceImpl service = service(mock(DzUserAdRegionMapper.class), adRegionMapper, new UserAdRegionProps());

        Method method = DzUserAdRegionServiceImpl.class.getDeclaredMethod("resolveCompatibleQueryRegionIds", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, "422801101000");

        assertThat(result).containsExactly("422801101000", "422801000000", "422800000000", "420000000000");
    }

    @Test
    public void resolveUsersReturnsAllMatchedActiveRoleUsersAndKeepsMissingPhone() {
        DzUserAdRegionMapper mapper = mock(DzUserAdRegionMapper.class);
        AdRegionMapper adRegionMapper = mock(AdRegionMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        AdRegion targetRegion = region("422801000000", "422800000000");
        targetRegion.setLevel(3);
        when(adRegionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(targetRegion);
        when(adRegionMapper.selectById("422801000000")).thenReturn(targetRegion);
        when(adRegionMapper.selectById("422800000000")).thenReturn(region("422800000000", "420000000000"));
        when(adRegionMapper.selectById("420000000000")).thenReturn(region("420000000000", null));
        DzUserAdRegionVo user1Region = binding("422801000000", 3);
        user1Region.setUserId(1L);
        DzUserAdRegionVo user2Region = binding("422800000000", 2);
        user2Region.setUserId(2L);
        when(mapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user1Region, user2Region));
        SysRole role = new SysRole();
        role.setRoleId(9L);
        role.setRoleKey(SysRoleEnum.DZ_FGXIANZHANG.getRoleKey());
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(role);
        SysUserRole userRole1 = new SysUserRole();
        userRole1.setUserId(1L);
        SysUserRole userRole2 = new SysUserRole();
        userRole2.setUserId(2L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole1, userRole2));
        SysUser user1 = user(1L, "审批人A", "13900000001");
        SysUser user2 = user(2L, "审批人B", null);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user1, user2));

        List<DzUserContactVo> result = service(mapper, adRegionMapper, roleMapper, userMapper, userRoleMapper, new UserAdRegionProps())
            .resolveUsers("恩施市", null, null, 3, SysRoleEnum.DZ_FGXIANZHANG.getRoleKey());

        assertThat(result).extracting(DzUserContactVo::getUserId).containsExactly(1L, 2L);
        assertThat(result).extracting(DzUserContactVo::getPhoneNumber).containsExactly("13900000001", null);
    }

    private static DzUserAdRegionServiceImpl service(DzUserAdRegionMapper mapper, AdRegionMapper adRegionMapper, UserAdRegionProps props) {
        return service(
            mapper,
            adRegionMapper,
            mock(SysRoleMapper.class),
            mock(SysUserMapper.class),
            mock(SysUserRoleMapper.class),
            props
        );
    }

    private static DzUserAdRegionServiceImpl service(DzUserAdRegionMapper mapper,
                                                     AdRegionMapper adRegionMapper,
                                                     SysRoleMapper roleMapper,
                                                     SysUserMapper userMapper,
                                                     SysUserRoleMapper userRoleMapper,
                                                     UserAdRegionProps props) {
        return new DzUserAdRegionServiceImpl(
            mapper,
            adRegionMapper,
            roleMapper,
            userMapper,
            userRoleMapper,
            props
        );
    }

    private static DzUserAdRegionVo binding(String adRegionId, Integer level) {
        DzUserAdRegionVo vo = new DzUserAdRegionVo();
        vo.setUserId(1L);
        vo.setAdRegionId(adRegionId);
        vo.setAdRegionLevel(level);
        return vo;
    }

    private static AdRegion region(String id, String pcode) {
        AdRegion region = new AdRegion();
        region.setId(id);
        region.setPcode(pcode);
        return region;
    }

    private static SysUser user(Long userId, String nickName, String phone) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setNickName(nickName);
        user.setUserName("user" + userId);
        user.setPhonenumber(phone);
        return user;
    }
}
