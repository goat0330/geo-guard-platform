/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 斜坡单元预报雨量序列响应对象。
 *
 * @author system
 * @date 2026-05-17
 */
@Data
public class SlopeUnitForecastRainfallSeriesVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元ID。
     */
    private String slopeUnitId;

    /**
     * 分期雨量序列。
     */
    private List<RainfallPeriodItem> rainfallSeries;

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
         * 数据来源（actual/forecast）。
         */
        private String source;
    }
}
