/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 托管统计摘要。
 */
@Data
public class AiHostingSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 已协助处理数量。
     */
    private Long assistedCount;

    /**
     * 自动派发数量。
     */
    private Long autoDispatchCount;

    /**
     * 完成 AI 识图数量。
     */
    private Long aiVisionCompletedCount;

    /**
     * 生成报告数量。
     */
    private Long generatedReportCount;

    /**
     * 待人工确认数量。
     */
    private Long pendingConfirmCount;

    /**
     * 已结束事件数量。
     */
    private Long endedEventCount;
}
