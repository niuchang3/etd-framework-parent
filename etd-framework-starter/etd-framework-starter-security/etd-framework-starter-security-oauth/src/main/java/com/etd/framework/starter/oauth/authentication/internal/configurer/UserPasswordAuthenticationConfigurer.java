package com.etd.framework.starter.oauth.authentication.internal.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.token.TokenValue;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.internal.converter.UserPasswordRequestAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.internal.filter.UserPasswordAuthenticationRequestFilter;
import com.etd.framework.starter.oauth.authentication.internal.provider.UserPasswordAuthenticationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

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
        this.authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, endpoint);;
        return this;
    }

    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider(builder);
        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            // 默认密码登录端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/internal/login");
        }
        builder.authenticationProvider(provider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(HttpSecurity builder) {
        ApplicationContext applicationContext = getApplicationContext(builder);
        TokenEncoder<Authentication, TokenValue> tokenEncoder = applicationContext.getBean(TokenEncoder.class);
        SecurityProperties securityProperties = applicationContext.getBean(SecurityProperties.class);
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        EtdAuthenticationSuccessHandler successHandler = new EtdAuthenticationSuccessHandler(objectMapper);

        UserPasswordAuthenticationRequestFilter filter = UserPasswordAuthenticationRequestFilter.builder()
                .authenticationEndpointMatcher(authenticationEndpointMatcher)
                .authenticationManager(getAuthenticationManager(builder))
                .converter(new UserPasswordRequestAuthenticationConverter(objectMapper))
                .successHandler(successHandler)
                .failureHandler(new EtdAuthenticationFailureHandler(objectMapper))
                .tokenEncoder(tokenEncoder)
                .securityProperties(securityProperties)
                .build();
        builder.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 获取登录端点匹配器。
     *
     * @return 登录端点匹配器
     */
    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }

    /**
     * 创建用户名密码认证提供者。
     *
     * @param httpSecurity HTTP 安全构建器
     * @return 认证提供者
     */
    private AuthenticationProvider getAuthenticationProvider(HttpSecurity httpSecurity) {
        PasswordEncoder passwordEncoder = getApplicationContext(httpSecurity).getBean(PasswordEncoder.class);
        IUserService userService = getApplicationContext(httpSecurity).getBean(IUserService.class);
        return UserPasswordAuthenticationProvider.builder()
                .passwordEncoder(passwordEncoder)
                .userService(userService)
                .build();
    }
}
