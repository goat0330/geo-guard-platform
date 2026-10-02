/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRiskVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzReportDisasterMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzRiskAssessmentServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DzTaskHandle.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DzTaskHandle.class
            );
        }
    }

    @Test
    void countHandlingTasksUsesTaskListEntryProcessScope() throws Exception {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        when(taskHandleMapper.selectCount(any())).thenReturn(10L);
        DzRiskAssessmentServiceImpl service = new DzRiskAssessmentServiceImpl(
            mock(DzRiskAssessmentMapper.class),
            mock(ISlopeUnitService.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            taskHandleMapper,
            mock(IDzUserAdRegionService.class),
            mock(AdRegionMapper.class)
        );

        Long count = invokeCountHandlingTasks(service, statBo("恩施市"));

        ArgumentCaptor<LambdaQueryWrapper<DzTaskHandle>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskHandleMapper).selectCount(captor.capture());
        LambdaQueryWrapper<DzTaskHandle> wrapper = captor.getValue();
        assertThat(count).isEqualTo(10L);
        assertThat(wrapper.getSqlSegment()).contains("handle_process IN");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1, 2, "恩施市");
    }

    @Test
    void reportRegionSqlUsesGeometryAdRegionTable() throws Exception {
        DzRiskAssessmentServiceImpl service = new DzRiskAssessmentServiceImpl(
            mock(DzRiskAssessmentMapper.class),
            mock(ISlopeUnitService.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(IDzUserAdRegionService.class),
            mock(AdRegionMapper.class)
        );

        Method method = DzRiskAssessmentServiceImpl.class.getDeclaredMethod(
            "buildReportRegionIdsExistsSql",
            java.util.List.class
        );
        method.setAccessible(true);
        String sql = (String) method.invoke(service, java.util.List.of("422801"));

        assertThat(sql).contains("from data_ad_region ar");
        assertThat(sql).contains("ST_GeomFromText(check_center, 4326)");
        assertThat(sql).contains("ST_Contains(ar.geom, ST_GeomFromText(check_center, 4326))");
        assertThat(sql).doesNotContain("from data.data_ad_region ar");
        assertThat(sql).doesNotContain("ST_SetSRID(ar.geom");
    }

    @Test
    void reportRegionFilterSqlPreservesAdRegionSrid() throws Exception {
        DzRiskAssessmentServiceImpl service = new DzRiskAssessmentServiceImpl(
            mock(DzRiskAssessmentMapper.class),
            mock(ISlopeUnitService.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(IDzUserAdRegionService.class),
            mock(AdRegionMapper.class)
        );

        Method method = DzRiskAssessmentServiceImpl.class.getDeclaredMethod(
            "buildReportRegionExistsSql",
            DzRiskAssessmentStatBo.class
        );
        method.setAccessible(true);
        String sql = (String) method.invoke(service, statBo("恩施市"));

        assertThat(sql).contains("ST_Contains(ar.geom, ST_GeomFromText(check_center, 4326))");
        assertThat(sql).doesNotContain("ST_SetSRID(ar.geom");
    }

    @Test
    void statIncludesHazardPointCountByRiskLevel() {
        assertHazardPointStat(0);
    }

    @Test
    void omittedOffsetUsesCurrentDayStatistics() {
        assertHazardPointStat(null);
    }

    private void assertHazardPointStat(Integer offset) {
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);
        when(riskAssessmentMapper.statRisk(any(), any(), any())).thenReturn(List.of());
        when(riskAssessmentMapper.statArea(any(), any(), any())).thenReturn(List.of());
        when(riskAssessmentMapper.statAttribution(any(), any(), any())).thenReturn(List.of());
        when(riskAssessmentMapper.statMonitDevice(any(), any(), any())).thenReturn(List.of());
        StatRiskVo hazardPointStat = new StatRiskVo();
        hazardPointStat.setDynamicRiskLevel(3);
        hazardPointStat.setCount(2);
        when(riskAssessmentMapper.statHazardPoint(any(), any(), any())).thenReturn(List.of(hazardPointStat));

        DzRiskAssessmentServiceImpl service = new DzRiskAssessmentServiceImpl(
            riskAssessmentMapper,
            mock(ISlopeUnitService.class),
            mock(DzTaskDistListMapper.class),
            mock(DzReportDisasterMapper.class),
            mock(DzTaskHandleMapper.class),
            mock(IDzUserAdRegionService.class),
            mock(AdRegionMapper.class)
        );
        DzRiskAssessmentStatBo bo = new DzRiskAssessmentStatBo();
        bo.setOffset(offset);

        assertThat(service.stat(bo).getStatHazardPoint()).containsExactly(hazardPointStat);
    }

    private Long invokeCountHandlingTasks(DzRiskAssessmentServiceImpl service, DzRiskAssessmentStatBo bo) throws Exception {
        Method method = DzRiskAssessmentServiceImpl.class.getDeclaredMethod(
            "countHandlingTasks",
            DzRiskAssessmentStatBo.class,
            java.util.Date.class,
            java.util.Date.class
        );
        method.setAccessible(true);
        return (Long) method.invoke(service, bo, null, null);
    }

    private DzRiskAssessmentStatBo statBo(String county) {
        DzRiskAssessmentStatBo bo = new DzRiskAssessmentStatBo();
        bo.setCounty(county);
        return bo;
    }
}
