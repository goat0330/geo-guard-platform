/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lizheng
 * @date 2026-01-06
 */
@Data
@AutoMapper(target = MonitorDevice.class, reverseConvertGenerate = false)
public class MonitorDeviceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @JsonProperty("sbid")
    private String id;

    /**
     * 设备ID (clientID)
     */
    @JsonProperty("clientid")
    private String clientId;

    /**
     * 监测点编号
     */
    @JsonProperty("jcdbh")
    private String monitorPointCode;

    /**
     * 监测点id
     */
    @JsonProperty("jcdid")
    private String monitorPointId;

    /**
     * 监测类型
     */
    @JsonProperty("jclx")
    private String monitorType;

    /**
     * 纬度
     */
    @JsonProperty("lat")
    private Double latitude;

    /**
     * 经度
     */
    @JsonProperty("lon")
    private Double longitude;

    /**
     * 设备名称
     */
    @JsonProperty("sbmc")
    private String deviceName;

    /**
     * 是否故障 (0-正常, 1-故障)
     */
    @JsonProperty("sfgz")
    private String isFault;
}
