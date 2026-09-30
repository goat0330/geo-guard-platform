package cn.edu.pku.whai.geological.disaster.service.mapper;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListRemark;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务备注 Mapper 接口
 *
 * @author kongweiguang
 * @date 2026-04-27
 */
public interface DzTaskDistListRemarkMapper {

    /**
     * 新增备注
     *
     * @param entity 备注实体
     * @return 影响行数
     */
    @Insert("""
        INSERT INTO dz_task_dist_list_remark(
            id, task_id, remark, create_date, update_date, user_id
        )
        VALUES (
            #{entity.id}, #{entity.taskId}, #{entity.remark}, #{entity.createDate}, #{entity.updateDate}, #{entity.userId}
        )
        """)
    int insertEntity(@Param("entity") DzTaskDistListRemark entity);

    /**
     * 按任务id查询每个任务的最新一条备注
     *
     * @param taskIds 任务id列表
     * @return 最新备注列表（每个task_id最多一条）
     */
    @Select("""
        <script>
        SELECT DISTINCT ON (task_id) id, task_id, remark, create_date, update_date, user_id
        FROM dz_task_dist_list_remark
        WHERE task_id IN
        <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
            #{taskId}
        </foreach>
        ORDER BY task_id, create_date DESC, id DESC
        </script>
        """)
    List<DzTaskDistListRemark> selectLatestByTaskIds(@Param("taskIds") List<Long> taskIds);

    /**
     * 按任务id列表查询全部备注，按任务和创建时间倒序。
     *
     * @param taskIds 任务id列表
     * @return 备注列表
     */
    @Select("""
        <script>
        SELECT r.id, r.task_id, r.remark, r.create_date, r.update_date, r.user_id, u.nick_name AS user_name
        FROM dz_task_dist_list_remark r
        LEFT JOIN sys_user u ON u.user_id = r.user_id
        WHERE r.task_id IN
        <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
            #{taskId}
        </foreach>
        ORDER BY r.task_id, r.create_date DESC, r.id DESC
        </script>
        """)
    List<DzTaskDistListRemark> selectListByTaskIds(@Param("taskIds") List<Long> taskIds);

    /**
     * 按任务id查询全部备注，按创建时间倒序
     *
     * @param taskId 任务id
     * @return 备注列表
     */
    @Select("""
        SELECT r.id, r.task_id, r.remark, r.create_date, r.update_date, r.user_id, u.nick_name AS user_name
        FROM dz_task_dist_list_remark r
        LEFT JOIN sys_user u ON u.user_id = r.user_id
        WHERE r.task_id = #{taskId}
        ORDER BY r.create_date DESC, r.id DESC
        """)
    List<DzTaskDistListRemark> selectListByTaskId(@Param("taskId") Long taskId);
}
