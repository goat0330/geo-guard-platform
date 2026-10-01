/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【请填写功能名称】Mapper接口
 *
 * @author kongweiguang
 * @date 2025-12-24
 */
public interface AdRegionMapper extends BaseMapperPlus<AdRegion, AdRegionVo> {

    List<AdRegionVo> selectTreeList(@Param("withWkt") Boolean withWkt);

    List<AdRegionVo> selectUserRegionList(@Param("userId") Long userId);

    Page<AdRegionVo> selectVoPage1(@Param("page") Page<AdRegion> page,
                                   @Param("ew") LambdaQueryWrapper<AdRegion> ew,
                                   @Param("simpWktLevel") Double simpWktLevel);

    AdRegionVo getUserRegion(Long userId);

    @Select("""
        SELECT "area"
        FROM public.data_ad_region
        WHERE id = #{regionCode}
        LIMIT 1
        """)
    BigDecimal selectAreaByRegionCode(@Param("regionCode") String regionCode);
}
