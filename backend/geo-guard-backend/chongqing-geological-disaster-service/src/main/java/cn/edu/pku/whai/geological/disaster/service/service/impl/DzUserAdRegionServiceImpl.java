/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzUserAdRegion;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzUserAdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.props.UserAdRegionProps;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户行政区划关联Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class DzUserAdRegionServiceImpl implements IDzUserAdRegionService {

    private final DzUserAdRegionMapper baseMapper;
    private final AdRegionMapper adRegionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserAdRegionProps userAdRegionProps;

    @Override
    public DzUserAdRegionVo queryByUserId(Long userId) {
        return baseMapper.selectVoOne(buildUserWrapper(userId));
    }

    @Override
    public List<DzUserAdRegionVo> queryListByUserId(Long userId) {
        return baseMapper.selectVoList(buildUserListWrapper(userId));
    }

    @Override
    public List<DzUserAdRegionVo> queryEffectiveListByUserId(Long userId) {
        return resolveEffectiveRegions(queryListByUserId(userId));
    }

    @Override
    public TableDataInfo<DzUserAdRegionVo> queryPageList(DzUserAdRegionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzUserAdRegion> lqw = buildQueryWrapper(bo);
        Page<DzUserAdRegionVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<DzUserAdRegionStatVo> statRegionPage(DzUserAdRegionBo bo, PageQuery pageQuery) {
        fillCompatibleQueryRegionIds(bo);
        Page<DzUserAdRegionStatVo> page = resolveStatPage(bo, pageQuery);
        Page<DzUserAdRegionStatVo> result = baseMapper.selectStatRegionPage(page, bo);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzUserAdRegionVo> queryList(DzUserAdRegionBo bo) {
        LambdaQueryWrapper<DzUserAdRegion> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzUserAdRegion> buildQueryWrapper(DzUserAdRegionBo bo) {
        LambdaQueryWrapper<DzUserAdRegion> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, DzUserAdRegion::getUserId, bo.getUserId());
        lqw.in(ObjUtil.isNotEmpty(bo.getUserIds()), DzUserAdRegion::getUserId, bo.getUserIds());
        fillCompatibleQueryRegionIds(bo);
        List<String> compatibleRegionIds = bo.getCompatibleAdRegionIds() == null ? Collections.emptyList() : bo.getCompatibleAdRegionIds();
        lqw.in(!compatibleRegionIds.isEmpty(), DzUserAdRegion::getAdRegionId, compatibleRegionIds);
        lqw.like(StringUtils.isNotBlank(bo.getAdRegionName()), DzUserAdRegion::getAdRegionName, bo.getAdRegionName());
        lqw.eq(bo.getAdRegionLevel() != null, DzUserAdRegion::getAdRegionLevel, bo.getAdRegionLevel());
        lqw.orderByDesc(DzUserAdRegion::getUpdateTime, DzUserAdRegion::getCreateTime);
        return lqw;
    }

    private void fillCompatibleQueryRegionIds(DzUserAdRegionBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getAdRegionId())) {
            return;
        }
        bo.setCompatibleAdRegionIds(resolveCompatibleQueryRegionIds(bo.getAdRegionId()));
    }

    private List<DzUserAdRegionVo> resolveEffectiveRegions(List<DzUserAdRegionVo> regions) {
        if (!Boolean.TRUE.equals(userAdRegionProps.getPreferHighestLevel()) || regions == null || regions.size() <= 1) {
            return regions;
        }
        Integer highestLevel = regions.stream()
                                      .filter(Objects::nonNull)
                                      .map(DzUserAdRegionVo::getAdRegionLevel)
                                      .filter(Objects::nonNull)
                                      .min(Integer::compareTo)
                                      .orElse(null);
        if (highestLevel == null) {
            return regions;
        }
        return regions.stream()
                      .filter(Objects::nonNull)
                      .filter(region -> Objects.equals(region.getAdRegionLevel(), highestLevel))
                      .toList();
    }

    private List<String> resolveCompatibleQueryRegionIds(String adRegionId) {
        if (StringUtils.isBlank(adRegionId)) {
            return Collections.emptyList();
        }
        List<String> regionIds = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String currentId = adRegionId;
        while (StringUtils.isNotBlank(currentId) && visited.add(currentId)) {
            regionIds.add(currentId);
            AdRegion currentRegion = adRegionMapper.selectById(currentId);
            if (currentRegion == null || StringUtils.isBlank(currentRegion.getPcode())) {
                break;
            }
            currentId = currentRegion.getPcode();
        }
        return regionIds;
    }

    private Page<DzUserAdRegionStatVo> resolveStatPage(DzUserAdRegionBo bo, PageQuery pageQuery) {
        Integer limit = bo == null ? null : bo.getLimit();
        if (limit != null && limit > 0) {
            return new Page<>(1, limit);
        }
        return pageQuery.build();
    }

    private LambdaQueryWrapper<DzUserAdRegion> buildUserWrapper(Long userId) {
        return buildUserListWrapper(userId).last("limit 1");
    }

    private LambdaQueryWrapper<DzUserAdRegion> buildUserListWrapper(Long userId) {
        return Wrappers.<DzUserAdRegion>lambdaQuery()
                       .eq(DzUserAdRegion::getUserId, userId)
                       .orderByDesc(DzUserAdRegion::getUpdateTime, DzUserAdRegion::getCreateTime);
    }

    private LambdaQueryWrapper<DzUserAdRegion> buildUserRegionWrapper(Long userId, String adRegionId) {
        return Wrappers.<DzUserAdRegion>lambdaQuery()
                       .eq(DzUserAdRegion::getUserId, userId)
                       .eq(DzUserAdRegion::getAdRegionId, adRegionId)
                       .last("limit 1");
    }

    @Override
    public Boolean insertByBo(DzUserAdRegionBo bo) {
        DzUserAdRegion add = MapstructUtils.convert(bo, DzUserAdRegion.class);
        fillAdRegionInfo(add);
        validEntityBeforeSave(add, false);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(DzUserAdRegionBo bo) {
        DzUserAdRegion update = MapstructUtils.convert(bo, DzUserAdRegion.class);
        DzUserAdRegion exist = baseMapper.selectOne(buildUserWrapper(update.getUserId()));
        if (exist == null) {
            throw new ServiceException("用户行政区划关联不存在");
        }
        fillAdRegionInfo(update);
        validEntityBeforeSave(update, true);
        return baseMapper.updateById(update) > 0;
    }

    private void fillAdRegionInfo(DzUserAdRegion entity) {
        AdRegion adRegion = adRegionMapper.selectById(entity.getAdRegionId());
        if (adRegion == null) {
            throw new ServiceException("行政区划不存在");
        }
        entity.setAdRegionName(adRegion.getName());
        entity.setAdRegionLevel(adRegion.getLevel());
    }

    private void validEntityBeforeSave(DzUserAdRegion entity, boolean isUpdate) {
        if (entity.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (StringUtils.isBlank(entity.getAdRegionId())) {
            throw new ServiceException("行政区划ID不能为空");
        }
        if (StringUtils.isBlank(entity.getAdRegionName())) {
            throw new ServiceException("行政区划名称不能为空");
        }
        if (entity.getAdRegionLevel() == null) {
            throw new ServiceException("行政区划层级不能为空");
        }
        DzUserAdRegion duplicate = baseMapper.selectOne(buildUserRegionWrapper(entity.getUserId(), entity.getAdRegionId()));
        if (!isUpdate && duplicate != null) {
            throw new ServiceException("当前用户已绑定该行政区划");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (Boolean.TRUE.equals(isValid) && (ids == null || ids.isEmpty())) {
            throw new ServiceException("用户ID不能为空");
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<DzUserContactVo> resolveUsers(String county, String street, String village, Integer level, String roleKey) {
        LambdaQueryWrapper<AdRegion> lqw = Wrappers.lambdaQuery();
        if (level == null) {
            throw new ServiceException("请指定正确的街镇层级");
        }
        if (StringUtils.isBlank(roleKey)) {
            throw new ServiceException("角色标识不能为空");
        }
        lqw.eq(AdRegion::getLevel, level);
        lqw.eq(StringUtils.isNotBlank(county) && level >= 3, AdRegion::getCounty, county);
        lqw.eq(StringUtils.isNotBlank(street) && level >= 4, AdRegion::getStreet, street);
        lqw.eq(StringUtils.isNotBlank(village) && level >= 5, AdRegion::getVillage, village);
        AdRegion adRegion = adRegionMapper.selectOne(lqw);
        if (adRegion == null) {
            throw new ServiceException("未匹配到对应行政区划");
        }
        DzUserAdRegionBo dzUserAdRegionBo = new DzUserAdRegionBo();
        dzUserAdRegionBo.setAdRegionId(adRegion.getId());
        List<DzUserAdRegionVo> dzUserAdRegionVos = queryList(dzUserAdRegionBo);
        List<Long> adRegionUserIds = dzUserAdRegionVos.stream()
                                                      .map(DzUserAdRegionVo::getUserId)
                                                      .filter(Objects::nonNull)
                                                      .distinct()
                                                      .toList();
        if (adRegionUserIds.isEmpty()) {
            return List.of();
        }
        SysRoleEnum targetRoleEnum = SysRoleEnum.getByRoleKey(roleKey);
        if (targetRoleEnum == null) {
            throw new ServiceException("未匹配到指定角色");
        }
        SysRole targetRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                                                             .eq(SysRole::getRoleKey, targetRoleEnum.getRoleKey())
                                                             .eq(SysRole::getDelFlag, "0")
                                                             .eq(SysRole::getStatus, "0")
                                                             .last("limit 1"));
        if (targetRole == null || targetRole.getRoleId() == null) {
            throw new ServiceException("系统中不存在有效角色配置");
        }
        List<SysUserRole> sysUserRoles = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                                                                              .eq(SysUserRole::getRoleId, targetRole.getRoleId())
                                                                              .in(SysUserRole::getUserId, adRegionUserIds));
        Set<Long> targetUserIdSet = sysUserRoles.stream()
                                                .map(SysUserRole::getUserId)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toSet());
        List<Long> orderedTargetUserIds = adRegionUserIds.stream()
                                                         .filter(targetUserIdSet::contains)
                                                         .toList();
        if (orderedTargetUserIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> sysUsers = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                                                                  .in(SysUser::getUserId, orderedTargetUserIds)
                                                                  .eq(SysUser::getDelFlag, "0")
                                                                  .eq(SysUser::getStatus, "0"));
        Map<Long, SysUser> sysUserMap = sysUsers.stream()
                                                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (a, b) -> a));
        return orderedTargetUserIds.stream()
                                   .map(sysUserMap::get)
                                   .filter(Objects::nonNull)
                                   .map(sysUser -> {
                                       String userName = StringUtils.isNotBlank(sysUser.getNickName()) ? sysUser.getNickName() : sysUser.getUserName();
                                       return new DzUserContactVo(sysUser.getUserId(), userName, sysUser.getPhonenumber());
                                   })
                                   .toList();
    }
}
