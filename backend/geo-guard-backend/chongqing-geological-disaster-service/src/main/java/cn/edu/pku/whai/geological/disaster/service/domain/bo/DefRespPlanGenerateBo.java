/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.Date;

/**
 * 生成最新防御响应方案入参。
 */
@Data
public class DefRespPlanGenerateBo {

    /**
     * 防御响应方案 id。
     */
    private Long defId;

    /**
     * 城市。
     */
    private String city;

    /**
     * 街道。
     */
    private String streets;

    /**
     * 启动条件。
     */
    private String triggerCondition;

    /**
     * 责任单位。
     */
    private String responsibilityUnit;

    /**
     * 响应级别。
     */
    private Integer level;

    /**
     * 创建时间。
     */
    private Date createDate;

    /**
     * 红色监测频次。
     */
    private String redMonitorFrequency;

    /**
     * 红色巡查次数。
     */
    private String redPatrolTimes;

    /**
     * 橙色监测频次。
     */
    private String orangeMonitorFrequency;

    /**
     * 橙色巡查次数。
     */
    private String orangePatrolTimes;

    /**
     * 黄色监测频次。
     */
    private String yellowMonitorFrequency;

    /**
     * 黄色巡查次数。
     */
    private String yellowPatrolTimes;

    /**
     * 蓝色监测频次。
     */
    private String blueMonitorFrequency;

    /**
     * 蓝色巡查次数。
     */
    private String bluePatrolTimes;
}
