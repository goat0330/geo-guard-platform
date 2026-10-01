/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class HouseSlopeMapperTest {

    @Test
    void pointMatchQueriesUseHouseCimSourceAndKeepDedupRules() throws NoSuchMethodException {
        String bufferedSql = selectSql("selectBufferedPointMatches", Double.class, Double.class, Double.class);
        String exactSql = selectSql("selectExactPointMatches", Double.class, Double.class);
        String countSql = selectSql("countByPoint", Double.class, Double.class, Double.class);

        assertThat(bufferedSql)
            .contains("FROM public.data_house h")
            .contains("DISTINCT ON (COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id))")
            .contains("h.id AS houseId")
            .contains("h.house_unit_id AS houseUnitId")
            .contains("h.building_code AS buildingCode")
            .contains("h.building_name AS buildingName")
            .contains("h.geometry::geography")
            .contains("ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)")
            .contains("ST_Envelope")
            .contains("ST_Buffer")
            .contains("ST_DWithin");

        assertThat(exactSql)
            .contains("FROM public.data_house h")
            .contains("DISTINCT ON (COALESCE(NULLIF(h.building_code, ''), NULLIF(h.house_unit_id, ''), h.id))")
            .contains("h.geometry,")
            .contains("ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)")
            .contains("ST_Intersects")
            .doesNotContain("ST_SetSRID(h.geometry")
            .doesNotContain("v_house_slope");

        assertThat(countSql)
            .contains("vhs.house_geom::geography")
            .contains("ST_SetSRID(ST_MakePoint(#{longitude}::double precision, #{latitude}::double precision), 4326)")
            .doesNotContain("ST_SetSRID(vhs.house_geom");
    }

    private String selectSql(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = HouseSlopeMapper.class.getMethod(methodName, parameterTypes);
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        return String.join("\n", select.value());
    }
}
