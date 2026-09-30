/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistDayStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistStatSourceTypeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistStatStatusVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskStatisticsVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;

/**
 * 任务派发清单Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-08
     */
    public interface DzTaskDistListMapper extends BaseMapperPlus<DzTaskDistList, DzTaskDistListVo> {

    @Select("""
            SELECT status,
                   COUNT(*) AS count
            FROM dz_task_dist_list
            ${ew.customSqlSegment}
            """)
    List<TaskDistStatStatusVo> statStatus(@Param("ew") QueryWrapper<Object> ew);

    @Select("""
            SELECT source_type,
                   COUNT(*) AS count
            FROM dz_task_dist_list
            ${ew.customSqlSegment}
            """)
    List<TaskDistStatSourceTypeVo> statSourceType(@Param("ew") QueryWrapper<Object> ew);

    @Select("""
            SELECT COUNT(*) AS total_count
            FROM dz_task_dist_list;
            """)
    TaskStatisticsVo statPush();

    @Select("""
        <script>
        SELECT COUNT(*) AS total_count
        FROM dz_task_dist_list t1
                 JOIN data_slope_unit t2 ON t1.unit_id = t2.id
        WHERE 1=1
        <if test="startTime != null">
            AND t1.create_date &gt;= #{startTime}
        </if>
        <if test="endTime != null">
            AND t1.create_date &lt;= #{endTime}
        </if>
        <if test="bo != null and bo.province != null and bo.province != ''">
            AND t2.province = #{bo.province}
        </if>
        <if test="bo != null and bo.city != null and bo.city != ''">
            AND t2.city = #{bo.city}
        </if>
        <if test="bo != null and bo.county != null and bo.county != ''">
            AND t2.county = #{bo.county}
        </if>
        <if test="bo != null and bo.street != null and bo.street != ''">
            AND t2.street = #{bo.street}
        </if>
        <if test="bo != null and bo.village != null and bo.village != ''">
            AND t2.village = #{bo.village}
        </if>
        <if test="bo != null and bo.adRegionIds != null and bo.adRegionIds.size > 0">
            AND (
                t2.province_code IN
                <foreach collection="bo.adRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
                OR t2.city_code IN
                <foreach collection="bo.adRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
                OR t2.county_code IN
                <foreach collection="bo.adRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
                OR t2.street_code IN
                <foreach collection="bo.adRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
                OR t2.village_code IN
                <foreach collection="bo.adRegionIds" item="adRegionId" open="(" separator="," close=")">
                    #{adRegionId}
                </foreach>
            )
        </if>
        </script>
        """)
    TaskStatisticsVo statPushByRegion(@Param("bo") DzRiskAssessmentStatBo bo,
                                      @Param("startTime") Date startTime,
                                      @Param("endTime") Date endTime);

    @Select("""
            select t1.*
            from dz_task_dist_list t1
                     join data_slope_unit t2 on t1.unit_id = t2.id
            ${ew.customSqlSegment}
            """)
    List<DzTaskDistListVo> selectTaskDistList(@Param("ew") QueryWrapper<DzTaskDistList> ew);

    Page<DzTaskDistListVo> selectTaskDistPage(@Param("page") Page<DzTaskDistListVo> page,
                                              @Param(Constants.WRAPPER) Wrapper<DzTaskDistList> queryWrapper);

    List<DzTaskDistListVo> selectTaskDistListByQuery(@Param(Constants.WRAPPER) Wrapper<DzTaskDistList> queryWrapper);

    /**
     * 查询防御响应任务列表
     *
     * @param defId 防御响应方案 id
     * @return 任务列表
     */
    @Select("""
            SELECT *
            FROM dz_task_dist_list
            WHERE def_id = #{defId}
              AND "delete" = 0
            ORDER BY update_date DESC, plan_type ASC
            """)
    List<DzTaskDistListVo> selectDefRespTaskList(@Param("defId") Long defId);

    /**
     * 统计每日任务数量
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日统计列表
     */
    @Select("""
            SELECT TO_CHAR(create_date, 'YYYY-MM-DD') AS day,
                   COUNT(*)                           AS count
            FROM dz_task_dist_list
            WHERE status <= 3
              AND create_date >= #{startDate}
              AND create_date < #{endDate}
            GROUP BY TO_CHAR(create_date, 'YYYY-MM-DD')
            ORDER BY day DESC
            """)
    List<TaskDistDayStatVo> statDay(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
