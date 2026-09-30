/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataAlarmMapper;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanChildGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanStreetGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.GeoAdviceAlarmMatch;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class DzDefRespGeoAdviceDelegate {

    private static final int ALARM_SOURCE_TYPE_MANUAL = 1;
    private static final int ALARM_SOURCE_TYPE_REPORT_ANALYSIS = 2;
    private static final int ALARM_STATUS_UNTRIGGERED = 0;
    private static final int ALARM_STATUS_EXPIRED = 2;
    private static final int GEO_ADVICE_ALARM_LOOKUP_DAYS = 30;
    private static final long GEO_ADVICE_ALARM_LOOKUP_MILLIS = GEO_ADVICE_ALARM_LOOKUP_DAYS * 24L * 60 * 60 * 1000;
    private static final List<Integer> GEO_ADVICE_SOURCE_TYPES = List.of(
        ALARM_SOURCE_TYPE_REPORT_ANALYSIS,
        ALARM_SOURCE_TYPE_MANUAL
    );
    private static final String ENSHI_CITY = "恩施市";

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final DataAlarmMapper dataAlarmMapper;

    DzDefRespGeoAdviceDelegate(DzDefRespPlanServiceImpl service) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dataAlarmMapper = service.dataAlarmMapper;
    }

    public List<DefRespPlanChildGeoAdviceVo> listChildGeoAdvice(Long alarmId) {
        DefRespPlan parent = listGeoAdviceParentPlans();
        Map<String, DefRespPlan> childByStreet = ObjUtil.isEmpty(parent)
            ? Map.of()
            : buildChildGeoAdvicePlanByStreet(listGeoAdviceChildPlans(parent));
        Map<String, GeoAdviceAlarmMatch> adviceByStreet = buildChildGeoAdviceAlarmAdviceByStreet(alarmId);

        LinkedHashSet<String> orderedStreets = new LinkedHashSet<>();
        if (alarmId != null) {
            // 指定预警时仅返回有效地象建议等级的乡镇，剔除无法映射防御响应等级的街道。
            orderedStreets.addAll(adviceByStreet.keySet());
        } else {
            orderedStreets.addAll(childByStreet.keySet());
        }
        if (orderedStreets.isEmpty()) {
            return List.of();
        }
        return orderedStreets.stream()
                             .map(street -> buildChildGeoAdviceVo(street,
                                 childByStreet.get(street),
                                 adviceByStreet.get(street)))
                             .toList();
    }

    /**
     * 按乡镇/街道列表查询地象建议
     */
    public List<DefRespPlanStreetGeoAdviceVo> queryStreetGeoAdvice(List<String> streets) {
        if (streets == null || streets.isEmpty()) {
            throw new ServiceException("streets不能为空");
        }
        List<String> normalizedStreets = DzDefRespPlanServiceImpl.normalizeStreetNames(streets);
        if (normalizedStreets.isEmpty()) {
            throw new ServiceException("streets不能为空");
        }
        List<String> enshiTownStreets = null;
        Map<String, List<String>> queryStreetsByInput = new LinkedHashMap<>();
        LinkedHashSet<String> allQueryStreets = new LinkedHashSet<>();
        for (String street : normalizedStreets) {
            if (enshiTownStreets == null && Objects.equals(street, ENSHI_CITY)) {
                enshiTownStreets = loadEnshiTownStreets();
            }
            List<String> queryStreets = resolveGeoAdviceQueryStreets(street, enshiTownStreets);
            queryStreetsByInput.put(street, queryStreets);
            allQueryStreets.addAll(queryStreets);
        }
        Map<String, GeoAdviceAlarmMatch> adviceByQueryStreet = buildBestGeoAdviceByStreet(allQueryStreets, new Date());
        Map<String, Integer> townLevelByStreet = batchResolveTownLevels(allQueryStreets);
        return normalizedStreets.stream()
                                .map(street -> buildStreetGeoAdviceItem(
                                    street,
                                    queryStreetsByInput.get(street),
                                    adviceByQueryStreet,
                                    townLevelByStreet))
                                .toList();
    }

    /**
     * 组装单街道地象建议项（复用请求级预警与等级缓存）。
     */
    private DefRespPlanStreetGeoAdviceVo buildStreetGeoAdviceItem(String street,
                                                                  List<String> queryStreets,
                                                                  Map<String, GeoAdviceAlarmMatch> adviceByQueryStreet,
                                                                  Map<String, Integer> townLevelByStreet) {
        String normalizedStreet = StringUtils.trim(street);
        if (StringUtils.isBlank(normalizedStreet)) {
            throw new ServiceException("street不能为空");
        }
        Integer currentLevel = resolveStreetCurrentTownLevel(queryStreets, normalizedStreet, townLevelByStreet);
        GeoAdviceAlarmMatch match = buildBestGeoAdviceMatch(queryStreets, adviceByQueryStreet);
        if (match == null) {
            DefRespPlanStreetGeoAdviceVo defRespPlanStreetGeoAdviceVo = new DefRespPlanStreetGeoAdviceVo();
            defRespPlanStreetGeoAdviceVo.setStreets(normalizedStreet);
            defRespPlanStreetGeoAdviceVo.setLevel(currentLevel);
            return defRespPlanStreetGeoAdviceVo;
        }
        return buildStreetGeoAdviceVo(normalizedStreet, currentLevel, match);
    }

    /**
     * 加载恩施市下全部乡镇街道名称。
     */
    private List<String> loadEnshiTownStreets() {
        List<String> townStreets = service.listTownAdRegionsByCounty(ENSHI_CITY).stream()
                                                                        .map(service::resolveAdRegionStreetName)
                                                                        .filter(StringUtils::isNotBlank)
                                                                        .map(String::trim)
                                                                        .distinct()
                                                                        .toList();
        if (townStreets.isEmpty()) {
            throw new ServiceException("未找到恩施市下的乡镇行政区划");
        }
        return townStreets;
    }

    /**
     * 解析地象建议查询涉及的街道；恩施市场景可复用已加载的乡镇列表。
     */
    private List<String> resolveGeoAdviceQueryStreets(String street, List<String> enshiTownStreets) {
        if (StringUtils.isBlank(street)) {
            return List.of();
        }
        if (!Objects.equals(street, ENSHI_CITY)) {
            return List.of(street);
        }
        return enshiTownStreets == null ? loadEnshiTownStreets() : enshiTownStreets;
    }

    /**
     * 查询地象建议父节点范围
     */
    private DefRespPlan listGeoAdviceParentPlans() {
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.<DefRespPlan>lambdaQuery()
                                                      .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                                                      .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode())
                                                      .in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes())
                                                      .in(DefRespPlan::getExecuteStatus,
                                                          DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                                                          DefRespExecuteStatusEnum.RUNNING.getCode())
                                                      .eq(DefRespPlan::getDeleted, 0);
        DefRespPlan parent = baseMapper.selectOne(lqw);
        if (ObjUtil.isEmpty(parent)) {
            return new DefRespPlan();
        }
        return parent;
    }

    /**
     * 查询父节点下乡镇子节点
     */
    private List<DefRespPlan> listGeoAdviceChildPlans(DefRespPlan parents) {
        Long parentId = parents.getId();
        if (parentId == null) {
            return List.of();
        }
        LambdaQueryWrapper<DefRespPlan> lqw = Wrappers.<DefRespPlan>lambdaQuery()
                                                      .eq(DefRespPlan::getParentDefId, parentId)
                                                      .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                                                      .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                                                      .eq(DefRespPlan::getDeleted, 0)
                                                      .in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes())
                                                      .in(DefRespPlan::getExecuteStatus,
                                                          DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                                                          DefRespExecuteStatusEnum.RUNNING.getCode());
        return baseMapper.selectList(lqw);
    }

    /**
     * 解析街道当前乡镇响应等级
     */
    private Integer resolveStreetCurrentTownLevel(List<String> streets,
                                                  String originalStreet,
                                                  Map<String, Integer> townLevelByStreet) {
        List<Integer> levels = streets.stream()
                                      .map(street -> townLevelByStreet.get(StringUtils.trim(street)))
                                      .filter(Objects::nonNull)
                                      .distinct()
                                      .toList();
        if (levels.isEmpty()) {
            return null;
        }
        if (Objects.equals(originalStreet, ENSHI_CITY)) {
            return levels.stream().min(Integer::compareTo).orElse(null);
        }
        return levels.getFirst();
    }

    /**
     * 批量解析乡镇当前响应等级，单次查询后按街道索引。
     */
    private Map<String, Integer> batchResolveTownLevels(Set<String> streets) {
        if (streets == null || streets.isEmpty()) {
            return Map.of();
        }
        List<String> normalizedStreets = DzDefRespPlanServiceImpl.normalizeStreetNames(streets);
        if (normalizedStreets.isEmpty()) {
            return Map.of();
        }
        List<DefRespPlan> townPlans = baseMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes())
                    .in(DefRespPlan::getStreets, normalizedStreets)
                    .in(DefRespPlan::getExecuteStatus,
                        DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                        DefRespExecuteStatusEnum.RUNNING.getCode())
        );
        if (townPlans == null || townPlans.isEmpty()) {
            return Map.of();
        }
        Map<String, DefRespPlan> latestPlanByStreet = new LinkedHashMap<>();
        for (DefRespPlan plan : townPlans) {
            if (plan == null || StringUtils.isBlank(plan.getStreets())) {
                continue;
            }
            String street = plan.getStreets().trim();
            latestPlanByStreet.merge(street, plan, this::chooseLatestTownPlan);
        }
        Map<String, Integer> levelByStreet = new LinkedHashMap<>();
        latestPlanByStreet.forEach((street, plan) -> levelByStreet.put(street, plan.getLevel()));
        return levelByStreet;
    }

    DefRespPlan chooseLatestTownPlan(DefRespPlan left, DefRespPlan right) {
        Date leftUpdateDate = left.getUpdateDate();
        Date rightUpdateDate = right.getUpdateDate();
        if (leftUpdateDate == null && rightUpdateDate == null) {
            return ObjUtil.defaultIfNull(right.getId(), Long.MIN_VALUE) > ObjUtil.defaultIfNull(left.getId(), Long.MIN_VALUE)
                ? right
                : left;
        }
        if (leftUpdateDate == null) {
            return right;
        }
        if (rightUpdateDate == null) {
            return left;
        }
        if (!Objects.equals(leftUpdateDate, rightUpdateDate)) {
            return rightUpdateDate.after(leftUpdateDate) ? right : left;
        }
        return ObjUtil.defaultIfNull(right.getId(), Long.MIN_VALUE) > ObjUtil.defaultIfNull(left.getId(), Long.MIN_VALUE)
            ? right
            : left;
    }

    /**
     * 从街道列表中取最佳地象建议（复用请求级街道索引）。
     */
    private GeoAdviceAlarmMatch buildBestGeoAdviceMatch(List<String> streets,
                                                        Map<String, GeoAdviceAlarmMatch> adviceByQueryStreet) {
        if (streets == null || streets.isEmpty() || adviceByQueryStreet == null || adviceByQueryStreet.isEmpty()) {
            return null;
        }
        return streets.stream()
                      .map(street -> adviceByQueryStreet.get(StringUtils.trim(street)))
                      .filter(Objects::nonNull)
                      .reduce(this::chooseHigherAdvice)
                      .orElse(null);
    }

    /**
     * 按街道计算最佳地象建议：优先有效期内预警，否则取最近未来且 status=0 的预警；同街道取等级高者。
     */
    private Map<String, GeoAdviceAlarmMatch> buildBestGeoAdviceByStreet(Set<String> targetStreetSet, Date now) {
        if (targetStreetSet == null || targetStreetSet.isEmpty()) {
            return Map.of();
        }
        List<DataAlarm> candidates = loadGeoAdviceCandidateAlarms(now);
        Map<String, GeoAdviceAlarmMatch> bestByStreet = new LinkedHashMap<>();
        for (DataAlarm alarm : candidates) {
            if (!isGeoAdviceStrictActivePeriod(alarm, now)) {
                continue;
            }
            mergeGeoAdviceMatchesForAlarm(bestByStreet, alarm, targetStreetSet);
        }
        Set<String> missingStreets = targetStreetSet.stream()
                                                    .filter(street -> !bestByStreet.containsKey(street))
                                                    .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingStreets.isEmpty()) {
            for (String street : missingStreets) {
                chooseNearestFutureGeoAdviceForStreet(street, candidates, now)
                    .ifPresent(match -> bestByStreet.put(street, match));
            }
        }
        return bestByStreet;
    }

    /**
     * 加载地象建议候选预警（不含已过期 status=2 及 valid_end 已过的记录）。
     */
    private List<DataAlarm> loadGeoAdviceCandidateAlarms(Date now) {
        Date futureLimit = new Date(now.getTime() + GEO_ADVICE_ALARM_LOOKUP_MILLIS);
        return dataAlarmMapper.selectList(
            Wrappers.<DataAlarm>lambdaQuery()
                    .in(DataAlarm::getSourceType, GEO_ADVICE_SOURCE_TYPES)
                    .ne(DataAlarm::getStatus, ALARM_STATUS_EXPIRED)
                    .ge(DataAlarm::getValidEndDate, now)
                    .and(wrapper -> wrapper
                        .nested(active -> active.lt(DataAlarm::getValidStartDate, now).gt(DataAlarm::getValidEndDate, now))
                        .or(future -> future.ge(DataAlarm::getValidStartDate, now)
                                            .le(DataAlarm::getValidStartDate, futureLimit)
                                            .eq(DataAlarm::getStatus, ALARM_STATUS_UNTRIGGERED))
                    )
        );
    }

    private boolean isGeoAdviceStrictActivePeriod(DataAlarm alarm, Date now) {
        Date validStartDate = alarm.getValidStartDate();
        Date validEndDate = alarm.getValidEndDate();
        return validStartDate != null && validEndDate != null
            && validStartDate.before(now) && validEndDate.after(now);
    }

    private void mergeGeoAdviceMatchesForAlarm(Map<String, GeoAdviceAlarmMatch> bestByStreet,
                                               DataAlarm alarm,
                                               Set<String> targetStreetSet) {
        Map<String, Integer> streetLevels = DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(alarm.getLevelStreetsJson());
        for (String street : targetStreetSet) {
            Integer warningLevel = streetLevels.get(street);
            if (warningLevel == null) {
                continue;
            }
            Integer geoAdviceLevel = resolveGeoAdviceLevel(alarm, warningLevel);
            if (geoAdviceLevel == null) {
                continue;
            }
            bestByStreet.merge(street, new GeoAdviceAlarmMatch(alarm, geoAdviceLevel), this::chooseHigherAdvice);
        }
    }

    private Optional<GeoAdviceAlarmMatch> chooseNearestFutureGeoAdviceForStreet(String street,
                                                                                List<DataAlarm> candidates,
                                                                                Date now) {
        GeoAdviceAlarmMatch best = null;
        for (DataAlarm alarm : candidates) {
            if (!Objects.equals(alarm.getStatus(), ALARM_STATUS_UNTRIGGERED)) {
                continue;
            }
            Date validStartDate = alarm.getValidStartDate();
            if (validStartDate == null || !validStartDate.after(now)) {
                continue;
            }
            Integer warningLevel = DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(alarm.getLevelStreetsJson()).get(street);
            if (warningLevel == null) {
                continue;
            }
            Integer geoAdviceLevel = resolveGeoAdviceLevel(alarm, warningLevel);
            if (geoAdviceLevel == null) {
                continue;
            }
            GeoAdviceAlarmMatch candidate = new GeoAdviceAlarmMatch(alarm, geoAdviceLevel);
            if (best == null) {
                best = candidate;
                continue;
            }
            int startCompare = compareValidStartDateAsc(best.alarm(), candidate.alarm());
            if (startCompare > 0) {
                best = candidate;
            } else if (startCompare == 0) {
                best = chooseHigherAdvice(best, candidate);
            }
        }
        return Optional.ofNullable(best);
    }

    private int compareValidStartDateAsc(DataAlarm left, DataAlarm right) {
        Date leftValidStartDate = left.getValidStartDate();
        Date rightValidStartDate = right.getValidStartDate();
        if (leftValidStartDate == null) {
            return rightValidStartDate == null ? 0 : 1;
        }
        if (rightValidStartDate == null) {
            return -1;
        }
        return leftValidStartDate.compareTo(rightValidStartDate);
    }

    /**
     * 按预警等级映射为真实防御响应等级。
     */
    private Integer resolveGeoAdviceLevel(DataAlarm alarm, Integer warningLevel) {
        if (alarm == null || warningLevel == null) {
            return null;
        }
        DataAlarmVo dataAlarmVo = BeanUtil.copyProperties(alarm, DataAlarmVo.class);
        return service.resolveDefenseResponseLevel(dataAlarmVo, warningLevel);
    }

    /**
     * 同等级预警中取创建时间更晚的
     */
    private GeoAdviceAlarmMatch chooseLaterAlarm(GeoAdviceAlarmMatch left, GeoAdviceAlarmMatch right) {
        Date leftCreateDate = left.alarm().getCreateDate();
        Date rightCreateDate = right.alarm().getCreateDate();
        if (leftCreateDate == null) {
            return right;
        }
        if (rightCreateDate == null) {
            return left;
        }
        return rightCreateDate.after(leftCreateDate) ? right : left;
    }

    /**
     * 同街道候选中取建议等级更高的
     */
    private GeoAdviceAlarmMatch chooseHigherAdvice(GeoAdviceAlarmMatch left, GeoAdviceAlarmMatch right) {
        int compare = Integer.compare(left.geoAdviceLevel(), right.geoAdviceLevel());
        if (compare > 0) {
            return left;
        }
        if (compare < 0) {
            return right;
        }
        return chooseLaterAlarm(left, right);
    }

    /**
     * 按街道索引乡镇子节点方案
     */
    private Map<String, DefRespPlan> buildChildGeoAdvicePlanByStreet(List<DefRespPlan> children) {
        if (children == null || children.isEmpty()) {
            return Map.of();
        }
        Map<String, DefRespPlan> childByStreet = new LinkedHashMap<>();
        for (DefRespPlan child : children) {
            for (String street : DzDefRespPlanServiceImpl.splitStreets(child.getStreets())) {
                childByStreet.merge(street, child, this::choosePreferredChildGeoAdvicePlan);
            }
        }
        return childByStreet;
    }

    /**
     * 根据 alarmId 构建子节点地象建议。
     */
    private Map<String, GeoAdviceAlarmMatch> buildChildGeoAdviceAlarmAdviceByStreet(Long alarmId) {
        if (alarmId == null) {
            return Map.of();
        }
        DataAlarm alarm = dataAlarmMapper.selectById(alarmId);
        if (alarm == null || StringUtils.isBlank(alarm.getLevelStreetsJson())) {
            return Map.of();
        }
        Map<String, Integer> levelByStreet = DataAlarmLevelStreetsUtil.toStreetHighestLevelMap(alarm.getLevelStreetsJson());
        if (levelByStreet.isEmpty()) {
            return Map.of();
        }
        Map<String, GeoAdviceAlarmMatch> adviceByStreet = new LinkedHashMap<>();
        levelByStreet.forEach((street, warningLevel) -> {
            Integer geoAdviceLevel = resolveGeoAdviceLevel(alarm, warningLevel);
            if (StringUtils.isBlank(street) || geoAdviceLevel == null) {
                return;
            }
            adviceByStreet.put(street.trim(), new GeoAdviceAlarmMatch(alarm, geoAdviceLevel));
        });
        return adviceByStreet;
    }

    /**
     * 同街道冲突时选择优先子节点
     */
    private DefRespPlan choosePreferredChildGeoAdvicePlan(DefRespPlan left, DefRespPlan right) {
        int levelCompare = Integer.compare(
            ObjUtil.defaultIfNull(left.getLevel(), Integer.MIN_VALUE),
            ObjUtil.defaultIfNull(right.getLevel(), Integer.MIN_VALUE)
        );
        if (levelCompare > 0) {
            return left;
        }
        if (levelCompare < 0) {
            return right;
        }
        Date leftUpdateDate = left.getUpdateDate();
        Date rightUpdateDate = right.getUpdateDate();
        if (leftUpdateDate == null && rightUpdateDate == null) {
            return ObjUtil.defaultIfNull(right.getId(), Long.MIN_VALUE) > ObjUtil.defaultIfNull(left.getId(), Long.MIN_VALUE)
                ? right
                : left;
        }
        if (leftUpdateDate == null) {
            return right;
        }
        if (rightUpdateDate == null) {
            return left;
        }
        if (!Objects.equals(leftUpdateDate, rightUpdateDate)) {
            return rightUpdateDate.after(leftUpdateDate) ? right : left;
        }
        return ObjUtil.defaultIfNull(right.getId(), Long.MIN_VALUE) > ObjUtil.defaultIfNull(left.getId(), Long.MIN_VALUE)
            ? right
            : left;
    }

    /**
     * 组装子节点地象建议返回对象
     */
    private DefRespPlanChildGeoAdviceVo buildChildGeoAdviceVo(String street, DefRespPlan child, GeoAdviceAlarmMatch match) {
        DefRespPlanChildGeoAdviceVo vo = new DefRespPlanChildGeoAdviceVo();
        vo.setStreets(street);
        if (child != null) {
            vo.setDefId(child.getId());
            vo.setLevel(child.getLevel());
        }
        if (match != null) {
            applyGeoAdviceMatch(vo::setGeoAdviceLevel, vo::setAlarmId, vo::setAlarmCreateDate, match);
        }
        return vo;
    }

    /**
     * 组装街道地象建议返回对象
     */
    private DefRespPlanStreetGeoAdviceVo buildStreetGeoAdviceVo(String street, Integer currentLevel, GeoAdviceAlarmMatch match) {
        DefRespPlanStreetGeoAdviceVo vo = new DefRespPlanStreetGeoAdviceVo();
        vo.setStreets(street);
        vo.setLevel(currentLevel);
        if (match != null) {
            applyGeoAdviceMatch(vo::setGeoAdviceLevel, vo::setAlarmId, vo::setAlarmCreateDate, match);
        }
        return vo;
    }

    private void applyGeoAdviceMatch(Consumer<Integer> geoAdviceLevelSetter,
                                     Consumer<Long> alarmIdSetter,
                                     Consumer<Date> alarmCreateDateSetter,
                                     GeoAdviceAlarmMatch match) {
        if (match == null || match.alarm() == null) {
            return;
        }
        DataAlarm alarm = match.alarm();
        geoAdviceLevelSetter.accept(match.geoAdviceLevel());
        alarmIdSetter.accept(alarm.getId());
        alarmCreateDateSetter.accept(alarm.getCreateDate());
    }


}
