/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorStatReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceStatByHazardVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceWarningLevelVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorStatVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 监测设备基本情况Mapper接口
 *
 * @author lizheng
 * @date 2026-01-06
 */
public interface MonitorDeviceMapper extends BaseMapperPlus<MonitorDevice, MonitorDeviceVo> {

    @Select("""
            select COUNT(*)                                     AS monitorDeviceCount,
                   COUNT(CASE WHEN is_enabled = '2' THEN 1 END) AS monitorDeviceOnlineCount
            from v_monitor_device
            where pilot_area_1 = 1
            """)
    MonitorStatVo statMonitor();

    @Select("""
            <script>
            SELECT COUNT(*) AS monitorDeviceCount,
                   COUNT(CASE WHEN ts.status_code = 1 THEN 1 END) AS monitorDeviceOnlineCount
            FROM v_monitor_device d
            LEFT JOIN data_tp_device_status_current ts ON ts.device_id = d.id
            <where>
                <if test="req != null and req.pilotArea1 != null">
                    d.pilot_area_1 = #{req.pilotArea1}
                </if>
                <if test="req != null and req.pilotArea2 != null">
                    AND d.pilot_area_2 = #{req.pilotArea2}
                </if>
            </where>
            </script>
            """)
    MonitorStatVo statMonitorByArea(@Param("req") MonitorStatReq req);

    @Select("""
            <script>
            SELECT *
            FROM v_monitor_device
            WHERE monitoring_point_id IN
            <foreach collection="monitoringPointIds" item="monitoringPointId" open="(" separator="," close=")">
                #{monitoringPointId}
            </foreach>
            ORDER BY monitoring_point_id, id
            </script>
            """)
    List<MonitorDeviceVo> selectVoListByMonitoringPointIds(@Param("monitoringPointIds") List<String> monitoringPointIds);

    @Select("""
            <script>
            SELECT latest.device_id AS deviceId,
                   latest.warning_level AS warningLevel
            FROM (
                SELECT device_id,
                       warning_level,
                       ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY warning_time DESC NULLS LAST, update_date DESC NULLS LAST, create_date DESC NULLS LAST) AS rn
                FROM data_tp_warning_event
                WHERE device_id IN
                <foreach collection="deviceIds" item="deviceId" open="(" separator="," close=")">
                    #{deviceId}
                </foreach>
            ) latest
            WHERE latest.rn = 1
            </script>
            """)
    List<MonitorDeviceWarningLevelVo> selectLatestWarningLevelByDeviceIds(@Param("deviceIds") List<String> deviceIds);

    /**
     * 按灾害点唯一编号（v_hazard_point.unique_disaster_id = v_monitor_point.basic_info_id）
     * 批量聚合统计其下监测点的专业监测设备台套、GNSS 台数、裂缝计台数。
     *
     * @param disasterIds 灾害点唯一编号列表
     * @return 每个灾害点一条统计记录
     */
    @Select("""
            <script>
            SELECT p.basic_info_id                                              AS disasterId,
                   COUNT(DISTINCT d.id) FILTER (
                       WHERE d.monitoring_type ~ '^(.*,)?(L1|L2)_'
                   )                                                             AS professionalDeviceTotal,
                   COUNT(DISTINCT d.id) FILTER (
                       WHERE d.monitoring_type LIKE '%L1_GP%'
                   )                                                             AS gnssCount,
                   COUNT(DISTINCT d.id) FILTER (
                       WHERE d.monitoring_type LIKE '%L1_LF%'
                   )                                                             AS crackMeterCount
            FROM v_monitor_point p
            LEFT JOIN v_monitor_device d ON d.monitoring_point_id = p.id
            WHERE p.basic_info_id IN
            <foreach collection="disasterIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            GROUP BY p.basic_info_id
            </script>
            """)
    List<MonitorDeviceStatByHazardVo> statByDisasterIds(@Param("disasterIds") List<String> disasterIds);

}
