/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.NearestHouseCimVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class DisasterDetailedAddressResolverTest {

    @Test
    void resolveCheckCenterLocationShouldComposeNearestHouseDirectionAndDistance() {
        DataHouseMapper mapper = mock(DataHouseMapper.class);
        NearestHouseCimVo house = new NearestHouseCimVo();
        house.setBuildingName("湖北省恩施土家族苗族自治州恩施市屯堡乡木贡村大苞谷地组85号");
        house.setLongitude(new BigDecimal("109.2209606000"));
        house.setLatitude(new BigDecimal("30.3998889500"));
        when(mapper.selectNearestHouseByPoint(any(), any(), any())).thenReturn(house);

        DisasterDetailedAddressResolver resolver = new DisasterDetailedAddressResolver(mapper);

        assertThat(resolver.resolveCheckCenterLocation("POINT(109.22075655175031 30.399669964703445)"))
            .isEqualTo("屯堡乡木贡村大苞谷地组85号西南方向31米");
    }

    @Test
    void resolveCheckCenterLocationShouldFallbackWhenCoordinateIsInvalidOrNoHouseMatched() {
        DataHouseMapper mapper = mock(DataHouseMapper.class);
        when(mapper.selectNearestHouseByPoint(any(), any(), any())).thenReturn(null);

        DisasterDetailedAddressResolver resolver = new DisasterDetailedAddressResolver(mapper);

        assertThat(resolver.resolveCheckCenterLocation("bad-coordinate"))
            .isEqualTo(DisasterDetailedAddressResolver.CHECK_CENTER_LOCATION_FALLBACK);
        assertThat(resolver.resolveCheckCenterLocation("POINT(109.22075655175031 30.399669964703445)"))
            .isEqualTo(DisasterDetailedAddressResolver.CHECK_CENTER_LOCATION_FALLBACK);
    }

    @Test
    void composeDetailedAddressShouldTrimCityPrefixAndResolveEightDirections() {
        assertThat(DisasterDetailedAddressResolver.extractBuildingDisplayName(
            "湖北省恩施土家族苗族自治州恩施市屯堡乡大庙村大石包组103号"
        )).isEqualTo("屯堡乡大庙村大石包组103号");

        assertThat(DisasterDetailedAddressResolver.resolveDirection(109.0, 30.0, 109.01, 29.99))
            .isEqualTo("东南");
    }
}
