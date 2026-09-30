package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgUuidTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 【请填写功能名称】对象 message_feedbacks
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@TableName("message_feedbacks")
public class MessageFeedbacks implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 应用ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String appId;

    /**
     * 会话ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String conversationId;

    /**
     * 消息ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
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
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String fromEndUserId;

    /**
     * 来源账户ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
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
