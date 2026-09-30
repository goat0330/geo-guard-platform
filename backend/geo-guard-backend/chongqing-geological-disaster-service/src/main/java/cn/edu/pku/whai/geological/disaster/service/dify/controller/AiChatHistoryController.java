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
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryVo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 对话历史
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/ai/agent/chatHistory")
public class AiChatHistoryController extends BaseController {

    private final IAiChatHistoryService aiChatHistoryService;

    /**
     * 查询对话历史列表
     */
    @GetMapping("/list")
    public TableDataInfo<AiChatHistoryVo> list(AiChatHistoryBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        return aiChatHistoryService.queryPageList(bo, pageQuery);
    }


    /**
     * 修改对话历史
     */
    @Log(title = "AI对话历史", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AiChatHistoryBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        return toAjax(aiChatHistoryService.updateByBo(bo));
    }

    /**
     * 删除对话历史
     *
     * @param ids 主键串
     */
    @Log(title = "AI对话历史", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(aiChatHistoryService.deleteWithValidByIds(List.of(ids), true));
    }

}
