/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 撤离路线明细视图对象。
 *
 * @author whai
 * @date 2026-04-29
 */
@Data
public class EvacuationPlanRouteDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 撤离区域名称。
     */
    private String evacuationArea;

    /**
     * 安置点名称，取自撤离方案的 resettlement_location。
     */
    @ExcelProperty(value = "安置点")
    private String resettlementPoint;

    /**
     * 距离（单位：km）。
     */
    private BigDecimal distanceKm;

    /**
     * 预计耗时（分钟）。
     */
    @ExcelProperty(value = "预计耗时（分钟）")
    private Integer estimatedTimeMinutes;

    /**
     * 撤离方向。
     */
    private String evacuationDirection;

    /**
     * 撤离路线（WKT）。
     */
    private String evacuationRoad;

    /**
     * 风险区到路网连接线（MULTILINESTRING WKT）。
     */
    private String evacuationAreaToRoad;

    /**
     * 路网到安置区连接线（MULTILINESTRING WKT）。
     */
    private String roadToResettlement;

    /**
     * 住户信息。
     */
    private ResidentsInfoVo residentsInfo;
}
