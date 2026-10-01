/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 降雨实况栅格单元视图对象
 * <p>
 * 对应观测栅格中的单个网格点（经度、纬度、降雨量）。
 *
 * @author system
 * @date 2026-01-29
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = RainfallLogRasterUnit.class)
public class RainfallLogRasterUnitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private Double lon;

    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private Double lat;

    /**
     * 降雨量（单位：mm）
     */
    @ExcelProperty(value = "降雨量(mm)")
    private Double rainfall;

    /**
     * 记录时间
     */
    private Date logTime;

    /**
     * 创建时间（对应栅格数据入库时间）
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
