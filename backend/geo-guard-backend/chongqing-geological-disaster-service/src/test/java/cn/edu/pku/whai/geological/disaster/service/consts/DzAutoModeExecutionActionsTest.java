/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class DzAutoModeExecutionActionsTest {

    @Test
    void defaultNameCoversKeyAutoModeActions() {
        assertThat(AutoModeExecutionActions.defaultName(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_CODE))
            .isEqualTo(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_NAME);
        assertThat(AutoModeExecutionActions.defaultName(AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_CODE))
            .isEqualTo(AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_NAME);
        assertThat(AutoModeExecutionActions.defaultName(AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_CODE))
            .isEqualTo(AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_NAME);
        assertThat(AutoModeExecutionActions.defaultName(AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_CODE))
            .isEqualTo(AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_NAME);
        assertThat(AutoModeExecutionActions.defaultName(AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH))
            .isEqualTo(AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_NAME);
        assertThat(AutoModeExecutionActions.defaultName("custom_action")).isEqualTo("custom_action");
    }

    @Test
    void resolveCodeSupportsLegacySlugAndNumericText() {
        assertThat(AutoModeExecutionActions.resolveCode(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE))
            .isEqualTo(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_CODE);
        assertThat(AutoModeExecutionActions.resolveCode("6"))
            .isEqualTo(AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_CODE);
        assertThat(AutoModeExecutionActions.resolveCode("unknown")).isNull();
    }

    @Test
    void autoDispatchActionTypesContainDispatchOnlyActions() {
        assertThat(AutoModeExecutionActions.AUTO_DISPATCH_ACTION_TYPES)
            .contains(
                AutoModeExecutionActions.TASK_UNPUSHED_AUTO_PUSH_CODE,
                AutoModeExecutionActions.TASK_DAILY_PATROL_AUTO_PUSH_CODE,
                AutoModeExecutionActions.REPORT_TASK_AUTO_PUSH_CODE
            )
            .doesNotContain(
                AutoModeExecutionActions.REPORT_AUTO_HANDLE_CODE,
                AutoModeExecutionActions.RISK_PREDICTION_AUTO_PUSH_CODE,
                AutoModeExecutionActions.DAILY_PATROL_GENERATE_CODE
            );
    }
}
