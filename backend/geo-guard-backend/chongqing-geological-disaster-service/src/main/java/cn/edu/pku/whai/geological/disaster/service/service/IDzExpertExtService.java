/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzExpertExtBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzExpertExtVo;

import java.util.Collection;
import java.util.List;

/**
 * 专家扩展信息Service接口
 *
 * @author kongweiguang
 * @date 2026-02-03
 */
public interface IDzExpertExtService {

    /**
     * 查询专家扩展信息
     *
     * @param id 主键
     * @return 专家扩展信息
     */
    DzExpertExtVo queryById(Long id);

    /**
     * 分页查询专家扩展信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 专家扩展信息分页列表
     */
    TableDataInfo<DzExpertExtVo> queryPageList(DzExpertExtBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的专家扩展信息列表
     *
     * @param bo 查询条件
     * @return 专家扩展信息列表
     */
    List<DzExpertExtVo> queryList(DzExpertExtBo bo);

    /**
     * 新增专家扩展信息
     *
     * @param bo 专家扩展信息
     * @return 是否新增成功
     */
    Boolean insertByBo(DzExpertExtBo bo);

    /**
     * 修改专家扩展信息
     *
     * @param bo 专家扩展信息
     * @return 是否修改成功
     */
    Boolean updateByBo(DzExpertExtBo bo);

    /**
     * 校验并批量删除专家扩展信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
