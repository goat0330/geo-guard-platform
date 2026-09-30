/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.util.List;
public record JcdWarningFetchResult(String jcdId, List<ThirdMonitorDeviceWarningResp> warnings, long costMs) {}
