/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallForecastRasterUnit;

import java.util.Date;
import java.util.List;

/**
 * 降雨预报栅格单元 Service 接口
 * <p>
 * 主要提供预报栅格点结果的批量入库能力，供 service 模块调用。
 *
 * @author system
 * @date 2026-01-27
 */
public interface IRainfallForecastRasterUnitService {

    /**
     * 批量保存降雨预报栅格单元
     *
     * @param units 预报栅格单元列表
     */
    void saveBatch(List<RainfallForecastRasterUnit> units);

    /**
     * 根据ID列表批量删除降雨预报栅格单元
     *
     * @param ids ID列表
     */
    void deleteBatchByIds(List<Long> ids);

    /**
     * 按预报时间区间删除降雨预报栅格单元。
     *
     * @param startForecastTime 起始预报时间
     * @param endForecastTime   结束预报时间
     */
    void deleteByForecastTimeRange(Date startForecastTime, Date endForecastTime);

    /**
     * 按预报时间区间替换降雨预报栅格单元。
     *
     * @param units             新的预报栅格单元
     * @param startForecastTime 起始预报时间
     * @param endForecastTime   结束预报时间
     */
    void replaceByForecastTimeRange(List<RainfallForecastRasterUnit> units, Date startForecastTime, Date endForecastTime);
}
