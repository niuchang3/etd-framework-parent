package com.etd.framework.starter.oauth.authentication.token.request;

import com.etd.framework.starter.oauth.authentication.token.AbstractOauth2AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 客户端凭证身份认证令牌
 * 适用于没有前端的命令行应用，即在命令行下请求令牌。
 */
@Getter
@Setter
public class Oauth2ClientCredentialsAuthenticationToken extends AbstractOauth2AuthenticationToken {


    private String grantType;


    public Oauth2ClientCredentialsAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal, credentials);
    }

    public Oauth2ClientCredentialsAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, principal);
    }
}
