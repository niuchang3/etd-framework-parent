package org.etd.framework.common.core.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * 用户接口操作权限。
 * <p>
 * 用户只归属一个租户，权限对象只表达具体操作权限码，不再重复携带租户信息。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionAuthority implements GrantedAuthority {

    private static final long serialVersionUID = -1L;

    /**
     * 具体操作权限码，例如 system:user:read。
     */
    private String authority;
}
