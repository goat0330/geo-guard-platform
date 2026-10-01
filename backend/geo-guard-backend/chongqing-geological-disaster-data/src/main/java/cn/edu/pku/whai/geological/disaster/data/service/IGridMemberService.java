/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.GridMemberBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.GridMember;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GridMemberVo;

import java.util.Collection;
import java.util.List;

/**
 * 网格人员信息Service接口
 **/
public interface IGridMemberService {

    /**
     * 查询网格人员信息
     *
     * @param userId 主键
     * @return 网格人员信息
     */
    GridMemberVo queryById(String userId);

    /**
     * 分页查询网格人员信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 网格人员信息分页列表
     */
    TableDataInfo<GridMemberVo> queryPageList(GridMemberBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的网格人员信息列表
     *
     * @param bo 查询条件
     * @return 网格人员信息列表
     */
    List<GridMemberVo> queryList(GridMemberBo bo);

    /**
     * 新增网格人员信息
     *
     * @param bo 网格人员信息
     * @return 是否新增成功
     */
    Boolean insertByBo(GridMemberBo bo);

    /**
     * 修改网格人员信息
     *
     * @param bo 网格人员信息
     * @return 是否修改成功
     */
    Boolean updateByBo(GridMemberBo bo);

    /**
     * 校验并批量删除网格人员信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    GridMember queryByGridId(String village);
}
