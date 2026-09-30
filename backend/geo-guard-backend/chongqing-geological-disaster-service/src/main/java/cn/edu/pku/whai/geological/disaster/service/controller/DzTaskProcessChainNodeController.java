/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainSummaryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务流程链路节点
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskProcessChainNode")
public class DzTaskProcessChainNodeController extends BaseController {

    private final IDzTaskProcessChainSummaryService taskProcessChainSummaryService;

    /**
     * 查询每条可展示主链的最新流程节点。
     */
    @GetMapping("/list/latest-process-node")
    public TableDataInfo<TaskProcessChainSummaryVo> listLatestProcessNode(TaskProcessChainSummaryBo bo,
                                                                          PageQuery pageQuery) {
        return taskProcessChainSummaryService.queryPageList(bo, pageQuery);
    }
}
