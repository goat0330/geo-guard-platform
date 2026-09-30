/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 按链路范围短信预览结果。
 */
@Data
public class TaskDistChainPreviewResultVo {

    /**
     * 结果编码：SUCCESS/PARTIAL_SUCCESS/NO_CHAIN_MATCH/NO_UNPUSHED_TASK/NO_PUSH_PERMISSION
     */
    private String resultCode;

    /**
     * 结果说明。
     */
    private String resultMessage;

    /**
     * 短信预览列表。
     */
    private List<TaskDistSmsPreviewItemVo> previewItems;

    /**
     * 解析摘要。
     */
    private TaskChainPushSummaryVo summary;
}
