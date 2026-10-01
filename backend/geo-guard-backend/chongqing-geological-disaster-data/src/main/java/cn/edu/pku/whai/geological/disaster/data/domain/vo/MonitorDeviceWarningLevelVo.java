/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监测设备最近报警等级
 */
@Data
public class MonitorDeviceWarningLevelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 最近报警等级原始值，可能为内部编码或三方编码
     */
    private String warningLevel;
}
