/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallHourlySeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticResultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitTodayHourlyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.TodayRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallLogSlopeUnitStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 斜坡单元实况雨量统计查询接口。
 * <p>
 * 为减少对既有控制器的影响，这里单独提供统计查询入口，
 * 不将查询逻辑混入原有斜坡单元控制器。
 *
 * @author system
 * @date 2026-04-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/rainfallLogSlopeUnitStatistic")
public class RainfallLogSlopeUnitStatisticController extends BaseController {

    private final IRainfallLogSlopeUnitStatisticService rainfallLogSlopeUnitStatisticService;

    /**
     * 根据时间范围和斜坡单元 ID 查询按日统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 分页结果
     */
    @GetMapping("/listBySlopeUnit")
    public RainfallLogSlopeUnitStatisticResultVo listBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return rainfallLogSlopeUnitStatisticService.queryPageListBySlopeUnit(bo);
    }

    /**
     * 根据时间范围、行政区划名称、行政区划编码查询按日统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 分页结果
     */
    @GetMapping("/listByAdRegion")
    public RainfallLogSlopeUnitStatisticResultVo listByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return rainfallLogSlopeUnitStatisticService.queryPageListByAdRegion(bo);
    }

    /**
     * 查询所有斜坡单元近7天按日统计的实况雨量信息。
     *
     * @param bo 查询条件
     * @return 实况雨量列表
     */
    @SaIgnore
    @GetMapping("/dailyListByAllSlopeUnits")
    public R<List<SlopeUnitDailyRainfallSeriesVo>> dailyListByAllSlopeUnits(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryDailyListByAllSlopeUnits(bo));
    }

    /**
     * 查询当日按街道/乡镇统计的雨量数据。
     *
     * @return 按街道/乡镇统计的今日雨量列表
     */
    @GetMapping("/listTodayRainfall")
    public R<List<TodayRainfallVo>> listTodayRainfall() {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryTodayRainfall());
    }

    /**
     * 根据斜坡单元ID查询当天24小时雨量序列。
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 当天24小时雨量
     */
    @GetMapping("/todayHourlyBySlopeUnit")
    public R<SlopeUnitTodayHourlyRainfallVo> todayHourlyBySlopeUnit(String slopeUnitId) {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryTodayHourlyRainfallBySlopeUnit(slopeUnitId));
    }

    /**
     * 根据时间范围和斜坡单元ID查询按小时统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 小时雨量序列
     */
    @GetMapping("/hourlyBySlopeUnit")
    public R<RainfallHourlySeriesVo> hourlyBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryHourlyRainfallBySlopeUnit(bo));
    }

    /**
     * 查询斜坡单元固定分期的实况、预报雨量序列。
     *
     * @param slopeUnitId 斜坡单元ID，为空时查询所有斜坡单元
     * @return 分期雨量序列
     */
    @SaIgnore
    @GetMapping("/forecastBySlopeUnit")
    public R<List<SlopeUnitForecastRainfallSeriesVo>> forecastBySlopeUnit(
        @RequestParam(required = false) String slopeUnitId) {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryForecastRainfallBySlopeUnit(slopeUnitId));
    }

    /**
     * 根据时间范围、行政区划名称、行政区划编码查询按小时统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 小时雨量序列
     */
    @GetMapping("/hourlyByAdRegion")
    public R<RainfallHourlySeriesVo> hourlyByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo) {
        return R.ok(rainfallLogSlopeUnitStatisticService.queryHourlyRainfallByAdRegion(bo));
    }
}
