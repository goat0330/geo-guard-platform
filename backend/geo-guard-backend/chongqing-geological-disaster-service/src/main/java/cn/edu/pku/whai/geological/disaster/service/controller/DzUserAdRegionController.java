/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户行政区划关联
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/userAdRegion")
public class DzUserAdRegionController extends BaseController {

    private final IDzUserAdRegionService dzUserAdRegionService;

    /**
     * 查询用户行政区划关联列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzUserAdRegionVo> list(DzUserAdRegionBo bo, PageQuery pageQuery) {
        return dzUserAdRegionService.queryPageList(bo, pageQuery);
    }

    /**
     * 分页统计用户区域分布
     */
    @GetMapping("/statRegion")
    public TableDataInfo<DzUserAdRegionStatVo> statRegion(DzUserAdRegionBo bo, PageQuery pageQuery) {
        return dzUserAdRegionService.statRegionPage(bo, pageQuery);
    }

    /**
     * 导出用户行政区划关联列表
     */
    @Log(title = "用户行政区划关联", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzUserAdRegionBo bo, HttpServletResponse response) {
        List<DzUserAdRegionVo> list = dzUserAdRegionService.queryList(bo);
        ExcelUtil.exportExcel(list, "用户行政区划关联", DzUserAdRegionVo.class, response);
    }

    /**
     * 获取用户行政区划关联详细信息
     *
     * @param userId 用户ID
     */
    @GetMapping("/{userId}")
    public R<List<DzUserAdRegionVo>> getInfo(@NotNull(message = "用户ID不能为空")
                                             @PathVariable Long userId) {
        return R.ok(dzUserAdRegionService.queryListByUserId(userId));
    }

    /**
     * 新增用户行政区划关联
     */
    @Log(title = "用户行政区划关联", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzUserAdRegionBo bo) {
        return toAjax(dzUserAdRegionService.insertByBo(bo));
    }

    /**
     * 修改用户行政区划关联
     */
    @Log(title = "用户行政区划关联", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@RequestBody DzUserAdRegionBo bo) {
        return toAjax(dzUserAdRegionService.updateByBo(bo));
    }

    /**
     * 删除用户行政区划关联
     *
     * @param ids 用户ID串
     */
    @Log(title = "用户行政区划关联", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "用户ID不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzUserAdRegionService.deleteWithValidByIds(List.of(ids), true));
    }
}
