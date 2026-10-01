/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallHourlySeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticResultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RainfallLogSlopeUnitStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitForecastRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallSeriesVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitDailyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitTodayHourlyRainfallVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.TodayRainfallVo;

import java.util.List;

/**
 * 斜坡单元实况雨量统计 Service。
 * <p>
 * 提供按斜坡单元和按行政区划维度的雨量统计查询能力。
 *
 * @author system
 * @date 2026-03-31
 */
public interface IRainfallLogSlopeUnitStatisticService {

    /**
     * 根据起止日期和斜坡单元 ID 查询按日统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 分页后的统计结果
     */
    RainfallLogSlopeUnitStatisticResultVo queryPageListBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo);

    /**
     * 根据起止日期及行政区划名称、行政区划编码查询按日统计的雨量信息。
     *
     * @param bo 查询条件
     * @return 分页后的统计结果
     */
    RainfallLogSlopeUnitStatisticResultVo queryPageListByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo);

    /**
     * 查询全部斜坡单元近7天按日统计的实况雨量数据。
     *
     * @param bo 查询条件
     * @return 统计结果
     */
    List<SlopeUnitDailyRainfallSeriesVo> queryDailyListByAllSlopeUnits(RainfallLogSlopeUnitStatisticQueryBo bo);

    /**
     * 查询当日按街道/乡镇统计的雨量数据。
     *
     * @return 按街道/乡镇统计的今日雨量列表
     */
    List<TodayRainfallVo> queryTodayRainfall();

    /**
     * 根据斜坡单元ID查询当天24小时雨量序列。
     * <p>
     * 同一小时优先返回历史雨量；当历史缺失时，回退预测雨量。
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 当天24小时雨量序列
     */
    SlopeUnitTodayHourlyRainfallVo queryTodayHourlyRainfallBySlopeUnit(String slopeUnitId);

    /**
     * 查询斜坡单元固定分期的实况、预报雨量序列。
     *
     * @param slopeUnitId 斜坡单元ID，为空时查询所有斜坡单元
     * @return 分期雨量序列
     */
    List<SlopeUnitForecastRainfallSeriesVo> queryForecastRainfallBySlopeUnit(String slopeUnitId);

    /**
     * 根据斜坡单元ID和日期查询当天预报累计雨量。
     *
     * @param slopeUnitId 斜坡单元ID
     * @param statDate    统计日期
     * @return 当日预报累计雨量
     */
    Double queryForecastDailyRainfallBySlopeUnit(String slopeUnitId, java.time.LocalDate statDate);

    /**
     * 根据时间范围和斜坡单元ID查询按小时雨量序列。
     *
     * @param bo 查询条件
     * @return 按小时雨量序列
     */
    RainfallHourlySeriesVo queryHourlyRainfallBySlopeUnit(RainfallLogSlopeUnitStatisticQueryBo bo);

    /**
     * 根据时间范围及行政区划名称、行政区划编码查询按小时雨量序列。
     *
     * @param bo 查询条件
     * @return 按小时雨量序列
     */
    RainfallHourlySeriesVo queryHourlyRainfallByAdRegion(RainfallLogSlopeUnitStatisticQueryBo bo);
}
