package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;


import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistory;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 对话历史业务对象 ai_chat_history
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AiChatHistory.class, reverseConvertGenerate = false)
public class AiChatHistoryBo extends BaseEntity {

    /**
     * id
     */
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
    private Integer deleteFlag;

    private Integer pinnedAt;

}
