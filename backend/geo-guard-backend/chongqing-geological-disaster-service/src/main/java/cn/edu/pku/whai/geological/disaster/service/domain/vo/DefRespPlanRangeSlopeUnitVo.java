/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 防御响应范围斜坡单元
 */
@Data
public class DefRespPlanRangeSlopeUnitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属乡镇/街道名称
     */
    private String street;

    /**
     * 斜坡单元ID
     */
    private String slopeUnitId;

    /**
     * 中心点
     */
    private String center;

    /**
     * 边界多边形
     */
    private String wkt;
}
