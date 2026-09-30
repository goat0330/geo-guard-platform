/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DzRiskPredictionStatVo {
    /**
     * 统计数据
     */
    private List<StatRiskVo> stat;

    /**
     * 行政区划统计
     */
    private Map<Integer, List<StatAreaVo>> statArea;

    /**
     * 相较上一次周期的比值
     */
    private Map<Integer, Integer> diffRisk;

    /**
     * 最新的时间
     */
    private String last;

}