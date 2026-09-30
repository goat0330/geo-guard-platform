/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
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
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionPushVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RiskPredictionPushReportVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskPredictionPushService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地灾风险预测推送
 *
 * @author system
 * @date 2026-03-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/riskPredictionPush")
public class DzRiskPredictionPushController extends BaseController {

    private final IDzRiskPredictionPushService dzRiskPredictionPushService;

    /**
     * 查询地灾风险预测推送列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzRiskPredictionPushVo> list(DzRiskPredictionPushBo bo, PageQuery pageQuery) {
        return dzRiskPredictionPushService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出地灾风险预测推送列表
     */
    @Log(title = "地灾风险预测推送", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzRiskPredictionPushBo bo, HttpServletResponse response) {
        List<DzRiskPredictionPushVo> list = dzRiskPredictionPushService.queryList(bo);
        ExcelUtil.exportExcel(list, "地灾风险预测推送", DzRiskPredictionPushVo.class, response);
    }

    /**
     * 获取地灾风险预测推送详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzRiskPredictionPushVo> getInfo(@NotNull(message = "主键不能为空")
                                             @PathVariable Long id) {
        return R.ok(dzRiskPredictionPushService.queryById(id));
    }

    /**
     * 新增地灾风险预测推送
     */
    @Log(title = "地灾风险预测推送", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Long> add(@Validated(AddGroup.class) @RequestBody DzRiskPredictionPushBo bo) {
        boolean inserted = dzRiskPredictionPushService.insertByBo(bo);
        if (!inserted) {
            throw new ServiceException("新增失败");
        }
        return R.ok(bo.getId());
    }

    /**
     * 修改地灾风险预测推送
     */
    @Log(title = "地灾风险预测推送", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzRiskPredictionPushBo bo) {
        return toAjax(dzRiskPredictionPushService.updateByBo(bo));
    }

    /**
     * 删除地灾风险预测推送
     *
     * @param ids 主键串
     */
    @Log(title = "地灾风险预测推送", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzRiskPredictionPushService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 推送当前报告
     */
    @Log(title = "地灾风险预测推送", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/pushReport/{id}")
    public R<RiskPredictionPushReportVo> pushReport(@PathVariable Long id) {
        RiskPredictionPushReportVo vo = dzRiskPredictionPushService.pushReport(id);
        return R.ok(vo);
    }
}
