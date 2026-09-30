/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskProcessChainNode;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskProcessChainNodeVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 任务流程链路节点 Mapper
 */
public interface DzTaskProcessChainNodeMapper extends BaseMapperPlus<DzTaskProcessChainNode, TaskProcessChainNodeVo> {

    @Select("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(#{chainId}, 0))")
    Integer lockChain(@Param("chainId") String chainId);

    @Select("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(chain_id FROM 10) AS INTEGER)), 0)
        FROM dz_task_process_chain_node
        WHERE chain_id LIKE CONCAT(#{datePrefix}, '-%')
          AND chain_id ~ CONCAT('^', #{datePrefix}, '-[0-9]+$')
        """)
    Integer selectMaxDailyChainSequence(@Param("datePrefix") String datePrefix);

    @Select("""
        <script>
        SELECT DISTINCT ON (task_id)
               id,
               chain_id,
               parent_chain_id,
               parent_node_id,
               link_name,
               trigger_reason,
               operator_id,
               operator_name,
               operator_role,
               trigger_time,
               biz_type,
               biz_id,
               task_id,
               source_type,
               node_category,
               stage_type,
               root_chain_id,
               root_chain_closed,
               display_biz_type,
               display_biz_id,
               chain_segment_type,
               root_biz_type,
               root_biz_id,
               root_source_type,
               deleted,
               create_date,
               update_date
        FROM dz_task_process_chain_node
        WHERE deleted = 0
          AND task_id IN
        <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
            #{taskId}
        </foreach>
        ORDER BY task_id, create_date DESC, id DESC
        </script>
        """)
    List<DzTaskProcessChainNode> selectLatestByTaskIds(@Param("taskIds") List<Long> taskIds);

    @Select("""
        SELECT *
        FROM (
            SELECT DISTINCT ON (chain_id)
                   id,
                   chain_id,
                   parent_chain_id,
                   parent_node_id,
                   link_name,
                   trigger_reason,
                   operator_id,
                   operator_name,
                   operator_role,
                   trigger_time,
                   biz_type,
                   biz_id,
                   task_id,
                   source_type,
                   node_category,
                   stage_type,
                   root_chain_id,
                   root_chain_closed,
                   display_biz_type,
                   display_biz_id,
                   chain_segment_type,
                   root_biz_type,
                   root_biz_id,
                   root_source_type,
                   deleted,
                   create_date,
                   update_date
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
            ORDER BY chain_id, create_date DESC, id DESC
        ) latest
        ORDER BY create_date DESC, id DESC
        """)
    List<DzTaskProcessChainNode> selectLatestNodesByChain();

    @Select("""
        SELECT *
        FROM (
            SELECT DISTINCT ON (chain_id)
                   id,
                   chain_id,
                   parent_chain_id,
                   parent_node_id,
                   link_name,
                   trigger_reason,
                   operator_id,
                   operator_name,
                   operator_role,
                   trigger_time,
                   biz_type,
                   biz_id,
                   task_id,
                   source_type,
                   node_category,
                   stage_type,
                   root_chain_id,
                   root_chain_closed,
                   display_biz_type,
                   display_biz_id,
                   chain_segment_type,
                   root_biz_type,
                   root_biz_id,
                   root_source_type,
                   deleted,
                   create_date,
                   update_date
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
              AND task_id IS NOT NULL
            ORDER BY chain_id, create_date DESC, id DESC
        ) latest
        ORDER BY create_date DESC, id DESC
        """)
    List<DzTaskProcessChainNode> selectLatestTaskNodesByChain();

    @Select("""
        SELECT DISTINCT ON (parent.chain_id, child.task_id)
               child.id,
               child.chain_id,
               COALESCE(child.parent_chain_id, parent.chain_id) AS parent_chain_id,
               child.parent_node_id,
               child.link_name,
               child.trigger_reason,
               child.operator_id,
               child.operator_name,
               child.operator_role,
               child.trigger_time,
               child.biz_type,
               child.biz_id,
               child.task_id,
               child.source_type,
               child.node_category,
               child.stage_type,
               child.root_chain_id,
               child.root_chain_closed,
               child.display_biz_type,
               child.display_biz_id,
               child.chain_segment_type,
               child.root_biz_type,
               child.root_biz_id,
               child.root_source_type,
               child.deleted,
               child.create_date,
               child.update_date
        FROM dz_task_process_chain_node parent
        INNER JOIN dz_task_process_chain_node child ON child.parent_node_id = parent.id
        WHERE parent.deleted = 0
          AND child.deleted = 0
          AND parent.chain_id IS NOT NULL
          AND parent.node_category = 7
          AND child.task_id IS NOT NULL
        ORDER BY parent.chain_id, child.task_id, child.create_date ASC, child.id ASC
        """)
    List<DzTaskProcessChainNode> selectEmergencyChildTaskNodesByParentChain();

    @Select("""
        <script>
        SELECT DISTINCT ON (task_id)
               id,
               chain_id,
               parent_chain_id,
               parent_node_id,
               link_name,
               trigger_reason,
               operator_id,
               operator_name,
               operator_role,
               trigger_time,
               biz_type,
               biz_id,
               task_id,
               source_type,
               node_category,
               stage_type,
               root_chain_id,
               root_chain_closed,
               display_biz_type,
               display_biz_id,
               chain_segment_type,
               root_biz_type,
               root_biz_id,
               root_source_type,
               deleted,
               create_date,
               update_date
        FROM dz_task_process_chain_node
        WHERE deleted = 0
          AND task_id IS NOT NULL
          AND parent_node_id IN
        <foreach collection="parentNodeIds" item="parentNodeId" open="(" separator="," close=")">
            #{parentNodeId}
        </foreach>
        ORDER BY task_id, create_date ASC, id ASC
        </script>
        """)
    List<DzTaskProcessChainNode> selectChildTaskNodesByParentIds(@Param("parentNodeIds") List<Long> parentNodeIds);

    @Select("""
        SELECT DISTINCT main_chain_id
        FROM (
            SELECT COALESCE(NULLIF(root_chain_id, ''), NULLIF(parent_chain_id, ''), chain_id) AS main_chain_id,
                   create_date,
                   id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND task_id = #{taskId}
              AND chain_id IS NOT NULL
        ) t
        WHERE main_chain_id IS NOT NULL
        ORDER BY main_chain_id
        """)
    List<String> selectMainChainIdsByTaskId(@Param("taskId") Long taskId);

    @Select("""
        SELECT DISTINCT main_chain_id
        FROM (
            SELECT COALESCE(NULLIF(root_chain_id, ''), NULLIF(parent_chain_id, ''), chain_id) AS main_chain_id,
                   create_date,
                   id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
              AND (
                  (display_biz_type = #{handleBizType} AND display_biz_id = #{handleId})
                  OR (biz_type = #{handleBizType} AND biz_id = #{handleId})
                  OR (root_biz_type = #{handleBizType} AND root_biz_id = #{handleId})
              )
        ) t
        WHERE main_chain_id IS NOT NULL
        ORDER BY main_chain_id
        """)
    List<String> selectMainChainIdsByHandleId(@Param("handleId") Long handleId,
                                              @Param("handleBizType") Integer handleBizType);

    @Select("""
        SELECT COUNT(*)
        FROM (
            SELECT main_chain_id, MAX(ended_at) AS ended_at
            FROM (
                SELECT COALESCE(NULLIF(chain_id, ''), NULLIF(root_chain_id, '')) AS main_chain_id,
                       COALESCE(trigger_time, create_date) AS ended_at
                FROM dz_task_process_chain_node
                WHERE deleted = 0
                  AND root_chain_closed = 1
                  AND chain_id IS NOT NULL
                  AND (
                      node_category IN (8, 9, 13, 16)
                      OR link_name LIKE '%任务关闭'
                      OR link_name LIKE '%任务过期'
                      OR link_name IN ('结束归档', '防御响应结束归档', '灾险情关闭', 'AI险情核实任务反馈', '现场处置任务反馈')
                  )
            ) ended_nodes
            WHERE main_chain_id IS NOT NULL
              AND ended_at IS NOT NULL
            GROUP BY main_chain_id
        ) ended_main_chains
        WHERE ended_at >= #{startTime}
          AND ended_at <= #{endTime}
    """)
    Long selectEndedMainChainCount(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("""
        SELECT *
        FROM (
            SELECT DISTINCT ON (chain_id)
                   id,
                   chain_id,
                   parent_chain_id,
                   parent_node_id,
                   link_name,
                   trigger_reason,
                   operator_id,
                   operator_name,
                   operator_role,
                   trigger_time,
                   biz_type,
                   biz_id,
                   task_id,
                   source_type,
                   node_category,
                   stage_type,
                   root_chain_id,
                   root_chain_closed,
                   display_biz_type,
                   display_biz_id,
                   chain_segment_type,
                   root_biz_type,
                   root_biz_id,
                   root_source_type,
                   deleted,
                   create_date,
                   update_date
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id IS NOT NULL
              AND (root_chain_id = #{rootChainId}
                OR (root_chain_id IS NULL AND chain_id = #{rootChainId}))
            ORDER BY chain_id, create_date DESC, id DESC
        ) latest
        ORDER BY create_date DESC, id DESC
    """)
    List<DzTaskProcessChainNode> selectLatestNodesByRootChainId(@Param("rootChainId") String rootChainId);

    @Select("""
        WITH RECURSIVE chain_tree AS (
            SELECT id,
                   chain_id,
                   parent_chain_id,
                   parent_node_id,
                   link_name,
                   trigger_reason,
                   operator_id,
                   operator_name,
                   operator_role,
                   trigger_time,
                   biz_type,
                   biz_id,
                   task_id,
                   source_type,
                   node_category,
                   stage_type,
                   root_chain_id,
                   root_chain_closed,
                   display_biz_type,
                   display_biz_id,
                   chain_segment_type,
                   root_biz_type,
                   root_biz_id,
                   root_source_type,
                   deleted,
                   create_date,
                   update_date
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id = #{chainId}
            UNION
            SELECT child.id,
                   child.chain_id,
                   child.parent_chain_id,
                   child.parent_node_id,
                   child.link_name,
                   child.trigger_reason,
                   child.operator_id,
                   child.operator_name,
                   child.operator_role,
                   child.trigger_time,
                   child.biz_type,
                   child.biz_id,
                   child.task_id,
                   child.source_type,
                   child.node_category,
                   child.stage_type,
                   child.root_chain_id,
                   child.root_chain_closed,
                   child.display_biz_type,
                   child.display_biz_id,
                   child.chain_segment_type,
                   child.root_biz_type,
                   child.root_biz_id,
                   child.root_source_type,
                   child.deleted,
                   child.create_date,
                   child.update_date
            FROM dz_task_process_chain_node child
            INNER JOIN chain_tree parent ON child.parent_node_id = parent.id
            WHERE child.deleted = 0
        )
        SELECT *
        FROM (
            SELECT DISTINCT ON (chain_id)
                   id,
                   chain_id,
                   parent_chain_id,
                   parent_node_id,
                   link_name,
                   trigger_reason,
                   operator_id,
                   operator_name,
                   operator_role,
                   trigger_time,
                   biz_type,
                   biz_id,
                   task_id,
                   source_type,
                   node_category,
                   stage_type,
                   root_chain_id,
                   root_chain_closed,
                   display_biz_type,
                   display_biz_id,
                   chain_segment_type,
                   root_biz_type,
                   root_biz_id,
                   root_source_type,
                   deleted,
                   create_date,
                   update_date
            FROM chain_tree
            WHERE chain_id IS NOT NULL
            ORDER BY chain_id, create_date DESC, id DESC
        ) latest
        ORDER BY create_date DESC, id DESC
        """)
    List<DzTaskProcessChainNode> selectLatestNodesByChainTree(@Param("chainId") String chainId);

    @Update("""
        WITH RECURSIVE chain_tree AS (
            SELECT id
            FROM dz_task_process_chain_node
            WHERE deleted = 0
              AND chain_id = #{chainId}
            UNION
            SELECT child.id
            FROM dz_task_process_chain_node child
            INNER JOIN chain_tree parent ON child.parent_node_id = parent.id
            WHERE child.deleted = 0
        )
        UPDATE dz_task_process_chain_node
        SET root_chain_closed = #{rootChainClosed}
        WHERE deleted = 0
          AND id IN (SELECT id FROM chain_tree)
        """)
    int updateRootChainClosedByChainTree(@Param("chainId") String chainId,
                                         @Param("rootChainClosed") Integer rootChainClosed);

    @Select("""
        <script>
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
            WHERE chain_id IN
            <foreach collection="chainIds" item="chainId" open="(" separator="," close=")">
                #{chainId}
            </foreach>
            UNION
            SELECT child.chain_id
            FROM chain_relations child
            INNER JOIN scoped_chains parent ON child.parent_chain_id = parent.chain_id
        )
        SELECT node.task_id
        FROM dz_task_process_chain_node node
        INNER JOIN scoped_chains scoped ON scoped.chain_id = node.chain_id
        WHERE node.deleted = 0
          AND node.task_id IS NOT NULL
        ORDER BY node.create_date ASC, node.id ASC
        </script>
        """)
    List<Long> selectRelatedTaskIdsByChainIds(@Param("chainIds") Collection<String> chainIds);
}
