/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 推送任务接口 data 节点响应实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTaskDataResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功接收的任务ID列表
     */
    private List<Long> receivedTaskIds;

    /**
     * 推送失败的任务ID列表
     */
    private List<Long> errorTaskIds;

    /**
     * 对应失败任务的错误信息列表
     */
    private List<String> errorMessages;
}
