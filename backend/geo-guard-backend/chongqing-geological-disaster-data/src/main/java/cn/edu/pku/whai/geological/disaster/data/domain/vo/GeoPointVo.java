/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 经纬度坐标点。
 *
 * @author whai
 * @date 2026-05-14
 */
@Data
public class GeoPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 经度。
     */
    private Double lon;

    /**
     * 纬度。
     */
    private Double lat;
}
