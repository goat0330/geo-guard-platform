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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.GridMemberBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GridMemberVo;
import cn.edu.pku.whai.geological.disaster.data.service.IGridMemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 网格人员信息
 *
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/gridMember")
public class GridMemberController extends BaseController {

    private final IGridMemberService gridMemberService;

    /**
     * 查询网格人员信息列表
     */
    @GetMapping("/list")
    public TableDataInfo<GridMemberVo> list(GridMemberBo bo, PageQuery pageQuery) {
        return gridMemberService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出网格人员信息列表
     */
    @Log(title = "网格人员信息", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(GridMemberBo bo, HttpServletResponse response) {
        List<GridMemberVo> list = gridMemberService.queryList(bo);
        ExcelUtil.exportExcel(list, "网格人员信息", GridMemberVo.class, response);
    }

    /**
     * 获取网格人员信息详细信息
     *
     * @param userId 主键
     */
    @GetMapping("/{userId}")
    public R<GridMemberVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable String userId) {
        return R.ok(gridMemberService.queryById(userId));
    }
}
