/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日实际雨量累加器（内部类提取）。
 */
public final class DailyActualAccumulator {
    public final RainfallEntityGridMapping mapping;
    public final LocalDate statDate;
    public double dailyCumulativeRainfall;
    public double latestHourRainfall;
    public LocalDateTime latestHourTime;

    public DailyActualAccumulator(RainfallEntityGridMapping mapping, LocalDate statDate) {
        this.mapping = mapping;
        this.statDate = statDate;
    }

    public void add(LocalDateTime logTime, Double rainfall) {
        double value = rainfall == null ? 0D : rainfall;
        dailyCumulativeRainfall += value;
        if (latestHourTime == null || logTime.isAfter(latestHourTime)) {
            latestHourTime = logTime;
            latestHourRainfall = value;
            return;
        }
        if (logTime.equals(latestHourTime)) {
            latestHourRainfall += value;
        }
    }
}
