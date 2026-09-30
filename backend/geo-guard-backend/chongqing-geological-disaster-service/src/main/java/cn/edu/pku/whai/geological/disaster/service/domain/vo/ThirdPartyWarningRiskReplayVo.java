/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 今日预警风险联动补跑结果
 */
@Data
public class ThirdPartyWarningRiskReplayVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 补跑日期
     */
    private String replayDate;

    /**
     * 时间窗口开始时间（毫秒时间戳）
     */
    private Long startTime;

    /**
     * 时间窗口结束时间（毫秒时间戳）
     */
    private Long endTime;

    /**
     * 本次重放的预警条数
     */
    private Integer warningCount;

    /**
     * 本次补跑总耗时（毫秒）
     */
    private Long totalDurationMs;
}
