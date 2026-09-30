/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.RegionDefRespCloseReason;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
class DzDefRespRegionBuildDelegate {

    private static final long DEF_RESP_PLACEHOLDER_HANDLE_ID = DzDefRespPlanServiceImpl.DEF_RESP_PLACEHOLDER_HANDLE_ID;
    private static final int DEF_RESP_PLACEHOLDER_PROCESS = DzDefRespPlanServiceImpl.DEF_RESP_PLACEHOLDER_PROCESS;
    private static final String DEF_RESP_CODE_MARKER_TOWN = DzDefRespPlanServiceImpl.DEF_RESP_CODE_MARKER_TOWN;
    private static final String DEFAULT_REGION_RESP_UNIT = DzDefRespPlanServiceImpl.DEFAULT_REGION_RESP_UNIT;

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IAdRegionService adRegionService;
    private final IDzTaskDistListService dzTaskDistListService;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final IDzProcessProgressService dzProcessProgressService;

    DzDefRespRegionBuildDelegate(DzDefRespPlanServiceImpl service,
                                 IAdRegionService adRegionService,
                                 IDzTaskDistListService dzTaskDistListService,
                                 DzTaskDistListMapper dzTaskDistListMapper,
                                 IDzProcessProgressService dzProcessProgressService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.adRegionService = adRegionService;
        this.dzTaskDistListService = dzTaskDistListService;
        this.dzTaskDistListMapper = dzTaskDistListMapper;
        this.dzProcessProgressService = dzProcessProgressService;
    }

    private List<DefRespPlan> listEffectiveRegionPlans(String county, Integer regionScopeType, Long excludeId) { return service.listEffectiveRegionPlans(county, regionScopeType, excludeId); }

    private List<String> normalizeStreetNames(Collection<String> streets) { return DzDefRespPlanServiceImpl.normalizeStreetNames(streets); }

    private String buildCountyRegionPlanName(String county, Integer level, Date referenceDate) { return service.buildCountyRegionPlanName(county, level, referenceDate); }

    private String buildRegionTriggerConditionByLevel(Integer level) { return service.buildRegionTriggerConditionByLevel(level); }

    private boolean insertByBo(DefRespPlanBo bo, boolean skipRegionConstraint) { return service.insertByBo(bo, skipRegionConstraint); }

    private boolean insertByBo(DefRespPlanBo bo) { return service.insertByBo(bo); }

    private DefRespPlan getDefRespPlanById(Long defId) { return service.getDefRespPlanById(defId); }

    private String resolveAdRegionStreetName(AdRegionVo adRegionVo) { return service.resolveAdRegionStreetName(adRegionVo); }

    private List<DefRespPlan> listTownPlansByCountyId(Long countyDefId) { return service.listTownPlansByCountyId(countyDefId); }

    private Integer resolveTownExecuteStatusForCounty(DefRespPlan county) { return service.resolveTownExecuteStatusForCounty(county); }

    private String buildDefRespPlanCode(String marker) { return service.buildDefRespPlanCode(marker); }

    private Map<String, Integer> resolveAlarmStreetWarningLevelMap(DataAlarmVo alarm) { return service.resolveAlarmStreetWarningLevelMap(alarm); }

    private Integer resolveDefenseResponseLevel(DataAlarmVo alarm, Integer warningLevel) { return service.resolveDefenseResponseLevel(alarm, warningLevel); }

    private boolean isTownRowActive(DefRespPlan town) { return service.isTownRowActive(town); }
    DefRespPlan requireUniqueEffectiveCountyRegionPlan(String county) {
        List<DefRespPlan> effectiveCountyPlans = listEffectiveRegionPlans(county, RegionScopeTypeEnum.COUNTY.getCode(), null);
        if (effectiveCountyPlans.isEmpty()) {
            return null;
        }
        if (effectiveCountyPlans.size() > 1) {
            throw new ServiceException("同一个县存在多条未关闭或归档的县级区域防御响应，无法确定唯一主记录");
        }
        return effectiveCountyPlans.getFirst();
    }

