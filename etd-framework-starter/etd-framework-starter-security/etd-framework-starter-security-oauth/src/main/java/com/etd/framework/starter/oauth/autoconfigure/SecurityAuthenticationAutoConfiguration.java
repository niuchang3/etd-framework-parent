package com.etd.framework.starter.oauth.autoconfigure;


import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.configurer.SecurityAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.memory.MemoryPermissionsServiceImpl;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.internal.factory.AuthenticationFilterSupport;
import com.etd.framework.starter.oauth.authentication.internal.factory.RefreshTokenAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.factory.UserPasswordAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.configurer.RefreshTokenAuthenticationConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;

import java.security.PrivateKey;
import java.util.List;

/**
 * 内部登录认证自动配置。
 * <p>
 * 默认注册登录、刷新令牌和 Bearer 访问令牌认证能力，业务系统可以通过自定义 Bean 覆盖默认实现。
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@Import({
        AccessDeniedHandlerImpl.class,
        AuthenticationEntryPointImpl.class,
        EtdAuthenticationFailureHandler.class,
        EtdAuthenticationSuccessHandler.class,
        AuthenticationFilterSupport.class,
        UserPasswordAuthenticationFilterFactory.class,
        RefreshTokenAuthenticationFilterFactory.class
})
public class SecurityAuthenticationAutoConfiguration {

    /**
     * 默认安全过滤器链。
     *
     * @param http HTTP 安全构建器
     * @param securityProperties 安全配置
     * @return 安全过滤器链
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http, SecurityProperties securityProperties,
                                                           AccessDeniedHandlerImpl accessDeniedHandler,
                                                           AuthenticationEntryPointImpl authenticationEntryPoint) throws Exception {
        SecurityAuthenticationConfigurer configurer = new SecurityAuthenticationConfigurer();
        configurer.addConfigurer(new UserPasswordAuthenticationConfigurer().addEndpointMatcher(securityProperties.getAccessToken().getEndpoint()));
        configurer.addConfigurer(new RefreshTokenAuthenticationConfigurer().addEndpointMatcher(securityProperties.getRefreshToken().getEndpoint()));
        configurer.addConfigurer(new BearerAuthenticationConfigurer());

        List<String> ignorePermissions = securityProperties.getPermissions() == null ? null : securityProperties.getPermissions().getIgnore();
        String[] urls = CollectionUtils.isEmpty(ignorePermissions) ? new String[0] : ignorePermissions.toArray(String[]::new);
        http.with(configurer, Customizer.withDefaults());
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    if (urls.length > 0) {
                        authorize.requestMatchers(urls).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    /**
     * 默认 JWT 令牌签发器。
     *
     * @param securityProperties 安全配置
     * @return 令牌签发器
     */
    @Bean
    @ConditionalOnMissingBean(TokenEncoder.class)
    public TokenEncoder<Authentication, ?> tokenEncoder(SecurityProperties securityProperties) {
        PrivateKey privateKey = SecurityKeyLoader.loadPrivateKey(securityProperties);
        return new JwtTokenEncoder(privateKey, securityProperties);
    }

    /**
     * 默认内存用户服务。
     *
     * @return 用户服务
     */
    @Bean
    @ConditionalOnMissingBean(IUserService.class)
    public IUserService userService() {
        return new MemoryUserServiceImpl();
    }


    /**
     * 默认内存权限服务。
     *
     * @return 权限服务
     */
    @Bean
    @ConditionalOnMissingBean(PermissionsService.class)
    public PermissionsService permissionsService() {
        return new MemoryPermissionsServiceImpl();
    }

}
