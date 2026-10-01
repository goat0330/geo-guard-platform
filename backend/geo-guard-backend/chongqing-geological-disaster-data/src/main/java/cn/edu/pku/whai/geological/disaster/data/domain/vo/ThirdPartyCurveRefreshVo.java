/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 手动全量重刷设备监测曲线结果
 */
@Data
public class ThirdPartyCurveRefreshVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 开始时间（毫秒时间戳）
     */
    private Long startTime;

    /**
     * 结束时间（毫秒时间戳）
     */
    private Long endTime;

    /**
     * 设备数量
     */
    private Integer deviceCount;

    /**
     * 实际同步的设备-监测类型数量
     */
    private Integer targetCurveTypeCount;

    /**
     * 落库影响行数（insert/update）
     */
    private Integer savedPointCount;

    /**
     * 实际发起的三方请求数
     */
    private Integer requestCount;

    /**
     * 三方请求成功数
     */
    private Integer successCount;

    /**
     * 三方请求失败数
     */
    private Integer failedCount;

    /**
     * 超时次数
     */
    private Integer timeoutCount;

    /**
     * 超时率，范围 0~1
     */
    private Double timeoutRate;

    /**
     * 429 次数
     */
    private Integer http429Count;

    /**
     * 429 比例，范围 0~1
     */
    private Double http429Rate;

    /**
     * 5xx 次数
     */
    private Integer http5xxCount;

    /**
     * 5xx 比例，范围 0~1
     */
    private Double http5xxRate;

    /**
     * 平均请求耗时（毫秒）
     */
    private Double avgRequestDurationMs;

    /**
     * 本次刷新总用时（毫秒）
     */
    private Long totalDurationMs;
}
