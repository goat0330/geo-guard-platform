/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.mapper.HazardPointMapper;
import cn.edu.pku.whai.geological.disaster.data.props.DisasterPreventionPlatformProps;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class HazardPointServiceImplTest {

    @Test
    void countBySlopeUnitIdsReturnsZeroWhenInputIsEmpty() {
        HazardPointMapper mapper = mock(HazardPointMapper.class);
        HazardPointServiceImpl service = new HazardPointServiceImpl(mapper, mock(DisasterPreventionPlatformProps.class));

        Long count = service.countBySlopeUnitIds(List.of());

        assertThat(count).isEqualTo(0L);
        verify(mapper, never()).countBySlopeUnitIds(anyList());
    }

    @Test
    void countBySlopeUnitIdsDelegatesToMapper() {
        HazardPointMapper mapper = mock(HazardPointMapper.class);
        when(mapper.countBySlopeUnitIds(List.of("001", "002"))).thenReturn(4L);
        HazardPointServiceImpl service = new HazardPointServiceImpl(mapper, mock(DisasterPreventionPlatformProps.class));

        Long count = service.countBySlopeUnitIds(List.of("001", "002"));

        assertThat(count).isEqualTo(4L);
        verify(mapper).countBySlopeUnitIds(List.of("001", "002"));
    }
}
