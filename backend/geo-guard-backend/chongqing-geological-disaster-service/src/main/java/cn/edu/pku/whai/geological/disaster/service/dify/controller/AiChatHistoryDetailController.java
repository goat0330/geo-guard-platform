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
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailVo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 对话历史记录详情
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/ai/agent/chatHistoryDetail")
public class AiChatHistoryDetailController extends BaseController {

    private final IAiChatHistoryDetailService aiChatHistoryDetailService;

    /**
     * 查询对话历史记录详情列表
     */
    @GetMapping("/list")
    public TableDataInfo<AiChatHistoryDetailVo> list(AiChatHistoryDetailBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        return aiChatHistoryDetailService.queryPageList(bo, pageQuery);
    }

    /**
     * 修改对话历史记录详情
     */
    @Log(title = "AI对话历史详情", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AiChatHistoryDetailBo bo) {
        return toAjax(aiChatHistoryDetailService.updateByBo(bo));
    }

    /**
     * 删除对话历史记录详情
     *
     * @param ids 主键串
     */
    @Log(title = "AI对话历史详情", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(aiChatHistoryDetailService.deleteWithValidByIds(List.of(ids), true));
    }
}
