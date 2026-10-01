/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.*;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.data.service.*;
import cn.hutool.core.bean.BeanUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 第三方风险预警预警接口
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/warningData")
public class ThirdPartyWarningDataController extends BaseController {

    private final IThirdPartyWeatherWarningService weatherWarningService;
    private final IThirdPartyMonitorWarningService monitorWarningService;
    private final IThirdPartyMonitorDeviceService monitorDeviceService;
    private final IThirdPartyDeviceCurveQueryService deviceCurveQueryService;
    private final IThirdPartyWarningDataService thirdPartyWarningDataService;
    private final IHouseSlopeService houseSlopeService;


    /**
     * 获取对应时间段的已发布的气象预警数据列表
     */
    @GetMapping("/getWeatherWarning")
    public TableDataInfo<RiskWarningRecordVo> getWeatherWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return weatherWarningService.getWeatherWarning(bo, pageQuery);
    }

    /**
     * 根据预警数据id获取气象预警数据详情
     *
     * @param id 主键
     */
    @GetMapping("/weatherWarningDetail/{id}")
    public R<RiskWarningRecordVo> weatherDetail(@NotNull(message = "主键不能为空")
                                                @PathVariable String id) {
        return R.ok(BeanUtil.copyProperties(weatherWarningService.weatherWarningDetail(id), RiskWarningRecordVo.class));
    }

    /**
     * 获取对应时间段的已发布的监测设备预警数据列表
     */
    @GetMapping("/getMonitorWarning")
    public TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return monitorWarningService.getMonitorWarning(bo, pageQuery);
    }

    /**
     * 获取三方预警处置数据列表
     */
    @GetMapping("/getMonitorWarningDisposal")
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return monitorWarningService.getMonitorWarningDisposal(bo, pageQuery);
    }

    /**
     * 查询三方设备监测曲线
     */
    @PostMapping("/queryDeviceCure")
    public R<ThirdPartyDeviceCureVo> queryDeviceCure(@RequestBody ThirdPartyDeviceCureBo bo) {
        return R.ok(deviceCurveQueryService.queryDeviceCure(bo));
    }

    /**
     * 直接调用三方设备监测曲线接口并返回原始响应
     */
    @PostMapping("/queryDeviceCureDirect")
    public R<ThirdPartyWarningDataResp> queryDeviceCureDirect(
        @RequestBody ThirdPartyDeviceCureDirectBo bo) {
        return R.ok(deviceCurveQueryService.queryDeviceCureDirect(bo));
    }

    /**
     * 获取三方监测设备分页列表
     */
    @GetMapping("/getMonitorDevicePage")
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        return monitorDeviceService.getMonitorDevicePage(bo, pageQuery);
    }

    /**
     * 获取三方监测设备树
     */
    @GetMapping("/getMonitorDeviceTree")
    public R<List<ThirdPartyMonitorDeviceTreeVo>> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo) {
        return R.ok(monitorDeviceService.getMonitorDeviceTree(bo));
    }

    /**
     * 全量监测设备，附加最近预警等级与三方设备状态
     */
    @GetMapping("/listAllMonitorDevicesRuntime")
    public R<List<MonitorDeviceRuntimeVo>> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId) {
        return R.ok(monitorDeviceService.listAllMonitorDevicesRuntime(regionCode, slopeUnitId));
    }

    /**
     * 通过行政区划、斜坡单元查询房屋 geometry。
     */
    @GetMapping("/listHouseGeometry")
    public R<List<HouseSlopeGeometryVo>> listHouseGeometry(String adRegionId, String slopeUnitId) {
        return R.ok(houseSlopeService.queryGeometryList(adRegionId, slopeUnitId));
    }

    /**
     * 统计对应时间段内已发布的监测设备预警数据，保持监测预警页面使用的聚合口径。
     */
    @PostMapping("/statMonitorWarning")
    public R<StatThirdWarningVo> statMonitorWarning(@RequestBody StatThirdWarningBo bo) {
        StatThirdWarningVo statThirdWarningVo = monitorWarningService.statMonitorWarning(bo);
        return R.ok(statThirdWarningVo);
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/getLiveRain")
    public R<List<LiveRainVo>> getLiveRain(String date) {
        List<LiveRainVo> liveRainVos = weatherWarningService.getLiveRain(date);
        return R.ok(liveRainVos);
    }

    /**
     * 根据设备ID获取雨量与位移统计
     */
    @GetMapping("/getDeviceRainfallAndDisplacement")
    public R<DeviceRainfallDisplacementVo> getDeviceRainfallAndDisplacement(@NotNull(message = "设备id不能为空") String deviceId) {
        return R.ok(monitorDeviceService.getDeviceRainfallAndDisplacement(deviceId));
    }

    /**
     * 检查三方接口连通性
     */
    @GetMapping("/checkThirdPartyConnectivity")
    public R<java.util.Map<String, Object>> checkThirdPartyConnectivity() {
        return R.ok(thirdPartyWarningDataService.checkThirdPartyConnectivity());
    }


}
