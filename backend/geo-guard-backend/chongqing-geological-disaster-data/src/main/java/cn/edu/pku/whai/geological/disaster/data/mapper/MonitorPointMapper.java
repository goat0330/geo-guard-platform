/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorStatReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 监测点基本情况Mapper接口
 *
 * @author lizheng
 * @date 2026-01-06
 */
public interface MonitorPointMapper extends BaseMapperPlus<MonitorPoint, MonitorPointVo> {

    /**
     * 仅查询监测点主键与行政区划，供批量关联
     */
    @Select("SELECT id, administrative_region_code FROM v_monitor_point WHERE pilot_area_1 = 1")
    List<MonitorPoint> selectIdAndAdministrativeRegionCode();

    @Select("""
            <script>
            SELECT *
            FROM v_monitor_point
            WHERE monitor_name IN
            <foreach collection="monitorPointNames" item="monitorPointName" open="(" separator="," close=")">
                #{monitorPointName}
            </foreach>
            ORDER BY id
            </script>
            """)
    List<MonitorPointVo> selectVoListByMonitorNames(@Param("monitorPointNames") List<String> monitorPointNames);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM v_monitor_point
            <where>
                <if test="req != null and req.pilotArea1 != null">
                    pilot_area_1 = #{req.pilotArea1}
                </if>
                <if test="req != null and req.pilotArea2 != null">
                    AND pilot_area_2 = #{req.pilotArea2}
                </if>
            </where>
            </script>
            """)
    Long statCountByArea(@Param("req") MonitorStatReq req);
}
