package org.etd.framework.common.core.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 框架内部登录用户详情。
 * <p>
 * 登录校验阶段允许包含密码，签发 JWT 前会复制安全字段，避免密码写入令牌。
 */
@Data
public class UserDetails implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 用户标识。
     */
    private Long id;
    /**
     * 登录账号。
     */
    private String account;
    /**
     * 手机号码。
     */
    private String mobile;

    /**
     * 密码信息。
     */
    @ToString.Exclude
    private String password;

    /**
     * 用户名称。
     */
    private String userName;
    /**
     * 生日。
     */
    private LocalDate birthday;
    /**
     * 性别。
     */
    private Integer gender;
    /**
     * 头像。
     */
    private String avatar;

    /**
     * 昵称。
     */
    private String nickName;

    /**
     * 账号是否锁定。
     */
    private Boolean locked;

    /**
     * 账号是否启用。
     */
    private Boolean enabled;

    /**
     * 用户唯一归属的租户标识。
     */
    private Long tenantId;

    /**
     * 用户拥有的全部角色权限码。
     */
    private Set<String> roleCodes;

    /**
     * 是否平台管理员。
     */
    private Boolean platformAdmin = false;
    /**
     * 是否租户管理员。
     */
    private Boolean tenantAdmin = false;

    /**
     * 权限信息。
     */
    private List<RoleAuthority> authorities;

    /**
     * 用户主组织/部门标识。
     */
    private Long orgId;

    /**
     * 用户关联的所有组织/部门标识集合。
     */
    private Set<Long> orgIds;

    /**
     * 计算合并后的数据权限范围类型编码。
     */
    private String permissionType;

    /**
     * 用户全部角色的数据权限类型编码，用于按并集合并多个角色的数据范围。
     */
    private Set<String> permissionTypes;

    /**
     * 自定义数据权限范围绑定的组织/部门标识集合。
     */
    private Set<Long> customOrgIds;

    /**
     * 最终计算合并的可访问组织/部门标识集合（包含本部门、下级部门或自定义部门等）。
     */
    private Set<Long> scopeOrgIds;

    /**
     * 是否为平台管理员。
     *
     * @return 是否平台管理员
     */
    public boolean isPlatformAdmin() {
        return Boolean.TRUE.equals(platformAdmin);
    }

    /**
     * 是否租户管理员。
     *
     * @return 是否租户管理员
     */
    public boolean isTenantAdmin() {
        return Boolean.TRUE.equals(tenantAdmin);
    }

    public boolean isAdmin(){
        return isPlatformAdmin() || isTenantAdmin();
    }

}
