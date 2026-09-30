package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgJacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.imfangs.dify.client.model.file.FileInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 对话历史记录详情对象 ai_chat_history_detail
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@TableName(value = "ai_chat_history_detail", autoResultMap = true)
public class AiChatHistoryDetail implements Serializable {

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
     * 对话id
     */
    private String historyId;

    /**
     * 对话传入额外参数
     */
    @TableField(typeHandler = PgJacksonTypeHandler.class)
    private Map<String, Object> inputs;

    /**
     * 本次对话请求携带的附件
     */
    @TableField(typeHandler = PgJacksonTypeHandler.class)
    private List<FileInfo> files;

    /**
     * 问题
     */
    private String query;

    /**
     * 答案
     */
    private String answer;

    /**
     * 额外响应数据
     */
    @TableField(typeHandler = PgJacksonTypeHandler.class)
    private Map<String, Object> messageMetadata;

    /**
     * 创建时间
     */
    private Date createDate;


}
