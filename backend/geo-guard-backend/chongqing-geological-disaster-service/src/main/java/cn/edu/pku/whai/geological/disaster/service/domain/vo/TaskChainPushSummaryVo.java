/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

/**
 * 按链路范围推送解析摘要。
 */
@Data
public class TaskChainPushSummaryVo {

    /**
     * 输入显式链路数。
     */
    private Integer inputChainIdCount;

    /**
     * 条件筛出链路数。
     */
    private Integer filteredChainIdCount;

    /**
     * 最终生效链路数。
     */
    private Integer finalChainIdCount;

    /**
     * 展开关联任务总数（包含重复任务）。
     */
    private Integer expandedTaskCount;

    /**
     * 状态为 1 的任务数。
     */
    private Integer statusOneTaskCount;

    /**
     * 去重后的任务数。
     */
    private Integer deduplicatedTaskCount;

    /**
     * 旧推送逻辑最终命中的任务数。
     */
    private Integer finalMatchedTaskCount;

    /**
     * 无权限或被过滤的任务数。
     */
    private Integer deniedTaskCount;
}
