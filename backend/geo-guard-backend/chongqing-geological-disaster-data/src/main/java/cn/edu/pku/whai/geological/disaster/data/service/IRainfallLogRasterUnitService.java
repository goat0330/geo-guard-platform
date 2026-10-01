/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;

import java.util.Date;
import java.util.List;

/**
 * 降雨实况栅格单元 Service 接口
 * <p>
 * 主要提供实况栅格点结果的批量入库能力，供 service 模块调用。
 *
 * @author system
 * @date 2026-01-27
 */
public interface IRainfallLogRasterUnitService {

    /**
     * 批量保存降雨实况栅格单元
     *
     * @param units 栅格单元列表
     */
    void saveBatch(List<RainfallLogRasterUnit> units);

    /**
     * 按观测时间区间查询降雨实况栅格单元。
     *
     * @param startTime 开始时间（含）
     * @param endTime 结束时间（不含）
     * @return 栅格单元列表
     */
    List<RainfallLogRasterUnit> listByLogTimeRange(Date startTime, Date endTime);
}
