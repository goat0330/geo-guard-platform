/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import java.time.LocalDateTime;

/**
 * 短期临时雨量查询端口。
 */
public interface IShortTermTemporaryRainfallQueryService {

    Double tryInterpolateObservation(LocalDateTime logTime, double lon, double lat);

    Double tryInterpolate24HourForecast(LocalDateTime forecastTime, double lon, double lat);
}