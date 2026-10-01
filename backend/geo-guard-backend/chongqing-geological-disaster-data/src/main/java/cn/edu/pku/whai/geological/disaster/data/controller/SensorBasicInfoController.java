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
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorBasicInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorBasicInfoVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISensorBasicInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 传感器基础信息
 **/
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/sensorBasicInfo")
public class SensorBasicInfoController extends BaseController {

    private final ISensorBasicInfoService sensorBasicInfoService;

    /**
     * 查询传感器基础信息列表
     */
    @GetMapping("/list")
    public TableDataInfo<SensorBasicInfoVo> list(SensorBasicInfoBo bo, PageQuery pageQuery) {
        return sensorBasicInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出传感器基础信息列表
     */
    @Log(title = "传感器基础信息", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(SensorBasicInfoBo bo, HttpServletResponse response) {
        List<SensorBasicInfoVo> list = sensorBasicInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "传感器基础信息", SensorBasicInfoVo.class, response);
    }

    /**
     * 获取传感器基础信息详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SensorBasicInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                        @PathVariable String id) {
        return R.ok(sensorBasicInfoService.queryById(id));
    }
}
