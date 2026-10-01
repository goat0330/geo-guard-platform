/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 降雨数据视图对象
 *
 * @author system
 * @date 2026-01-29
 */
@Data
public class RainfallData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 经度
     */
    @ExcelProperty("经度")
    private Double lon;

    /**
     * 纬度
     */
    @ExcelProperty("纬度")
    private Double lat;

    /**
     * 降雨量（单位：mm）
     */
    @ExcelProperty("降雨量（单位：mm）")
    private Double totalRainfall;
}
