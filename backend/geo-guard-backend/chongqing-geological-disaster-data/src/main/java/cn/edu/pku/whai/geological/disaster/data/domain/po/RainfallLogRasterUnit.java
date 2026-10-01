/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 降雨栅格单元对象 rainfall_record
 * <p>
 * 对应观测/预报栅格中的单个网格点（经度、纬度、降雨量）。
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@TableName("data_rainfall_log_raster_unit")
public class RainfallLogRasterUnit implements Serializable {

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
     * 记录时间
     */
    private Date logTime;

    /**
     * 创建时间（对应栅格数据入库时间）
     */
    private Date createTime;
}
