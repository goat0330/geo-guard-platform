/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 角色信息实体类
 */
@Data
public class RoleInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Alias("roleid")
    private String roleId;

    /**
     * 企业ID
     */
    @Alias("corpoid")
    private String corpid;

    /**
     * 父角色ID
     */
    @Alias("rolepid")
    private String rolePid;

    /**
     * 角色编码
     */
    @Alias("rolecode")
    private String roleCode;

    /**
     * 角色路径
     */
    @Alias("rolepath")
    private String rolePath;

    /**
     * 角色名称
     */
    @Alias("rolename")
    private String roleName;

    /**
     * 角色类型
     */
    @Alias("roletype")
    private String roleType;

    /**
     * 级别
     */
    @Alias("levels")
    private String levels;

    /**
     * 状态
     */
    @Alias("status")
    private String status;

    /**
     * 配置信息
     */
    @Alias("configs")
    private String configs;

    /**
     * 图标
     */
    @Alias("icon")
    private String icon;

    /**
     * 描述
     */
    @Alias("description")
    private String description;

    /**
     * 创建人ID
     */
    @Alias("cuserid")
    private String createUserId;

    /**
     * 创建时间
     */
    @Alias("ctime")
    private String createTime;

    /**
     * 序号
     */
    @Alias("seqno")
    private String seqNo;

    /**
     * 是否启用
     */
    @Alias("enable")
    private String enable;
}