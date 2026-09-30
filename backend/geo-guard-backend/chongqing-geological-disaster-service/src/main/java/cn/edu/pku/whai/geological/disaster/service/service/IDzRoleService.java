/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRoleUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RoleUserVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRoleCountVo;
import org.dromara.system.domain.vo.SysRoleVo;

import java.util.List;

public interface IDzRoleService {
    List<StatRoleCountVo> statCount();
    TableDataInfo<RoleUserVo> queryPageList(DzRoleUserBo bo, PageQuery pageQuery);
    List<SysRoleVo> queryRolesByUserId(Long userId);
}
