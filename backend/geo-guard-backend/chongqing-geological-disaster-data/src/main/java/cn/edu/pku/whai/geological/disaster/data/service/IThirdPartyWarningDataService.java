/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import java.util.Map;

/**
 * 对接第三方气象风险预警预警接口
 */
public interface IThirdPartyWarningDataService extends
    IThirdPartyWeatherWarningService,
    IThirdPartyMonitorWarningService,
    IThirdPartyMonitorDeviceService,
    IThirdPartyDeviceCurveQueryService {

    /**
     * 检查三方接口连通性
     */
    Map<String, Object> checkThirdPartyConnectivity();
}
