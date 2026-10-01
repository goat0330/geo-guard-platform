/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 三方监测设备树查询条件
 */
@Data
public class ThirdPartyMonitorDeviceTreeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 名称
     */
    private String name;

    /**
     * 查询类型：1=监测点名称，2=监测设备名称
     */
    private Integer type;
}
