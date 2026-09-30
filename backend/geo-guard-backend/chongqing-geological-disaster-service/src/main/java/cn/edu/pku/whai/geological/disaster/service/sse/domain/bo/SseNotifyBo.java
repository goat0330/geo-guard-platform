/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sse.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE 自定义通知请求参数
 */
@Data
public class SseNotifyBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "接收用户不能为空")
    private List<Long> userIds;

    @NotNull(message = "通知内容不能为空")
    private Object content;
}
