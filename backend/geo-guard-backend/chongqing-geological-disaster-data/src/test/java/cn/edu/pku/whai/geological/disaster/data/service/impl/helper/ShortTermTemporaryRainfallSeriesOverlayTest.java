/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.service.IShortTermTemporaryRainfallQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ShortTermTemporaryRainfallSeriesOverlayTest {

    /**
     * 用真实 Spring 构造器解析回归分步迁移场景，确保缺失业务扩展不阻断 data 装配。
     */
    @Test
    void shouldStartWithoutOptionalQueryServiceAndKeepOriginalRainfall() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(ShortTermTemporaryRainfallSeriesOverlay.class);
            context.refresh();
            var item = period(LocalDateTime.of(2026, 7, 10, 8, 0), 12D, "actual");
            context.getBean(ShortTermTemporaryRainfallSeriesOverlay.class)
                .apply(List.of(item), mapping(1), Map.of());
            assertThat(item.getRainfall()).isEqualTo(12D);
        }
    }

    /**
     * 验证未来迁入实现后 Spring 会自动接入真实扩展，不会永久退化为缺省分支。
     */
    @Test
    void shouldUseQueryServiceWhenRegisteredInSpring() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(IShortTermTemporaryRainfallQueryService.class, () -> rainfallService);
            context.register(ShortTermTemporaryRainfallSeriesOverlay.class);
            context.refresh();
            var item = period(LocalDateTime.of(2026, 7, 10, 8, 0), 12D, "forecast");
            when(rainfallService.tryInterpolate24HourForecast(item.getEndTime(), 109.21D, 30.42D))
                .thenReturn(19D);
            context.getBean(ShortTermTemporaryRainfallSeriesOverlay.class)
                .apply(List.of(item), mapping(1), Map.of());
            assertThat(item.getRainfall()).isEqualTo(19D);
        }
    }

    private final IShortTermTemporaryRainfallQueryService rainfallService =
        mock(IShortTermTemporaryRainfallQueryService.class, invocation -> null);
    private final ShortTermTemporaryRainfallSeriesOverlay overlay =
        new ShortTermTemporaryRainfallSeriesOverlay(rainfallService);

    @Test
    void shouldReplaceAvailableObservationByCoordinateAndHourThenRecalculatePeriod() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 7, 10, 8, 0);
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item = period(periodStart, 12D, "actual");
        Map<LocalDateTime, Double> hourlyRainfall = new LinkedHashMap<>();
        hourlyRainfall.put(periodStart.plusHours(2), 5D);
        hourlyRainfall.put(periodStart.plusHours(3), 7D);
        when(rainfallService.tryInterpolateObservation(periodStart.plusHours(2), 109.21D, 30.42D)).thenReturn(9D);
        when(rainfallService.tryInterpolateObservation(periodStart.plusHours(3), 109.21D, 30.42D)).thenReturn(null);

        overlay.apply(List.of(item), mapping(1), hourlyRainfall);

        assertThat(item.getRainfall()).isEqualTo(16D);
    }

    @Test
    void shouldSupplementShortTermObservationWhenOriginalGridHourIsMissing() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 7, 10, 8, 0);
        LocalDateTime missingGridHour = periodStart.plusHours(8);
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item = period(periodStart, 0.33D, "actual");
        Map<LocalDateTime, Double> hourlyRainfall = Map.of(periodStart.plusHours(11), 0.33D);
        when(rainfallService.tryInterpolateObservation(missingGridHour, 109.21D, 30.42D)).thenReturn(249.31D);

        overlay.apply(List.of(item), mapping(1), hourlyRainfall);

        assertThat(item.getRainfall()).isEqualTo(249.64D);
    }

    @Test
    void shouldReplaceForecastWith24HourValueAtPeriodEnd() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 7, 11, 8, 0);
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item = period(periodStart, 20D, "forecast");
        when(rainfallService.tryInterpolate24HourForecast(periodStart.plusDays(1), 109.21D, 30.42D)).thenReturn(42D);

        overlay.apply(List.of(item), mapping(1), Map.of());

        assertThat(item.getRainfall()).isEqualTo(42D);
    }

    @Test
    void shouldKeepOriginalValuesOutsideShortTermRange() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 7, 10, 8, 0);
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem actual = period(periodStart, 12D, "actual");
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem forecast = period(periodStart.plusDays(1), 20D, "forecast");

        overlay.apply(List.of(actual, forecast), mapping(0), Map.of(periodStart.plusHours(1), 12D));

        assertThat(actual.getRainfall()).isEqualTo(12D);
        assertThat(forecast.getRainfall()).isEqualTo(20D);
        verify(rainfallService, never()).tryInterpolateObservation(periodStart.plusHours(1), 109.21D, 30.42D);
        verify(rainfallService, never()).tryInterpolate24HourForecast(periodStart.plusDays(2), 109.21D, 30.42D);
    }

    private static SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem period(
        LocalDateTime start, double rainfall, String source) {
        SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem item = new SlopeUnitForecastRainfallSeriesVo.RainfallPeriodItem();
        item.setStartTime(start);
        item.setEndTime(start.plusDays(1));
        item.setRainfall(rainfall);
        item.setSource(source);
        return item;
    }

    private static RainfallEntityGridMapping mapping(int inRange) {
        RainfallEntityGridMapping mapping = new RainfallEntityGridMapping();
        mapping.setCenterLon(109.21D);
        mapping.setCenterLat(30.42D);
        mapping.setInShortTermTemporaryRainfall5km(inRange);
        return mapping;
    }
}
