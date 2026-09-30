/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserService;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 地灾项目用户Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class DzUserServiceImpl implements IDzUserService {

    private final ISysUserService userService;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final AdRegionMapper adRegionMapper;

    @Override
    public TableDataInfo<DzUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        TableDataInfo<SysUserVo> userTable = userService.selectPageUserList(user, pageQuery);
        List<SysUserVo> sysUsers = userTable.getData();
        if (CollUtil.isEmpty(sysUsers)) {
            return new TableDataInfo<>(Collections.emptyList(), userTable.getTotal());
        }
        DzUserAdRegionBo bo = new DzUserAdRegionBo();
        bo.setUserIds(sysUsers.stream()
            .map(SysUserVo::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        Map<Long, List<DzUserAdRegionVo>> userAdRegionMap = dzUserAdRegionService.queryList(bo).stream()
            .filter(item -> item.getUserId() != null)
            .collect(Collectors.groupingBy(DzUserAdRegionVo::getUserId));
        List<DzUserVo> dzUsers = sysUsers.stream()
            .map(item -> buildDzUserVo(item, userAdRegionMap))
            .toList();
        return new TableDataInfo<>(dzUsers, userTable.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long insertUser(DzUserBo user) {
        long userId = userService.insertUser(toSysUserBo(user));
        user.setUserId(userId);
        replaceUserAdRegions(user);
        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(DzUserBo user) {
        int rows = userService.updateUser(toSysUserBo(user));
        replaceUserAdRegions(user);
        return rows;
    }

    private DzUserVo buildDzUserVo(SysUserVo source, Map<Long, List<DzUserAdRegionVo>> userAdRegionMap) {
        DzUserVo target = new DzUserVo();
        BeanUtil.copyProperties(source, target);
        target.setUserAdRegions(userAdRegionMap.getOrDefault(source.getUserId(), Collections.emptyList()));
        return target;
    }

    private SysUserBo toSysUserBo(DzUserBo source) {
        SysUserBo target = new SysUserBo();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private void replaceUserAdRegions(DzUserBo user) {
        if (user.getUserId() == null || user.getUserAdRegions() == null) {
            return;
        }
        dzUserAdRegionService.deleteWithValidByIds(List.of(user.getUserId()), false);

        for (DzUserAdRegionBo userAdRegion : user.getUserAdRegions()) {
            AdRegion adRegion = adRegionMapper.selectById(userAdRegion.getAdRegionId());
            userAdRegion.setUserId(user.getUserId());
            userAdRegion.setAdRegionId(userAdRegion.getAdRegionId());
            userAdRegion.setAdRegionName(adRegion.getName());
            userAdRegion.setAdRegionLevel(adRegion.getLevel());
            dzUserAdRegionService.insertByBo(userAdRegion);
        }
    }

}

