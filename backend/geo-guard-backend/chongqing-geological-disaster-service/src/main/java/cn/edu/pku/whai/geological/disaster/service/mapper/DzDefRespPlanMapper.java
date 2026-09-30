/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownScopeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownStatItemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DzDefRespPlanMapper extends BaseMapperPlus<DefRespPlan, DefRespPlanVo> {

    List<DefRespTownStatItemVo> selectCirculatingTownStats(@Param("parentDefId") Long parentDefId,
                                                           @Param("filterByParent") boolean filterByParent);

    List<DefRespTownScopeVo> selectCirculatingTownScopes(@Param("parentDefId") Long parentDefId,
                                                         @Param("filterByParent") boolean filterByParent);

    Integer selectCirculatingCountyLevel();
}
