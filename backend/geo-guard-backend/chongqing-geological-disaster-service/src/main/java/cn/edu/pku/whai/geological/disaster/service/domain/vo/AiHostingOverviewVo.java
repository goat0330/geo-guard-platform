/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI 托管概览。
 */
@Data
public class AiHostingOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 托管是否正在运行。
     */
    private Boolean running;

    /**
     * 托管状态文案，例如：运行中、已关闭。
     */
    private String statusText;

    /**
     * 本次托管窗口开始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 本次托管窗口结束时间；运行中时为当前查询时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 本次托管已运行秒数。
     */
    private Long runningDurationSeconds;

    /**
     * 本次托管已运行时长文案。
     */
    private String runningDurationText;

    /**
     * 本次托管运行记录；未按记录查询时为空。
     */
    private AiHostingRecordSessionVo hostingRecord;

    /**
     * 本次托管统计摘要。
     */
    private AiHostingSummaryVo summary;

    /**
     * 正在处理的近似记录列表。
     */
    private List<AiHostingRecordVo> inProgress;

    /**
     * 最近完成的托管记录列表。
     */
    private List<AiHostingRecordVo> recentRecords;

    /**
     * 等待人工确认的记录列表。
     */
    private List<AiHostingRecordVo> pendingConfirmations;
}
