/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzDefRespStartSmsConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespStartSmsConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzDefRespStartSmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespStartSmsConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespStartSmsConfigService;
import org.dromara.system.domain.SysRole;
import org.dromara.system.mapper.SysRoleMapper;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 启动短信配置 Service业务层处理
 */
@Service
@RequiredArgsConstructor
public class DzDefRespStartSmsConfigServiceImpl implements IDzDefRespStartSmsConfigService {

    private final DzDefRespStartSmsConfigMapper baseMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<DzDefRespStartSmsConfigVo> queryList() {
        return fillRoleMeta(baseMapper.selectVoList(buildQueryWrapper(null)));
    }

    @Override
    public List<DzDefRespStartSmsConfigVo> queryEnabledList() {
        return fillRoleMeta(baseMapper.selectVoList(buildQueryWrapper(1)));
    }

    @Override
    public Boolean updateByBo(DzDefRespStartSmsConfigBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("配置主键不能为空");
        }
        DzDefRespStartSmsConfig exist = baseMapper.selectById(bo.getId());
        if (exist == null || StringUtils.equals(exist.getDelFlag(), "1")) {
            throw new ServiceException("启动短信配置不存在");
        }
        List<String> normalizedRoleKeys = normalizeRoleKeys(bo.getRoleKeyList());
        if (Objects.equals(bo.getStatus(), 1) && normalizedRoleKeys.isEmpty()) {
            throw new ServiceException("启用状态下必须至少配置一个系统角色");
        }
        DzDefRespStartSmsConfig duplicate = baseMapper.selectOne(
            Wrappers.<DzDefRespStartSmsConfig>lambdaQuery()
                    .eq(DzDefRespStartSmsConfig::getBizKey, StringUtils.trim(bo.getBizKey()))
                    .eq(DzDefRespStartSmsConfig::getDelFlag, "0")
                    .ne(DzDefRespStartSmsConfig::getId, bo.getId())
                    .last("limit 1")
        );
        if (duplicate != null) {
            throw new ServiceException("业务角色key已存在");
        }
        validateRoleKeysForUpdate(normalizedRoleKeys);
        DzDefRespStartSmsConfig update = MapstructUtils.convert(bo, DzDefRespStartSmsConfig.class);
        update.setBizKey(StringUtils.trim(bo.getBizKey()));
        update.setBizName(StringUtils.trim(bo.getBizName()));
        update.setRoleKeys(JSONUtil.toJsonStr(normalizedRoleKeys));
        update.setSmsTemplate(StringUtils.trim(bo.getSmsTemplate()));
        update.setRemark(normalizeNullable(bo.getRemark()));
        update.setUpdateBy(LoginHelper.getUserId());
        update.setUpdateTime(new Date());
        update.setDelFlag(exist.getDelFlag());
        update.setCreateBy(exist.getCreateBy());
        update.setCreateTime(exist.getCreateTime());
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public String buildEnabledConfigDigest() {
        List<DzDefRespStartSmsConfigVo> configs = queryEnabledList();
        if (configs.isEmpty()) {
            return "empty";
        }
        String raw = JSONUtil.toJsonStr(configs.stream()
                                               .map(config -> Map.of(
                                                   "id", config.getId(),
                                                   "bizKey", config.getBizKey(),
                                                   "roleKeys", config.getRoleKeyList(),
                                                   "smsTemplate", StringUtils.defaultString(config.getSmsTemplate()),
                                                   "status", config.getStatus(),
                                                   "sort", config.getSort(),
                                                   "updateTime", config.getUpdateTime() == null ? 0L : config.getUpdateTime().getTime()
                                               ))
                                               .toList());
        return Integer.toHexString(raw.hashCode());
    }

    private LambdaQueryWrapper<DzDefRespStartSmsConfig> buildQueryWrapper(Integer status) {
        LambdaQueryWrapper<DzDefRespStartSmsConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(DzDefRespStartSmsConfig::getDelFlag, "0");
        lqw.eq(status != null, DzDefRespStartSmsConfig::getStatus, status);
        lqw.orderByAsc(DzDefRespStartSmsConfig::getSort, DzDefRespStartSmsConfig::getId);
        return lqw;
    }

    private List<DzDefRespStartSmsConfigVo> fillRoleMeta(List<DzDefRespStartSmsConfigVo> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<String> allRoleKeys = list.stream()
                                       .flatMap(item -> parseRoleKeys(item.getRoleKeys()).stream())
                                       .distinct()
                                       .toList();
        Map<String, SysRole> validRoleMap = loadValidRoleMap(allRoleKeys);
        for (DzDefRespStartSmsConfigVo item : list) {
            List<String> roleKeys = parseRoleKeys(item.getRoleKeys()).stream()
                                                                     .filter(validRoleMap::containsKey)
                                                                     .toList();
            item.setRoleKeyList(roleKeys);
            item.setRoleNameList(roleKeys.stream()
                                         .map(validRoleMap::get)
                                         .filter(Objects::nonNull)
                                         .map(SysRole::getRoleName)
                                         .toList());
        }
        return list.stream()
                   .sorted(Comparator.comparing(DzDefRespStartSmsConfigVo::getSort, Comparator.nullsLast(Integer::compareTo))
                                     .thenComparing(DzDefRespStartSmsConfigVo::getId, Comparator.nullsLast(Long::compareTo)))
                   .toList();
    }

    private List<String> parseRoleKeys(String rawRoleKeys) {
        if (StringUtils.isBlank(rawRoleKeys)) {
            return List.of();
        }
        try {
            return normalizeRoleKeys(JSONUtil.toList(JSONUtil.parseArray(rawRoleKeys), String.class));
        } catch (Exception e) {
            throw new ServiceException("启动短信配置中的role_keys不是合法JSON数组");
        }
    }

    private List<String> normalizeRoleKeys(List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String roleKey : roleKeys) {
            String normalized = normalizeNullable(roleKey);
            if (normalized == null || result.contains(normalized)) {
                continue;
            }
            result.add(normalized);
        }
        return result;
    }

    private void validateRoleKeysForUpdate(List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return;
        }
        Map<String, SysRole> validRoleMap = loadValidRoleMap(roleKeys);
        for (String roleKey : roleKeys) {
            if (validRoleMap.containsKey(roleKey)) {
                continue;
            }
            SysRoleEnum roleEnum = SysRoleEnum.getByRoleKey(roleKey);
            String roleName = roleEnum == null ? roleKey : roleEnum.getRoleName();
            throw new ServiceException(roleName + "角色已被删除");
        }
    }

    private Map<String, SysRole> loadValidRoleMap(List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Map.of();
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                                                               .in(SysRole::getRoleKey, roleKeys)
                                                               .eq(SysRole::getDelFlag, "0"));
        if (roles == null || roles.isEmpty()) {
            return Map.of();
        }
        Map<String, SysRole> result = new LinkedHashMap<>();
        roles.stream()
             .filter(Objects::nonNull)
             .filter(role -> StringUtils.isNotBlank(role.getRoleKey()))
             .forEach(role -> result.put(role.getRoleKey(), role));
        return result;
    }

    private String normalizeNullable(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}
