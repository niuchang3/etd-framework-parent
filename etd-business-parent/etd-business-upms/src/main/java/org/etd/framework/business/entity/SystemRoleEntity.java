package org.etd.framework.business.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@TableName("system_role")
public class SystemRoleEntity extends BaseEntity implements Serializable {


    private static final long serialVersionUID = 5312247040589046419L;

    /**
     * 租户ID
     */
    @TableField("TENANT_ID")
    private Long tenantId;
    /**
     * 是否为内置角色
     */
    @TableField("BUILT_IN")
    private Boolean builtIn;
    /**
     * 角色名称
     */
    @TableField("ROLE_NAME")
    private String roleName;
    /**
     * 角色CODE
     */
    @TableField("ROLE_CODE")
    private String roleCode;
    /**
     * 角色描述
     */
    @TableField("ROLE_DESC")
    private String roleDesc;

    /**
     * 权限类型
     */
    @TableField("PERMISSION_TYPE")
    private String permissionType;

    /**
     * 菜单权限
     */
    @TableField("MENUS")
    private String menus;

}
