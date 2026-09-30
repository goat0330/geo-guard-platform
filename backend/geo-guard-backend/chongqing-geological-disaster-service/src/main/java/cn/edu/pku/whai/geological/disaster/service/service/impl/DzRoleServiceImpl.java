/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRoleUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RoleUserVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRoleCountVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRoleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRoleService;
import cn.edu.pku.whai.geological.disaster.service.utils.OnlineUtil;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.service.ISysRoleService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DzRoleServiceImpl implements IDzRoleService {
    private final DzRoleMapper dzRoleMapper;
    private final ISysRoleService roleService;

    private static final List<String> DEFAULT_QUERY_ROLE_KEYS = List.of("dz_zj");


    @Override
    public List<StatRoleCountVo> statCount() {
        QueryWrapper<Object> query = Wrappers.query();
        query.eq("r.del_flag", '0');
        query.groupBy("r.role_id, r.role_name, r.role_key");
        return dzRoleMapper.selectRoleCount(query);
    }

    @Override
    public TableDataInfo<RoleUserVo> queryPageList(DzRoleUserBo bo, PageQuery pageQuery) {
        DzRoleUserBo queryBo = bo == null ? new DzRoleUserBo() : bo;
        List<String> roleKeys = resolveRoleKeys(queryBo);
        QueryWrapper<Object> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", '0')
                .in("r.role_key", roleKeys)
                .like(StringUtils.isNotBlank(queryBo.getNickName()), "u.nick_name", queryBo.getNickName())
                .groupBy("u.user_id, u.user_name, u.nick_name, u.email, u.phonenumber, u.status, u.create_time, dee.expert_type, dee.introduction, dee.meeting_count, d.dept_name")
                .orderByAsc("u.user_id");
        Page<RoleUserVo> page = dzRoleMapper.selectRoleUserPage(pageQuery.build(), wrapper);
        if (page.getSize() <= 0) {
            return TableDataInfo.build();
        }
        Set<Long> realtimeOnlineUser = OnlineUtil.getRealtimeOnlineUser(page.getRecords().stream()
                .map(RoleUserVo::getUserId)
                .toList());
        page.getRecords().forEach(vo -> vo.setOnline(realtimeOnlineUser.contains(vo.getUserId()) ? 1 : 0));
        return TableDataInfo.build(page);
    }

    @Override
    public List<SysRoleVo> queryRolesByUserId(Long userId) {
        return roleService.selectRolesByUserId(userId);
    }

    private List<String> resolveRoleKeys(DzRoleUserBo bo) {
        List<String> roleKeys = bo == null ? null : bo.getRoleKeys();
        if (CollUtil.isEmpty(roleKeys)) {
            return DEFAULT_QUERY_ROLE_KEYS;
        }
        List<String> normalized = roleKeys.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        return CollUtil.isEmpty(normalized) ? DEFAULT_QUERY_ROLE_KEYS : normalized;
    }
}
