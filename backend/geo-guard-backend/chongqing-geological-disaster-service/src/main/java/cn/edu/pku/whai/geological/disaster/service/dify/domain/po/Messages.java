package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgUuidTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 【请填写功能名称】对象 messages
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@TableName("messages")
public class Messages implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 应用ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String appId;

    /**
     * 模型提供商
     */
    private String modelProvider;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 覆盖模型配置
     */
    private String overrideModelConfigs;

    /**
     * 会话ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String conversationId;

    /**
     * 输入
     */
    private String inputs;

    /**
     * 查询
     */
    private String query;

    /**
     * 消息
     */
    private String message;

    /**
     * 消息Token数
     */
    private Integer messageTokens;

    /**
     * 消息单价
     */
    private BigDecimal messageUnitPrice;

    /**
     * 回答
     */
    private String answer;

    /**
     * 回答Token数
     */
    private Integer answerTokens;

    /**
     * 回答单价
     */
    private BigDecimal answerUnitPrice;

    /**
     * 提供商响应延迟
     */
    private Double providerResponseLatency;

    /**
     * 总价
     */
    private BigDecimal totalPrice;

    /**
     * 货币
     */
    private String currency;

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

    /**
     * 基于Agent
     */
    private Boolean agentBased;

    /**
     * 消息价格单位
     */
    private BigDecimal messagePriceUnit;

    /**
     * 回答价格单位
     */
    private BigDecimal answerPriceUnit;

    /**
     * 工作流运行ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String workflowRunId;

    /**
     * 状态
     */
    private String status;

    /**
     * 错误
     */
    private String error;

    /**
     * 消息元数据
     */
    private String messageMetadata;

    /**
     * 调用来源
     */
    private String invokeFrom;

    /**
     * 父消息ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String parentMessageId;


}
