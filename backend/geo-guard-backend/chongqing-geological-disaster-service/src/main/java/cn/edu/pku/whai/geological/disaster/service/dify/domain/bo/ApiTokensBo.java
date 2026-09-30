package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.ApiTokens;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 【请填写功能名称】业务对象 api_tokens
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = ApiTokens.class, reverseConvertGenerate = false)
public class ApiTokensBo {

    /**
     * ID
     */
    @NotBlank(message = "ID不能为空", groups = {EditGroup.class})
    private String id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 类型
     */
    @NotBlank(message = "类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String type;

    /**
     * Token
     */
    @NotBlank(message = "Token不能为空", groups = {AddGroup.class, EditGroup.class})
    private String token;

    /**
     * 最后使用时间
     */
    private Date lastUsedAt;

    /**
     * 创建时间
     */
    @NotNull(message = "创建时间不能为空", groups = {AddGroup.class, EditGroup.class})
    private Date createdAt;

    /**
     * 租户ID
     */
    private String tenantId;


}
