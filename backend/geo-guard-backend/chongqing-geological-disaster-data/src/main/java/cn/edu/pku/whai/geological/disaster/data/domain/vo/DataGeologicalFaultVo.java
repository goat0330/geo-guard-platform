/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地质断层 VO（含 11 原始字段 + 3 计算字段：距离/方位角度/方位名）
 *
 * @author zhuzc
 * @date 2026-06-17
 */
@Data
public class DataGeologicalFaultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String wkt;
    private String name;
    private String strike;
    private String length;
    private String width;
    private String feature;
    private String property;
    private String dipDirection;
    private String dipAngle;
    private String geom;

    /** 距离点位距离（米，ST_Distance 球面） */
    private Double distance;

    /** 方位角度（0-360°，以垂点为基准指向点位） */
    private Double azimuth;

    /** 8 方位名（N/E/S/W/NE/NW/SE/SW） */
    private String azimuthName;
}
