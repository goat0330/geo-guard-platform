/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import java.util.Map;
public record DeviceStatusFetchResult(Map<String, ThirdPartyMonitorDeviceVo> byId, Map<String, ThirdPartyMonitorDeviceVo> byClientId) {}
