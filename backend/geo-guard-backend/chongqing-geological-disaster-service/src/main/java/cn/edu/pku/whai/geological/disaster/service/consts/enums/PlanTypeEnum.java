/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 计划类型枚举
 * 对应DzTaskDistList.planType字段及现场记录措施字段
 *
 * @author whai
 */
@Getter
@AllArgsConstructor
public enum PlanTypeEnum {

    RESETTLEMENT(1, "人员安置"),
    PROTECTION(2, "警示防护"),
    MONITORING(3, "监测巡查（群测群防）"),
    HAZARD_REMOVAL(4, "排危除险"),
    ENGINEERING(5, "工程治理"),
    PUBLICITY(6, "宣传告知"),
    TRAFFIC_CONTROL(7, "交通管制"),
    OTHER_SUGGESTIONS(8, "其他建议"),
    INSTRUMENT_MONITORING(9, "监测巡查（仪器监测）");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型名称
     */
    private final String name;

    private static final Map<Integer, PlanTypeEnum> CODE_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(PlanTypeEnum::getCode, Function.identity()));

    private static final Map<String, PlanTypeEnum> NAME_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(PlanTypeEnum::getName, Function.identity()));

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static PlanTypeEnum getByCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 名称
     * @return 枚举对象
     */
    public static PlanTypeEnum getByName(String name) {
        return name == null ? null : NAME_MAP.get(name);
    }

}
