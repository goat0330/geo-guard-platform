/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DzRiskAssessmentStatVo {
    /**
     * 统计数据
     */
    private List<StatRiskVo> stat;

    /**
     * 行政区划统计
     */
    private Map<Integer, List<StatAreaVo>> statArea;

    /**
     * 归因分析统计
     */
    private Map<Integer, Object> statAttribution;

    /**
     * 相较上一次周期的比值
     */
    private Map<Integer, Integer> diffRisk;

    /**
     * 统计设备驱动器数据
     */
    private List<StatRiskVo> statDevice;

    /**
     * 统计灾害点数量
     */
    private List<StatRiskVo> statHazardPoint;
}
