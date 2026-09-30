/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 防御响应方案状态枚举
 * 对应DefRespPlan.status字段
 *
 * @author whai
 */
@Getter
@AllArgsConstructor
public enum DefRespPlanStatusEnum {

    UNSTARTED(0, "未启动"),
    STARTED(1, "方案生成"),
    MODEL_ANALYZED(2, "专家确认"),
    CONSULTATION_CONFIRMED(3, "行政审批"),
    APPROVAL_PASSED(4, "启动防御响应"),
    TASK_PUBLISHED(5, "任务生成与推送"),
    ENDED(6, "响应结束");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 流转中状态列表(1-5)
     */
    private static final List<Integer> STARTED_STATUS_CODES = Arrays.asList(1, 2, 3, 4, 5);

    /**
     * 未启动防御响应状态列表(1-3)
     */
    private static final List<Integer> UNDEFENSE_STARTED_STATUS_CODES = Arrays.asList(1, 2, 3);

    /**
     * 已启动防御响应状态列表(4-5)
     */
    private static final List<Integer> DEFENSE_STARTED_STATUS_CODES = Arrays.asList(4, 5);

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static DefRespPlanStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DefRespPlanStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 判断状态是否处于流转中(1-5)
     *
     * @param status 状态编码
     * @return 是否处于流转中
     */
    public static boolean isStarted(Integer status) {
        return status != null && STARTED_STATUS_CODES.contains(status);
    }

    /**
     * 获取流转中状态编码列表
     *
     * @return 状态编码列表
     */
    public static List<Integer> getStartedStatusCodes() {
        return STARTED_STATUS_CODES;
    }

    /**
     * 判断是否已进入防御响应启动前的执行阶段(1-3)
     *
     * @param status 状态编码
     * @return 是否已启动防御响应
     */
    public static boolean isUnDefenseStarted(Integer status) {
        return status != null && UNDEFENSE_STARTED_STATUS_CODES.contains(status);
    }

    /**
     * 获取未启动防御响应状态编码列表
     *
     * @return 状态编码列表
     */
    public static List<Integer> getUnDefenseStartedStatusCodes() {
        return UNDEFENSE_STARTED_STATUS_CODES;
    }

    /**
     * 判断是否已进入防御响应启动后的执行阶段(4-5)
     *
     * @param status 状态编码
     * @return 是否已启动防御响应
     */
    public static boolean isDefenseStarted(Integer status) {
        return status != null && DEFENSE_STARTED_STATUS_CODES.contains(status);
    }

    /**
     * 获取已启动防御响应状态编码列表
     *
     * @return 状态编码列表
     */
    public static List<Integer> getDefenseStartedStatusCodes() {
        return DEFENSE_STARTED_STATUS_CODES;
    }
}
