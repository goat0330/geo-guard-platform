/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前方案撤离路线查询结果。
 *
 * @author whai
 * @date 2026-04-29
 */
@Data
public class EvacuationPlanRouteResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处置主键。
     */
    private Long handleId;

    /**
     * 坡向。
     */
    private String slopeDirection;

    /**
     * 坡度。
     */
    private String slopeAngle;

    /**
     * 风险区域范围（WKT）列表。
     */
    private List<String> disasterPolygonsWktList;

    /**
     * 安置点列表（name + areaWkt）。
     */
    private List<ResettlementAreaItem> resettlementAreas;

    /**
     * 撤离路线列表。
     */
    private List<EvacuationPlanRouteDetailVo> routes;

    @Data
    public static class ResettlementAreaItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 安置点名称。
         */
        private String name;

        /**
         * 安置点区域（WKT）。
         */
        private String areaWkt;

        /**
         * 安置点坐标。
         */
        private GeoPointVo point;
    }
}
