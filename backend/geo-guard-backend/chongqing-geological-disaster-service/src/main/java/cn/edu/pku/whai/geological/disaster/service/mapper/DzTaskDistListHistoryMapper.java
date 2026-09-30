/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务提交历史记录 Mapper 接口
 */
public interface DzTaskDistListHistoryMapper {

    /**
     * 新增任务提交历史记录
     *
     * @param entity 历史记录实体
     * @return 影响行数
     */
    @Insert("""
        INSERT INTO dz_task_dist_list_history(
            id, task_id, text_record, submit_status, scene_photo, check_center, check_center_location,
            create_date, update_date, user_id
        )
        VALUES (
            #{entity.id}, #{entity.taskId}, #{entity.textRecord}, #{entity.submitStatus}, #{entity.scenePhoto},
            #{entity.checkCenter}, #{entity.checkCenterLocation}, #{entity.createDate}, #{entity.updateDate},
            #{entity.userId}
        )
        """)
    int insertEntity(@Param("entity") DzTaskDistListHistory entity);

    /**
     * 按历史记录主键查询提交历史。
     *
     * @param id 历史记录主键
     * @return 历史记录
     */
    @Select("""
        SELECT h.id, h.task_id, h.text_record, h.submit_status, h.scene_photo, h.check_center,
               h.check_center_location, h.create_date, h.update_date, h.user_id, u.nick_name AS user_name
        FROM dz_task_dist_list_history h
        LEFT JOIN sys_user u ON u.user_id = h.user_id
        WHERE h.id = #{id}
        """)
    DzTaskDistListHistory selectById(@Param("id") Long id);

    /**
     * 按任务id列表查询全部提交历史，按任务和创建时间倒序。
     *
     * @param taskIds 任务id列表
     * @return 历史记录列表
     */
    @Select("""
        <script>
        SELECT h.id, h.task_id, h.text_record, h.submit_status, h.scene_photo, h.check_center,
               h.check_center_location, h.create_date, h.update_date, h.user_id, u.nick_name AS user_name
        FROM dz_task_dist_list_history h
        LEFT JOIN sys_user u ON u.user_id = h.user_id
        WHERE h.task_id IN
        <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
            #{taskId}
        </foreach>
        ORDER BY h.task_id, h.create_date DESC, h.id DESC
        </script>
        """)
    List<DzTaskDistListHistory> selectListByTaskIds(@Param("taskIds") List<Long> taskIds);

    /**
     * 按任务id查询全部提交历史，按创建时间倒序。
     *
     * @param taskId 任务id
     * @return 历史记录列表
     */
    @Select("""
        SELECT h.id, h.task_id, h.text_record, h.submit_status, h.scene_photo, h.check_center,
               h.check_center_location, h.create_date, h.update_date, h.user_id, u.nick_name AS user_name
        FROM dz_task_dist_list_history h
        LEFT JOIN sys_user u ON u.user_id = h.user_id
        WHERE h.task_id = #{taskId}
        ORDER BY h.create_date DESC, h.id DESC
        """)
    List<DzTaskDistListHistory> selectListByTaskId(@Param("taskId") Long taskId);
}
