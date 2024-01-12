package com.etd.framework.starter.oauth.authentication.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

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
