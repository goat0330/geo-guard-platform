/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 按链路范围执行任务批量操作结果。
 */
@Data
public class TaskDistChainScopeActionResultVo {

    /**
     * 结果编码：SUCCESS/PARTIAL_SUCCESS/NO_TASK_MATCH
     */
    private String resultCode;

    /**
     * 结果说明。
     */
    private String resultMessage;

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
     * 去重后的任务数。
     */
    private Integer deduplicatedTaskCount;

    /**
     * 成功处理任务数。
     */
    private Integer successCount;

    /**
     * 跳过任务数。
     */
    private Integer skippedCount;

    /**
     * 失败任务数。
     */
    private Integer failedCount;

    /**
     * 失败或跳过明细。
     */
    private List<TaskActionFailure> failures = new ArrayList<>();

    /**
     * 失败或跳过任务明细。
     */
    @Data
    public static class TaskActionFailure {

        /**
         * 任务 id。
         */
        private Long taskId;

        /**
         * 失败或跳过原因。
         */
        private String reason;
    }
}
