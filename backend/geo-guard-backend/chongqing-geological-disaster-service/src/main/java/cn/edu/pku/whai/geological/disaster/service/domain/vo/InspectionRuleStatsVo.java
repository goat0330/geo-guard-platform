/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

/**
 * 巡查规则统计结果
 */
@Data
public class InspectionRuleStatsVo {

    /**
     * 统计日期
     */
    private String date;

    /**
     * 有效巡查次数
     */
    private Long validInspectionCount;

    /**
     * 单次逾期数
     */
    private Long singleOverdueCount;

    /**
     * 连续逾期数
     */
    private Long continuousOverdueCount;

    /**
     * 当日未达标数
     */
    private Integer underQuotaCount;

    /**
     * 无效提交拦截数
     */
    private Long invalidSubmitInterceptCount;

    /**
     * 电子围栏半径（米）
     */
    private Integer checkInFenceRadiusMeters;

    /**
     * 最短停留时长（分钟）
     */
    private Integer minStayDurationMinutes;

}
