/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import lombok.Data;
import java.util.List;

/**
 * 三方监测设备预警分页响应 DTO（从 ThirdPartyWarningDataSplitSupport 提取）。
 */
@Data
public class ThirdMonitorDeviceWarningPageResp {
    private List<ThirdMonitorDeviceWarningResp> records;
    private Integer total;
    private Integer size;
    private Integer current;
    private Integer pages;
}
