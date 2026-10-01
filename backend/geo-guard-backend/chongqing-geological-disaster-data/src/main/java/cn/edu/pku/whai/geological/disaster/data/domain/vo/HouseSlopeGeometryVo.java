/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 房屋与斜坡单元几何信息。
 */
@Data
public class HouseSlopeGeometryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房屋ID。
     */
    private String houseId;

    /**
     * 建筑编码。
     */
    private String buildingCode;

    /**
     * 建筑名称。
     */
    private String buildingName;

    /**
     * 斜坡单元ID。
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称。
     */
    private String slopeUnitName;

    /**
     * 房屋几何 WKT。
     */
    private String geometry;
}
