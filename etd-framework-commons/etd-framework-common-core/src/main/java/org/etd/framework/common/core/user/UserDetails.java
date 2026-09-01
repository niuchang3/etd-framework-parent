package org.etd.framework.common.core.user;

import lombok.Data;

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
