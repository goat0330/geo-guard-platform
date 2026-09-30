package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.EndUsers;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 【请填写功能名称】视图对象 end_users
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = EndUsers.class)
public class EndUsersVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用ID
     */
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
