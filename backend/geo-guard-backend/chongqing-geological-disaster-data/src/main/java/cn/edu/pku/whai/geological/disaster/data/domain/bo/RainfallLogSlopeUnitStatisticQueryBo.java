/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 斜坡单元实况雨量统计查询对象。
 * <p>
 * 该对象同时服务两类查询接口：
 * 1. 按斜坡单元 ID 查询；
 * 2. 按行政区划名称、行政区划编码查询。
 * 未用到的条件可以为空。
 *
 * @author system
 * @date 2026-04-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RainfallLogSlopeUnitStatisticQueryBo extends BaseEntity {

    /**
     * 统计开始日期，按天查询，包含当天。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 统计结束日期，按天查询，包含当天。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 统计开始时间（小时级），格式支持 yyyy-MM-dd HH:mm / yyyy-MM-dd HH:mm:ss。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    /**
     * 统计结束时间（小时级），格式支持 yyyy-MM-dd HH:mm / yyyy-MM-dd HH:mm:ss，可为空。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    /**
     * 斜坡单元 ID。
     */
    private String slopeUnitId;

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
     * 省行政区划编码。
     */
    private String provinceCode;

    /**
     * 市行政区划编码。
     */
    private String cityCode;

    /**
     * 区县行政区划编码。
     */
    private String countyCode;

    /**
     * 乡镇街道行政区划编码。
     */
    private String streetCode;

    /**
     * 村行政区划编码。
     */
    private String villageCode;
}
