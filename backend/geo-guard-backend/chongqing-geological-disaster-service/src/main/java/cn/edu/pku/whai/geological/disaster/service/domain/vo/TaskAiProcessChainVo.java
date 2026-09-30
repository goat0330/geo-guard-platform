/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 分析流程链路展示对象。
 */
@Data
public class TaskAiProcessChainVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 普通流程链路节点。
     */
    private List<TaskProcessChainNodeVo> processChain;

    /**
     * 处置管理 id
     */
    private Long handleId;

    /**
     * 防御响应方案 id
     */
    private Long defId;

    /**
     * 报告 id 列表；任务反馈可能产生多条报告
     */
    private List<Long> reportIds;

    /**
     * 任务理解。
     */
    private String taskUnderstanding;

    /**
     * 当前判断。
     */
    private String currentJudgement;
}
