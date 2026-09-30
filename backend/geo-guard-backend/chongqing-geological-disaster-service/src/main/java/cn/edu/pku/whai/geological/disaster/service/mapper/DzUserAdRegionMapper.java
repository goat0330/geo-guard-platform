/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzUserAdRegion;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户行政区划关联Mapper接口
 */
public interface DzUserAdRegionMapper extends BaseMapperPlus<DzUserAdRegion, DzUserAdRegionVo> {

    /**
     * 分页统计用户行政区划分布。
     *
     * @param page 分页参数
     * @param bo   查询条件
     * @return 用户行政区划分布统计分页
     */
    @Select("""
            <script>
            SELECT ad_region_id AS ad_region_id,
                   ad_region_name AS ad_region_name,
                   ad_region_level AS ad_region_level,
                   COUNT(user_id) AS user_count
            FROM dz_user_ad_region
            WHERE del_flag = '0'
            <if test="bo != null and bo.compatibleAdRegionIds != null and bo.compatibleAdRegionIds.size > 0">
                AND ad_region_id IN
                <foreach collection="bo.compatibleAdRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
            </if>
            <if test="bo != null and bo.adRegionName != null and bo.adRegionName != ''">
                AND ad_region_name LIKE CONCAT('%', #{bo.adRegionName}, '%')
            </if>
            <if test="bo != null and bo.adRegionLevel != null">
                AND ad_region_level = #{bo.adRegionLevel}
            </if>
            GROUP BY ad_region_id, ad_region_name, ad_region_level
            ORDER BY user_count DESC, ad_region_name ASC
            </script>
            """)
    Page<DzUserAdRegionStatVo> selectStatRegionPage(@Param("page") Page<DzUserAdRegionStatVo> page,
                                                    @Param("bo") DzUserAdRegionBo bo);
}