    /**
     * 为乡镇等级更新创建县级方案
     */
    DefRespPlan createCountyRegionPlanForTownLevelUpdate(String county, Integer level, List<String> targetStreets, Date now, Long alarmId) {
        if (StringUtils.isBlank(county) || level == null) {
            throw new ServiceException("新增县级区域防御响应缺少区县或等级");
        }
        List<String> streets = normalizeStreetNames(targetStreets);
        if (streets.isEmpty()) {
            throw new ServiceException("新增县级区域防御响应至少需要关联一个乡镇");
        }
        DefRespPlanBo countyBo = new DefRespPlanBo();
        countyBo.setId(resolveCountyRegionHandleIdFromMeeting());
        countyBo.setCode(resolveCountyRegionPlanCodeFromMeeting());
        countyBo.setName(buildCountyRegionPlanName(county, level, now));
        countyBo.setType(DefRespPlanTypeEnum.REGION.getCode());
        countyBo.setRegionScopeType(RegionScopeTypeEnum.COUNTY.getCode());
        countyBo.setStatus(DefRespPlanStatusEnum.STARTED.getCode());
        countyBo.setExecuteStatus(DefRespExecuteStatusEnum.NOT_STARTED.getCode());
        countyBo.setTriggerCondition(buildRegionTriggerConditionByLevel(level));
        countyBo.setCounty(county);
        countyBo.setStreets("");
        countyBo.setLevel(level);
        countyBo.setResponsibilityUnit(DEFAULT_REGION_RESP_UNIT);
        countyBo.setSourceAlarmId(alarmId);
        countyBo.setCurrentRoundNo(1);
        countyBo.setCreateDate(now);
        countyBo.setUpdateDate(now);
        countyBo.setDeleted(0);
        if (!insertByBo(countyBo, true)) {
            throw new ServiceException("新增县级区域防御响应失败");
        }
        return getDefRespPlanById(countyBo.getId());
    }

    Long resolveCountyRegionHandleIdFromMeeting() {
        MeetingInfoVo meetingInfo = requireCountyDefRespMeetingInfo();
        if (meetingInfo.getHandleId() == null) {
            throw new ServiceException("未找到县级防御响应对应的会议handleId");
        }
        return meetingInfo.getHandleId();
    }

    String resolveCountyRegionPlanCodeFromMeeting() {
        MeetingInfoVo meetingInfo = requireCountyDefRespMeetingInfo();
        if (StringUtils.isBlank(meetingInfo.getPlanCode())) {
            throw new ServiceException("未找到县级防御响应对应的会议planCode");
        }
        return meetingInfo.getPlanCode();
    }

