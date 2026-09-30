/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

/**
 * 任务派发清单按日统计 VO
 */
@Data
public class TaskDistDayStatVo {
    /**
     * 日期（格式：YYYY-MM-DD）
     */
    private String day;

    /**
     * 数量
     */
    private Integer count;
}
