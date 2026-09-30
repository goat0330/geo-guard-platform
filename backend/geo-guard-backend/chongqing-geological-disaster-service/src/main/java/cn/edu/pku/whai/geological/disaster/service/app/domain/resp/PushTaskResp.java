/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推送任务接口完整响应实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTaskResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码，200表示成功
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private PushTaskDataResp data;
}
