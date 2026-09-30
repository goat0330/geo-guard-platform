/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.hutool.core.collection.CollStreamUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public enum TaskNeedAttentionEnum {

    zsyf("1",
        "重点观测坡体裂缝（宽度、长度）、岩土松动 / 脱落情况，记录坡面变形、坍塌痕迹；排查周边堆载、开挖等人工扰动，检查支护结构完好状态。",
        "该单元已处于【XX行政区域】防御响应范围内，因自身易发性值高，需加密巡查频次（建议每X小时巡查1次），重点跟踪坡体裂缝、岩土松动的动态变化，发现裂缝扩张、岩土脱落等异常立即上报。"),
    jycyz("2",
        "检查周边沟渠、涵洞排水是否通畅，记录积水位置与水深；观测坡面雨水渗流、泥水流痕，排查坡脚土体软化、鼓胀情况。",
        "该单元已处于【XX行政区域】防御响应范围内，因降雨超阈值，需加密巡查频次（建议每X小时巡查1次），重点跟踪排水堵塞、坡体渗流情况，发现异常立即上报。"),
    cztmdd("3",
        "核查承灾体周边警示标识是否完好，确认疏散通道的通畅性。",
        "该单元已处于【XX行政区域】防御响应范围内，因承灾体密度大，需加密巡查频次（建议每X小时巡查1次），重点跟踪疏散通道是否畅通，发现异常立即上报。"),
    cxzq("4",
        "核查裂缝扩张、坡体滑移、局部坍塌等灾险范围及发展态势，划定影响边界，拍摄核心区域多角度照片，确认是否有人员、财产受威胁。",
        "该单元已处于【XX行政区域】防御响应范围内，因已出现灾险情，需加密巡查频次（建议每X小时巡查1次），重点跟踪灾险情扩张趋势，发现异常立即上报。");

    private final String code;
    private final String daily;
    private final String defense;

    public static final Map<String, TaskNeedAttentionEnum> codeMap =
        CollStreamUtil.toIdentityMap(Arrays.asList(values()), TaskNeedAttentionEnum::getCode);

}
