package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 对话历史对象 ai_chat_history
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@TableName("ai_chat_history")
public class AiChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 区分不同对话
     */
    private String type;

    /**
     * 当次会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 是否删除（0：未删除 1：已删除）
     */
    @TableLogic(value = "0")
    private Integer deleteFlag;

    /**
     * 置顶顺序
     */
    private Integer pinnedAt;

}
