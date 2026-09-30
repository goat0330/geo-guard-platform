/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

@Data
public class AiModifyReportBo {
    private Long id;
    private String prompt = "生成报告";
}
