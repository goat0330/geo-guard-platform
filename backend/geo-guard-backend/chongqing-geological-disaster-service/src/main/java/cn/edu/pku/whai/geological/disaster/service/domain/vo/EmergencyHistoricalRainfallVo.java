/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 应急调查报告历史降雨响应对象
 *
 * @author zhuzc
 * @date 2026-06-15
 */
@Data
public class EmergencyHistoricalRainfallVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元ID。
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
     * 按 24 小时跨度展示的雨量序列。
     */
    private List<RainfallPeriodItem> hourlySeries = new ArrayList<>();

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
    public static class RainfallPeriodItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 统计区间开始时间（包含）。
         */
        private LocalDateTime startTime;

        /**
         * 统计区间结束时间（不包含）。
         */
        private LocalDateTime endTime;

        /**
         * 区间累计雨量。
         */
        private Double rainfall;

        /**
         * 数据来源（actual/forecast/missing）。
         */
        private String source;
    }
}
