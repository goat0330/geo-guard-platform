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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorWarningInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorWarningInfoVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISensorWarningInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 传感器预警
 **/
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/sensorWarningInfo")
public class SensorWarningInfoController extends BaseController {

    private final ISensorWarningInfoService sensorWarningInfoService;

    /**
     * 查询传感器预警列表
     */
    @GetMapping("/list")
    public TableDataInfo<SensorWarningInfoVo> list(SensorWarningInfoBo bo, PageQuery pageQuery) {
        return sensorWarningInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出传感器预警列表
     */
    @Log(title = "传感器预警", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(SensorWarningInfoBo bo, HttpServletResponse response) {
        List<SensorWarningInfoVo> list = sensorWarningInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "传感器预警", SensorWarningInfoVo.class, response);
    }

    /**
     * 获取传感器预警详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SensorWarningInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                          @PathVariable String id) {
        return R.ok(sensorWarningInfoService.queryById(id));
    }
}
