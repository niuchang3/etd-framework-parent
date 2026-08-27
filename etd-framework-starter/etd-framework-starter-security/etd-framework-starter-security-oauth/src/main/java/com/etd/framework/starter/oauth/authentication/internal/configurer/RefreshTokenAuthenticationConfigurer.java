package com.etd.framework.starter.oauth.authentication.internal.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.token.TokenValue;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.configurer.AbstractSecurityEndpointConfigurer;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.internal.converter.RefreshTokenRequestConverter;
import com.etd.framework.starter.oauth.authentication.internal.filter.RefreshTokenRequestFilter;
import com.etd.framework.starter.oauth.authentication.internal.provider.RefreshTokenAuthenticationProvider;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

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
        this.authenticationEndpointMatcher = PATH_MATCHER.matcher(endpoint);
        return this;
    }

    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider(builder);
        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            // 默认刷新 token 端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/internal/refresh");
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

        RefreshTokenRequestFilter filter = RefreshTokenRequestFilter.builder()
                .refreshTokenEndpointMatcher(authenticationEndpointMatcher)
                .failureHandler(new EtdAuthenticationFailureHandler(objectMapper))
                .successHandler(successHandler)
                .converter(new RefreshTokenRequestConverter(objectMapper))
                .authenticationManager(getAuthenticationManager(builder))
                .tokenEncoder(tokenEncoder)
                .securityProperties(securityProperties)
                .build();

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
     * @param httpSecurity HTTP 安全构建器
     * @return 认证提供者
     */
    @SuppressWarnings("unchecked")
    private AuthenticationProvider getAuthenticationProvider(HttpSecurity httpSecurity) {

        TokenDecode<SignedJWT> tokenDecode = getApplicationContext(httpSecurity).getBean(TokenDecode.class);
        IUserService userService = getApplicationContext(httpSecurity).getBean(IUserService.class);
        ObjectMapper objectMapper = getApplicationContext(httpSecurity).getBean(ObjectMapper.class);
        return RefreshTokenAuthenticationProvider.builder()
                .tokenDecode(tokenDecode)
                .userService(userService)
                .objectMapper(objectMapper)
                .build();
    }
}
