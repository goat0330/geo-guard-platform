/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

/**
 * 按链路范围推送结果。
 */
@Data
public class TaskDistChainPushResultVo {

    /**
     * 结果编码：SUCCESS/PARTIAL_SUCCESS/NO_CHAIN_MATCH/NO_UNPUSHED_TASK/NO_PUSH_PERMISSION
     */
    private String resultCode;

    /**
     * 结果说明。
     */
    private String resultMessage;

    /**
     * 旧推送结果。
     */
    private TaskDistPushVo pushResult;

    /**
     * 解析摘要。
     */
    private TaskChainPushSummaryVo summary;
}
