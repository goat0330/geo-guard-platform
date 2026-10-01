/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 未来逐日预报雨量统计视图对象。
 *
 * @author system
 * @date 2026-04-08
 */
@Data
public class RainfallForecastDailyStatisticVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 当日预报降雨量。
     * 该值由当天所有预报时次的左下角最近网格雨量累计得到。
     */
    private Double dailyForecastRainfall;
}
