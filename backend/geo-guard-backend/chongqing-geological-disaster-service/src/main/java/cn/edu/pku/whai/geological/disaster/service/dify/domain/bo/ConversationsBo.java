package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.Conversations;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 【请填写功能名称】业务对象 conversations
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = Conversations.class, reverseConvertGenerate = false)
public class ConversationsBo {

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
     * 应用模型配置ID
     */
    private String appModelConfigId;

    /**
     * 模型提供商
     */
    private String modelProvider;

    /**
     * 覆盖模型配置
     */
    private String overrideModelConfigs;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 模式
     */
    @NotBlank(message = "模式不能为空", groups = {AddGroup.class, EditGroup.class})
    private String mode;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 输入
     */
    @NotBlank(message = "输入不能为空", groups = {AddGroup.class, EditGroup.class})
    private String inputs;

    /**
     * 介绍
     */
    private String introduction;

    /**
     * 系统指令
     */
    private String systemInstruction;

    /**
     * 系统指令Token数
     */
    @NotNull(message = "系统指令Token数不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer systemInstructionTokens;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private String status;

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
     * 阅读时间
     */
    private Date readAt;

    /**
     * 阅读账户ID
     */
    private String readAccountId;

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
     * 是否删除
     */
    @NotNull(message = "是否删除不能为空", groups = {AddGroup.class, EditGroup.class})
    private Boolean isDeleted;

    /**
     * 调用来源
     */
    private String invokeFrom;

    /**
     * 对话数量
     */
    @NotNull(message = "对话数量不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer dialogueCount;


}
