/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStepsBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskAssessmentStepsVo;

import java.util.Collection;
import java.util.List;

/**
 * 地质灾害预测与易发性评价数据Service接口
 *
 * @author kongweiguang
 * @date 2026-01-19
 */
public interface IDzRiskAssessmentStepsService {

    /**
     * 查询地质灾害预测与易发性评价数据
     *
     * @param id 主键
     * @return 地质灾害预测与易发性评价数据
     */
    DzRiskAssessmentStepsVo queryById(Long id);

    /**
     * 分页查询地质灾害预测与易发性评价数据列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地质灾害预测与易发性评价数据分页列表
     */
    TableDataInfo<DzRiskAssessmentStepsVo> queryPageList(DzRiskAssessmentStepsBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的地质灾害预测与易发性评价数据列表
     *
     * @param bo 查询条件
     * @return 地质灾害预测与易发性评价数据列表
     */
    List<DzRiskAssessmentStepsVo> queryList(DzRiskAssessmentStepsBo bo);

    /**
     * 新增地质灾害预测与易发性评价数据
     *
     * @param bo 地质灾害预测与易发性评价数据
     * @return 是否新增成功
     */
    Boolean insertByBo(DzRiskAssessmentStepsBo bo);

    /**
     * 修改地质灾害预测与易发性评价数据
     *
     * @param bo 地质灾害预测与易发性评价数据
     * @return 是否修改成功
     */
    Boolean updateByBo(DzRiskAssessmentStepsBo bo);

    /**
     * 校验并批量删除地质灾害预测与易发性评价数据信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