    MeetingInfoVo requireCountyDefRespMeetingInfo() {
        Long meetingId = MeetingRedisCache.getHandleProcess(DEF_RESP_PLACEHOLDER_HANDLE_ID, DEF_RESP_PLACEHOLDER_PROCESS);
        if (meetingId == null) {
            throw new ServiceException("未找到县级防御响应对应的会议");
        }
        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            throw new ServiceException("未找到县级防御响应对应的会议信息");
        }
        return meetingInfo;
    }

    /**
     * 按街道索引有效乡镇方案
     */
    Map<String, List<DefRespPlan>> buildEffectiveTownPlansByStreet(String county) {
        return listEffectiveRegionPlans(county, RegionScopeTypeEnum.TOWN.getCode(), null)
            .stream()
            .filter(townPlan -> StringUtils.isNotBlank(townPlan.getStreets()))
            .collect(Collectors.groupingBy(townPlan -> townPlan.getStreets().trim(), LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 同步乡镇方案继承字段
     */
    boolean syncTownPlanInheritedFields(DefRespPlan townPlan, DefRespPlan targetCounty, Date now) {
        boolean changed = !Objects.equals(townPlan.getParentDefId(), targetCounty.getId())
            || !Objects.equals(townPlan.getTriggerCondition(), targetCounty.getTriggerCondition())
            || !Objects.equals(townPlan.getResponsibilityUnit(), targetCounty.getResponsibilityUnit())
            || !Objects.equals(townPlan.getResponsiblePerson(), targetCounty.getResponsiblePerson())
            || !Objects.equals(townPlan.getResponsiblePersonPhone(), targetCounty.getResponsiblePersonPhone())
            || !Objects.equals(townPlan.getStatus(), targetCounty.getStatus())
            || !Objects.equals(townPlan.getCurrentRoundNo(), targetCounty.getCurrentRoundNo());
        townPlan.setParentDefId(targetCounty.getId());
        townPlan.setTriggerCondition(targetCounty.getTriggerCondition());
        townPlan.setResponsibilityUnit(targetCounty.getResponsibilityUnit());
        townPlan.setResponsiblePerson(targetCounty.getResponsiblePerson());
        townPlan.setResponsiblePersonPhone(targetCounty.getResponsiblePersonPhone());
        townPlan.setStatus(targetCounty.getStatus());
        townPlan.setCurrentRoundNo(targetCounty.getCurrentRoundNo());
        townPlan.setUpdateDate(now);
        return changed;
    }

    /**
     * 按街道名查询乡镇行政区划
     */
    List<AdRegionVo> queryTownAdRegions(List<String> streetNames) {
        if (streetNames == null || streetNames.isEmpty()) {
            return List.of();
        }
        AdRegionBo adRegionBo = new AdRegionBo();
        adRegionBo.setLevel(4);
        return adRegionService.queryList(adRegionBo).stream()
                              .filter(region -> {
                                  String streetName = resolveAdRegionStreetName(region);
                                  return StringUtils.isNotBlank(streetName) && streetNames.contains(streetName.trim());
                              })
                              .toList();
    }

    /**
     * 查询县下全部乡镇行政区划
     */
    List<AdRegionVo> listTownAdRegionsByCounty(String county) {
        if (StringUtils.isBlank(county)) {
            return List.of();
        }
        AdRegionBo adRegionBo = new AdRegionBo();
        adRegionBo.setLevel(4);
        adRegionBo.setCounty(county);
        return adRegionService.queryList(adRegionBo).stream()
                              .filter(region -> StringUtils.isNotBlank(resolveAdRegionStreetName(region)))
                              .toList();
    }

    /**
     * 按县级 id 和街道查找乡镇行
     */
    DefRespPlan findTownByCountyAndStreet(Long countyId, String street) {
        if (countyId == null || StringUtils.isBlank(street)) {
            return null;
        }
        String t = street.trim();
        List<DefRespPlan> towns = listTownPlansByCountyId(countyId);
        for (DefRespPlan p : towns) {
            if (p.getStreets() != null && t.equals(p.getStreets().trim())) {
                return p;
            }
        }
        return null;
    }

    /**
     * 关闭乡镇区域方案
     */
    void closeTownPlan(DefRespPlan town, String reason, Date now) {
        dzTaskDistListService.closeOpenDefRespTasksByDefId(town.getId(), reason);
        town.setExecuteStatus(DefRespExecuteStatusEnum.CLOSED.getCode());
        town.setCloseReason(reason);
        town.setCloseTime(now);
        town.setUpdateDate(now);
        baseMapper.updateById(town);
    }

    /**
     * 在县级下新增乡镇行
     */
    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, String oneStreet, Date now) {
        if (county == null || StringUtils.isBlank(oneStreet)) {
            return null;
        }
        String street = oneStreet.trim();
        DefRespPlanBo townBo = new DefRespPlanBo();
        townBo.setCode(buildDefRespPlanCode(DEF_RESP_CODE_MARKER_TOWN));
        townBo.setName(county.getCounty() + street + "_乡镇区域防御响应");
        townBo.setType(DefRespPlanTypeEnum.REGION.getCode());
        townBo.setRegionScopeType(RegionScopeTypeEnum.TOWN.getCode());
        townBo.setParentDefId(county.getId());
        townBo.setStreets(street);
        townBo.setCounty(county.getCounty());
        townBo.setLevel(county.getLevel());
        Integer countyStatus = county.getStatus();
        townBo.setStatus(countyStatus == null ? DefRespPlanStatusEnum.STARTED.getCode() : countyStatus);
        townBo.setExecuteStatus(resolveTownExecuteStatusForCounty(county));
        townBo.setTriggerCondition(county.getTriggerCondition());
        townBo.setResponsibilityUnit(county.getResponsibilityUnit());
        townBo.setResponsiblePerson(county.getResponsiblePerson());
        townBo.setResponsiblePersonPhone(county.getResponsiblePersonPhone());
        townBo.setCurrentRoundNo(county.getCurrentRoundNo() == null ? 1 : county.getCurrentRoundNo());
        townBo.setSourceAlarmId(county.getSourceAlarmId());
        townBo.setCreateDate(now);
        townBo.setUpdateDate(now);
        townBo.setDeleted(0);
        if (!insertByBo(townBo)) {
            log.warn("插入乡镇区域防御响应失败, countyId={}, street={}", county.getId(), street);
            return null;
        }
        return getDefRespPlanById(townBo.getId());
    }

    /**
     * 按预警在县级下新增乡镇行
     */
    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county, DataAlarmVo alarm, String oneStreet, Date now) {
        Integer warningLevel = resolveAlarmStreetWarningLevelMap(alarm).get(oneStreet.trim());
        Integer defRespLevel = resolveDefenseResponseLevel(alarm, warningLevel);
        return insertTownPlanUnderCounty(county, alarm, oneStreet, defRespLevel, now);
    }

    DefRespPlan insertTownPlanUnderCounty(DefRespPlan county,
                                                  DataAlarmVo alarm,
                                                  String oneStreet,
                                                  Integer defRespLevel,
                                                  Date now) {
        DefRespPlanBo townBo = new DefRespPlanBo();
        townBo.setCode(buildDefRespPlanCode(DEF_RESP_CODE_MARKER_TOWN));
        townBo.setName(alarm.getCity() + oneStreet.trim() + "_乡镇区域防御响应");
        townBo.setType(DefRespPlanTypeEnum.REGION.getCode());
        townBo.setRegionScopeType(RegionScopeTypeEnum.TOWN.getCode());
        townBo.setParentDefId(county.getId());
        townBo.setStreets(oneStreet.trim());
        townBo.setCounty(alarm.getCity());
        townBo.setLevel(defRespLevel);
        Integer countyStatus = county.getStatus();
        townBo.setStatus(countyStatus == null ? DefRespPlanStatusEnum.STARTED.getCode() : countyStatus);
        townBo.setExecuteStatus(resolveTownExecuteStatusForCounty(county));
        townBo.setTriggerCondition("湖北省" + alarm.getSource() + "的"
            + LevelCodeUtil.resolveDefenseResponseRomanLevel(defRespLevel) + "预警");
        townBo.setResponsibilityUnit(county.getResponsibilityUnit());
        townBo.setResponsiblePerson(county.getResponsiblePerson());
        townBo.setResponsiblePersonPhone(county.getResponsiblePersonPhone());
        townBo.setCurrentRoundNo(county.getCurrentRoundNo() == null ? 1 : county.getCurrentRoundNo());
        townBo.setCreateDate(now);
        townBo.setUpdateDate(now);
        townBo.setDeleted(0);
        if (!insertByBo(townBo)) {
            log.warn("插入乡镇区域防御响应失败, countyId={}, street={}", county.getId(), oneStreet);
            return null;
        }
        return getDefRespPlanById(townBo.getId());
    }

    /**
     * 归档县级下有效乡镇行
     */
    void archiveTownPlansUnderCounty(Long countyDefId, Date date) {
        if (countyDefId == null) {
            return;
        }
        for (DefRespPlan town : listTownPlansByCountyId(countyDefId)) {
            if (!isTownRowActive(town)) {
                continue;
            }
            dzTaskDistListService.closeOpenDefRespTasksByDefId(town.getId(), RegionDefRespCloseReason.TOWN_ARCHIVE_WITH_COUNTY);
            long total = dzTaskDistListMapper.selectCount(
                Wrappers.<DzTaskDistList>lambdaQuery()
                        .eq(DzTaskDistList::getDefId, town.getId())
                        .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                        .eq(DzTaskDistList::getDelete, 0)
            );
            long completed = dzTaskDistListMapper.selectCount(
                Wrappers.<DzTaskDistList>lambdaQuery()
                        .eq(DzTaskDistList::getDefId, town.getId())
                        .eq(DzTaskDistList::getSourceType, DzTaskDistList.SOURCE_TYPE_DEF_RESP)
                        .eq(DzTaskDistList::getDelete, 0)
                        .eq(DzTaskDistList::getStatus, DzTaskDistList.STATUS_FEEDBACKED)
                        .in(DzTaskDistList::getTaskSource,
                            DzTaskDistList.PLAN_NAME_MONITOR,
                            DzTaskDistList.PLAN_NAME_PATROL)
            );
            town.setTaskPublishTotal(total);
            town.setTaskCompleteTotal(completed);
            town.setStatus(DefRespPlanStatusEnum.ENDED.getCode());
            town.setExecuteStatus(DefRespExecuteStatusEnum.ENDED.getCode());
            town.setCloseTime(date);
            town.setCloseReason(RegionDefRespCloseReason.TOWN_ARCHIVE_WITH_COUNTY);
            town.setUpdateDate(date);
            baseMapper.updateById(town);
            DzProcessProgressBo pp = new DzProcessProgressBo();
            pp.setDefId(town.getId());
            pp.setStatus(DefRespPlanStatusEnum.ENDED.getCode());
            pp.setCreateDate(date);
            dzProcessProgressService.insertByBo(pp);
        }
    }

}
