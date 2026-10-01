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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.WarningDisposalRecordMonitorBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.WarningDisposalRecordMonitorVo;
import cn.edu.pku.whai.geological.disaster.data.service.IWarningDisposalRecordMonitorService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 预警处置记录监测
 *
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/warningDisposalRecordMonitor")
public class WarningDisposalRecordMonitorController extends BaseController {

    private final IWarningDisposalRecordMonitorService warningDisposalRecordMonitorService;

    /**
     * 查询预警处置记录监测列表
     */
    @GetMapping("/list")
    public TableDataInfo<WarningDisposalRecordMonitorVo> list(WarningDisposalRecordMonitorBo bo, PageQuery pageQuery) {
        return warningDisposalRecordMonitorService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出预警处置记录监测列表
     */
    @Log(title = "预警处置记录监测", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(WarningDisposalRecordMonitorBo bo, HttpServletResponse response) {
        List<WarningDisposalRecordMonitorVo> list = warningDisposalRecordMonitorService.queryList(bo);
        ExcelUtil.exportExcel(list, "预警处置记录监测", WarningDisposalRecordMonitorVo.class, response);
    }

    /**
     * 获取预警处置记录监测详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<WarningDisposalRecordMonitorVo> getInfo(@NotNull(message = "主键不能为空")
                                                     @PathVariable String id) {
        return R.ok(warningDisposalRecordMonitorService.queryById(id));
    }
}
