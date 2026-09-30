/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ReportDisasterStatVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 总数
    private Long totalCount;
    // 低风险数量
    private Long lowCount;
    // 中风险数量
    private Long middleCount;
    // 高风险数量
    private Long highCount;
    // 极高风险数量
    private Long veryHighCount;

    // 待处理数量
    private Long unHandleCount;
    // 已报送数量
    private Long sendCount;
    // 已处理数量
    private Long handleCount;

    // 今日新增数量
    private Long todayCount;
    // 昨日新增数量
    private Long yesterdayCount;

}
