/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 短期临时雨量站整点小时实况雨量。 */
@Data
@TableName("data_short_term_temporary_rainfall_observation")
public class ShortTermTemporaryRainfallObservation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private LocalDateTime logTime;
    private Double rainfall;
    private Double cumulativeRainfall;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
