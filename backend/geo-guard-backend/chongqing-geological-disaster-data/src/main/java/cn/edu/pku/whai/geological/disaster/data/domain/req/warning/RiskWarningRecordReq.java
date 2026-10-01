/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req.warning;

import lombok.Data;

@Data
public class RiskWarningRecordReq {

    /**
     * 页码
     */
    private Integer pageIndex = 1;

    /**
     * 每页记录数
     */
    private Integer pageSize = 100;

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
