/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户行政区划分布统计视图对象。
 */
@Data
public class DzUserAdRegionStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 行政区划ID。
     */
    private String adRegionId;

    /**
     * 行政区划名称。
     */
    private String adRegionName;

    /**
     * 行政区划层级:1省2市3县4乡镇5村。
     */
    private Integer adRegionLevel;

    /**
     * 绑定用户数量。
     */
    private Long userCount;
}
