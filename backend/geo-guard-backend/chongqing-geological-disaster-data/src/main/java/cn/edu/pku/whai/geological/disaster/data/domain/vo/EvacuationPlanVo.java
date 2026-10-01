/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 撤离方案表视图对象
 *
 * @author whai
 * @date 2026-02-04
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = EvacuationPlan.class)
public class EvacuationPlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "方案编号")
    private Long handleId;

    @ExcelProperty(value = "撤离区域")
    private String evacuationArea;

    @ExcelProperty(value = "风险区域范围")
    private String evacuationAreaWkt;

    @ExcelProperty(value = "安置点")
    private String resettlementLocation;

    @ExcelProperty(value = "距离(km)")
    private BigDecimal distanceKm;

    /**
     * 预计耗时（分钟）
     */
    @ExcelProperty(value = "预计耗时（分钟）")
    private Integer estimatedTimeMinutes;

    @ExcelProperty(value = "撤离方向")
    private String evacuationRoute;

    /**
     * 撤离路线（WKT 字符串）
     */
    @ExcelProperty(value = "撤离路线")
    private String evacuationRoad;

    @ExcelProperty(value = "风险区到路网")
    private String evacuationAreaToRoad;

    @ExcelProperty(value = "路网到安置区")
    private String roadToResettlement;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "安置点区域")
    private String resettlementWkt;

    @ExcelProperty(value = "安置点坐标")
    private String resettlementPoint;

    @ExcelProperty(value = "住户信息")
    private String residentsInfo;
}
