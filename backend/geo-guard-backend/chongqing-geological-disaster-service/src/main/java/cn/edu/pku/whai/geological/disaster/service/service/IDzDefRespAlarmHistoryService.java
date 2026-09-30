/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;

public interface IDzDefRespAlarmHistoryService {

    /**
     * 记录警报信息转防御响应的触发历史。
     */
    void saveHistory(DataAlarmVo alarm, DefRespPlan defRespPlan);
}
