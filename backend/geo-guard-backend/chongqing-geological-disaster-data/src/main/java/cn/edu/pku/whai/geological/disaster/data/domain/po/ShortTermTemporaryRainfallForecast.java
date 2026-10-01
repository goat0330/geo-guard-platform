/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 短期临时雨量站累计预报雨量。 */
@Data
@TableName("data_short_term_temporary_rainfall_forecast")
public class ShortTermTemporaryRainfallForecast implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private LocalDateTime issueTime;
    private LocalDateTime forecastTime;
    private Integer periodHours;
    private Double rainfall;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
