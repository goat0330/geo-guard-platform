/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.util.Map;
public record LocalWarningLookup(Map<String, DeviceLatestWarningInfo> byDeviceId, Map<String, DeviceLatestWarningInfo> byClientId, Map<String, DeviceLatestWarningInfo> byPointId) {}
