/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class AlgorithmReceiveBo {
    private Long batchId;
    private List<Long> riskIds;
}
