package com.etd.framework.starter.client.core.authentication.bearer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Bearer 令牌认证配置器。
 * <p>
 * 负责注册 Bearer 认证提供者，并把 Bearer 认证过滤器加入 Spring Security 过滤器链。
 */
public class BearerAuthenticationConfigurer extends AbstractHttpConfigurer<BearerAuthenticationConfigurer, HttpSecurity> {

    /**
     * 注册 Bearer 认证提供者。
     *
     * @param builder HTTP 安全构建器
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(HttpSecurity builder) {
        ApplicationContext applicationContext = builder.getSharedObject(ApplicationContext.class);
        TokenDecode<SignedJWT> tokenDecode = applicationContext.getBean(TokenDecode.class);
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        BearerAuthenticationProvider provider = new BearerAuthenticationProvider(tokenDecode, objectMapper);
        builder.authenticationProvider(provider);
    }

    /**
     * 注册请求头令牌解析过滤器。
     *
     * @param builder HTTP 安全构建器
     */
    @Override
    public void configure(HttpSecurity builder) {
        AuthenticationConverter converter = new BearerAuthenticationConverter();
        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);
        BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(converter, authenticationManager);
        builder.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
    }
}
