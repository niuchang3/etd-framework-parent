package com.etd.framework.starter.oauth.authentication.internal.token;

import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 刷新令牌认证请求对象。
 */
public class RefreshTokenRequestToken extends AbstractAuthenticationToken {

    /**
     * 刷新令牌原文。
     */
    @Setter
    private String credentials;

    /**
     * 认证成功后的用户标识。
     */
    @Setter
    private String principal;

    /**
     * 创建刷新令牌认证对象。
     *
     * @param authorities 用户权限集合
     */
    public RefreshTokenRequestToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}
