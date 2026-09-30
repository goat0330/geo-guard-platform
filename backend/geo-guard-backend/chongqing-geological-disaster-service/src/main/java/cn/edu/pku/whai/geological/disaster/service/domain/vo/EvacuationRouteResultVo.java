/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 疏散路线生成结果
 *
 * @author whai
 */
@Data
public class EvacuationRouteResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处置主键
     */
    @JsonProperty("handle_id")
    private Long handleId;

    /**
     * 斜坡单元ID
     */
    @JsonProperty("slope_unit_id")
    private String slopeUnitId;

    /**
     * 风险点数量
     */
    @JsonProperty("hazard_points_count")
    private Integer hazardPointsCount;

    /**
     * 路线列表
     */
    @JsonProperty("routes")
    private List<RouteItem> routes;

    /**
     * 跳过生成的风险点
     */
    @JsonProperty("skipped")
    private List<SkippedItem> skipped;

    /**
     * 将三方旧版 residents_info 内的预计耗时提升到路线层级。
     */
    public void normalizeLegacyEstimatedTimeMinutes() {
        if (routes == null || routes.isEmpty()) {
            return;
        }
        for (RouteItem route : routes) {
            if (route == null || route.getEstimatedTimeMinutes() != null || route.getResidentsInfo() == null) {
                continue;
            }
            route.setEstimatedTimeMinutes(route.getResidentsInfo().getEstimatedTimeMinutes());
        }
    }

    /**
     * 跳过生成的风险点明细
     */
    @Data
    public static class SkippedItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 风险点序号
         */
        @JsonProperty("hazard_index")
        private Integer hazardIndex;

        /**
         * 风险点标识
         */
        @JsonProperty("hazard_label")
        private String hazardLabel;

        /**
         * 风险点坐标
         */
        @JsonProperty("hazard_point")
        private GeoPoint hazardPoint;

        /**
         * 匹配状态
         */
        @JsonProperty("match_status")
        private String matchStatus;

        /**
         * 跳过原因
         */
        @JsonProperty("reason")
        private String reason;
    }

    /**
     * 经纬度坐标
     */
    @Data
    public static class GeoPoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 经度
         */
        @JsonProperty("lon")
        private Double lon;

        /**
         * 纬度
         */
        @JsonProperty("lat")
        private Double lat;
    }

    /**
     * 受影响住户信息
     */
    @Data
    public static class ResidentsInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否有住户
         */
        @JsonProperty("has_residents")
        private Boolean hasResidents;

        /**
         * 住户总人数
         */
        @JsonProperty("total_count")
        private Integer totalCount;

        /**
         * 老人数量
         */
        @JsonProperty("elderly_count")
        private Integer elderlyCount;

        /**
         * 儿童数量
         */
        @JsonProperty("children_count")
        private Integer childrenCount;

        /**
         * 户主电话
         */
        @JsonProperty("household_head_phone")
        private String householdHeadPhone;

        /**
         * 户主姓名
         */
        @JsonProperty("household_head_name")
        private String householdHeadName;

        /**
         * 三方旧版嵌套预计耗时，仅用于反序列化兼容；对外不再嵌套返回。
         */
        @JsonProperty(value = "estimated_time_minutes", access = JsonProperty.Access.WRITE_ONLY)
        private Integer estimatedTimeMinutes;
    }
}
