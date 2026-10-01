/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import lombok.Data;

/**
 * @author 防御响应方案范围
 */
@Data
public class DefRespRange {

    /**
     * 所属乡镇/街道名称
     */
    private String street;

    /**
     * 斜坡单元id
     */
    private String slopeUnitId;

    /**
     * 几何中心点
     */
    private String center;

    /**
     * 边界多边形
     */
    private String wkt;
}
