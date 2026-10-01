/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdRegionStatVo {
    /**
     * 地理范围 m^2
     */
    private BigDecimal geoScope;

    /**
     * 人口底数（人）
     */
    private Integer population;

    /**
     * 建筑数量（栋）
     */
    private Integer buildingNum;

    /**
     * 斜坡单元数量（个）
     */
    private Long slopeUnitNum;
}
