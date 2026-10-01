/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskZone;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 风险区Mapper接口
 *
 * @author lizheng
 * @date 2026-01-10
 */
public interface RiskZoneMapper extends BaseMapperPlus<RiskZone, RiskZoneVo> {


    @Select("""
            select o_name ,
                   count(*) count
            from data_risk_zone
            group by o_name;
            """)
    List<RiskZoneStatVo> statRiskLevel();
}
