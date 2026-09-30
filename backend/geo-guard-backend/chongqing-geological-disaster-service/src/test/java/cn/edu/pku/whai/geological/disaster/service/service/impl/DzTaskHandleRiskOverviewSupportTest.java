/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzTaskHandleRiskOverviewSupportTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DefRespPlan.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DefRespPlan.class
            );
        }
        if (TableInfoHelper.getTableInfo(DzTaskHandle.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DzTaskHandle.class
            );
        }
    }

    @Test
    void riskOverviewTypeTwoCountsAffectedRegionRange() {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzDefRespPlanService defRespPlanService = mock(IDzDefRespPlanService.class);
        DzTaskHandlePermissionSupport permissionSupport = mock(DzTaskHandlePermissionSupport.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        when(permissionSupport.listCurrentUserRegions("无法查看风险概况")).thenReturn(List.of(region("恩施市", null)));
        when(defRespPlanService.eventCount(any(), any())).thenReturn(1L, 4L);
        when(defRespPlanMapper.selectList(any())).thenReturn(List.of(regionPlan("红土乡,屯堡乡")));
        when(slopeUnitService.querySlopeUnitListByStreets(any())).thenReturn(List.of(slope("001"), slope("002"), slope("003")));
        when(hazardPointService.countBySlopeUnitIds(any())).thenReturn(8L);
        DzTaskHandleRiskOverviewSupport support = new DzTaskHandleRiskOverviewSupport(
            taskHandleMapper,
            defRespPlanMapper,
            defRespPlanService,
            permissionSupport,
            slopeUnitService,
            hazardPointService
        );

        DzRiskOverviewVo vo = support.riskOverview(2);

        assertThat(vo.getUnstartedEventCount()).isEqualTo("1");
        assertThat(vo.getOngoingEventCount()).isEqualTo("4");
        assertThat(vo.getAffectedTownCount()).isEqualTo("2");
        assertThat(vo.getAffectedSlopeUnitCount()).isEqualTo("3");
        assertThat(vo.getAffectedHazardPointCount()).isEqualTo("8");
    }

    @Test
    void riskOverviewTypeTwoDeduplicatesAffectedRange() {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzDefRespPlanService defRespPlanService = mock(IDzDefRespPlanService.class);
        DzTaskHandlePermissionSupport permissionSupport = mock(DzTaskHandlePermissionSupport.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        when(permissionSupport.listCurrentUserRegions("无法查看风险概况")).thenReturn(List.of(region("恩施市", null)));
        when(defRespPlanService.eventCount(any(), any())).thenReturn(0L, 2L);
        when(defRespPlanMapper.selectList(any())).thenReturn(List.of(
            regionPlan("红土乡,屯堡乡"),
            regionPlan("屯堡乡,白杨坪镇")
        ));
        when(slopeUnitService.querySlopeUnitListByStreets(any())).thenReturn(List.of(
            slope("001"),
            slope("002"),
            slope("002"),
            slope("003")
        ));
        when(hazardPointService.countBySlopeUnitIds(any())).thenReturn(5L);
        DzTaskHandleRiskOverviewSupport support = new DzTaskHandleRiskOverviewSupport(
            taskHandleMapper,
            defRespPlanMapper,
            defRespPlanService,
            permissionSupport,
            slopeUnitService,
            hazardPointService
        );

        DzRiskOverviewVo vo = support.riskOverview(2);
        ArgumentCaptor<List<String>> slopeUnitIdsCaptor = ArgumentCaptor.forClass(List.class);

        verify(hazardPointService).countBySlopeUnitIds(slopeUnitIdsCaptor.capture());
        assertThat(vo.getAffectedTownCount()).isEqualTo("3");
        assertThat(vo.getAffectedSlopeUnitCount()).isEqualTo("3");
        assertThat(vo.getAffectedHazardPointCount()).isEqualTo("5");
        assertThat(slopeUnitIdsCaptor.getValue()).containsExactlyInAnyOrder("001", "002", "003");
    }

    @Test
    void riskOverviewTypeTwoReturnsZeroWhenNoRunningRegionPlan() {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzDefRespPlanService defRespPlanService = mock(IDzDefRespPlanService.class);
        DzTaskHandlePermissionSupport permissionSupport = mock(DzTaskHandlePermissionSupport.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        when(permissionSupport.listCurrentUserRegions("无法查看风险概况")).thenReturn(List.of(region("恩施市", null)));
        when(defRespPlanService.eventCount(any(), any())).thenReturn(0L, 0L);
        when(defRespPlanMapper.selectList(any())).thenReturn(List.of());
        DzTaskHandleRiskOverviewSupport support = new DzTaskHandleRiskOverviewSupport(
            taskHandleMapper,
            defRespPlanMapper,
            defRespPlanService,
            permissionSupport,
            slopeUnitService,
            hazardPointService
        );

        DzRiskOverviewVo vo = support.riskOverview(2);

        assertThat(vo.getAffectedTownCount()).isEqualTo("0");
        assertThat(vo.getAffectedSlopeUnitCount()).isEqualTo("0");
        assertThat(vo.getAffectedHazardPointCount()).isEqualTo("0");
        verify(hazardPointService, never()).countBySlopeUnitIds(any());
    }

    @Test
    void riskOverviewTypeOneKeepsOriginalCountAndNewFieldsStayZero() {
        DzTaskHandleMapper taskHandleMapper = mock(DzTaskHandleMapper.class);
        DzDefRespPlanMapper defRespPlanMapper = mock(DzDefRespPlanMapper.class);
        IDzDefRespPlanService defRespPlanService = mock(IDzDefRespPlanService.class);
        DzTaskHandlePermissionSupport permissionSupport = mock(DzTaskHandlePermissionSupport.class);
        ISlopeUnitService slopeUnitService = mock(ISlopeUnitService.class);
        IHazardPointService hazardPointService = mock(IHazardPointService.class);
        when(permissionSupport.listCurrentUserRegions("无法查看风险概况")).thenReturn(List.of(region("恩施市", null)));
        when(taskHandleMapper.selectCount(any())).thenReturn(2L, 5L);
        when(defRespPlanMapper.selectList(any())).thenReturn(List.of());
        DzTaskHandleRiskOverviewSupport support = new DzTaskHandleRiskOverviewSupport(
            taskHandleMapper,
            defRespPlanMapper,
            defRespPlanService,
            permissionSupport,
            slopeUnitService,
            hazardPointService
        );

        DzRiskOverviewVo vo = support.riskOverview(1);

        assertThat(vo.getUnstartedEventCount()).isEqualTo("2");
        assertThat(vo.getOngoingEventCount()).isEqualTo("5");
        assertThat(vo.getAffectedTownCount()).isEqualTo("0");
        assertThat(vo.getAffectedSlopeUnitCount()).isEqualTo("0");
        assertThat(vo.getAffectedHazardPointCount()).isEqualTo("0");
        verify(hazardPointService, never()).countBySlopeUnitIds(any());
    }

    @Test
    void riskOverviewThrowsWhenCurrentUserHasNoRegionPermission() {
        DzTaskHandleRiskOverviewSupport support = new DzTaskHandleRiskOverviewSupport(
            mock(DzTaskHandleMapper.class),
            mock(DzDefRespPlanMapper.class),
            mock(IDzDefRespPlanService.class),
            permissionSupportWithoutRegion(),
            mock(ISlopeUnitService.class),
            mock(IHazardPointService.class)
        );

        assertThatThrownBy(() -> support.riskOverview(2))
            .isInstanceOf(ServiceException.class)
            .hasMessage("当前用户未配置行政区划，无法查看风险概况");
    }

    private DzTaskHandlePermissionSupport permissionSupportWithoutRegion() {
        DzTaskHandlePermissionSupport permissionSupport = mock(DzTaskHandlePermissionSupport.class);
        when(permissionSupport.listCurrentUserRegions("无法查看风险概况")).thenReturn(List.of());
        return permissionSupport;
    }

    private AdRegionVo region(String county, String street) {
        AdRegionVo vo = new AdRegionVo();
        vo.setCounty(county);
        vo.setStreet(street);
        return vo;
    }

    private DefRespPlan regionPlan(String streets) {
        DefRespPlan plan = new DefRespPlan();
        plan.setCounty("恩施市");
        plan.setStreets(streets);
        plan.setLevel(2);
        plan.setStatus(DefRespPlanStatusEnum.TASK_PUBLISHED.getCode());
        plan.setRegionScopeType(RegionScopeTypeEnum.TOWN.getCode());
        return plan;
    }

    private SlopeUnitVo slope(String id) {
        SlopeUnitVo vo = new SlopeUnitVo();
        vo.setId(id);
        return vo;
    }
}
