/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 托管记录。
 */
@Data
public class AiHostingRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录发生时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

    /**
     * 记录标题。
     */
    private String title;

    /**
     * 记录描述。
     */
    private String description;

    /**
     * 业务类型：0未知 1任务 2报灾/报告 3处置管理 4防御响应 5预警信息 6监测预警。
     */
    private Integer bizType;

    /**
     * 业务主键。
     */
    private Long bizId;

    /**
     * 关联任务派发 ID。
     */
    private Long taskId;

    /**
     * 关联任务数量。
     */
    private Integer taskCount;

    /**
     * 记录来源。
     */
    private String source;

    /**
     * 记录状态。
     */
    private String status;
}
