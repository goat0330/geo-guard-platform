/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态统计 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatisticsVo {

    /**
     * 总任务数
     */
    private Long totalCount;
}
