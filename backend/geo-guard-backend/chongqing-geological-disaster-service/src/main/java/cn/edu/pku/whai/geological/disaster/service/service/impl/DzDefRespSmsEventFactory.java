/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataAlarmMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessNodeCategoryEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.Action;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.AlarmSnapshot;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.Source;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespAlarmLevelUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 区域防御响应生命周期短信事件工厂。
 */
class DzDefRespSmsEventFactory {

    private static final int TOWN_ADMIN_REGION_LEVEL = 4;

    private final DzDefRespPlanServiceImpl service;
    private final DataAlarmMapper dataAlarmMapper;
    private final IAdRegionService adRegionService;
    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;

    DzDefRespSmsEventFactory(DzDefRespPlanServiceImpl service,
                             DataAlarmMapper dataAlarmMapper,
                             IAdRegionService adRegionService,
                             IDzTaskProcessChainNodeService taskProcessChainNodeService) {
        this.service = service;
        this.dataAlarmMapper = dataAlarmMapper;
        this.adRegionService = adRegionService;
        this.taskProcessChainNodeService = taskProcessChainNodeService;
    }

    DefRespSmsEventSnapshot buildStartEvent(DefRespPlan countyPlan, Date actionTime) {
        requireCountyPlan(countyPlan);
        Map<String, Integer> afterLevels = captureTownLevels(countyPlan);
        boolean alarmSource = isAlarmActivated(countyPlan);
        return new DefRespSmsEventSnapshot(
            Action.START,
            alarmSource ? Source.ALARM : Source.DIRECT,
            countyPlan.getId(),
            defaultRoundNo(countyPlan.getCurrentRoundNo()),
            toLocalDateTime(actionTime),
            countyPlan.getCounty(),
            Map.of(),
            afterLevels,
            null,
            alarmSource ? loadAlarmSnapshot(countyPlan.getSourceAlarmId()) : null
        );
    }

    DefRespSmsEventSnapshot buildEndEvent(DefRespPlan countyPlan,
                                           Date actionTime,
                                           Map<String, Integer> beforeLevels) {
        requireCountyPlan(countyPlan);
        boolean alarmSource = isAlarmActivated(countyPlan);
        return new DefRespSmsEventSnapshot(
            Action.END,
            alarmSource ? Source.ALARM : Source.DIRECT,
            countyPlan.getId(),
            defaultRoundNo(countyPlan.getCurrentRoundNo()),
            toLocalDateTime(actionTime),
            countyPlan.getCounty(),
            beforeLevels,
            Map.of(),
            alarmSource ? loadAlarmSnapshot(countyPlan.getSourceAlarmId()) : null,
            null
        );
    }

    DefRespSmsEventSnapshot buildAdjustEvent(DefRespPlan countyPlan,
                                              Date actionTime,
                                              Map<String, Integer> beforeLevels,
                                              Map<String, Integer> afterLevels,
                                              Long beforeAlarmId,
                                              Long afterAlarmId) {
        requireCountyPlan(countyPlan);
        boolean alarmSource = afterAlarmId != null && !Objects.equals(beforeAlarmId, afterAlarmId);
        return new DefRespSmsEventSnapshot(
            Action.ADJUST,
            alarmSource ? Source.ALARM : Source.DIRECT,
            countyPlan.getId(),
            defaultRoundNo(countyPlan.getCurrentRoundNo()),
            toLocalDateTime(actionTime),
            countyPlan.getCounty(),
            beforeLevels,
            afterLevels,
            alarmSource ? loadAlarmSnapshot(beforeAlarmId) : null,
            alarmSource ? loadAlarmSnapshot(afterAlarmId) : null
        );
    }

