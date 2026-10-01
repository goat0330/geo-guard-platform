/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.ShortTermTemporaryRainfallForecast;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface ShortTermTemporaryRainfallForecastMapper extends BaseMapperPlus<ShortTermTemporaryRainfallForecast, ShortTermTemporaryRainfallForecast> {

    @Insert("""
        INSERT INTO public.data_short_term_temporary_rainfall_forecast
            (device_id, issue_time, forecast_time, period_hours, rainfall, create_time, update_time)
        VALUES
            (#{deviceId}, #{issueTime}, #{forecastTime}, #{periodHours}, #{rainfall}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (device_id, issue_time, period_hours) DO UPDATE SET
            forecast_time = EXCLUDED.forecast_time,
            rainfall = EXCLUDED.rainfall,
            update_time = CURRENT_TIMESTAMP
        """)
    int upsert(ShortTermTemporaryRainfallForecast forecast);

    @Select("""
        SELECT DISTINCT ON (device_id)
            id, device_id, issue_time, forecast_time, period_hours, rainfall, create_time, update_time
        FROM public.data_short_term_temporary_rainfall_forecast
        WHERE forecast_time = #{forecastTime}
        ORDER BY device_id, issue_time DESC
        """)
    List<ShortTermTemporaryRainfallForecast> selectLatestByForecastTime(@Param("forecastTime") LocalDateTime forecastTime);

    @Select("""
        SELECT DISTINCT ON (device_id)
            id, device_id, issue_time, forecast_time, period_hours, rainfall, create_time, update_time
        FROM public.data_short_term_temporary_rainfall_forecast
        WHERE forecast_time = #{forecastTime}
          AND period_hours = 24
        ORDER BY device_id, issue_time DESC
        """)
    List<ShortTermTemporaryRainfallForecast> selectLatest24HourByForecastTime(
        @Param("forecastTime") LocalDateTime forecastTime);
}
