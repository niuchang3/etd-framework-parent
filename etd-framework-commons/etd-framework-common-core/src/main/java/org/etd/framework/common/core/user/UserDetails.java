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
     * 所属租户是否锁定。
     */
    private Boolean tenantLocked;

    /**
     * 所属租户是否启用。
     */
    private Boolean tenantEnabled;

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
     * 用户实际获授的接口读写权限，角色编码单独存放在 roleCodes。
     */
    private List<PermissionAuthority> authorities = new java.util.ArrayList<>();

    /**
     * 用户主组织/部门标识。
     */
    private Long orgId;

    /**
     * 用户关联的所有组织/部门标识集合。
     */
    private Set<Long> orgIds;

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

    /**
     * 判断当前用户是否允许登录。
     *
     * @return 用户和所属租户均启用时返回 true
     */
    public boolean isLoginEnabled() {
        return Boolean.TRUE.equals(enabled) && Boolean.TRUE.equals(tenantEnabled);
    }

    /**
     * 判断当前用户是否只能执行只读操作。
     *
     * @return 用户或所属租户锁定时返回 true
     */
    public boolean isReadOnly() {
        return Boolean.TRUE.equals(locked) || Boolean.TRUE.equals(tenantLocked);
    }

    /**
     * 判断 Admin 状态
     *
     * @return 处理结果
     */
    public boolean isAdmin(){
        return isPlatformAdmin() || isTenantAdmin();
    }

}
