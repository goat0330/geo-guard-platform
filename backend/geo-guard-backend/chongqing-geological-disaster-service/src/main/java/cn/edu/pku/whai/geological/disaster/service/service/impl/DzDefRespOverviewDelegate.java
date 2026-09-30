/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DefRespRange;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DefRespRangeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
class DzDefRespOverviewDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;

    DzDefRespOverviewDelegate(DzDefRespPlanServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
    }

    private AdRegionVo requireCurrentUserDefRespPlanRegion() { return service.requireCurrentUserDefRespPlanRegion(); }

    private Map<Long, String> buildSlopeUnitIdByHandleId(List<Long> handleIds) { return service.buildSlopeUnitIdByHandleId(handleIds); }

    private void appendCountyRegionViewStreetCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) { service.appendCountyRegionViewStreetCondition(lqw, street); }

    private Map<String, List<DefRespRange>> buildRegionRangesByStreetKey(List<DefRespPlan> regionPlans) { return service.buildRegionRangesByStreetKey(regionPlans); }

    private String joinStreetKey(String streets) { return service.joinStreetKey(streets); }

    private void appendStreetContainsCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) { service.appendStreetContainsCondition(lqw, street); }

    private List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return service.listTownPlansByCountyId(countyDefId); }

    private LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo, boolean withOrderBy) { return service.buildQueryWrapper(bo, withOrderBy); }
    public DefRespRangeResp getDefenseRespRange() {
        AdRegionVo currentUserRegion = requireCurrentUserDefRespPlanRegion();
        String county = currentUserRegion == null ? null : currentUserRegion.getCounty();
        String street = currentUserRegion == null ? null : currentUserRegion.getStreet();
        // 1. 单点防御响应方案
        List<DefRespPlan> singlePlans = baseMapper.selectList(
            Wrappers.lambdaQuery(DefRespPlan.class)
                    .select(DefRespPlan::getId, DefRespPlan::getStatus, DefRespPlan::getHandleId, DefRespPlan::getCenter)
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.SINGLE.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .eq(StringUtils.isNotBlank(county), DefRespPlan::getCounty, county)
                    .eq(StringUtils.isNotBlank(street), DefRespPlan::getStreets, street)
        );

        Map<Long, List<DefRespRange>> singleStarted = new LinkedHashMap<>();
        if (singlePlans != null && !singlePlans.isEmpty()) {
            Map<Long, String> slopeUnitIdByHandleId = buildSlopeUnitIdByHandleId(singlePlans.stream()
                                                                                            .map(DefRespPlan::getHandleId)
                                                                                            .filter(Objects::nonNull)
                                                                                            .toList());
            for (DefRespPlan plan : singlePlans) {
                if (plan == null || plan.getId() == null || plan.getHandleId() == null) {
                    continue;
                }
                DefRespRange vo = BeanUtil.toBean(plan, DefRespRange.class);
                String slopeUnitId = slopeUnitIdByHandleId.get(plan.getHandleId());
                if (StringUtils.isBlank(slopeUnitId)) {
                    log.warn("单点防御响应方案关联处置记录缺失，已跳过，defId={}, handleId={}", plan.getId(), plan.getHandleId());
                    continue;
                }
                vo.setSlopeUnitId(slopeUnitId);
                List<DefRespRange> voList = List.of(vo);
                if (DefRespPlanStatusEnum.isStarted(plan.getStatus())) {
                    singleStarted.put(plan.getId(), voList);
                }
            }
        }
        // 2. 区域防御响应方案（仅县级行；乡镇行由县级聚合展示，不参与范围图）
        LambdaQueryWrapper<DefRespPlan> regionLqw = Wrappers.lambdaQuery(DefRespPlan.class)
                                                            .select(DefRespPlan::getId, DefRespPlan::getStatus, DefRespPlan::getStreets)
                                                            .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                                                            .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode())
                                                            .eq(DefRespPlan::getDeleted, 0)
                                                            .eq(StringUtils.isNotBlank(county), DefRespPlan::getCounty, county);
        appendCountyRegionViewStreetCondition(regionLqw, street);
        List<DefRespPlan> regionPlans = baseMapper.selectList(regionLqw);

        Map<Long, List<DefRespRange>> regionStarted = new LinkedHashMap<>();
        Map<Long, List<DefRespRange>> regionUnstarted = new LinkedHashMap<>();

        if (regionPlans != null) {
            Map<String, List<DefRespRange>> regionRangesByStreetKey = buildRegionRangesByStreetKey(regionPlans);
            for (DefRespPlan plan : regionPlans) {
                if (plan == null || plan.getId() == null) {
                    continue;
                }
                List<DefRespRange> defRespRanges = regionRangesByStreetKey.getOrDefault(joinStreetKey(plan.getStreets()), List.of());
                Integer status = plan.getStatus();
                if (DefRespPlanStatusEnum.isDefenseStarted(status)) {
                    regionStarted.put(plan.getId(), defRespRanges);
                } else if (DefRespPlanStatusEnum.isUnDefenseStarted(status)) {
                    regionUnstarted.put(plan.getId(), defRespRanges);
                }
            }
        }
        DefRespRangeResp resp = new DefRespRangeResp();
        DefRespRangeResp.DefRespRangeStatus singleRange = new DefRespRangeResp.DefRespRangeStatus();
        singleRange.setStarted(singleStarted);
        resp.setSingle(singleRange);
        DefRespRangeResp.DefRespRangeStatus regionRange = new DefRespRangeResp.DefRespRangeStatus();
        regionRange.setStarted(regionStarted);
        regionRange.setUnstarted(regionUnstarted);
        resp.setRegion(regionRange);
        return resp;
    }

    /**
     * 查询方案覆盖范围内的斜坡单元
     */

    public Long eventCount(AdRegionVo adRegionVo, List<Integer> status) {
        LambdaQueryWrapper<DefRespPlan> singleLqw = Wrappers.lambdaQuery();
        singleLqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DefRespPlan::getCounty, adRegionVo.getCounty());
        appendStreetContainsCondition(singleLqw, adRegionVo.getStreet());
        singleLqw.in(DefRespPlan::getStatus, status);
        singleLqw.eq(DefRespPlan::getDeleted, 0);
        singleLqw.isNull(DefRespPlan::getRegionScopeType);
        LambdaQueryWrapper<DefRespPlan> regionLqw = Wrappers.lambdaQuery();
        regionLqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DefRespPlan::getCounty, adRegionVo.getCounty());
        appendStreetContainsCondition(regionLqw, adRegionVo.getStreet());
        regionLqw.in(DefRespPlan::getStatus, status);
        regionLqw.eq(DefRespPlan::getDeleted, 0);
        regionLqw.eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode());
        Long singleEventCount = baseMapper.selectCount(singleLqw);
        Long regionEventCount = baseMapper.selectCount(regionLqw);
        return singleEventCount + regionEventCount;
    }

    /**
     * 查询方案关联的单点或区域信息
     */
    public List<DefRespPlanVo> getRelationInfo(Long id) {
        DefRespPlan defRespPlan = baseMapper.selectById(id);
        if (defRespPlan != null && DefRespPlanTypeEnum.SINGLE.getCode().equals(defRespPlan.getType())) {
            List<DefRespPlanVo> defRespPlanVoList = new ArrayList<>();
            if (defRespPlan.getRegId() != null) {
                DefRespPlanVo planVo = baseMapper.selectVoById(defRespPlan.getRegId());
                defRespPlanVoList.add(planVo);
                return defRespPlanVoList;
            } else {
                return defRespPlanVoList;
            }
        } else if (defRespPlan != null && DefRespPlanTypeEnum.REGION.getCode().equals(defRespPlan.getType())) {
            if (RegionScopeTypeEnum.COUNTY.getCode().equals(defRespPlan.getRegionScopeType())) {
                List<Long> townPlanIds = listTownPlansByCountyId(id).stream()
                                                                    .map(DefRespPlan::getId)
                                                                    .filter(Objects::nonNull)
                                                                    .toList();
                if (townPlanIds.isEmpty()) {
                    return List.of();
                }
                return baseMapper.selectVoList(
                    Wrappers.<DefRespPlan>lambdaQuery()
                            .eq(DefRespPlan::getType, DefRespPlanTypeEnum.SINGLE.getCode())
                            .in(DefRespPlan::getRegId, townPlanIds)
                            .eq(DefRespPlan::getDeleted, 0)
                            .orderByDesc(DefRespPlan::getCreateDate)
                );
            }
            DefRespPlanBo defRespPlanBo = new DefRespPlanBo();
            defRespPlanBo.setRegId(id);
            defRespPlanBo.setType(DefRespPlanTypeEnum.SINGLE.getCode());
            return baseMapper.selectVoList(buildQueryWrapper(defRespPlanBo, true));
        } else {
            throw new ServiceException("防御响应类型错误");
        }
    }

}
