/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 今日降雨量视图对象。
 * <p>
 * 用于向前端返回按街道/乡镇统计的今日降雨量数据。
 *
 * @author system
 * @date 2026-04-20
 */
@Data
public class TodayRainfallVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 街道/乡镇名称。
     */
    private String street;

    /**
     * 今日降雨量。
     */
    private Double rainfall;

    /**
     * 街道/乡镇中心点坐标，格式：[经度, 纬度]。
     */
    private List<Double> coordinates;
}
