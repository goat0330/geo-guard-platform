/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 按灾害点ID聚合的监测设备统计
 *
 * @author zhuzc
 * @date 2026-06-11
 */
@Data
public class MonitorDeviceStatByHazardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 灾害点唯一编号（v_hazard_point.unique_disaster_id = v_monitor_point.basic_info_id）
     */
    private String disasterId;

    /**
     * 专业监测设备总台套（变形监测L1 + 物理场监测L2 设备合计）
     */
    private Long professionalDeviceTotal;

    /**
     * GNSS 设备台数（monitoring_type 含 L1_GP）
     */
    private Long gnssCount;

    /**
     * 裂缝计台数（monitoring_type 含 L1_LF）
     */
    private Long crackMeterCount;
}
