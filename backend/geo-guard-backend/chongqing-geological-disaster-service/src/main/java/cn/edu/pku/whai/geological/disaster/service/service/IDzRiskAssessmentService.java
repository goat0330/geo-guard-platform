/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentInfoBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentQueryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;

import java.util.Date;
import java.util.List;

/**
 * 斜坡单元风险评估Service接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface IDzRiskAssessmentService {

    /**
     * 查询斜坡单元风险评估
     *
     * @param id 主键
     * @return 斜坡单元风险评估
     */
    DzRiskAssessmentVo queryById(Long id);

    DzRiskAssessmentVo queryByUnitId(String id, Date time);

    /**
     * 分页查询斜坡单元风险评估列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 斜坡单元风险评估分页列表
     */
    TableDataInfo<DzRiskAssessmentVo> queryPageList(DzRiskAssessmentBo bo, PageQuery pageQuery);

    /**
     * 查询今天临时动态风险等级不为空的风险评估列表
     *
     * @return 斜坡单元风险评估列表
     */
    List<DzRiskAssessmentVo> queryTodayTemDynamicRiskList();

    /**
     * 根据条件查询斜坡单元风险评估列表
     *
     * @param bo 查询条件
     * @return 结果列表
     */
    DzRiskAssessmentQueryVo queryRiskList(DzRiskAssessmentQueryBo bo);

    DzRiskAssessmentStatVo stat(DzRiskAssessmentStatBo bo);

    List<RiskHazardQueryVo> statHazard(DzRiskAssessmentStatBo n);

    DzRiskAssessmentVo getInfo(DzRiskAssessmentInfoBo bo);

    RiskChatBannerVo statChatBanner(DzRiskAssessmentStatBo bo);

}
