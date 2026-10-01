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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.OrganizationInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.OrganizationInfoVo;
import cn.edu.pku.whai.geological.disaster.data.service.IOrganizationInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构信息
 **/
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/organizationInfo")
public class OrganizationInfoController extends BaseController {

    private final IOrganizationInfoService organizationInfoService;

    /**
     * 查询组织机构信息列表
     */
    @GetMapping("/list")
    public TableDataInfo<OrganizationInfoVo> list(OrganizationInfoBo bo, PageQuery pageQuery) {
        return organizationInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出组织机构信息列表
     */
    @Log(title = "组织机构信息", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(OrganizationInfoBo bo, HttpServletResponse response) {
        List<OrganizationInfoVo> list = organizationInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "组织机构信息", OrganizationInfoVo.class, response);
    }

    /**
     * 获取组织机构信息详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<OrganizationInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                         @PathVariable String id) {
        return R.ok(organizationInfoService.queryById(id));
    }
}
