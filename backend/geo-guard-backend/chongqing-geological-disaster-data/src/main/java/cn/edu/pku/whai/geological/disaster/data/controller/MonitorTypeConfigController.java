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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorTypeConfigBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorTypeConfigVo;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorTypeConfigService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 监测类型配置
 **/
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/monitorTypeConfig")
public class MonitorTypeConfigController extends BaseController {

    private final IMonitorTypeConfigService monitorTypeConfigService;

    /**
     * 查询监测类型配置列表
     */
    @GetMapping("/list")
    public TableDataInfo<MonitorTypeConfigVo> list(MonitorTypeConfigBo bo, PageQuery pageQuery) {
        return monitorTypeConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出监测类型配置列表
     */
    @Log(title = "监测类型配置", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(MonitorTypeConfigBo bo, HttpServletResponse response) {
        List<MonitorTypeConfigVo> list = monitorTypeConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "监测类型配置", MonitorTypeConfigVo.class, response);
    }

    /**
     * 获取监测类型配置详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<MonitorTypeConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                          @PathVariable String id) {
        return R.ok(monitorTypeConfigService.queryById(id));
    }
}
