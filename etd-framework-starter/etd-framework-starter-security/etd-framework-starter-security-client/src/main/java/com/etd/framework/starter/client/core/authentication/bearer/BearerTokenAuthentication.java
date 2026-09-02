package com.etd.framework.starter.client.core.authentication.bearer;

import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Bearer 令牌认证对象。
 * <p>
 * 认证前只携带请求头中的令牌，认证后会写入用户标识、权限集合和用户详情。
 */
public class BearerTokenAuthentication extends AbstractAuthenticationToken {

    /**
     * 请求头中的访问令牌。
     */
    @Setter
    private String credentials;

    /**
     * 当前登录用户标识。
     */
    @Setter
    private String principal;

    /**
     * 创建认证对象。
     *
     * @param authorities 用户权限集合
     */
    public BearerTokenAuthentication(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    /**
     * 创建待认证的 Bearer 请求对象。
     *
     * @param authorities 用户权限集合
     * @param credentials 访问令牌
     */
    public BearerTokenAuthentication(Collection<? extends GrantedAuthority> authorities, String credentials) {
        super(authorities);
        this.credentials = credentials;
    }

    /**
     * 获取 Credentials 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getCredentials() {
        return credentials;
    }

    /**
     * 获取 Principal 属性值
     *
     * @return 处理结果
     */
    @Override
    public Object getPrincipal() {
        return principal;
    }
}
