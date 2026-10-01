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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DisasterPreventionPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DisasterPreventionPlanVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDisasterPreventionPlanService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 防灾预案
 **/
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/disasterPreventionPlan")
public class DisasterPreventionPlanController extends BaseController {

    private final IDisasterPreventionPlanService disasterPreventionPlanService;

    /**
     * 查询防灾预案列表
     */
    @GetMapping("/list")
    public TableDataInfo<DisasterPreventionPlanVo> list(DisasterPreventionPlanBo bo, PageQuery pageQuery) {
        return disasterPreventionPlanService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出防灾预案列表
     */
    @Log(title = "防灾预案", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DisasterPreventionPlanBo bo, HttpServletResponse response) {
        List<DisasterPreventionPlanVo> list = disasterPreventionPlanService.queryList(bo);
        ExcelUtil.exportExcel(list, "防灾预案", DisasterPreventionPlanVo.class, response);
    }

    /**
     * 获取防灾预案详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DisasterPreventionPlanVo> getInfo(@NotNull(message = "主键不能为空")
                                               @PathVariable String id) {
        return R.ok(disasterPreventionPlanService.queryById(id));
    }
}
