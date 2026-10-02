/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentInfoBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentQueryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 斜坡单元风险评估Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzRiskAssessmentServiceImpl implements IDzRiskAssessmentService {

    private static final String DYNAMIC_RISK_SUGGEST_CONDITION =
        "string_to_array(dynamic_risk_suggest, ',') && string_to_array({0}, ',')";

    private final DzRiskAssessmentMapper baseMapper;
    private final ISlopeUnitService slopeUnitService;
    private final DzTaskDistListMapper dzTaskDistListMapper;
    private final DzReportDisasterMapper dzReportDisasterMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final AdRegionMapper adRegionMapper;

    /**
     * 查询斜坡单元风险评估
     *
     * @param id 主键
     * @return 斜坡单元风险评估
     */
    @Override
    public DzRiskAssessmentVo queryById(Long id) {
        DzRiskAssessmentVo vo = baseMapper.selectVoById(id);
        vo.setSlopeUnit(slopeUnitService.queryById(vo.getSlopeUnitId()));
        return vo;
    }

    /**
     * 分页查询斜坡单元风险评估列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 斜坡单元风险评估分页列表
     */
    @Override
    public TableDataInfo<DzRiskAssessmentVo> queryPageList(DzRiskAssessmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzRiskAssessment> lqw = buildQueryWrapper(bo);
        Page<DzRiskAssessmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillSlopeUnitIfNeeded(result.getRecords(), bo.getWithSlopeUnit());
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzRiskAssessmentVo> queryTodayTemDynamicRiskList() {
        Date now = new Date();
        LambdaQueryWrapper<DzRiskAssessment> lqw = Wrappers.<DzRiskAssessment>lambdaQuery()
            .isNotNull(DzRiskAssessment::getTemDynamicRiskLevel)
            .between(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(now), DateUtil.endOfDay(now))
            .orderByDesc(DzRiskAssessment::getCreateDate, DzRiskAssessment::getId);
        List<DzRiskAssessmentVo> records = baseMapper.selectVoList(lqw);
        fillSlopeUnitIfNeeded(records, true);
        return records;
    }

    private void fillSlopeUnitIfNeeded(List<DzRiskAssessmentVo> records, Boolean withSlopeUnit) {
        if (!Boolean.TRUE.equals(withSlopeUnit) || records == null || records.isEmpty()) {
            return;
        }
        List<String> slopeUnitIds = records.stream()
            .map(DzRiskAssessmentVo::getSlopeUnitId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (slopeUnitIds.isEmpty()) {
            return;
        }
        Map<String, SlopeUnitVo> slopeUnitMap = slopeUnitService.listPoByIds(slopeUnitIds).stream()
            .map(item -> BeanUtil.copyProperties(item, SlopeUnitVo.class))
            .collect(Collectors.toMap(SlopeUnitVo::getId, item -> item, (o1, o2) -> o1));
        records.forEach(record -> record.setSlopeUnit(slopeUnitMap.get(record.getSlopeUnitId())));
    }


    private LambdaQueryWrapper<DzRiskAssessment> buildQueryWrapper(DzRiskAssessmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzRiskAssessment> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getBatchId() != null, DzRiskAssessment::getBatchId, bo.getBatchId());
        lqw.like(StringUtils.isNotBlank(bo.getSlopeUnitId()), DzRiskAssessment::getSlopeUnitId, bo.getSlopeUnitId());
        lqw.eq(bo.getSusceptibility() != null, DzRiskAssessment::getSusceptibility, bo.getSusceptibility());
        lqw.eq(bo.getHazard() != null, DzRiskAssessment::getHazard, bo.getHazard());
        lqw.eq(bo.getVulnerability() != null, DzRiskAssessment::getVulnerability, bo.getVulnerability());
        lqw.eq(bo.getRisk() != null, DzRiskAssessment::getRisk, bo.getRisk());
        lqw.eq(bo.getDynamicRiskLevel() != null, DzRiskAssessment::getDynamicRiskLevel, bo.getDynamicRiskLevel());
        lqw.eq(bo.getDynamicRiskValue() != null, DzRiskAssessment::getDynamicRiskValue, bo.getDynamicRiskValue());
        lqw.eq(bo.getSusceptibilityLevel() != null, DzRiskAssessment::getSusceptibilityLevel, bo.getSusceptibilityLevel());
        lqw.eq(bo.getHazardLevel() != null, DzRiskAssessment::getHazardLevel, bo.getHazardLevel());
        lqw.eq(bo.getVulnerabilityLevel() != null, DzRiskAssessment::getVulnerabilityLevel, bo.getVulnerabilityLevel());
        lqw.eq(bo.getRiskLevel() != null, DzRiskAssessment::getRiskLevel, bo.getRiskLevel());

        lqw.in(bo.getDynamicRiskLevels() != null, DzRiskAssessment::getDynamicRiskLevel, bo.getDynamicRiskLevels());
        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzRiskAssessment::getCreateDate,
                DateUtil.parseDateTime((String) params.get("beginTime")),
                DateUtil.parseDateTime((String) params.get("endTime")));
        } else {
            lqw.ge(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(new Date()));
            lqw.le(DzRiskAssessment::getCreateDate, DateUtil.endOfDay(new Date()));
        }

        appendDynamicRiskSuggestCondition(lqw, bo.getDynamicRiskSuggest());

        return lqw;
    }

    static void appendDynamicRiskSuggestCondition(LambdaQueryWrapper<DzRiskAssessment> lqw, String dynamicRiskSuggest) {
        List<String> dynamicRiskSuggestList = normalizeDynamicRiskSuggests(dynamicRiskSuggest);
        lqw.apply(!dynamicRiskSuggestList.isEmpty(),
            DYNAMIC_RISK_SUGGEST_CONDITION,
            String.join(",", dynamicRiskSuggestList));
    }

    private static List<String> normalizeDynamicRiskSuggests(String dynamicRiskSuggest) {
        return StringUtils.splitList(dynamicRiskSuggest).stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
    }

    // 创建包含所有风险级别的映射
    Map<Integer, String> riskLevelMap = Map.of(
        0, "无风险",
        1, "低风险",
        2, "中风险",
        3, "高风险",
        4, "极高风险"
    );

    @Override
    public DzRiskAssessmentQueryVo queryRiskList(DzRiskAssessmentQueryBo bo) {
        QueryWrapper<DzRiskAssessment> lqw = Wrappers.query();
        lqw.like(StringUtils.isNotBlank(bo.getArea()), "concat(t2.province, t2.city, t2.county, t2.street, t2.village, t2.community)", bo.getArea());
        lqw.in(bo.getRiskLevels() != null && !bo.getRiskLevels().isEmpty(), "dynamic_risk_level", bo.getRiskLevels());
        Date startTime = bo.getStartTime();
        Date endTime = bo.getEndTime();
        if (bo.getStartTime() == null && bo.getEndTime() == null) {
            startTime = DateUtil.beginOfDay(new Date());
            endTime = DateUtil.endOfDay(new Date());
        }

        lqw.ge("create_date", startTime);
        lqw.le("create_date", endTime);

        if (bo.getPilotArea1() == null && bo.getPilotArea2() == null) {
            lqw.eq("t2.pilot_area_1", 1);
        } else {
            lqw.eq(bo.getPilotArea1() != null, "t2.pilot_area_1", bo.getPilotArea1());
            lqw.eq(bo.getPilotArea2() != null, "t2.pilot_area_2", bo.getPilotArea2());
        }
        List<DzRiskAssessmentVo> dzRiskAssessmentVos = baseMapper.selectRiskAndSlopeUnit(lqw);
        Map<Integer, List<DzRiskAssessmentVo>> longListMap = CollStreamUtil.groupByKey(dzRiskAssessmentVos, DzRiskAssessmentVo::getDynamicRiskLevel);

        Map<String, Object> resultMap = new HashMap<>();

        // 计算时间范围内的所有日期
        List<String> dateList = DateUtil.rangeToList(startTime, endTime, DateField.DAY_OF_MONTH).stream()
            .map(DateUtil::formatDate)
            .distinct()
            .toList();

        // 对每个风险级别进行处理
        for (Map.Entry<Integer, String> entry : riskLevelMap.entrySet()) {
            Integer level = entry.getKey();
            String levelName = entry.getValue();

            boolean isRequested = bo.getRiskLevels() != null && bo.getRiskLevels().contains(level);
            if (!isRequested) {
                continue;
            }

            List<DzRiskAssessmentVo> list = longListMap.getOrDefault(level, List.of());


            // 按日期统计
            Map<String, Long> statMap = list.stream()
                .collect(Collectors.groupingBy(
                    item -> DateUtil.formatDate(item.getCreateDate()),
                    Collectors.counting()
                ));

            // 补充缺失日期的数据
            for (String date : dateList) {
                statMap.putIfAbsent(date, 0L);
            }

            HashMap<Object, Object> m = new HashMap<>();
//            m.put("src", list);
            m.put("stat", statMap);
            resultMap.put(levelName, m);
        }

        DzRiskAssessmentQueryVo fr = new DzRiskAssessmentQueryVo();
        fr.setQuery(resultMap);
        return fr;
    }

    public static final StatRiskVo defsr = new StatRiskVo();

    static {
        defsr.setDynamicRiskLevel(0);
        defsr.setCount(0);
    }

    @Override
    public DzRiskAssessmentStatVo stat(DzRiskAssessmentStatBo bo) {
        DzRiskAssessmentStatVo vo = new DzRiskAssessmentStatVo();
        Date now = new Date();
        DateTime old;
        DateTime oold;
        int offset = bo.getOffset() == null ? 0 : bo.getOffset();
        if (offset <= 0) {
            old = DateUtil.beginOfDay(now);
            oold = DateUtil.beginOfDay(DateUtil.offsetDay(old, -1));
        } else {
            old = DateUtil.endOfDay(DateUtil.offsetDay(now, -offset));
            oold = DateUtil.endOfDay(DateUtil.offsetDay(old, -offset));
        }

        // 统计风险
        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> statResult = baseMapper.statRisk(bo, old, now);
            vo.setStat(statResult);
        }, Run.executor);

        // 统计上一次周期的风险
        AtomicReference<List<StatRiskVo>> stat = new AtomicReference<>();
        CompletableFuture<Void> t4 = CompletableFuture.runAsync(() -> {
            stat.set(baseMapper.statRisk(bo, oold, old));
        }, Run.executor);

        // 统计行政区划风险
        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            List<StatAreaVo> areaStatList = baseMapper.statArea(bo, old, now);
            // 过滤掉 dynamicRiskLevel 为 null 的记录，避免 Map 的 key 为 null
            Map<Integer, List<StatAreaVo>> riskAreaMap = areaStatList.stream()
                .filter(item -> item.getDynamicRiskLevel() != null)
                .collect(Collectors.groupingBy(StatAreaVo::getDynamicRiskLevel));
            vo.setStatArea(riskAreaMap);
        }, Run.executor);

        // 统计归因分析
        CompletableFuture<Void> t3 = CompletableFuture.runAsync(() -> {

            List<Map<String, Object>> attributionList = baseMapper.statAttribution(bo, old, now);
            Map<Integer, Object> attributionStat = new HashMap<>();

            Map<String, Integer> level3Map = new HashMap<>();
            Map<String, Integer> level4Map = new HashMap<>();

            for (Map<String, Object> record : attributionList) {
                Integer riskLevel = (Integer) record.get("dynamic_risk_level");
                String suggest = (String) record.get("suggest");

                if (StringUtils.isBlank(suggest)) {
                    continue;
                }

                Map<String, Integer> targetMap = (riskLevel == 3) ? level3Map : (riskLevel == 4 ? level4Map : null);
                if (targetMap == null) {
                    continue;
                }

                String[] suggestions = suggest.split(",");
                for (String s : suggestions) {
                    if (StringUtils.isNotBlank(s)) {
                        targetMap.merge(s.trim(), 1, Integer::sum);
                    }
                }
            }

            attributionStat.put(3, level3Map);
            attributionStat.put(4, level4Map);
            vo.setStatAttribution(attributionStat);

        }, Run.executor);

        // 统计设备驱动器数据
        CompletableFuture<Void> t5 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> st = baseMapper.statMonitDevice(bo, old, now);
            vo.setStatDevice(st);
        }, Run.executor);

        // 统计灾害点数量
        CompletableFuture<Void> t6 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> st = baseMapper.statHazardPoint(bo, old, now);
            vo.setStatHazardPoint(st);
        }, Run.executor);

        CompletableFuture.allOf(t1, t2, t3, t4, t5, t6).join();

        Map<Integer, Integer> diffedRisk = diffRisk(vo.getStat(), stat.get());
        vo.setDiffRisk(diffedRisk);
        return vo;
    }


    public static Map<Integer, Integer> diffRisk(List<StatRiskVo> current, List<StatRiskVo> previous) {
        Map<Integer, StatRiskVo> currentStat = CollStreamUtil.toIdentityMap(current, StatRiskVo::getDynamicRiskLevel);
        Map<Integer, StatRiskVo> previousStat = CollStreamUtil.toIdentityMap(previous, StatRiskVo::getDynamicRiskLevel);

        Map<Integer, Integer> diffRisk = new HashMap<>();

        // 计算风险等级3的变化
        int count3 = currentStat.getOrDefault(3, defsr).getCount() - previousStat.getOrDefault(3, defsr).getCount();
        diffRisk.put(3, count3);

        // 计算风险等级4的变化
        int count4 = currentStat.getOrDefault(4, defsr).getCount() - previousStat.getOrDefault(4, defsr).getCount();
        diffRisk.put(4, count4);

        return diffRisk;
    }

    @Override
    public DzRiskAssessmentVo queryByUnitId(String id, Date time) {
        LambdaQueryWrapper<DzRiskAssessment> lqw = Wrappers.lambdaQuery();
        lqw.eq(DzRiskAssessment::getSlopeUnitId, id);
        if (time != null) {
            lqw.ge(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(time));
            lqw.le(DzRiskAssessment::getCreateDate, DateUtil.endOfDay(time));
        } else {
            Date date = new Date();
            lqw.ge(DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(date));
            lqw.le(DzRiskAssessment::getCreateDate, DateUtil.endOfDay(date));
        }
        lqw.last("limit 1");
        lqw.orderByDesc(DzRiskAssessment::getCreateDate);
        DzRiskAssessmentVo vo = baseMapper.selectVoOne(lqw);
        return vo;
    }

    @Override
    public List<RiskHazardQueryVo> statHazard(DzRiskAssessmentStatBo bo) {
        Date now = new Date();
        DateTime old;
        if (bo.getOffset() <= 0) {
            old = DateUtil.beginOfDay(now);
        } else {
            old = DateUtil.beginOfDay(DateUtil.offsetDay(now, -(bo.getOffset() - 1)));
        }

        List<RiskHazardQueryVo> stat = baseMapper.statHazard(bo, old, now);
        // 计算时间范围内的所有日期
        List<String> dateList = DateUtil.rangeToList(old, DateUtil.endOfDay(now), DateField.DAY_OF_MONTH).stream()
            .map(DateUtil::formatDate)
            .distinct()
            .toList();

        Map<String, RiskHazardQueryVo> statMap = stat.stream()
            .collect(Collectors.toMap(RiskHazardQueryVo::getDate, item -> item, (o1, o2) -> o1));

        List<RiskHazardQueryVo> result = new ArrayList<>();
        for (String date : dateList) {
            RiskHazardQueryVo vo = statMap.get(date);
            if (vo == null) {
                vo = new RiskHazardQueryVo();
                vo.setDate(date);
                vo.setRisk(BigDecimal.ZERO);
                vo.setRiskLevel(-1);
                vo.setHazard(BigDecimal.ZERO);
                vo.setHazardLevel(-1);
            }
            result.add(vo);
        }

        return result;
    }

    @Override
    public DzRiskAssessmentVo getInfo(DzRiskAssessmentInfoBo bo) {
        LambdaQueryWrapper<DzRiskAssessment> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjUtil.isNotNull(bo.getId()), DzRiskAssessment::getSlopeUnitId, bo.getId());
        lqw.ge(ObjUtil.isNotNull(bo.getDate()), DzRiskAssessment::getCreateDate, DateUtil.beginOfDay(bo.getDate()));
        lqw.le(ObjUtil.isNotNull(bo.getDate()), DzRiskAssessment::getCreateDate, DateUtil.endOfDay(bo.getDate()));
        lqw.orderByDesc(DzRiskAssessment::getCreateDate);
        lqw.last("limit 1");

        DzRiskAssessmentVo vo = baseMapper.selectVoOne(lqw);
        if (vo != null) {
            vo.setSlopeUnit(slopeUnitService.queryById(bo.getId()));
        }
        return vo;
    }

    @Override
    public RiskChatBannerVo statChatBanner(DzRiskAssessmentStatBo bo) {
        DzRiskAssessmentStatBo permissionBo = buildCurrentUserStatScope(bo);
        RiskChatBannerVo vo = new RiskChatBannerVo();
        if (permissionBo == null) {
            vo.setSlopUnitCount(0L);
            vo.setHighRiskCount(0);
            vo.setVeryHighRiskCount(0);
            vo.setUnHandleReportCount(0L);
            vo.setUnfinishedEventTotalCount(0L);
            vo.setHandlingTaskCount(0L);
            vo.setOngoingMeetingCount(0L);
            vo.setKeypointArea(List.of());
            vo.setStatPush(new TaskStatisticsVo(0L));
            return vo;
        }
        Date now = new Date();
        Date todayStart = DateUtil.beginOfDay(now);
        Date todayEnd = DateUtil.endOfDay(now);
        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
            SlopeUnitBo slopeUnitBo = buildSlopeUnitBo(permissionBo);
            Long count = hasAdRegionIds(permissionBo)
                ? slopeUnitService.countPilotArea1ByAdRegionIds(slopeUnitBo, permissionBo.getAdRegionIds())
                : slopeUnitService.countPilotArea1(slopeUnitBo);
            vo.setSlopUnitCount(count);
        }, Run.executor);

        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> statRiskVos = baseMapper.statRisk(permissionBo, todayStart, todayEnd);
            Map<Integer, StatRiskVo> imap = CollStreamUtil.toIdentityMap(statRiskVos, StatRiskVo::getDynamicRiskLevel);
            vo.setHighRiskCount(imap.getOrDefault(3, defsr).getCount());
            vo.setVeryHighRiskCount(imap.getOrDefault(4, defsr).getCount());
        }, Run.executor);


        CompletableFuture<Void> t3 = CompletableFuture.runAsync(() -> {
            List<RiskChatBannerVo.RiskChatBannerItemVo> keypoint = baseMapper.statAreaTop3(permissionBo, todayStart, todayEnd);
            vo.setKeypointArea(keypoint);
        }, Run.executor);

        CompletableFuture<Void> t4 = CompletableFuture.runAsync(() -> {
            TaskStatisticsVo push = dzTaskDistListMapper.statPushByRegion(permissionBo, null, null);
            vo.setStatPush(push);
        }, Run.executor);

        CompletableFuture<Void> t5 = CompletableFuture.runAsync(() -> {
            Long count = countUnhandledReports(permissionBo, null, null);
            vo.setUnHandleReportCount(count);
        }, Run.executor);

        CompletableFuture<Void> t6 = CompletableFuture.runAsync(() -> {
            Long count = countUnfinishedEvents(permissionBo, null, null);
            vo.setUnfinishedEventTotalCount(count);
        }, Run.executor);

        CompletableFuture<Void> t7 = CompletableFuture.runAsync(() -> {
            Long count = countHandlingTasks(permissionBo, null, null);
            vo.setHandlingTaskCount(count);
        }, Run.executor);
        Long userId = LoginHelper.getUserId();
        CompletableFuture<Void> t8 = CompletableFuture.runAsync(() -> {
            Long count = MeetingRedisCache.countMeetingsByParticipant(userId);
            vo.setOngoingMeetingCount(count);
        }, Run.executor);

        CompletableFuture.allOf(t1, t2, t3, t4, t5, t6, t7, t8).join();
        return vo;
    }

    private Long countUnhandledReports(DzRiskAssessmentStatBo statBo, Date startTime, Date endTime) {
        QueryWrapper<DzReportDisaster> queryWrapper = Wrappers.query();
        queryWrapper.eq("status", 1);
        queryWrapper.ge(startTime != null, "create_date", startTime);
        queryWrapper.le(endTime != null, "create_date", endTime);
        if (hasAdRegionIds(statBo)) {
            queryWrapper.apply(buildReportRegionIdsExistsSql(statBo.getAdRegionIds()));
        } else if (!isEmptyScope(statBo)) {
            queryWrapper.apply(buildReportRegionExistsSql(statBo));
        }
        return dzReportDisasterMapper.selectCount(queryWrapper);
    }

    private Long countUnfinishedEvents(DzRiskAssessmentStatBo statBo, Date startTime, Date endTime) {
        QueryWrapper<DzReportDisaster> queryWrapper = Wrappers.query();
        queryWrapper.in("status", 1, 2, 3, 4);
        queryWrapper.ge(startTime != null, "create_date", startTime);
        queryWrapper.le(endTime != null, "create_date", endTime);
        if (hasAdRegionIds(statBo)) {
            queryWrapper.apply(buildReportRegionIdsExistsSql(statBo.getAdRegionIds()));
        } else if (!isEmptyScope(statBo)) {
            queryWrapper.apply(buildReportRegionExistsSql(statBo));
        }
        return dzReportDisasterMapper.selectCount(queryWrapper);
    }

    private String buildReportRegionIdsExistsSql(List<String> adRegionIds) {
        return """
            exists (
                select 1
                from data_ad_region ar
                where ar.id in (%s)
                  and ar.geom is not null
                  and check_center is not null
                  and ST_Contains(ar.geom, ST_GeomFromText(check_center, 4326))
            )
            """.formatted(buildQuotedInValues(adRegionIds));
    }

    private String buildReportRegionExistsSql(DzRiskAssessmentStatBo statBo) {
        StringBuilder sql = new StringBuilder("""
            exists (
                select 1
                from data_ad_region ar
                where ar.geom is not null
                  and check_center is not null
                  and ST_Contains(ar.geom, ST_GeomFromText(check_center, 4326))
            """);
        appendRegionEquals(sql, "ar.province", statBo.getProvince());
        appendRegionEquals(sql, "ar.city", statBo.getCity());
        appendRegionEquals(sql, "ar.county", statBo.getCounty());
        appendRegionEquals(sql, "ar.street", statBo.getStreet());
        appendRegionEquals(sql, "ar.village", statBo.getVillage());
        sql.append("\n)");
        return sql.toString();
    }

    private Long countHandlingTasks(DzRiskAssessmentStatBo statBo, Date startTime, Date endTime) {
        LambdaQueryWrapper<DzTaskHandle> lqw = Wrappers.lambdaQuery();
        lqw.in(DzTaskHandle::getHandleProcess,
            HandleProcessEnum.EMERGENCY_INVESTIGATION.getCode(),
            HandleProcessEnum.CONSULTATION_JUDGMENT.getCode());
        lqw.ge(startTime != null, DzTaskHandle::getCreateDate, startTime);
        lqw.le(endTime != null, DzTaskHandle::getCreateDate, endTime);
        if (hasAdRegionIds(statBo)) {
            appendTaskHandleRegionPermission(lqw, statBo.getAdRegionIds());
        } else {
            lqw.eq(StringUtils.isNotBlank(statBo.getProvince()), DzTaskHandle::getProvince, statBo.getProvince());
            lqw.eq(StringUtils.isNotBlank(statBo.getCity()), DzTaskHandle::getCity, statBo.getCity());
            lqw.eq(StringUtils.isNotBlank(statBo.getCounty()), DzTaskHandle::getCounty, statBo.getCounty());
            lqw.eq(StringUtils.isNotBlank(statBo.getStreet()), DzTaskHandle::getStreet, statBo.getStreet());
            lqw.eq(StringUtils.isNotBlank(statBo.getVillage()), DzTaskHandle::getVillage, statBo.getVillage());
        }
        return dzTaskHandleMapper.selectCount(lqw);
    }

    private void appendTaskHandleRegionPermission(LambdaQueryWrapper<DzTaskHandle> lqw, List<String> adRegionIds) {
        List<AdRegion> adRegions = adRegionMapper.selectBatchIds(adRegionIds);
        if (adRegions == null || adRegions.isEmpty()) {
            lqw.apply("1 = 0");
            return;
        }
        List<AdRegion> effectiveRegions = adRegions.stream()
                                                    .filter(this::hasAnyTaskHandleRegionScope)
                                                    .toList();
        if (effectiveRegions.isEmpty()) {
            lqw.apply("1 = 0");
            return;
        }
        lqw.and(wrapper -> {
            for (AdRegion adRegion : effectiveRegions) {
                wrapper.or(item -> {
                    item.eq(StringUtils.isNotBlank(adRegion.getProvince()), DzTaskHandle::getProvince, adRegion.getProvince());
                    item.eq(StringUtils.isNotBlank(adRegion.getCity()), DzTaskHandle::getCity, adRegion.getCity());
                    item.eq(StringUtils.isNotBlank(adRegion.getCounty()), DzTaskHandle::getCounty, adRegion.getCounty());
                    item.eq(StringUtils.isNotBlank(adRegion.getStreet()), DzTaskHandle::getStreet, adRegion.getStreet());
                    item.eq(StringUtils.isNotBlank(adRegion.getVillage()), DzTaskHandle::getVillage, adRegion.getVillage());
                });
            }
        });
    }

    private boolean hasAnyTaskHandleRegionScope(AdRegion adRegion) {
        return adRegion != null
            && (StringUtils.isNotBlank(adRegion.getProvince())
            || StringUtils.isNotBlank(adRegion.getCity())
            || StringUtils.isNotBlank(adRegion.getCounty())
            || StringUtils.isNotBlank(adRegion.getStreet())
            || StringUtils.isNotBlank(adRegion.getVillage()));
    }

    private void appendRegionEquals(StringBuilder sql, String column, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        sql.append("\n  and ").append(column).append(" = '").append(value.replace("'", "''")).append("'");
    }

    private boolean isEmptyScope(DzRiskAssessmentStatBo statBo) {
        return statBo == null
               || (StringUtils.isBlank(statBo.getProvince())
                   && StringUtils.isBlank(statBo.getCity())
                   && StringUtils.isBlank(statBo.getCounty())
                   && StringUtils.isBlank(statBo.getStreet())
                   && StringUtils.isBlank(statBo.getVillage()));
    }

    private DzRiskAssessmentStatBo buildCurrentUserStatScope(DzRiskAssessmentStatBo bo) {
        if (bo == null) {
            bo = new DzRiskAssessmentStatBo();
        }
        if (LoginHelper.isSuperAdmin()) {
            return bo;
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return null;
        }
        List<String> adRegionIds = dzUserAdRegionService.queryEffectiveListByUserId(userId)
                                                        .stream()
                                                        .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
                                                        .map(DzUserAdRegionVo::getAdRegionId)
                                                        .distinct()
                                                        .toList();
        if (adRegionIds.isEmpty()) {
            return null;
        }
        if (adRegionIds.size() > 1) {
            bo.setAdRegionIds(adRegionIds);
            return bo;
        }
        AdRegion adRegion = adRegionMapper.selectById(adRegionIds.get(0));
        if (adRegion == null) {
            return null;
        }
        Integer level = adRegion.getLevel();
        if (level == null) {
            return null;
        }
        if (level >= 1) {
            bo.setProvince(adRegion.getProvince());
        }
        if (level >= 2) {
            bo.setCity(adRegion.getCity());
        }
        if (level >= 3) {
            bo.setCounty(adRegion.getCounty());
        }
        if (level >= 4) {
            bo.setStreet(adRegion.getStreet());
        }
        if (level >= 5) {
            bo.setVillage(adRegion.getVillage());
        }
        return bo;
    }

    private boolean hasAdRegionIds(DzRiskAssessmentStatBo statBo) {
        return statBo != null && statBo.getAdRegionIds() != null && !statBo.getAdRegionIds().isEmpty();
    }

    private String buildQuotedInValues(List<String> values) {
        return values.stream()
                     .filter(StringUtils::isNotBlank)
                     .distinct()
                     .map(value -> "'" + value.replace("'", "''") + "'")
                     .collect(Collectors.joining(","));
    }

    private SlopeUnitBo buildSlopeUnitBo(DzRiskAssessmentStatBo statBo) {
        SlopeUnitBo bo = new SlopeUnitBo();
        if (statBo == null) {
            return bo;
        }
        bo.setProvince(statBo.getProvince());
        bo.setCity(statBo.getCity());
        bo.setCounty(statBo.getCounty());
        bo.setStreet(statBo.getStreet());
        bo.setVillage(statBo.getVillage());
        bo.setPilotArea1(statBo.getPilotArea1());
        bo.setPilotArea2(statBo.getPilotArea2());
        return bo;
    }
}
