package cn.edu.pku.whai.geological.disaster.service.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPrediction;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RiskChatBannerVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatAreaVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRiskVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 风险预测Mapper接口
 *
 * @author kongweiguang
 * @date 2026-03-02
 */
public interface DzRiskPredictionMapper extends BaseMapperPlus<DzRiskPrediction, DzRiskPredictionVo> {

    Page<DzRiskPredictionVo> selectVoPageWithSlopeUnit(@Param("page") Page<DzRiskPrediction> page,
                                                       @Param(Constants.WRAPPER) Wrapper<DzRiskPrediction> queryWrapper);

    @Select("""
            SELECT ${ew.sqlSelect} 
            FROM dz_risk_prediction t1
                    LEFT JOIN data_slope_unit t2 ON t1.slope_unit_id = t2.id
            ${ew.customSqlSegment}
            """)
    List<StatRiskVo> statRisk(@Param("ew") QueryWrapper<DzRiskPrediction> ew);

    @Select("""
            SELECT ${ew.sqlSelect}
            FROM dz_risk_prediction t1
               LEFT JOIN data_slope_unit t2 ON t1.slope_unit_id = t2.id
            ${ew.customSqlSegment} 
            """)
    List<StatAreaVo> statArea(@Param("ew") QueryWrapper<DzRiskPrediction> ew);

    @Select("""
            SELECT u.street    AS name,
                   u.street_code AS code,
                   COUNT(p.id) AS risk_count
            FROM dz_risk_prediction p
                     JOIN
                 data_slope_unit u ON p.slope_unit_id = u.id
            WHERE p.type = #{type}
              AND p.prediction_date_str = #{predictionDate}
              AND p.risk_level IN (4, 5)          
            GROUP BY u.street, u.street_code
            ORDER BY risk_count DESC
            LIMIT 3;
            """)
    List<RiskChatBannerVo.RiskChatBannerItemVo> statAreaTop3(@Param("type") Integer type, @Param("predictionDate") String predictionDate);

    @Select("""
            select max(prediction_date)
            from dz_risk_prediction
            where type = #{type}
            """)
    Date selectLastByType(Integer type);
}
