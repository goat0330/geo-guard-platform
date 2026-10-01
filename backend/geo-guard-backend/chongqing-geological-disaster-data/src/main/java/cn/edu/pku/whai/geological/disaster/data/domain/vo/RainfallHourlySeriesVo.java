/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 按小时雨量序列响应对象。
 *
 * @author system
 * @date 2026-04-28
 */
@Data
public class RainfallHourlySeriesVo implements Serializable {

    public static final String SOURCE_HISTORY = "history";

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元ID（按行政区划查询时为空）。
     */
    private String slopeUnitId;

    /**
     * 起始时间（包含）。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（不包含）。
     */
    private LocalDateTime endTime;

    /**
     * 小时雨量序列。
     */
    private List<HourlyRainfallItem> hourlySeries;

    /**
     * 区间累计雨量。
     */
    private Double totalRainfall;

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
