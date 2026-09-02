package com.etd.framework.starter.oauth.authentication.internal.configurer;

import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.factory.UserPasswordAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.filter.UserPasswordAuthenticationRequestFilter;
import com.etd.framework.starter.oauth.authentication.internal.provider.UserPasswordAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用户名密码登录配置器。
 * <p>
 * 注册登录认证提供者和登录请求过滤器。
 */
public class UserPasswordAuthenticationConfigurer extends AbstractSecurityEndpointConfigurer {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    private RequestMatcher authenticationEndpointMatcher;

    /**
     * 自定义登录端点匹配器。
     *
     * @param endpoint 登录端点匹配器
     */
    public UserPasswordAuthenticationConfigurer addEndpointMatcher(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return this;
        }
        this.authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, endpoint);
        return this;
    }

    /**
     * 初始化
     *
     * @param builder 参数 builder
     */
    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider();
        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            // 默认密码登录端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/internal/login");
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
        UserPasswordAuthenticationFilterFactory factory = getApplicationContext(builder)
                .getBean(UserPasswordAuthenticationFilterFactory.class);
        UserPasswordAuthenticationRequestFilter filter = factory.create(authenticationEndpointMatcher, getAuthenticationManager(builder));
        builder.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 获取登录端点匹配器。
     *
     * @return 登录端点匹配器
     */
    /**
     * 获取 RequestMatcher 属性值
     *
     * @return 处理结果
     */
    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }

    /**
     * 创建用户名密码认证提供者。
     *
     * @return 认证提供者
     */
    private AuthenticationProvider getAuthenticationProvider() {
        UserPasswordAuthenticationProvider provider = UserPasswordAuthenticationProvider.builder().build();
        return postProcess(provider);
    }
}
