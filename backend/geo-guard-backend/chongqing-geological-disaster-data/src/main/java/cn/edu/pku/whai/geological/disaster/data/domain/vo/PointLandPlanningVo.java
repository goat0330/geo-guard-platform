/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 点位国土空间规划用地类型内部视图对象
 *
 * @author zhuzc
 * @date 2026-06-25
 */
@Data
public class PointLandPlanningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用地类型名称
     */
    private String landName;

    /**
     * 用地类型
     */
    private String landCategoryName;
}
