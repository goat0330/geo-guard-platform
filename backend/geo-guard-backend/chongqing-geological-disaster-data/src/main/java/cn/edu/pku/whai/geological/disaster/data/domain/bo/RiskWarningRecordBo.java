/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RiskWarningRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 开始时间 示例 2025-05-01
     */
    private String startTime;

    /**
     * 结束时间 示例 2025-05-31
     */
    private String endTime;

    /**
     * 预警数据状态
     * 必传，默认传1，无需更改
     */
    private String state = "1";
}
