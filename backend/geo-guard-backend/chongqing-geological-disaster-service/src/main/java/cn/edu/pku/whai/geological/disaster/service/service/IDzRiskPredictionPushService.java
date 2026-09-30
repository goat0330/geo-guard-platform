/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionPushVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RiskPredictionPushReportVo;

import java.util.Collection;
import java.util.List;

/**
 * 地灾风险预测推送Service接口
 *
 * @author system
 * @date 2026-03-03
 */
public interface IDzRiskPredictionPushService {

    /**
     * 查询地灾风险预测推送
     *
     * @param id 主键
     * @return 地灾风险预测推送
     */
    DzRiskPredictionPushVo queryById(Long id);

    /**
     * 分页查询地灾风险预测推送列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地灾风险预测推送分页列表
     */
    TableDataInfo<DzRiskPredictionPushVo> queryPageList(DzRiskPredictionPushBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的地灾风险预测推送列表
     *
     * @param bo 查询条件
     * @return 地灾风险预测推送列表
     */
    List<DzRiskPredictionPushVo> queryList(DzRiskPredictionPushBo bo);

    /**
     * 新增地灾风险预测推送
     *
     * @param bo 地灾风险预测推送
     * @return 是否新增成功
     */
    Boolean insertByBo(DzRiskPredictionPushBo bo);

    /**
     * 修改地灾风险预测推送
     *
     * @param bo 地灾风险预测推送
     * @return 是否修改成功
     */
    Boolean updateByBo(DzRiskPredictionPushBo bo);

    /**
     * 校验并批量删除地灾风险预测推送信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    RiskPredictionPushReportVo pushReport(Long id);
}
