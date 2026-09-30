/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainFilterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainSummaryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainSummaryVo;

import java.util.Set;

/**
 * 任务流程链路最新节点汇总 Service。
 */
public interface IDzTaskProcessChainSummaryService {

    TableDataInfo<TaskProcessChainSummaryVo> queryPageList(TaskProcessChainSummaryBo bo, PageQuery pageQuery);

    /**
     * 按 latest-process-node 同口径条件查询链路 id 集合。
     *
     * @param bo 筛链条件
     * @return 去重链路 id 集合
     */
    Set<String> queryChainIdsByFilter(TaskProcessChainFilterBo bo);

    void refreshByNode(DzTaskProcessChainNode node);

    void refreshReportRiskLevelIfCurrent(Long reportId);

    void refreshDynamicRiskLevelByRiskAssessment(Long riskId);

    void refreshDynamicRiskLevelByUnitId(String unitId);
}
