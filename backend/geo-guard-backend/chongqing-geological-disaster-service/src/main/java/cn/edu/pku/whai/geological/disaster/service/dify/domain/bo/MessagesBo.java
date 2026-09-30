package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.Messages;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 【请填写功能名称】业务对象 messages
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = Messages.class, reverseConvertGenerate = false)
public class MessagesBo {

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
    @NotBlank(message = "会话ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String conversationId;

    /**
     * 输入
     */
    @NotBlank(message = "输入不能为空", groups = {AddGroup.class, EditGroup.class})
    private String inputs;

    /**
     * 查询
     */
    @NotBlank(message = "查询不能为空", groups = {AddGroup.class, EditGroup.class})
    private String query;

    /**
     * 消息
     */
    @NotBlank(message = "消息不能为空", groups = {AddGroup.class, EditGroup.class})
    private String message;

    /**
     * 消息Token数
     */
    @NotNull(message = "消息Token数不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer messageTokens;

    /**
     * 消息单价
     */
    @NotNull(message = "消息单价不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal messageUnitPrice;

    /**
     * 回答
     */
    @NotBlank(message = "回答不能为空", groups = {AddGroup.class, EditGroup.class})
    private String answer;

    /**
     * 回答Token数
     */
    @NotNull(message = "回答Token数不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer answerTokens;

    /**
     * 回答单价
     */
    @NotNull(message = "回答单价不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal answerUnitPrice;

    /**
     * 提供商响应延迟
     */
    @NotNull(message = "提供商响应延迟不能为空", groups = {AddGroup.class, EditGroup.class})
    private Double providerResponseLatency;

    /**
     * 总价
     */
    private BigDecimal totalPrice;

    /**
     * 货币
     */
    @NotBlank(message = "货币不能为空", groups = {AddGroup.class, EditGroup.class})
    private String currency;

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

    /**
     * 基于Agent
     */
    @NotNull(message = "基于Agent不能为空", groups = {AddGroup.class, EditGroup.class})
    private Boolean agentBased;

    /**
     * 消息价格单位
     */
    @NotNull(message = "消息价格单位不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal messagePriceUnit;

    /**
     * 回答价格单位
     */
    @NotNull(message = "回答价格单位不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal answerPriceUnit;

    /**
     * 工作流运行ID
     */
    private String workflowRunId;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
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
    private String parentMessageId;


}
