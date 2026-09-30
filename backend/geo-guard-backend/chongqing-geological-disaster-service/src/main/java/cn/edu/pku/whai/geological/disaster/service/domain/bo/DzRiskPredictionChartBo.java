/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

@Data
public class DzRiskPredictionChartBo {
    /**
     * 统计类型：1-日，2-周，3-月，4-季，5-年
     */
    private Integer type;
}
