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
 * 【请填写功能名称】对象 api_tokens
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@TableName("api_tokens")
public class ApiTokens implements Serializable {

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
     * 类型
     */
    private String type;

    /**
     * Token
     */
    private String token;

    /**
     * 最后使用时间
     */
    private Date lastUsedAt;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 租户ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String tenantId;


}
