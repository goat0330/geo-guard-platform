/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 处置任务风险区建筑与人口统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DzTaskHandleRiskAreaStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 建筑总数
     */
    private Long buildingTotal;

    /**
     * 人口总数
     */
    private Long populationTotal;

    /**
     * 60岁及以上人口占比，保留两位小数
     */
    private BigDecimal age60AndAboveRatio;
}
