package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 对话历史记录详情反馈对象 ai_chat_history_detail_feedbacks
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@TableName("ai_chat_history_detail_feedbacks")
public class AiChatHistoryDetailFeedbacks implements Serializable {

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
     * 历史对话id
     */
    private String historyId;

    /**
     * 对话详情id
     */
    private String historyDetailId;

    /**
     * 评价（1：喜欢，2：不喜欢）
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;


}
