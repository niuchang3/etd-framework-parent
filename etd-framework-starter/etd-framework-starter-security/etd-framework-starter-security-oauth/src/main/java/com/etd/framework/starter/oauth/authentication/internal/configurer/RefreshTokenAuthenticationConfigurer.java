package com.etd.framework.starter.oauth.authentication.internal.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.factory.RefreshTokenAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.filter.RefreshTokenRequestFilter;
import com.etd.framework.starter.oauth.authentication.internal.provider.RefreshTokenAuthenticationProvider;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 刷新令牌认证配置器。
 * <p>
 * 注册刷新令牌认证提供者和刷新请求过滤器。
 */
public class RefreshTokenAuthenticationConfigurer extends AbstractSecurityEndpointConfigurer {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    private RequestMatcher authenticationEndpointMatcher;

    /**
     * 自定义刷新令牌端点匹配器。
     *
     * @param endpoint 刷新令牌端点匹配器
     */
    public RefreshTokenAuthenticationConfigurer addEndpointMatcher(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return this;
        }
        this.authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, endpoint);
        return this;
    }

    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider();
        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            // 默认刷新 token 端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/internal/refresh");
        }
        builder.authenticationProvider(provider);
    }

    @Override
    public void configure(HttpSecurity builder) {
        RefreshTokenAuthenticationFilterFactory factory = getApplicationContext(builder)
                .getBean(RefreshTokenAuthenticationFilterFactory.class);
        RefreshTokenRequestFilter filter = factory.create(authenticationEndpointMatcher, getAuthenticationManager(builder));
        builder.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 获取刷新令牌端点匹配器。
     *
     * @return 刷新令牌端点匹配器
     */
    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }

    /**
     * 创建刷新令牌认证提供者。
     *
     * @return 认证提供者
     */
    private AuthenticationProvider getAuthenticationProvider() {
        RefreshTokenAuthenticationProvider provider = RefreshTokenAuthenticationProvider.builder().build();
        return postProcess(provider);
    }
}
