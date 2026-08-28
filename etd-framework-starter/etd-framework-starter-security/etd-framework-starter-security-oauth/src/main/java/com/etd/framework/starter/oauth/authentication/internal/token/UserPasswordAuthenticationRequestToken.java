package com.etd.framework.starter.oauth.authentication.internal.token;

import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 用户名密码认证请求对象。
 * <p>
 * 认证前 principal 是请求里的用户名，认证成功后 principal 会被替换为用户标识。
 */
public class UserPasswordAuthenticationRequestToken extends AbstractAuthenticationToken {

    /**
     * 登录账号或认证后的用户标识。
     */
    @Setter
    private String username;

    /**
     * 登录密码。
     */
    @Setter
    private String password;

    /**
     * OAuth2授权流程登录成功后的回跳地址。
     */
    @Setter
    private String redirect;


    /**
     * 创建认证对象。
     *
     * @param authorities 用户权限集合
     */
    public UserPasswordAuthenticationRequestToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    @Override
    public Object getCredentials() {
        return password;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    /**
     * 获取OAuth2授权流程登录成功后的回跳地址。
     *
     * @return 回跳地址
     */
    public String getRedirect() {
        return redirect;
    }
}
