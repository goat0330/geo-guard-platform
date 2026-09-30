/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

@Data
public class StatAreaVo {
    private Integer dynamicRiskLevel;
    private String areaName;
    private Integer count;
}
