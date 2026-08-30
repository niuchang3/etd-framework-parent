package org.etd.framework.common.core.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * 用户角色权限。
 * <p>
 * 用户只归属一个租户，权限对象只表达角色权限码，不再重复携带租户信息。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAuthority implements GrantedAuthority {

    private static final long serialVersionUID = -1L;

    /**
     * 角色权限码。
     */
    private String authority;
}
