/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DefRespRange;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.DefRespPlanDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanRangeSlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespQueryDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final IAdRegionService adRegionService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final ISlopeUnitService slopeUnitService;

    DzDefRespQueryDelegate(DzDefRespPlanServiceImpl service,
                           IDzUserAdRegionService dzUserAdRegionService,
                           IAdRegionService adRegionService,
                           DzTaskHandleMapper dzTaskHandleMapper,
                           ISlopeUnitService slopeUnitService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzUserAdRegionService = dzUserAdRegionService;
        this.adRegionService = adRegionService;
        this.dzTaskHandleMapper = dzTaskHandleMapper;
        this.slopeUnitService = slopeUnitService;
    }

    private boolean isCountyRegionPlan(DefRespPlan plan) { return service.isCountyRegionPlan(plan); }

    private boolean isCountyRegionPlan(Integer type, Integer regionScopeType) { return service.isCountyRegionPlan(type, regionScopeType); }

    private boolean isTownRegionPlan(DefRespPlan plan) { return service.isTownRegionPlan(plan); }

    private List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return service.listTownPlansByCountyId(countyDefId); }

    private DefRespPlan getActiveCountyRegionPlanById(Long countyDefId) { return service.getActiveCountyRegionPlanById(countyDefId); }

    private boolean countyRegionContainsStreet(String streets, String currentStreet) { return service.countyRegionContainsStreet(streets, currentStreet); }

    private List<String> splitStreets(String streets) { return DzDefRespPlanServiceImpl.splitStreets(streets); }
    void appendStreetContainsCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) {
        if (StringUtils.isBlank(street)) {
            return;
        }
        lqw.apply("position({0} in (',' || replace(coalesce(streets, ''), '，', ',') || ',')) > 0", "," + street.trim() + ",");
    }

    /**
     * 分页条件查询防御响应方案
     */
    public TableDataInfo<DefRespPlanVo> listByCondition(DefRespPlanDto bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DefRespPlan> lqw = buildQueryWrapper(BeanUtil.toBean(bo, DefRespPlanBo.class));
        appendCurrentUserDefRespPlanPermission(lqw, true);
        String userKeyword = bo.getUserKeyword();
        if (StringUtils.isNotBlank(userKeyword)) {
            lqw.and(wrapper -> wrapper.eq(DefRespPlan::getCode, userKeyword)
                                      .or()
                                      .like(DefRespPlan::getName, userKeyword));
        }
        if (bo.getBeginTime() != null && bo.getEndTime() != null) {
            lqw.between(DefRespPlan::getCreateDate, bo.getBeginTime(), bo.getEndTime());
        } else if (bo.getBeginTime() != null) {
            lqw.ge(DefRespPlan::getCreateDate, bo.getBeginTime());
        } else if (bo.getEndTime() != null) {
            lqw.le(DefRespPlan::getCreateDate, bo.getEndTime());
        }
        lqw.in(bo.getProcess() != null && !bo.getProcess().isEmpty(), DefRespPlan::getStatus, bo.getProcess());
        if (bo.getRelation() != null) {
            lqw.isNull(Integer.valueOf(1).equals(bo.getRelation()), DefRespPlan::getRegId);
            lqw.isNotNull(Integer.valueOf(2).equals(bo.getRelation()), DefRespPlan::getRegId);
        }
        lqw.and(wrapper -> wrapper.isNull(DefRespPlan::getRegionScopeType)
                                  .or()
                                  .ne(DefRespPlan::getRegionScopeType, 2));
        Page<DefRespPlanVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<DefRespPlanVo> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            enrichDefRespPlanVos(records);
        }
        return TableDataInfo.build(result);
    }

    /**
     * 获取当前用户防御响应主辖区
     */
    AdRegionVo requireCurrentUserDefRespPlanRegion() {
        if (LoginHelper.isSuperAdmin()) {
            return null;
        }
        List<AdRegionVo> userRegions = listCurrentUserDefRespPlanRegions();
        if (userRegions.isEmpty()) {
            throw new ServiceException("当前用户未配置行政区划，无法查看方案");
        }
        return userRegions.getFirst();
    }

    /**
     * 获取当前用户全部防御响应辖区
     */
    List<AdRegionVo> listCurrentUserDefRespPlanRegions() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }
        List<String> adRegionIds = dzUserAdRegionService.queryEffectiveListByUserId(userId)
                                                        .stream()
                                                        .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
                                                        .map(DzUserAdRegionVo::getAdRegionId)
                                                        .distinct()
                                                        .toList();
        if (adRegionIds.isEmpty()) {
            return List.of();
        }
        List<AdRegionVo> userRegions = adRegionIds.stream()
                                                  .map(adRegionService::getInfo)
                                                  .filter(Objects::nonNull)
                                                  .toList();
        if (userRegions.isEmpty()) {
            throw new ServiceException("当前用户关联的行政区划不存在，无法查看方案");
        }
        return userRegions;
    }

    /**
     * 追加当前用户数据权限条件
     */
    void appendCurrentUserDefRespPlanPermission(LambdaQueryWrapper<DefRespPlan> lqw, boolean includeAssociatedCountyRegion) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        List<AdRegionVo> userRegions = listCurrentUserDefRespPlanRegions();
        if (userRegions.isEmpty()) {
            lqw.apply("1 = 0");
            return;
        }
        List<AdRegionVo> effectiveRegions = userRegions.stream()
                                                       .filter(this::hasAnyDefRespPlanPermissionScope)
                                                       .toList();
        if (effectiveRegions.isEmpty()) {
            lqw.apply("1 = 0");
            return;
        }
        lqw.and(wrapper -> {
            for (AdRegionVo adRegionVo : effectiveRegions) {
                wrapper.or(inner -> appendSingleDefRespPlanPermission(inner, adRegionVo, includeAssociatedCountyRegion));
            }
        });
    }

    private boolean hasAnyDefRespPlanPermissionScope(AdRegionVo adRegionVo) {
        return adRegionVo != null
            && (StringUtils.isNotBlank(adRegionVo.getCounty())
            || StringUtils.isNotBlank(adRegionVo.getStreet())
            || StringUtils.isNotBlank(adRegionVo.getVillage()));
    }

    /**
     * 校验当前用户查看权限
     */
    void validateCurrentUserDefRespPlanViewAccess(DefRespPlanVo defRespPlanVo) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        List<AdRegionVo> userRegions = listCurrentUserDefRespPlanRegions();
        boolean matched = userRegions.stream().anyMatch(item -> hasDefRespPlanViewAccess(item, defRespPlanVo));
        if (!matched) {
            throw new ServiceException("该行政区划不在用户辖区");
        }
    }

    /**
     * 校验当前用户操作权限
     */
    void validateCurrentUserDefRespPlanAccess(Long handleId, String county, String streets) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        List<AdRegionVo> userRegions = listCurrentUserDefRespPlanRegions();
        if (handleId != null) {
            DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
            if (dzTaskHandle == null) {
                throw new ServiceException("未查询到关联的灾害处置记录");
            }
            boolean matched = userRegions.stream().anyMatch(item -> matchesRegion(item, dzTaskHandle.getProvince(), dzTaskHandle.getCity(), dzTaskHandle.getCounty(), dzTaskHandle.getStreet(), dzTaskHandle.getVillage()));
            if (!matched) {
                throw new ServiceException("该行政区划不在用户辖区");
            }
            return;
        }
        boolean matched = userRegions.stream().anyMatch(item -> matchesRegion(item, null, null, county, streets, null));
        if (!matched) {
            throw new ServiceException("该行政区划不在用户辖区");
        }
    }

    /**
     * 判断用户是否有方案查看权限
     */
    boolean hasDefRespPlanViewAccess(AdRegionVo adRegionVo, DefRespPlanVo defRespPlanVo) {
        if (adRegionVo == null || defRespPlanVo == null) {
            return false;
        }
        if (isCountyRegionPlan(defRespPlanVo.getType(), defRespPlanVo.getRegionScopeType())) {
            return matchesAssociatedCountyRegionPlan(adRegionVo, defRespPlanVo.getCounty(), defRespPlanVo.getStreets());
        }
        Long handleId = defRespPlanVo.getHandleId();
        if (handleId != null) {
            DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
            if (dzTaskHandle == null) {
                throw new ServiceException("未查询到关联的灾害处置记录");
            }
            return matchesRegion(adRegionVo, dzTaskHandle.getProvince(), dzTaskHandle.getCity(), dzTaskHandle.getCounty(), dzTaskHandle.getStreet(), dzTaskHandle.getVillage());
        }
        return matchesRegion(adRegionVo, null, null, defRespPlanVo.getCounty(), defRespPlanVo.getStreets(), null);
    }

    /**
     * 追加单点方案权限条件
     */
    void appendSingleDefRespPlanPermission(LambdaQueryWrapper<DefRespPlan> lqw, AdRegionVo adRegionVo, boolean includeAssociatedCountyRegion) {
        lqw.eq(StringUtils.isNotBlank(adRegionVo.getCounty()), DefRespPlan::getCounty, adRegionVo.getCounty());
        if (includeAssociatedCountyRegion) {
            appendDefRespPlanViewStreetCondition(lqw, adRegionVo.getStreet());
        } else {
            appendStreetContainsCondition(lqw, adRegionVo.getStreet());
        }
        if (StringUtils.isNotBlank(adRegionVo.getVillage())) {
            lqw.and(wrapper -> wrapper.ne(DefRespPlan::getType, DefRespPlanTypeEnum.SINGLE.getCode())
                                      .or(inner -> inner.apply("exists (select 1 from dz_task_handle dth where dth.id = handle_id and dth.village = {0})",
                                          adRegionVo.getVillage())));
        }
    }

    /**
     * 追加方案查看街道条件
     */
    void appendDefRespPlanViewStreetCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) {
        if (StringUtils.isBlank(street)) {
            return;
        }
        appendStreetContainsCondition(lqw, street);
    }

    /**
     * 追加县级区域查看街道条件
     */
    void appendCountyRegionViewStreetCondition(LambdaQueryWrapper<DefRespPlan> lqw, String street) {
        if (StringUtils.isBlank(street)) {
            return;
        }
        appendStreetContainsCondition(lqw, street);
    }

    /**
     * 判断是否匹配关联县级区域方案
     */
    boolean matchesAssociatedCountyRegionPlan(AdRegionVo adRegionVo, String county, String streets) {
        if (adRegionVo == null) {
            return false;
        }
        if (StringUtils.isNotBlank(adRegionVo.getCounty())
            && StringUtils.isNotBlank(county)
            && !adRegionVo.getCounty().equals(county)) {
            return false;
        }
        if (StringUtils.isBlank(adRegionVo.getStreet())) {
            return true;
        }
        return countyRegionContainsStreet(streets, adRegionVo.getStreet());
    }

    /**
     * 判断区划是否匹配
     */
    boolean matchesRegion(AdRegionVo adRegionVo, String province, String city, String county, String street, String village) {
        return (StringUtils.isBlank(adRegionVo.getProvince()) || StringUtils.isBlank(province) || adRegionVo.getProvince().equals(province))
            && (StringUtils.isBlank(adRegionVo.getCity()) || StringUtils.isBlank(city) || adRegionVo.getCity().equals(city))
            && (StringUtils.isBlank(adRegionVo.getCounty()) || StringUtils.isBlank(county) || adRegionVo.getCounty().equals(county))
            && (StringUtils.isBlank(adRegionVo.getStreet()) || splitStreets(street).contains(adRegionVo.getStreet().trim()))
            && (StringUtils.isBlank(adRegionVo.getVillage()) || StringUtils.isBlank(village) || adRegionVo.getVillage().equals(village));
    }

    /**
     * 填充方案列表扩展字段
     */
    void enrichDefRespPlanVos(List<DefRespPlanVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<DefRespPlanVo> singleVos = vos.stream()
                                           .filter(Objects::nonNull)
                                           .filter(vo -> DefRespPlanTypeEnum.SINGLE.getCode().equals(vo.getType()))
                                           .toList();
        if (!singleVos.isEmpty()) {
            Map<Long, String> slopeUnitIdByHandleId = buildSlopeUnitIdByHandleId(singleVos.stream()
                                                                                          .map(DefRespPlanVo::getHandleId)
                                                                                          .filter(Objects::nonNull)
                                                                                          .toList());
            for (DefRespPlanVo vo : singleVos) {
                vo.setSlopeUnitId(slopeUnitIdByHandleId.get(vo.getHandleId()));
            }
        }
        List<DefRespPlanVo> regionVos = vos.stream()
                                           .filter(Objects::nonNull)
                                           .filter(vo -> DefRespPlanTypeEnum.REGION.getCode().equals(vo.getType()))
                                           .toList();
        if (!regionVos.isEmpty()) {
            Map<String, Integer> slopeUnitCountByStreetKey = buildSlopeUnitCountByStreetKey(regionVos.stream()
                                                                                                     .map(DefRespPlanVo::getStreets)
                                                                                                     .toList());
            for (DefRespPlanVo vo : regionVos) {
                vo.setSlopeUnitCount(slopeUnitCountByStreetKey.getOrDefault(joinStreetKey(vo.getStreets()), 0));
            }
        }
    }

    /**
     * 按处置 id 构建斜坡单元映射
     */
    Map<Long, String> buildSlopeUnitIdByHandleId(List<Long> handleIds) {
        if (handleIds == null || handleIds.isEmpty()) {
            return Map.of();
        }
        return dzTaskHandleMapper.selectList(
                                     Wrappers.<DzTaskHandle>lambdaQuery().in(DzTaskHandle::getId, handleIds)
                                 ).stream()
                                 .filter(Objects::nonNull)
                                 .filter(handle -> StringUtils.isNotBlank(handle.getSlopeUnitId()))
                                 .collect(Collectors.toMap(DzTaskHandle::getId, DzTaskHandle::getSlopeUnitId, (left, right) -> left));
    }

    /**
     * 构建单点方案范围斜坡单元
     */
    List<DefRespPlanRangeSlopeUnitVo> buildSingleRangeSlopeUnits(DefRespPlan defRespPlan) {
        if (defRespPlan.getHandleId() == null) {
            return List.of();
        }
        String slopeUnitId = buildSlopeUnitIdByHandleId(List.of(defRespPlan.getHandleId())).get(defRespPlan.getHandleId());
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        SlopeUnitVo slopeUnitVo = slopeUnitService.queryById(slopeUnitId);
        if (slopeUnitVo == null) {
            return List.of();
        }
        DefRespPlanRangeSlopeUnitVo vo = new DefRespPlanRangeSlopeUnitVo();
        vo.setStreet(slopeUnitVo.getStreet());
        vo.setSlopeUnitId(slopeUnitVo.getId());
        vo.setCenter(slopeUnitVo.getCenter());
        vo.setWkt(slopeUnitVo.getWkt());
        return List.of(vo);
    }

    /**
     * 构建区域方案范围斜坡单元
     */
    List<DefRespPlanRangeSlopeUnitVo> buildRegionRangeSlopeUnits(DefRespPlan defRespPlan) {
        List<SlopeUnitVo> slopeUnitVos = slopeUnitService.querySlopeUnitListByStreets(splitStreets(defRespPlan.getStreets()));
        if (slopeUnitVos == null || slopeUnitVos.isEmpty()) {
            return List.of();
        }
        return slopeUnitVos.stream()
                           .filter(Objects::nonNull)
                           .map(this::buildRangeSlopeUnitVo)
                           .toList();
    }

    /**
     * 构建范围斜坡单元 VO
     */
    DefRespPlanRangeSlopeUnitVo buildRangeSlopeUnitVo(SlopeUnitVo slopeUnitVo) {
        DefRespPlanRangeSlopeUnitVo vo = new DefRespPlanRangeSlopeUnitVo();
        vo.setStreet(slopeUnitVo.getStreet());
        vo.setSlopeUnitId(slopeUnitVo.getId());
        vo.setCenter(slopeUnitVo.getCenter());
        vo.setWkt(slopeUnitVo.getWkt());
        return vo;
    }

    /**
     * 按街道键统计斜坡单元数量
     */
    Map<String, Integer> buildSlopeUnitCountByStreetKey(List<String> streetsList) {
        if (streetsList == null || streetsList.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> result = new HashMap<>();
        for (String streets : streetsList) {
            String streetKey = joinStreetKey(streets);
            if (streetKey.isEmpty() || result.containsKey(streetKey)) {
                continue;
            }
            List<SlopeUnitVo> slopeUnits = slopeUnitService.querySlopeUnitListByStreets(splitStreets(streets));
            result.put(streetKey, slopeUnits == null ? 0 : slopeUnits.size());
        }
        return result;
    }

    /**
     * 按街道键构建区域范围映射
     */
    Map<String, List<DefRespRange>> buildRegionRangesByStreetKey(List<DefRespPlan> regionPlans) {
        if (regionPlans == null || regionPlans.isEmpty()) {
            return Map.of();
        }
        Map<String, List<DefRespRange>> result = new HashMap<>();
        for (DefRespPlan plan : regionPlans) {
            if (plan == null) {
                continue;
            }
            String streetKey = joinStreetKey(plan.getStreets());
            if (streetKey.isEmpty() || result.containsKey(streetKey)) {
                continue;
            }
            List<AdRegionVo> adRegionVos = adRegionService.querySlopeUnitListByStreets(splitStreets(plan.getStreets()));
            result.put(streetKey, BeanUtil.copyToList(adRegionVos, DefRespRange.class));
        }
        return result;
    }

    /**
     * 将街道列表拼接为查询键
     */
    String joinStreetKey(String streets) {
        List<String> streetList = splitStreets(streets);
        return streetList.isEmpty() ? "" : String.join("|", streetList);
    }

    /**
     * 构建查询条件
     */
    LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo) {
        return buildQueryWrapper(bo, true);
    }

    /**
     * 构建带排序的查询条件
     */
    LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo, boolean withOrderBy) {
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, DefRespPlan::getId, bo.getId());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), DefRespPlan::getCode, bo.getCode());
        lqw.like(StringUtils.isNotBlank(bo.getName()), DefRespPlan::getName, bo.getName());
        lqw.eq(bo.getType() != null, DefRespPlan::getType, bo.getType());
        lqw.eq(bo.getStatus() != null, DefRespPlan::getStatus, bo.getStatus());
        lqw.eq(bo.getHandleId() != null, DefRespPlan::getHandleId, bo.getHandleId());
        lqw.eq(bo.getRegId() != null, DefRespPlan::getRegId, bo.getRegId());
        lqw.eq(StringUtils.isNotBlank(bo.getCenter()), DefRespPlan::getCenter, bo.getCenter());
        lqw.like(StringUtils.isNotBlank(bo.getTriggerCondition()), DefRespPlan::getTriggerCondition, bo.getTriggerCondition());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), DefRespPlan::getCounty, bo.getCounty());
        lqw.like(StringUtils.isNotBlank(bo.getStreets()), DefRespPlan::getStreets, bo.getStreets());
        lqw.like(StringUtils.isNotBlank(bo.getResponsibilityUnit()), DefRespPlan::getResponsibilityUnit, bo.getResponsibilityUnit());
        lqw.like(StringUtils.isNotBlank(bo.getResponsiblePerson()), DefRespPlan::getResponsiblePerson, bo.getResponsiblePerson());
        lqw.eq(bo.getLevel() != null, DefRespPlan::getLevel, bo.getLevel());
        lqw.eq(bo.getCreateDate() != null, DefRespPlan::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, DefRespPlan::getUpdateDate, bo.getUpdateDate());
        lqw.eq(bo.getDeleted() != null, DefRespPlan::getDeleted, bo.getDeleted());
        // 区域改造：县级/乡镇分层查询（不传则不参与条件）
        lqw.eq(bo.getRegionScopeType() != null, DefRespPlan::getRegionScopeType, bo.getRegionScopeType());
        lqw.eq(bo.getParentDefId() != null, DefRespPlan::getParentDefId, bo.getParentDefId());
        lqw.eq(bo.getCurrentRoundNo() != null, DefRespPlan::getCurrentRoundNo, bo.getCurrentRoundNo());
        if (withOrderBy) {
            lqw.orderByDesc(DefRespPlan::getCreateDate);
        }
        return lqw;
    }

}
