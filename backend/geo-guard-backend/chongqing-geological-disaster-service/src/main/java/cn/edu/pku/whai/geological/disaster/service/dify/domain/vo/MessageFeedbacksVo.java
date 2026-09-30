package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;


import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.MessageFeedbacks;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 【请填写功能名称】视图对象 message_feedbacks
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = MessageFeedbacks.class)
public class MessageFeedbacksVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 评价
     */
    private Integer rating;

    /**
     * 内容
     */
    private String content;

    /**
     * 来源
     */
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
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;


}
