/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserVo;
import org.dromara.system.domain.bo.SysUserBo;

/**
 * 地灾项目用户Service接口
 */
public interface IDzUserService {

    /**
     * 分页查询用户列表，并补充行政区划信息
     *
     * @param user 用户查询条件
     * @param pageQuery 分页参数
     * @return 用户分页列表
     */
    TableDataInfo<DzUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery);

    long insertUser(DzUserBo user);

    int updateUser(DzUserBo user);

}
