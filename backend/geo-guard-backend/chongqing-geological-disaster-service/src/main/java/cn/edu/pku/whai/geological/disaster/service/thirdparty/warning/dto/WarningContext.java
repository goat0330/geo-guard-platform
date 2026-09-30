/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
public record WarningContext(ThirdMonitorPointResp monitorPointResp, ThirdMonitorDeviceWarningResp warningResp, MonitorPoint localMonitorPoint) {
    public String warningId() { return warningResp == null ? null : warningResp.getJcdyjid(); }
}
