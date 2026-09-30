package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;


import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetailFeedbacks;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 对话历史记录详情反馈业务对象 ai_chat_history_detail_feedbacks
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AiChatHistoryDetailFeedbacks.class, reverseConvertGenerate = false)
public class AiChatHistoryDetailFeedbacksBo extends BaseEntity {

    /**
     * id
     */
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
