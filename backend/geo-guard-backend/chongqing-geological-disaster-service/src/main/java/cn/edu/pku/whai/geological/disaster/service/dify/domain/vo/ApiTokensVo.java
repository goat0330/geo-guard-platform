package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.ApiTokens;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 【请填写功能名称】视图对象 api_tokens
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = ApiTokens.class)
public class ApiTokensVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 应用ID
     */
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
    private String tenantId;


}
