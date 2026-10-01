/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 斜坡单元按日雨量统计明细对象。
 *
 * @author system
 * @date 2026-05-17
 */
@Data
public class SlopeUnitDailyRainfallVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 该统计日日累计雨量。
     */
    private Double dailyCumulativeRainfall;
}
