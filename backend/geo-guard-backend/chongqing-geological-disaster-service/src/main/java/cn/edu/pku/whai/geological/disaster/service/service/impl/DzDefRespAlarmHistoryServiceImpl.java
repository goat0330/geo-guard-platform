/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.utils.DataAlarmLevelStreetsUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzDefRespAlarmHistoryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespAlarmHistory;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespAlarmHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespAlarmHistoryService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class DzDefRespAlarmHistoryServiceImpl implements IDzDefRespAlarmHistoryService {

    private final DzDefRespAlarmHistoryMapper baseMapper;

    @Override
    public void saveHistory(DataAlarmVo alarm, DefRespPlan defRespPlan) {
        if (alarm == null || alarm.getId() == null || defRespPlan == null || defRespPlan.getId() == null) {
            return;
        }
        Date now = new Date();
        DzDefRespAlarmHistoryBo bo = new DzDefRespAlarmHistoryBo();
        bo.setId(IdUtil.getSnowflakeNextId());
        bo.setAlarmId(alarm.getId());
        bo.setDefId(defRespPlan.getId());
        bo.setAlarmCode(alarm.getCode());
        bo.setTriggerTime(now);
        bo.setDefRespLevel(defRespPlan.getLevel());
        bo.setAlarmLevel(DataAlarmLevelStreetsUtil.resolveHighestLevel(alarm.getLevelStreetsJson()));
        bo.setAlarmSource(alarm.getSource());
        bo.setAlarmSourceType(alarm.getSourceType());
        bo.setCreateDate(now);
        insertByBo(bo);
    }

    private void insertByBo(DzDefRespAlarmHistoryBo bo) {
        DzDefRespAlarmHistory add = MapstructUtils.convert(bo, DzDefRespAlarmHistory.class);
        validEntityBeforeSave(add);
        if (baseMapper.insert(add) <= 0) {
            throw new ServiceException("新增警报转防御响应历史失败");
        }
        bo.setId(add.getId());
    }

    private void validEntityBeforeSave(DzDefRespAlarmHistory entity) {
        if (entity.getId() == null) {
            throw new ServiceException("历史记录id不能为空");
        }
        if (entity.getAlarmId() == null) {
            throw new ServiceException("alarmId不能为空");
        }
        if (entity.getDefId() == null) {
            throw new ServiceException("defId不能为空");
        }
        if (entity.getTriggerTime() == null) {
            throw new ServiceException("triggerTime不能为空");
        }
        if (entity.getCreateDate() == null) {
            throw new ServiceException("createDate不能为空");
        }
    }
}
