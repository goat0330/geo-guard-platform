package cn.edu.pku.whai.geological.disaster.service.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionChartBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionChartVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionVo;

import java.util.Collection;
import java.util.List;

/**
 * 风险预测Service接口
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
public interface IDzRiskPredictionService {

    /**
     * 查询风险预测
     *
     * @param id 主键
     * @return 风险预测
     */
    DzRiskPredictionVo queryById(Long id);

    /**
     * 根据批次ID查询风险预测列表
     *
     * @param batchId 批次ID
     * @return 风险预测列表
     */
    List<DzRiskPredictionVo> queryByBatchId(Long batchId);

    /**
     * 分页查询风险预测列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 风险预测分页列表
     */
    TableDataInfo<DzRiskPredictionVo> queryPageList(DzRiskPredictionBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的风险预测列表
     *
     * @param bo 查询条件
     * @return 风险预测列表
     */
    List<DzRiskPredictionVo> queryList(DzRiskPredictionBo bo);

    /**
     * 新增风险预测
     *
     * @param bo 风险预测
     * @return 是否新增成功
     */
    Boolean insertByBo(DzRiskPredictionBo bo);

    /**
     * 修改风险预测
     *
     * @param bo 风险预测
     * @return 是否修改成功
     */
    Boolean updateByBo(DzRiskPredictionBo bo);

    /**
     * 校验并批量删除风险预测信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    DzRiskPredictionStatVo stat(DzRiskPredictionStatBo bo);

    DzRiskPredictionChartVo statChart(DzRiskPredictionChartBo bo);
}
