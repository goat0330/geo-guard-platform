package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionChartBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPrediction;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionChartVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatAreaVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRiskVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskPredictionMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskPredictionPushMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskPredictionService;
import cn.edu.pku.whai.geological.disaster.service.utils.Run;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static cn.edu.pku.whai.geological.disaster.service.service.impl.DzRiskAssessmentServiceImpl.defsr;
import static cn.edu.pku.whai.geological.disaster.service.service.impl.DzRiskAssessmentServiceImpl.diffRisk;

/**
 * 风险预测Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzRiskPredictionServiceImpl implements IDzRiskPredictionService {

    private final DzRiskPredictionMapper baseMapper;
    private final DzRiskPredictionPushMapper dzRiskPredictionPushMapper;
    private final ISlopeUnitService slopeUnitService;

    /**
     * 查询风险预测
     *
     * @param id 主键
     * @return 风险预测
     */
    @Override
    public DzRiskPredictionVo queryById(Long id) {
        DzRiskPredictionVo vo = baseMapper.selectVoById(id);
        if (vo != null && vo.getSlopeUnitId() != null) {
            vo.setSlopeUnit(slopeUnitService.queryById(vo.getSlopeUnitId()));
        }
        return vo;
    }

    @Override
    public List<DzRiskPredictionVo> queryByBatchId(Long batchId) {
        DzRiskPredictionBo bo = new DzRiskPredictionBo();
        bo.setBatchId(batchId);
        return queryList(bo);
    }

    /**
     * 分页查询风险预测列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 风险预测分页列表
     */
    @Override
    public TableDataInfo<DzRiskPredictionVo> queryPageList(DzRiskPredictionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzRiskPrediction> lqw = buildQueryWrapper(bo);
        Page<DzRiskPredictionVo> result = baseMapper.selectVoPageWithSlopeUnit(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的风险预测列表
     *
     * @param bo 查询条件
     * @return 风险预测列表
     */
    @Override
    public List<DzRiskPredictionVo> queryList(DzRiskPredictionBo bo) {
        LambdaQueryWrapper<DzRiskPrediction> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzRiskPrediction> buildQueryWrapper(DzRiskPredictionBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzRiskPrediction> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getBatchId() != null, DzRiskPrediction::getBatchId, bo.getBatchId());
        lqw.eq(bo.getSlopeUnitId() != null, DzRiskPrediction::getSlopeUnitId, bo.getSlopeUnitId());
        lqw.eq(bo.getPredictionScoreW() != null, DzRiskPrediction::getPredictionScoreW, bo.getPredictionScoreW());
        lqw.in(CollUtil.isNotEmpty(bo.getRiskLevelList()), DzRiskPrediction::getRiskLevel, bo.getRiskLevelList());

        if (bo.getLatest() != null && bo.getLatest() == 1) {
            lqw.apply("""
                prediction_date = (select max(prediction_date)
                from dz_risk_prediction
                where type = {0} )
                """, bo.getType());
        }

        lqw.eq(bo.getPredictionDate() != null, DzRiskPrediction::getPredictionDate, bo.getPredictionDate());
        lqw.eq(bo.getPredictionDateStr() != null, DzRiskPrediction::getPredictionDateStr, bo.getPredictionDateStr());
        lqw.eq(bo.getType() != null, DzRiskPrediction::getType, bo.getType());

        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzRiskPrediction::getCreateDate, params.get("beginTime"), params.get("endTime"));
        }

        lqw.apply(bo.getProvince() != null, "t2.province = {0}", bo.getProvince());
        lqw.apply(bo.getCity() != null, "t2.city = {0}", bo.getCity());
        lqw.apply(bo.getCounty() != null, "t2.county = {0}", bo.getCounty());
        lqw.apply(bo.getStreet() != null, "t2.street = {0}", bo.getStreet());
        lqw.apply(bo.getVillage() != null, "t2.village = {0}", bo.getVillage());

        return lqw;
    }

    /**
     * 新增风险预测
     *
     * @param bo 风险预测
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzRiskPredictionBo bo) {
        DzRiskPrediction add = MapstructUtils.convert(bo, DzRiskPrediction.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改风险预测
     *
     * @param bo 风险预测
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzRiskPredictionBo bo) {
        DzRiskPrediction update = MapstructUtils.convert(bo, DzRiskPrediction.class);
        if (bo == null) {
            throw new ServiceException("风险预测不能为空");
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzRiskPrediction entity) {
        if (entity == null) {
            throw new ServiceException("风险预测不能为空");
        }
        if (entity.getBatchId() == null) {
            throw new ServiceException("预测批次不能为空");
        }
        if (StringUtils.isBlank(entity.getSlopeUnitId())) {
            throw new ServiceException("斜坡单元不能为空");
        }
        if (entity.getPredictionScoreW() == null) {
            throw new ServiceException("预测分数不能为空");
        }
        if (entity.getRiskLevel() == null || entity.getRiskLevel() < 0 || entity.getRiskLevel() > 5) {
            throw new ServiceException("风险等级非法");
        }
        if (entity.getType() == null || entity.getType() < 1 || entity.getType() > 5) {
            throw new ServiceException("预测类型非法");
        }
        if (entity.getPredictionDate() == null && StringUtils.isBlank(entity.getPredictionDateStr())) {
            throw new ServiceException("预测日期不能为空");
        }
        LambdaQueryWrapper<DzRiskPrediction> lqw = Wrappers.<DzRiskPrediction>lambdaQuery()
                                                           .eq(DzRiskPrediction::getBatchId, entity.getBatchId())
                                                           .eq(DzRiskPrediction::getSlopeUnitId, entity.getSlopeUnitId())
                                                           .eq(DzRiskPrediction::getType, entity.getType())
                                                           .eq(StringUtils.isNotBlank(entity.getPredictionDateStr()), DzRiskPrediction::getPredictionDateStr, entity.getPredictionDateStr())
                                                           .eq(entity.getPredictionDate() != null, DzRiskPrediction::getPredictionDate, entity.getPredictionDate());
        if (entity.getId() != null) {
            lqw.ne(DzRiskPrediction::getId, entity.getId());
        }
        if (baseMapper.selectCount(lqw) > 0) {
            throw new ServiceException("同一批次同一斜坡单元同一周期的风险预测已存在");
        }
    }

    /**
     * 校验并批量删除风险预测信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<DzRiskPrediction> predictions = baseMapper.selectByIds(ids);
            if (predictions.size() != ids.size()) {
                throw new ServiceException("存在风险预测记录不存在，无法删除");
            }
            List<Long> batchIds = predictions.stream().map(DzRiskPrediction::getBatchId).filter(ObjUtil::isNotNull).distinct().toList();
            if (!batchIds.isEmpty() && dzRiskPredictionPushMapper.selectCount(Wrappers.<cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush>lambdaQuery()
                                                                                      .in(cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush::getPredictionBatchId, batchIds)) > 0) {
                throw new ServiceException("已存在推送记录的风险预测批次不允许删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public DzRiskPredictionStatVo stat(DzRiskPredictionStatBo bo) {
        DzRiskPredictionStatVo vo = new DzRiskPredictionStatVo();

        if (bo == null) {
            throw new ServiceException("统计参数不能为空");
        }

        String predictionDateStr = bo.getPredictionDateStr();
        if (StringUtils.isBlank(predictionDateStr)) {
            throw new ServiceException("统计时间不能为空");
        }
        Date predictionDate = DateUtil.parse(predictionDateStr,"yyyy-MM");

        String curr = resolveCurrentPeriodStr(predictionDate, bo.getType());
        vo.setLast(curr);
        String old = resolvePreviousPeriodStr(predictionDate, bo.getType());

        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> statResult = baseMapper.statRisk(buildRiskWrap(bo, curr));
            vo.setStat(statResult);
        }, Run.executor);

        AtomicReference<List<StatRiskVo>> stat = new AtomicReference<>();
        CompletableFuture<Void> t4 = CompletableFuture.runAsync(() -> stat.set(baseMapper.statRisk(buildRiskWrap(bo, old))), Run.executor);

        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            List<StatAreaVo> areaStatList = baseMapper.statArea(buildAreaWrap(bo, curr));
            Map<Integer, List<StatAreaVo>> riskAreaMap = CollStreamUtil.groupByKey(areaStatList, StatAreaVo::getDynamicRiskLevel);
            vo.setStatArea(riskAreaMap);
        }, Run.executor);

        CompletableFuture.allOf(t1, t2, t4).join();

        Map<Integer, Integer> diffedRisk = diffRisk(vo.getStat(), stat.get());
        vo.setDiffRisk(diffedRisk);
        return vo;
    }

    @Override
    public DzRiskPredictionChartVo statChart(DzRiskPredictionChartBo bo) {
        DzRiskPredictionChartVo vo = new DzRiskPredictionChartVo();
        Date now = new Date();
        String curr = resolveCurrentPeriodStr(now, bo.getType());
        String next = resolveNextPeriodStr(now, bo.getType());

        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() ->
                vo.setKeypointArea(baseMapper.statAreaTop3(bo.getType(), curr)),
            Run.executor);

        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            List<StatRiskVo> statRiskVos = baseMapper.statRisk(buildPredictionRiskWrap(next));
            Map<Integer, StatRiskVo> imap = CollStreamUtil.toIdentityMap(statRiskVos, StatRiskVo::getDynamicRiskLevel);

            vo.setNextPeriodHighRiskCount(imap.getOrDefault(4, defsr).getCount());
            vo.setNextPeriodVeryHighRiskCount(imap.getOrDefault(5, defsr).getCount());
        }, Run.executor);

        CompletableFuture.allOf(t1, t2).join();

        return vo;
    }

    private static DateTime resolveCurrentPeriodStart(Date now, Integer type) {
        return switch (type == null ? 1 : type) {
            case 2 -> DateUtil.beginOfWeek(now);
            case 3 -> DateUtil.beginOfMonth(now);
            case 4 -> DateUtil.beginOfQuarter(now);
            case 5 -> DateUtil.beginOfYear(now);
            default -> DateUtil.beginOfDay(now);
        };
    }

    private static DateTime resolvePreviousPeriodStart(Date currentPeriodStart, Integer type) {
        return switch (type == null ? 1 : type) {
            case 2 -> DateUtil.beginOfWeek(DateUtil.offsetWeek(currentPeriodStart, -1));
            case 3 -> DateUtil.beginOfMonth(DateUtil.offsetMonth(currentPeriodStart, -1));
            case 4 -> DateUtil.beginOfQuarter(DateUtil.offset(currentPeriodStart, DateField.MONTH, -3));
            case 5 -> DateUtil.beginOfYear(DateUtil.offsetYear(currentPeriodStart, -1));
            default -> DateUtil.beginOfDay(DateUtil.offsetDay(currentPeriodStart, -1));
        };
    }

    /**
     * 获取当前周期的字符串表示
     *
     * @param now  当前时间
     * @param type 周期类型 (1:日, 2:周, 3:月, 4:季, 5:年)
     * @return 格式化后的字符串
     */
    public static String resolveCurrentPeriodStr(Date now, Integer type) {
        DateTime start = resolveCurrentPeriodStart(now, type);
        return formatPeriod(start, type);
    }

    /**
     * 获取下一个周期的字符串表示
     *
     * @param now  当前时间
     * @param type 周期类型
     * @return 格式化后的字符串
     */
    public static String resolveNextPeriodStr(Date now, Integer type) {
        DateTime currentStart = resolveCurrentPeriodStart(now, type);
        DateTime nextStart = switch (type == null ? 1 : type) {
            case 2 -> DateUtil.offsetWeek(currentStart, 1);
            case 3 -> DateUtil.offsetMonth(currentStart, 1);
            case 4 -> DateUtil.offset(currentStart, DateField.MONTH, 3);
            case 5 -> DateUtil.offsetYear(currentStart, 1);
            default -> DateUtil.offsetDay(currentStart, 1);
        };
        return formatPeriod(nextStart, type);
    }

    /**
     * 获取上一个周期的字符串表示
     *
     * @param now  当前时间
     * @param type 周期类型 (1:日, 2:周, 3:月, 4:季, 5:年)
     * @return 格式化后的字符串 (如: 2026-02, 2025-4, 2025)
     */
    public static String resolvePreviousPeriodStr(Date now, Integer type) {
        // 1. 先计算当前周期的起点
        DateTime currentStart = resolveCurrentPeriodStart(now, type);

        // 2. 根据类型向前偏移一个周期
        DateTime prevStart = switch (type == null ? 1 : type) {
            case 2 -> DateUtil.offsetWeek(currentStart, -1);
            case 3 -> DateUtil.offsetMonth(currentStart, -1);
            case 4 -> DateUtil.offset(currentStart, DateField.MONTH, -3);
            case 5 -> DateUtil.offsetYear(currentStart, -1);
            default -> DateUtil.offsetDay(currentStart, -1);
        };

        // 3. 格式化输出
        return formatPeriod(prevStart, type);
    }

    /**
     * 内部辅助方法：根据类型转换格式
     */
    private static String formatPeriod(Date date, Integer type) {
        int t = (type == null) ? 1 : type;
        return switch (t) {
            case 2 -> DateUtil.format(date, "yyyy-ww"); // 周：2026-10 (第几周)
            case 3 -> DateUtil.format(date, "yyyy-MM"); // 月份：2026-03
            case 4 -> {
                int quarter = DateUtil.quarter(date); // 获取季度 (1-4)
                yield DateUtil.format(date, "yyyy") + "-" + quarter; // 季度：2026-1
            }
            case 5 -> DateUtil.format(date, "yyyy"); // 年份：2026
            default -> DateUtil.format(date, "yyyy-MM-dd"); // 天：2026-03-06
        };
    }

    private static QueryWrapper<DzRiskPrediction> buildRiskWrap(DzRiskPredictionStatBo bo, String predictionDate) {
        QueryWrapper<DzRiskPrediction> wrapper = new QueryWrapper<>();

        wrapper.select("t1.risk_level as dynamicRiskLevel ", "CAST(COUNT(*) AS INTEGER) AS count");
        // 1. 基础条件 (Where)
        wrapper.eq("t2.pilot_area_1", 1);
        wrapper.eq("t1.prediction_date_str", predictionDate);
        wrapper.eq(ObjUtil.isNotNull(bo.getType()), "t1.type", bo.getType());
        wrapper.eq(StringUtils.isNotBlank(bo.getProvince()), "t2.province", bo.getProvince());
        wrapper.eq(StringUtils.isNotBlank(bo.getCity()), "t2.city", bo.getCity());
        wrapper.eq(StringUtils.isNotBlank(bo.getCounty()), "t2.county", bo.getCounty());
        wrapper.eq(StringUtils.isNotBlank(bo.getStreet()), "t2.street", bo.getStreet());
        wrapper.eq(StringUtils.isNotBlank(bo.getVillage()), "t2.village", bo.getVillage());

        // 3. 分组条件 (Group By)
        wrapper.groupBy("t1.risk_level");
        return wrapper;
    }

    public QueryWrapper<DzRiskPrediction> buildAreaWrap(DzRiskPredictionStatBo bo, String predictionDate) {
        QueryWrapper<DzRiskPrediction> wrapper = new QueryWrapper<>();

        // --- 1. 处理动态 SELECT 字段
        String areaField;
        if (StringUtils.isNotBlank(bo.getVillage())) {
            areaField = "t2.village";
        } else if (StringUtils.isNotBlank(bo.getStreet())) {
            areaField = "t2.village";
        } else if (StringUtils.isNotBlank(bo.getCounty())) {
            areaField = "t2.street";
        } else if (StringUtils.isNotBlank(bo.getCity())) {
            areaField = "t2.county";
        } else if (StringUtils.isNotBlank(bo.getProvince())) {
            areaField = "t2.city";
        } else {
            areaField = "t2.province";
        }

        // 构造完整的 COALESCE 语句作为 areaName
        String areaNameSql = String.format("COALESCE(%s, 'Unknown') AS areaName", areaField);

        // 设置查询字段
        wrapper.select("t1.risk_level as dynamicRiskLevel ", areaNameSql, "CAST(COUNT(t1.id) AS INTEGER) AS count");

        // --- 2. 处理 WHERE 条件 ---
        wrapper.eq("t2.pilot_area_1", 1);
        wrapper.in("t1.risk_level", 3, 4);
        wrapper.eq("t1.prediction_date_str", predictionDate);

        // 动态拼接行政区划过滤
        wrapper.eq(StringUtils.isNotBlank(bo.getProvince()), "t2.province", bo.getProvince());
        wrapper.eq(StringUtils.isNotBlank(bo.getCity()), "t2.city", bo.getCity());
        wrapper.eq(StringUtils.isNotBlank(bo.getCounty()), "t2.county", bo.getCounty());
        wrapper.eq(StringUtils.isNotBlank(bo.getStreet()), "t2.street", bo.getStreet());
        wrapper.eq(StringUtils.isNotBlank(bo.getVillage()), "t2.village", bo.getVillage());

        // --- 3. 处理 GROUP BY ---
        wrapper.groupBy("t1.risk_level", "areaName");
        return wrapper;
    }


    private static QueryWrapper<DzRiskPrediction> buildPredictionRiskWrap(String predictionDate) {
        QueryWrapper<DzRiskPrediction> wrapper = new QueryWrapper<>();
        wrapper.select("t1.risk_level as dynamicRiskLevel ", "CAST(COUNT(*) AS INTEGER) AS count");
        wrapper.eq("t2.pilot_area_1", 1);
        wrapper.in("t1.risk_level", 4, 5);
        wrapper.eq("t1.prediction_date_str", predictionDate);
        wrapper.groupBy("t1.risk_level");
        return wrapper;
    }
}
