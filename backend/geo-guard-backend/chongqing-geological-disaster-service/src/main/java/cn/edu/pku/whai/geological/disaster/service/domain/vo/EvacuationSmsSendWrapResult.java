/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 撤离短信群发接口统一返回：外层包装，包含撤离短信与负责人短信两类群发结果
 *
 * @author whai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvacuationSmsSendWrapResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 短信总量
     */
    private int totalCount;

    /**
     * 短信触达数量
     */
    private int reachCount;

    /**
     * 任务总量
     */
    private int taskTotalCount;

    /**
     * 任务完成数量
     */
    private int taskCompleteCount;

    /**
     * 完成度比例，范围 0.00~1.00，保留两位小数
     */
    private BigDecimal completeRate;

}
