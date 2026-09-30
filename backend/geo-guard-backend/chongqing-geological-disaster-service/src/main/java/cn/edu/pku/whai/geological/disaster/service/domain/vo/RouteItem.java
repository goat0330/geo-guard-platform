/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 疏散路线明细
 *
 * @author whai
 */
@Data
public class RouteItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 风险点标识
     */
    @JsonProperty("hazard_label")
    private String hazardLabel;

    /**
     * 风险点坐标
     */
    @JsonProperty("hazard_point")
    private EvacuationRouteResultVo.GeoPoint hazardPoint;

    /**
     * 避险安置点名称
     */
    @JsonProperty("resettlement_location")
    private String resettlementLocation;

    /**
     * 避险安置点坐标
     */
    @JsonProperty("resettlement_point")
    private EvacuationRouteResultVo.GeoPoint resettlementPoint;

    /**
     * 避险安置点范围WKT
     */
    @JsonProperty("resettlement_wkt")
    private String resettlementWkt;

    /**
     * 住户信息
     */
    @JsonProperty("residents_info")
    private EvacuationRouteResultVo.ResidentsInfo residentsInfo;

    /**
     * 距离（公里）
     */
    @JsonProperty("distance_km")
    private Double distanceKm;

    /**
     * 预计耗时（分钟）
     */
    @JsonProperty("estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    /**
     * 撤离路线说明
     */
    @JsonProperty("evacuation_route")
    private String evacuationRoute;

    /**
     * 路径WKT
     */
    @JsonProperty("path_wkt")
    private String pathWkt;

    /**
     * 路径坐标点
     */
    @JsonProperty("path")
    private List<EvacuationRouteResultVo.GeoPoint> path;
}
