/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiPictureDto {
    /**
     * 风险等级（1：低 2：中 3：高  4：极高）
     */
    private Integer aiRiskLevel;

    /**
     * 风险标签
     */
    private String aiRiskLabel;

    /**
     * 报告详情
     */
    private String aiReportDetail;

    /**
     * AI 识图补充属性
     */
    private Map<String, Object> aiVisionProps;
}
