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
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterFeedbackBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReportDisasterStatVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzReportDisasterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报灾管理
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/reportDisaster")
public class DzReportDisasterController extends BaseController {

    private final IDzReportDisasterService dzReportDisasterService;

    /**
     * 查询报灾管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzReportDisasterVo> list(DzReportDisasterBo bo, PageQuery pageQuery) {
        return dzReportDisasterService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出报灾管理列表
     */
    @Log(title = "报灾管理", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzReportDisasterBo bo, HttpServletResponse response) {
        List<DzReportDisasterVo> list = dzReportDisasterService.queryList(bo);
        ExcelUtil.exportExcel(list, "报灾管理", DzReportDisasterVo.class, response);
    }

    /**
     * 获取报灾管理详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzReportDisasterVo> getInfo(@NotNull(message = "主键不能为空")
                                         @PathVariable Long id) {
        return R.ok(dzReportDisasterService.queryById(id));
    }

    /**
     * 新增报灾管理
     */
    @Log(title = "报灾管理", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzReportDisasterBo bo) {
        return toAjax(dzReportDisasterService.insertByBo(bo));
    }

    /**
     * 修改报灾管理
     */
    @Log(title = "报灾管理", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzReportDisasterBo bo) {
        return toAjax(dzReportDisasterService.updateByBo(bo));
    }

    /**
     * 删除报灾管理
     *
     * @param ids 主键串
     */
    @Log(title = "报灾管理", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzReportDisasterService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 处理当前报灾
     */
    @Log(title = "报灾处理", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/handle/{id}")
    public R<DzReportDisasterHandleVo> handle(@PathVariable Long id) {
        return R.ok(dzReportDisasterService.handle(id));
    }

    /**
     * 统计当前报灾
     */
    @PostMapping("/stat")
    public R<ReportDisasterStatVo> stat() {
        ReportDisasterStatVo vo = dzReportDisasterService.stat();
        return R.ok(vo);
    }

    /**
     * 关闭报灾
     *
     * @param id 主键
     */
    @Log(title = "报灾关闭", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/closeReport/{id}")
    public R<Void> closeReport(@PathVariable Long id) {
        dzReportDisasterService.closeReport(id);
        return R.ok();
    }

    /**
     * 获取当前所有正在处理的报灾
     */
    @GetMapping("/getAllHandling")
    public R<List<DzReportDisasterVo>> getAllHandling() {
        return R.ok(dzReportDisasterService.getAllHandling());
    }

    /**
     * 添加报灾人工反馈
     */
    @Log(title = "报灾反馈", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping("/feedback")
    public R<Void> addFeedback(@Validated @RequestBody DzReportDisasterFeedbackBo bo) {
        dzReportDisasterService.addFeedback(bo);
        return R.ok();
    }
}
