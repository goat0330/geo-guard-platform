package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgUuidTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 【请填写功能名称】对象 conversations
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@TableName("conversations")
public class Conversations implements Serializable {

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
     * 应用模型配置ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
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
    private String mode;

    /**
     * 名称
     */
    private String name;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 输入
     */
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
    private Integer systemInstructionTokens;

    /**
     * 状态
     */
    private String status;

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
     * 阅读时间
     */
    private Date readAt;

    /**
     * 阅读账户ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String readAccountId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 是否删除
     */
    private Boolean isDeleted;

    /**
     * 调用来源
     */
    private String invokeFrom;

    /**
     * 对话数量
     */
    private Integer dialogueCount;


}
