/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskReplyBo {

    @NotNull(message = "taskId不能为空")
    private String taskId;

    /**
     * 回复内容 bool类型传入true / false  obj类型传json字符串
     */
    @NotNull(message = "reply不能为空")
    private String reply;
}
