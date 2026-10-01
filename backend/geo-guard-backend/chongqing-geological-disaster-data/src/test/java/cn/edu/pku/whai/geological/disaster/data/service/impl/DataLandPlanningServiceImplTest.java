/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.PointLandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataLandPlanningMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class DataLandPlanningServiceImplTest {

    private static final double LON = 109.1d;
    private static final double LAT = 30.2d;

    @Mock
    private DataLandPlanningMapper baseMapper;

    @Test
    void queryPointLandPlanningTypeFallsBackToCategoryWhenLandNameIsBlank() {
        PointLandPlanningVo pointLandPlanning = new PointLandPlanningVo();
        pointLandPlanning.setLandName(" ");
        pointLandPlanning.setLandCategoryName("城镇建设用地");
        when(baseMapper.selectPointLandPlanning(LON, LAT)).thenReturn(pointLandPlanning);
        DataLandPlanningServiceImpl service = new DataLandPlanningServiceImpl(baseMapper);

        String result = service.queryPointLandPlanningType(LON, LAT);

        assertThat(result).isEqualTo("城镇建设用地");
    }
}
