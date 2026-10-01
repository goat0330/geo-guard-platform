/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 斜坡单元基础信息视图对象
 *
 * @author zhuzc
 */
@Data
public class SlopeUnitGeologyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符（斜坡单元主键）
     */
    private String id;

    /**
     * 斜坡单元名称
     */
    private String name;

    /**
     * 村地址
     */
    private String villageAddress;

    /**
     * 中心点坐标
     */
    private String center;

    /**
     * 斜坡类型
     */
    private String slopeType;

    /**
     * 涉水河流、沟谷
     */
    private String waterRiverValley;

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
