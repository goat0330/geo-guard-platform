/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitGridMemberRelationBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
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
 * 斜坡单元网格员信息关联
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/slopeUnitGridMemberRelation")
public class SlopeUnitGridMemberRelationController extends BaseController {

    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;

    /**
     * 查询斜坡单元网格员信息关联列表
     */
    @GetMapping("/list")
    public TableDataInfo<SlopeUnitGridMemberRelationVo> list(SlopeUnitGridMemberRelationBo bo, PageQuery pageQuery) {
        return slopeUnitGridMemberRelationService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出斜坡单元网格员信息关联列表
     */
    @Log(title = "斜坡单元网格员信息关联", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(SlopeUnitGridMemberRelationBo bo, HttpServletResponse response) {
        List<SlopeUnitGridMemberRelationVo> list = slopeUnitGridMemberRelationService.queryList(bo);
        ExcelUtil.exportExcel(list, "斜坡单元网格员信息关联", SlopeUnitGridMemberRelationVo.class, response);
    }

    /**
     * 获取斜坡单元网格员信息关联详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SlopeUnitGridMemberRelationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(slopeUnitGridMemberRelationService.queryById(id));
    }

    /**
     * 新增斜坡单元网格员信息关联
     */
    @Log(title = "斜坡单元网格员信息关联", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SlopeUnitGridMemberRelationBo bo) {
        return toAjax(slopeUnitGridMemberRelationService.insertByBo(bo));
    }

    /**
     * 修改斜坡单元网格员信息关联
     */
    @Log(title = "斜坡单元网格员信息关联", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SlopeUnitGridMemberRelationBo bo) {
        return toAjax(slopeUnitGridMemberRelationService.updateByBo(bo));
    }

    /**
     * 删除斜坡单元网格员信息关联
     *
     * @param ids 主键串
     */
    @Log(title = "斜坡单元网格员信息关联", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(slopeUnitGridMemberRelationService.deleteWithValidByIds(List.of(ids), true));
    }
}
