/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("data_rainfall_forecast_raster_unit")
public class RainfallForecastRasterUnit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 经度
     */
    private Double lon;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 降雨量（单位：mm）
     */
    private Double rainfall;

    /**
     * 预报时间
     */
    private Date forecastTime;

    /**
     * 创建时间（对应栅格数据入库时间）
     */
    private Date createTime;

}
