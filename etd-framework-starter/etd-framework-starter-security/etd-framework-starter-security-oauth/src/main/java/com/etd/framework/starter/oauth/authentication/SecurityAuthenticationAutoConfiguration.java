package com.etd.framework.starter.oauth.authentication;


import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.configurer.SecurityAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.memory.MemoryPermissionsServiceImpl;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.etd.framework.starter.oauth.authentication.internal.configurer.LogoutAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.configurer.RefreshTokenAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.factory.AuthenticationFilterSupport;
import com.etd.framework.starter.oauth.authentication.internal.factory.LogoutAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.factory.RefreshTokenAuthenticationFilterFactory;
import com.etd.framework.starter.oauth.authentication.internal.factory.UserPasswordAuthenticationFilterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.ObjectUtils;

import java.security.PrivateKey;

/**
 * 内部登录认证自动配置。
 * <p>
 * 默认注册登录和刷新令牌认证能力，业务系统可以通过自定义 Bean 覆盖默认实现。
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@Import({
        AuthenticationFilterSupport.class,
        UserPasswordAuthenticationFilterFactory.class,
        RefreshTokenAuthenticationFilterFactory.class,
        LogoutAuthenticationFilterFactory.class
})
public class SecurityAuthenticationAutoConfiguration {

    /**
     * 内部认证服务过滤器链。
     * <p>
     * 只处理登录、刷新令牌等内部认证端点，普通业务请求交给客户端侧 Bearer 资源链处理。
     *
     * @param http HTTP 安全构建器
     * @param securityProperties 安全配置
     * @return 内部认证服务过滤器链
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "internalAuthenticationServer")
    public SecurityFilterChain internalAuthenticationServer(HttpSecurity http, SecurityProperties securityProperties,
                                                            AccessDeniedHandlerImpl accessDeniedHandler,
                                                            AuthenticationEntryPointImpl authenticationEntryPoint) throws Exception {
        SecurityAuthenticationConfigurer configurer = new SecurityAuthenticationConfigurer();
        configurer.addConfigurer(new UserPasswordAuthenticationConfigurer().addEndpointMatcher(securityProperties.getAccessToken().getEndpoint()));
        configurer.addConfigurer(new RefreshTokenAuthenticationConfigurer().addEndpointMatcher(securityProperties.getRefreshToken().getEndpoint()));
        if (isLogoutEnabled(securityProperties)) {
            configurer.addConfigurer(new LogoutAuthenticationConfigurer().addEndpointMatcher(securityProperties.getLogout().getEndpoint()));
        }

        http.securityMatcher(configurer.getEndpointsMatcher());
        http.with(configurer, Customizer.withDefaults());
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    /**
     * 判断内部退出登录端点是否启用。
     *
     * @param securityProperties 安全配置
     * @return 是否启用
     */
    private boolean isLogoutEnabled(SecurityProperties securityProperties) {
        return !ObjectUtils.isEmpty(securityProperties.getLogout())
                && Boolean.TRUE.equals(securityProperties.getLogout().getEnabled());
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
     * 默认安全上下文仓储。
     * <p>
     * OAuth2授权码登录成功后会把认证状态保存到HttpSession，Spring Session存在时底层会落到Redis。
     *
     * @return 安全上下文仓储
     */
    /**
     * security Context Repository
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(SecurityContextRepository.class)
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 默认内存用户服务。
     *
     * @return 用户服务
     */
    /**
     * user Service
     *
     * @return 处理结果
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
    /**
     * permissions Service
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(PermissionsService.class)
    public PermissionsService permissionsService() {
        return new MemoryPermissionsServiceImpl();
    }

}
