package cn.edu.pku.whai.geological.disaster.service.dify.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.TaskReplyBo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.ITaskReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * llm任务处理
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/dizai/llm/reply")
@RequiredArgsConstructor
public class TaskReplyController {

    private final ITaskReplyService taskReplyService;

    /**
     * llm任务处理
     *
     * @param bo
     * @return
     */
    @Log(title = "LLM任务处理", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("handle")
    public R<Void> handle(@RequestBody TaskReplyBo bo) {
        taskReplyService.handle(bo);
        return R.ok();
    }

}
