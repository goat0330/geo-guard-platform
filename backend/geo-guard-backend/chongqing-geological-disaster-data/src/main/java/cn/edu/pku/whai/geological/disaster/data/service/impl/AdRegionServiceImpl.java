/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionStatBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HouseSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.po.PersonSlope;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.HouseSlopeMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.PersonSlopeMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行政区划Service业务层处理
 *
 * @author kongweiguang
 * @date 2025-12-24
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AdRegionServiceImpl implements IAdRegionService {

    private static final String ROOT_REGION_ID = "420000";

    private final AdRegionMapper baseMapper;
    private final SlopeUnitMapper slopeUnitMapper;
    private final PersonSlopeMapper personSlopeMapper;
    private final HouseSlopeMapper houseSlopeMapper;

    @Value("${dizai.user-ad-region.prefer-highest-level:false}")
    private Boolean preferHighestLevelUserAdRegion;

    @Override
    public List<Tree<String>> selectTree(Boolean withWkt, Boolean skipAuth) {
        List<AdRegionVo> regions = baseMapper.selectTreeList(withWkt);
        List<AdRegionVo> visibleRegions = Boolean.TRUE.equals(skipAuth)
            ? regions
            : filterCurrentUserVisibleRegions(regions);
        return buildRegionTree(visibleRegions, Boolean.TRUE.equals(withWkt));
    }

    /**
     * 分页查询行政区划列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 行政区划分页列表
     */
    @Override
    public TableDataInfo<AdRegionVo> queryPageList(AdRegionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AdRegion> lqw = buildQueryWrapper(bo);
        Page<AdRegionVo> result = baseMapper.selectVoPage1(pageQuery.build(), lqw, bo.getSimpWktLevel());
        return TableDataInfo.build(result);
    }


    private LambdaQueryWrapper<AdRegion> buildQueryWrapper(AdRegionBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<AdRegion> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getId()), AdRegion::getId, bo.getId());
        lqw.eq(StringUtils.isNotBlank(bo.getPcode()), AdRegion::getPcode, bo.getPcode());
        lqw.like(StringUtils.isNotBlank(bo.getName()), AdRegion::getName, bo.getName());
        lqw.eq(bo.getLevel() != null, AdRegion::getLevel, bo.getLevel());
        lqw.like(StringUtils.isNotBlank(bo.getLevelName()), AdRegion::getLevelName, bo.getLevelName());
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), AdRegion::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), AdRegion::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), AdRegion::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), AdRegion::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), AdRegion::getVillage, bo.getVillage());
        lqw.eq(StringUtils.isNotBlank(bo.getCommunity()), AdRegion::getCommunity, bo.getCommunity());
        lqw.eq(StringUtils.isNotBlank(bo.getCenter()), AdRegion::getCenter, bo.getCenter());
        lqw.eq(StringUtils.isNotBlank(bo.getWkt()), AdRegion::getWkt, bo.getWkt());
        lqw.eq(bo.getArea() != null, AdRegion::getArea, bo.getArea());
        return lqw;
    }


    /**
     * 统计行政区划数据
     *
     * @param bo 统计条件
     * @return 统计数据
     */
    @Override
    public AdRegionStatVo stat(AdRegionStatBo bo) {
        AdRegionStatVo vo = new AdRegionStatVo();
        String id = bo.getId();
        AdRegion adRegion = baseMapper.selectById(id);
        if (adRegion == null) {
            throw new ServiceException("行政区划不存在");
        }

        // 设置地理范围
        vo.setGeoScope(adRegion.getArea());

        // 计算人口底数
        int population = calculatePopulation(adRegion, bo);
        vo.setPopulation(population);

        // 计算建筑数量
        int buildingNum = calculateBuildingNum(adRegion, bo);
        vo.setBuildingNum(buildingNum);

        // 计算边坡单元数量
        Long slopeUnitNum = calculateSlopeUnitNum(adRegion, bo);
        vo.setSlopeUnitNum(slopeUnitNum);

        return vo;
    }

    /**
     * 计算人口底数
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 人口数量
     */
    private int calculatePopulation(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<PersonSlope> personFilter = buildPersonSlopeFilter(adRegion, bo);
        Long population = personSlopeMapper.selectCount(personFilter);
        return Math.toIntExact(population);
    }

    /**
     * 计算建筑数量（根据building_code去重）
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 建筑数量
     */
    private int calculateBuildingNum(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<HouseSlope> houseFilter = buildHouseSlopeFilter(adRegion, bo);
        // 根据building_code去重后统计建筑数量
        Long buildingNum = houseSlopeMapper.countDistinctBuildingCode(houseFilter);
        return buildingNum != null ? Math.toIntExact(buildingNum) : 0;
    }

    /**
     * 计算边坡单元数量
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 边坡单元数量
     */
    private Long calculateSlopeUnitNum(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<SlopeUnit> lqw = buildSlopeUnitFilter(adRegion, bo);
        return slopeUnitMapper.selectCount(lqw);
    }

    /**
     * 构建人员边坡查询条件
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<PersonSlope> buildPersonSlopeFilter(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<PersonSlope> filter = Wrappers.lambdaQuery();
        applyRegionFilter(filter, adRegion,
            PersonSlope::getSlopeUnitProvince,
            PersonSlope::getSlopeUnitCity,
            PersonSlope::getSlopeUnitCounty,
            PersonSlope::getSlopeUnitStreet,
            PersonSlope::getSlopeUnitVillage,
            PersonSlope::getSlopeUnitCommunity);
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea1()), PersonSlope::getPilotArea1, bo.getPilotArea1());
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea2()), PersonSlope::getPilotArea2, bo.getPilotArea2());
        return filter;
    }

    /**
     * 构建房屋边坡查询条件
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<HouseSlope> buildHouseSlopeFilter(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<HouseSlope> filter = Wrappers.lambdaQuery();
        applyRegionFilter(filter, adRegion,
            HouseSlope::getSlopeUnitProvince,
            HouseSlope::getSlopeUnitCity,
            HouseSlope::getSlopeUnitCounty,
            HouseSlope::getSlopeUnitStreet,
            HouseSlope::getSlopeUnitVillage,
            HouseSlope::getSlopeUnitCommunity);
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea1()), HouseSlope::getPilotArea1, bo.getPilotArea1());
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea2()), HouseSlope::getPilotArea2, bo.getPilotArea2());
        return filter;
    }

    /**
     * 构建边坡单元查询条件
     *
     * @param adRegion 行政区划
     * @param bo       统计条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<SlopeUnit> buildSlopeUnitFilter(AdRegion adRegion, AdRegionStatBo bo) {
        LambdaQueryWrapper<SlopeUnit> filter = Wrappers.lambdaQuery();
        applyRegionFilter(filter, adRegion,
            SlopeUnit::getProvince,
            SlopeUnit::getCity,
            SlopeUnit::getCounty,
            SlopeUnit::getStreet,
            SlopeUnit::getVillage,
            SlopeUnit::getCommunity);
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea1()), SlopeUnit::getPilotArea1, bo.getPilotArea1());
        filter.eq(ObjUtil.isNotNull(bo.getPilotArea2()), SlopeUnit::getPilotArea2, bo.getPilotArea2());
        return filter;
    }

    /**
     * 应用区域级别过滤条件（通用方法，消除代码重复）
     *
     * @param filter    查询条件
     * @param adRegion  行政区划
     * @param province  省份字段引用
     * @param city      城市字段引用
     * @param county    区县字段引用
     * @param street    街道字段引用
     * @param village   村字段引用
     * @param community 社区字段引用
     */
    private <T> void applyRegionFilter(
        LambdaQueryWrapper<T> filter,
        AdRegion adRegion,
        SFunction<T, ?> province,
        SFunction<T, ?> city,
        SFunction<T, ?> county,
        SFunction<T, ?> street,
        SFunction<T, ?> village,
        SFunction<T, ?> community) {
        String name = adRegion.getName();
        if (StringUtils.equals(name, adRegion.getProvince())) {
            filter.eq(province, name);
        } else if (StringUtils.equals(name, adRegion.getCity())) {
            filter.eq(city, name);
        } else if (StringUtils.equals(name, adRegion.getCounty())) {
            filter.eq(county, name);
        } else if (StringUtils.equals(name, adRegion.getStreet())) {
            filter.eq(street, name);
        } else if (StringUtils.equals(name, adRegion.getVillage())) {
            filter.eq(village, name);
        } else if (StringUtils.equals(name, adRegion.getCommunity())) {
            filter.eq(community, name);
        }
    }

    @Override
    public AdRegionVo getInfo(String id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<AdRegionVo> queryTownBasicInfoByNames(List<String> townNames) {
        List<String> normalizedTownNames = normalizeRegionNames(townNames);
        if (normalizedTownNames.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AdRegion> filter = Wrappers.lambdaQuery();
        filter.in(AdRegion::getName, normalizedTownNames);
        filter.eq(AdRegion::getLevel, 4);
        return baseMapper.selectVoList(filter);
    }

    @Override
    public List<AdRegionVo> querySlopeUnitListByStreets(List<String> streets) {
        LambdaQueryWrapper<AdRegion> filter = Wrappers.lambdaQuery();
        filter.in(AdRegion::getStreet, streets);
        filter.eq(AdRegion::getLevel, 4);
        return baseMapper.selectVoList(filter);
    }

    @Override
    public AdRegionVo getUserRegion(Long userId) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        return baseMapper.getUserRegion(userId);
    }

    @Override
    public List<AdRegionVo> queryList(AdRegionBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private List<String> normalizeRegionNames(List<String> regionNames) {
        if (regionNames == null || regionNames.isEmpty()) {
            return List.of();
        }
        return regionNames.stream()
                          .filter(StringUtils::isNotBlank)
                          .map(String::trim)
                          .distinct()
                          .toList();
    }

    private List<Tree<String>> buildRegionTree(List<AdRegionVo> regions, boolean withWkt) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }

        TreeNodeConfig config = new TreeNodeConfig();
        config.setWeightKey("level");
        config.setChildrenKey("children");
        config.setIdKey("id");
        config.setParentIdKey("pcode");

        List<Tree<String>> trees = TreeUtil.build(regions, ROOT_REGION_ID, config, (region, tree) -> {
            String id = ObjUtil.toString(region.getId());
            tree.setId(id);
            tree.setParentId(region.getPcode());
            tree.setName(region.getName());
            tree.putExtra("level", region.getLevel());
            tree.putExtra("level_name", region.getLevelName());
            tree.putExtra("province", region.getProvince());
            tree.putExtra("city", region.getCity());
            tree.putExtra("county", region.getCounty());
            tree.putExtra("street", region.getStreet());
            tree.putExtra("village", region.getVillage());
            tree.putExtra("community", region.getCommunity());
            tree.putExtra("center", region.getCenter());
            tree.putExtra("area", region.getArea());
            if (withWkt) {
                tree.putExtra("wkt", region.getWkt());
            }
        });
        return trees == null ? List.of() : trees;
    }

    private List<AdRegionVo> filterCurrentUserVisibleRegions(List<AdRegionVo> regions) {
        if (regions == null || regions.isEmpty() || LoginHelper.isSuperAdmin()) {
            return regions;
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }
        List<AdRegionVo> userRegions = resolveEffectiveUserRegions(baseMapper.selectUserRegionList(userId));
        if (userRegions == null || userRegions.isEmpty()) {
            throw new ServiceException("当前用户未配置行政区划，无法访问行政区划树");
        }
        Set<String> visibleIds = collectVisibleRegionIds(regions, userRegions);
        return regions.stream()
                      .filter(region -> visibleIds.contains(region.getId()))
                      .toList();
    }

    private Set<String> collectVisibleRegionIds(List<AdRegionVo> regions, List<AdRegionVo> userRegions) {
        Map<String, AdRegionVo> regionMap = regions.stream()
                                                   .filter(Objects::nonNull)
                                                   .filter(region -> StringUtils.isNotBlank(region.getId()))
                                                   .collect(Collectors.toMap(AdRegionVo::getId, region -> region, (left, right) -> left, HashMap::new));
        Map<String, List<AdRegionVo>> childrenMap = new HashMap<>();
        for (AdRegionVo region : regions) {
            if (region == null || StringUtils.isBlank(region.getPcode())) {
                continue;
            }
            childrenMap.computeIfAbsent(region.getPcode(), key -> new ArrayList<>()).add(region);
        }
        Set<String> visibleIds = new HashSet<>();
        for (AdRegionVo userRegion : userRegions) {
            if (userRegion == null || StringUtils.isBlank(userRegion.getId())) {
                continue;
            }
            appendAncestors(regionMap, visibleIds, userRegion.getId());
            appendDescendants(childrenMap, visibleIds, userRegion.getId());
        }
        return visibleIds;
    }

    private List<AdRegionVo> resolveEffectiveUserRegions(List<AdRegionVo> userRegions) {
        if (!Boolean.TRUE.equals(preferHighestLevelUserAdRegion) || userRegions == null || userRegions.size() <= 1) {
            return userRegions;
        }
        Integer highestLevel = userRegions.stream()
                                          .filter(Objects::nonNull)
                                          .map(AdRegionVo::getLevel)
                                          .filter(Objects::nonNull)
                                          .min(Integer::compareTo)
                                          .orElse(null);
        if (highestLevel == null) {
            return userRegions;
        }
        return userRegions.stream()
                          .filter(Objects::nonNull)
                          .filter(region -> Objects.equals(region.getLevel(), highestLevel))
                          .toList();
    }

    private void appendAncestors(Map<String, AdRegionVo> regionMap, Set<String> visibleIds, String regionId) {
        String currentId = regionId;
        while (StringUtils.isNotBlank(currentId) && visibleIds.add(currentId)) {
            AdRegionVo current = regionMap.get(currentId);
            if (current == null || StringUtils.isBlank(current.getPcode())) {
                break;
            }
            currentId = current.getPcode();
        }
    }

    private void appendDescendants(Map<String, List<AdRegionVo>> childrenMap, Set<String> visibleIds, String regionId) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(regionId);
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (!visited.add(currentId)) {
                continue;
            }
            visibleIds.add(currentId);
            for (AdRegionVo child : childrenMap.getOrDefault(currentId, List.of())) {
                if (child != null && StringUtils.isNotBlank(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
    }


}
