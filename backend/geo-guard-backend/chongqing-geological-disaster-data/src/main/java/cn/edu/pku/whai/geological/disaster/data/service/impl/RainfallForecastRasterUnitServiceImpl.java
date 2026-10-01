/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallForecastRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallForecastRasterUnitMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallForecastRasterUnitService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 降雨预报栅格单元 Service 实现
 *
 * @author system
 * @date 2026-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RainfallForecastRasterUnitServiceImpl implements IRainfallForecastRasterUnitService {

    private final RainfallForecastRasterUnitMapper baseMapper;

    @Override
    public void saveBatch(List<RainfallForecastRasterUnit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        try {
            // 使用批量插入，提高性能
            baseMapper.insertBatch(units);
            log.info("批量保存降雨预报栅格单元 {} 条", units.size());
        } catch (Exception e) {
            log.error("批量保存降雨预报栅格单元失败，数据量: {}", units.size(), e);
            throw e;
        }
    }

    @Override
    public void deleteBatchByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int deletedCount = baseMapper.deleteByIds(ids);
        log.info("批量删除降雨预报栅格单元 {} 条，实际删除 {} 条", ids.size(), deletedCount);
    }

    @Override
    public void deleteByForecastTimeRange(Date startForecastTime, Date endForecastTime) {
        if (startForecastTime == null || endForecastTime == null) {
            return;
        }
        int deletedCount = baseMapper.delete(Wrappers.<RainfallForecastRasterUnit>lambdaQuery()
                                                     .between(RainfallForecastRasterUnit::getForecastTime,
                                                         startForecastTime,
                                                         endForecastTime));
        log.info("按预报时间区间删除降雨预报栅格单元，区间: [{} - {}]，实际删除 {} 条",
            startForecastTime, endForecastTime, deletedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceByForecastTimeRange(List<RainfallForecastRasterUnit> units, Date startForecastTime, Date endForecastTime) {
        if (units == null || units.isEmpty()) {
            return;
        }
        deleteByForecastTimeRange(startForecastTime, endForecastTime);
        saveBatch(units);
    }
}
