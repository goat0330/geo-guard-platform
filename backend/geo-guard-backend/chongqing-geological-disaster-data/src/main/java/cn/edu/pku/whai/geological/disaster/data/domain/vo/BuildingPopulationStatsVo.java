/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

/**
 * 按建筑编码聚合的人口统计
 */
@Data
public class BuildingPopulationStatsVo {

    /**
     * 总人数
     */
    private Long personCount;

    /**
     * 0-18岁人数
     */
    private Long age0To18Count;

    /**
     * 18-45岁人数
     */
    private Long age18To45Count;

    /**
     * 45-65岁人数
     */
    private Long age45To65Count;

    /**
     * 65岁及以上人数
     */
    private Long age65AndAboveCount;

    /**
     * 男性人数
     */
    private Long maleCount;

    /**
     * 女性人数
     */
    private Long femaleCount;
}
