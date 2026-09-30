/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReportDisasterStatVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

/**
 * 报灾管理Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
public interface DzReportDisasterMapper extends BaseMapperPlus<DzReportDisaster, DzReportDisasterVo> {

    Page<DzReportDisasterVo> selectListPage(@Param("page") Page<DzReportDisaster> page,
                                            @Param(Constants.WRAPPER) Wrapper<DzReportDisaster> queryWrapper);

    @Select("""
            SELECT COUNT(*)                                                                                              AS total_count,
                   COUNT(*) FILTER (WHERE ai_risk_level = 1)                                                             AS low_count,
                   COUNT(*) FILTER (WHERE ai_risk_level = 2)                                                             AS middle_count,
                   COUNT(*) FILTER (WHERE ai_risk_level = 3)                                                             AS high_count,
                   COUNT(*) FILTER (WHERE ai_risk_level = 4)                                                             AS very_high_count,
                   COUNT(*) FILTER (WHERE status = 1)                                                                    AS unHandle_count,
                   COUNT(*) FILTER (WHERE status = 2)                                                                    AS send_count,
                   COUNT(*) FILTER (WHERE status = 3)                                                                    AS handle_count,
                   COUNT(*) FILTER (WHERE create_date >= CURRENT_DATE)                                                   AS today_count,
                   COUNT(*) FILTER (WHERE create_date >= CURRENT_DATE - INTERVAL '1 day' AND create_date <  CURRENT_DATE)               AS yesterday_count
            FROM dz_report_disaster;
            """)
    ReportDisasterStatVo stat();

    /**
     * 按报灾记录主键加行锁查询，供处理报灾时串行化派单和流程链创建。
     *
     * @param id 报灾记录主键
     * @return 加锁后的报灾记录
     */
    @Select("""
            SELECT *
            FROM dz_report_disaster
            WHERE id = #{id}
            FOR UPDATE
            """)
    @ResultMap("DzReportDisasterResult")
    DzReportDisaster selectByIdForUpdate(@Param("id") Long id);

}
