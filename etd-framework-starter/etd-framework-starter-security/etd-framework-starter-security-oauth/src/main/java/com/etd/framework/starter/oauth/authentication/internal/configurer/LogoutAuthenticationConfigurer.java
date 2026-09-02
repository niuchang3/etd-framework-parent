package com.etd.framework.starter.oauth.authentication.internal.configurer;

import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationProvider;
import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import com.etd.framework.starter.oauth.authentication.internal.factory.LogoutAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.filter.LogoutRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 退出登录配置器。
 * <p>
 * 注册 Bearer 访问令牌认证提供者和退出登录请求过滤器。
 */
public class LogoutAuthenticationConfigurer extends AbstractSecurityEndpointConfigurer {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    private RequestMatcher logoutEndpointMatcher;

    /**
     * 自定义退出登录端点匹配器。
     *
     * @param endpoint 退出登录端点匹配器
     * @return 退出登录配置器
     */
    public LogoutAuthenticationConfigurer addEndpointMatcher(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return this;
        }
        this.logoutEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, endpoint);
        return this;
    }

    /**
     * 初始化
     *
     * @param builder 参数 builder
     */
    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider(builder);
        if (ObjectUtils.isEmpty(logoutEndpointMatcher)) {
            // 默认退出登录端点。
            logoutEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/internal/logout");
        }
        builder.authenticationProvider(provider);
    }

    /**
     * configure
     *
     * @param builder 参数 builder
     */
    @Override
    public void configure(HttpSecurity builder) {
        LogoutAuthenticationFilterFactory factory = getApplicationContext(builder)
                .getBean(LogoutAuthenticationFilterFactory.class);
        LogoutRequestFilter filter = factory.create(logoutEndpointMatcher, getAuthenticationManager(builder));
        builder.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 获取退出登录端点匹配器。
     *
     * @return 退出登录端点匹配器
     */
    /**
     * 获取 RequestMatcher 属性值
     *
     * @return 处理结果
     */
    @Override
    public RequestMatcher getRequestMatcher() {
        return logoutEndpointMatcher;
    }

    /**
     * 创建访问令牌认证提供者。
     *
     * @param builder HTTP 安全构建器
     * @return 认证提供者
     */
    @SuppressWarnings("unchecked")
    private AuthenticationProvider getAuthenticationProvider(HttpSecurity builder) {
        TokenDecode<SignedJWT> tokenDecode = getApplicationContext(builder).getBean(TokenDecode.class);
        ObjectMapper objectMapper = getApplicationContext(builder).getBean(ObjectMapper.class);
        UserLoginTokenStorage tokenStorage = getApplicationContext(builder).getBean(UserLoginTokenStorage.class);
        BearerAuthenticationProvider provider = new BearerAuthenticationProvider(tokenDecode, objectMapper, tokenStorage);
        return postProcess(provider);
    }
}
