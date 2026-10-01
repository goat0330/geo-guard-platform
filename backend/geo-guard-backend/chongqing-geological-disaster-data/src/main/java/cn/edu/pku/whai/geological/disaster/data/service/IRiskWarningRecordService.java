/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskWarningRecord;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface IRiskWarningRecordService {

    /**
     * 批量新增第三方气象预警数据记录
     *
     * @param riskWarningRecords
     * @return
     */
    Boolean insertBatch(List<RiskWarningRecord> riskWarningRecords);

    /**
     * 根据ID集合查询已存在的记录ID（用于同步时过滤重复数据）
     *
     * @param ids 待检查的ID集合
     * @return 已存在的ID集合
     */
    Set<String> getExistingIds(Collection<String> ids);

    /**
     * 按预警日期范围查询记录（用于定时任务：获取待解析的预警列表）
     *
     * @param startInclusive 开始日期（含）
     * @param endInclusive   结束日期（含）
     * @return 预警记录列表
     */
    List<RiskWarningRecord> listByWarningDateBetween(Date startInclusive, Date endInclusive);

}
