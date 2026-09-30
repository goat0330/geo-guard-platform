package cn.edu.pku.whai.geological.disaster.service.dify.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailFeedbacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.FeedBacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailFeedbacksVo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailFeedbacksService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 对话历史记录详情反馈
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/ai/agent/feedbacks")
public class AiChatHistoryDetailFeedbacksController extends BaseController {

    private final IAiChatHistoryDetailFeedbacksService aiChatHistoryDetailFeedbacksService;

    /**
     * 查询对话历史记录详情反馈列表
     */
    @GetMapping("/list")
    public TableDataInfo<AiChatHistoryDetailFeedbacksVo> list(AiChatHistoryDetailFeedbacksBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());

        return aiChatHistoryDetailFeedbacksService.queryPageList(bo, pageQuery);
    }

    /**
     * 修改对话历史记录详情反馈
     */
    @Log(title = "AI对话反馈", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AiChatHistoryDetailFeedbacksBo bo) {
        bo.setUserId(LoginHelper.getUserId());

        return toAjax(aiChatHistoryDetailFeedbacksService.updateByBo(bo));
    }

    /**
     * 删除对话历史记录详情反馈
     *
     * @param ids 主键串
     */
    @Log(title = "AI对话反馈", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(aiChatHistoryDetailFeedbacksService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 处理消息反馈
     *
     * @return
     */
    @Log(title = "AI对话反馈处理", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("handle")
    public R<Void> handle(@RequestBody FeedBacksBo bo) {
        aiChatHistoryDetailFeedbacksService.handle(bo);
        return R.ok();
    }

}
