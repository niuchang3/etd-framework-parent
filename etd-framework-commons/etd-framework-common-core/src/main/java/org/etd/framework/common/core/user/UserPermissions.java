package org.etd.framework.common.core.user;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户权限聚合结果。
 * <p>
 * 租户归属在用户维度唯一，角色编码可以有多个。
 */
@Data
public class UserPermissions implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 用户唯一归属的租户标识。
     */
    private Long tenantId;

    /**
     * 用户全部角色权限码。
     */
    private Set<String> roleCodes = new LinkedHashSet<>();

    /**
     * 用户全部角色的数据权限类型编码。
     */
    private Set<String> permissionTypes = new LinkedHashSet<>();

    /**
     * 用户主组织标识。
     */
    private Long primaryOrganizationId;

    /**
     * 用户直接关联的组织标识集合。
     */
    private Set<Long> organizationIds = new LinkedHashSet<>();

    /**
     * 自定义跨组织角色授权的组织标识集合。
     */
    private Set<Long> customOrganizationIds = new LinkedHashSet<>();

    /**
     * 多角色合并后允许访问的组织标识集合。
     */
    private Set<Long> scopeOrganizationIds = new LinkedHashSet<>();

    /**
     * 是否平台管理员。
     */
    private Boolean platformAdmin = false;

    /**
     * 是否租户管理员。
     */
    private Boolean tenantAdmin = false;
}
