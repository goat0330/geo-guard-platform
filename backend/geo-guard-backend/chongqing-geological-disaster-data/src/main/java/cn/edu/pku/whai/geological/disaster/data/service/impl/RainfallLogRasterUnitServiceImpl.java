/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallLogRasterUnit;
import cn.edu.pku.whai.geological.disaster.data.mapper.RainfallCellMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IRainfallLogRasterUnitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 降雨实况栅格单元 Service 实现
 *
 * @author system
 * @date 2026-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RainfallLogRasterUnitServiceImpl implements IRainfallLogRasterUnitService {

    private final RainfallCellMapper baseMapper;

    @Override
    public void saveBatch(List<RainfallLogRasterUnit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        try {
            baseMapper.insertBatch(units);
            log.info("批量保存降雨实况栅格单元 {} 条", units.size());
        } catch (Exception e) {
            log.error("批量保存降雨实况栅格单元失败，数据量: {}", units.size(), e);
            throw e;
        }
    }

    @Override
    public List<RainfallLogRasterUnit> listByLogTimeRange(Date startTime, Date endTime) {
        LambdaQueryWrapper<RainfallLogRasterUnit> lqw = Wrappers.lambdaQuery();
        lqw.ge(startTime != null, RainfallLogRasterUnit::getLogTime, startTime)
            .lt(endTime != null, RainfallLogRasterUnit::getLogTime, endTime)
            .orderByAsc(RainfallLogRasterUnit::getLogTime, RainfallLogRasterUnit::getLon, RainfallLogRasterUnit::getLat);
        return baseMapper.selectList(lqw);
    }
}
