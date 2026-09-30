/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class DzRiskPredictionChartVo {
    /**
     * 当前周期高风险前三区域
     */
    private List<RiskChatBannerVo.RiskChatBannerItemVo> keypointArea;

    /**
     * 下个周期高风险数量
     */
    private Integer nextPeriodHighRiskCount;

    /**
     * 下个周期极高风险数量
     */
    private Integer nextPeriodVeryHighRiskCount;
}
