/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 斜坡单元当天24小时雨量响应对象。
 *
 * @author system
 * @date 2026-04-27
 */
@Data
public class SlopeUnitTodayHourlyRainfallVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元ID。
     */
    private String slopeUnitId;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 24小时雨量序列。
     */
    private List<HourlyRainfallItem> hourlySeries;

    /**
     * 当天累计雨量。
     */
    private Double dailyTotalRainfall;

    /**
     * 历史命中的小时数。
     */
    private Integer historyHourCount;

    /**
     * 预测回填的小时数。
     */
    private Integer forecastFallbackHourCount;

    /**
     * 未命中数据的小时数。
     */
    private Integer missingHourCount;

    /**
     * 小时雨量项。
     */
    @Data
    public static class HourlyRainfallItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 小时开始时间。
         */
        private LocalDateTime hourTime;

        /**
         * 小时雨量。
         */
        private Double rainfall;

        /**
         * 数据来源（history/forecast_fallback/missing）。
         */
        private String source;
    }
}
