package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;


import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.MessageFeedbacks;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 【请填写功能名称】业务对象 message_feedbacks
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = MessageFeedbacks.class, reverseConvertGenerate = false)
public class MessageFeedbacksBo {

    /**
     * ID
     */
    @NotBlank(message = "ID不能为空", groups = {EditGroup.class})
    private String id;

    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String appId;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String conversationId;

    /**
     * 消息ID
     */
    @NotBlank(message = "消息ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String messageId;

    /**
     * 评价
     */
    @NotNull(message = "评价不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer rating;

    /**
     * 内容
     */
    private String content;

    /**
     * 来源
     */
    @NotBlank(message = "来源不能为空", groups = {AddGroup.class, EditGroup.class})
    private String fromSource;

    /**
     * 来源终端用户ID
     */
    private String fromEndUserId;

    /**
     * 来源账户ID
     */
    private String fromAccountId;

    /**
     * 创建时间
     */
    @NotNull(message = "创建时间不能为空", groups = {AddGroup.class, EditGroup.class})
    private Date createdAt;

    /**
     * 更新时间
     */
    @NotNull(message = "更新时间不能为空", groups = {AddGroup.class, EditGroup.class})
    private Date updatedAt;


}
