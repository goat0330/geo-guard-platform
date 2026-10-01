/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 雨量统计对象与网格映射。
 *
 * @author system
 * @date 2026-04-22
 */
@Data
@TableName("data_rainfall_entity_grid_mapping")
public class RainfallEntityGridMapping implements Serializable {

    public static final String TYPE_SLOPE_UNIT = "SLOPE_UNIT";
    public static final String TYPE_AD_REGION = "AD_REGION";

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String entityType;

    private String slopeUnitId;

    private String adRegionId;

    private Integer adRegionLevel;

    private String center;

    private Double centerLon;

    private Double centerLat;

    /** 是否位于任一短期临时雨量站点 5 公里范围内：1-是，0-否。 */
    @TableField(value = "in_short_term_temporary_rainfall_5km",
        insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer inShortTermTemporaryRainfall5km;

    private Double gridLon;

    private Double gridLat;

    private Double forecastGridLon;

    private Double forecastGridLat;

    private String province;

    private String city;

    private String county;

    private String street;

    private String village;

    private String community;

    private String provinceCode;

    private String cityCode;

    private String countyCode;

    private String streetCode;

    private String villageCode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
