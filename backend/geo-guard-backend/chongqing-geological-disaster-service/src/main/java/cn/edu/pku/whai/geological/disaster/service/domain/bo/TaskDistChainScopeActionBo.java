/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 按链路范围执行任务批量操作请求对象。
 */
@Data
public class TaskDistChainScopeActionBo {

    /**
     * 单个任务 id。
     */
    private Long taskId;

    /**
     * 多个任务 id。
     */
    private List<Long> taskIds;

    /**
     * 显式链路 id 集合，按精确匹配处理。
     */
    private List<String> chainIds;

    /**
     * latest-process-node 同口径筛链条件。
     */
    private TaskProcessChainFilterBo latestProcessNodeFilter;

    /**
     * 是否返回解析调试摘要。
     */
    private Boolean returnDebugInfo;
}
