/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 斜坡单元风险评估Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface DzRiskAssessmentMapper extends BaseMapperPlus<DzRiskAssessment, DzRiskAssessmentVo> {

    List<StatRiskVo> statRisk(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<StatRiskVo> statHazardPoint(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<RiskHazardQueryVo> statHazard(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<StatAreaVo> statArea(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<Map<String, Object>> statAttribution(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select( """
              SELECT t1.dynamic_risk_level,
                     t1.create_date
              FROM dz_risk_assessment t1
                             LEFT JOIN data_slope_unit t2 ON t1.slope_unit_id = t2.id
              ${ew.customSqlSegment}
            """)
    List<DzRiskAssessmentVo> selectRiskAndSlopeUnit(@Param(Constants.WRAPPER) Wrapper<DzRiskAssessment> queryWrapper);

    @Select("""
            select dynamic_risk_level,
                   CAST(COUNT(id) AS INTEGER) AS count
            FROM dz_risk_assessment
            WHERE dynamic_risk_level IN (3, 4)
            AND create_date BETWEEN #{startTime} AND #{endTime}
            group by dynamic_risk_level
            """)
    List<StatRiskVo> statCurrentRisk(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<StatRiskVo> statMonitDevice(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("""
            select dynamic_risk_level,
                   CAST(COUNT(id) AS INTEGER) AS count
            from dz_risk_assessment
            ${ew.customSqlSegment}
            """)
    List<StatRiskVo> statRiskByRiskIds(@Param(Constants.WRAPPER) Wrapper<DzRiskAssessment> lqw1);

    List<RiskChatBannerVo.RiskChatBannerItemVo> statAreaTop3(@Param("bo") DzRiskAssessmentStatBo bo, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

}
