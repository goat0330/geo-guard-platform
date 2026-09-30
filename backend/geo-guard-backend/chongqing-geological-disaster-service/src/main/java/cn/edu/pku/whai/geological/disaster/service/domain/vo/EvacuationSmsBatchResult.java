/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 按 handleId 多方案群发短信的汇总结果
 *
 * @author whai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvacuationSmsBatchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总发送成功数
     */
    private int totalSuccessCount;

    /**
     * 总发送失败数
     */
    private int totalFailCount;

    /**
     * 按配置跳过发送的短信总数
     */
    private int totalSkipCount;

    /**
     * 因无有效经纬度等原因跳过的方案数
     */
    private int skipCount;

    /**
     * 短信总量
     */
    private int totalCount;

    /**
     * 各任务明细（按方案维度）
     */
    @Builder.Default
    private List<EvacuationSmsResult> taskDetails = new ArrayList<>();
}
