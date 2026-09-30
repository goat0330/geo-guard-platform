package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListAddBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListAddVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListAddService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 任务上报信息（dz_task_dist_list_add）
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskDistListAdd")
public class DzTaskDistListAddController extends BaseController {

    private final IDzTaskDistListAddService dzTaskDistListAddService;

    /**
     * 分页查询任务上报信息列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskDistListAddVo> list(DzTaskDistListAddBo bo, PageQuery pageQuery) {
        return dzTaskDistListAddService.queryPageList(bo, pageQuery);
    }

    /**
     * 按任务 id 与用户 id 获取详情
     */
    @GetMapping("/detail")
    public R<DzTaskDistListAddVo> getInfo(@NotNull(message = "任务id不能为空") @RequestParam Long taskId,
                                          @NotNull(message = "用户id不能为空") @RequestParam Long userId) {
        return R.ok(dzTaskDistListAddService.queryByKeys(taskId, userId));
    }

    /**
     * 新增任务上报信息
     */
    @Log(title = "任务上报信息", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskDistListAddBo bo) {
        return toAjax(dzTaskDistListAddService.insertByBo(bo));
    }

    /**
     * 修改任务上报信息（联合主键 taskId + userId）
     */
    @Log(title = "任务上报信息", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzTaskDistListAddBo bo) {
        return toAjax(dzTaskDistListAddService.updateByBo(bo));
    }

    /**
     * 删除任务上报信息
     */
    @Log(title = "任务上报信息", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping()
    public R<Void> remove(@NotNull(message = "任务id不能为空") @RequestParam Long taskId,
                          @NotNull(message = "用户id不能为空") @RequestParam Long userId) {
        return toAjax(dzTaskDistListAddService.deleteWithValidByKeys(taskId, userId, true));
    }
}
