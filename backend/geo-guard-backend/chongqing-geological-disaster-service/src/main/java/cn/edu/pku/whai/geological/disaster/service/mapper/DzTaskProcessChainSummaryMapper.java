/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainSummary;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainSummaryVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 任务流程链路最新节点汇总 Mapper
 */
public interface DzTaskProcessChainSummaryMapper
    extends BaseMapperPlus<DzTaskProcessChainSummary, TaskProcessChainSummaryVo> {

    @Select("""
        SELECT MAX((level_item.key)::int)
        FROM data_alarm alarm
             CROSS JOIN LATERAL jsonb_each_text(alarm.level_streets_json::jsonb) AS level_item
        WHERE alarm.id = #{id}
          AND alarm.level_streets_json IS NOT NULL
          AND alarm.level_streets_json <> ''
        """)
    Integer selectAlarmMaxLevel(@Param("id") Long id);

    @Select("""
        WITH RECURSIVE chain_relations AS (
            SELECT DISTINCT ON (chain_id)
                   chain_id,
                   parent_chain_id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
            ORDER BY chain_id, create_date DESC, id DESC
        ),
        scoped_chains AS (
            SELECT chain_id
            FROM chain_relations
            WHERE chain_id = #{chainId}
            UNION
            SELECT child.chain_id
            FROM chain_relations child
            INNER JOIN scoped_chains parent ON child.parent_chain_id = parent.chain_id
        )
        SELECT COUNT(DISTINCT task.id)::int
        FROM dz_task_process_chain_node node
        INNER JOIN scoped_chains scoped ON scoped.chain_id = node.chain_id
        INNER JOIN dz_task_dist_list task ON task.id = node.task_id
        WHERE node.deleted = 0
          AND task.delete = 0
          AND task.status = 1
        """)
    Integer selectPendingPushTaskCountByChainId(@Param("chainId") String chainId);

    @Select("""
        WITH RECURSIVE chain_relations AS (
            SELECT DISTINCT ON (chain_id)
                   chain_id,
                   parent_chain_id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
            ORDER BY chain_id, create_date DESC, id DESC
        ),
        scoped_chains AS (
            SELECT chain_id
            FROM chain_relations
            WHERE chain_id = #{chainId}
            UNION
            SELECT child.chain_id
            FROM chain_relations child
            INNER JOIN scoped_chains parent ON child.parent_chain_id = parent.chain_id
        ),
        biz_candidates AS (
            SELECT node.create_date,
                   node.id,
                   CASE
                       WHEN node.display_biz_type = #{bizType} THEN node.display_biz_id
                       WHEN node.biz_type = #{bizType} THEN node.biz_id
                       WHEN node.root_biz_type = #{bizType} THEN node.root_biz_id
                       ELSE NULL
                   END AS biz_id
            FROM dz_task_process_chain_node node
            INNER JOIN scoped_chains scoped ON scoped.chain_id = node.chain_id
            WHERE node.deleted = 0
        )
        SELECT biz_id
        FROM biz_candidates
        WHERE biz_id IS NOT NULL
        ORDER BY create_date DESC, id DESC
        LIMIT 1
    """)
    Long selectLatestBizIdByChainIdAndType(@Param("chainId") String chainId, @Param("bizType") Integer bizType);

    @Update("""
        WITH RECURSIVE chain_tree AS (
            SELECT id, chain_id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id = #{chainId}
            UNION
            SELECT child.id, child.chain_id
            FROM dz_task_process_chain_node child
            INNER JOIN chain_tree parent ON child.parent_node_id = parent.id
            WHERE child.deleted = 0
        ),
        scoped_chains AS (
            SELECT DISTINCT chain_id
            FROM chain_tree
            WHERE chain_id IS NOT NULL
        )
        UPDATE dz_task_process_chain_summary
        SET root_chain_closed = #{rootChainClosed}
        WHERE deleted = 0
          AND chain_id IN (SELECT chain_id FROM scoped_chains)
        """)
    int updateRootChainClosedByChainTree(@Param("chainId") String chainId,
                                         @Param("rootChainClosed") Integer rootChainClosed);
}
