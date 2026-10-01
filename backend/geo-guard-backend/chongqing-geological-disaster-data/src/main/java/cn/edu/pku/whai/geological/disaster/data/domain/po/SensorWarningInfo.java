/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 传感器预警对象 v_sensor_warning_info
 **/
@Data
@TableName("v_sensor_warning_info")
public class SensorWarningInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 传感器预警记录唯一主键ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 传感器ID（关联传感器基础表）
     */
    private String sensorId;

    /**
     * 监测点预警ID（关联监测点预警记录表）
     */
    private String monitoringPointWarningId;

    /**
     * 设备ID（关联监测设备表）
     */
    private String deviceId;

    /**
     * 监测点ID（关联监测点基础表JC_BA10_JCD）
     */
    private String monitoringPointId;

    /**
     * 预警发生时间（含年月日时分秒）
     */
    private Date warningOccurTime;

    /**
     * 预警等级编码（如C1/C2/C3/C4等）
     */
    private String warningLevelCode;

    /**
     * 预警描述（预警原因、异常情况等详细说明）
     */
    private String warningDescription;


}
