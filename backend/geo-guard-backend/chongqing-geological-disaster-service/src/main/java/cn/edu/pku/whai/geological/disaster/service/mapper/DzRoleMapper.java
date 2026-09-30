/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RoleUserVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRoleCountVo;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysRoleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DzRoleMapper extends BaseMapperPlus<SysRole, SysRoleVo> {
    @Select("""
            SELECT r.role_id,
                   r.role_name,
                   r.role_key,
                   COUNT(ur.user_id) AS count
            FROM sys_role r
                     LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id
            ${ew.customSqlSegment}
            """)
    List<StatRoleCountVo> selectRoleCount(@Param("ew") QueryWrapper<Object> ew);


    @Select("""
            SELECT u.user_id,
                   u.user_name,
                   u.nick_name,
                   u.email,
                   u.phonenumber,
                   u.status,
                   STRING_AGG(DISTINCT CAST(r.role_id AS TEXT), ',') AS matched_role_ids,
                   STRING_AGG(DISTINCT r.role_key, ',') AS matched_role_keys,
                   STRING_AGG(DISTINCT r.role_name, ',') AS matched_role_names,
                   u.create_time,
                   dee.expert_type,
                   dee.introduction,
                   dee.meeting_count,
                   d.dept_name AS dept_name
            FROM sys_user u
                     LEFT JOIN sys_user_role sur ON u.user_id = sur.user_id
                     LEFT JOIN sys_role r ON r.role_id = sur.role_id
                     LEFT JOIN dz_expert_ext dee ON dee.user_id = u.user_id
                     LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
            ${ew.customSqlSegment}
            """)
    Page<RoleUserVo> selectRoleUserPage(@Param("page") Page<SysUser> build, @Param("ew") QueryWrapper<Object> ew);

}
