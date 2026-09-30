/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 当前登录用户角色工具类（基于 sys_user_role / sys_role）。
 *
 * <p>同一用户可绑定多个角色；鉴权请使用 roleKey（而非 roleId），优先使用 {@link #hasRole(String)}、
 * {@link #hasAnyRole(Collection)}、{@link #requireCurrentRoleKeys()}，避免误用「单一角色」语义。</p>
 */
public class CurrentRoleUtil {

    private CurrentRoleUtil() {
    }

    /**
     * 当前用户已绑定的角色 ID列表（去重后按 ID 升序，不可变）。
     *
     * @throws ServiceException 未登录或未配置任何角色
     */
    public static List<Long> requireCurrentRoleIds() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }
        return loadRoleIdsForUser(userId);
    }

    /**
     * 当前用户已绑定的角色 key 列表（去重后按字典序，不可变）。
     *
     * @throws ServiceException 未登录、未配置角色，或角色数据缺失
     */
    public static List<String> requireCurrentRoleKeys() {
        List<SysRole> roles = requireCurrentRoles();
        List<String> keys = roles.stream()
                                 .map(SysRole::getRoleKey)
                                 .filter(ObjectUtil::isNotNull)
                                 .map(String::trim)
                                 .filter(s -> !s.isEmpty())
                                 .distinct()
                                 .sorted()
                                 .toList();
        if (CollUtil.isEmpty(keys)) {
            throw new ServiceException("当前用户角色key为空，请检查 sys_role.role_key 配置");
        }
        return List.copyOf(keys);
    }

    /**
     * 当前用户已绑定的角色详情
     *
     * @throws ServiceException 未登录、未配置角色，或 sys_role 中缺失对应记录
     */
    public static List<SysRole> requireCurrentRoles() {
        List<Long> ids = requireCurrentRoleIds();
        SysRoleMapper roleMapper = SpringUtils.getBean(SysRoleMapper.class);
        List<SysRole> rows = roleMapper.selectByIds(ids);
        if (CollUtil.isEmpty(rows) || rows.size() != ids.size()) {
            throw new ServiceException("角色数据不完整，请检查 sys_role 与 sys_user_role 配置");
        }
        Map<Long, SysRole> byId = rows.stream().collect(Collectors.toMap(SysRole::getRoleId, r -> r, (a, b) -> a, LinkedHashMap::new));
        List<SysRole> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            SysRole r = byId.get(id);
            if (r == null) {
                throw new ServiceException("角色不存在，roleId=" + id);
            }
            ordered.add(r);
        }
        return List.copyOf(ordered);
    }

    /**
     * 是否拥有指定角色（按 roleKey 判断）。
     */
    public static boolean hasRole(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return false;
        }
        return requireCurrentRoleKeys().contains(roleKey.trim());
    }

    /**
     * 是否拥有任意一个给定角色（按 roleKey 判断）。
     */
    public static boolean hasAnyRole(Collection<String> roleKeys) {
        if (CollUtil.isEmpty(roleKeys)) {
            return false;
        }
        List<String> current = requireCurrentRoleKeys();
        return current.stream().anyMatch(roleKeys::contains);
    }

    /**
     * 是否拥有任意一个给定角色（便捷重载，按 roleKey 判断）。
     */
    public static boolean hasAnyRole(String... roleKeys) {
        if (roleKeys == null || roleKeys.length == 0) {
            return false;
        }
        return hasAnyRole(Arrays.stream(roleKeys)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList());
    }

    /**
     * 是否拥有指定角色（按 roleId 判断）。
     *
     * @deprecated 鉴权请改用 {@link #hasRole(String)}（roleKey）。
     */
    @Deprecated
    public static boolean hasRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        return requireCurrentRoleIds().contains(roleId);
    }

    /**
     * 是否拥有任意一个给定角色（按 roleId 判断）。
     *
     * @deprecated 鉴权请改用 {@link #hasAnyRole(Collection)}（roleKey）。
     */
    @Deprecated
    public static boolean hasAnyRoleById(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        List<Long> current = requireCurrentRoleIds();
        return current.stream().anyMatch(roleIds::contains);
    }

    /**
     * 是否拥有任意一个给定角色（便捷重载，按 roleId 判断）。
     *
     * @deprecated 鉴权请改用 {@link #hasAnyRole(String...)}（roleKey）。
     */
    @Deprecated
    public static boolean hasAnyRole(Long... roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            return false;
        }
        return hasAnyRoleById(Arrays.stream(roleIds).filter(Objects::nonNull).toList());
    }

    /**
     * 获取当前用户「唯一」角色 ID。
     *
     * @deprecated 多角色用户请改用 {@link #requireCurrentRoleIds()} 或 {@link #hasRole(Long)}。
     */
    @Deprecated
    public static Long requireCurrentRoleId() {
        List<Long> ids = requireCurrentRoleIds();
        if (ids.size() != 1) {
            throw new ServiceException("当前用户存在多个角色，请使用 requireCurrentRoleIds、hasRole、hasAnyRole 等方法进行鉴权");
        }
        return ids.getFirst();
    }

    /**
     * 获取当前用户「唯一」角色详情。
     *
     * @deprecated 多角色用户请改用 {@link #requireCurrentRoles()}。
     */
    @Deprecated
    public static SysRole requireCurrentRole() {
        Long roleId = requireCurrentRoleId();
        SysRoleMapper roleMapper = SpringUtils.getBean(SysRoleMapper.class);
        SysRole role = roleMapper.selectById(roleId);
        if (ObjectUtil.isNull(role)) {
            throw new ServiceException("角色不存在，roleId=" + roleId);
        }
        return role;
    }

    private static List<Long> loadRoleIdsForUser(Long userId) {
        SysUserRoleMapper userRoleMapper = SpringUtils.getBean(SysUserRoleMapper.class);
        List<SysUserRole> list = userRoleMapper.selectList(
            Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId)
        );
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException("当前用户未配置角色");
        }
        return list.stream()
                   .map(SysUserRole::getRoleId)
                   .filter(Objects::nonNull)
                   .distinct()
                   .sorted()
                   .toList();
    }
}
