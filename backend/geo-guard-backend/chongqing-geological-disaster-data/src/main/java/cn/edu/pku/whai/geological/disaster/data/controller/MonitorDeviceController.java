/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorStatReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.MonitorDeviceTreeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorStatVo;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorDeviceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 监测设备基本情况
 *
 * @author lizheng
 * @date 2026-01-06
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/monitorDevice")
public class MonitorDeviceController extends BaseController {

    private final IMonitorDeviceService dataMonitorDeviceService;

    /**
     * 查询监测设备基本情况列表
     */
    @GetMapping("/list")
    public TableDataInfo<MonitorDeviceVo> list(MonitorDeviceBo bo, PageQuery pageQuery) {
        return dataMonitorDeviceService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取监测设备基本情况详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<MonitorDeviceVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable String id) {
        return R.ok(dataMonitorDeviceService.queryById(id));
    }

    /**
     * 获取监测设备监测数据
     *
     * @param id 主键
     */
    @GetMapping("/getMonitorData/{id}")
    public R<MonitorDeviceTreeResp> getMonitorData(@NotNull(message = "主键不能为空")
                                                    @PathVariable String id) {
        return R.ok(dataMonitorDeviceService.queryMonitorDataById(id));
    }


    @PostMapping("stat")
    public R<MonitorStatVo> stat() {
        MonitorStatVo vo = dataMonitorDeviceService.stat();
        return R.ok(vo);
    }

    /**
     * 按试点区域统计监测点与设备在线情况
     */
    @PostMapping("/statByArea")
    public R<MonitorStatVo> statByArea(@RequestBody(required = false) MonitorStatReq req) {
        return R.ok(dataMonitorDeviceService.statByArea(req));
    }
}
