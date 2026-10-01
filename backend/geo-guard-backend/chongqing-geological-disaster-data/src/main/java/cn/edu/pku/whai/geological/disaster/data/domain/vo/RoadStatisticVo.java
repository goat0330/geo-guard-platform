/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 道路面内明细视图对象
 * <p>
 * 一条道路一行,展示道路名称、等级,以及道路与查询面(ST_Intersection)相交后的长度(米)。
 * 总长度请由调用方对该字段求和。
 *
 * @author zhuzc
 * @date 2026-06-09
 */
@Data
public class RoadStatisticVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 道路名称
     */
    private String roadName;

    /**
     * 道路等级/类型
     */
    private String roadLevel;

    /**
     * 道路在查询面内的长度（米），由 ST_Intersection 几何求交后 ST_Length 计算
     */
    private BigDecimal lengthInWkt;
}
