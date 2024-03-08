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
     * 权限id
     */
    private Long id;
    /**
     * 权限父级ID
     */
    private Long parentId;

    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * 全险吗名称
     */
    private String authorityName;
    /**
     * 权限码CODE
     */
    private String authority;


    @Override
    public String getAuthority() {
        return authority;
    }

}
