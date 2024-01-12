package com.etd.framework.starter.oauth.authentication.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 抽象的Oauth2身份认证令牌
 * <ui>
 * <li>
 * <a src='https://www.ruanyifeng.com/blog/2019/04/oauth-grant-types.html'>参考文档</a>
 * </li>
 * <li>
 * authorize(授权)模式下
 * principal : client_id
 * </li>
 * <li>
 *
 * </li>
 * </ui>
 */
public abstract class AbstractOauth2AuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 通常存放账号
     */
    private Object principal;
    /**
     * 通常存放密码
     */
    private Object credentials;


    public AbstractOauth2AuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal, Object credentials) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
    }

    public AbstractOauth2AuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities);
        this.principal = principal;
    }

    /**
     * 返回密码
     *
     * @return
     */
    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    /**
     * 返回账号信息
     *
     * @return
     */
    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * 擦除凭据内容
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
