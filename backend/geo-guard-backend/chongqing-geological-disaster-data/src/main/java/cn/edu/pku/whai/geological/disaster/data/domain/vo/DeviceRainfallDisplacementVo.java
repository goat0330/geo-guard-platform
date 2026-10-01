/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备雨量与位移统计结果
 */
@Data
public class DeviceRainfallDisplacementVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 最近1小时降雨量(mm)
     */
    private Double last1HourRainfall;

    /**
     * 最近24小时降雨量(mm)
     */
    private Double last24HoursRainfall;

    /**
     * 最近7天降雨量(mm)
     */
    private Double last7DaysRainfall;

    /**
     * 近1天位移(m)
     */
    private Double last1DayDisplacement;

    /**
     * 近3天位移(m)
     */
    private Double last3DaysDisplacement;

    /**
     * 近7天位移(m)
     */
    private Double last7DaysDisplacement;
}
