/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 斜坡单元按日雨量统计列表对象。
 *
 * @author system
 * @date 2026-05-17
 */
@Data
public class SlopeUnitDailyRainfallSeriesVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 斜坡单元ID。
     */
    private String slopeUnitId;

    /**
     * 按日雨量统计列表。
     */
    private List<SlopeUnitDailyRainfallVo> dailyRainfallList;

    /**
     * 按日预报雨量统计列表。
     */
    private List<RainfallForecastDailyStatisticVo> dailyForcastList;
}
