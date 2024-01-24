package com.etd.framework.starter.oauth.authentication.token;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class OAuth2AccessToken extends AbstractOauth2AuthenticationToken{



    public OAuth2AccessToken(Collection<? extends GrantedAuthority> authorities, Object principal, Object credentials) {
        super(authorities, principal, credentials);
    }

    public OAuth2AccessToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities, principal);
    }
}
