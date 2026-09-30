/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 确认任务SSE数据对象
 */
@Data
@Builder
public class ConfirmationTaskDto {
    /**
     * 消息类型
     */
    private String type;
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 提示内容
     */
    private String prompt;
}
