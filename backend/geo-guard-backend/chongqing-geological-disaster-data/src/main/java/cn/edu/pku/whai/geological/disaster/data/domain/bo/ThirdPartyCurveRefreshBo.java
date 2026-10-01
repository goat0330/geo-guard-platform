/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import cn.edu.pku.whai.geological.disaster.data.jackson.MillisOrBeijingDateStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 手动全量重刷设备监测曲线请求
 */
@Data
public class ThirdPartyCurveRefreshBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 开始时间：毫秒时间戳，或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" 等字符串
     */
    @NotNull(message = "开始时间不能为空")
    @JsonDeserialize(using = MillisOrBeijingDateStringDeserializer.class)
    private Long startTime;

    /**
     * 结束时间：毫秒时间戳，或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" 等字符串
     */
    @NotNull(message = "结束时间不能为空")
    @JsonDeserialize(using = MillisOrBeijingDateStringDeserializer.class)
    private Long endTime;
}
