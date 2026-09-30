/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import lombok.Data;

/**
 * 三方监测预警处置详情响应 DTO（从 ThirdPartyWarningDataSplitSupport 提取）。
 */
@Data
public class ThirdMonitorWarningHandleDetailResp {
    private String jcdid;
    private String clr;
    private String jcdyjid;
    private Integer sfwb;
    private Integer sfcylh;
    private String type;
    private String zhyp;
    private String clsj;
    private String yjdj;
    private String yjms;
}
