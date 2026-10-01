/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SensorWarningInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 传感器预警业务对象 v_sensor_warning_info
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SensorWarningInfo.class, reverseConvertGenerate = false)
public class SensorWarningInfoBo extends BaseEntity {

    /**
     * 传感器预警记录唯一主键ID
     */

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
