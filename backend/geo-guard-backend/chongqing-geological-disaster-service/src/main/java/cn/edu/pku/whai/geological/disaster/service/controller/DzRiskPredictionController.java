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
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionChartBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionChartVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskPredictionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险预测
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/riskPrediction")
public class DzRiskPredictionController extends BaseController {

    private final IDzRiskPredictionService dzRiskPredictionService;

    /**
     * 查询风险预测列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzRiskPredictionVo> list(DzRiskPredictionBo bo, PageQuery pageQuery) {
        return dzRiskPredictionService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出风险预测列表
     */
    @Log(title = "风险预测", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzRiskPredictionBo bo, HttpServletResponse response) {
        List<DzRiskPredictionVo> list = dzRiskPredictionService.queryList(bo);
        ExcelUtil.exportExcel(list, "风险预测", DzRiskPredictionVo.class, response);
    }

    /**
     * 获取风险预测详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzRiskPredictionVo> getInfo(@NotNull(message = "主键不能为空")
                                         @PathVariable Long id) {
        return R.ok(dzRiskPredictionService.queryById(id));
    }

    /**
     * 根据批次ID获取风险预测列表
     *
     * @param batchId 批次ID
     */
    @PostMapping("/batch/{batchId}")
    public R<List<DzRiskPredictionVo>> queryByBatchId(@NotNull(message = "batchId不能为空")
                                                      @PathVariable Long batchId) {
        return R.ok(dzRiskPredictionService.queryByBatchId(batchId));
    }

    /**
     * 新增风险预测
     */
    @Log(title = "风险预测", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzRiskPredictionBo bo) {
        return toAjax(dzRiskPredictionService.insertByBo(bo));
    }

    /**
     * 修改风险预测
     */
    @Log(title = "风险预测", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzRiskPredictionBo bo) {
        return toAjax(dzRiskPredictionService.updateByBo(bo));
    }

    /**
     * 删除风险预测
     *
     * @param ids 主键串
     */
    @Log(title = "风险预测", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzRiskPredictionService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 统计风险预测数据
     */
    @PostMapping("/stat")
    public R<DzRiskPredictionStatVo> stat(@RequestBody DzRiskPredictionStatBo bo) {
        DzRiskPredictionStatVo fr = dzRiskPredictionService.stat(bo);
        return R.ok(fr);
    }

    /**
     * 统计风险预测banner
     */
    @PostMapping("/statBanner")
    public R<DzRiskPredictionChartVo> statChart(@RequestBody DzRiskPredictionChartBo bo) {
        return R.ok(dzRiskPredictionService.statChart(bo));
    }


}
