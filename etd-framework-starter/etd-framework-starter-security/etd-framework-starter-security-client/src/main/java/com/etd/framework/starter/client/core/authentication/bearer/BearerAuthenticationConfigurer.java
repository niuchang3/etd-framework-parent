package com.etd.framework.starter.client.core.authentication.bearer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Bearer 令牌认证配置器。
 * <p>
 * 负责注册 Bearer 认证提供者，并把 Bearer 认证过滤器加入 Spring Security 过滤器链。
 */
public class BearerAuthenticationConfigurer extends AbstractSecurityEndpointConfigurer {

    /**
     * 注册 Bearer 认证提供者。
     *
     * @param builder HTTP 安全构建器
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(HttpSecurity builder) {
        TokenDecode<SignedJWT> tokenDecode = getApplicationContext(builder).getBean(TokenDecode.class);
        ObjectMapper objectMapper = getApplicationContext(builder).getBean(ObjectMapper.class);
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
        BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(converter, getAuthenticationManager(builder));
        builder.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
    }

    /**
     * Bearer 过滤器是全局请求过滤器，不单独暴露端点匹配器。
     *
     * @return 固定返回 {@code null}
     */
    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }
}
