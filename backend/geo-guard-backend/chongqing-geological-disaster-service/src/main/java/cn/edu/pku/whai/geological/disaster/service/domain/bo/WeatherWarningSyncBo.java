/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 手动触发第三方气象预警同步请求
 */
@Data
public class WeatherWarningSyncBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 开始日期，格式 yyyy-MM-dd
     */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "开始时间格式错误，应为 yyyy-MM-dd")
    private String startTime;

    /**
     * 结束日期，格式 yyyy-MM-dd
     */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "结束时间格式错误，应为 yyyy-MM-dd")
    private String endTime;
}
