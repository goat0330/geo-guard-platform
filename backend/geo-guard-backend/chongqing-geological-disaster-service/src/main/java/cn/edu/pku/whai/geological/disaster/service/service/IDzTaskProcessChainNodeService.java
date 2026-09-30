/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务流程链路节点 Service
 */
public interface IDzTaskProcessChainNodeService {

    /**
     * 记录流程节点。
     *
     * @param node 流程节点
     * @return 节点 id
     */
    Long recordNode(DzTaskProcessChainNode node);

    Long recordNodeIfAbsent(DzTaskProcessChainNode node);

    Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task);

    Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task,
                        Long parentNodeId, Integer nodeCategory);

    Long recordTaskNode(String chainId, String linkName, String triggerReason, DzTaskDistList task,
                        Long parentNodeId, Integer nodeCategory, Integer stageType);

    Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                       Long taskId, Long operatorId, String operatorName, Integer sourceType);

    Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                       Long taskId, Long operatorId, String operatorName, Integer sourceType,
                       Long parentNodeId, String operatorRole, Integer nodeCategory);

    Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                       Long taskId, Long operatorId, String operatorName, Integer sourceType,
                       Long parentNodeId, String operatorRole, Integer nodeCategory, Integer stageType);

    Long recordBizNode(String chainId, String linkName, String triggerReason, Integer bizType, Long bizId,
                       Long taskId, Long operatorId, String operatorName, Integer sourceType,
                       Long parentNodeId, String operatorRole, Integer nodeCategory, Integer stageType,
                       String taskType);

    String generateChainId();

    String resolveSavedChainIdByTaskId(Long taskId);

    String resolveSavedChainIdByBiz(Integer bizType, Long bizId);

    String resolveSavedChainIdByBizFast(Integer bizType, Long bizId);

    DzTaskProcessChainNode resolveSavedNodeByBizAndLink(Integer bizType, Long bizId, String linkName, Integer nodeCategory);

    DzTaskProcessChainNode resolveSavedNodeByBizAndLinkFast(Integer bizType, Long bizId, String linkName, Integer nodeCategory);

    DzTaskProcessChainNode resolveSavedNodeByBizAndCategory(Integer bizType, Long bizId, Integer nodeCategory);

    /**
     * 通过任务 id 查询真实流程链路；没有入库节点时抛出业务异常。
     *
     * @param taskId 任务 id
     * @return 流程链路节点列表
     */
    List<TaskProcessChainNodeVo> queryChainByTaskId(Long taskId);

    /**
     * 通过链路 id 查询整条流程链路。
     *
     * @param chainId 链路 id
     * @return 流程链路节点列表
     */
    List<TaskProcessChainNodeVo> queryChainByChainId(String chainId);

    /**
     * 按任务 id 批量查询每个任务最新一条流程节点。
     *
     * @param taskIds 任务 id 列表
     * @return key 为任务 id，value 为最新节点
     */
    Map<Long, TaskProcessChainNodeVo> queryLatestNodeByTaskIds(List<Long> taskIds);

    /**
     * 查询流程链路当前应展示的最新任务 id。
     *
     * @return 最新任务 id 集合
     */
    Set<Long> queryLatestDisplayTaskIds();

    /**
     * 按链路 id 集合批量归集当前链路及其递归子链关联的任务 id。
     *
     * @param chainIds 链路 id 集合
     * @return 任务 id 列表，允许包含重复值以便统计展开量
     */
    List<Long> queryRelatedTaskIdsByChainIds(Collection<String> chainIds);
}
