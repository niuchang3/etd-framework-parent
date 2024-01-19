package com.etd.framework.starter.oauth.authentication.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 授权码身份认证令牌
 * <a src='https://datatracker.ietf.org/doc/html/rfc6749#section-4.1'>Oauth2授权码请求</a>
 */
@Getter
@Setter
public class Oauth2AuthorizationCodeRequestToken extends AbstractOauth2AuthenticationToken {

    private String responseType;

    private String redirectUri;

    private Set<String> scope;

    private String state;


    public Oauth2AuthorizationCodeRequestToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

}