    Map<String, Integer> captureTownLevels(DefRespPlan countyPlan) {
        if (countyPlan == null || countyPlan.getId() == null) {
            return Map.of();
        }
        Map<String, Integer> rawLevels = new LinkedHashMap<>();
        for (DefRespPlan town : service.listTownPlansByCountyId(countyPlan.getId())) {
            if (!service.isTownRowActive(town) || StringUtils.isBlank(town.getStreets()) || town.getLevel() == null) {
                continue;
            }
            rawLevels.putIfAbsent(town.getStreets().trim(), town.getLevel());
        }
        if (rawLevels.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
        for (String street : listOrderedCountyStreets(countyPlan.getCounty())) {
            Integer level = rawLevels.remove(street);
            if (level != null) {
                ordered.put(street, level);
            }
        }
        rawLevels.entrySet().stream()
                 .sorted(Map.Entry.comparingByKey())
                 .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    Map<Long, CountySmsState> captureStartedCountyStates() {
        Map<Long, CountySmsState> states = new LinkedHashMap<>();
        List<DefRespPlan> counties = service.baseMapper.selectList(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<DefRespPlan>lambdaQuery()
                .eq(DefRespPlan::getType, cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum.REGION.getCode())
                .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode())
                .eq(DefRespPlan::getDeleted, 0)
                .in(DefRespPlan::getStatus,
                    cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum.APPROVAL_PASSED.getCode(),
                    cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum.TASK_PUBLISHED.getCode())
        );
        for (DefRespPlan county : counties) {
            if (county == null || county.getId() == null) {
                continue;
            }
            states.put(county.getId(), new CountySmsState(county, captureTownLevels(county), county.getSourceAlarmId()));
        }
        return states;
    }

    private boolean isAlarmActivated(DefRespPlan plan) {
        DzTaskProcessChainNode node = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            plan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_FROM_ALARM.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        if (node != null) {
            return true;
        }
        DzTaskProcessChainNode directNode = taskProcessChainNodeService.resolveSavedNodeByBizAndLinkFast(
            TaskProcessBizTypeEnum.DEF_RESP.getCode(),
            plan.getId(),
            TaskProcessChainNodeTextEnum.REGION_DEF_RESP_START.getLinkName(),
            TaskProcessNodeCategoryEnum.REGION_DEF_RESP_START.getCode()
        );
        return directNode == null && plan.getSourceAlarmId() != null;
    }

    private AlarmSnapshot loadAlarmSnapshot(Long alarmId) {
        if (alarmId == null) {
            return null;
        }
        DataAlarm alarm = dataAlarmMapper.selectById(alarmId);
        if (alarm == null) {
            return null;
        }
        Integer highestWarningLevel = DataAlarmLevelStreetsUtil.resolveHighestLevel(alarm.getLevelStreetsJson());
        Integer sourceType = alarm.getSourceType();
        Integer defenseResponseLevel = DefRespAlarmLevelUtil.resolveDefenseResponseLevel(sourceType, highestWarningLevel);
        return new AlarmSnapshot(
            toLocalDateTimeOrNull(alarm.getValidStartDate()),
            toLocalDateTimeOrNull(alarm.getValidEndDate()),
            alarm.getId() == null ? alarmId : alarm.getId(),
            sourceType,
            highestWarningLevel,
            defenseResponseLevel
        );
    }

    private List<String> listOrderedCountyStreets(String county) {
        if (StringUtils.isBlank(county)) {
            return List.of();
        }
        AdRegionBo bo = new AdRegionBo();
        bo.setCounty(county.trim());
        bo.setLevel(TOWN_ADMIN_REGION_LEVEL);
        List<AdRegionVo> regions = adRegionService.queryList(bo);
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }
        Set<String> ordered = new LinkedHashSet<>();
        regions.stream()
               .filter(Objects::nonNull)
               .sorted(Comparator.comparing(AdRegionVo::getPcode, Comparator.nullsLast(String::compareTo)))
               .map(region -> StringUtils.isNotBlank(region.getStreet()) ? region.getStreet() : region.getName())
               .filter(StringUtils::isNotBlank)
               .map(String::trim)
               .forEach(ordered::add);
        return new ArrayList<>(ordered);
    }

    private void requireCountyPlan(DefRespPlan plan) {
        if (plan == null || plan.getId() == null || !service.isCountyRegionPlan(plan)) {
            throw new IllegalArgumentException("仅县级区域防御响应支持生命周期短信");
        }
    }

    private int defaultRoundNo(Integer roundNo) {
        return roundNo == null || roundNo <= 0 ? 1 : roundNo;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTimeOrNull(date == null ? new Date() : date);
    }

    private LocalDateTime toLocalDateTimeOrNull(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    record CountySmsState(DefRespPlan countyPlan, Map<String, Integer> townLevels, Long sourceAlarmId) {
    }
}
