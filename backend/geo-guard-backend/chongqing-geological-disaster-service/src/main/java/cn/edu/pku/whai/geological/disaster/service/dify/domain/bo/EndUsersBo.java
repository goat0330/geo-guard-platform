package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.EndUsers;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 【请填写功能名称】业务对象 end_users
 *
 * @author kongweiguang
 * @date 2025-12-19
 */
@Data
@AutoMapper(target = EndUsers.class, reverseConvertGenerate = false)
public class EndUsersBo {

    /**
     * ID
     */
    @NotBlank(message = "ID不能为空", groups = {EditGroup.class})
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
    @NotBlank(message = "类型不能为空", groups = {AddGroup.class, EditGroup.class})
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
    @NotNull(message = "是否匿名不能为空", groups = {AddGroup.class, EditGroup.class})
    private Boolean isAnonymous;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String sessionId;

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


}
