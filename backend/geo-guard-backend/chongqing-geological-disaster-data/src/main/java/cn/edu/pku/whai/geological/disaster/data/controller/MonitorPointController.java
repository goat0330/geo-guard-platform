/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.AreaWktReq;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorPointNamesReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorPointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 监测点基本情况
 *
 * @author lizheng
 * @date 2026-01-06
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/monitorPoint")
public class MonitorPointController extends BaseController {

    private final IMonitorPointService dataMonitorPointService;

    /**
     * 查询监测点基本情况列表
     */
    @GetMapping("/list")
    public TableDataInfo<MonitorPointVo> list(MonitorPointBo bo, PageQuery pageQuery) {
        return dataMonitorPointService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取监测点基本情况详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<MonitorPointVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable String id) {
        return R.ok(dataMonitorPointService.queryById(id));
    }

    /**
     * 根据范围WKT查询监测点列表
     */
    @PostMapping("/queryByWkt")
    public R<List<MonitorPointVo>> queryByWkt(@Valid @RequestBody AreaWktReq req) {
        return R.ok(dataMonitorPointService.queryByWkt(req.getWkt()));
    }

    /**
     * 根据监测点名称批量查询监测点及设备列表
     */
    @PostMapping("/queryByMonitorNames")
    public R<List<MonitorPointVo>> queryByMonitorNames(@Valid @RequestBody MonitorPointNamesReq req) {
        return R.ok(dataMonitorPointService.queryByMonitorNames(req.getMonitorPointNames()));
    }

}
