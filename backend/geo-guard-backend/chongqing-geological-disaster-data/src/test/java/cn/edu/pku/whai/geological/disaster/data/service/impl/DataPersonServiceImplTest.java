package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class DataPersonServiceImplTest {
    @Test
    void omittedAreaTypeCountsLocalRecordsWithoutPilotFilter() {
        DataPersonMapper people = mock(DataPersonMapper.class);
        DataHouseMapper houses = mock(DataHouseMapper.class);
        SlopeUnitMapper slopes = mock(SlopeUnitMapper.class);
        when(people.selectCount(any())).thenReturn(7L);
        when(houses.selectCount(any())).thenReturn(3L);
        when(slopes.selectCount(any())).thenReturn(2L);
        DataPersonServiceImpl service = new DataPersonServiceImpl(people, houses, mock(AdRegionMapper.class), slopes);

        var result = service.statPopulationAndHouse(null, null, null, "500101", null, null);

        assertThat(result).containsEntry("personCount", 7L).containsEntry("houseCount", 3L)
            .containsEntry("slopeUnitCount", 2L);
        ArgumentCaptor<QueryWrapper> query = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(slopes).selectCount(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("county_code").doesNotContain("pilot_area");
        assertThat(query.getValue().getParamNameValuePairs().values()).contains("500101");
    }
}
