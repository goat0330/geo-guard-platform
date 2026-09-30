package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 任务处置审批Mapper接口
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
public interface DzTaskHandleApprovalMapper extends BaseMapperPlus<DzTaskHandleApproval, DzTaskHandleApprovalVo> {

    /**
     * 类型 1：仅状态为 1 的最新一条（关联 sys_user_role、sys_role 带出 role_name）
     */
    List<DzTaskHandleApprovalVo> selectListStatusType1(@Param("handleId") Long handleId, @Param("roundNo") Integer roundNo);

    /**
     * 类型 2：不限状态，按创建时间升序全部（关联 sys_user_role、sys_role 带出 role_name）
     */
    List<DzTaskHandleApprovalVo> selectListStatusType2(@Param("handleId") Long handleId, @Param("roundNo") Integer roundNo);

    @Update("""
        update dz_expert_ext
        set meeting_count = meeting_count + 1
        where user_id = #{userId}
        """)
    void updateExpertCount(@Param("userId") Long userId);
}
