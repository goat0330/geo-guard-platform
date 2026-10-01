/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监测统计请求对象
 */
@Data
public class MonitorStatReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 试点区域1
     */
    @JsonProperty("pilot_area_1")
    private Integer pilotArea1;

    /**
     * 试点区域2
     */
    @JsonProperty("pilot_area_2")
    private Integer pilotArea2;
}
