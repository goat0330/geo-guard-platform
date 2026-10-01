/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 距离点位最近的房屋记录
 */
@Data
public class NearestHouseCimVo {

    /**
     * 建筑编码
     */
    private String buildingCode;

    /**
     * 建筑名称
     */
    private String buildingName;

    /**
     * 面积
     */
    private BigDecimal area;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * WKT
     */
    private String wkt;
}
