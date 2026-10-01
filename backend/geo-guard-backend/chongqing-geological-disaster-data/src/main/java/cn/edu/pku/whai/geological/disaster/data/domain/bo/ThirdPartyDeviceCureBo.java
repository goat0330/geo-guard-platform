/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import cn.edu.pku.whai.geological.disaster.data.jackson.MillisOrDateStringDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 三方设备监测曲线查询条件
 */
@Data
public class ThirdPartyDeviceCureBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测类型
     */
    private String type;

    /**
     * 设备ID
     */
    @JsonProperty("clientID")
    private String clientId;

    /**
     * 开始时间：毫秒时间戳，或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" 等字符串
     */
    @JsonDeserialize(using = MillisOrDateStringDeserializer.class)
    private Long startTime;

    /**
     * 结束时间：毫秒时间戳，或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" 等字符串
     */
    @JsonDeserialize(using = MillisOrDateStringDeserializer.class)
    private Long endTime;
}
