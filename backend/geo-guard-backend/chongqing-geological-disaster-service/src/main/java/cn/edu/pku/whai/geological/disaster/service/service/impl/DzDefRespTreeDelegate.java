/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespTreeDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IAdRegionService adRegionService;

    DzDefRespTreeDelegate(DzDefRespPlanServiceImpl service, IAdRegionService adRegionService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.adRegionService = adRegionService;
    }

    private AdRegionVo requireCurrentUserDefRespPlanRegion() { return service.requireCurrentUserDefRespPlanRegion(); }

    private boolean isCountyRegionPlan(DefRespPlan plan) { return service.isCountyRegionPlan(plan); }

    private boolean isCountyRegionPlan(Integer type, Integer regionScopeType) { return service.isCountyRegionPlan(type, regionScopeType); }

    private boolean isTownRegionPlan(DefRespPlan plan) { return service.isTownRegionPlan(plan); }

    private List<String> splitStreets(String streets) { return DzDefRespPlanServiceImpl.splitStreets(streets); }
    public List<Tree<String>> getTree() {
        AdRegionVo currentUserRegion = requireCurrentUserDefRespPlanRegion();
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.lambdaQuery();
        lqw.select(DefRespPlan::getType, DefRespPlan::getStatus, DefRespPlan::getRegId, DefRespPlan::getRegionScopeType,
            DefRespPlan::getCounty, DefRespPlan::getStreets, DefRespPlan::getHandleId, DefRespPlan::getDeleted);
        lqw.eq(DefRespPlan::getDeleted, 0);
        lqw.and(wrapper -> wrapper.isNull(DefRespPlan::getRegionScopeType)
                                  .or()
                                  .ne(DefRespPlan::getRegionScopeType, 2));
        appendCurrentUserDefRespPlanTreePermission(lqw, currentUserRegion);
        List<DefRespPlan> list = filterCurrentUserVisibleTreePlans(baseMapper.selectList(lqw), currentUserRegion);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, List<DefRespPlan>> byType = list.stream()
                                                     .filter(e -> e.getType() != null)
                                                     .collect(Collectors.groupingBy(DefRespPlan::getType));
        List<Tree<String>> roots = new ArrayList<>();
        for (Map.Entry<Integer, List<DefRespPlan>> typeEntry : byType.entrySet()) {
            Integer type = typeEntry.getKey();
            List<DefRespPlan> typeList = typeEntry.getValue();
            long countedSize = countTreeVisiblePlans(typeList);
            if (countedSize <= 0) {
                continue;
            }
            Tree<String> typeNode = new Tree<>();
            typeNode.setId("type-" + type);
            typeNode.setParentId("0");
            typeNode.setName(String.valueOf(type));
            typeNode.putExtra("type", type);
            typeNode.putExtra("count", countedSize);
            List<Tree<String>> statusChildren = new ArrayList<>();
            statusChildren.add(buildStatusGroupNode(type, typeList, "1-5", e -> DefRespPlanStatusEnum.isStarted(e.getStatus())));
            statusChildren.add(buildStatusGroupNode(type, typeList, "6", e -> DefRespPlanStatusEnum.ENDED.getCode().equals(e.getStatus())));
            typeNode.setChildren(statusChildren);
            roots.add(typeNode);
        }
        roots.sort((a, b) -> {
            Integer ta = (Integer) a.get("type");
            Integer tb = (Integer) b.get("type");
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return ta.compareTo(tb);
        });
        return roots;
    }

    /**
     * 追加树查询用户权限条件
     */
    void appendCurrentUserDefRespPlanTreePermission(LambdaQueryWrapper<DefRespPlan> lqw, AdRegionVo adRegionVo) {
        if (adRegionVo == null) {
            return;
        }
        Set<String> visibleCounties = resolveTreeVisibleCounties(adRegionVo);
        if (!visibleCounties.isEmpty()) {
            lqw.in(DefRespPlan::getCounty, visibleCounties);
        } else {
            String currentCounty = resolveAdRegionCountyName(adRegionVo);
            if (StringUtils.isNotBlank(currentCounty)) {
                lqw.eq(DefRespPlan::getCounty, currentCounty);
            }
        }
    }

    /**
     * 过滤当前用户可见的树节点方案
     */
    List<DefRespPlan> filterCurrentUserVisibleTreePlans(List<DefRespPlan> plans, AdRegionVo adRegionVo) {
        if (plans == null || plans.isEmpty() || adRegionVo == null) {
            return plans;
        }
        Set<String> visibleCounties = resolveTreeVisibleCounties(adRegionVo);
        return plans.stream()
                    .filter(Objects::nonNull)
                    .filter(plan -> canCurrentUserViewTreePlan(plan, adRegionVo, visibleCounties))
                    .toList();
    }

    /**
     * 判断用户是否可见树中方案
     */
    boolean canCurrentUserViewTreePlan(DefRespPlan plan, AdRegionVo adRegionVo, Set<String> visibleCounties) {
        if (plan == null) {
            return false;
        }
        if (!visibleCounties.isEmpty() && !visibleCounties.contains(plan.getCounty())) {
            return false;
        }
        String currentCounty = resolveAdRegionCountyName(adRegionVo);
        if (visibleCounties.isEmpty() && StringUtils.isNotBlank(currentCounty) && !StringUtils.equals(currentCounty, plan.getCounty())) {
            return false;
        }
        if (!isStreetLevelTreeRegion(adRegionVo)) {
            return true;
        }
        String currentStreet = resolveAdRegionStreetName(adRegionVo);
        if (StringUtils.isBlank(currentStreet)) {
            return false;
        }
        if (DefRespPlanTypeEnum.REGION.getCode().equals(plan.getType())) {
            return RegionScopeTypeEnum.COUNTY.getCode().equals(plan.getRegionScopeType())
                && countyRegionContainsStreet(plan.getStreets(), currentStreet);
        }
        return StringUtils.equals(currentStreet, plan.getStreets());
    }

    /**
     * 判断县级方案是否包含指定街道
     */
    boolean countyRegionContainsStreet(String streets, String currentStreet) {
        if (StringUtils.isBlank(currentStreet)) {
            return false;
        }
        return splitStreets(streets).contains(currentStreet.trim());
    }

    /**
     * 解析树查询可见县集合
     */
    Set<String> resolveTreeVisibleCounties(AdRegionVo adRegionVo) {
        if (adRegionVo == null) {
            return Set.of();
        }
        String currentCounty = resolveAdRegionCountyName(adRegionVo);
        if (StringUtils.isNotBlank(currentCounty)) {
            return Set.of(currentCounty);
        }
        if (!Integer.valueOf(2).equals(adRegionVo.getLevel()) || StringUtils.isBlank(adRegionVo.getCity())) {
            return Set.of();
        }
        AdRegionBo adRegionBo = new AdRegionBo();
        adRegionBo.setLevel(3);
        adRegionBo.setCity(adRegionVo.getCity());
        return adRegionService.queryList(adRegionBo).stream()
                              .map(this::resolveAdRegionCountyName)
                              .filter(StringUtils::isNotBlank)
                              .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 解析行政区划县名
     */
    String resolveAdRegionCountyName(AdRegionVo adRegionVo) {
        if (adRegionVo == null) {
            return null;
        }
        if (StringUtils.isNotBlank(adRegionVo.getCounty())) {
            return adRegionVo.getCounty();
        }
        return Integer.valueOf(3).equals(adRegionVo.getLevel()) ? adRegionVo.getName() : null;
    }

    /**
     * 解析行政区划街道名
     */
    String resolveAdRegionStreetName(AdRegionVo adRegionVo) {
        if (adRegionVo == null) {
            return null;
        }
        if (StringUtils.isNotBlank(adRegionVo.getStreet())) {
            return adRegionVo.getStreet();
        }
        return Integer.valueOf(4).equals(adRegionVo.getLevel()) ? adRegionVo.getName() : null;
    }

    /**
     * 判断是否为街道级树查询辖区
     */
    boolean isStreetLevelTreeRegion(AdRegionVo adRegionVo) {
        return adRegionVo != null
            && adRegionVo.getLevel() != null
            && adRegionVo.getLevel() >= 4
            && StringUtils.isNotBlank(resolveAdRegionStreetName(adRegionVo));
    }

    /**
     * 统计树中可见方案数量
     */
    long countTreeVisiblePlans(List<DefRespPlan> typeList) {
        if (typeList == null || typeList.isEmpty()) {
            return 0;
        }
        return typeList.stream()
                       .filter(Objects::nonNull)
                       .filter(plan -> DefRespPlanStatusEnum.isStarted(plan.getStatus())
                           || DefRespPlanStatusEnum.ENDED.getCode().equals(plan.getStatus()))
                       .count();
    }

    /**
     * 构建树形状态分组节点
     */
    Tree<String> buildStatusGroupNode(Integer type, List<DefRespPlan> typeList, String groupKey, Predicate<DefRespPlan> statusPredicate) {
        List<DefRespPlan> groupList = typeList.stream()
                                              .filter(Objects::nonNull)
                                              .filter(statusPredicate)
                                              .toList();
        Tree<String> statusNode = new Tree<>();
        statusNode.setId("type-" + type + "-status-" + groupKey);
        statusNode.setParentId("type-" + type);
        statusNode.setName(groupKey);
        statusNode.putExtra("statusGroup", groupKey);
        statusNode.putExtra("count", groupList.size());
        statusNode.setChildren(List.of(
            buildReqIdGroupNode(statusNode.getId(), true, groupList),
            buildReqIdGroupNode(statusNode.getId(), false, groupList)
        ));
        return statusNode;
    }

    Tree<String> buildReqIdGroupNode(String parentId, boolean reqIdEmpty, List<DefRespPlan> groupList) {
        Tree<String> node = new Tree<>();
        node.setId(parentId + (reqIdEmpty ? "-reqId-empty" : "-reqId-notEmpty"));
        node.setParentId(parentId);
        node.setName(reqIdEmpty ? "reqId为空" : "reqId不为空");
        long count = groupList.stream()
                              .filter(e -> reqIdEmpty ? e.getRegId() == null : e.getRegId() != null)
                              .count();
        node.putExtra("reqIdEmpty", reqIdEmpty);
        node.putExtra("count", count);
        return node;
    }

}
