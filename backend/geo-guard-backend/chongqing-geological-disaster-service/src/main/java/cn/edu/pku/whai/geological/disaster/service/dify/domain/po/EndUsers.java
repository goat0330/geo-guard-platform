package cn.edu.pku.whai.geological.disaster.service.dify.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgUuidTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 【请填写功能名称】对象 end_users
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@TableName("end_users")
public class EndUsers implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 租户ID
     */
    @TableField(typeHandler = PgUuidTypeHandler.class)
    private String tenantId;

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
     * 外部用户ID
     */
    private String externalUserId;

    /**
     * 名称
     */
    private String name;

    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;


}
