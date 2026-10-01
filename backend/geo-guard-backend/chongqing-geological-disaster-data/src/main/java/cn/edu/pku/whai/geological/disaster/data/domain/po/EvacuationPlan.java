/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 撤离方案表（表名：dz_evacuation_plan）
 *
 * @author whai
 * @date 2026-02-04
 */
@Data
@TableName("dz_evacuation_plan")
public class EvacuationPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 方案编号
     */
    private Long handleId;

    /**
     * 撤离区域
     */
    private String evacuationArea;

    /**
     * 风险区域范围（WKT）
     */
    private String evacuationAreaWkt;

    /**
     * 安置点
     */
    private String resettlementLocation;

    /**
     * 距离(单位:km)
     */
    private BigDecimal distanceKm;

    /**
     * 预计耗时（分钟）
     */
    private Integer estimatedTimeMinutes;

    /**
     * 撤离方向
     */
    private String evacuationRoute;

    /**
     * 撤离路线（PostGIS geometry，查询时用 ST_AsText 转 WKT 字符串）
     */
    private String evacuationRoad;

    /**
     * 风险区到路网连接线（PostGIS MULTILINESTRING，查询时用 ST_AsText 转 WKT 字符串）
     */
    private String evacuationAreaToRoad;

    /**
     * 路网到安置区连接线（PostGIS MULTILINESTRING，查询时用 ST_AsText 转 WKT 字符串）
     */
    private String roadToResettlement;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 安置点区域（WKT）
     */
    private String resettlementWkt;

    /**
     * 安置点坐标（JSON）
     */
    private String resettlementPoint;

    /**
     * 住户信息（JSON）
     */
    private String residentsInfo;
}
