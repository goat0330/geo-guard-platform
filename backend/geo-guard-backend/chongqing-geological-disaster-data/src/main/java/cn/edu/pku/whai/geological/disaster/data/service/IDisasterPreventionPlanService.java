/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DisasterPreventionPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DisasterPreventionPlanVo;

import java.util.Collection;
import java.util.List;

/**
 * 防灾预案Service接口
 **/
public interface IDisasterPreventionPlanService {

    /**
     * 查询防灾预案
     *
     * @param id 主键
     * @return 防灾预案
     */
    DisasterPreventionPlanVo queryById(String id);

    /**
     * 分页查询防灾预案列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 防灾预案分页列表
     */
    TableDataInfo<DisasterPreventionPlanVo> queryPageList(DisasterPreventionPlanBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的防灾预案列表
     *
     * @param bo 查询条件
     * @return 防灾预案列表
     */
    List<DisasterPreventionPlanVo> queryList(DisasterPreventionPlanBo bo);

    /**
     * 新增防灾预案
     *
     * @param bo 防灾预案
     * @return 是否新增成功
     */
    Boolean insertByBo(DisasterPreventionPlanBo bo);

    /**
     * 修改防灾预案
     *
     * @param bo 防灾预案
     * @return 是否修改成功
     */
    Boolean updateByBo(DisasterPreventionPlanBo bo);

    /**
     * 校验并批量删除防灾预案信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
