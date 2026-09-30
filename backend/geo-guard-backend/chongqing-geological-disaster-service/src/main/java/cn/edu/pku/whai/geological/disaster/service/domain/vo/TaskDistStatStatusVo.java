/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

@Data
public class TaskDistStatStatusVo {
    /**
     * 任务状态（1：未推送 2：未核查 3：核查中 4：已关闭 5：已反馈 6：申请技术协查 7：已过期）
     */
    private Integer status;

    private Integer count;
}
