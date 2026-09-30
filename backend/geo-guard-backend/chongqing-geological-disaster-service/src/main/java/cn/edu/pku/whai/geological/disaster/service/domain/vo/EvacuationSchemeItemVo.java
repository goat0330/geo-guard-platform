/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 撤离方案单项
 *
 * @author whai
 */
@Data
public class EvacuationSchemeItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 方案类型
     */
    @JsonProperty("plan_type")
    private String planType;

    /**
     * 具体措施
     */
    @JsonProperty("specific_measures")
    private String specificMeasures;
}
