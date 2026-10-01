/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.OrganizationInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.OrganizationInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * 组织机构信息Service接口
 **/
public interface IOrganizationInfoService {

    /**
     * 查询组织机构信息
     *
     * @param id 主键
     * @return 组织机构信息
     */
    OrganizationInfoVo queryById(String id);

    /**
     * 分页查询组织机构信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 组织机构信息分页列表
     */
    TableDataInfo<OrganizationInfoVo> queryPageList(OrganizationInfoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的组织机构信息列表
     *
     * @param bo 查询条件
     * @return 组织机构信息列表
     */
    List<OrganizationInfoVo> queryList(OrganizationInfoBo bo);

    /**
     * 新增组织机构信息
     *
     * @param bo 组织机构信息
     * @return 是否新增成功
     */
    Boolean insertByBo(OrganizationInfoBo bo);

    /**
     * 修改组织机构信息
     *
     * @param bo 组织机构信息
     * @return 是否修改成功
     */
    Boolean updateByBo(OrganizationInfoBo bo);

    /**
     * 校验并批量删除组织机构信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
