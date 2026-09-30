/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallHourlySeriesVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 应急调查报告历史降雨转换工具
 *
 * @author zhuzc
 * @date 2026-06-15
 */
public final class EmergencyHistoricalRainfallConverter {

    private static final String SOURCE_HISTORY = "history";
    private static final String SOURCE_FORECAST_FALLBACK = "forecast_fallback";
    private static final String SOURCE_ACTUAL = "actual";
    private static final String SOURCE_FORECAST = "forecast";
    private static final String SOURCE_MISSING = "missing";
    private static final int PERIOD_HOURS = 24;

    private EmergencyHistoricalRainfallConverter() {
    }

    /**
     * 将按小时雨量序列转换为按 24 小时分段的展示数据。
     *
     * @param source 原始小时雨量序列
     * @return 转换后的历史降雨展示数据
     */
    public static EmergencyHistoricalRainfallVo convert(RainfallHourlySeriesVo source) {
        if (source == null) {
            return null;
        }
        EmergencyHistoricalRainfallVo target = new EmergencyHistoricalRainfallVo();
        target.setSlopeUnitId(source.getSlopeUnitId());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setTotalRainfall(source.getTotalRainfall());
        target.setHistoryHourCount(source.getHistoryHourCount());
        target.setForecastFallbackHourCount(source.getForecastFallbackHourCount());
        target.setMissingHourCount(source.getMissingHourCount());
        target.setHourlySeries(buildPeriodSeries(source));
        return target;
    }

    /**
     * 将逐小时序列按 24 小时窗口聚合。
     *
     * @param source 原始小时雨量序列
     * @return 聚合后的时间段列表
     */
    private static List<EmergencyHistoricalRainfallVo.RainfallPeriodItem> buildPeriodSeries(RainfallHourlySeriesVo source) {
        List<EmergencyHistoricalRainfallVo.RainfallPeriodItem> result = new ArrayList<>();
        List<RainfallHourlySeriesVo.HourlyRainfallItem> sourceItems = source.getHourlySeries();
        LocalDateTime startTime = source.getStartTime();
        LocalDateTime endTime = resolveDisplayEndTime(source);
        if (sourceItems == null || sourceItems.isEmpty() || startTime == null || endTime == null) {
            return result;
        }
        for (LocalDateTime periodStart = startTime; periodStart.isBefore(endTime); periodStart = periodStart.plusHours(PERIOD_HOURS)) {
            LocalDateTime periodEnd = periodStart.plusHours(PERIOD_HOURS);
            result.add(buildPeriodItem(sourceItems, periodStart, periodEnd));
        }
        return result;
    }

    /**
     * 生成单个 24 小时区间的聚合结果。
     *
     * @param sourceItems 小时雨量列表
     * @param periodStart 区间开始时间
     * @param periodEnd   区间结束时间
     * @return 单个分段结果
     */
    private static EmergencyHistoricalRainfallVo.RainfallPeriodItem buildPeriodItem(
        List<RainfallHourlySeriesVo.HourlyRainfallItem> sourceItems, LocalDateTime periodStart, LocalDateTime periodEnd) {
        double rainfall = 0D;
        boolean containsHistory = false;
        boolean containsForecast = false;
        for (RainfallHourlySeriesVo.HourlyRainfallItem sourceItem : sourceItems) {
            if (!isInPeriod(sourceItem, periodStart, periodEnd)) {
                continue;
            }
            rainfall += sourceItem.getRainfall() == null ? 0D : sourceItem.getRainfall();
            containsHistory = containsHistory || SOURCE_HISTORY.equals(sourceItem.getSource());
            containsForecast = containsForecast || SOURCE_FORECAST_FALLBACK.equals(sourceItem.getSource());
        }
        EmergencyHistoricalRainfallVo.RainfallPeriodItem targetItem =
            new EmergencyHistoricalRainfallVo.RainfallPeriodItem();
        targetItem.setStartTime(periodStart);
        targetItem.setEndTime(periodEnd);
        targetItem.setRainfall(roundRainfall(rainfall));
        targetItem.setSource(resolvePeriodSource(containsHistory, containsForecast));
        return targetItem;
    }

    /**
     * 解析展示用结束时间，避免 7 天小时序列因闭开区间多出 1 小时而生成第 8 段。
     *
     * @param source 原始小时雨量序列
     * @return 展示用结束时间
     */
    private static LocalDateTime resolveDisplayEndTime(RainfallHourlySeriesVo source) {
        if (source.getStartTime() == null || source.getEndTime() == null) {
            return source.getEndTime();
        }
        long hourCount = java.time.Duration.between(source.getStartTime(), source.getEndTime()).toHours();
        if (hourCount % PERIOD_HOURS == 1) {
            return source.getEndTime().minusHours(1);
        }
        return source.getEndTime();
    }

    /**
     * 判断小时数据是否位于当前统计区间内。
     *
     * @param sourceItem   小时数据
     * @param periodStart  区间开始时间
     * @param periodEnd    区间结束时间
     * @return 是否命中
     */
    private static boolean isInPeriod(RainfallHourlySeriesVo.HourlyRainfallItem sourceItem,
                                      LocalDateTime periodStart,
                                      LocalDateTime periodEnd) {
        if (sourceItem == null || sourceItem.getHourTime() == null) {
            return false;
        }
        return !sourceItem.getHourTime().isBefore(periodStart) && sourceItem.getHourTime().isBefore(periodEnd);
    }

    /**
     * 解析区间数据来源。
     *
     * @param containsHistory  是否包含历史实况
     * @param containsForecast 是否包含预测回填
     * @return 展示用来源编码
     */
    private static String resolvePeriodSource(boolean containsHistory, boolean containsForecast) {
        if (containsHistory) {
            return SOURCE_ACTUAL;
        }
        if (containsForecast) {
            return SOURCE_FORECAST;
        }
        return SOURCE_MISSING;
    }

    /**
     * 雨量统一保留两位小数，避免浮点展示误差。
     *
     * @param rainfall 原始雨量
     * @return 四舍五入后的雨量
     */
    private static double roundRainfall(double rainfall) {
        return BigDecimal.valueOf(rainfall).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
