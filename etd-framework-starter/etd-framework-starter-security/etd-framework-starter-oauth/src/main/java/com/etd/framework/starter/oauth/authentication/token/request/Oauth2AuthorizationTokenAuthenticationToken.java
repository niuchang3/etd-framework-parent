package com.etd.framework.starter.oauth.authentication.token.request;

import com.etd.framework.starter.oauth.authentication.token.AbstractOauth2AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@Setter
public class Oauth2AuthorizationTokenAuthenticationToken extends AbstractOauth2AuthenticationToken {


    private String grantType;

    private String code;

    private String redirectUri;


    public Oauth2AuthorizationTokenAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

    public Oauth2AuthorizationTokenAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal);
    }
}
