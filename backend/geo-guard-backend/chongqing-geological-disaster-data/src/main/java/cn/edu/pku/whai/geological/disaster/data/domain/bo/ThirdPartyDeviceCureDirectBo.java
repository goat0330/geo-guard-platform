/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 三方设备监测曲线直连查询参数
 */
@Data
public class ThirdPartyDeviceCureDirectBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备 client_id
     */
    @JsonAlias("client_id")
    private String clientId;

    /**
     * 监测类型 monitor_type
     */
    @JsonAlias("monitor_type")
    private String monitorType;

    /**
     * 开始时间，格式：yyyyMMddHHmmss
     */
    @JsonAlias("start_time")
    private String startTime;

    /**
     * 结束时间，格式：yyyyMMddHHmmss
     */
    @JsonAlias("end_time")
    private String endTime;
}
