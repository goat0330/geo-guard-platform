/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.service.IShortTermTemporaryRainfallQueryService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 将可用短期临时雨量覆盖到同一斜坡单元中心点和统计时段的分期序列。
 */
@Component
public class ShortTermTemporaryRainfallSeriesOverlay {

    private static final int SHORT_TERM_RANGE_FLAG = 1;
    private static final String SERIES_SOURCE_ACTUAL = "actual";
    private static final String SERIES_SOURCE_FORECAST = "forecast";

    private final IShortTermTemporaryRainfallQueryService shortTermTemporaryRainfallService;

    /**
     * 短临雨量属于可选业务扩展；只迁入 data 时允许缺省，避免反向要求 service 的实现。
     */
    public ShortTermTemporaryRainfallSeriesOverlay(
        @Nullable IShortTermTemporaryRainfallQueryService shortTermTemporaryRainfallService) {
        this.shortTermTemporaryRainfallService = shortTermTemporaryRainfallService;
    }

    /**
     * 扩展未装配时保留已有栅格统计值；装配后仍沿用原有短临覆盖规则，不伪造零雨量。
     */
    public void apply(
        List<SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem> rainfallSeries,
        RainfallEntityGridMapping mapping,
        Map<LocalDateTime, Double> actualHourlyRainfall) {
        if (shortTermTemporaryRainfallService == null
            || !isEligible(mapping) || rainfallSeries == null || rainfallSeries.isEmpty()) {
            return;
        }
        for (SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item : rainfallSeries) {
            if (SERIES_SOURCE_ACTUAL.equals(item.getSource())) {
                applyObservation(item, mapping, actualHourlyRainfall);
            } else if (SERIES_SOURCE_FORECAST.equals(item.getSource())) {
                applyForecast(item, mapping);
            }
        }
    }

    private void applyObservation(
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item,
        RainfallEntityGridMapping mapping,
        Map<LocalDateTime, Double> actualHourlyRainfall) {
        if (item.getStartTime() == null || item.getEndTime() == null || actualHourlyRainfall == null) {
            return;
        }
        double rainfall = 0D;
        boolean hasHourlyRainfall = false;
        for (LocalDateTime logTime = item.getStartTime();
             logTime.isBefore(item.getEndTime());
             logTime = logTime.plusHours(1)) {
            Double shortTermRainfall = shortTermTemporaryRainfallService.tryInterpolateObservation(
                logTime, mapping.getCenterLon(), mapping.getCenterLat());
            if (shortTermRainfall != null) {
                rainfall += shortTermRainfall;
                hasHourlyRainfall = true;
                continue;
            }
            if (actualHourlyRainfall.containsKey(logTime)) {
                rainfall += defaultValue(actualHourlyRainfall.get(logTime));
                hasHourlyRainfall = true;
            }
        }
        if (hasHourlyRainfall) {
            item.setRainfall(roundRainfall(rainfall));
        }
    }

    private void applyForecast(
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item,
        RainfallEntityGridMapping mapping) {
        if (item.getEndTime() == null) {
            return;
        }
        Double shortTermRainfall = shortTermTemporaryRainfallService.tryInterpolate24HourForecast(
            item.getEndTime(), mapping.getCenterLon(), mapping.getCenterLat());
        if (shortTermRainfall != null) {
            item.setRainfall(roundRainfall(shortTermRainfall));
        }
    }

    private boolean isEligible(RainfallEntityGridMapping mapping) {
        return mapping != null
            && Integer.valueOf(SHORT_TERM_RANGE_FLAG).equals(mapping.getInShortTermTemporaryRainfall5km())
            && mapping.getCenterLon() != null
            && mapping.getCenterLat() != null;
    }

    private double defaultValue(Double rainfall) {
        return rainfall == null ? 0D : rainfall;
    }

    private double roundRainfall(double rainfall) {
        return Math.round(rainfall * 100D) / 100D;
    }
}
