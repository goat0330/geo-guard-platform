/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts;

import org.dromara.common.core.utils.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 自动模式执行留痕动作常量。
 */
public final class AutoModeExecutionActions {

    public static final String DISPOSAL_AUTO_ADVANCE = "disposal_auto_advance";
    public static final String TASK_UNPUSHED_AUTO_PUSH = "task_unpushed_auto_push";
    public static final String TASK_DAILY_PATROL_AUTO_PUSH = "task_daily_patrol_auto_push";
    public static final String TASK_AUTO_REMIND = "task_auto_remind";
    public static final String REPORT_AUTO_HANDLE = "report_auto_handle";
    public static final String REPORT_TASK_AUTO_PUSH = "report_task_auto_push";
    public static final String RISK_PREDICTION_AUTO_PUSH = "risk_prediction_auto_push";
    public static final String DAILY_PATROL_GENERATE = "daily_patrol_generate";

    public static final Integer DISPOSAL_AUTO_ADVANCE_CODE = 1;
    public static final Integer TASK_UNPUSHED_AUTO_PUSH_CODE = 2;
    public static final Integer TASK_DAILY_PATROL_AUTO_PUSH_CODE = 3;
    public static final Integer TASK_AUTO_REMIND_CODE = 4;
    public static final Integer REPORT_AUTO_HANDLE_CODE = 5;
    public static final Integer REPORT_TASK_AUTO_PUSH_CODE = 6;
    public static final Integer RISK_PREDICTION_AUTO_PUSH_CODE = 7;
    public static final Integer DAILY_PATROL_GENERATE_CODE = 8;

    public static final String DISPOSAL_AUTO_ADVANCE_NAME = "处置自动推进";
    public static final String TASK_UNPUSHED_AUTO_PUSH_NAME = "未推送任务自动推送";
    public static final String TASK_DAILY_PATROL_AUTO_PUSH_NAME = "日常巡逻自动推送";
    public static final String TASK_AUTO_REMIND_NAME = "自动催办";
    public static final String REPORT_AUTO_HANDLE_NAME = "报灾自动处理";
    public static final String REPORT_TASK_AUTO_PUSH_NAME = "报灾任务推送";
    public static final String RISK_PREDICTION_AUTO_PUSH_NAME = "风险预测推送";
    public static final String DAILY_PATROL_GENERATE_NAME = "日常巡逻生成";

    public static final String SOURCE_NAME = "自动模式留痕";

    private static final Map<Integer, String> ACTION_NAME_MAP = Map.of(
        DISPOSAL_AUTO_ADVANCE_CODE, DISPOSAL_AUTO_ADVANCE_NAME,
        TASK_UNPUSHED_AUTO_PUSH_CODE, TASK_UNPUSHED_AUTO_PUSH_NAME,
        TASK_DAILY_PATROL_AUTO_PUSH_CODE, TASK_DAILY_PATROL_AUTO_PUSH_NAME,
        TASK_AUTO_REMIND_CODE, TASK_AUTO_REMIND_NAME,
        REPORT_AUTO_HANDLE_CODE, REPORT_AUTO_HANDLE_NAME,
        REPORT_TASK_AUTO_PUSH_CODE, REPORT_TASK_AUTO_PUSH_NAME,
        RISK_PREDICTION_AUTO_PUSH_CODE, RISK_PREDICTION_AUTO_PUSH_NAME,
        DAILY_PATROL_GENERATE_CODE, DAILY_PATROL_GENERATE_NAME
    );

    private static final Map<Integer, String> ACTION_SLUG_MAP = Map.of(
        DISPOSAL_AUTO_ADVANCE_CODE, DISPOSAL_AUTO_ADVANCE,
        TASK_UNPUSHED_AUTO_PUSH_CODE, TASK_UNPUSHED_AUTO_PUSH,
        TASK_DAILY_PATROL_AUTO_PUSH_CODE, TASK_DAILY_PATROL_AUTO_PUSH,
        TASK_AUTO_REMIND_CODE, TASK_AUTO_REMIND,
        REPORT_AUTO_HANDLE_CODE, REPORT_AUTO_HANDLE,
        REPORT_TASK_AUTO_PUSH_CODE, REPORT_TASK_AUTO_PUSH,
        RISK_PREDICTION_AUTO_PUSH_CODE, RISK_PREDICTION_AUTO_PUSH,
        DAILY_PATROL_GENERATE_CODE, DAILY_PATROL_GENERATE
    );

    private static final Map<String, Integer> LEGACY_ACTION_CODE_MAP = Map.of(
        DISPOSAL_AUTO_ADVANCE, DISPOSAL_AUTO_ADVANCE_CODE,
        TASK_UNPUSHED_AUTO_PUSH, TASK_UNPUSHED_AUTO_PUSH_CODE,
        TASK_DAILY_PATROL_AUTO_PUSH, TASK_DAILY_PATROL_AUTO_PUSH_CODE,
        TASK_AUTO_REMIND, TASK_AUTO_REMIND_CODE,
        REPORT_AUTO_HANDLE, REPORT_AUTO_HANDLE_CODE,
        REPORT_TASK_AUTO_PUSH, REPORT_TASK_AUTO_PUSH_CODE,
        RISK_PREDICTION_AUTO_PUSH, RISK_PREDICTION_AUTO_PUSH_CODE,
        DAILY_PATROL_GENERATE, DAILY_PATROL_GENERATE_CODE
    );

    public static final Set<Integer> AUTO_DISPATCH_ACTION_TYPES = Set.of(
        TASK_UNPUSHED_AUTO_PUSH_CODE,
        TASK_DAILY_PATROL_AUTO_PUSH_CODE,
        REPORT_TASK_AUTO_PUSH_CODE
    );

    private AutoModeExecutionActions() {
    }

    public static Integer resolveCode(Integer actionType) {
        return actionType;
    }

    public static Integer resolveCode(String actionType) {
        if (StringUtils.isBlank(actionType)) {
            return null;
        }
        String normalized = actionType.trim();
        Integer legacyCode = LEGACY_ACTION_CODE_MAP.get(normalized);
        if (legacyCode != null) {
            return legacyCode;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String defaultName(Integer actionType) {
        if (actionType == null) {
            return null;
        }
        return ACTION_NAME_MAP.getOrDefault(actionType, String.valueOf(actionType));
    }

    public static String defaultName(String actionType) {
        Integer code = resolveCode(actionType);
        return code == null ? actionType : defaultName(code);
    }

    public static String legacySlug(Integer actionType) {
        if (actionType == null) {
            return null;
        }
        return ACTION_SLUG_MAP.get(actionType);
    }
}
