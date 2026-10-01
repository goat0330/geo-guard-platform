/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 三方监测设备查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyMonitorDeviceBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 监测点名称
     */
    private String monitorPointName;

    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 设备厂商
     */
    private String manufacturer;
}
