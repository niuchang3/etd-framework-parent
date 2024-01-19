package com.etd.framework.starter.oauth.authentication.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * <a src='https://datatracker.ietf.org/doc/html/rfc6749#section-4.1'>Oauth2 授权码认证</a>
 */
@Getter
@Setter
public class Oauth2AuthorizationCodeAuthenticationToken extends AbstractOauth2AuthenticationToken {


    private String grantType;

    private String code;

    private String redirectUri;


    public Oauth2AuthorizationCodeAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

    public Oauth2AuthorizationCodeAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal);
    }
}
