/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.ShortTermTemporaryRainfallObservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface ShortTermTemporaryRainfallObservationMapper extends BaseMapperPlus<ShortTermTemporaryRainfallObservation, ShortTermTemporaryRainfallObservation> {

    @Insert("""
        INSERT INTO public.data_short_term_temporary_rainfall_observation
            (device_id, log_time, rainfall, cumulative_rainfall, create_time, update_time)
        VALUES
            (#{deviceId}, #{logTime}, #{rainfall}, #{cumulativeRainfall}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (device_id, log_time) DO UPDATE SET
            rainfall = EXCLUDED.rainfall,
            cumulative_rainfall = EXCLUDED.cumulative_rainfall,
            update_time = CURRENT_TIMESTAMP
        """)
    int upsert(ShortTermTemporaryRainfallObservation observation);

    @Select("""
        SELECT id, device_id, log_time, rainfall, cumulative_rainfall, create_time, update_time
        FROM public.data_short_term_temporary_rainfall_observation
        WHERE log_time = #{logTime}
        ORDER BY device_id
        """)
    List<ShortTermTemporaryRainfallObservation> selectByLogTime(@Param("logTime") LocalDateTime logTime);

    @Select("""
        SELECT id, device_id, log_time, rainfall, cumulative_rainfall, create_time, update_time
        FROM public.data_short_term_temporary_rainfall_observation
        WHERE device_id = #{deviceId}
          AND log_time < #{logTime}
          AND cumulative_rainfall IS NOT NULL
        ORDER BY log_time DESC
        LIMIT 1
        """)
    ShortTermTemporaryRainfallObservation selectLatestBefore(
        @Param("deviceId") String deviceId, @Param("logTime") LocalDateTime logTime);
}
