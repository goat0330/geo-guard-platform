package cn.edu.pku.whai.geological.disaster.service.mapper;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListAddBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListAdd;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListAddVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务上报信息 Mapper 接口
 * 该表按 task_id + user_id 作为联合键使用，不暴露 MyBatis-Plus 的 xxById 能力，
 * 避免后续误用单主键 CRUD。
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
public interface DzTaskDistListAddMapper {

    DzTaskDistListAddVo selectVoByKeys(@Param("taskId") Long taskId, @Param("userId") Long userId);

    Page<DzTaskDistListAddVo> selectVoPage(@Param("page") Page<DzTaskDistListAdd> page,
                                           @Param("bo") DzTaskDistListAddBo bo);

    List<DzTaskDistListAddVo> selectVoList(@Param("bo") DzTaskDistListAddBo bo);

    List<DzTaskDistListAddVo> selectVoListByTaskIdsAndUserId(@Param("taskIds") List<Long> taskIds,
                                                             @Param("userId") Long userId);

    /**
     * 按任务id列表查询每个任务最新一条上报备注
     *
     * @param taskIds 任务id列表
     * @return 上报备注列表（每个task_id最多一条）
     */
    List<DzTaskDistListAddVo> selectLatestByTaskIds(@Param("taskIds") List<Long> taskIds);

    int insertEntity(@Param("entity") DzTaskDistListAdd entity);

    int updateByKeys(@Param("entity") DzTaskDistListAdd entity);

    int deleteByKeys(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
