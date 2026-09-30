package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleSceneRecordBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处置管理现场记录
 *
 * @author kongweiguang
 * @date 2026-01-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskHandleSceneRecord")
public class DzTaskHandleSceneRecordController extends BaseController {

    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;

    /**
     * 查询处置管理现场记录列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskHandleSceneRecordVo> list(DzTaskHandleSceneRecordBo bo, PageQuery pageQuery) {
        return dzTaskHandleSceneRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出处置管理现场记录列表
     */
    @Log(title = "处置管理现场记录", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzTaskHandleSceneRecordBo bo, HttpServletResponse response) {
        List<DzTaskHandleSceneRecordVo> list = dzTaskHandleSceneRecordService.queryList(bo);
        ExcelUtil.exportExcel(list, "处置管理现场记录", DzTaskHandleSceneRecordVo.class, response);
    }

    /**
     * 获取处置管理现场记录详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzTaskHandleSceneRecordVo> getInfo(@NotNull(message = "主键不能为空")
                                                @PathVariable Long id) {
        return R.ok(dzTaskHandleSceneRecordService.queryById(id));
    }

    /**
     * 新增处置管理现场记录
     */
    @Log(title = "处置管理现场记录", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskHandleSceneRecordBo bo) {
        return toAjax(dzTaskHandleSceneRecordService.insertByBo(bo));
    }

    /**
     * 修改处置管理现场记录
     */
    @Log(title = "处置管理现场记录", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzTaskHandleSceneRecordBo bo) {
        return toAjax(dzTaskHandleSceneRecordService.updateByBo(bo));
    }

    /**
     * 删除处置管理现场记录
     *
     * @param ids 主键串
     */
    @Log(title = "处置管理现场记录", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzTaskHandleSceneRecordService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 通过handleId获取处置管理现场记录列表
     */
    @GetMapping("/getByHandleId/{handleId}")
    public R<DzTaskHandleSceneRecordVo> getByHandleId(@NotNull(message = "主键不能为空")
                                                              @PathVariable Long handleId) {
        return R.ok(dzTaskHandleSceneRecordService.getByHandleId(handleId));
    }

}
