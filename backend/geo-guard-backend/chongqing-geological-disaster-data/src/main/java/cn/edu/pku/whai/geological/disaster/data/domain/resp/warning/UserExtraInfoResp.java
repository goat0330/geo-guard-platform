/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户扩展信息实体类（包含角色列表）
 */
@Data
public class UserExtraInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色列表
     */
    @Alias("roles")
    private List<RoleInfoResp> roles;
}
