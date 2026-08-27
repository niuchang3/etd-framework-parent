package com.etd.framework.starter.oauth.authentication.internal.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.token.TokenValue;
import com.etd.framework.starter.oauth.authentication.internal.converter.RefreshTokenRequestConverter;
import com.etd.framework.starter.oauth.authentication.internal.converter.UserPasswordRequestAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 认证过滤器公共依赖支持。
 * <p>
 * 收拢登录和刷新令牌过滤器共享的 Bean 依赖，避免配置器直接承担对象装配细节。
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilterSupport {

    private final ObjectMapper objectMapper;

    private final TokenEncoder<Authentication, ?> tokenEncoder;

    private final SecurityProperties securityProperties;

    private final EtdAuthenticationSuccessHandler successHandler;

    private final EtdAuthenticationFailureHandler failureHandler;

    /**
     * 创建用户密码登录请求转换器。
     *
     * @return 用户密码登录请求转换器
     */
    public UserPasswordRequestAuthenticationConverter userPasswordConverter() {
        return new UserPasswordRequestAuthenticationConverter(objectMapper);
    }

    /**
     * 创建刷新令牌请求转换器。
     *
     * @return 刷新令牌请求转换器
     */
    public RefreshTokenRequestConverter refreshTokenConverter() {
        return new RefreshTokenRequestConverter(objectMapper);
    }

    /**
     * 获取令牌签发器。
     *
     * @return 令牌签发器
     */
    @SuppressWarnings("unchecked")
    public TokenEncoder<Authentication, TokenValue> tokenEncoder() {
        return (TokenEncoder<Authentication, TokenValue>) tokenEncoder;
    }

    /**
     * 获取安全认证配置。
     *
     * @return 安全认证配置
     */
    public SecurityProperties securityProperties() {
        return securityProperties;
    }

    /**
     * 获取认证成功响应处理器。
     *
     * @return 认证成功响应处理器
     */
    public EtdAuthenticationSuccessHandler successHandler() {
        return successHandler;
    }

    /**
     * 获取认证失败响应处理器。
     *
     * @return 认证失败响应处理器
     */
    public EtdAuthenticationFailureHandler failureHandler() {
        return failureHandler;
    }
}
