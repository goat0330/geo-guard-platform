/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 斜坡单元地质信息对象 data_slope_geology
 *
 * @author zhuzc
 */
@Data
@TableName("data_slope_geology")
public class DataSlopeGeology implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 关联斜坡单元表ID
     */
    @TableField("slope_unit_id")
    private String slopeUnitId;

    /**
     * 斜坡类型
     */
    private String slopeType;

    /**
     * 涉水河流、沟谷
     */
    @TableField("water_river_valley")
    private String waterRiverValley;

    /**
     * 坡顶高程（m）
     */
    @TableField("top_elevation")
    private BigDecimal topElevation;

    /**
     * 坡底高程（m）
     */
    @TableField("bottom_elevation")
    private BigDecimal bottomElevation;

    /**
     * 顺坡长度（m）
     */
    @TableField("slope_length")
    private BigDecimal slopeLength;

    /**
     * 坡形
     */
    private String slopeShape;

    /**
     * 斜坡地形地貌、河流冲沟切割及斜坡形体特征
     */
    private String landformFeature;

    /**
     * 斜坡地层岩性
     */
    private String stratumLithology;

    /**
     * 斜坡组合结构类型及特征
     */
    private String structureFeature;

    /**
     * 斜坡水文地质条件
     */
    private String hydrogeology;

    /**
     * 斜坡土地及植被
     */
    private String landVegetation;

    /**
     * 控制崩滑结构面
     */
    private String controlStructuralPlane;

    /**
     * 斜坡稳定性影响因素
     */
    private String stabilityFactors;

    /**
     * 斜坡稳定性分析现状
     */
    private String stabilityCurrent;

    /**
     * 斜坡稳定性分析趋势
     */
    private String stabilityTrend;

}
