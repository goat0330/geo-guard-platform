/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 点查建筑人口画像返回对象
 */
@Data
public class BuildingPopulationProfileVo {

    /**
     * 建筑名称
     */
    private String buildingName;

    /**
     * 建筑面积
     */
    private BigDecimal buildingArea;

    /**
     * 人数
     */
    private Long personCount;

    /**
     * 0-18岁人数
     */
    private Long age0To18Count;

    /**
     * 18-45岁人数
     */
    private Long age18To45Count;

    /**
     * 45-65岁人数
     */
    private Long age45To65Count;

    /**
     * 65岁及以上人数
     */
    private Long age65AndAboveCount;

    /**
     * 男性占比
     */
    private BigDecimal maleRatio;

    /**
     * 女性占比
     */
    private BigDecimal femaleRatio;

    /**
     * 承载体WKT
     */
    private String carrierWkt;
}
