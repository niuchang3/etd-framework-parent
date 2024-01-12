package com.etd.framework.starter.oauth.authentication.token;

import com.etd.framework.starter.oauth.authentication.token.AbstractOauth2AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 隐藏式身份认证令牌
 * 适用于没有后端服务，直接向前端颁发令牌。这种方式没有授权码这个中间步骤
 */
@Getter
@Setter
public class Oauth2ImplicitAuthenticationToken extends AbstractOauth2AuthenticationToken {

    private String responseType;

    private String redirectUri;

    private Set<String> scope;

    private String state;


    public Oauth2ImplicitAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

    public Oauth2ImplicitAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal);
    }
}
