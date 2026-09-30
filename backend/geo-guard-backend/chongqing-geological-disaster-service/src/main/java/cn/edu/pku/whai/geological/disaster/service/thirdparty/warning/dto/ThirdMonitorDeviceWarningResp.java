/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import lombok.Data;

/**
 * 三方监测设备预警响应 DTO（从 ThirdPartyWarningDataSplitSupport 提取）。
 */
@Data
public class ThirdMonitorDeviceWarningResp {
    private String addvcd;
    private String yjsj;
    private String jcdid;
    private String jcdbh;
    private String jcdname;
    private String jcdyjid;
    private String type;
    private String sbid;
    private String sbname;
    private String yjdj;
    private String clr;
    private String clsj;
    private Integer sfwb;
    private String czzt;
    private Double lon;
    private Double lat;
}
