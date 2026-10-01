/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.ShortTermTemporaryRainfallForecast;
import cn.edu.pku.whai.geological.disaster.data.domain.po.ShortTermTemporaryRainfallObservation;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ShortTermTemporaryRainfallMapperContractTest {

    @Test
    void observationUpsertUsesBusinessUniqueKey() throws Exception {
        String sql = insertSql(ShortTermTemporaryRainfallObservationMapper.class,
            ShortTermTemporaryRainfallObservation.class);

        assertThat(sql).contains("on conflict (device_id, log_time) do update");
    }

    @Test
    void forecastUpsertUsesBusinessUniqueKey() throws Exception {
        String sql = insertSql(ShortTermTemporaryRainfallForecastMapper.class,
            ShortTermTemporaryRainfallForecast.class);

        assertThat(sql).contains("on conflict (device_id, issue_time, period_hours) do update");
    }

    private static String insertSql(Class<?> mapperClass, Class<?> entityClass) throws Exception {
        Method upsertMethod = mapperClass.getDeclaredMethod("upsert", entityClass);
        Insert insert = upsertMethod.getAnnotation(Insert.class);
        return String.join("\n", insert.value()).toLowerCase();
    }
}
