/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 国土空间规划用地类型视图对象
 *
 * @author zhuzc
 * @date 2026-06-25
 */
@Data
public class LandPlanningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用地类型名称
     */
    private String landName;

    /**
     * 覆盖面积/空间交集面面积（平方米）
     */
    private BigDecimal intersectionArea;
}
