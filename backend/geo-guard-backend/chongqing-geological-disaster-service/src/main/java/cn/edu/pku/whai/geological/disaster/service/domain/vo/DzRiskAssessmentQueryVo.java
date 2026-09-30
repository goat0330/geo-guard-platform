/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DzRiskAssessmentQueryVo {
    private Map<String, Object> query;
    private String gtype;
}
