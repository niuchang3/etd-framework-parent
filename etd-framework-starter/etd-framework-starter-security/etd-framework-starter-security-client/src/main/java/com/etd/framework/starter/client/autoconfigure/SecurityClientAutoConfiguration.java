package com.etd.framework.starter.client.autoconfigure;


import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * 安全客户端自动配置。
 * <p>
 * 提供密码编码器、JWT 公钥解码器以及客户端侧基础组件扫描。
 */
@AutoConfiguration
@ComponentScan("com.etd.framework.starter.client")
@EnableConfigurationProperties(value = SecurityProperties.class)
public class SecurityClientAutoConfiguration {

    /**
     * Bearer 令牌资源访问过滤器链。
     * <p>
     * 作为低优先级兜底链处理普通业务请求，内部登录、刷新令牌等更高优先级链会先匹配。
     *
     * @param http HTTP 安全构建器
     * @param securityProperties 安全配置
     * @return Bearer 令牌资源访问过滤器链
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "bearerAuthenticationSecurityFilterChain")
    public SecurityFilterChain bearerAuthenticationSecurityFilterChain(HttpSecurity http,
                                                                       SecurityProperties securityProperties,
                                                                       AccessDeniedHandlerImpl accessDeniedHandler,
                                                                       AuthenticationEntryPointImpl authenticationEntryPoint) throws Exception {
        List<String> ignorePermissions = securityProperties.getPermissions() == null ? null : securityProperties.getPermissions().getIgnore();
        String[] urls = CollectionUtils.isEmpty(ignorePermissions) ? new String[0] : ignorePermissions.toArray(String[]::new);
        http.with(new BearerAuthenticationConfigurer(), Customizer.withDefaults());
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
     * 提供 Spring Security 默认密码编码器。
     * <p>
     * 业务系统未自定义 {@link PasswordEncoder} 时，使用带算法前缀的委托编码器。
     *
     * @return 密码编码器
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 提供 JWT 解码器。
     *
     * @return 令牌解码器
     */
    @Bean
    @ConditionalOnMissingBean(TokenDecode.class)
    public TokenDecode tokenDecode(SecurityProperties securityProperties) {
        RSAPublicKey rsaPublicKey = SecurityKeyLoader.loadPublicKey(securityProperties);
        return new JwtTokenDecode(rsaPublicKey);
    }

}
