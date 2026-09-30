/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

@Data
public class DzRiskPredictionStatBo {
    /**
     * 统计类型：1-日，2-周，3-月，4-季，5-年
     */
    private Integer type;

    /**
     * 统计时间：按该时间所属周期统计风险预测数据
     */
    private String predictionDateStr;

    private String slopeUnitId;
    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属地级市
     */
    private String city;

    /**
     * 所属区/县
     */
    private String county;

    /**
     * 所属乡镇/街道
     */
    private String street;

    /**
     * 所属行政村
     */
    private String village;

    /**
     * 所属社区（若适用）
     */
    private String community;
}
