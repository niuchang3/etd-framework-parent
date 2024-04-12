package org.etd.framework.business.vo;


import lombok.Data;

import java.io.Serializable;

@Data
public class UserRoleVo implements Serializable {

    private static final long serialVersionUID = -7093248445340142291L;

    /**
     * 用户与角色的关系表ID
     */
    private Long id;
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 角色ID
     */
    private Long roleId;
    /**
     * 角色CODE
     */
    private String roleCode;
    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 权限类型
     */
    private String permissionType;
    /**
     * 角色菜单
     */
    private String menus;
}
