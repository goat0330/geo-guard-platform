/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class PlanTypeEnumTest {

    @Test
    void monitoringPlanTypesUseSplitNamesAndDoNotMapLegacyName() {
        assertThat(PlanTypeEnum.getByCode(3)).isEqualTo(PlanTypeEnum.MONITORING);
        assertThat(PlanTypeEnum.getByCode(9)).isEqualTo(PlanTypeEnum.INSTRUMENT_MONITORING);
        assertThat(PlanTypeEnum.getByName("监测巡查（群测群防）")).isEqualTo(PlanTypeEnum.MONITORING);
        assertThat(PlanTypeEnum.getByName("监测巡查（仪器监测）")).isEqualTo(PlanTypeEnum.INSTRUMENT_MONITORING);
        assertThat(PlanTypeEnum.getByName("监测巡查")).isNull();
    }

}
