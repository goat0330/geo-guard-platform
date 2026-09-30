/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 风险总览数据视图对象。
 */
@Data
public class DzRiskOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 红色预警区域统计信息。
     */
    private Map<String, String> redAlertRegion;

    /**
     * 橙色预警区域统计信息。
     */
    private Map<String, String> orangeAlertRegion;

    /**
     * 黄色预警区域统计信息。
     */
    private Map<String, String> yellowAlertRegion;

    /**
     * 蓝色预警区域统计信息。
     */
    private Map<String, String> blueAlertRegion;

    /**
     * 未启动事件数量。
     */
    private String unstartedEventCount;

    /**
     * 处理中事件数量。
     */
    private String ongoingEventCount;

    /**
     * 启动中的区域防御响应涉及乡镇数量。
     */
    private String affectedTownCount;

    /**
     * 启动中的区域防御响应涉及斜坡单元数量。
     */
    private String affectedSlopeUnitCount;

    /**
     * 启动中的区域防御响应涉及灾害点数量。
     */
    private String affectedHazardPointCount;
}
