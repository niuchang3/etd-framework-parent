package com.etd.framework.starter.oauth.authentication.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Oauth密码认证访问令牌
 * 适用于高度信任某个应用，RFC 6749 也允许用户把用户名和密码，直接告诉该应用。该应用就使用你的密码，申请令牌
 */
@Getter
@Setter
public class LoginAuthenticationToken extends AbstractOauth2AuthenticationToken {

    private String captcha;


    public LoginAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal, Object credentials) {
        super(authorities, principal, credentials);
    }

    public LoginAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities, principal);
    }
}
