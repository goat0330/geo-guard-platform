/* @author kongweiguang */
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
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStepsBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskAssessmentStepsVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentStepsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 斜坡单元风险评估步骤
 *
 * @author kongweiguang
 * @date 2026-01-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/riskAssessmentSteps")
public class DzRiskAssessmentStepsController extends BaseController {

    private final IDzRiskAssessmentStepsService dzRiskAssessmentStepsService;

    /**
     * 查询地质灾害预测与易发性评价数据列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzRiskAssessmentStepsVo> list(DzRiskAssessmentStepsBo bo, PageQuery pageQuery) {
        return dzRiskAssessmentStepsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出地质灾害预测与易发性评价数据列表
     */
    @Log(title = "斜坡单元风险评估步骤", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzRiskAssessmentStepsBo bo, HttpServletResponse response) {
        List<DzRiskAssessmentStepsVo> list = dzRiskAssessmentStepsService.queryList(bo);
        ExcelUtil.exportExcel(list, "地质灾害预测与易发性评价数据", DzRiskAssessmentStepsVo.class, response);
    }

    /**
     * 获取地质灾害预测与易发性评价数据详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzRiskAssessmentStepsVo> getInfo(@NotNull(message = "主键不能为空")
                                              @PathVariable Long id) {
        return R.ok(dzRiskAssessmentStepsService.queryById(id));
    }

    /**
     * 新增地质灾害预测与易发性评价数据
     */
    @Log(title = "斜坡单元风险评估步骤", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzRiskAssessmentStepsBo bo) {
        return toAjax(dzRiskAssessmentStepsService.insertByBo(bo));
    }

    /**
     * 修改地质灾害预测与易发性评价数据
     */
    @Log(title = "斜坡单元风险评估步骤", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzRiskAssessmentStepsBo bo) {
        return toAjax(dzRiskAssessmentStepsService.updateByBo(bo));
    }

    /**
     * 删除地质灾害预测与易发性评价数据
     *
     * @param ids 主键串
     */
    @Log(title = "斜坡单元风险评估步骤", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzRiskAssessmentStepsService.deleteWithValidByIds(List.of(ids), true));
    }
}
