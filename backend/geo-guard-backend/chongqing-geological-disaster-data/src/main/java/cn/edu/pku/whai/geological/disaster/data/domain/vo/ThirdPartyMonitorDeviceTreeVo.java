/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 三方监测设备树响应
 */
@Data
public class ThirdPartyMonitorDeviceTreeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备ID
     */
    private String id;

    /**
     * 监测器列表
     */
    private List<MonitorItem> monitor;

    @Data
    public static class MonitorItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 监测器名称
         */
        private String name;

        /**
         * 监测器类型
         */
        private String type;
    }
}
