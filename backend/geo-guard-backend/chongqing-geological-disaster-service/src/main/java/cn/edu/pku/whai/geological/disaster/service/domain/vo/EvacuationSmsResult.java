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
 * 单条撤离短信群发任务结果
 *
 * @author whai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvacuationSmsResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 撤离区域（当前方案）
     */
    private String evacuationArea;

    /**
     * 发送成功数
     */
    private int successCount;

    /**
     * 发送失败数
     */
    private int failCount;

    /**
     * 按配置跳过发送数
     */
    private int skipCount;

    /**
     * 短信总量
     */
    private int totalCount;

    /**
     * 发送失败的号码列表
     */
    @Builder.Default
    private List<String> failedPhones = new ArrayList<>();
}
