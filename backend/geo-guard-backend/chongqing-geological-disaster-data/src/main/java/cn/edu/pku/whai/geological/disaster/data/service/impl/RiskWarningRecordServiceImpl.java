/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskWarningRecord;
import cn.edu.pku.whai.geological.disaster.data.mapper.RiskWarningRecordMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskWarningRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RiskWarningRecordServiceImpl implements IRiskWarningRecordService {
    private final RiskWarningRecordMapper baseMapper;

    @Override
    public Boolean insertBatch(List<RiskWarningRecord> riskWarningRecords) {
        if (riskWarningRecords.isEmpty()) {
            return false;
        }
        return baseMapper.insertBatch(riskWarningRecords);
    }

    @Override
    public Set<String> getExistingIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<RiskWarningRecord> lqw = Wrappers.lambdaQuery();
        lqw.in(RiskWarningRecord::getId, ids);
        lqw.select(RiskWarningRecord::getId);
        List<RiskWarningRecord> list = baseMapper.selectList(lqw);
        return list.stream().map(RiskWarningRecord::getId).collect(Collectors.toSet());
    }

    @Override
    public List<RiskWarningRecord> listByWarningDateBetween(Date startInclusive, Date endInclusive) {
        LambdaQueryWrapper<RiskWarningRecord> lqw = Wrappers.lambdaQuery();
        lqw.ge(startInclusive != null, RiskWarningRecord::getWarningDate, startInclusive);
        lqw.le(endInclusive != null, RiskWarningRecord::getWarningDate, endInclusive);
        lqw.orderByAsc(RiskWarningRecord::getWarningDate);
        return baseMapper.selectList(lqw);
    }

}
