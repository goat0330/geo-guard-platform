/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 三方设备曲线监测类型字典
 */
@Getter
@RequiredArgsConstructor
public enum ThirdPartyCurveTypeEnum {

    JS("JS", "加速度"),
    QJ("QJ", "倾角"),
    DX("DX", "地下水温/水位"),
    NW("NW", "泥水位"),
    JZ("JZ", "JZ"),
    LB("LB", "预警喇叭"),
    LD("LD", "雷达"),
    QY("QY", "气压"),
    CJ("CJ", "沉降"),
    LS("LS", "流速"),
    ST("ST", "渗透压力"),
    HS("HS", "土壤含水率"),
    TW("TW", "土壤温度"),
    QW("QW", "气温"),
    YL("YL", "雨量、应力"),
    DS("DS", "地声"),
    CS("CS", "次声"),
    TY("TY", "土压力"),
    ZD("ZD", "振动"),
    SW("SW", "深部位移"),
    GP("GP", "地表位移"),
    LF("LF", "裂缝");

    private final String dicCode;
    private final String dicName;

    public static final Map<String, ThirdPartyCurveTypeEnum> CODE_MAP =
        CollStreamUtil.toIdentityMap(Arrays.asList(values()), item -> item.dicCode);

    public static Optional<ThirdPartyCurveTypeEnum> fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(StrUtil.trim(code).toUpperCase()));
    }

    public static boolean containsCode(String code) {
        return fromCode(code).isPresent();
    }

    public static String resolveDicName(String code) {
        return fromCode(code)
            .map(ThirdPartyCurveTypeEnum::getDicName)
            .orElse(code);
    }

    public static Set<String> supportedCodes() {
        return Arrays.stream(values())
                     .map(ThirdPartyCurveTypeEnum::getDicCode)
                     .collect(Collectors.toUnmodifiableSet());
    }
}
