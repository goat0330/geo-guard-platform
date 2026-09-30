package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListRemarkBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListRemarkVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListRemarkService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务备注
 *
 * @author kongweiguang
 * @date 2026-04-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskDistListRemark")
public class DzTaskDistListRemarkController extends BaseController {

    private final IDzTaskDistListRemarkService dzTaskDistListRemarkService;

    /**
     * 按任务id查询全部备注（创建时间倒序）
     */
    @GetMapping("/listByTaskId")
    public R<List<DzTaskDistListRemarkVo>> listByTaskId(@NotNull(message = "任务id不能为空") @RequestParam Long taskId) {
        return R.ok(dzTaskDistListRemarkService.queryListByTaskId(taskId));
    }

    /**
     * 新增备注
     */
    @Log(title = "任务备注", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskDistListRemarkBo bo) {
        return toAjax(dzTaskDistListRemarkService.insertByBo(bo));
    }

}
