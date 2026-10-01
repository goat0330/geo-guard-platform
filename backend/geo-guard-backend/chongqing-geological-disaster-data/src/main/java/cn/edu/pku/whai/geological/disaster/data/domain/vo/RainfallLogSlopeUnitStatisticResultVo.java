/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 斜坡单元雨量统计查询结果。
 * 包含实况雨量列表和未来三天预报雨量列表。
 *
 * @author system
 * @date 2026-04-08
 */
@Data
public class RainfallLogSlopeUnitStatisticResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实况雨量列表。
     */
    private List<RainfallLogSlopeUnitStatisticVo> actualStatistics;

    /**
     * 未来三天预报雨量列表。
     */
    private List<RainfallForecastDailyStatisticVo> forecastStatistics;
}
