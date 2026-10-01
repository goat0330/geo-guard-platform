/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DataAlarmBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataAlarmReportAnalysisReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingStatusBatchUpdateResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingGroupResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.WeatherAlarmStatsResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface IDataAlarmService {

    DataAlarmVo queryById(Long id);

    TableDataInfo<DataAlarmVo> queryPageList(DataAlarmBo bo, PageQuery pageQuery);

    List<DataAlarmVo> queryList(DataAlarmBo bo);

    /**
     * 按指定时间匹配有效预警并返回排序结果
     */
    List<DataAlarmVo> listMatchByData(Date data);

    /**
     * 查询未触发预警列表，并按发布日期分组返回
     */
    List<DataAlarmPendingGroupResp> queryPendingGroupList();

    Boolean insertByBo(DataAlarmBo bo);

    /**
     * 人工补录预警：仅需 publishDate、validStartDate、validEndDate、level、city、streets，code 由系统生成。
     */
    Long insertManualByBo(DataAlarmBo bo);

    /**
     * 修改人工补录预警，按主键更新人工补录相关字段，保留原预警编号。
     */
    Long updateManualByBo(DataAlarmBo bo);

    Boolean insertBatchByBo(List<DataAlarmBo> boList);

    Boolean updateByBo(DataAlarmBo bo);

    /**
     * 按主键仅更新 status、pending_status、update_date，不走全量字段校验。
     */
    Boolean updateTriggerStatusById(Long id, Integer status, Integer pendingStatus, Date updateDate);

    /**
     * 批量更新待触发状态
     */
    DataAlarmPendingStatusBatchUpdateResp batchUpdatePendingStatus(List<String> ids, Integer pendingStatus);

    /**
     * 导入算法侧 report_analysis 预警数据
     */
    List<Long> importReportAnalysis(DataAlarmReportAnalysisReq req);

    Set<String> getExistingCodes(Collection<String> codes);

    List<String> listCodesBySessionId(String sessionId);

    /**
     * 关闭已过有效期且仍未触发的预警记录
     */
    int closeExpiredUntriggeredAlarms(Date now);

    /**
     * 获取指定id气象预警涉及的总面积、乡镇数量、斜坡单元数量
     *
     * @param id 告警主键id
     */
    WeatherAlarmStatsResp getWeatherAlarmStats(Long id);

    /**
     * 获取最新一批气象预警统计数据；若最新数据不是当天则返回空统计结果。
     */
    WeatherAlarmStatsResp latest();

    /**
     * 统计当天全部气象预警数据；若当天无数据则返回空统计结果。
     */
    WeatherAlarmStatsResp todayStats();
}
