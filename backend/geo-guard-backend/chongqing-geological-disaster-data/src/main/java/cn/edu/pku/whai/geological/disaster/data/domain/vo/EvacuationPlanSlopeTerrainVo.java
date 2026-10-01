/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 撤离路线查询关联的斜坡单元地形信息。
 */
@Data
public class EvacuationPlanSlopeTerrainVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 坡向。
     */
    private String slopeDirection;

    /**
     * 坡度。
     */
    private String slopeAngle;
}
