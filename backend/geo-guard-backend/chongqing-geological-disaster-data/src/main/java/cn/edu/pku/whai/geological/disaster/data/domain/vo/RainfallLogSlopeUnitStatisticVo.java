/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 斜坡单元实况雨量统计视图对象。
 * <p>
 * 用于向前端返回按日聚合后的雨量统计结果。
 *
 * @author system
 * @date 2026-04-01
 */
@Data
public class RainfallLogSlopeUnitStatisticVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 斜坡单元 ID。
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称。
     */
    private String slopeUnitName;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 该统计日最新一小时的小时雨量。
     */
    private Double hourlyRainfall;

    /**
     * 该统计日日累计雨量。
     */
    private Double dailyCumulativeRainfall;

    /**
     * 最近一次参与累计的小时。
     */
    private LocalDateTime lastHourTime;

    /**
     * 省名称。
     */
    private String province;

    /**
     * 市名称。
     */
    private String city;

    /**
     * 区县名称。
     */
    private String county;

    /**
     * 乡镇街道名称。
     */
    private String street;

    /**
     * 村名称。
     */
    private String village;

    /**
     * 社区名称。
     */
    private String community;

    /**
     * 省编码。
     */
    private String provinceCode;

    /**
     * 市编码。
     */
    private String cityCode;

    /**
     * 区县编码。
     */
    private String countyCode;

    /**
     * 乡镇街道编码。
     */
    private String streetCode;

    /**
     * 村编码。
     */
    private String villageCode;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
