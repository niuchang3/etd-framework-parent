package com.etd.framework.starter.oauth.authentication.token.request;

import com.etd.framework.starter.oauth.authentication.token.AbstractOauth2AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 授权码身份认证令牌
 */
@Getter
@Setter
public class Oauth2AuthorizationCodeAuthenticationToken extends AbstractOauth2AuthenticationToken {

    private String responseType;

    private String redirectUri;

    private Set<String> scope;


    private String state;


    public Oauth2AuthorizationCodeAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal, Object credentials) {
        super(authorities, principal, credentials);
    }

    public Oauth2AuthorizationCodeAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities, principal);
    }
}
