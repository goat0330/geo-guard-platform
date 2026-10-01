/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 三方设备监测曲线响应
 */
@Data
public class ThirdPartyDeviceCureVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;

    /**
     * 列定义
     */
    private List<ColumnInfo> columns;

    /**
     * 数据值，首列对应 source_update_time 的北京时间毫秒时间戳
     */
    private List<List<Object>> values;

    @Data
    public static class DeviceInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @JsonProperty("clientID")
        private String clientId;
        private String monitorPointId;
        private String monitorMethod;
        private String monitorType;
        private String dicName;
        private String sensorName;
        private String monitorSerialNo;
    }

    @Data
    public static class ColumnInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String code;
        private String name;
        private String unit;
    }
}
