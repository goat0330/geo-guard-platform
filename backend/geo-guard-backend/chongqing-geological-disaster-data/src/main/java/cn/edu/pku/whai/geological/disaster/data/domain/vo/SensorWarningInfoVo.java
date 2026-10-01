/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.SensorWarningInfo;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 传感器预警视图对象 v_sensor_warning_info
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SensorWarningInfo.class)
public class SensorWarningInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 传感器预警记录唯一主键ID
     */
    @ExcelProperty(value = "传感器预警记录唯一主键ID")
    private String id;

    /**
     * 传感器ID（关联传感器基础表）
     */
    @ExcelProperty(value = "传感器ID")

    private String sensorId;

    /**
     * 监测点预警ID（关联监测点预警记录表）
     */
    @ExcelProperty(value = "监测点预警ID")

    private String monitoringPointWarningId;

    /**
     * 设备ID（关联监测设备表）
     */
    @ExcelProperty(value = "设备ID")

    private String deviceId;

    /**
     * 监测点ID（关联监测点基础表JC_BA10_JCD）
     */
    @ExcelProperty(value = "监测点ID")

    private String monitoringPointId;

    /**
     * 预警发生时间（含年月日时分秒）
     */
    @ExcelProperty(value = "预警发生时间")

    private Date warningOccurTime;

    /**
     * 预警等级编码（如C1/C2/C3/C4等）
     */
    @ExcelProperty(value = "预警等级编码")

    private String warningLevelCode;

    /**
     * 预警描述（预警原因、异常情况等详细说明）
     */
    @ExcelProperty(value = "预警描述")

    private String warningDescription;


}
