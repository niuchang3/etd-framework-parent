package com.etd.framework.starter.client.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * 租户维度的权限模型。
 * <p>
 * 同一个用户在不同租户下可能拥有不同角色和权限，通过该对象表达权限归属。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAuthority implements GrantedAuthority {


    private static final long serialVersionUID = -1L;
    /**
     * 租户标识。
     */
    private Long tenantId;

    /**
     * 租户下角色编码。
     */
    private String roleCode;
    /**
     * 权限编码。
     */
    private String authority;

}
