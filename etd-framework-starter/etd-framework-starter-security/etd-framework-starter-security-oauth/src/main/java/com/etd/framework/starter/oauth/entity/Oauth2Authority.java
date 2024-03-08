package com.etd.framework.starter.oauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oauth2Authority implements GrantedAuthority {

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
    private String tenantId;
    /**
     * 全险吗名称
     */
    private String authorityName;
    /**
     * 权限码CODE
     */
    private String authorityCode;


    @Override
    public String getAuthority() {
        return "";
    }
}
