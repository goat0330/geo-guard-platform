/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonitorStatVo {
    private Long monitorPointCount;
    private Long monitorDeviceCount;
    private Long monitorDeviceOnlineCount;
    private BigDecimal onlineRate;
}
