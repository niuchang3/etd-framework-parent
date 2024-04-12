package com.etd.framework.starter.client.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAuthority implements GrantedAuthority {


    private static final long serialVersionUID = -1L;
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户下角色Code
     */
    private String roleCode;
    /**
     * 权限码CODE
     */
    private String authority;

}
