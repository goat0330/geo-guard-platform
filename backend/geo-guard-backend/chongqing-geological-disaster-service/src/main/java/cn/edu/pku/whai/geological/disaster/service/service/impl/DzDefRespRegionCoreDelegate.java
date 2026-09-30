/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class DzDefRespRegionCoreDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;

    DzDefRespRegionCoreDelegate(DzDefRespPlanServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
    }

    /**
     * 判断是否为县级区域方案
     */
    boolean isCountyRegionPlan(DefRespPlan plan) {
        return plan != null && isCountyRegionPlan(plan.getType(), plan.getRegionScopeType());
    }

    /**
     * 按类型判断是否为县级区域方案
     */
    boolean isCountyRegionPlan(Integer type, Integer regionScopeType) {
        return DefRespPlanTypeEnum.REGION.getCode().equals(type)
            && RegionScopeTypeEnum.COUNTY.getCode().equals(regionScopeType);
    }

    /**
     * 校验县级状态流转角色权限
     */
    void validateCountyRegionStatusTransitionRole() {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_FGXIANZHANG.getRoleKey())
            && !CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZGJLD.getRoleKey())) {
            throw new ServiceException("仅分管县长或县自规局领导允许流转县级区域防御响应状态");
        }
    }

    /**
     * 判断是否为乡镇区域方案
     */
    boolean isTownRegionPlan(DefRespPlan plan) {
        return plan != null
            && DefRespPlanTypeEnum.REGION.getCode().equals(plan.getType())
            && RegionScopeTypeEnum.TOWN.getCode().equals(plan.getRegionScopeType());
    }

    /**
     * 查询县级方案下全部乡镇子行
     */
    List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) {
        if (countyDefId == null) {
            return List.of();
        }
        return baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getParentDefId, countyDefId)
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
        );
    }

    /**
     * 判断乡镇行是否仍参与流程
     */
    boolean isTownRowActive(DefRespPlan town) {
        if (town == null) {
            return false;
        }
        Integer es = town.getExecuteStatus();
        return DefRespExecuteStatusEnum.NOT_STARTED.getCode().equals(es)
            || DefRespExecuteStatusEnum.RUNNING.getCode().equals(es);
    }

    /**
     * 判断执行状态是否仍可参与县级/乡镇区域流转
     */
    boolean isActiveExecuteStatus(Integer executeStatus) {
        return DefRespExecuteStatusEnum.NOT_STARTED.getCode().equals(executeStatus)
            || DefRespExecuteStatusEnum.RUNNING.getCode().equals(executeStatus);
    }

    /**
     * 判断区域方案是否仍有效
     */
    boolean isEffectiveRegionPlan(DefRespPlan plan) {
        if (plan == null || !Objects.equals(plan.getDeleted(), 0)) {
            return false;
        }
        if (!isActiveExecuteStatus(plan.getExecuteStatus())) {
            return false;
        }
        return !Objects.equals(plan.getStatus(), DefRespPlanStatusEnum.ENDED.getCode());
    }

    /**
     * 按 id 查询仍可更新的县级区域防御响应
     */
    DefRespPlan getActiveCountyRegionPlanById(Long countyDefId) {
        if (countyDefId == null) {
            return null;
        }
        DefRespPlan county = baseMapper.selectOne(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getId, countyDefId)
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode())
                    .in(DefRespPlan::getExecuteStatus,
                        DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                        DefRespExecuteStatusEnum.RUNNING.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .ne(DefRespPlan::getStatus, DefRespPlanStatusEnum.ENDED.getCode())
        );
        return isCountyRegionPlan(county) ? county : null;
    }

    /**
     * 更新仍处于执行中的县级区域防御响应
     */
    void updateActiveCountyRegionPlanOrThrow(DefRespPlan county, String errorMessage) {
        if (county == null || county.getId() == null) {
            throw new ServiceException(errorMessage);
        }
        LambdaUpdateWrapper<DefRespPlan> wrapper = Wrappers.<DefRespPlan>lambdaUpdate()
                                                           .eq(DefRespPlan::getId, county.getId())
                                                           .in(DefRespPlan::getExecuteStatus,
                                                               DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                                                               DefRespExecuteStatusEnum.RUNNING.getCode());
        if (baseMapper.update(county, wrapper) <= 0) {
            throw new ServiceException(errorMessage);
        }
    }

    /**
     * 更新仍处于执行中的乡镇区域防御响应
     */
    void updateActiveTownRegionPlanOrThrow(DefRespPlan town, String errorMessage) {
        if (town == null || town.getId() == null) {
            throw new ServiceException(errorMessage);
        }
        LambdaUpdateWrapper<DefRespPlan> wrapper = Wrappers.<DefRespPlan>lambdaUpdate()
                                                           .eq(DefRespPlan::getId, town.getId())
                                                           .in(DefRespPlan::getExecuteStatus,
                                                               DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                                                               DefRespExecuteStatusEnum.RUNNING.getCode());
        if (baseMapper.update(town, wrapper) <= 0) {
            throw new ServiceException(errorMessage);
        }
    }

    /**
     * 批量更新仍处于执行中的乡镇区域防御响应
     */
    void updateActiveTownRegionPlansOrThrow(List<DefRespPlan> towns, String errorMessage) {
        if (towns == null || towns.isEmpty()) {
            return;
        }
        for (DefRespPlan town : towns) {
            updateActiveTownRegionPlanOrThrow(town, errorMessage);
        }
    }

    /**
     * 查询指定层级的有效区域方案
     */
    List<DefRespPlan> listEffectiveRegionPlans(String county, Integer regionScopeType, Long excludeId) {
        if (StringUtils.isBlank(county) || regionScopeType == null) {
            return List.of();
        }
        List<DefRespPlan> regionPlans = baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, regionScopeType)
                    .eq(DefRespPlan::getCounty, county)
                    .in(DefRespPlan::getExecuteStatus,
                        DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                        DefRespExecuteStatusEnum.RUNNING.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
        );
        if (regionPlans == null || regionPlans.isEmpty()) {
            return List.of();
        }
        return regionPlans.stream()
                          .filter(this::isEffectiveRegionPlan)
                          .filter(plan -> excludeId == null || !Objects.equals(plan.getId(), excludeId))
                          .toList();
    }

    /**
     * 校验县级区域并存约束
     */
    void validateCountyRegionConstraints(String county, String streets, Long currentCountyId) {
        if (StringUtils.isBlank(county)) {
            throw new ServiceException("县级区域防御响应所属区县不能为空");
        }
        List<DefRespPlan> effectiveCountyPlans = listEffectiveRegionPlans(county, RegionScopeTypeEnum.COUNTY.getCode(), currentCountyId);
        if (!effectiveCountyPlans.isEmpty()) {
            throw new ServiceException("同一个县最多同时存在1个未关闭或归档的县级区域防御响应");
        }
        validateCountyRegionStreetOccupancy(county, streets, currentCountyId);
    }

    /**
     * 校验县级街道占用约束
     */
    void validateCountyRegionStreetOccupancy(String county, String streets, Long currentCountyId) {
        List<String> targetStreets = DzDefRespPlanServiceImpl.splitStreets(streets);
        if (targetStreets.isEmpty()) {
            throw new ServiceException("县级区域防御响应至少需要关联一个乡镇");
        }
        Map<String, List<DefRespPlan>> effectiveTownPlansByStreet = listEffectiveRegionPlans(county, RegionScopeTypeEnum.TOWN.getCode(), null)
            .stream()
            .filter(plan -> StringUtils.isNotBlank(plan.getStreets()))
            .collect(Collectors.groupingBy(plan -> plan.getStreets().trim()));
        for (String street : targetStreets) {
            List<DefRespPlan> matchedPlans = effectiveTownPlansByStreet.getOrDefault(street, List.of());
            if (matchedPlans.isEmpty()) {
                continue;
            }
            boolean occupiedByOtherCounty = matchedPlans.stream()
                                                        .anyMatch(plan -> !Objects.equals(plan.getParentDefId(), currentCountyId));
            if (occupiedByOtherCounty) {
                throw new ServiceException("乡镇" + street + "已关联其他未关闭或归档的县级区域防御响应");
            }
            long sameCountyActiveTownCount = matchedPlans.stream()
                                                         .filter(plan -> Objects.equals(plan.getParentDefId(), currentCountyId))
                                                         .count();
            if (sameCountyActiveTownCount > 1) {
                throw new ServiceException("乡镇" + street + "同时存在多条未关闭或归档的乡镇级区域防御响应");
            }
        }
    }

    /**
     * 校验乡镇区域并存约束
     */
    void validateTownRegionConstraints(String county, String street, Long currentTownId) {
        if (StringUtils.isBlank(county) || StringUtils.isBlank(street)) {
            throw new ServiceException("乡镇级区域防御响应的区县和乡镇不能为空");
        }
        boolean occupied = listEffectiveRegionPlans(county, RegionScopeTypeEnum.TOWN.getCode(), currentTownId).stream()
                                                                                                              .anyMatch(plan -> street.trim().equals(StringUtils.trim(plan.getStreets())));
        if (occupied) {
            throw new ServiceException("同一个乡镇最多同时存在一个未关闭或归档的乡镇级区域防御响应");
        }
    }

    /**
     * 校验区域方案约束
     */
    void validateRegionPlanConstraints(DefRespPlan plan) {
        if (plan == null || !Objects.equals(plan.getType(), DefRespPlanTypeEnum.REGION.getCode()) || !isEffectiveRegionPlan(plan)) {
            return;
        }
        if (Objects.equals(plan.getRegionScopeType(), RegionScopeTypeEnum.COUNTY.getCode())) {
            validateCountyRegionConstraints(plan.getCounty(), plan.getStreets(), plan.getId());
            return;
        }
        if (Objects.equals(plan.getRegionScopeType(), RegionScopeTypeEnum.TOWN.getCode())) {
            validateTownRegionConstraints(plan.getCounty(), plan.getStreets(), plan.getId());
        }
    }

    /**
     * 合并更新字段构建约束校验候选
     */
    DefRespPlan buildRegionConstraintCandidate(DefRespPlan current, DefRespPlanBo bo) {
        DefRespPlan candidate = new DefRespPlan();
        candidate.setId(current.getId());
        candidate.setType(current.getType());
        candidate.setRegionScopeType(current.getRegionScopeType());
        candidate.setCounty(current.getCounty());
        candidate.setStreets(current.getStreets());
        candidate.setLevel(current.getLevel());
        candidate.setParentDefId(current.getParentDefId());
        candidate.setStatus(current.getStatus());
        candidate.setExecuteStatus(current.getExecuteStatus());
        candidate.setDeleted(current.getDeleted());
        if (bo.getType() != null) {
            candidate.setType(bo.getType());
        }
        if (bo.getRegionScopeType() != null) {
            candidate.setRegionScopeType(bo.getRegionScopeType());
        }
        if (bo.getCounty() != null) {
            candidate.setCounty(bo.getCounty());
        }
        if (bo.getStreets() != null) {
            candidate.setStreets(bo.getStreets());
        }
        if (bo.getLevel() != null) {
            candidate.setLevel(bo.getLevel());
        }
        if (bo.getParentDefId() != null) {
            candidate.setParentDefId(bo.getParentDefId());
        }
        if (bo.getStatus() != null) {
            candidate.setStatus(bo.getStatus());
        }
        if (bo.getExecuteStatus() != null) {
            candidate.setExecuteStatus(bo.getExecuteStatus());
        }
        if (bo.getDeleted() != null) {
            candidate.setDeleted(bo.getDeleted());
        }
        return candidate;
    }

    /**
     * 按县级状态解析新建乡镇行的 execute_status
     */
    Integer resolveTownExecuteStatusForCounty(DefRespPlan county) {
        if (county != null && DefRespPlanStatusEnum.TASK_PUBLISHED.getCode().equals(county.getStatus())) {
            return DefRespExecuteStatusEnum.RUNNING.getCode();
        }
        if (county == null || county.getExecuteStatus() == null) {
            return DefRespExecuteStatusEnum.RUNNING.getCode();
        }
        return county.getExecuteStatus();
    }

    /**
     * 同步乡镇行状态与县级对齐
     */
    void syncTownMirrorStatus(Long countyDefId, Integer newStatus, Date date) {
        if (countyDefId == null || newStatus == null) {
            return;
        }
        DefRespPlan county = baseMapper.selectById(countyDefId);
        Integer currentRoundNo = county == null || county.getCurrentRoundNo() == null ? 1 : county.getCurrentRoundNo();
        boolean syncExecuteStatusToRunning = DefRespPlanStatusEnum.TASK_PUBLISHED.getCode().equals(newStatus);
        List<DefRespPlan> towns = listTownPlansByCountyId(countyDefId);
        for (DefRespPlan town : towns) {
            if (!isTownRowActive(town)) {
                continue;
            }
            town.setStatus(newStatus);
            if (syncExecuteStatusToRunning) {
                town.setExecuteStatus(DefRespExecuteStatusEnum.RUNNING.getCode());
            }
            town.setCurrentRoundNo(currentRoundNo);
            if (county != null && !Objects.equals(town.getResponsiblePerson(), county.getResponsiblePerson())) {
                town.setResponsiblePerson(county.getResponsiblePerson());
            }
            if (county != null && !Objects.equals(town.getResponsiblePersonPhone(), county.getResponsiblePersonPhone())) {
                town.setResponsiblePersonPhone(county.getResponsiblePersonPhone());
            }
            town.setUpdateDate(date);
            updateActiveTownRegionPlanOrThrow(town, "更新乡镇区域防御响应失败");
        }
    }
}
