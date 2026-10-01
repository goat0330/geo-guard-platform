/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 防御响应方案只读查询 Mapper。
 */
public interface DzDefRespPlanReadMapper {

    /**
     * 查询指定时间范围内已结束的县级区域防御响应关联的预警 ID。
     *
     * @param regionType       方案类型：区域
     * @param countyScopeType  区域层级：县级
     * @param endedStatus      流程状态：响应结束
     * @param updateDateStart  更新时间下限（含）
     * @param updateDateEnd    更新时间上限（含）
     * @return 来源预警 ID 列表
     */
    @Select("""
        SELECT source_alarm_id
        FROM dz_def_resp_plan
        WHERE deleted = 0
          AND type = #{regionType}
          AND region_scope_type = #{countyScopeType}
          AND status = #{endedStatus}
          AND source_alarm_id IS NOT NULL
          AND update_date >= #{updateDateStart}
          AND update_date <= #{updateDateEnd}
        """)
    List<Long> listEndedCountySourceAlarmIds(@Param("regionType") Integer regionType,
                                             @Param("countyScopeType") Integer countyScopeType,
                                             @Param("endedStatus") Integer endedStatus,
                                             @Param("updateDateStart") Date updateDateStart,
                                             @Param("updateDateEnd") Date updateDateEnd);
}
