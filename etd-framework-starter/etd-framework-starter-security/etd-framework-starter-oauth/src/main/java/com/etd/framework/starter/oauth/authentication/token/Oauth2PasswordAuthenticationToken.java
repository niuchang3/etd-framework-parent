package com.etd.framework.starter.oauth.authentication.token;

import com.etd.framework.starter.oauth.authentication.token.AbstractOauth2AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * Oauth密码认证访问令牌
 * 适用于高度信任某个应用，RFC 6749 也允许用户把用户名和密码，直接告诉该应用。该应用就使用你的密码，申请令牌
 */
@Getter
@Setter
public class Oauth2PasswordAuthenticationToken extends AbstractOauth2AuthenticationToken {

    private String captcha;

    private String grantType;

    private Set<String> scope;

    public Oauth2PasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

    public Oauth2PasswordAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal);
    }


}
