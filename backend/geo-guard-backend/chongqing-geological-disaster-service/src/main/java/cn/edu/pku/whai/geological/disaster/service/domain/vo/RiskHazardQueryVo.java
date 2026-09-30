/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RiskHazardQueryVo {
    private String date;
    private BigDecimal hazard;
    private BigDecimal risk;
    private Integer hazardLevel;
    private Integer riskLevel;
}
