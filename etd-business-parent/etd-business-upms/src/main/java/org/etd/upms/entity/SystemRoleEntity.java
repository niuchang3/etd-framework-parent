package org.etd.upms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("system_role")
public class SystemRoleEntity extends BaseEntity implements Serializable {


    private static final long serialVersionUID = 5312247040589046419L;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;
    /**
     * 是否为内置角色
     */
    @TableField("built_in")
    private Boolean builtIn;
    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;
    /**
     * 角色CODE
     */
    @TableField("role_code")
    private String roleCode;
    /**
     * 角色描述
     */
    @TableField("role_desc")
    private String roleDesc;

    /**
     * 权限类型
     */
    @TableField("permission_type")
    private String permissionType;

    /**
     * 菜单权限
     */
    @TableField("menus")
    private String menus;

}
